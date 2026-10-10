"""Proceduralno zgradi 3D maskoto aplikacije Streak v Blenderju.

Lik: oranzna krogla s crno obrobo (inverted hull), "rubber-hose" roke in noge,
bele rokavice in superge, obraz (oci z zenicami, obrvi, usta v vec razlicicah).
Ogrodje (armatura) z animacijami, ki ustrezajo stanjem MascotMood v aplikaciji.

Uporaba (headless):
  blender -b -P tools/mascot/build_mascot.py -- [--glb pot] [--renders mapa]

Izhod:
  - GLB z vsemi animacijami (privzeto app/src/main/assets/models/mascot.glb)
  - WebP poze za 2D fallback, widgete in obvestila (app/src/main/res/drawable-nodpi/mascot_*.webp)

Koordinate so v Blender enotah, Z je gor, lik gleda v smeri -Y.
"""
import math
import os
import sys

import bmesh
import bpy
from mathutils import Matrix, Quaternion, Vector

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "tools", "mascot", "out")
args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
GLB = os.path.join(ROOT, "app", "src", "main", "assets", "models", "mascot.glb")
# Poze kot WebP neposredno v vire aplikacije (2D fallback, widgeti, obvestila)
RENDERS = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
if "--glb" in args:
    GLB = args[args.index("--glb") + 1]
if "--renders" in args:
    RENDERS = args[args.index("--renders") + 1]
NO_RENDER = "--no-render" in args
FPS = 24

# ---------------------------------------------------------------- scena
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.render.fps = FPS
col = scene.collection


def srgb(h):
    """Hex -> linearna RGBA (Blender materiali so v linearnem prostoru)."""
    h = h.lstrip("#")
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lin = [x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return (*lin, 1.0)


def material(name, hex_color, rough=0.55):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    m.use_backface_culling = True  # glTF doubleSided = false (potrebno za obrobo)
    bsdf = m.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = srgb(hex_color)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Specular IOR Level"].default_value = 0.25
    return m


M_ORANGE = material("Orange", "#F28A3D", 0.5)
M_WHITE = material("White", "#FFFFFF", 0.6)
M_INK = material("Ink", "#111114", 0.8)
M_PINK = material("Tongue", "#FF7A8A", 0.6)
M_OUTLINE = material("Outline", "#111114", 1.0)


def link(obj):
    col.objects.link(obj)
    return obj


def mesh_object(name, bm, mat, smooth=True):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    if smooth:
        for p in me.polygons:
            p.use_smooth = True
    me.materials.append(mat)
    return link(bpy.data.objects.new(name, me))


def sphere(name, center, radius, mat, scale=(1, 1, 1), segs=22, rings=11):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=segs, v_segments=rings, radius=radius)
    c = Vector(center)
    # Geometrija v absolutnih koordinatah (objekt ostane v izhodiscu)
    for v in bm.verts:
        v.co = Vector((v.co.x * scale[0], v.co.y * scale[1], v.co.z * scale[2])) + c
    return mesh_object(name, bm, mat)


def outline(obj, thickness):
    """Inverted hull: kopija z napihnjenimi verteksi in obrnjenimi normalami."""
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bm.normal_update()
    for v in bm.verts:
        v.co += v.normal * thickness
    bmesh.ops.reverse_faces(bm, faces=bm.faces)
    o = mesh_object(obj.name + "_outline", bm, M_OUTLINE)
    o.matrix_world = obj.matrix_world.copy()
    return o


def tube(name, points, radius, mat, segs=10, samples=12, taper=None):
    """Cev vzdolz polilinije (Catmull-Rom). Vrne objekt in parameter t (0..1) za vsak verteks."""
    pts = [Vector(p) for p in points]

    def cr(p0, p1, p2, p3, t):
        return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)

    ext = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    path = []
    for i in range(1, len(ext) - 2):
        for j in range(samples):
            path.append(cr(ext[i - 1], ext[i], ext[i + 1], ext[i + 2], j / samples))
    path.append(pts[-1])
    bm = bmesh.new()
    rings = []
    params = []
    up = Vector((0, 1, 0))
    n = len(path)
    for i, p in enumerate(path):
        t_dir = (path[min(i + 1, n - 1)] - path[max(i - 1, 0)]).normalized()
        side = t_dir.cross(up)
        if side.length < 1e-4:
            side = t_dir.cross(Vector((1, 0, 0)))
        side.normalize()
        nrm = side.cross(t_dir).normalized()
        r = radius * (taper(i / (n - 1)) if taper else 1.0)
        ring = []
        for k in range(segs):
            a = 2 * math.pi * k / segs
            ring.append(bm.verts.new(p + side * math.cos(a) * r + nrm * math.sin(a) * r))
            params.append(i / (n - 1))
        rings.append(ring)
    for i in range(n - 1):
        for k in range(segs):
            a, b = rings[i][k], rings[i][(k + 1) % segs]
            c, d = rings[i + 1][(k + 1) % segs], rings[i + 1][k]
            bm.faces.new((a, b, c, d))
    bm.faces.new(list(reversed(rings[0])))
    bm.faces.new(rings[-1])
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.normal_update()
    return mesh_object(name, bm, mat), params


def on_face(x, z, lift):
    """Tocka na sprednji povrsini telesa (krogla) z odmikom navzven."""
    dz = z - 1.62
    r2 = max(0.0, 1.0 - x * x - dz * dz)
    n = Vector((x, -math.sqrt(r2), dz)).normalized()
    return Vector((0, 0, 1.62)) + n * (1.0 + lift)


def arc_tube(name, center, width, height, radius, mat, up=True, depth_y=0.0, segs=16):
    """Lok (obrv, zaprta usta) kot cev, polozena na povrsino obraza."""
    pts = []
    for i in range(segs + 1):
        a = math.pi * i / segs
        x = center[0] - width / 2 + width * i / segs
        z = center[2] + (math.sin(a) * height if up else -math.sin(a) * height)
        pts.append(on_face(x, z, radius * 0.6 + depth_y))
    obj, _ = tube(name, pts, radius, mat, segs=8, samples=2)
    return obj


def half_disc(name, center, w, h, mat, thickness=0.05):
    """Odprta usta (oblika D) na povrsini obraza: pahljaca trikotnikov iz sredisca."""
    bm = bmesh.new()
    n = 24
    rim = []
    for i in range(n + 1):
        a = math.pi * i / n
        rim.append((center[0] - math.cos(a) * w / 2, center[2] - math.sin(a) * h))
    # zgornji rob (ravna crta) z vmesnimi tockami, da sledi ukrivljenosti
    top = [(center[0] + w / 2 - w * i / 8, center[2]) for i in range(1, 8)]
    outline_pts = rim + top
    c2 = (center[0], center[2] - h * 0.4)
    front_c = bm.verts.new(on_face(c2[0], c2[1], 0.025))
    back_c = bm.verts.new(on_face(c2[0], c2[1], 0.025) + Vector((0, thickness, 0)))
    fr = [bm.verts.new(on_face(x, z, 0.025)) for x, z in outline_pts]
    bk = [bm.verts.new(v.co + Vector((0, thickness, 0))) for v in fr]
    m = len(fr)
    for i in range(m):
        j = (i + 1) % m
        bm.faces.new((front_c, fr[j], fr[i]))
        bm.faces.new((back_c, bk[i], bk[j]))
        bm.faces.new((fr[i], fr[j], bk[j], bk[i]))
    # Zaprto telo: normale navzven (sicer jih izlocanje zadnjih ploskev skrije)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.normal_update()
    return mesh_object(name, bm, mat, smooth=False)


# ---------------------------------------------------------------- mere
BODY_C = Vector((0, 0, 1.62))
BODY_R = 1.0
EYE_Y = -0.84
SHOULDER = {"L": Vector((0.93, 0, 1.40)), "R": Vector((-0.93, 0, 1.40))}
ELBOW = {"L": Vector((1.22, 0, 1.05)), "R": Vector((-1.22, 0, 1.05))}
HAND = {"L": Vector((1.32, 0, 0.72)), "R": Vector((-1.32, 0, 0.72))}
HIP = {"L": Vector((0.34, 0, 0.82)), "R": Vector((-0.34, 0, 0.82))}
KNEE = {"L": Vector((0.37, 0, 0.50)), "R": Vector((-0.37, 0, 0.50))}
ANKLE = {"L": Vector((0.40, 0, 0.20)), "R": Vector((-0.40, 0, 0.20))}

# ---------------------------------------------------------------- armatura
arm_data = bpy.data.armatures.new("MascotRig")
rig = link(bpy.data.objects.new("MascotRig", arm_data))
bpy.context.view_layer.objects.active = rig
rig.select_set(True)
bpy.ops.object.mode_set(mode="EDIT")
eb = arm_data.edit_bones


def bone(name, head, tail, parent=None, connect=False):
    b = eb.new(name)
    b.head, b.tail = Vector(head), Vector(tail)
    if parent:
        b.parent = eb[parent]
        b.use_connect = connect
    return b


bone("root", (0, 0, 0), (0, 0, 0.3))
bone("body", (0, 0, 0.75), (0, 0, 1.62), "root")
for s in ("L", "R"):
    sx = 1 if s == "L" else -1
    bone(f"eye.{s}", (0.35 * sx, EYE_Y, 1.80), (0.35 * sx, EYE_Y - 0.2, 1.80), "body")
    bone(f"brow.{s}", (0.36 * sx, -0.80, 2.14), (0.36 * sx, -1.0, 2.14), "body")
    bone(f"arm_up.{s}", SHOULDER[s], ELBOW[s], "body")
    bone(f"arm_lo.{s}", ELBOW[s], HAND[s], f"arm_up.{s}", True)
    bone(f"hand.{s}", HAND[s], HAND[s] + Vector((0.05 * sx, 0, -0.15)), f"arm_lo.{s}", True)
    bone(f"leg_up.{s}", HIP[s], KNEE[s], "root")
    bone(f"leg_lo.{s}", KNEE[s], ANKLE[s], f"leg_up.{s}", True)
    bone(f"foot.{s}", ANKLE[s], ANKLE[s] + Vector((0, -0.2, -0.05)), f"leg_lo.{s}", True)
for m in ("smile", "open", "frown", "flat", "o"):
    bone(f"mouth.{m}", (0, -0.95, 1.40), (0, -1.15, 1.40), "body")
bpy.ops.object.mode_set(mode="OBJECT")


def parent_to_bone(obj, bone_name):
    bpy.context.view_layer.update()
    mw = obj.matrix_world.copy()
    obj.parent = rig
    obj.parent_type = "BONE"
    obj.parent_bone = bone_name
    bpy.context.view_layer.update()
    obj.matrix_world = mw


def skin(obj, params, bones, split=(0.3, 0.75)):
    """Mehke utezi vzdolz cevi za "rubber-hose" upogib (zgornja -> spodnja kost)."""
    up, lo = bones
    g_up = obj.vertex_groups.new(name=up)
    g_lo = obj.vertex_groups.new(name=lo)
    a, b = split
    for i, t in enumerate(params):
        w = min(1.0, max(0.0, (t - a) / (b - a)))
        w = w * w * (3 - 2 * w)
        g_up.add([i], 1 - w, "REPLACE")
        g_lo.add([i], w, "REPLACE")
    obj.parent = rig
    mod = obj.modifiers.new("Armature", "ARMATURE")
    mod.object = rig


# ---------------------------------------------------------------- telo in obraz
body = sphere("Body", BODY_C, BODY_R, M_ORANGE, segs=40, rings=20)
body_ol = outline(body, 0.045)
for o in (body, body_ol):
    parent_to_bone(o, "body")

for s in ("L", "R"):
    sx = 1 if s == "L" else -1
    eye = sphere(f"Eye.{s}", (0.35 * sx, EYE_Y, 1.80), 0.2, M_WHITE, scale=(0.95, 0.55, 1.25), segs=20, rings=10)
    eye_ol = outline(eye, 0.03)
    pupil = sphere(f"Pupil.{s}", (0.36 * sx, EYE_Y - 0.1, 1.76), 0.085, M_INK, scale=(1, 0.6, 1.15), segs=12, rings=6)
    for o in (eye, eye_ol, pupil):
        parent_to_bone(o, f"eye.{s}")
    brow = arc_tube(f"Brow.{s}", (0.36 * sx, -0.82, 2.10), 0.36, 0.07, 0.05, M_INK)
    parent_to_bone(brow, f"brow.{s}")

mouths = {
    "smile": [arc_tube("Mouth.smile", (0, -0.96, 1.42), 0.42, 0.11, 0.055, M_INK, up=False)],
    "open": [half_disc("Mouth.open", (0, -0.985, 1.50), 0.48, 0.28, M_INK),
             sphere("Tongue", tuple(on_face(0, 1.31, 0.02)), 0.085, M_PINK, scale=(1.3, 0.45, 0.55), segs=12, rings=6)],
    "frown": [arc_tube("Mouth.frown", (0, -0.96, 1.30), 0.36, 0.10, 0.055, M_INK, up=True)],
    "flat": [arc_tube("Mouth.flat", (0.02, -0.965, 1.37), 0.26, 0.02, 0.055, M_INK, up=False)],
    "o": [sphere("Mouth.o", (0, -0.97, 1.36), 0.08, M_INK, scale=(1, 0.5, 1.15), segs=12, rings=6)],
}
for key, objs in mouths.items():
    for o in objs:
        parent_to_bone(o, f"mouth.{key}")

# ---------------------------------------------------------------- roke in rokavice
for s in ("L", "R"):
    sx = 1 if s == "L" else -1
    arm, params = tube(f"Arm.{s}", [SHOULDER[s] + Vector((-0.15 * sx, 0, 0.05)), ELBOW[s], HAND[s]], 0.075, M_INK)
    skin(arm, params, (f"arm_up.{s}", f"arm_lo.{s}"))
    glove = sphere(f"Glove.{s}", HAND[s] + Vector((0.03 * sx, 0, -0.12)), 0.21, M_WHITE, scale=(1, 0.9, 1.05))
    thumb = sphere(f"Thumb.{s}", HAND[s] + Vector((-0.13 * sx, -0.12, -0.05)), 0.09, M_WHITE, scale=(1, 1, 1.3), segs=12, rings=6)
    cuff, _ = tube(f"Cuff.{s}", [HAND[s] + Vector((0, 0, 0.06)), HAND[s] + Vector((0.01 * sx, 0, -0.01))], 0.13, M_WHITE, samples=2)
    for o in (glove, thumb, cuff):
        parent_to_bone(o, f"hand.{s}")
        parent_to_bone(outline(o, 0.025), f"hand.{s}")

    leg, lparams = tube(f"Leg.{s}", [HIP[s], KNEE[s], ANKLE[s]], 0.08, M_INK)
    skin(leg, lparams, (f"leg_up.{s}", f"leg_lo.{s}"), split=(0.35, 0.7))
    # Superga: zaobljen podplat in kapica spredaj
    shoe = sphere(f"Shoe.{s}", ANKLE[s] + Vector((0.02 * sx, -0.12, -0.08)), 0.2, M_WHITE, scale=(0.95, 1.45, 0.6))
    sole = sphere(f"Sole.{s}", ANKLE[s] + Vector((0.02 * sx, -0.12, -0.16)), 0.2, M_INK, scale=(1.0, 1.5, 0.22))
    for o in (shoe, sole):
        parent_to_bone(o, f"foot.{s}")
    parent_to_bone(outline(shoe, 0.025), f"foot.{s}")

# ---------------------------------------------------------------- animacija (ciljne tocke)
pb = rig.pose.bones


def rest_dir(name):
    b = arm_data.bones[name]
    return (b.tail_local - b.head_local).normalized()


def aim_chain(names, points):
    """Usmeri verigo kosti proti zaporedju tock (komolec, roka ...) v prostoru ogrodja."""
    world_parent = Quaternion()
    for name, target_from, target_to in zip(names, points[:-1], points[1:]):
        b = arm_data.bones[name]
        B = b.matrix_local.to_quaternion()
        current = world_parent @ B
        cur_dir = current @ Vector((0, 1, 0))
        want = (Vector(target_to) - Vector(target_from)).normalized()
        delta = cur_dir.rotation_difference(want)
        local = current.inverted() @ delta @ current
        pb[name].rotation_mode = "QUATERNION"
        pb[name].rotation_quaternion = local
        world_parent = world_parent @ B @ local @ B.inverted()


def reset_pose():
    for p in pb:
        p.rotation_mode = "QUATERNION"
        p.rotation_quaternion = Quaternion()
        p.location = Vector()
        p.scale = Vector((1, 1, 1))


def key_all(frame):
    for p in pb:
        p.keyframe_insert("rotation_quaternion", frame=frame)
        p.keyframe_insert("location", frame=frame)
        p.keyframe_insert("scale", frame=frame)


def mouth(kind):
    for m in ("smile", "open", "frown", "flat", "o"):
        s = 1.0 if m == kind else 0.001
        pb[f"mouth.{m}"].scale = Vector((s, s, s))


def body_move(dz=0.0, lean_x=0.0, tilt_y=0.0, squash=1.0):
    pb["body"].location = Vector((0, 0, 0))
    pb["root"].location = Vector((0, 0, dz))
    q = Quaternion((1, 0, 0), math.radians(lean_x)) @ Quaternion((0, 1, 0), math.radians(tilt_y))
    pb["body"].rotation_quaternion = q
    pb["body"].scale = Vector((1 / math.sqrt(squash), squash, 1 / math.sqrt(squash)))


def eyes(open_amount=1.0, look=(0.0, 0.0)):
    for s in ("L", "R"):
        pb[f"eye.{s}"].scale = Vector((1, 1, max(open_amount, 0.08)))
        pb[f"eye.{s}"].location = Vector((look[0], 0, look[1]))


def brows(raise_l=0.0, raise_r=0.0, angle=0.0):
    pb["brow.L"].location = Vector((0, 0, raise_l))
    pb["brow.R"].location = Vector((0, 0, raise_r))
    pb["brow.L"].rotation_quaternion = Quaternion((0, 1, 0), math.radians(angle))
    pb["brow.R"].rotation_quaternion = Quaternion((0, 1, 0), math.radians(-angle))


def arm(s, elbow, hand):
    aim_chain([f"arm_up.{s}", f"arm_lo.{s}"], [SHOULDER[s], Vector(elbow), Vector(hand)])


def leg(s, knee, ankle):
    aim_chain([f"leg_up.{s}", f"leg_lo.{s}"], [HIP[s], Vector(knee), Vector(ankle)])


def arms_down(swing=0.0, out=0.0):
    arm("L", (1.24 + out, swing, 1.05), (1.30 + out, -swing, 0.72))
    arm("R", (-1.24 - out, -swing, 1.05), (-1.30 - out, swing, 0.72))


def legs_stand():
    for s in ("L", "R"):
        leg(s, KNEE[s], ANKLE[s])


def new_action(name):
    act = bpy.data.actions.new(name)
    act.use_fake_user = True
    rig.animation_data_create()
    rig.animation_data.action = act
    return act


def make(name, frames, pose_fn):
    """Ustvari akcijo: pose_fn(frame) nastavi pozo za vsak kljucni okvir."""
    new_action(name)
    for f in frames:
        reset_pose()
        pose_fn(f)
        key_all(f)
    act = rig.animation_data.action
    for fc in act.fcurves:
        for kp in fc.keyframe_points:
            kp.interpolation = "BEZIER"
    return act


ACTIONS = {}


def idle(f):
    t = f / 48 * 2 * math.pi
    body_move(dz=0.03 * math.sin(t), squash=1 + 0.015 * math.sin(t))
    arms_down(swing=0.04 * math.sin(t))
    legs_stand()
    eyes(0.1 if f == 42 else 1.0)
    brows()
    mouth("open")


ACTIONS["idle"] = make("idle", [0, 12, 24, 36, 41, 42, 44, 48], idle)


def wave(f):
    t = f / 12 * math.pi
    body_move(dz=0.02 * math.sin(t), tilt_y=-4)
    arm("L", (1.30, 0, 1.05), (1.32, 0, 0.72))
    hx = -1.25 - 0.22 * math.sin(t)
    arm("R", (-1.42, -0.05, 1.85), (hx, -0.15, 2.55))
    legs_stand()
    eyes(1.0, (0, 0.02))
    brows(0.05, 0.08)
    mouth("open")


ACTIONS["wave"] = make("wave", list(range(0, 49, 6)), wave)


def cheer(f):
    jump = max(0.0, math.sin(f / 36 * 2 * math.pi))
    body_move(dz=0.35 * jump, squash=1 + 0.06 * (jump - 0.5))
    for s, sx in (("L", 1), ("R", -1)):
        arm(s, (1.35 * sx, -0.05, 1.95 + 0.1 * jump), (1.15 * sx, -0.1, 2.75 + 0.1 * jump))
        leg(s, (0.45 * sx, -0.18 * jump, 0.55), (0.45 * sx, 0.05 * jump, 0.22 + 0.05 * jump))
    eyes(0.35)
    brows(0.1, 0.1)
    mouth("open")


ACTIONS["cheer"] = make("cheer", list(range(0, 37, 6)), cheer)


def sad(f):
    t = f / 72 * 2 * math.pi
    body_move(dz=-0.04, lean_x=-10, tilt_y=4 * math.sin(t))
    arm("L", (1.10, -0.15, 0.95), (1.02, -0.2, 0.55))
    arm("R", (-1.10, -0.15, 0.95), (-1.02, -0.2, 0.55))
    legs_stand()
    eyes(0.8, (0, -0.04))
    brows(0.04, 0.04, angle=-14)
    mouth("frown")


ACTIONS["sad"] = make("sad", list(range(0, 73, 12)), sad)


def think(f):
    t = f / 72 * 2 * math.pi
    body_move(tilt_y=6 + 2 * math.sin(t))
    arm("L", (1.30, 0, 1.05), (1.32, 0, 0.72))
    arm("R", (-1.05, -0.65, 0.95), (-0.38, -1.02, 1.12 + 0.02 * math.sin(t)))
    legs_stand()
    eyes(1.0, (0.06, 0.06))
    brows(0.0, 0.12)
    mouth("flat")


ACTIONS["think"] = make("think", list(range(0, 73, 12)), think)


def sleep(f):
    t = f / 96 * 2 * math.pi
    body_move(dz=-0.05, tilt_y=10, squash=1 + 0.03 * math.sin(t))
    arm("L", (1.05, -0.4, 1.0), (0.5, -0.85, 1.05))
    arm("R", (-1.05, -0.4, 1.0), (-0.5, -0.85, 1.05))
    legs_stand()
    eyes(0.08)
    brows(-0.02, -0.02)
    pb["mouth.o"].scale = Vector((0.8 + 0.2 * math.sin(t),) * 3)
    for m in ("smile", "open", "frown", "flat"):
        pb[f"mouth.{m}"].scale = Vector((0.001,) * 3)


ACTIONS["sleep"] = make("sleep", list(range(0, 97, 12)), sleep)


def meditate(f):
    t = f / 96 * 2 * math.pi
    body_move(dz=0.45 + 0.08 * math.sin(t), squash=1 + 0.02 * math.sin(t))
    for s, sx in (("L", 1), ("R", -1)):
        leg(s, (1.0 * sx, -0.5, 0.48), (-0.22 * sx, -0.75, 0.40))
        arm(s, (1.28 * sx, -0.3, 1.0), (1.02 * sx, -0.62, 0.62))
    eyes(0.08)
    brows()
    mouth("smile")


ACTIONS["meditate"] = make("meditate", list(range(0, 97, 12)), meditate)


def thumbs(f):
    t = f / 48 * 2 * math.pi
    body_move(dz=0.02 * math.sin(t), tilt_y=-5)
    arm("L", (1.30, 0, 1.05), (1.32, 0, 0.72))
    arm("R", (-1.30, -0.35, 1.25), (-1.05, -0.85, 1.65 + 0.03 * math.sin(t)))
    legs_stand()
    pb["eye.L"].scale = Vector((1, 1, 1))
    pb["eye.R"].scale = Vector((1, 1, 0.12 if 8 <= f <= 16 else 1))
    brows(0.06, 0.02)
    mouth("open")


ACTIONS["thumbs"] = make("thumbs", list(range(0, 49, 4)), thumbs)


def walk(f):
    t = f / 24 * 2 * math.pi
    body_move(dz=0.05 * abs(math.sin(t)), tilt_y=3 * math.sin(t))
    sw = 0.3 * math.sin(t)
    arm("L", (1.24, sw, 1.05), (1.30, sw * 1.5, 0.75))
    arm("R", (-1.24, -sw, 1.05), (-1.30, -sw * 1.5, 0.75))
    for s, sx, ph in (("L", 1, 0), ("R", -1, math.pi)):
        st = math.sin(t + ph)
        lift = max(0.0, math.cos(t + ph)) * 0.12
        leg(s, (0.37 * sx, -0.25 * st - 0.05, 0.50 + lift), (0.40 * sx, -0.3 * st, 0.20 + lift))
    eyes(1.0)
    brows()
    mouth("smile")


ACTIONS["walk"] = make("walk", list(range(0, 25, 3)), walk)

# Poza za staticne slike: okvir posamezne akcije
POSE_FRAMES = {"idle": 0, "wave": 12, "cheer": 9, "sad": 0, "think": 18, "sleep": 0, "meditate": 24, "thumbs": 0, "walk": 6}

# Konec akcij nastavi sceno
reset_pose()
rig.animation_data.action = ACTIONS["idle"]
scene.frame_start, scene.frame_end = 0, 48

# ---------------------------------------------------------------- izvoz GLB
os.makedirs(os.path.dirname(GLB), exist_ok=True)
for o in scene.objects:
    o.select_set(True)
bpy.ops.export_scene.gltf(
    filepath=GLB,
    export_format="GLB",
    use_selection=False,
    export_apply=True,
    export_animations=True,
    export_animation_mode="ACTIONS",
    export_force_sampling=True,
    export_frame_step=1,
    export_skins=True,
    export_morph=False,
    export_yup=True,
    export_lights=False,
    export_cameras=False,
)
print("GLB:", GLB, os.path.getsize(GLB), "B")

# ---------------------------------------------------------------- render poz
if not NO_RENDER:
    os.makedirs(RENDERS, exist_ok=True)
    cam_data = bpy.data.cameras.new("Cam")
    cam_data.lens = 86
    cam = link(bpy.data.objects.new("Cam", cam_data))
    cam.location = Vector((1.1, -9.8, 2.4))
    direction = Vector((0, 0, 1.32)) - cam.location
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    scene.camera = cam
    sun = link(bpy.data.objects.new("Sun", bpy.data.lights.new("Sun", "SUN")))
    sun.data.energy = 3.2
    sun.rotation_euler = (math.radians(50), math.radians(10), math.radians(-35))
    fill = link(bpy.data.objects.new("Fill", bpy.data.lights.new("Fill", "SUN")))
    fill.data.energy = 1.0
    fill.rotation_euler = (math.radians(70), 0, math.radians(140))
    world = bpy.data.worlds.new("World")
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs[0].default_value = (0.9, 0.9, 0.95, 1)
    world.node_tree.nodes["Background"].inputs[1].default_value = 0.9
    scene.world = world
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.film_transparent = True
    scene.render.resolution_x = 480
    scene.render.resolution_y = 520
    scene.render.image_settings.file_format = "WEBP"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.quality = 88
    scene.view_settings.view_transform = "Standard"
    for name, act in ACTIONS.items():
        rig.animation_data.action = act
        scene.frame_set(POSE_FRAMES[name])
        scene.render.filepath = os.path.join(RENDERS, f"mascot_{name}.webp")
        bpy.ops.render.render(write_still=True)
        print("render:", name)

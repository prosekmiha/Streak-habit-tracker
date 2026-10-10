"""Izpise povzetek GLB: animacije, trikotniki, materiali, velikost (preverjanje proracuna)."""
import json, struct, sys, os
path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "models", "mascot.glb")
data = open(path, "rb").read()
magic, version, length = struct.unpack_from("<III", data, 0)
clen, ctype = struct.unpack_from("<II", data, 12)
gltf = json.loads(data[20:20 + clen])
tris = 0
for m in gltf["meshes"]:
    for p in m["primitives"]:
        acc = gltf["accessors"][p["indices"]]
        tris += acc["count"] // 3
print("size_bytes", len(data))
print("triangles", tris)
print("meshes", len(gltf["meshes"]), "nodes", len(gltf["nodes"]), "skins", len(gltf.get("skins", [])))
print("materials", [(m["name"], m.get("doubleSided", False)) for m in gltf["materials"]])
print("animations", [a["name"] for a in gltf.get("animations", [])])
print("extensions", gltf.get("extensionsUsed", []))
budget_ok = tris <= 15000 and len(data) <= 2_000_000
print("BUDGET_OK" if budget_ok else "BUDGET_EXCEEDED")
sys.exit(0 if budget_ok else 1)

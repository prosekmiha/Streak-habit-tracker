package com.streak.app.ui.mascot

import android.content.Context
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.ToneMapper
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder

// En Filament Engine za celo aplikacijo (ustvarjanje je drago)
internal object MascotEngine {
    val engine: Engine by lazy {
        Gltfio.init()
        Engine.create()
    }

    // GLB se prebere enkrat in ostane v pomnilniku (~0,5 MB)
    @Volatile private var cachedGlb: ByteBuffer? = null

    fun glb(context: Context): ByteBuffer = cachedGlb ?: synchronized(this) {
        cachedGlb ?: context.assets.open(MODEL_PATH).use { input ->
            val bytes = input.readBytes()
            ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).also { it.flip() }
        }.also { cachedGlb = it }
    }

    const val MODEL_PATH = "models/mascot.glb"
}

// Izris 3D maskote v TextureView s prozornim ozadjem. Kamera in luci se ujemajo z
// renderjem v Blenderju, zato so 3D in 2D (WebP) poze kadrirane enako.
internal class MascotScene(private val context: Context, private val textureView: TextureView) {
    private val engine = MascotEngine.engine
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val sun = EntityManager.get().create()
    private val fill = EntityManager.get().create()
    private val indirectLight: IndirectLight
    private val materialProvider = UbershaderProvider(engine)
    private val assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
    private val resourceLoader = ResourceLoader(engine)
    private var asset: FilamentAsset? = null
    private var swapChain: SwapChain? = null
    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private val choreographer = Choreographer.getInstance()

    private var animationNames = emptyList<String>()
    private var current = 0
    private var currentStart = 0L
    private var previous = -1
    private var previousStart = 0L
    private var fadeStart = 0L
    private var running = false
    private var destroyed = false
    var frameIntervalNanos = 0L // 0 = vsak okvir; za varcni nacin npr. 33 ms
    private var lastFrame = 0L

    init {
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        view.colorGrading = ColorGrading.Builder().toneMapper(ToneMapper.Linear()).build(engine)
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = true; sampleCount = 4 }
        renderer.clearOptions = renderer.clearOptions.apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }
        camera.setExposure(16f, 1f / 125f, 100f)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.98f, 0.95f)
            .intensity(29_000f)
            .direction(-0.45f, -0.75f, -0.5f)
            .castShadows(false)
            .build(engine, sun)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.85f, 0.88f, 1f)
            .intensity(9_000f)
            .direction(0.6f, -0.3f, 0.4f)
            .castShadows(false)
            .build(engine, fill)
        scene.addEntity(sun)
        scene.addEntity(fill)
        indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(1f, 1f, 1.05f))
            .intensity(26_000f)
            .build(engine)
        scene.indirectLight = indirectLight

        loadModel()

        uiHelper.isOpaque = false
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface, uiHelper.swapChainFlags)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    engine.flushAndWait()
                }
                swapChain = null
            }

            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                // Kamera kot v Blenderju (86 mm, senzor 36 mm po visini): 2 * atan(18 / 86)
                camera.setProjection(VERTICAL_FOV, width.toDouble() / height, 0.1, 50.0, Camera.Fov.VERTICAL)
                camera.lookAt(1.1, 2.4, 9.8, 0.0, 1.32, 0.0, 0.0, 1.0, 0.0)
            }
        }
        uiHelper.attachTo(textureView)
    }

    private fun loadModel() {
        val buffer = MascotEngine.glb(context).duplicate()
        val a = assetLoader.createAsset(buffer) ?: return
        resourceLoader.loadResources(a)
        a.releaseSourceData()
        scene.addEntities(a.entities)
        asset = a
        val animator = a.instance.animator
        animationNames = (0 until animator.animationCount).map { animator.getAnimationName(it) }
    }

    // Zamenjaj animacijo z mehkim prehodom (cross-fade)
    fun setAnimation(name: String) {
        val index = animationNames.indexOf(name).takeIf { it >= 0 } ?: animationNames.indexOf("idle").coerceAtLeast(0)
        if (index == current && currentStart != 0L) return
        val now = System.nanoTime()
        if (currentStart != 0L) {
            previous = current
            previousStart = currentStart
            fadeStart = now
        }
        current = index
        currentStart = now
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running || destroyed) return
            choreographer.postFrameCallback(this)
            if (frameIntervalNanos > 0 && frameTimeNanos - lastFrame < frameIntervalNanos) return
            lastFrame = frameTimeNanos
            renderFrame(frameTimeNanos)
        }
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val a = asset ?: return
        val animator = a.instance.animator
        if (animator.animationCount > 0) {
            if (currentStart == 0L) currentStart = frameTimeNanos
            val duration = animator.getAnimationDuration(current).coerceAtLeast(0.01f)
            val t = ((frameTimeNanos - currentStart) / 1e9f) % duration
            animator.applyAnimation(current, t)
            if (previous >= 0) {
                val alpha = ((frameTimeNanos - fadeStart) / 1e9f / FADE_SECONDS).coerceIn(0f, 1f)
                if (alpha >= 1f) {
                    previous = -1
                } else {
                    val pDur = animator.getAnimationDuration(previous).coerceAtLeast(0.01f)
                    val pt = ((frameTimeNanos - previousStart) / 1e9f) % pDur
                    animator.applyCrossFade(previous, pt, alpha)
                }
            }
            animator.updateBoneMatrices()
        }
        val chain = swapChain ?: return
        if (uiHelper.isReadyToRender && renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun resume() {
        if (running || destroyed) return
        running = true
        choreographer.postFrameCallback(frameCallback)
    }

    fun pause() {
        running = false
        choreographer.removeFrameCallback(frameCallback)
    }

    // Sprosti vse GPU vire (Engine ostane za naslednjo uporabo)
    fun destroy() {
        if (destroyed) return
        pause()
        destroyed = true
        uiHelper.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
        asset?.let {
            scene.removeEntities(it.entities)
            assetLoader.destroyAsset(it)
        }
        asset = null
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        assetLoader.destroy()
        resourceLoader.destroy()
        engine.destroyIndirectLight(indirectLight)
        engine.destroyEntity(sun)
        engine.destroyEntity(fill)
        EntityManager.get().destroy(sun)
        EntityManager.get().destroy(fill)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.flushAndWait()
    }

    private companion object {
        const val VERTICAL_FOV = 23.64
        const val FADE_SECONDS = 0.35f
    }
}

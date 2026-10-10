package com.streak.app.ui.mascot

import androidx.compose.ui.graphics.Color

// Vektorska risba maskote v koordinatah viewBox 240 x 260 (enako kot mockupi).
// Poti so SVG "d" nizi; risanje je v MascotView.

internal sealed interface MascotEl {
    data class P(val d: String, val fill: Color? = null, val stroke: Color? = null, val width: Float = 0f, val alpha: Float = 1f) : MascotEl
    data class C(val cx: Float, val cy: Float, val r: Float, val fill: Color? = null, val stroke: Color? = null, val width: Float = 0f) : MascotEl
    data class E(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val fill: Color? = null, val stroke: Color? = null, val width: Float = 0f, val alpha: Float = 1f) : MascotEl
    data class R(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float, val fill: Color, val rotate: Float = 0f) : MascotEl
}

internal object MascotArt {
    const val VIEW_W = 240f
    const val VIEW_H = 260f

    val Ink = Color(0xFF111114)
    private val Orange = Color(0xFFF28A3D)
    private val Highlight = Color(0xFFFFC9A3)
    private val White = Color.White
    private val Tongue = Color(0xFFFF7A8A)

    // --- skupni deli ---
    private fun shadow(rx: Float = 52f, cy: Float = 246f, alpha: Float = .18f) =
        MascotEl.E(120f, cy, rx, 8f, fill = Ink, alpha = alpha)

    private val legs = MascotEl.P("M104 176C102 200 98 214 96 226M136 176C138 200 142 214 144 226", stroke = Ink, width = 8f)
    private val shoeL = MascotEl.P("M72 238c0-10 8-18 22-18h6c9 0 14 7 14 15v3H72z", fill = White, stroke = Ink, width = 5f)
    private val shoeR = MascotEl.P("M126 238v-3c0-8 5-15 14-15h6c14 0 22 8 22 18H126z", fill = White, stroke = Ink, width = 5f)
    private fun body(cy: Float = 118f) = listOf(
        MascotEl.C(120f, cy, 62f, fill = Orange, stroke = Ink, width = 6f),
        MascotEl.P(highlightPath(cy), stroke = Highlight, width = 8f)
    )
    private fun highlightPath(cy: Float): String {
        val y1 = cy - 20f
        val y2 = cy - 50f
        return "M74 ${y1}a50 50 0 0 1 26-${y1 - y2}"
    }
    private fun eyesOpen(cy: Float = 110f) = listOf(
        MascotEl.E(100f, cy, 13f, 16f, fill = White, stroke = Ink, width = 5f),
        MascotEl.E(140f, cy, 13f, 16f, fill = White, stroke = Ink, width = 5f)
    )
    private fun pupils(dx: Float, dy: Float, baseY: Float = 110f) = listOf(
        MascotEl.C(100f + dx, baseY + dy, 6.5f, fill = Ink),
        MascotEl.C(140f + dx, baseY + dy, 6.5f, fill = Ink)
    )
    private fun glove(cx: Float, cy: Float, r: Float = 15f) = MascotEl.C(cx, cy, r, fill = White, stroke = Ink, width = 5f)
    private fun line(d: String, w: Float = 6f) = MascotEl.P(d, stroke = Ink, width = w)
    private val bigSmile = listOf(
        MascotEl.P("M102 138q18 20 36 0z", fill = Ink, stroke = Ink, width = 3f),
        MascotEl.P("M112 147q8-6 16 0q-8 5-16 0z", fill = Tongue)
    )

    private val idle: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M60 128C44 140 40 156 38 168M180 128C196 140 200 156 202 168", 8f))
        addAll(body()); addAll(eyesOpen()); addAll(pupils(3f, 4f))
        add(line("M86 84q14-8 28 0M126 84q14-8 28 0"))
        addAll(bigSmile)
        add(glove(38f, 174f)); add(glove(202f, 174f))
    }

    private val wave: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M60 128C44 140 40 156 38 168M178 112C198 96 206 74 206 52", 8f))
        addAll(body()); addAll(eyesOpen()); addAll(pupils(4f, 2f))
        add(line("M86 82q14-10 28-2M126 80q14-8 28 2"))
        add(MascotEl.P("M100 136q20 24 40 0z", fill = Ink, stroke = Ink, width = 3f))
        add(MascotEl.P("M112 147q8-6 16 0q-8 5-16 0z", fill = Tongue))
        add(glove(38f, 174f)); add(glove(206f, 44f, 16f))
        add(line("M218 18l8-8M226 34l10-2M192 18l-4-10", 4f))
    }

    private val cheer: List<MascotEl> = buildList {
        add(MascotEl.R(20f, 20f, 12f, 7f, 2f, Color(0xFFF7CD63), -20f))
        add(MascotEl.R(214f, 96f, 12f, 7f, 2f, Color(0xFFFC8FC6), 25f))
        add(MascotEl.R(12f, 104f, 12f, 7f, 2f, Color(0xFFA9C8F5), 30f))
        add(MascotEl.R(112f, 10f, 12f, 7f, 2f, Color(0xFFB8EB6C), -10f))
        add(MascotEl.C(176f, 20f, 5f, fill = Color(0xFF4E55E0)))
        add(MascotEl.C(64f, 42f, 4f, fill = Color(0xFFFC8FC6)))
        add(shadow(44f, alpha = .14f))
        add(line("M104 172C100 192 94 204 88 214M136 172C140 192 146 204 152 214", 8f))
        add(MascotEl.P("M62 228c0-10 8-18 22-18h6c9 0 14 7 14 15v3H62z", fill = White, stroke = Ink, width = 5f))
        add(MascotEl.P("M136 228v-3c0-8 5-15 14-15h6c14 0 22 8 22 18H136z", fill = White, stroke = Ink, width = 5f))
        add(line("M62 110C42 94 34 72 34 50M178 110C198 94 206 72 206 50", 8f))
        addAll(body(112f))
        add(line("M88 104q12-14 24 0M128 104q12-14 24 0"))
        add(line("M86 76q14-10 28-4M126 72q14-6 28 4"))
        add(MascotEl.P("M94 126q26 40 52 0z", fill = Ink, stroke = Ink, width = 3f))
        add(MascotEl.P("M108 144q12-8 24 0q-12 9-24 0z", fill = Tongue))
        add(glove(34f, 42f, 16f)); add(glove(206f, 42f, 16f))
    }

    private val sad: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M62 134C52 152 50 168 52 182M178 134C188 152 190 168 188 182", 8f))
        addAll(body(120f))
        add(MascotEl.E(100f, 114f, 13f, 15f, fill = White, stroke = Ink, width = 5f))
        add(MascotEl.E(140f, 114f, 13f, 15f, fill = White, stroke = Ink, width = 5f))
        add(MascotEl.C(100f, 120f, 6.5f, fill = Ink)); add(MascotEl.C(140f, 120f, 6.5f, fill = Ink))
        add(line("M86 94l26-8M128 86l26 8"))
        add(line("M104 152q16-12 32 0"))
        add(glove(52f, 186f)); add(glove(188f, 186f))
    }

    private val sleep: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M62 132C54 150 66 162 86 166M178 132C186 150 174 162 154 166", 8f))
        addAll(body(120f))
        add(line("M88 114q12 10 24 0M128 114q12 10 24 0"))
        add(line("M88 92q12-4 24 0M128 92q12-4 24 0"))
        add(MascotEl.C(120f, 146f, 7f, fill = Ink))
        add(glove(90f, 168f)); add(glove(150f, 168f))
        add(line("M176 34h18l-18 20h18M204 8h12l-12 13h12", 5f))
    }

    private val meditate: List<MascotEl> = buildList {
        add(MascotEl.E(120f, 248f, 34f, 6f, fill = Ink, alpha = .85f))
        add(MascotEl.P("M30 130c-14 0-20-12-10-20 2-12 18-14 24-6 10-6 24 2 20 14", fill = White, stroke = Ink, width = 4f))
        add(MascotEl.P("M210 120c14 0 20-12 10-20-2-12-18-14-24-6-10-6-24 2-20 14", fill = White, stroke = Ink, width = 4f))
        add(line("M80 194q40 24 80 0", 9f))
        add(MascotEl.P("M50 206c0-8 6-14 16-14h6c9 0 14 6 14 14v2H50z", fill = White, stroke = Ink, width = 5f))
        add(MascotEl.P("M154 208v-2c0-8 5-14 14-14h6c10 0 16 6 16 14v2h-36z", fill = White, stroke = Ink, width = 5f))
        add(line("M62 116C46 138 52 166 70 178M178 116C194 138 188 166 170 178", 8f))
        addAll(body(104f))
        add(line("M88 102q12 9 24 0M128 102q12 9 24 0"))
        add(line("M88 80q12-3 24 0M128 80q12-3 24 0"))
        add(line("M108 128q12 8 24 0"))
        add(glove(72f, 182f, 14f)); add(glove(168f, 182f, 14f))
    }

    private val think: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M60 128C44 140 40 156 38 168M178 132C196 152 184 172 160 168", 8f))
        addAll(body()); addAll(eyesOpen()); addAll(pupils(6f, -6f))
        add(line("M86 86q14-6 28 0M128 76q14-10 28-2"))
        add(line("M108 142q8 3 24-2"))
        add(glove(38f, 174f)); add(glove(154f, 166f))
        add(MascotEl.C(196f, 48f, 6f, stroke = Ink, width = 4f))
        add(MascotEl.C(212f, 28f, 9f, stroke = Ink, width = 4f))
    }

    private val thumbs: List<MascotEl> = buildList {
        add(shadow()); add(legs); add(shoeL); add(shoeR)
        add(line("M60 128C44 140 40 156 38 168M178 126C196 124 206 116 210 104", 8f))
        addAll(body())
        add(MascotEl.E(100f, 110f, 13f, 16f, fill = White, stroke = Ink, width = 5f))
        add(MascotEl.C(103f, 113f, 6.5f, fill = Ink))
        add(line("M128 112q12-10 24 0"))
        add(line("M86 84q14-8 28 0M126 88q14-6 28 0"))
        add(line("M102 136q20 18 40-2"))
        add(glove(38f, 174f))
        add(MascotEl.P("M206 92h10a10 10 0 0 1 10 10v6a10 10 0 0 1-10 10h-10a10 10 0 0 1-10-10v-6a10 10 0 0 1 10-10z", fill = White, stroke = Ink, width = 5f))
        add(MascotEl.P("M208 92V72a6 6 0 0 1 12 0v20", fill = White, stroke = Ink, width = 5f))
    }

    fun elements(mood: MascotMood): List<MascotEl> = when (mood) {
        MascotMood.IDLE, MascotMood.WALK, MascotMood.STRETCHING -> idle
        MascotMood.WAVE -> wave
        MascotMood.HAPPY, MascotMood.CHEER -> cheer
        MascotMood.SAD -> sad
        MascotMood.SLEEPING -> sleep
        MascotMood.MEDITATING -> meditate
        MascotMood.THINKING, MascotMood.WORKING -> think
        MascotMood.THUMBS_UP -> thumbs
        MascotMood.FREEZE -> sleep
        MascotMood.VACATION -> thumbs
    }
}

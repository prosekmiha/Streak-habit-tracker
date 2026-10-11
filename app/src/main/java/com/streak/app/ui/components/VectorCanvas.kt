package com.streak.app.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import kotlin.math.roundToInt

// Izris Compose vektorja (Material ikone) na Android Canvas - za bitne slike izven
// kompozicije (widgeti, slika za deljenje). Ikone so enobarvne, zato barva velja za vse poti.
fun Canvas.drawImageVector(vector: ImageVector, left: Float, top: Float, size: Float, color: Int) {
    withTranslation(left, top) {
        scale(size / vector.viewportWidth, size / vector.viewportHeight)
        drawGroup(vector.root, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }
}

private fun Canvas.drawGroup(group: VectorGroup, paint: Paint): Unit = withSave {
    translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
    rotate(group.rotation)
    scale(group.scaleX, group.scaleY)
    translate(-group.pivotX, -group.pivotY)
    for (node in group) {
        when (node) {
            is VectorGroup -> drawGroup(node, paint)
            is VectorPath -> {
                val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
                path.fillType = if (node.pathFillType == PathFillType.EvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
                val alpha = paint.alpha
                paint.alpha = (alpha * node.fillAlpha).roundToInt()
                drawPath(path, paint)
                paint.alpha = alpha
            }
        }
    }
}

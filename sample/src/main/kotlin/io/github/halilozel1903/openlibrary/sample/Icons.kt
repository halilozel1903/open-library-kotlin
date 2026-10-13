package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Small drawn icons, so the sample needs no icon library. */
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawCircle(color, radius = s * 0.28f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = stroke))
        drawLine(color, Offset(s * 0.63f, s * 0.63f), Offset(s * 0.86f, s * 0.86f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Three books on a shelf. */
@Composable
fun ShelfIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.09f
        val style = Stroke(width = stroke, join = StrokeJoin.Round)
        drawRoundRect(color, Offset(s * 0.14f, s * 0.22f), Size(s * 0.18f, s * 0.6f), CornerRadius(s * 0.03f), style = style)
        drawRoundRect(color, Offset(s * 0.38f, s * 0.14f), Size(s * 0.18f, s * 0.68f), CornerRadius(s * 0.03f), style = style)
        val lean = Path().apply {
            moveTo(s * 0.62f, s * 0.3f)
            lineTo(s * 0.78f, s * 0.24f)
            lineTo(s * 0.92f, s * 0.78f)
            lineTo(s * 0.76f, s * 0.84f)
            close()
        }
        drawPath(lean, color, style = style)
        drawLine(color, Offset(s * 0.06f, s * 0.9f), Offset(s * 0.94f, s * 0.9f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** A bookmark ribbon. */
@Composable
fun BookmarkIcon(color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Canvas(modifier) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.26f, s * 0.12f)
            lineTo(s * 0.74f, s * 0.12f)
            lineTo(s * 0.74f, s * 0.88f)
            lineTo(s * 0.5f, s * 0.68f)
            lineTo(s * 0.26f, s * 0.88f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = s * 0.09f, join = StrokeJoin.Round))
    }
}

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.24f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.76f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.1f
        drawLine(color, Offset(s * 0.26f, s * 0.26f), Offset(s * 0.74f, s * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.74f, s * 0.26f), Offset(s * 0.26f, s * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

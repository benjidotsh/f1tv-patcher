package sh.benji.f1tvpatcher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import sh.benji.f1tvpatcher.ui.theme.HudPalette

fun Modifier.hudBackground(): Modifier = drawBehind {
    drawRect(HudPalette.bg)

    val step = 64f
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = HudPalette.gridLine,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = HudPalette.gridLine,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += step
    }

    var ys = 0f
    while (ys < size.height) {
        drawRect(
            color = HudPalette.scanLine,
            topLeft = Offset(0f, ys),
            size = androidx.compose.ui.geometry.Size(size.width, 1f),
        )
        ys += 4f
    }

    val radius = maxOf(size.width, size.height) * 0.75f
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.45f to Color.Transparent,
                1f to HudPalette.vignette,
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = radius,
        ),
    )
}

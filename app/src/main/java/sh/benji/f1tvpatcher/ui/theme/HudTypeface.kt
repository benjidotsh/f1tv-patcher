package sh.benji.f1tvpatcher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object HudTypeface {
    val mono = FontFamily.Monospace
    val display = FontFamily.SansSerif

    val displayHeadline = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.015).sp,
    )

    val mono13 = TextStyle(
        fontFamily = mono,
        fontSize = 13.sp,
        letterSpacing = 0.14.sp,
    )

    val mono14 = TextStyle(
        fontFamily = mono,
        fontSize = 14.sp,
        letterSpacing = 0.08.sp,
    )

    val mono12Caps = TextStyle(
        fontFamily = mono,
        fontSize = 12.sp,
        letterSpacing = 0.14.sp,
    )

    val mono14Key = TextStyle(
        fontFamily = mono,
        fontSize = 14.sp,
        letterSpacing = 0.12.sp,
    )
}

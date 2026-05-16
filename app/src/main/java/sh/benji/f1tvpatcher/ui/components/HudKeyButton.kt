package sh.benji.f1tvpatcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import sh.benji.f1tvpatcher.ui.theme.HudPalette
import sh.benji.f1tvpatcher.ui.theme.HudTypeface

@Composable
fun HudKeyButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    HudKeyFrame(
        primary = primary,
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
        paddingHorizontal = 20.dp,
        paddingVertical = 13.dp,
    ) {
        BasicText(
            text = label,
            style = HudTypeface.mono14Key.copy(
                color = if (primary) Color.White else HudPalette.keyText,
            ),
        )
    }
}

@Composable
fun HudIconButton(
    @DrawableRes drawableRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HudKeyFrame(
        primary = false,
        onClick = onClick,
        modifier = modifier,
        paddingHorizontal = 12.dp,
        paddingVertical = 10.dp,
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(HudPalette.keyText),
        )
    }
}

@Composable
private fun HudKeyFrame(
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    paddingHorizontal: Dp,
    paddingVertical: Dp,
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val background = when {
        primary -> HudPalette.red
        focused -> HudPalette.keyFocused
        else -> HudPalette.keyResting
    }
    val borderColor = when {
        primary && focused -> Color.White
        primary -> HudPalette.red
        focused -> Color.White
        else -> HudPalette.keyBorder
    }
    val borderWidth = if (focused || primary) 2.dp else 1.dp

    val frame = modifier
        .then(if (primary) Modifier.shadow(4.dp, shape = RectangleShape) else Modifier)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { focused = it.isFocused }
        .focusable()
        .clickable(onClick = onClick)
        .background(background)
        .border(BorderStroke(borderWidth, borderColor))
        .padding(horizontal = paddingHorizontal, vertical = paddingVertical)

    Box(modifier = frame, contentAlignment = Alignment.Center) {
        content()
    }
}

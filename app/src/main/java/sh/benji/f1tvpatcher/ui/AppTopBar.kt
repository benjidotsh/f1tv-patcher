package sh.benji.f1tvpatcher.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sh.benji.f1tvpatcher.BuildConfig
import sh.benji.f1tvpatcher.ui.theme.HudPalette
import sh.benji.f1tvpatcher.ui.theme.HudTypeface

@Composable
fun AppTopBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 14.dp),
    ) {
        BasicText(
            text = "F1 TV Patcher",
            style = HudTypeface.mono13.copy(color = HudPalette.textDim),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = "v${BuildConfig.VERSION_NAME}",
            style = HudTypeface.mono13.copy(
                color = HudPalette.textDim,
                textAlign = TextAlign.End,
            ),
        )
    }
}

package sh.benji.f1tvpatcher.ui

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.benji.f1tvpatcher.BuildConfig
import sh.benji.f1tvpatcher.R
import sh.benji.f1tvpatcher.data.GithubHttpException
import sh.benji.f1tvpatcher.domain.DebugMocks
import sh.benji.f1tvpatcher.domain.InstalledApp
import sh.benji.f1tvpatcher.domain.UpdateStatus
import sh.benji.f1tvpatcher.ui.components.HudIconButton
import sh.benji.f1tvpatcher.ui.components.HudKeyButton
import sh.benji.f1tvpatcher.ui.theme.HudPalette
import sh.benji.f1tvpatcher.ui.theme.HudTypeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HudScreen(
    state: UiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onDebugSelect: ((UiState) -> Unit)? = null,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudPalette.bg),
    ) {
        HudTopBar(indicator = state.indicator)
        Hairline()
        HudStage(
            state = state,
            context = context,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Hairline()
        HudKeys(
            state = state,
            onCheck = onCheck,
            onInstall = onInstall,
            onUninstall = onUninstall,
            onDebugSelect = onDebugSelect,
        )
    }
}

@Composable
private fun HudTopBar(indicator: InstallIndicator) {
    val (label, color) = indicatorLabel(indicator)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 14.dp),
    ) {
        BasicText(
            text = label,
            style = HudTypeface.mono13.copy(color = color),
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

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HudPalette.border),
    )
}

@Composable
private fun HudStage(state: UiState, context: Context, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 56.dp, vertical = 24.dp),
    ) {
        val view = stageContent(state, context)

        BasicText(
            text = view.headline.uppercase(),
            style = HudTypeface.displayHeadline.copy(color = view.headlineColor),
            modifier = Modifier
                .heightIn(min = 110.dp)
                .padding(top = 6.dp, bottom = 10.dp),
        )
        BasicText(
            text = view.sub,
            style = HudTypeface.mono14.copy(color = HudPalette.textDim, lineHeight = 20.sp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(bottom = 14.dp),
        )
        if (state is UiState.Busy) {
            LinearProgressIndicator(
                color = HudPalette.red,
                trackColor = HudPalette.border,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            )
        }
        view.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = row.label,
                    style = HudTypeface.mono12Caps.copy(color = HudPalette.textSubtle),
                )
                Spacer(Modifier.width(16.dp))
                BasicText(
                    text = row.value,
                    style = HudTypeface.mono13.copy(
                        color = row.color,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Hairline()
        }
    }
}

@Composable
private fun HudKeys(
    state: UiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onDebugSelect: ((UiState) -> Unit)?,
) {
    val keys = buildKeys(state, onCheck, onInstall, onUninstall)
    val primaryRequester = remember { FocusRequester() }
    val primaryIndex = keys.indexOfFirst { it.primary }

    LaunchedEffect(primaryIndex, keys.size) {
        if (primaryIndex >= 0) primaryRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (leftKeys, rightKeys) = keys.partition { !it.alignEnd }
        leftKeys.forEachIndexed { index, key ->
            HudKeyButton(
                label = key.label,
                primary = key.primary,
                onClick = key.onClick,
                focusRequester = if (key === keys.getOrNull(primaryIndex)) primaryRequester else null,
            )
            if (index < leftKeys.lastIndex) Spacer(Modifier.width(12.dp))
        }
        Spacer(Modifier.weight(1f))
        if (BuildConfig.DEBUG && onDebugSelect != null) {
            DebugMenuButton(onSelect = onDebugSelect)
            Spacer(Modifier.width(12.dp))
        }
        rightKeys.forEachIndexed { index, key ->
            HudKeyButton(
                label = key.label,
                primary = key.primary,
                onClick = key.onClick,
                focusRequester = if (key === keys.getOrNull(primaryIndex)) primaryRequester else null,
            )
            if (index < rightKeys.lastIndex) Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun DebugMenuButton(onSelect: (UiState) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HudIconButton(drawableRes = R.drawable.ic_bug, onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(HudPalette.bg),
        ) {
            debugStates().forEach { (label, state) ->
                DropdownMenuItem(
                    text = {
                        BasicText(
                            text = label,
                            style = HudTypeface.mono13.copy(color = HudPalette.text),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(state)
                    },
                )
            }
        }
    }
}

private fun debugStates(): List<Pair<String, UiState>> = listOf(
    "CHECKING" to UiState.Busy.checking(InstallIndicator.NotInstalled),
    "INSTALLING" to UiState.Busy.installing(InstallIndicator.NotInstalled),
    "UPDATE AVAIL" to UiState.Ready(
        status = UpdateStatus.UpdateAvailable(DebugMocks.installed, DebugMocks.release),
        sizeBytes = DebugMocks.release.asset.size,
        lastCheckedAt = System.currentTimeMillis(),
    ),
    "ORIGINAL" to UiState.Ready(
        status = UpdateStatus.OriginalOrUnknownInstalled(DebugMocks.installed, DebugMocks.release),
        sizeBytes = DebugMocks.release.asset.size,
        lastCheckedAt = System.currentTimeMillis(),
    ),
    "UP TO DATE" to UiState.Ready(
        status = UpdateStatus.PatchedCurrent(DebugMocks.installed, DebugMocks.release),
        sizeBytes = DebugMocks.release.asset.size,
        lastCheckedAt = System.currentTimeMillis(),
    ),
    "NOT INSTALLED" to UiState.Ready(
        status = UpdateStatus.NotInstalled(DebugMocks.release),
        sizeBytes = DebugMocks.release.asset.size,
        lastCheckedAt = System.currentTimeMillis(),
    ),
    "ERROR · FETCH" to UiState.FetchError(
        throwable = RuntimeException("Timeout connecting to api.github.com"),
        indicator = InstallIndicator.NotInstalled,
    ),
    "ERROR · INSTALL" to UiState.InstallError(
        throwable = RuntimeException("INSTALL_FAILED_INSUFFICIENT_STORAGE"),
        indicator = InstallIndicator.Installed,
    ),
)

private data class StageView(
    val headline: String,
    val headlineColor: Color,
    val sub: String,
    val rows: List<DataRow>,
)

private data class DataRow(val label: String, val value: String, val color: Color = HudPalette.text)

private data class KeySpec(
    val label: String,
    val primary: Boolean,
    val alignEnd: Boolean = false,
    val onClick: () -> Unit,
)

private fun indicatorLabel(indicator: InstallIndicator): Pair<String, Color> = when (indicator) {
    InstallIndicator.NotInstalled -> "F1 TV · NOT INSTALLED" to HudPalette.amber
    InstallIndicator.Installed -> "F1 TV · INSTALLED" to HudPalette.textDim
    InstallIndicator.Patched -> "F1 TV · PATCHED" to HudPalette.cyan
    InstallIndicator.Original -> "F1 TV · ORIGINAL" to HudPalette.red
}

private fun stageContent(state: UiState, context: Context): StageView = when (state) {
    is UiState.Busy -> StageView(
        headline = state.headline,
        headlineColor = HudPalette.text,
        sub = state.sub,
        rows = emptyList(),
    )
    is UiState.Ready -> readyView(state, context)
    is UiState.FetchError -> fetchErrorView(state)
    is UiState.InstallError -> installErrorView(state)
}

private fun readyView(state: UiState.Ready, context: Context): StageView = when (val status = state.status) {
    is UpdateStatus.UpdateAvailable -> StageView(
        headline = "Update\navailable",
        headlineColor = HudPalette.text,
        sub = "A new version of the patch is available.",
        rows = listOfNotNull(
            DataRow("CURRENT", shortVersion(status.installed)),
            DataRow("LATEST", status.release.tagName),
            DataRow("SIZE", formatSize(context, state.sizeBytes)),
            lastCheckedRow(state.lastCheckedAt),
        ),
    )
    is UpdateStatus.OriginalOrUnknownInstalled -> StageView(
        headline = "Uninstall\nrequired",
        headlineColor = HudPalette.text,
        sub = "The original app needs to be uninstalled first. Keep in mind you will have to sign in again.",
        rows = listOfNotNull(
            DataRow("ORIGINAL", shortVersion(status.installed)),
            DataRow("PATCH", status.release.tagName),
            lastCheckedRow(state.lastCheckedAt),
        ),
    )
    is UpdateStatus.PatchedCurrent -> StageView(
        headline = "\nUp to date",
        headlineColor = HudPalette.text,
        sub = "You're on the latest version.",
        rows = listOfNotNull(
            DataRow("VERSION", status.release.tagName),
            lastCheckedRow(state.lastCheckedAt),
        ),
    )
    is UpdateStatus.NotInstalled -> StageView(
        headline = "Ready to\ninstall",
        headlineColor = HudPalette.text,
        sub = "F1 TV is not installed yet. This will directly install the patched build.",
        rows = listOfNotNull(
            DataRow("VERSION", status.release.tagName),
            DataRow("SIZE", formatSize(context, state.sizeBytes)),
            lastCheckedRow(state.lastCheckedAt),
        ),
    )
}

private fun fetchErrorView(state: UiState.FetchError): StageView {
    val t = state.throwable
    val reason = when (t) {
        is GithubHttpException -> when {
            t.status == 403 || t.status == 429 -> {
                val reset = t.rateLimitResetEpoch
                if (reset != null) {
                    "RATE LIMITED · RESETS ${SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(reset * 1000))}"
                } else "RATE LIMITED"
            }
            t.status == 404 -> "RELEASE NOT FOUND"
            t.status in 500..599 -> "GITHUB ${t.status}"
            else -> "HTTP ${t.status}"
        }
        is java.net.UnknownHostException -> "NO NETWORK / DNS"
        is java.net.SocketTimeoutException -> "TIMEOUT"
        is java.net.ConnectException -> "CONNECTION REFUSED"
        is javax.net.ssl.SSLException -> "TLS HANDSHAKE FAILED"
        is java.io.IOException -> "NETWORK FAILURE"
        else -> "UNEXPECTED ERROR"
    }
    return StageView(
        headline = "Connection\nfailed",
        headlineColor = HudPalette.red,
        sub = "Please make sure you're connected to the internet and try again later.",
        rows = listOf(
            DataRow("REASON", reason, HudPalette.red),
            DataRow("DETAIL", t.message ?: t.javaClass.simpleName),
            DataRow("HOST", "api.github.com"),
        ),
    )
}

private fun installErrorView(state: UiState.InstallError): StageView = StageView(
    headline = "Installation\nfailed",
    headlineColor = HudPalette.red,
    sub = "The installation couldn't complete. Please check that you have enough storage and try again.",
    rows = listOf(
        DataRow(
            "REASON",
            state.throwable.message ?: state.throwable.javaClass.simpleName,
            HudPalette.red,
        ),
    ),
)

private fun buildKeys(
    state: UiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
): List<KeySpec> = when (state) {
    is UiState.Busy -> emptyList()
    is UiState.Ready -> when (state.status) {
        is UpdateStatus.UpdateAvailable, is UpdateStatus.NotInstalled -> listOf(
            KeySpec("INSTALL", primary = true, onClick = onInstall),
            KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true, onClick = onCheck),
        )
        is UpdateStatus.OriginalOrUnknownInstalled -> listOf(
            KeySpec("UNINSTALL", primary = true, onClick = onUninstall),
            KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true, onClick = onCheck),
        )
        is UpdateStatus.PatchedCurrent -> listOf(
            KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true, onClick = onCheck),
        )
    }
    is UiState.FetchError -> listOf(KeySpec("RETRY", primary = true, onClick = onCheck))
    is UiState.InstallError -> listOf(KeySpec("RETRY", primary = true, onClick = onInstall))
}

private fun shortVersion(installed: InstalledApp): String =
    installed.versionName ?: "v${installed.versionCode}"

private fun formatSize(context: Context, bytes: Long): String =
    if (bytes <= 0) "—" else Formatter.formatShortFileSize(context, bytes)

private fun lastCheckedRow(epoch: Long): DataRow? {
    if (epoch == 0L) return null
    val diff = System.currentTimeMillis() - epoch
    val formatted = when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(epoch))
    }
    return DataRow("LAST CHECKED", formatted)
}

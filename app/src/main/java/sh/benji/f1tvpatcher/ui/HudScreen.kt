package sh.benji.f1tvpatcher.ui

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import sh.benji.f1tvpatcher.R
import sh.benji.f1tvpatcher.data.GithubHttpException
import sh.benji.f1tvpatcher.domain.DebugMocks
import sh.benji.f1tvpatcher.domain.InstalledApp
import sh.benji.f1tvpatcher.domain.UpdateStatus
import sh.benji.f1tvpatcher.ui.components.HudIconButton
import sh.benji.f1tvpatcher.ui.components.HudKeyButton
import sh.benji.f1tvpatcher.ui.theme.HudPalette
import sh.benji.f1tvpatcher.ui.theme.HudTypeface
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HudScreen(
    state: UiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onDebugSelect: ((UiState) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HudStage(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
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
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HudPalette.border),
    )
}

@Composable
private fun HudStage(state: UiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(horizontal = 56.dp, vertical = 24.dp),
    ) {
        val view = stageContent(state) { bytes -> formatSize(context, bytes) }

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
    val primaryKey = keys.firstOrNull { it.primary }

    LaunchedEffect(primaryKey) {
        if (primaryKey != null) primaryRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (leftKeys, rightKeys) = keys.partition { !it.alignEnd }
        KeyRow(leftKeys, primaryKey, primaryRequester)
        Spacer(Modifier.weight(1f))
        if (onDebugSelect != null) {
            DebugMenuButton(onSelect = onDebugSelect)
            Spacer(Modifier.width(12.dp))
        }
        KeyRow(rightKeys, primaryKey, primaryRequester)
    }
}

@Composable
private fun KeyRow(keys: List<KeySpec>, primaryKey: KeySpec?, primaryRequester: FocusRequester) {
    keys.forEachIndexed { index, key ->
        HudKeyButton(
            label = key.label,
            primary = key.primary,
            onClick = key.onClick,
            focusRequester = if (key === primaryKey) primaryRequester else null,
        )
        if (index < keys.lastIndex) Spacer(Modifier.width(12.dp))
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

private fun debugStates(): List<Pair<String, UiState>> {
    val now = System.currentTimeMillis()
    fun ready(status: UpdateStatus) = UiState.Ready(status, DebugMocks.release.asset.size, now)
    return listOf(
        "CHECKING FOR UPDATES" to UiState.Busy.checking(),
        "INSTALLING PATCH" to UiState.Busy.installing(),
        "UPDATE AVAILABLE" to ready(UpdateStatus.UpdateAvailable(DebugMocks.installed, DebugMocks.release)),
        "UNINSTALL REQUIRED" to ready(UpdateStatus.OriginalOrUnknownInstalled(DebugMocks.installed, DebugMocks.release)),
        "UP TO DATE" to ready(UpdateStatus.PatchedCurrent(DebugMocks.installed, DebugMocks.release)),
        "READY TO INSTALL" to ready(UpdateStatus.NotInstalled(DebugMocks.release)),
        "CONNECTION FAILED" to UiState.FetchError(
            throwable = RuntimeException("Timeout connecting to api.github.com"),
        ),
        "INSTALLATION FAILED" to UiState.InstallError(
            throwable = RuntimeException("INSTALL_FAILED_INSUFFICIENT_STORAGE"),
        ),
    )
}

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

private fun stageContent(state: UiState, formatBytes: (Long) -> String): StageView = when (state) {
    is UiState.Busy -> StageView(
        headline = state.headline,
        headlineColor = HudPalette.text,
        sub = state.sub,
        rows = emptyList(),
    )
    is UiState.Ready -> readyView(state, formatBytes)
    is UiState.FetchError -> fetchErrorView(state)
    is UiState.InstallError -> installErrorView(state)
}

private fun readyView(state: UiState.Ready, formatBytes: (Long) -> String): StageView = when (val status = state.status) {
    is UpdateStatus.UpdateAvailable -> StageView(
        headline = "Update\navailable",
        headlineColor = HudPalette.text,
        sub = "A new version of the patch is available.",
        rows = listOfNotNull(
            DataRow("CURRENT", shortVersion(status.installed)),
            DataRow("LATEST", status.release.tagName),
            DataRow("SIZE", formatBytes(state.sizeBytes)),
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
            DataRow("SIZE", formatBytes(state.sizeBytes)),
            lastCheckedRow(state.lastCheckedAt),
        ),
    )
}

private val rateLimitTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun fetchErrorView(state: UiState.FetchError): StageView {
    val t = state.throwable
    val reason = githubFailureReason(t)
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

private fun githubFailureReason(t: Throwable): String = when (t) {
    is GithubHttpException -> githubHttpReason(t)
    is java.net.UnknownHostException -> "NO NETWORK / DNS"
    is java.net.SocketTimeoutException -> "TIMEOUT"
    is java.net.ConnectException -> "CONNECTION REFUSED"
    is javax.net.ssl.SSLException -> "TLS HANDSHAKE FAILED"
    is java.io.IOException -> "NETWORK FAILURE"
    else -> "UNEXPECTED ERROR"
}

private fun githubHttpReason(t: GithubHttpException): String = when {
    t.status == 403 || t.status == 429 -> rateLimitedReason(t.rateLimitResetEpoch)
    t.status == 404 -> "RELEASE NOT FOUND"
    t.status in 500..599 -> "GITHUB ${t.status}"
    else -> "HTTP ${t.status}"
}

private fun rateLimitedReason(resetEpoch: Long?): String {
    if (resetEpoch == null) return "RATE LIMITED"
    val time = LocalTime.ofInstant(Instant.ofEpochSecond(resetEpoch), ZoneId.systemDefault())
    return "RATE LIMITED · RESETS ${time.format(rateLimitTimeFormatter)}"
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

private fun formatSize(context: android.content.Context, bytes: Long): String =
    if (bytes <= 0) "—" else Formatter.formatShortFileSize(context, bytes)

private fun lastCheckedRow(epoch: Long): DataRow? {
    if (epoch == 0L) return null
    val formatted = DateUtils.getRelativeTimeSpanString(
        epoch,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    return DataRow("LAST CHECKED", formatted)
}

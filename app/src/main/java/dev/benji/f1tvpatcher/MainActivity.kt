package dev.benji.f1tvpatcher

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var statusDot: HudStatusDot
    private lateinit var topNodeText: TextView
    private lateinit var topVersionText: TextView

    private sealed interface InstallIndicator {
        object NotInstalled : InstallIndicator
        object Installed : InstallIndicator
        object Patched : InstallIndicator
        object Original : InstallIndicator
    }

    private lateinit var progress: ProgressBar
    private lateinit var dataRows: LinearLayout

    private lateinit var statusHeadline: TextView
    private lateinit var statusSub: TextView

    private lateinit var keysRow: LinearLayout

    private var currentDownload: DownloadedApkm? = null
    private var currentStatus: UpdateStatus? = null
    private var awaitingInstallPermission = false
    private var packageReceiverRegistered = false

    private val packageEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart ?: return
            if (pkg != Constants.TARGET_PACKAGE) return
            val download = currentDownload ?: return
            val installed = InstalledAppInspector(this@MainActivity).inspect()
            val status = UpdateDecider.decide(installed, download)
            currentStatus = status
            render(status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper(this).ensureChannel()
        WeeklyUpdateScheduler.schedule(this)
        buildUi()
        maybeRequestNotificationPermission()
        checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (!packageReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                packageEventReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addDataScheme("package")
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            packageReceiverRegistered = true
        }
        maybeRequestNotificationPermission()
        val download = currentDownload
        if (download != null) {
            val installed = InstalledAppInspector(this).inspect()
            currentStatus = UpdateDecider.decide(installed, download)
        }
        if (awaitingInstallPermission) {
            awaitingInstallPermission = false
            if (InstallCoordinator(this).canRequestPackageInstalls()) {
                installPatch()
                return
            }
        }
        if (currentStatus == null && currentDownload == null) {
            checkForUpdates()
            return
        }
        render(currentStatus)
    }

    override fun onPause() {
        super.onPause()
        if (packageReceiverRegistered) {
            unregisterReceiver(packageEventReceiver)
            packageReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    // ---------------------------------------------------------------------
    // UI construction
    // ---------------------------------------------------------------------

    private fun buildUi() {
        val root = FrameLayout(this).apply { background = HudBackgroundDrawable() }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        column.addView(buildTopBar())
        column.addView(
            buildStage(),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        column.addView(buildKeysSection())

        root.addView(column)
        setContentView(root)
    }

    private fun buildTopBar(): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36), dp(14), dp(36), dp(14))
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusDot = HudStatusDot(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { marginEnd = dp(10) }
        }
        topNodeText = monoLabel("NODE · STANDBY").apply {
            setTextColor(HudPalette.textDim)
            letterSpacing = 0.14f
        }
        left.addView(statusDot)
        left.addView(topNodeText)

        topVersionText = monoLabel("v${BuildConfig.VERSION_NAME}").apply {
            setTextColor(HudPalette.textDim)
            letterSpacing = 0.14f
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        bar.addView(left)
        bar.addView(topVersionText)

        wrap.addView(bar)
        wrap.addView(horizontalHairline())
        return wrap
    }

    private fun buildStage(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(56), dp(32), dp(56), dp(24))
        }

        statusHeadline = TextView(this).apply {
            typeface = HudTypeface.display
            textSize = 56f
            setTextColor(HudPalette.text)
            isAllCaps = true
            text = "—"
            includeFontPadding = false
            setPadding(0, dp(6), 0, dp(10))
            setLineSpacing(0f, 0.9f)
            letterSpacing = -0.015f
            minLines = 2
        }
        statusSub = TextView(this).apply {
            typeface = HudTypeface.mono
            textSize = 14f
            letterSpacing = 0.08f
            setTextColor(HudPalette.textDim)
            text = ""
            setLineSpacing(0f, 1.4f)
            setPadding(0, 0, 0, dp(14))
            minHeight = dp(56)
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(HudPalette.red)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8))
                .apply { bottomMargin = dp(14) }
            visibility = View.GONE
        }
        dataRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        container.addView(statusHeadline)
        container.addView(statusSub)
        container.addView(progress)
        container.addView(dataRows)
        return container
    }

    private fun buildKeysSection(): View {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(horizontalHairline())
        keysRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(56), dp(22), dp(56), dp(22))
        }
        wrap.addView(keysRow)
        return wrap
    }

    private fun horizontalHairline(): View = View(this).apply {
        setBackgroundColor(HudPalette.border)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    }

    private fun monoLabel(initial: String): TextView = TextView(this).apply {
        typeface = HudTypeface.mono
        textSize = 13f
        text = initial
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun formatLastChecked(epoch: Long): String? {
        if (epoch == 0L) return null
        val diff = System.currentTimeMillis() - epoch
        return when {
            diff < 0 -> "Just now"
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(epoch))
        }
    }

    private fun lastCheckedRow(): DataRow? {
        val formatted = formatLastChecked(UpdateRepository(this).lastCheckedAt) ?: return null
        return DataRow("LAST CHECKED", formatted)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "—"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.ENGLISH, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.ENGLISH, "%.1f MB", mb)
        return String.format(Locale.ENGLISH, "%.2f GB", mb / 1024.0)
    }

    private fun setInstallIndicator(state: InstallIndicator) {
        val (text, color) = when (state) {
            InstallIndicator.NotInstalled -> "F1 TV · NOT INSTALLED" to HudPalette.amber
            InstallIndicator.Installed -> "F1 TV · INSTALLED" to HudPalette.textDim
            InstallIndicator.Patched -> "F1 TV · PATCHED" to HudPalette.cyan
            InstallIndicator.Original -> "F1 TV · ORIGINAL" to HudPalette.red
        }
        topNodeText.text = text
        topNodeText.setTextColor(color)
        statusDot.dotColor = color
    }

    private fun localInstallIndicator(): InstallIndicator =
        if (InstalledAppInspector(this).inspect() != null) {
            InstallIndicator.Installed
        } else {
            InstallIndicator.NotInstalled
        }

    private fun setDataRows(rows: List<DataRow>) {
        dataRows.removeAllViews()
        rows.forEach { row ->
            dataRows.addView(horizontalHairline())
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, dp(10))
            }
            item.addView(
                TextView(this).apply {
                    typeface = HudTypeface.mono
                    textSize = 12f
                    letterSpacing = 0.14f
                    setTextColor(HudPalette.textSubtle)
                    text = row.label
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { marginEnd = dp(16) }
                },
            )
            item.addView(
                TextView(this).apply {
                    typeface = HudTypeface.mono
                    textSize = 13f
                    setTextColor(row.color)
                    text = row.value
                    gravity = Gravity.END
                    layoutParams =
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            dataRows.addView(item)
        }
    }

    private fun setKeys(keys: List<KeySpec>) {
        keysRow.removeAllViews()
        val (rightKeys, leftKeys) = keys.partition { it.alignEnd }

        leftKeys.forEachIndexed { i, key ->
            val btn = hudKeyButton(this, key.label, key.primary).apply {
                setOnClickListener { key.onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginEnd = dp(12) }
            }
            keysRow.addView(btn)
            if (i == 0) btn.requestFocus()
        }

        keysRow.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            },
        )

        if (BuildConfig.DEBUG) {
            val btn = hudIconButton(this, R.drawable.ic_bug).apply {
                setOnClickListener { v -> showDebugMenu(v) }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginEnd = dp(12) }
            }
            keysRow.addView(btn)
        }

        rightKeys.forEachIndexed { i, key ->
            val btn = hudKeyButton(this, key.label, key.primary).apply {
                setOnClickListener { key.onClick() }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { if (i < rightKeys.size - 1) marginEnd = dp(12) }
            }
            keysRow.addView(btn)
            if (leftKeys.isEmpty() && i == 0) btn.requestFocus()
        }
    }

    private fun showDebugMenu(anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor, Gravity.END)
        debugStates.forEachIndexed { i, (label, _) ->
            popup.menu.add(0, i, i, label)
        }
        popup.setOnMenuItemClickListener { item ->
            debugStates[item.itemId].second()
            true
        }
        popup.show()
    }

    private val debugStates: List<Pair<String, () -> Unit>> by lazy {
        listOf(
            "CHECKING" to {
                setBusy(
                    headline = "Checking\nfor updates",
                    sub = "Fetching latest patch",
                )
            },
            "UPDATE AVAIL" to {
                renderUpdateAvailable(
                    UpdateStatus.UpdateAvailable(DebugMocks.installed, DebugMocks.release),
                )
            },
            "ORIGINAL" to {
                renderOriginalDetected(
                    UpdateStatus.OriginalOrUnknownInstalled(DebugMocks.installed, DebugMocks.release),
                )
            },
            "UP TO DATE" to {
                renderPatchedCurrent(
                    UpdateStatus.PatchedCurrent(DebugMocks.installed, DebugMocks.release),
                )
            },
            "NOT INSTALLED" to {
                val status = UpdateStatus.NotInstalled(DebugMocks.release)
                currentStatus = status
                currentDownload = null
                renderNotInstalled(status)
            },
            "ERROR" to {
                renderError(RuntimeException("Timeout connecting to api.github.com"))
            },
            "NOTIFICATION" to {
                NotificationHelper(this).notifyUpdateAvailable(DebugMocks.release)
            },
        )
    }

    private data class DataRow(
        val label: String,
        val value: String,
        val color: Int = HudPalette.text,
    )

    private data class KeySpec(
        val label: String,
        val primary: Boolean,
        val alignEnd: Boolean = false,
        val onClick: () -> Unit,
    )

    // ---------------------------------------------------------------------
    // State rendering
    // ---------------------------------------------------------------------

    private fun setBusy(headline: String, sub: String) {
        progress.visibility = View.VISIBLE
        setInstallIndicator(localInstallIndicator())
        statusHeadline.setTextColor(HudPalette.text)
        statusHeadline.text = headline
        statusSub.text = sub
        setDataRows(emptyList())
        setKeys(emptyList())
    }

    private fun render(status: UpdateStatus?) {
        progress.visibility = View.GONE
        currentStatus = status

        when (status) {
            null -> Unit
            is UpdateStatus.UpdateAvailable -> renderUpdateAvailable(status)
            is UpdateStatus.OriginalOrUnknownInstalled -> renderOriginalDetected(status)
            is UpdateStatus.PatchedCurrent -> renderPatchedCurrent(status)
            is UpdateStatus.NotInstalled -> renderNotInstalled(status)
        }
    }

    private fun renderUpdateAvailable(status: UpdateStatus.UpdateAvailable) {
        progress.visibility = View.GONE
        val release = status.release
        val installed = status.installed
        val sizeBytes = currentDownload?.release?.asset?.size ?: release.asset.size

        setInstallIndicator(InstallIndicator.Patched)

        statusHeadline.setTextColor(HudPalette.text)
        statusHeadline.text = "Update\navailable"
        statusSub.text = "A new version of the patch is available."

        setDataRows(
            listOfNotNull(
                DataRow("CURRENT", shortVersion(installed)),
                DataRow("LATEST", release.tagName),
                DataRow("SIZE", formatSize(sizeBytes)),
                lastCheckedRow(),
            ),
        )

        setKeys(
            listOf(
                KeySpec("INSTALL", true) { installPatch() },
                KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true) { checkForUpdates() },
            ),
        )
    }

    private fun renderOriginalDetected(status: UpdateStatus.OriginalOrUnknownInstalled) {
        progress.visibility = View.GONE
        val release = status.release
        val installed = status.installed

        setInstallIndicator(InstallIndicator.Original)

        statusHeadline.setTextColor(HudPalette.text)
        statusHeadline.text = "Uninstall\nrequired"
        statusSub.text =
            "The original app needs to be uninstalled first. Keep in mind you will have to sign in again."

        setDataRows(
            listOfNotNull(
                DataRow("ORIGINAL", shortVersion(installed)),
                DataRow("PATCH", release.tagName),
                lastCheckedRow(),
            ),
        )

        setKeys(
            listOf(
                KeySpec("UNINSTALL", true) { uninstallOriginal() },
                KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true) { checkForUpdates() },
            ),
        )
    }

    private fun renderPatchedCurrent(status: UpdateStatus.PatchedCurrent) {
        progress.visibility = View.GONE
        val release = status.release

        setInstallIndicator(InstallIndicator.Patched)

        statusHeadline.setTextColor(HudPalette.text)
        statusHeadline.text = "\nUp to date"
        statusSub.text = "You're on the latest version."

        setDataRows(
            listOfNotNull(
                DataRow("VERSION", release.tagName),
                lastCheckedRow(),
            ),
        )

        setKeys(listOf(KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true) { checkForUpdates() }))
    }

    private fun renderNotInstalled(status: UpdateStatus.NotInstalled) {
        progress.visibility = View.GONE
        val release = status.release

        setInstallIndicator(InstallIndicator.NotInstalled)

        statusHeadline.setTextColor(HudPalette.text)
        statusHeadline.text = "Ready to\ninstall"
        statusSub.text =
            "F1 TV is not installed yet. This will directly install the patched build."

        setDataRows(
            listOfNotNull(
                DataRow("VERSION", release.tagName),
                DataRow("SIZE", formatSize(release.asset.size)),
                lastCheckedRow(),
            ),
        )

        setKeys(
            listOf(
                KeySpec("INSTALL", true) { installPatch() },
                KeySpec("CHECK FOR UPDATES", primary = false, alignEnd = true) { checkForUpdates() },
            ),
        )
    }

    private fun renderError(throwable: Throwable) {
        progress.visibility = View.GONE

        setInstallIndicator(localInstallIndicator())

        val reason = when (throwable) {
            is GithubHttpException -> when {
                throwable.status == 403 || throwable.status == 429 -> {
                    val reset = throwable.rateLimitResetEpoch
                    if (reset != null) {
                        val fmt = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                        "RATE LIMITED · RESETS ${fmt.format(Date(reset * 1000))}"
                    } else "RATE LIMITED"
                }
                throwable.status == 404 -> "RELEASE NOT FOUND"
                throwable.status in 500..599 -> "GITHUB ${throwable.status}"
                else -> "HTTP ${throwable.status}"
            }
            is java.net.UnknownHostException -> "NO NETWORK / DNS"
            is java.net.SocketTimeoutException -> "TIMEOUT"
            is java.net.ConnectException -> "CONNECTION REFUSED"
            is javax.net.ssl.SSLException -> "TLS HANDSHAKE FAILED"
            is java.io.IOException -> "NETWORK FAILURE"
            else -> "UNEXPECTED ERROR"
        }

        statusHeadline.setTextColor(HudPalette.red)
        statusHeadline.text = "Connection\nerror"
        statusSub.text =
            "Couldn't fetch the latest patch. Please make sure you're connected to the internet and try again later."

        setDataRows(
            listOf(
                DataRow("REASON", reason, HudPalette.red),
                DataRow("DETAIL", throwable.message ?: throwable.javaClass.simpleName),
                DataRow("HOST", "api.github.com"),
            ),
        )

        setKeys(
            listOf(
                KeySpec("RETRY", true) { checkForUpdates() },
            ),
        )
    }

    private fun shortVersion(installed: InstalledApp): String =
        installed.versionName ?: "v${installed.versionCode}"

    // ---------------------------------------------------------------------
    // Business logic (unchanged from previous implementation)
    // ---------------------------------------------------------------------

    private fun checkForUpdates() {
        setBusy(
            headline = "Checking\nfor updates",
            sub = "Fetching latest patch",
        )
        executor.execute {
            runCatching {
                val releaseSource = ReleaseSource(this)
                val release = releaseSource.fetchLatestRelease()
                UpdateRepository(this).recordRelease(release)
                val downloaded = ApkmInspector(this).inspect(release, releaseSource.download(release))
                val installed = InstalledAppInspector(this).inspect()
                val status = UpdateDecider.decide(installed, downloaded)
                currentDownload = downloaded
                currentStatus = status
                status
            }.onSuccess { status ->
                main.post { render(status) }
            }.onFailure { throwable ->
                UpdateRepository(this).lastError = throwable.message
                main.post { renderError(throwable) }
            }
        }
    }

    private fun installPatch() {
        val downloaded = currentDownload ?: return checkForUpdates()
        val coordinator = InstallCoordinator(this)
        if (!coordinator.canRequestPackageInstalls()) {
            awaitingInstallPermission = true
            coordinator.openUnknownSourcesSettings()
            return
        }

        progress.visibility = View.VISIBLE
        setKeys(emptyList())

        executor.execute {
            runCatching {
                val selected = SplitSelector.select(downloaded.apkFiles, DeviceProfile.from(this))
                coordinator.install(selected)
            }.onFailure { throwable ->
                main.post { renderError(throwable) }
            }
        }
    }

    private fun uninstallOriginal() {
        InstallCoordinator(this).requestUninstall()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
    }
}

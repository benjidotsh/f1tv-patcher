package dev.benji.f1tvpatcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var checkButton: Button
    private lateinit var installButton: Button
    private lateinit var uninstallButton: Button
    private lateinit var settingsButton: Button

    private var currentDownload: DownloadedApkm? = null
    private var currentStatus: UpdateStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper(this).ensureChannel()
        WeeklyUpdateScheduler.schedule(this)
        maybeRequestNotificationPermission()
        buildUi()
        checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (maybeContinueAfterUninstall()) return
        render(currentStatus)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(72, 48, 72, 48)
            setBackgroundColor(Color.rgb(16, 20, 24))
        }

        val title = TextView(this).apply {
            text = "F1 TV Patcher"
            textSize = 34f
            setTextColor(Color.WHITE)
        }
        root.addView(title)

        statusText = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 36, 0, 8)
        }
        root.addView(statusText)

        detailText = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(183, 192, 200))
            setPadding(0, 0, 0, 28)
        }
        root.addView(detailText)

        progress = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progress)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        checkButton = tvButton("Check now") { checkForUpdates() }
        installButton = tvButton("Install patch") { installPatch() }
        uninstallButton = tvButton("Uninstall F1 TV") { uninstallOriginal() }
        settingsButton = tvButton("Allow installs") {
            InstallCoordinator(this).openUnknownSourcesSettings()
        }
        row.addView(checkButton)
        row.addView(installButton)
        row.addView(uninstallButton)
        row.addView(settingsButton)
        root.addView(row)

        setContentView(root)
        render(null)
    }

    private fun tvButton(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 16f
            isAllCaps = false
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = 18 }
        }

    private fun checkForUpdates() {
        setBusy("Checking latest F1 TV patch...")
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
            statusText.text = "Install permission needed"
            detailText.text = "Allow this app to install unknown apps, then return here."
            settingsButton.visibility = View.VISIBLE
            coordinator.openUnknownSourcesSettings()
            return
        }

        setBusy("Preparing Android install session...")
        executor.execute {
            runCatching {
                val selected = SplitSelector.select(downloaded.apkFiles, DeviceProfile.from(this))
                coordinator.install(selected)
            }.onSuccess {
                main.post {
                    progress.visibility = View.GONE
                    statusText.text = "Confirm installation"
                    detailText.text = "Android will show the required install confirmation screen."
                    checkButton.isEnabled = true
                    renderButtons()
                }
            }.onFailure { throwable ->
                main.post { renderError(throwable) }
            }
        }
    }

    private fun uninstallOriginal() {
        UpdateRepository(this).pendingInstallTag = currentDownload?.release?.tagName
        InstallCoordinator(this).requestUninstall()
    }

    private fun maybeContinueAfterUninstall(): Boolean {
        val repository = UpdateRepository(this)
        val pendingTag = repository.pendingInstallTag ?: return false
        val downloaded = currentDownload ?: return false
        if (pendingTag != downloaded.release.tagName) return false
        if (InstalledAppInspector(this).inspect() != null) return false

        repository.pendingInstallTag = null
        currentStatus = UpdateStatus.UpdateAvailable(null, downloaded.release)
        installPatch()
        return true
    }

    private fun setBusy(message: String) {
        progress.visibility = View.VISIBLE
        statusText.text = message
        detailText.text = ""
        checkButton.isEnabled = false
        installButton.visibility = View.GONE
        uninstallButton.visibility = View.GONE
        settingsButton.visibility = View.GONE
    }

    private fun render(status: UpdateStatus?) {
        progress.visibility = View.GONE
        checkButton.isEnabled = true
        currentStatus = status
        when (status) {
            null -> {
                statusText.text = "Ready"
                detailText.text = "Open the app to check the latest patched release."
            }

            is UpdateStatus.UpdateAvailable -> {
                statusText.text = "Patch available"
                detailText.text = if (status.installed == null) {
                    "${status.release.title} is ready to install."
                } else {
                    "Installed ${status.installed.versionName ?: status.installed.versionCode}; latest is ${status.release.title}."
                }
            }

            is UpdateStatus.OriginalOrUnknownInstalled -> {
                statusText.text = "Original F1 TV detected"
                detailText.text =
                    "Android requires uninstalling the current F1 TV app before installing ${status.release.title}. You will need to sign in again."
            }

            is UpdateStatus.PatchedCurrent -> {
                statusText.text = "Already up to date"
                detailText.text =
                    "Installed ${status.installed.versionName ?: status.installed.versionCode}; latest patch is ${status.release.title}."
            }

            UpdateStatus.NotInstalled -> {
                statusText.text = "F1 TV is not installed"
                detailText.text = "Check for the latest patched release to install it."
            }
        }
        renderButtons()
    }

    private fun renderButtons() {
        checkButton.visibility = View.VISIBLE
        settingsButton.visibility = if (InstallCoordinator(this).canRequestPackageInstalls()) {
            View.GONE
        } else {
            View.VISIBLE
        }
        installButton.visibility = if (currentStatus is UpdateStatus.UpdateAvailable) View.VISIBLE else View.GONE
        uninstallButton.visibility =
            if (currentStatus is UpdateStatus.OriginalOrUnknownInstalled) View.VISIBLE else View.GONE
    }

    private fun renderError(throwable: Throwable) {
        progress.visibility = View.GONE
        checkButton.isEnabled = true
        statusText.text = "Could not check patch"
        detailText.text = throwable.message ?: "Unknown error"
        renderButtons()
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

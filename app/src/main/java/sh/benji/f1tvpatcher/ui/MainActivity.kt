package sh.benji.f1tvpatcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import sh.benji.f1tvpatcher.Constants
import sh.benji.f1tvpatcher.data.UpdateRepository
import sh.benji.f1tvpatcher.install.InstallCoordinator
import sh.benji.f1tvpatcher.ui.theme.HudPalette

class MainActivity : ComponentActivity() {
    private val viewModel: UpdateViewModel by viewModels()
    private var packageReceiverRegistered = false

    private val packageEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart ?: return
            if (pkg != Constants.TARGET_PACKAGE) return
            viewModel.refreshFromInstalled()
        }
    }

    private val installResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.INSTALL_SUCCEEDED_ACTION -> viewModel.reportInstallSucceeded()
                Constants.INSTALL_FAILED_ACTION -> {
                    val message = intent.getStringExtra(Constants.EXTRA_INSTALL_FAILURE_MESSAGE)
                        ?: "Install failed"
                    viewModel.reportInstallFailure(message)
                }
            }
        }
    }

    private val unknownSourcesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        resumeInstallIfPermitted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            installResultReceiver,
            IntentFilter().apply {
                addAction(Constants.INSTALL_SUCCEEDED_ACTION)
                addAction(Constants.INSTALL_FAILED_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            val state by viewModel.state.collectAsState()
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = HudPalette.red,
                    surface = HudPalette.bg,
                    onSurface = HudPalette.text,
                    background = HudPalette.bg,
                    onBackground = HudPalette.text,
                ),
            ) {
                HudScreen(
                    state = state,
                    onCheck = { viewModel.refresh() },
                    onInstall = { requestInstall() },
                    onUninstall = { InstallCoordinator(this).requestUninstall() },
                    onDebugSelect = { viewModel.setDebugState(it) },
                )
            }
        }
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
        viewModel.refreshFromInstalled()
        resumeInstallIfPermitted()
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
        unregisterReceiver(installResultReceiver)
    }

    private fun requestInstall() {
        val coordinator = InstallCoordinator(this)
        if (!coordinator.canRequestPackageInstalls()) {
            UpdateRepository(this).awaitingInstallPermission = true
            Toast.makeText(
                this,
                "Please select F1 TV Patcher and reopen the app.",
                Toast.LENGTH_LONG,
            ).show()
            unknownSourcesLauncher.launch(coordinator.unknownSourcesIntent())
            return
        }
        viewModel.install()
    }

    private fun resumeInstallIfPermitted() {
        val repo = UpdateRepository(this)
        if (!repo.awaitingInstallPermission) return
        if (!InstallCoordinator(this).canRequestPackageInstalls()) return
        repo.awaitingInstallPermission = false
        viewModel.install()
    }
}

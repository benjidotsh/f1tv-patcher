package sh.benji.f1tvpatcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import sh.benji.f1tvpatcher.Constants
import sh.benji.f1tvpatcher.install.InstallCoordinator
import sh.benji.f1tvpatcher.ui.theme.HudPalette

class MainActivity : ComponentActivity() {
    private val viewModel: UpdateViewModel by viewModels()
    private var awaitingInstallPermission = false
    private var packageReceiverRegistered = false

    private val packageEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart ?: return
            if (pkg != Constants.TARGET_PACKAGE) return
            viewModel.refreshFromInstalled()
        }
    }

    private val installFailureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(Constants.EXTRA_INSTALL_FAILURE_MESSAGE)
                ?: "Install failed"
            viewModel.reportInstallFailure(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            installFailureReceiver,
            IntentFilter(Constants.INSTALL_FAILED_ACTION),
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
        if (awaitingInstallPermission) {
            awaitingInstallPermission = false
            if (InstallCoordinator(this).canRequestPackageInstalls()) {
                viewModel.install()
            }
        }
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
        unregisterReceiver(installFailureReceiver)
    }

    private fun requestInstall() {
        val coordinator = InstallCoordinator(this)
        if (!coordinator.canRequestPackageInstalls()) {
            awaitingInstallPermission = true
            coordinator.openUnknownSourcesSettings()
            return
        }
        viewModel.install()
    }
}

package sh.benji.f1tvpatcher

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import java.io.File

class InstallCoordinator(private val context: Context) {
    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings() {
        val uri = Uri.parse("package:${context.packageName}")
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestUninstall() {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${Constants.TARGET_PACKAGE}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            error("This device does not expose Android's uninstall confirmation screen")
        }
    }

    fun install(apkFiles: List<File>) {
        check(apkFiles.isNotEmpty()) { "No APK splits were selected for installation" }

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(Constants.TARGET_PACKAGE) }
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        session.use { installSession ->
            apkFiles.forEach { file ->
                installSession.openWrite(file.name, 0, file.length()).use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                    installSession.fsync(output)
                }
            }

            val callbackIntent = Intent(context, InstallStatusReceiver::class.java)
                .setAction(Constants.INSTALL_ACTION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callbackIntent, flags)
            installSession.commit(pendingIntent.intentSender)
        }
    }
}

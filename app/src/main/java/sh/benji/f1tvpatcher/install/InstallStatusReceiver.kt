package sh.benji.f1tvpatcher.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast
import androidx.core.content.IntentCompat
import sh.benji.f1tvpatcher.Constants

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = IntentCompat.getParcelableExtra(
                    intent, Intent.EXTRA_INTENT, Intent::class.java,
                )
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "F1 TV patch installed", Toast.LENGTH_LONG).show()
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Install failed"
                val failure = Intent(Constants.INSTALL_FAILED_ACTION)
                    .setPackage(context.packageName)
                    .putExtra(Constants.EXTRA_INSTALL_FAILURE_MESSAGE, message)
                context.sendBroadcast(failure)
            }
        }
    }
}

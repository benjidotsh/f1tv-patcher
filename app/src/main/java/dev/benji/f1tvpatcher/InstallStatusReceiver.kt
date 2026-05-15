package dev.benji.f1tvpatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(context, "F1 TV patch installed", Toast.LENGTH_LONG).show()
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Install failed"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

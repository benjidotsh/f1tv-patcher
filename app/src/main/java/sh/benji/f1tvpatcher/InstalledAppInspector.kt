package sh.benji.f1tvpatcher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class InstalledAppInspector(private val context: Context) {
    fun inspect(): InstalledApp? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val info = try {
            context.packageManager.getPackageInfo(Constants.TARGET_PACKAGE, flags)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

        return InstalledApp(
            packageName = info.packageName,
            versionName = info.versionName,
            versionCode = versionCode,
            signerDigest = Signer.sha256Digest(info),
        )
    }
}

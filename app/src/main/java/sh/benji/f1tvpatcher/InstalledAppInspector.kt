package sh.benji.f1tvpatcher

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

class InstalledAppInspector(private val context: Context) {
    fun inspect(): InstalledApp? {
        val info = try {
            context.packageManager.getPackageInfo(Constants.TARGET_PACKAGE, SIGNING_FLAGS)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        return InstalledApp(
            packageName = info.packageName,
            versionName = info.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            signerDigest = Signer.sha256Digest(info),
        )
    }
}

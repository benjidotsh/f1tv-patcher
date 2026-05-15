package sh.benji.f1tvpatcher

import android.content.pm.PackageInfo
import android.os.Build
import java.security.MessageDigest

object Signer {
    fun sha256Digest(packageInfo: PackageInfo): String? {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return null

        val first = signatures.firstOrNull() ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(first.toByteArray())
        return digest.joinToString(":") { byte -> "%02X".format(byte) }
    }
}

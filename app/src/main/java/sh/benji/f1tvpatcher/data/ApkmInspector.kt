package sh.benji.f1tvpatcher.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import sh.benji.f1tvpatcher.domain.ApkMetadata
import sh.benji.f1tvpatcher.domain.DownloadedApkm
import sh.benji.f1tvpatcher.domain.ReleaseInfo
import sh.benji.f1tvpatcher.domain.SIGNING_FLAGS
import sh.benji.f1tvpatcher.domain.Signer
import sh.benji.f1tvpatcher.domain.safeFileName
import java.io.File
import java.util.zip.ZipFile

private const val EXTRACTION_MARKER = ".extracted"

class ApkmInspector(private val context: Context) {
    fun inspect(release: ReleaseInfo, apkmFile: File): DownloadedApkm {
        val extractedDir = File(context.cacheDir, "apkm/${release.tagName.safeFileName()}")
        val marker = File(extractedDir, EXTRACTION_MARKER)
        val apks = if (marker.isFile) {
            extractedDir.listApks()
        } else {
            extractedDir.deleteRecursively()
            extractedDir.mkdirs()
            extractTo(apkmFile, extractedDir).also { marker.createNewFile() }
        }

        val base = apks.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
            ?: error("Downloaded APKM does not contain base.apk")

        val packageInfo = context.packageManager.getPackageArchiveInfo(base.absolutePath, SIGNING_FLAGS)
            ?: error("Could not parse base.apk from downloaded APKM")

        return DownloadedApkm(
            release = release,
            metadata = ApkMetadata(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                signerDigest = Signer.sha256Digest(packageInfo),
            ),
            apkFiles = apks,
        )
    }

    private fun extractTo(apkmFile: File, extractedDir: File): List<File> {
        val extracted = mutableListOf<File>()
        ZipFile(apkmFile).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .forEach { entry ->
                    val target = File(extractedDir, File(entry.name).name)
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    extracted += target
                }
        }
        return extracted
    }

    private fun File.listApks(): List<File> =
        listFiles { file -> file.extension.equals("apk", ignoreCase = true) }?.toList().orEmpty()
}

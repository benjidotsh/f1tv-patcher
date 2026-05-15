package dev.benji.f1tvpatcher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

class ApkmInspector(private val context: Context) {
    fun inspect(release: ReleaseInfo, apkmFile: File): DownloadedApkm {
        val extractedDir = File(context.cacheDir, "apkm/${release.tagName.safeFileName()}").apply {
            deleteRecursively()
            mkdirs()
        }
        ZipFile(apkmFile).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .forEach { entry ->
                    val target = File(extractedDir, File(entry.name).name)
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
        }

        val apks = extractedDir.listFiles { file -> file.extension.equals("apk", ignoreCase = true) }
            ?.toList()
            .orEmpty()
        val base = apks.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
            ?: error("Downloaded APKM does not contain base.apk")

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val packageInfo = context.packageManager.getPackageArchiveInfo(base.absolutePath, flags)
            ?: error("Could not parse base.apk from downloaded APKM")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        return DownloadedApkm(
            release = release,
            apkmFile = apkmFile,
            extractedDir = extractedDir,
            metadata = ApkMetadata(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName,
                versionCode = versionCode,
                signerDigest = Signer.sha256Digest(packageInfo),
            ),
            apkFiles = apks,
        )
    }
}

private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

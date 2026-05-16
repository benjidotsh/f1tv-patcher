package sh.benji.f1tvpatcher.domain

import android.util.DisplayMetrics
import sh.benji.f1tvpatcher.Constants
import java.io.File
import kotlin.math.abs

object SplitSelector {
    private val configSplitPrefixes = listOf(
        "config.",
        "split_config.",
        "${Constants.TARGET_PACKAGE}.config.",
    )

    private val abiTokens = mapOf(
        "arm64-v8a" to listOf("arm64_v8a", "arm64-v8a"),
        "armeabi-v7a" to listOf("armeabi_v7a", "armeabi-v7a"),
        "x86_64" to listOf("x86_64", "x86-64"),
        "x86" to listOf("x86"),
    )

    private val densities = listOf(
        "ldpi" to DisplayMetrics.DENSITY_LOW,
        "mdpi" to DisplayMetrics.DENSITY_MEDIUM,
        "hdpi" to DisplayMetrics.DENSITY_HIGH,
        "xhdpi" to DisplayMetrics.DENSITY_XHIGH,
        "xxhdpi" to DisplayMetrics.DENSITY_XXHIGH,
        "xxxhdpi" to DisplayMetrics.DENSITY_XXXHIGH,
    )

    fun select(files: List<File>, profile: DeviceProfile): List<File> {
        val apks = files.filter { it.extension.equals("apk", ignoreCase = true) }
        val selected = linkedSetOf<File>()
        apks.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }?.let(selected::add)
        apks.filter { file -> !file.name.equals("base.apk", ignoreCase = true) && !file.isConfigSplit() }
            .forEach(selected::add)

        findAbiSplit(apks, profile.supportedAbis)?.let(selected::add)
        findLanguageSplit(apks, profile.language)?.let(selected::add)
        findDensitySplit(apks, profile.densityDpi)?.let(selected::add)

        apks.filter { it.name.contains("nodpi", ignoreCase = true) }.forEach(selected::add)
        return selected.toList()
    }

    private fun findAbiSplit(files: List<File>, abis: List<String>): File? {
        for (abi in abis) {
            val tokens = abiTokens[abi].orEmpty()
            files.firstOrNull { file -> tokens.any { token -> file.hasSplitToken(token) } }
                ?.let { return it }
        }
        return null
    }

    private fun findLanguageSplit(files: List<File>, language: String): File? =
        files.firstOrNull { file -> file.hasSplitToken(language) }
            ?: files.firstOrNull { file -> file.hasSplitToken("en") }

    private fun findDensitySplit(files: List<File>, densityDpi: Int): File? {
        val available = densities.mapNotNull { (bucket, dpi) ->
            files.firstOrNull { file -> file.hasSplitToken(bucket) }?.let { file -> file to dpi }
        }
        return available.minByOrNull { (_, dpi) -> abs(dpi - densityDpi) }?.first
    }

    private fun File.isConfigSplit(): Boolean =
        configSplitPrefixes.any { prefix -> name.startsWith(prefix, ignoreCase = true) }

    private fun File.hasSplitToken(token: String): Boolean {
        val bareName = name.removeSuffix(".apk")
        return bareName.equals(token, ignoreCase = true) ||
            bareName.endsWith(".$token", ignoreCase = true) ||
            bareName.endsWith("_$token", ignoreCase = true) ||
            bareName.contains(".$token.", ignoreCase = true) ||
            bareName.contains("_$token.", ignoreCase = true)
    }
}

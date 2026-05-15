package dev.benji.f1tvpatcher

import java.io.File
import kotlin.math.abs

object SplitSelector {
    private val abiTokens = mapOf(
        "arm64-v8a" to listOf("arm64_v8a", "arm64-v8a"),
        "armeabi-v7a" to listOf("armeabi_v7a", "armeabi-v7a"),
        "x86_64" to listOf("x86_64", "x86-64"),
        "x86" to listOf("x86"),
    )

    private val densities = listOf(
        "ldpi" to 120,
        "mdpi" to 160,
        "hdpi" to 240,
        "xhdpi" to 320,
        "xxhdpi" to 480,
        "xxxhdpi" to 640,
    )

    fun select(files: List<File>, profile: DeviceProfile): List<File> {
        val apks = files.filter { it.extension.equals("apk", ignoreCase = true) }
        val selected = linkedSetOf<File>()
        apks.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }?.let(selected::add)

        findAbiSplit(apks, profile.supportedAbis)?.let(selected::add)
        findLanguageSplit(apks, profile.language)?.let(selected::add)
        findDensitySplit(apks, profile.densityDpi)?.let(selected::add)

        apks.filter { it.name.contains("nodpi", ignoreCase = true) }.forEach(selected::add)
        return selected.toList()
    }

    private fun findAbiSplit(files: List<File>, abis: List<String>): File? {
        for (abi in abis) {
            val tokens = abiTokens[abi].orEmpty()
            files.firstOrNull { file -> tokens.any { token -> file.name.contains(token, true) } }
                ?.let { return it }
        }
        return null
    }

    private fun findLanguageSplit(files: List<File>, language: String): File? =
        files.firstOrNull { file ->
            file.name.contains(".$language.", true) ||
                file.name.contains("_$language.", true) ||
                file.name.contains("config.$language", true)
        } ?: files.firstOrNull { file -> file.name.contains(".en.", true) || file.name.contains("config.en", true) }

    private fun findDensitySplit(files: List<File>, densityDpi: Int): File? {
        val target = densities.minBy { (_, dpi) -> abs(dpi - densityDpi) }.first
        return files.firstOrNull { file -> file.name.contains(target, ignoreCase = true) }
    }
}

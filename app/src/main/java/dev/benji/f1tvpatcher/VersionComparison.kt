package dev.benji.f1tvpatcher

object VersionComparison {
    fun isInstalledAtLeastRelease(installedVersionCode: Long, releaseVersionCode: Long): Boolean =
        installedVersionCode >= releaseVersionCode
}

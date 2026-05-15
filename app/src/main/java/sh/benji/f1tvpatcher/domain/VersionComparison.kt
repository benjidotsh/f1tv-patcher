package sh.benji.f1tvpatcher.domain

object VersionComparison {
    fun isInstalledAtLeastRelease(installedVersionCode: Long, releaseVersionCode: Long): Boolean =
        installedVersionCode >= releaseVersionCode
}

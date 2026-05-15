package sh.benji.f1tvpatcher

object UpdateDecider {
    fun decide(installed: InstalledApp?, downloaded: DownloadedApkm): UpdateStatus {
        check(downloaded.metadata.packageName == Constants.TARGET_PACKAGE) {
            "Release package is ${downloaded.metadata.packageName}, expected ${Constants.TARGET_PACKAGE}"
        }

        if (installed == null) return UpdateStatus.NotInstalled(downloaded.release)

        val sameSigner = installed.signerDigest != null &&
            installed.signerDigest == downloaded.metadata.signerDigest

        if (!sameSigner) {
            return UpdateStatus.OriginalOrUnknownInstalled(installed, downloaded.release)
        }

        return if (VersionComparison.isInstalledAtLeastRelease(
                installed.versionCode,
                downloaded.metadata.versionCode,
            )
        ) {
            UpdateStatus.PatchedCurrent(installed, downloaded.release)
        } else {
            UpdateStatus.UpdateAvailable(installed, downloaded.release)
        }
    }
}

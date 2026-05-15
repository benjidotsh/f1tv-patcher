package sh.benji.f1tvpatcher.domain

sealed interface UpdateStatus {
    data class NotInstalled(val release: ReleaseInfo) : UpdateStatus
    data class OriginalOrUnknownInstalled(val installed: InstalledApp, val release: ReleaseInfo) :
        UpdateStatus
    data class PatchedCurrent(val installed: InstalledApp, val release: ReleaseInfo) : UpdateStatus
    data class UpdateAvailable(val installed: InstalledApp, val release: ReleaseInfo) : UpdateStatus
}

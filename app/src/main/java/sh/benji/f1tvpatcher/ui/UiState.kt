package sh.benji.f1tvpatcher.ui

import sh.benji.f1tvpatcher.domain.UpdateStatus

enum class InstallIndicator { NotInstalled, Installed, Patched, Original }

sealed interface UiState {
    val indicator: InstallIndicator

    data class Busy(
        val headline: String,
        val sub: String,
        override val indicator: InstallIndicator,
    ) : UiState {
        companion object {
            fun checking(indicator: InstallIndicator) =
                Busy("Checking\nfor updates", "Fetching latest patch", indicator)

            fun installing(indicator: InstallIndicator) =
                Busy("Installing\npatch", "This can take a few seconds.", indicator)
        }
    }

    data class Ready(
        val status: UpdateStatus,
        val sizeBytes: Long,
        val lastCheckedAt: Long,
    ) : UiState {
        override val indicator: InstallIndicator = when (status) {
            is UpdateStatus.NotInstalled -> InstallIndicator.NotInstalled
            is UpdateStatus.OriginalOrUnknownInstalled -> InstallIndicator.Original
            is UpdateStatus.PatchedCurrent,
            is UpdateStatus.UpdateAvailable -> InstallIndicator.Patched
        }
    }

    data class FetchError(
        val throwable: Throwable,
        override val indicator: InstallIndicator,
    ) : UiState

    data class InstallError(
        val throwable: Throwable,
        override val indicator: InstallIndicator,
    ) : UiState
}

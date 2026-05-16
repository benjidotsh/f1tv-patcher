package sh.benji.f1tvpatcher.ui

import sh.benji.f1tvpatcher.domain.UpdateStatus

sealed interface UiState {
    data class Busy(
        val headline: String,
        val sub: String,
    ) : UiState {
        companion object {
            fun checking() = Busy("Checking\nfor updates", "Fetching latest patch")
            fun installing() = Busy("Installing\npatch", "This can take a few seconds.")
        }
    }

    data class Ready(
        val status: UpdateStatus,
        val sizeBytes: Long,
        val lastCheckedAt: Long,
    ) : UiState

    data class FetchError(val throwable: Throwable) : UiState

    data class InstallError(val throwable: Throwable) : UiState
}

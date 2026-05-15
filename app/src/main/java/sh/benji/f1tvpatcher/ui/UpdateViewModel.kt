package sh.benji.f1tvpatcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sh.benji.f1tvpatcher.data.ApkmInspector
import sh.benji.f1tvpatcher.data.InstalledAppInspector
import sh.benji.f1tvpatcher.data.ReleaseSource
import sh.benji.f1tvpatcher.data.UpdateRepository
import sh.benji.f1tvpatcher.domain.DeviceProfile
import sh.benji.f1tvpatcher.domain.DownloadedApkm
import sh.benji.f1tvpatcher.domain.SplitSelector
import sh.benji.f1tvpatcher.domain.UpdateDecider
import sh.benji.f1tvpatcher.install.InstallCoordinator

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app

    private val _state = MutableStateFlow<UiState>(
        UiState.Busy("Checking\nfor updates", "Fetching latest patch", InstallIndicator.NotInstalled),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var currentDownload: DownloadedApkm? = null
    private var checkJob: Job? = null
    private var installJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (checkJob?.isActive == true) return
        _state.value = UiState.Busy("Checking\nfor updates", "Fetching latest patch", localIndicator())
        checkJob = viewModelScope.launch {
            runCatching {
                val source = ReleaseSource(context)
                val release = source.fetchLatestRelease()
                UpdateRepository(context).recordRelease(release)
                val downloaded = ApkmInspector(context).inspect(release, source.download(release))
                val installed = InstalledAppInspector(context).inspect()
                val status = UpdateDecider.decide(installed, downloaded)
                currentDownload = downloaded
                status
            }.onSuccess { status ->
                _state.value = UiState.Ready(
                    status = status,
                    sizeBytes = currentDownload?.release?.asset?.size ?: 0L,
                    lastCheckedAt = UpdateRepository(context).lastCheckedAt,
                )
            }.onFailure { t ->
                UpdateRepository(context).lastError = t.message
                _state.value = UiState.FetchError(t, localIndicator())
            }
        }
    }

    fun install() {
        val download = currentDownload ?: return refresh()
        if (installJob?.isActive == true) return
        _state.value = UiState.Busy("Installing", "Committing install session", localIndicator())
        installJob = viewModelScope.launch {
            runCatching {
                val selected = SplitSelector.select(download.apkFiles, DeviceProfile.from(context))
                InstallCoordinator(context).install(selected)
            }.onSuccess {
                refreshFromInstalled()
            }.onFailure { t ->
                _state.value = UiState.InstallError(t, localIndicator())
            }
        }
    }

    fun reportInstallFailure(message: String) {
        _state.value = UiState.InstallError(RuntimeException(message), localIndicator())
    }

    fun refreshFromInstalled() {
        val download = currentDownload ?: return
        val installed = InstalledAppInspector(context).inspect()
        val status = UpdateDecider.decide(installed, download)
        _state.value = UiState.Ready(
            status = status,
            sizeBytes = download.release.asset.size,
            lastCheckedAt = UpdateRepository(context).lastCheckedAt,
        )
    }

    fun setDebugState(state: UiState) {
        _state.value = state
    }

    private fun localIndicator(): InstallIndicator =
        if (InstalledAppInspector(context).inspect() != null) {
            InstallIndicator.Installed
        } else {
            InstallIndicator.NotInstalled
        }
}

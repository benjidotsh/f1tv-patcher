package sh.benji.f1tvpatcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        UiState.Busy.checking(InstallIndicator.NotInstalled),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var currentDownload: DownloadedApkm? = null
    private var checkJob: Job? = null
    private var installJob: Job? = null
    private var installPending: Boolean = false
    private var installAfterCheck: Boolean = false

    init {
        refresh()
    }

    fun refresh() {
        if (checkJob?.isActive == true) return
        _state.value = UiState.Busy.checking(localIndicator())
        checkJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val source = ReleaseSource(context)
                    val release = source.fetchLatestRelease()
                    UpdateRepository(context).recordRelease(release)
                    val downloaded = ApkmInspector(context).inspect(release, source.download(release))
                    val installed = InstalledAppInspector(context).inspect()
                    val status = UpdateDecider.decide(installed, downloaded)
                    currentDownload = downloaded
                    status
                }
            }.onSuccess { status ->
                _state.value = UiState.Ready(
                    status = status,
                    sizeBytes = currentDownload?.release?.asset?.size ?: 0L,
                    lastCheckedAt = UpdateRepository(context).lastCheckedAt,
                )
                if (installAfterCheck) {
                    installAfterCheck = false
                    install()
                }
            }.onFailure { t ->
                installAfterCheck = false
                UpdateRepository(context).lastError = t.message
                _state.value = UiState.FetchError(t, localIndicator())
            }
        }
    }

    fun install() {
        val download = currentDownload
        if (download == null) {
            installAfterCheck = true
            if (checkJob?.isActive != true) refresh()
            return
        }
        if (installJob?.isActive == true) return
        installPending = true
        _state.value = UiState.Busy.installing(localIndicator())
        installJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val selected = SplitSelector.select(download.apkFiles, DeviceProfile.from(context))
                    InstallCoordinator(context).install(selected)
                }
            }.onFailure { t ->
                installPending = false
                _state.value = UiState.InstallError(t, localIndicator())
            }
        }
    }

    fun reportInstallFailure(message: String) {
        installPending = false
        _state.value = UiState.InstallError(RuntimeException(message), localIndicator())
    }

    fun reportInstallSucceeded() {
        installPending = false
        refreshFromInstalled()
    }

    fun refreshFromInstalled() {
        if (checkJob?.isActive == true || installJob?.isActive == true) return
        if (installPending) return
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

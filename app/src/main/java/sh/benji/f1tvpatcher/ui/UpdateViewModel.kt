package sh.benji.f1tvpatcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val repository = UpdateRepository(app)
    private val releaseSource = ReleaseSource(app)
    private val installedInspector = InstalledAppInspector(app)
    private val apkmInspector = ApkmInspector(app)
    private val installCoordinator = InstallCoordinator(app)

    private val _state = MutableStateFlow<UiState>(UiState.Busy.checking())
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
        _state.value = UiState.Busy.checking()
        checkJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val installedDeferred = async { installedInspector.inspect() }
                        val release = releaseSource.fetchLatestRelease()
                        repository.markChecked()
                        val downloaded = apkmInspector.inspect(release, releaseSource.download(release))
                        val status = UpdateDecider.decide(installedDeferred.await(), downloaded)
                        currentDownload = downloaded
                        status
                    }
                }
            }.onSuccess { status ->
                _state.value = UiState.Ready(
                    status = status,
                    sizeBytes = currentDownload?.release?.asset?.size ?: 0L,
                    lastCheckedAt = repository.lastCheckedAt,
                )
                if (installAfterCheck) {
                    installAfterCheck = false
                    install()
                }
            }.onFailure { t ->
                installAfterCheck = false
                _state.value = UiState.FetchError(t)
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
        _state.value = UiState.Busy.installing()
        installJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val selected = SplitSelector.select(download.apkFiles, DeviceProfile.from(context))
                    installCoordinator.install(selected)
                }
            }.onFailure { t ->
                installPending = false
                _state.value = UiState.InstallError(t)
            }
        }
    }

    fun uninstall() {
        installCoordinator.requestUninstall()
    }

    fun reportInstallFailure(message: String) {
        installPending = false
        _state.value = UiState.InstallError(RuntimeException(message))
    }

    fun reportInstallSucceeded() {
        installPending = false
        refreshFromInstalled()
    }

    fun refreshFromInstalled() {
        if (checkJob?.isActive == true || installJob?.isActive == true) return
        if (installPending) return
        val download = currentDownload ?: return
        val status = UpdateDecider.decide(installedInspector.inspect(), download)
        _state.value = UiState.Ready(
            status = status,
            sizeBytes = download.release.asset.size,
            lastCheckedAt = repository.lastCheckedAt,
        )
    }

    fun setDebugState(state: UiState) {
        _state.value = state
    }
}

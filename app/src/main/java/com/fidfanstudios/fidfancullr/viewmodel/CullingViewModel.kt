package com.fidfanstudios.fidfancullr.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fidfanstudios.fidfancullr.data.AppSettings
import com.fidfanstudios.fidfancullr.data.ExifData
import com.fidfanstudios.fidfancullr.data.PhotoGroup
import com.fidfanstudios.fidfancullr.data.SettingsRepository
import com.fidfanstudios.fidfancullr.data.SortDestination
import com.fidfanstudios.fidfancullr.util.ExifReader
import com.fidfanstudios.fidfancullr.util.FileMover
import com.fidfanstudios.fidfancullr.util.PhotoGrouper
import com.fidfanstudios.fidfancullr.util.PreviewLoader
import com.fidfanstudios.fidfancullr.util.PreviewResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PreviewUiState {
    object Loading : PreviewUiState()
    data class Loaded(val bitmap: android.graphics.Bitmap) : PreviewUiState()
    object NoPreview : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

data class CullingUiState(
    val isLoading: Boolean = true,
    val groups: List<PhotoGroup> = emptyList(),
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val currentExif: ExifData? = null,
    val previewState: PreviewUiState = PreviewUiState.Loading,
    val isComplete: Boolean = false
) {
    val currentGroup: PhotoGroup?
        get() = groups.getOrNull(currentIndex)
}

class CullingViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(CullingUiState())
    val uiState: StateFlow<CullingUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())
    private val _settings = settings as MutableStateFlow<AppSettings>

    // Preview requests are downsampled to roughly the device's screen size
    // (never full RAW/embedded-preview resolution) to keep memory bounded.
    private val reqWidth: Int
    private val reqHeight: Int

    private var previewLoadJob: Job? = null

    init {
        val metrics = application.resources.displayMetrics
        reqWidth = metrics.widthPixels
        reqHeight = metrics.heightPixels

        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _settings.value = s
                if (s.inboxUri != null && _uiState.value.groups.isEmpty() && !_uiState.value.isComplete) {
                    loadGroups(Uri.parse(s.inboxUri))
                }
            }
        }
    }

    fun loadGroups(inboxUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isComplete = false)
            val groups = withContext(Dispatchers.IO) {
                PhotoGrouper.scan(getApplication(), inboxUri)
            }
            _uiState.value = CullingUiState(
                isLoading = false,
                groups = groups,
                currentIndex = 0,
                totalCount = groups.size,
                sortedCount = 0,
                isComplete = groups.isEmpty()
            )
            loadCurrentPreviewAndExif()
        }
    }

    private fun loadCurrentPreviewAndExif() {
        previewLoadJob?.cancel()

        val group = _uiState.value.currentGroup ?: run {
            _uiState.value = _uiState.value.copy(currentExif = null, previewState = PreviewUiState.NoPreview)
            return
        }

        _uiState.value = _uiState.value.copy(previewState = PreviewUiState.Loading, currentExif = null)

        previewLoadJob = viewModelScope.launch {
            val exifDeferred = async(Dispatchers.IO) { ExifReader.read(getApplication(), group.primaryFile) }
            val previewResult = PreviewLoader.load(getApplication(), group.primaryFile, reqWidth, reqHeight)

            _uiState.value = _uiState.value.copy(
                currentExif = exifDeferred.await(),
                previewState = when (previewResult) {
                    is PreviewResult.Success -> PreviewUiState.Loaded(previewResult.bitmap)
                    is PreviewResult.NoPreviewAvailable -> PreviewUiState.NoPreview
                    is PreviewResult.Failed -> PreviewUiState.Error(previewResult.reason)
                }
            )

            preloadAdjacent()
        }
    }

    private fun preloadAdjacent() {
        val state = _uiState.value
        val nextGroup = state.groups.getOrNull(state.currentIndex + 1)
        val prevGroup = state.groups.getOrNull(state.currentIndex - 1)

        viewModelScope.launch(Dispatchers.IO) {
            nextGroup?.let { PreviewLoader.preload(getApplication(), it.primaryFile, reqWidth, reqHeight) }
            prevGroup?.let { PreviewLoader.preload(getApplication(), it.primaryFile, reqWidth, reqHeight) }
        }
    }

    fun sortCurrentGroup(destination: SortDestination) {
        val state = _uiState.value
        val group = state.currentGroup ?: return
        val inboxUri = _settings.value.inboxUri?.let { Uri.parse(it) } ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                FileMover.moveGroupTo(getApplication(), inboxUri, group, destination.folderName)
            }
            advance()
        }
    }

    fun skipCurrentGroup() {
        advance()
    }

    fun retryCurrentPreview() {
        loadCurrentPreviewAndExif()
    }

    private fun advance() {
        val state = _uiState.value
        val remaining = state.groups.filterIndexed { index, _ -> index != state.currentIndex }
        val nextIndex = if (state.currentIndex >= remaining.size) 0 else state.currentIndex
        _uiState.value = state.copy(
            groups = remaining,
            currentIndex = nextIndex,
            sortedCount = state.sortedCount + 1,
            isComplete = remaining.isEmpty()
        )
        loadCurrentPreviewAndExif()
    }

    fun updateDestinations(destinations: List<SortDestination>) {
        viewModelScope.launch { settingsRepository.setDestinations(destinations) }
    }

    fun setInbox(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.setInboxUri(uri.toString())
            loadGroups(uri)
        }
    }
}

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CullingUiState(
    val isLoading: Boolean = true,
    val groups: List<PhotoGroup> = emptyList(),
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val sortedCount: Int = 0,
    val currentExif: ExifData? = null,
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

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _settings.value = s
                if (s.inboxUri != null && _uiState.value.groups.isEmpty()) {
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
            loadCurrentExif()
        }
    }

    private fun loadCurrentExif() {
        val group = _uiState.value.currentGroup ?: run {
            _uiState.value = _uiState.value.copy(currentExif = null)
            return
        }
        viewModelScope.launch {
            val exif = withContext(Dispatchers.IO) {
                ExifReader.read(getApplication(), group.primaryFile)
            }
            _uiState.value = _uiState.value.copy(currentExif = exif)
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
        loadCurrentExif()
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

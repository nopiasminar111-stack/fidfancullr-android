package com.fidfanstudios.fidfancullr.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.AppSettings
import com.fidfanstudios.fidfancullr.data.SettingsRepository
import com.fidfanstudios.fidfancullr.data.SortDestination
import com.fidfanstudios.fidfancullr.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // Null until the first real value has been read from disk, so the UI can
    // show a brief loading state instead of flashing onboarding incorrectly.
    val settings: StateFlow<AppSettings?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setInbox(uri: Uri) = viewModelScope.launch { repository.setInboxUri(uri.toString()) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setAccent(accent: AccentPalette) = viewModelScope.launch { repository.setAccent(accent) }
    fun setLanguage(language: String) = viewModelScope.launch { repository.setLanguage(language) }
    fun setDestinations(destinations: List<SortDestination>) =
        viewModelScope.launch { repository.setDestinations(destinations) }
    fun setOnboardingComplete(complete: Boolean) =
        viewModelScope.launch { repository.setOnboardingComplete(complete) }
}

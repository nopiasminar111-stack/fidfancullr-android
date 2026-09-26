package com.fidfanstudios.fidfancullr.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.SortDestination
import com.fidfanstudios.fidfancullr.data.ThemeMode
import com.fidfanstudios.fidfancullr.viewmodel.SettingsViewModel

private val SUPPORTED_LANGUAGES = listOf(
    "en" to "English", "id" to "Bahasa Indonesia", "ja" to "日本語",
    "es" to "Español", "zh" to "中文"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: com.fidfanstudios.fidfancullr.data.AppSettings()
    var destinations by remember(settings.destinations) { mutableStateOf(settings.destinations) }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            viewModel.getApplication<android.app.Application>().contentResolver
                .takePersistableUriPermission(uri, flags)
            viewModel.setInbox(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionTitle(stringResource(R.string.reselect_inbox))
                Button(onClick = { pickFolderLauncher.launch(null) }) {
                    Text(stringResource(R.string.reselect_inbox))
                }
            }

            item {
                SectionTitle(stringResource(R.string.theme))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = { Text(mode.name) }
                        )
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.accent))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPalette.values().forEach { palette ->
                        FilterChip(
                            selected = settings.accentPalette == palette,
                            onClick = { viewModel.setAccent(palette) },
                            label = { Text(palette.name.replace("_", " ")) }
                        )
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.language))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUPPORTED_LANGUAGES.forEach { (code, label) ->
                        FilterChip(
                            selected = settings.language == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                SectionTitle(stringResource(R.string.folder_names))
                Text(stringResource(R.string.sorting_keys), style = MaterialTheme.typography.bodyMedium)
            }

            items(destinations) { destination ->
                DestinationEditorRow(
                    destination = destination,
                    onChanged = { updated ->
                        destinations = destinations.map { if (it.id == updated.id) updated else it }
                        viewModel.setDestinations(destinations)
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DestinationEditorRow(
    destination: SortDestination,
    onChanged: (SortDestination) -> Unit
) {
    var folderName by remember(destination.id) { mutableStateOf(destination.folderName) }
    var gestureKey by remember(destination.id) { mutableStateOf(destination.gestureKey) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = folderName,
            onValueChange = {
                folderName = it
                onChanged(destination.copy(folderName = it))
            },
            label = { Text(destination.label) },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = gestureKey,
            onValueChange = {
                gestureKey = it
                onChanged(destination.copy(gestureKey = it))
            },
            label = { Text("Key") },
            modifier = Modifier.width(72.dp)
        )
    }
}

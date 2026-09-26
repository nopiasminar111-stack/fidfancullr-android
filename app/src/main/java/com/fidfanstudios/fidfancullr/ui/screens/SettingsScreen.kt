package com.fidfanstudios.fidfancullr.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.AppSettings
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
    val settings = settingsState ?: AppSettings()
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
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(icon = Icons.Filled.Folder, title = stringResource(R.string.reselect_inbox)) {
                    FilledTonalButton(
                        onClick = { pickFolderLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.reselect_inbox))
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Palette, title = stringResource(R.string.theme)) {
                    ChipGroup {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = { Text(themeModeLabel(mode)) }
                            )
                        }
                    }

                    // Dynamic color is only meaningful on Android 12+. On
                    // older versions we don't show the toggle at all (it
                    // would do nothing there), but the Accent section below
                    // is ALWAYS present regardless of dynamic color or OS
                    // version — it's disabled rather than hidden, so the
                    // number of visible Settings items never changes.
                    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

                    if (dynamicColorSupported) {
                        Spacer(Modifier.height(12.dp))
                        SettingsSwitchRow(
                            title = stringResource(R.string.dynamic_color),
                            subtitle = stringResource(R.string.dynamic_color_desc),
                            checked = settings.useDynamicColor,
                            onCheckedChange = viewModel::setUseDynamicColor
                        )
                    }

                    val accentEnabled = !dynamicColorSupported || !settings.useDynamicColor

                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.accent),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (accentEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    ChipGroup {
                        AccentPalette.values().forEach { palette ->
                            FilterChip(
                                selected = accentEnabled && settings.accentPalette == palette,
                                enabled = accentEnabled,
                                onClick = { viewModel.setAccent(palette) },
                                label = { Text(palette.name.replace("_", " ")) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(icon = Icons.Filled.Language, title = stringResource(R.string.language)) {
                    ChipGroup {
                        SUPPORTED_LANGUAGES.forEach { (code, label) ->
                            FilterChip(
                                selected = settings.language == code,
                                onClick = { viewModel.setLanguage(code) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(
                    icon = Icons.Filled.SwapVert,
                    title = stringResource(R.string.folder_names),
                    subtitle = stringResource(R.string.sorting_keys)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        destinations.forEach { destination ->
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
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> stringResource(R.string.light)
    ThemeMode.DARK -> stringResource(R.string.dark)
    ThemeMode.SYSTEM -> stringResource(R.string.system)
}

@Composable
private fun ChipGroup(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

/**
 * A single grouped card with an icon + title header. This replaces the old
 * flat, ungrouped rows of chips that made Settings feel cluttered — each
 * concern now lives in its own clearly separated card, M3-Expressive style.
 */
@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
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
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = gestureKey,
            onValueChange = {
                gestureKey = it
                onChanged(destination.copy(gestureKey = it))
            },
            label = { Text("Key") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(80.dp)
        )
    }
}

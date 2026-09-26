package com.fidfanstudios.fidfancullr.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.AccentPalette
import com.fidfanstudios.fidfancullr.data.ThemeMode
import com.fidfanstudios.fidfancullr.viewmodel.SettingsViewModel

@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    onFinished: () -> Unit
) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: com.fidfanstudios.fidfancullr.data.AppSettings()
    var chosenAccent by remember { mutableStateOf(settings.accentPalette) }
    var chosenTheme by remember { mutableStateOf(settings.themeMode) }
    var inboxPicked by remember { mutableStateOf(settings.inboxUri != null) }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            viewModel.getApplication<android.app.Application>().contentResolver
                .takePersistableUriPermission(uri, flags)
            viewModel.setInbox(uri)
            inboxPicked = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.choose_inbox), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.choose_inbox_desc), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { pickFolderLauncher.launch(null) }) {
            Text(if (inboxPicked) "✓ ${stringResource(R.string.choose_inbox)}" else stringResource(R.string.choose_inbox))
        }

        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = chosenTheme == mode,
                    onClick = { chosenTheme = mode; viewModel.setTheme(mode) },
                    label = { Text(mode.name) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.accent), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AccentPalette.values().toList()) { palette ->
                FilterChip(
                    selected = chosenAccent == palette,
                    onClick = { chosenAccent = palette; viewModel.setAccent(palette) },
                    label = { Text(palette.name.replace("_", " ")) }
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = {
                viewModel.setOnboardingComplete(true)
                onFinished()
            },
            enabled = inboxPicked,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.start_culling))
        }
    }
}

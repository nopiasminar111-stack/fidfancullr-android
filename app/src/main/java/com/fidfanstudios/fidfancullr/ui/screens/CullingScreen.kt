package com.fidfanstudios.fidfancullr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.ExifData
import com.fidfanstudios.fidfancullr.data.PhotoGroup
import com.fidfanstudios.fidfancullr.data.SortDestination
import com.fidfanstudios.fidfancullr.util.ExifReader
import com.fidfanstudios.fidfancullr.viewmodel.CullingViewModel
import kotlin.math.abs

/** Swipe distance (px) needed to trigger a sort action. */
private const val SWIPE_THRESHOLD = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CullingScreen(
    viewModel: CullingViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showExif by remember { mutableStateOf(true) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    // Reset zoom/pan whenever the current group changes.
    LaunchedEffect(uiState.currentGroup?.stem) {
        scale = 1f; offsetX = 0f; offsetY = 0f; dragOffsetX = 0f
    }

    val bitmap = remember(uiState.currentGroup?.stem) {
        uiState.currentGroup?.let { group ->
            ExifReader.loadPreviewBitmap(context, group.primaryFile)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.groups.isNotEmpty())
                            stringResource(R.string.group_count, uiState.currentIndex + 1, uiState.groups.size)
                        else ""
                    )
                },
                actions = {
                    IconButton(onClick = { showExif = !showExif }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.show_exif))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.isComplete || uiState.currentGroup == null -> Text(
                    stringResource(R.string.culling_complete),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                else -> {
                    val group = uiState.currentGroup!!

                    Column(Modifier.fillMaxSize()) {
                        // ---- Preview area: pinch-zoom, pan, and swipe-to-sort ----
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .pointerInput(group.stem) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                                .pointerInput(group.stem, settings.destinations) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val dest = destinationForSwipe(dragOffsetX, settings.destinations)
                                            if (dest != null) viewModel.sortCurrentGroup(dest)
                                            dragOffsetX = 0f
                                        }
                                    ) { change, delta ->
                                        change.consume()
                                        dragOffsetX += delta.x
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = group.stem,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX + dragOffsetX,
                                            translationY = offsetY
                                        )
                                )
                            } else {
                                Text(
                                    stringResource(R.string.raw_no_preview),
                                    color = Color.White,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }

                            if (group.hasRaw) {
                                RawBadge(Modifier.align(Alignment.TopStart).padding(12.dp))
                            }

                            if (scale > 1.01f) {
                                TextButton(
                                    onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                ) { Text(stringResource(R.string.reset_zoom), color = Color.White) }
                            }
                        }

                        // ---- EXIF panel ----
                        AnimatedVisibility(visible = showExif) {
                            ExifPanel(uiState.currentExif)
                        }

                        // ---- Sort destination buttons ----
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            settings.destinations.forEach { destination ->
                                Button(
                                    onClick = { viewModel.sortCurrentGroup(destination) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${destination.label} (${destination.gestureKey})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun destinationForSwipe(dragX: Float, destinations: List<SortDestination>): SortDestination? {
    if (abs(dragX) < SWIPE_THRESHOLD || destinations.isEmpty()) return null
    // Convention: swipe right -> first destination (e.g. KEEP), swipe left -> last (e.g. REJECT).
    return if (dragX > 0) destinations.first() else destinations.last()
}

@Composable
private fun RawBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("RAW", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ExifPanel(exif: ExifData?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            ExifRow(stringResource(R.string.camera), exif?.camera)
            ExifRow(stringResource(R.string.lens), exif?.lens)
            Row {
                ExifRow(stringResource(R.string.iso), exif?.iso, Modifier.weight(1f))
                ExifRow(stringResource(R.string.aperture), exif?.aperture, Modifier.weight(1f))
                ExifRow(stringResource(R.string.shutter_speed), exif?.shutterSpeed, Modifier.weight(1f))
            }
            ExifRow(stringResource(R.string.focal_length), exif?.focalLength)
            ExifRow(stringResource(R.string.date_time), exif?.dateTime)
        }
    }
}

@Composable
private fun ExifRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value == null) return
    Column(modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

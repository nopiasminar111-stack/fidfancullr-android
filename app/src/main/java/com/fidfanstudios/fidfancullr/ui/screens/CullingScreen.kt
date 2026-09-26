package com.fidfanstudios.fidfancullr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.ExifData
import com.fidfanstudios.fidfancullr.data.SortDestination
import com.fidfanstudios.fidfancullr.util.ExifReader
import com.fidfanstudios.fidfancullr.viewmodel.CullingViewModel
import kotlin.math.abs

private const val SWIPE_THRESHOLD = 120f

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
    var showExif by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(uiState.currentGroup?.stem) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        dragOffsetX = 0f
    }

    val bitmap = remember(uiState.currentGroup?.stem) {
        uiState.currentGroup?.let { ExifReader.loadPreviewBitmap(context, it.primaryFile) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            PixelTopBar(
                currentIndex = uiState.currentIndex,
                total = uiState.groups.size,
                fileName = uiState.currentGroup?.stem,
                onInfo = { showExif = !showExif },
                onSettings = onOpenSettings
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

                uiState.isComplete || uiState.currentGroup == null -> {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            stringResource(R.string.culling_complete),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(28.dp)
                        )
                    }
                }

                else -> {
                    val group = uiState.currentGroup!!
                    val animatedDrag by animateFloatAsState(
                        targetValue = dragOffsetX,
                        label = "swipePreview"
                    )

                    Column(Modifier.fillMaxSize()) {
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
                                            val dest = destinationForSwipe(
                                                dragOffsetX,
                                                settings.destinations
                                            )
                                            if (dest != null) viewModel.sortCurrentGroup(dest)
                                            dragOffsetX = 0f
                                        },
                                        onDragCancel = { dragOffsetX = 0f }
                                    ) { change, delta ->
                                        change.consume()
                                        dragOffsetX += delta.x
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = group.stem,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX + animatedDrag,
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
                                RawBadge(
                                    Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                )
                            }

                            if (scale > 1.01f) {
                                FilledTonalButton(
                                    onClick = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.62f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(stringResource(R.string.reset_zoom))
                                }
                            }
                        }

                        AnimatedVisibility(visible = showExif) {
                            ExifPanel(uiState.currentExif)
                        }

                        PixelActionBar(
                            destinations = settings.destinations,
                            onDestination = viewModel::sortCurrentGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelTopBar(
    currentIndex: Int,
    total: Int,
    fileName: String?,
    onInfo: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 72.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (total > 0) "${currentIndex + 1} / $total" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!fileName.isNullOrBlank()) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onInfo) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = stringResource(R.string.show_exif)
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    }
}

@Composable
private fun PixelActionBar(
    destinations: List<SortDestination>,
    onDestination: (SortDestination) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                val primary = index == 0
                if (primary) {
                    Button(
                        onClick = { onDestination(destination) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text(destination.label, maxLines = 1)
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onDestination(destination) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text(destination.label, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun destinationForSwipe(
    dragX: Float,
    destinations: List<SortDestination>
): SortDestination? {
    if (abs(dragX) < SWIPE_THRESHOLD || destinations.isEmpty()) return null
    return if (dragX > 0) destinations.first() else destinations.last()
}

@Composable
private fun RawBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            "RAW",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExifPanel(exif: ExifData?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                "EXIF",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            ExifRow(stringResource(R.string.camera), exif?.camera)
            ExifRow(stringResource(R.string.lens), exif?.lens)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun ExifRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    if (value == null) return
    Column(modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

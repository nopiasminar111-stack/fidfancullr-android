package com.fidfanstudios.fidfancullr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.fidfanstudios.fidfancullr.ui.theme.PillShape
import com.fidfanstudios.fidfancullr.viewmodel.CullingViewModel
import com.fidfanstudios.fidfancullr.viewmodel.PreviewUiState
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD = 120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CullingScreen(
    viewModel: CullingViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showExif by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(targetState = uiState.groups.size to uiState.currentIndex, label = "counter") {
                        Text(
                            if (uiState.groups.isNotEmpty())
                                stringResource(R.string.group_count, uiState.currentIndex + 1, uiState.groups.size)
                            else "",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExif = !showExif }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.show_exif))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> LoadingState()

                uiState.isComplete || uiState.currentGroup == null -> CompleteState()

                else -> CullingWorkspace(
                    group = uiState.currentGroup!!,
                    previewState = uiState.previewState,
                    currentExif = uiState.currentExif,
                    showExif = showExif,
                    destinations = settings.destinations,
                    onSort = viewModel::sortCurrentGroup,
                    onRetry = viewModel::retryCurrentPreview
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CompleteState() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.culling_complete),
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * The image + gesture + sort-buttons area, isolated into its own composable
 * so that fast-changing gesture state (zoom/pan/drag) only recomposes this
 * subtree rather than the whole screen (top bar, scaffold, etc).
 */
@Composable
private fun CullingWorkspace(
    group: PhotoGroup,
    previewState: PreviewUiState,
    currentExif: ExifData?,
    showExif: Boolean,
    destinations: List<SortDestination>,
    onSort: (SortDestination) -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ZoomableSwipeableImage(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            group = group,
            previewState = previewState,
            destinations = destinations,
            onSort = onSort,
            onRetry = onRetry
        )

        AnimatedVisibility(visible = showExif, enter = fadeIn(), exit = fadeOut()) {
            ExifPanel(currentExif)
        }

        SortButtonsRow(destinations = destinations, onSort = onSort)
    }
}

@Composable
private fun ZoomableSwipeableImage(
    modifier: Modifier,
    group: PhotoGroup,
    previewState: PreviewUiState,
    destinations: List<SortDestination>,
    onSort: (SortDestination) -> Unit,
    onRetry: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scale = remember(group.stem) { androidx.compose.animation.core.Animatable(1f) }
    val offsetX = remember(group.stem) { androidx.compose.animation.core.Animatable(0f) }
    val offsetY = remember(group.stem) { androidx.compose.animation.core.Animatable(0f) }
    val dragOffsetX = remember(group.stem) { androidx.compose.animation.core.Animatable(0f) }

    val isZoomed = scale.value > 1.01f
    val springSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val baseModifier = Modifier
            .fillMaxSize()
            .pointerInput(group.stem) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scope.launch {
                        scale.snapTo((scale.value * zoom).coerceIn(1f, 5f))
                        offsetX.snapTo(offsetX.value + pan.x)
                        offsetY.snapTo(offsetY.value + pan.y)
                    }
                }
            }

        // Swipe-to-sort only engages at 1x zoom, so panning a zoomed-in
        // image never gets misread as a sort gesture.
        val gestureModifier = if (!isZoomed) {
            baseModifier.pointerInput(group.stem, destinations) {
                detectDragGestures(
                    onDragEnd = {
                        val dest = destinationForSwipe(dragOffsetX.value, destinations)
                        scope.launch {
                            if (dest != null) {
                                onSort(dest)
                            } else {
                                dragOffsetX.animateTo(0f, springSpec)
                            }
                        }
                    }
                ) { change, delta ->
                    change.consume()
                    scope.launch { dragOffsetX.snapTo(dragOffsetX.value + delta.x) }
                }
            }
        } else baseModifier

        Box(gestureModifier, contentAlignment = Alignment.Center) {
            when (previewState) {
                is PreviewUiState.Loading -> CircularProgressIndicator(color = Color.White)

                is PreviewUiState.Loaded -> Image(
                    bitmap = previewState.bitmap.asImageBitmap(),
                    contentDescription = group.stem,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value,
                            translationX = offsetX.value + dragOffsetX.value,
                            translationY = offsetY.value
                        )
                )

                is PreviewUiState.NoPreview -> PreviewMessage(stringResource(R.string.raw_no_preview))

                is PreviewUiState.Error -> ErrorMessage(previewState.message, onRetry)
            }

            if (group.hasRaw) {
                RawBadge(Modifier.align(Alignment.TopStart).padding(12.dp))
            }

            if (isZoomed) {
                TextButton(
                    onClick = {
                        scope.launch {
                            scale.animateTo(1f, springSpec)
                            offsetX.animateTo(0f, springSpec)
                            offsetY.animateTo(0f, springSpec)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                ) { Text(stringResource(R.string.reset_zoom), color = Color.White) }
            }
        }
    }
}

private fun destinationForSwipe(dragX: Float, destinations: List<SortDestination>): SortDestination? {
    if (abs(dragX) < SWIPE_THRESHOLD || destinations.isEmpty()) return null
    return if (dragX > 0) destinations.first() else destinations.last()
}

@Composable
private fun PreviewMessage(text: String) {
    Text(text, color = Color.White, modifier = Modifier.padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun RawBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("RAW", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SortButtonsRow(destinations: List<SortDestination>, onSort: (SortDestination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        destinations.forEach { destination ->
            Button(
                onClick = { onSort(destination) },
                shape = PillShape,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${destination.label} (${destination.gestureKey})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ExifPanel(exif: ExifData?) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
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
    Column(modifier.padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

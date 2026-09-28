package com.fidfanstudios.fidfancullr.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
importBox androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fidfanstudios.fidfancullr.R
import com.fidfanstudios.fidfancullr.data.*
import com.fidfanstudios.fidfancullr.ui.theme.PillShape
import com.fidfanstudios.fidfancullr.util.PreviewLoader
import com.fidfanstudios.fidfancullr.util.PreviewResult
import com.fidfanstudios.fidfancullr.viewmodel.*
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD=120f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CullingScreen(viewModel:CullingViewModel,onOpenSettings:()->Unit){
    val ui by viewModel.uiState.collectAsState(); val settings by viewModel.settings.collectAsState()
    var showExif by rememberSaveable{mutableStateOf(true)}; var compare by rememberSaveable{mutableStateOf(false)}
    var showTools by rememberSaveable{mutableStateOf(false)}; var showFilter by rememberSaveable{mutableStateOf(false)}
    val haptic=LocalHapticFeedback.current
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null){val f=Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION;viewModel.getApplication<android.app.Application>().contentResolver.takePersistableUriPermission(uri,f);viewModel.setInbox(uri)}}
    Scaffold(topBar={TopAppBar(title={AnimatedContent(ui.groups.size to ui.currentIndex,label="counter"){Text(if(ui.groups.isNotEmpty())"${ui.currentIndex+1} / ${ui.groups.size}" else "FidFan Cullr")}},actions={
        IconButton(onClick={viewModel::undoLast},enabled=ui.undo!=null){Icon(Icons.Default.Undo,"Undo")}
        IconButton(onClick={onOpenSettings}){Icon(Icons.Default.Settings,"Settings")}
    })}){padding->
        Box(Modifier.fillMaxSize().padding(padding).onPreviewKeyEvent{e->
            if(e.type!=KeyEventType.KeyDown)return@onPreviewKeyEvent false
            val g=ui.currentGroup?:return@onPreviewKeyEvent false
            val dest=when(e.key){Key.One->settings.destinations.getOrNull(0);Key.Two->settings.destinations.getOrNull(1);Key.Three->settings.destinations.getOrNull(2);Key.DirectionRight->settings.destinations.firstOrNull();Key.DirectionLeft->settings.destinations.lastOrNull();Key.DirectionUp->settings.destinations.firstOrNull{it.id=="select"}?:settings.destinations.getOrNull(1);else->null}
            if(dest!=null){if(settings.hapticFeedback)performCullHaptic(haptic,dest);viewModel.sortCurrentGroup(dest);true}else false
        }){
            when{ui.isLoading->LoadingState();ui.isComplete||ui.currentGroup==null->CompleteState({pick.launch(null)},onOpenSettings);else->Column(Modifier.fillMaxSize()){
                Box(Modifier.weight(1f).fillMaxWidth()){
                    if(compare) CompareView(ui,settings,onClose={compare=false}) else ZoomableSwipeableImage(ui.currentGroup!!,ui.previewState,settings.destinations,settings.hapticFeedback,viewModel::sortCurrentGroup,viewModel::retryCurrentPreview)
                    Row(Modifier.align(Alignment.TopEnd).padding(10.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SmallFab("Compare",Icons.Default.CompareArrows){compare=true}
                        SmallFab(if(showExif)"Hide EXIF" else "EXIF",Icons.Default.Info){showExif=!showExif}
                        SmallFab("Tools",Icons.Default.Tune){showTools=!showTools}
                    }
                }
                AnimatedVisibility(showTools,enter=fadeIn(),exit=fadeOut()){ToolPanel(ui,viewModel)}
                AnimatedVisibility(showExif,enter=fadeIn(),exit=fadeOut()){ExifPanel(ui.currentExif)}
                SortButtonsRow(settings.destinations,viewModel::sortCurrentGroup,settings.hapticFeedback)
            }}
        }
    }
    if(showFilter){} // reserved for a future modal surface; sorting is exposed in ToolPanel.
}


private fun performCullHaptic(
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    destination: SortDestination
) {
    haptic.performHapticFeedback(
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
    )
}

@Composable private fun SmallFab(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){FilledTonalIconButton(onClick=onClick,modifier=Modifier.height(40.dp).widthIn(min=40.dp)){Icon(icon,text)}}

@Composable private fun ToolPanel(ui:CullingUiState,vm:CullingViewModel){
    Card(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=4.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh)){
        Column(Modifier.padding(12.dp)){Text("Culling tools",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=8.dp)){(1..5).forEach{r->FilterChip(selected=ui.currentGroup?.let{ui.ratings[it.stem]==r}==true,onClick={vm.rateCurrent(r)},label={Text("★$r")})}}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=6.dp)){ColorTag.values().forEach{tag->FilterChip(selected=ui.currentGroup?.let{ui.tags[it.stem]==tag}==true,onClick={ {vm.tagCurrent(tag)} },label={Text(tag.name)})}}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=6.dp)){Text("Sort:");TextButton({vm.filterAndSort("name")}){Text("Name")};TextButton({vm.filterAndSort("date")}){Text("Date")};TextButton({vm.filterAndSort("size")}){Text("Size")};TextButton({vm.filterAndSort("focal")}){Text("Focal")}}
            Text("Swipe right = Keep • left = Reject • up = Select • keyboard 1/2/3",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun LoadingState(){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
@Composable private fun CompleteState(onChooseFolder:()->Unit,onOpenSettings:()->Unit){var visible by remember{mutableStateOf(false)};LaunchedEffect(Unit){visible=true};AnimatedVisibility(visible,enter=fadeIn(tween(220))+scaleIn(initialScale=.96f,animationSpec=spring())){Column(Modifier.fillMaxSize().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(Icons.Default.CheckCircle,null,Modifier.size(72.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(20.dp));Text(stringResource(R.string.culling_complete),style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(8.dp));Text(stringResource(R.string.culling_complete_desc),textAlign=androidx.compose.ui.text.style.TextAlign.Center);Spacer(Modifier.height(24.dp));Button(onChooseFolder,shape=PillShape){Icon(Icons.Default.CreateNewFolder,null);Spacer(Modifier.width(8.dp));Text(stringResource(R.string.choose_another_folder))};TextButton(onOpenSettings){Text(stringResource(R.string.settings))}}}}

@Composable
private fun ZoomableSwipeableImage(
    group: PhotoGroup,
    previewState: PreviewUiState,
    destinations: List<SortDestination>,
    haptics: Boolean,
    onSort: (SortDestination) -> Unit,
    onRetry: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var scale by remember(group.stem) { mutableFloatStateOf(1f) }
    var ox by remember(group.stem) { mutableFloatStateOf(0f) }
    var oy by remember(group.stem) { mutableFloatStateOf(0f) }
    var dragX by remember(group.stem) { mutableFloatStateOf(0f) }
    var dragY by remember(group.stem) { mutableFloatStateOf(0f) }
    var highlight by rememberSaveable(group.stem) { mutableStateOf(false) }
    var sorting by remember(group.stem) { mutableStateOf(false) }

    // Keep expensive bitmap inspection tiny and deterministic. Doing millions of
    // getPixel() calls during composition was a major source of UI-thread jank.
    val highlightRatio = remember(previewState) {
        val bitmap = (previewState as? PreviewUiState.Loaded)?.bitmap ?: return@remember 0f
        val targetSamples = 1_500
        val step = (bitmap.width * bitmap.height / targetSamples).coerceAtLeast(1)
        var clipped = 0
        var samples = 0
        var index = 0
        val pixels = bitmap.width * bitmap.height
        while (index < pixels) {
            val x = index % bitmap.width
            val y = index / bitmap.width
            val c = bitmap.getPixel(x, y)
            if (android.graphics.Color.red(c) > 245 &&
                android.graphics.Color.green(c) > 245 &&
                android.graphics.Color.blue(c) > 245
            ) clipped++
            samples++
            index += step
        }
        if (samples == 0) 0f else clipped.toFloat() / samples
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val gestures = Modifier.pointerInput(group.stem) {
            detectTransformGestures { _, pan, zoom, _ ->
                if (scale > 1f || zoom > 1f) {
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    ox += pan.x
                    oy += pan.y
                }
            }
        }.pointerInput(group.stem) {
            detectTapGestures(
                onDoubleTap = {
                    scale = if (scale > 1.01f) 1f else 2f
                    ox = 0f
                    oy = 0f
                },
                onTap = {
                    if (scale <= 1.01f) {
                        scale = 2f
                        ox = -it.x * .25f
                        oy = -it.y * .25f
                    }
                }
            )
        }

        val swipe = Modifier.pointerInput(group.stem, destinations, sorting, scale) {
            detectDragGestures(
                onDragEnd = {
                    if (sorting) return@detectDragGestures
                    val destination = when {
                        abs(dragY) > SWIPE_THRESHOLD && dragY < 0 ->
                            destinations.firstOrNull { it.id == "select" } ?: destinations.getOrNull(1)
                        abs(dragX) > SWIPE_THRESHOLD && dragX > 0 -> destinations.firstOrNull()
                        abs(dragX) > SWIPE_THRESHOLD -> destinations.lastOrNull()
                        else -> null
                    }

                    if (destination != null) {
                        sorting = true
                        if (haptics) performCullHaptic(haptic, destination)
                        val endX = when {
                            abs(dragY) > SWIPE_THRESHOLD && dragY < 0 -> 0f
                            dragX > 0 -> 900f
                            else -> -900f
                        }
                        val endY = if (abs(dragY) > SWIPE_THRESHOLD && dragY < 0) -900f else dragY
                        scope.launch {
                            // Finger tracking is immediate. Only the release is animated,
                            // with a non-bouncy ease-out similar to modern system UI.
                            val duration = 220
                            val startX = dragX
                            val startY = dragY
                            val start = System.nanoTime()
                            while (true) {
                                val elapsed = ((System.nanoTime() - start) / 1_000_000L).toInt()
                                val t = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                                val eased = FastOutSlowInEasing.transform(t)
                                dragX = startX + (endX - startX) * eased
                                dragY = startY + (endY - startY) * eased
                                if (t >= 1f) break
                                kotlinx.coroutines.yield()
                            }
                            onSort(destination)
                        }
                    } else {
                        scope.launch {
                            val startX = dragX
                            val startY = dragY
                            val start = System.nanoTime()
                            val duration = 180
                            while (true) {
                                val elapsed = ((System.nanoTime() - start) / 1_000_000L).toInt()
                                val t = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                                val eased = FastOutSlowInEasing.transform(t)
                                dragX = startX * (1f - eased)
                                dragY = startY * (1f - eased)
                                if (t >= 1f) break
                                kotlinx.coroutines.yield()
                            }
                        }
                    }
                }
            ) { change, delta ->
                if (!sorting && scale <= 1.01f) {
                    change.consume()
                    dragX += delta.x
                    dragY += delta.y
                }
            }
        }

        Box(
    modifier = gestures
        .then(swipe)
        .fillMaxSize(),
    contentAlignment = Alignment.Center
) {
            when (previewState) {
                is PreviewUiState.Loading -> CircularProgressIndicator(color = Color.White)
                is PreviewUiState.Loaded -> {
                    Image(
                        bitmap = previewState.bitmap.asImageBitmap(),
                        contentDescription = group.stem,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = ox + dragX,
                                translationY = oy + dragY
                            )
                    )
                    HistogramOverlay(previewState.bitmap)
                    if (highlight && highlightRatio > .005f) HighlightBadge(highlightRatio)
                }
                is PreviewUiState.NoPreview -> PreviewMessage(stringResource(R.string.raw_no_preview))
                is PreviewUiState.Error -> ErrorMessage(previewState.message, onRetry)
            }
            if (group.hasRaw) RawBadge(Modifier.align(Alignment.TopStart).padding(12.dp))
            FilledTonalIconButton(
                onClick = { highlight = !highlight },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 52.dp, start = 10.dp)
            ) { Icon(Icons.Default.BrightnessHigh, "Highlight alert") }
            if (scale > 1.01f) TextButton(
                onClick = { scale = 1f; ox = 0f; oy = 0f },
                modifier = Modifier.align(Alignment.BottomStart)
            ) { Text("100% / Reset", color = Color.White) }
        }
    }
}

@Composable private fun HistogramOverlay(bitmap:android.graphics.Bitmap){
    val bins=remember(bitmap){Array(3){IntArray(32)}.also{b->val step=(bitmap.width*bitmap.height/5000).coerceAtLeast(1);var i=0;while(i<bitmap.width*bitmap.height){val c=bitmap.getPixel(i%bitmap.width,i/bitmap.width);b[0][android.graphics.Color.red(c)*31/255]++;b[1][android.graphics.Color.green(c)*31/255]++;b[2][android.graphics.Color.blue(c)*31/255]++;i+=step}}}
    Canvas(Modifier.size(96.dp,52.dp).padding(4.dp).clip(MaterialTheme.shapes.small)){val max=bins.flatMap{it.asIterable()}.maxOrNull()?.coerceAtLeast(1)?:1;val channels=listOf(Color.Red,Color.Green,Color.Blue);for(ch in 0..2){for(i in bins[ch].indices){val h=bins[ch][i].toFloat()/max*size.height;drawLine(channels[ch].copy(alpha=.55f),Offset(i*size.width/bins[ch].size,size.height),Offset(i*size.width/bins[ch].size,size.height-h),strokeWidth=1.5f)}}}
}
@Composable private fun HighlightBadge(ratio:Float){Surface(Modifier.padding(top=10.dp).clip(PillShape),color=Color.Red.copy(alpha=.75f)){Text("HIGHLIGHT ${(ratio*100).toInt()}%",Modifier.padding(horizontal=8.dp,vertical=4.dp),color=Color.White,style=MaterialTheme.typography.labelSmall)}}
@Composable private fun PreviewMessage(text:String){Text(text,color=Color.White,modifier=Modifier.padding(24.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
@Composable private fun ErrorMessage(message:String,onRetry:()->Unit){Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.padding(24.dp)){Icon(Icons.Default.WarningAmber,null,tint=Color.White);Text(message,color=Color.White,textAlign=androidx.compose.ui.text.style.TextAlign.Center);FilledTonalButton(onRetry){Text(stringResource(R.string.retry))}}}
@Composable private fun RawBadge(modifier:Modifier=Modifier){Surface(modifier,color=MaterialTheme.colorScheme.primary,shape=PillShape){Text("RAW",Modifier.padding(horizontal=10.dp,vertical=4.dp),color=MaterialTheme.colorScheme.onPrimary)}}

@Composable private fun CompareView(ui:CullingUiState,settings:AppSettings,onClose:()->Unit){
    val group=ui.currentGroup?:return
    val other=ui.groups.getOrNull(ui.currentIndex+1)
    if(other==null){Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("No adjacent photo to compare",color=Color.White);TextButton(onClose){Text("Close")}}};return}
    var second by remember(other.stem){mutableStateOf<android.graphics.Bitmap?>(null)}
    val context=LocalContext.current
    LaunchedEffect(other.stem){
        second=null
        val p=PreviewLoader.load(context,other.primaryFile,1080,1080)
        if(p is PreviewResult.Success) second=p.bitmap
    }
    Box(Modifier.fillMaxSize().background(Color.Black)){
        Row(Modifier.fillMaxSize()){
            Column(Modifier.weight(1f).fillMaxHeight()){
                when(val p=ui.previewState){is PreviewUiState.Loaded->Image(p.bitmap.asImageBitmap(),group.stem,Modifier.fillMaxWidth().weight(1f));else->Box(Modifier.weight(1f))}
                Text("A  ${group.stem}",color=Color.White,modifier=Modifier.padding(8.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight()){
                if(second!=null) Image(second!!.asImageBitmap(),other.stem,Modifier.fillMaxWidth().weight(1f)) else Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
                Text("B  ${other.stem}",color=Color.White,modifier=Modifier.padding(8.dp))
            }
        }
        FilledTonalButton(onClose,Modifier.align(Alignment.BottomCenter).padding(12.dp)){Text("Close")}
    }
}

@Composable private fun SortButtonsRow(destinations:List<SortDestination>,onSort:(SortDestination)->Unit,haptics:Boolean){val h=LocalHapticFeedback.current;Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){destinations.forEach{d->Button({if(haptics)performCullHaptic(h,d);onSort(d)},Modifier.weight(1f),shape=PillShape){Text("${d.label} (${d.gestureKey})")}}}}
@Composable private fun ExifPanel(exif:ExifData?){Card(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=4.dp)){Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ExifRow("Camera",exif?.camera);ExifRow("Lens",exif?.lens)};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ExifRow("ISO",exif?.iso);ExifRow("Aperture",exif?.aperture);ExifRow("Shutter",exif?.shutterSpeed)};ExifRow("Focal",exif?.focalLength);ExifRow("Date",exif?.dateTime)}}}
@Composable private fun ExifRow(label:String,value:String?){if(value!=null)Column(Modifier.padding(vertical=2.dp)){Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(value,style=MaterialTheme.typography.bodySmall)}}

package com.fidfanstudios.fidfancullr.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fidfanstudios.fidfancullr.data.*
import com.fidfanstudios.fidfancullr.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PreviewUiState { data object Loading: PreviewUiState(); data class Loaded(val bitmap: android.graphics.Bitmap): PreviewUiState(); data object NoPreview: PreviewUiState(); data class Error(val message:String): PreviewUiState() }
data class UndoAction(val group: PhotoGroup, val destination: SortDestination)
data class CullingUiState(
    val isLoading:Boolean=true, val groups:List<PhotoGroup> = emptyList(), val currentIndex:Int=0,
    val totalCount:Int=0, val sortedCount:Int=0, val currentExif:ExifData?=null,
    val previewState:PreviewUiState=PreviewUiState.Loading, val isComplete:Boolean=false,
    val ratings:Map<String,Int> = emptyMap(), val tags:Map<String,ColorTag> = emptyMap(),
    val undo:UndoAction? = null, val compareEnabled:Boolean=false, val filterInfo:String=""
) { val currentGroup:PhotoGroup? get()=groups.getOrNull(currentIndex) }

class CullingViewModel(application:Application):AndroidViewModel(application) {
    private val repository=SettingsRepository(application)
    private val _uiState=MutableStateFlow(CullingUiState())
    val uiState:StateFlow<CullingUiState> = _uiState.asStateFlow()
    val settings:StateFlow<AppSettings> = MutableStateFlow(AppSettings())
    private val _settings=settings as MutableStateFlow<AppSettings>
    private val reqWidth:Int
    private val reqHeight:Int
    private var previewLoadJob:Job?=null

    init {
        val m=application.resources.displayMetrics; reqWidth=m.widthPixels; reqHeight=m.heightPixels
        viewModelScope.launch { repository.settingsFlow.collect { s -> _settings.value=s; if(s.inboxUri!=null && _uiState.value.groups.isEmpty() && !_uiState.value.isComplete) loadGroups(Uri.parse(s.inboxUri)) } }
    }

    fun loadGroups(inboxUri:Uri) = viewModelScope.launch {
        _uiState.value=_uiState.value.copy(isLoading=true,isComplete=false)
        val groups=withContext(Dispatchers.IO){ PhotoGrouper.scan(getApplication(),inboxUri) }
        _uiState.value=CullingUiState(isLoading=false,groups=groups,currentIndex=0,totalCount=groups.size,isComplete=groups.isEmpty())
        loadCurrentPreviewAndExif()
    }

    fun filterAndSort(sortBy:String) = viewModelScope.launch {
        val s=_uiState.value
        val sorted=when(sortBy){
            "date" -> s.groups.sortedByDescending { it.primaryFile.lastModified }
            "size" -> s.groups.sortedByDescending { it.files.sumOf { f->f.sizeBytes } }
            "focal" -> s.groups.sortedByDescending { ExifReader.read(getApplication(), it.primaryFile).focalLength?.filter { c -> c.isDigit() }?.toFloatOrNull() ?: 0f }
            else -> s.groups.sortedBy { it.stem }
        }
        _uiState.value=s.copy(groups=sorted,currentIndex=0,filterInfo=sortBy)
        loadCurrentPreviewAndExif()
    }

    private fun loadCurrentPreviewAndExif(){
        previewLoadJob?.cancel()
        val group=_uiState.value.currentGroup ?: run { _uiState.value=_uiState.value.copy(currentExif=null,previewState=PreviewUiState.NoPreview); return }
        _uiState.value=_uiState.value.copy(previewState=PreviewUiState.Loading)
        previewLoadJob=viewModelScope.launch {
            val exif=async(Dispatchers.IO){ExifReader.read(getApplication(),group.primaryFile)}
            val preview=PreviewLoader.load(getApplication(),group.primaryFile,reqWidth,reqHeight)
            _uiState.value=_uiState.value.copy(currentExif=exif.await(),previewState=when(preview){
                is PreviewResult.Success->PreviewUiState.Loaded(preview.bitmap)
                is PreviewResult.NoPreviewAvailable->PreviewUiState.NoPreview
                is PreviewResult.Failed->PreviewUiState.Error(preview.reason)
            })
            preloadAdjacent()
        }
    }

    private fun preloadAdjacent(){
        val s=_uiState.value; val next=s.groups.getOrNull(s.currentIndex+1); val prev=s.groups.getOrNull(s.currentIndex-1)
        viewModelScope.launch(Dispatchers.IO){ next?.let{PreviewLoader.preload(getApplication(),it.primaryFile,reqWidth,reqHeight)}; prev?.let{PreviewLoader.preload(getApplication(),it.primaryFile,reqWidth,reqHeight)} }
    }

    fun rateCurrent(rating:Int){ val g=_uiState.value.currentGroup?:return; _uiState.value=_uiState.value.copy(ratings=_uiState.value.ratings+(g.stem to rating.coerceIn(0,5))); persistSidecar(g) }
    fun tagCurrent(tag:ColorTag){ val g=_uiState.value.currentGroup?:return; _uiState.value=_uiState.value.copy(tags=_uiState.value.tags+(g.stem to tag)); persistSidecar(g) }
    private fun persistSidecar(group:PhotoGroup){
        if(!_settings.value.xmpSidecars)return
        val inbox=_settings.value.inboxUri?.let(Uri::parse)?:return
        val rating=_uiState.value.ratings[group.stem]?:0; val tag=_uiState.value.tags[group.stem]?:ColorTag.NONE
        viewModelScope.launch(Dispatchers.IO){ val root=androidx.documentfile.provider.DocumentFile.fromTreeUri(getApplication(),inbox)?:return@launch; XmpSidecar.write(getApplication(),root,group,rating,tag,_settings.value.customPrefix,_settings.value.customSuffix) }
    }

    fun sortCurrentGroup(destination:SortDestination){
        val s=_uiState.value; val group=s.currentGroup?:return; val inbox=_settings.value.inboxUri?.let(Uri::parse)?:return
        viewModelScope.launch {
            val ok=withContext(Dispatchers.IO){ FileMover.moveGroupTo(getApplication(),inbox,group,destination.folderName,_settings.value.customPrefix,_settings.value.customSuffix) }
            if(ok){
                val rating=s.ratings[group.stem]?:0; val tag=s.tags[group.stem]?:ColorTag.NONE
                if(_settings.value.xmpSidecars) withContext(Dispatchers.IO){
                    val root=androidx.documentfile.provider.DocumentFile.fromTreeUri(getApplication(),inbox)
                    val folder=root?.findFile(destination.folderName); if(folder!=null) XmpSidecar.write(getApplication(),folder,group,rating,tag,_settings.value.customPrefix,_settings.value.customSuffix)
                }
                advance(UndoAction(group,destination))
            }
        }
    }

    fun undoLast(){
        val action=_uiState.value.undo?:return; val inbox=_settings.value.inboxUri?.let(Uri::parse)?:return
        viewModelScope.launch {
            val ok=withContext(Dispatchers.IO){FileMover.moveGroupBack(getApplication(),inbox,action.group,action.destination.folderName,_settings.value.customPrefix,_settings.value.customSuffix)}
            if(ok){
                val restored=(_uiState.value.groups+action.group).sortedBy{it.stem}
                _uiState.value=_uiState.value.copy(groups=restored,currentIndex=restored.indexOf(action.group).coerceAtLeast(0),sortedCount=(_uiState.value.sortedCount-1).coerceAtLeast(0),isComplete=false,undo=null)
                loadCurrentPreviewAndExif()
            }
        }
    }

    fun skipCurrentGroup(){advance(null)}
    fun retryCurrentPreview(){loadCurrentPreviewAndExif()}
    private fun advance(action:UndoAction?){
        val s=_uiState.value; val remaining=s.groups.filterIndexed{index,_->index!=s.currentIndex}; val next=if(s.currentIndex>=remaining.size)0 else s.currentIndex
        _uiState.value=s.copy(groups=remaining,currentIndex=next,sortedCount=s.sortedCount+1,isComplete=remaining.isEmpty(),undo=action)
        loadCurrentPreviewAndExif()
    }
    fun updateDestinations(d:List<SortDestination>)=viewModelScope.launch{repository.setDestinations(d)}
    fun setInbox(uri:Uri)=viewModelScope.launch{repository.setInboxUri(uri.toString());loadGroups(uri)}
}

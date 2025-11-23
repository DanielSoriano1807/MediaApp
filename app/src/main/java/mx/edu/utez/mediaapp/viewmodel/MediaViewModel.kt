package mx.edu.utez.mediaapp.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import mx.edu.utez.mediaapp.ui.data.MediaItem
import mx.edu.utez.mediaapp.ui.data.MediaRepository
import mx.edu.utez.mediaapp.ui.data.MediaType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    val allAudio: StateFlow<List<MediaItem>> = repository.getAllAudio()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allImages: StateFlow<List<MediaItem>> = repository.getAllImages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allVideos: StateFlow<List<MediaItem>> = repository.getAllVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // FUNCIÓN BLINDADA: Si fallan los metadatos, guarda el archivo de todos modos
    fun insertMediaFromUri(uri: Uri, type: MediaType) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            // 1. Valores por defecto (por si falla la lectura)
            var finalName = "Archivo sin nombre ${System.currentTimeMillis()}"
            var finalDuration = 0L

            // 2. Intentamos leer metadatos (Nombre y Duración)
            try {
                val metadata = getMetadataFromUri(context.contentResolver, uri)
                if (metadata.first.isNotEmpty()) finalName = metadata.first
                finalDuration = metadata.second
            } catch (e: Exception) {
                // Si falla leer, solo imprimimos el error pero NO detenemos el guardado
                e.printStackTrace()
            }

            // 3. Guardamos en la Base de Datos SIEMPRE
            try {
                val item = MediaItem(
                    uri = uri.toString(),
                    name = finalName,
                    date = System.currentTimeMillis(),
                    duration = finalDuration,
                    type = type
                )
                repository.insertMedia(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertMediaFromFile(file: File, type: MediaType) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val authority = "${context.packageName}.fileprovider"
                // Usamos ruta completa para asegurar importación
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                insertMediaFromUri(uri, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getMetadataFromUri(contentResolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var aName = ""
        var aDuration = 0L

        // Intentar obtener nombre
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        aName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Intentar obtener duración (Solo Audio/Video)
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getApplication<Application>(), uri)
            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            aDuration = durationString?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (e: Exception) {
            // Es común que falle en videos temporales, no pasa nada
        }

        return Pair(aName, aDuration)
    }
}
package mx.edu.utez.mediaapp.ui.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String, // La URI del archivo (ej. content://...)
    val name: String,
    val date: Long, // Fecha de creación (timestamp)
    val duration: Long, // Duración en ms (0 para imágenes)
    val type: MediaType
)
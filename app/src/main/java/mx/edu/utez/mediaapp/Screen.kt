package mx.edu.utez.mediaapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Recording : Screen("recording", "Grabar", Icons.Default.Mic)
    // Uso de AutoMirrored para AudioFile que es equivalente a QueueMusic si no existe la de archivo
    object AudioList : Screen("audio", "Audios", Icons.AutoMirrored.Filled.QueueMusic)
    object ImageList : Screen("images", "Imágenes", Icons.Default.Image)
    object VideoList : Screen("videos", "Videos", Icons.Default.Videocam)

    // Pantalla de detalle (no va en la barra de navegación)
    object VideoPlayer : Screen("video_player/{uri}", "Video Player", Icons.Default.Videocam) {
        fun createRoute(uri: String) = "video_player/$uri"
    }
}

val navBarItems = listOf(
    Screen.Recording,
    Screen.AudioList,
    Screen.ImageList,
    Screen.VideoList,
)
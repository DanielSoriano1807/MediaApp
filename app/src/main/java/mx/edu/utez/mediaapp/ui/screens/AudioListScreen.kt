package mx.edu.utez.mediaapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.edu.utez.mediaapp.ui.data.MediaItem
import mx.edu.utez.mediaapp.viewmodel.MediaViewModel
import mx.edu.utez.mediaapp.viewmodel.PlaybackViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.filled.AcUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioListScreen(
    mediaViewModel: MediaViewModel,
    playbackViewModel: PlaybackViewModel
) {
    // Collect all flows as State
    val audioList by mediaViewModel.allAudio.collectAsState(initial = emptyList())
    val isPlaying by playbackViewModel.isPlaying.collectAsState()
    val isAccelerometerOn by playbackViewModel.isAccelerometerEnabled.collectAsState()
    val currentVolume by playbackViewModel.currentVolume.collectAsState()

    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Audios") },
                actions = {
                    // Botón para Activar/Desactivar Acelerómetro
                    IconButton(onClick = { playbackViewModel.toggleAccelerometer() }) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = "Control de Volumen por Sensor",
                            tint = if (isAccelerometerOn) Color.Red else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Mostrar nivel de volumen si el acelerómetro está activado
            if (isAccelerometerOn) {
                Text(
                    text = "Volumen (Sensor): ${"%.1f".format(currentVolume)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }

            // Lista de audios
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(audioList) { item ->
                    val isThisItemPlaying = isPlaying && item.uri == currentlyPlayingUri
                    AudioCard(
                        item = item,
                        isPlaying = isThisItemPlaying,
                        onPlayClick = {
                            if (isThisItemPlaying) {
                                playbackViewModel.togglePlayPause() // Pausar o reanudar
                            } else {
                                // Nuevo medio para reproducir
                                currentlyPlayingUri = item.uri
                                playbackViewModel.playMedia(item.uri)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioCard(
    item: MediaItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Más margen lateral
            .clickable(onClick = onPlayClick),
        // Forma personalizada: esquinas muy redondeadas
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            // Usamos un color de superficie variante o uno específico
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        // Borde neón si está sonando
        border = if (isPlaying)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp), // Más padding interno
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDuration(item.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary // Color de acento
                )
            }
            Spacer(Modifier.width(16.dp))

            // Botón Circular
            FilledIconButton(
                onClick = onPlayClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// Helper para formatear la duración de milisegundos a minutos:segundos
private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
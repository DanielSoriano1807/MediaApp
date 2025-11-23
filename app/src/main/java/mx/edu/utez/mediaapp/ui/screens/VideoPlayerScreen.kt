package mx.edu.utez.mediaapp.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
// Importa la anotación experimental explícitamente
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import mx.edu.utez.mediaapp.viewmodel.PlaybackViewModel

// Agregamos esta anotación para permitir el uso de la TopAppBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    uri: String,
    playbackViewModel: PlaybackViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Inicializar el reproductor con la URI
    LaunchedEffect(uri) {
        playbackViewModel.playMedia(uri)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Reproduciendo Video") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Composable que envuelve el PlayerView de ExoPlayer
            VideoPlayer(
                playbackViewModel = playbackViewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Proporción de video
            )

            // Controles de volumen/info aquí
            Text(
                text = "Video URI: $uri",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Manejo del ciclo de vida para pausar/reanudar el reproductor
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playbackViewModel.exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> playbackViewModel.exoPlayer.play()
                Lifecycle.Event.ON_DESTROY -> playbackViewModel.releasePlayer()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

// Composable que envuelve al PlayerView de ExoPlayer
@Composable
fun VideoPlayer(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = playbackViewModel.exoPlayer

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            // Se puede usar para actualizar propiedades del view si es necesario
        }
    )
}
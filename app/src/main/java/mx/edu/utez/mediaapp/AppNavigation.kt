package mx.edu.utez.mediaapp

import android.R.attr.type
import android.app.Application
import android.net.Uri
import android.net.http.SslCertificate.restoreState
import android.net.http.SslCertificate.saveState
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ejemplo.mediaapp.data.MediaType
import com.ejemplo.mediaapp.data.AudioRecorder
import com.ejemplo.mediaapp.ui.screens.AudioListScreen
import com.ejemplo.mediaapp.ui.screens.ImageListScreen
import com.ejemplo.mediaapp.ui.screens.RecordingScreen
import com.ejemplo.mediaapp.ui.screens.VideoListScreen
import com.ejemplo.mediaapp.ui.screens.VideoPlayerScreen
import com.ejemplo.mediaapp.viewmodel.MediaViewModel
import com.ejemplo.mediaapp.viewmodel.MediaViewModelFactory
import com.ejemplo.mediaapp.viewmodel.PlaybackViewModel
import com.ejemplo.mediaapp.viewmodel.PlaybackViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val audioRecorder = AudioRecorder(context) // Instancia del AudioRecorder

    // Factories para los ViewModels
    val mediaViewModel: MediaViewModel = viewModel(
        factory = MediaViewModelFactory(application)
    )
    val playbackViewModel: PlaybackViewModel = viewModel(
        factory = PlaybackViewModelFactory(application)
    )

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                navController = navController,
                mediaViewModel = mediaViewModel // Necesario para los Launchers
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Recording.route) {
                // Pasamos el mediaViewModel y el helper AudioRecorder
                RecordingScreen(
                    mediaViewModel = mediaViewModel,
                    audioRecorder = audioRecorder
                )
            }

            composable(Screen.AudioList.route) {
                AudioListScreen(
                    mediaViewModel = mediaViewModel,
                    playbackViewModel = playbackViewModel
                )
            }

            composable(Screen.ImageList.route) {
                ImageListScreen(mediaViewModel = mediaViewModel)
            }

            composable(Screen.VideoList.route) {
                VideoListScreen(
                    mediaViewModel = mediaViewModel,
                    navController = navController
                )
            }

            composable(
                route = Screen.VideoPlayer.route,
                arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uriEncoded = backStackEntry.arguments?.getString("uri")
                if (uriEncoded != null) {
                    // Decodificar la URI antes de usarla
                    val uri = Uri.decode(uriEncoded)
                    VideoPlayerScreen(
                        uri = uri,
                        playbackViewModel = playbackViewModel
                    )
                }
            }
        }
    }
}

// --- Componente de la Barra de Navegación ---
@Composable
fun AppBottomNavBar(
    navController: NavHostController,
    mediaViewModel: MediaViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Launchers para seleccionar archivos existentes
    val audioLauncher = createMediaLauncher(mediaViewModel, MediaType.AUDIO)
    val imageLauncherGallery = createMediaLauncher(mediaViewModel, MediaType.IMAGE)
    val videoLauncherGallery = createMediaLauncher(mediaViewModel, MediaType.VIDEO)

    NavigationBar {
        navBarItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = isSelected,
                onClick = {
                    // Navegación principal
                    if (screen.route != currentRoute) {
                        navController.navigate(screen.route) {
                            // Evitar la acumulación de destinos en la pila (top-level screens)
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // Si la pantalla ya está seleccionada, lanzar el selector de galería
                        when (screen) {
                            Screen.AudioList -> audioLauncher.launch("audio/*")
                            Screen.ImageList -> imageLauncherGallery.launch("image/*")
                            Screen.VideoList -> videoLauncherGallery.launch("video/*")
                            else -> { /* No hacer nada o manejar otra acción */ }
                        }
                    }
                }
            )
        }
    }
}

// Helper para crear un launcher que obtiene contenido y lo guarda en la BD
@Composable
private fun createMediaLauncher(
    mediaViewModel: MediaViewModel,
    type: MediaType
): ManagedActivityResultLauncher<String, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // Permite seleccionar cualquier tipo MIME
    ) { uri: Uri? ->
        uri?.let {
            mediaViewModel.insertMediaFromUri(it, type)
        }
    }
}
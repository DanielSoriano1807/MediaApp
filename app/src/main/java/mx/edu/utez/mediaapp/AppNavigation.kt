package mx.edu.utez.mediaapp

import android.app.Application
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
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

// IMPORTS CRÍTICOS
import mx.edu.utez.mediaapp.ui.data.MediaType
import mx.edu.utez.mediaapp.ui.data.AudioRecorder
import mx.edu.utez.mediaapp.ui.screens.AudioListScreen
import mx.edu.utez.mediaapp.ui.screens.ImageListScreen
import mx.edu.utez.mediaapp.ui.screens.RecordingScreen
import mx.edu.utez.mediaapp.ui.screens.VideoListScreen
import mx.edu.utez.mediaapp.ui.screens.VideoPlayerScreen
import mx.edu.utez.mediaapp.viewmodel.MediaViewModel
import mx.edu.utez.mediaapp.viewmodel.MediaViewModelFactory
import mx.edu.utez.mediaapp.viewmodel.PlaybackViewModel
import mx.edu.utez.mediaapp.viewmodel.PlaybackViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val audioRecorder = AudioRecorder(context)

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
                mediaViewModel = mediaViewModel
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Recording.route) {
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

@Composable
fun AppBottomNavBar(
    navController: NavHostController,
    mediaViewModel: MediaViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                    if (screen.route != currentRoute) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        when (screen) {
                            Screen.AudioList -> audioLauncher.launch("audio/*")
                            Screen.ImageList -> imageLauncherGallery.launch("image/*")
                            Screen.VideoList -> videoLauncherGallery.launch("video/*")
                            else -> { }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun createMediaLauncher(
    mediaViewModel: MediaViewModel,
    type: MediaType
): ManagedActivityResultLauncher<String, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { mediaViewModel.insertMediaFromUri(it, type) }
    }
}
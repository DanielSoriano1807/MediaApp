package mx.edu.utez.mediaapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import mx.edu.utez.mediaapp.ui.data.AudioRecorder
import mx.edu.utez.mediaapp.ui.data.MediaType
import mx.edu.utez.mediaapp.viewmodel.MediaViewModel
import java.io.File

@Composable
fun RecordingScreen(
    mediaViewModel: MediaViewModel,
    audioRecorder: AudioRecorder
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ESTADOS
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) } // Nuevo para fotos
    var currentAudioFile by remember { mutableStateOf<File?>(null) } // Nuevo para audio
    var isRecordingAudio by remember { mutableStateOf(false) }

    // Estado maestro de permisos
    var arePermissionsGranted by remember {
        mutableStateOf(checkAllPermissions(context))
    }

    // LISTA DINÁMICA DE PERMISOS
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    // Launcher de Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        arePermissionsGranted = checkAllPermissions(context)
    }

    // Launcher de Video
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempVideoUri?.let { uri ->
                mediaViewModel.insertMediaFromUri(uri, MediaType.VIDEO)
                Toast.makeText(context, "Video guardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher de Fotos (NUEVO)
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                mediaViewModel.insertMediaFromUri(uri, MediaType.IMAGE)
                Toast.makeText(context, "Foto guardada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Refresco de permisos al volver a la app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                arePermissionsGranted = checkAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Pide permisos al iniciar
    LaunchedEffect(Unit) {
        if (!arePermissionsGranted) permissionLauncher.launch(permissionsToRequest)
    }

    // --- INTERFAZ ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Centro de Grabación",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Botón Audio
        RecordingButton(
            text = if (isRecordingAudio) "DETENER AUDIO" else "GRABAR AUDIO",
            icon = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
            buttonColor = if (isRecordingAudio) Color.Red else MaterialTheme.colorScheme.primary,
            onClick = {
                if (checkAllPermissions(context)) {
                    if (isRecordingAudio) {
                        // DETENER Y GUARDAR
                        audioRecorder.stop()
                        currentAudioFile?.let { file ->
                            // ¡ESTA LÍNEA FALTABA! Guardar en BD
                            mediaViewModel.insertMediaFromFile(file, MediaType.AUDIO)
                            Toast.makeText(context, "Audio guardado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // INICIAR
                        val tempFile = createTempFile(context, MediaType.AUDIO)
                        currentAudioFile = tempFile // Guardamos referencia
                        audioRecorder.start(tempFile)
                    }
                    isRecordingAudio = !isRecordingAudio
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        // Botón Video
        RecordingButton(
            text = "GRABAR VIDEO",
            icon = Icons.Default.Videocam,
            isEnabled = !isRecordingAudio,
            buttonColor = MaterialTheme.colorScheme.tertiary,
            onClick = {
                if (checkAllPermissions(context)) {
                    val uri = createTempUri(context, MediaType.VIDEO)
                    tempVideoUri = uri
                    videoLauncher.launch(uri)
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        // Botón Foto
        RecordingButton(
            text = "TOMAR FOTO",
            icon = Icons.Default.PhotoCamera,
            isEnabled = !isRecordingAudio,
            buttonColor = MaterialTheme.colorScheme.secondary,
            onClick = {
                if (checkAllPermissions(context)) {
                    // LÓGICA DE FOTO IMPLEMENTADA
                    val uri = createTempUri(context, MediaType.IMAGE)
                    tempImageUri = uri
                    imageLauncher.launch(uri)
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        )
    }
}

// Helpers y Utilidades (Igual que antes)
private fun checkAllPermissions(context: Context): Boolean {
    val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun RecordingButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    buttonColor: Color
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        modifier = Modifier.fillMaxWidth().height(65.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

private fun createTempFile(context: Context, mediaType: MediaType): File {
    val dir = context.getExternalFilesDir(null)
    val prefix = when (mediaType) {
        MediaType.AUDIO -> "audio_"
        MediaType.IMAGE -> "image_"
        MediaType.VIDEO -> "video_"
    }
    val suffix = when (mediaType) {
        MediaType.AUDIO -> ".m4a"
        MediaType.IMAGE -> ".jpg"
        MediaType.VIDEO -> ".mp4"
    }
    return File.createTempFile(prefix, suffix, dir)
}

private fun createTempUri(context: Context, mediaType: MediaType): Uri {
    val file = createTempFile(context, mediaType)
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}
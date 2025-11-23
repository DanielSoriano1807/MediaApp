package mx.edu.utez.mediaapp.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ejemplo.mediaapp.data.AudioRecorder
import com.ejemplo.mediaapp.data.MediaType
import com.ejemplo.mediaapp.viewmodel.MediaViewModel
import java.io.File

// Lista de permisos requeridos para la grabación de audio, video y acceso a archivos
val requiredPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE // Para versiones antiguas
)

@Composable
fun RecordingScreen(
    mediaViewModel: MediaViewModel,
    audioRecorder: AudioRecorder // Inyectamos el helper
) {
    val context = LocalContext.current
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    // 1. Permisos
    var hasPermissions by remember { mutableStateOf(false) }

    // Launcher para solicitar los permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.entries.all { it.value }
    }

    // Comprobar permisos al inicio
    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        hasPermissions = allGranted
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }


    // 2. Launchers para Captura
    val imageLauncherCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Lógica para guardar el bitmap como archivo de imagen y luego usar mediaViewModel.insertMediaFromFile
        // (Simplificado aquí, se usaría un helper para guardar y obtener el File)
        if (bitmap != null) {
            // Nota: Aquí se necesitaría lógica de guardar a disco y luego usar insertMediaFromFile.
            // Para mantener la simplicidad, si la app solo necesita la Uri para guardar,
            // un launcher de captura de imágenes a URI es mejor. Usaremos la versión simple con Preview.
            // Se asume que el usuario luego seleccionará imágenes de galería para la lista de Imágenes.
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempVideoUri?.let { uri ->
                // Guardar el video capturado en la BD
                mediaViewModel.insertMediaFromUri(uri, MediaType.VIDEO)
            }
        }
        tempVideoUri = null // Limpiar
    }

    // Lógica de la UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Centro de Grabación",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (!hasPermissions) {
            Text("Se necesitan permisos de cámara, micrófono y almacenamiento para continuar.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(requiredPermissions) }) {
                Text("Otorga Permisos")
            }
        } else {
            // Botón de Grabación de Audio
            RecordingButton(
                text = if (isRecordingAudio) "Detener Audio" else "Grabar Audio",
                icon = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                onClick = {
                    if (isRecordingAudio) {
                        audioRecorder.stop()
                        // Aquí se debería obtener el archivo temporal y guardarlo
                        // (Lógica omitida en el snippet, se infiere una función de obtener el archivo temporal)
                    } else {
                        // Crear un archivo temporal para el audio y empezar la grabación
                        val tempFile = createTempFile(context, MediaType.AUDIO)
                        audioRecorder.start(tempFile)
                    }
                    isRecordingAudio = !isRecordingAudio
                },
                buttonColor = if (isRecordingAudio) Color.Red else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            // Botón de Captura de Imagen
            RecordingButton(
                text = "Capturar Foto",
                icon = Icons.Default.PhotoCamera,
                onClick = {
                    //imageLauncherCamera.launch(null) // Lanza el intent
                },
                isEnabled = !isRecordingAudio,
                buttonColor = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(16.dp))

            // Botón de Grabación de Video
            RecordingButton(
                text = "Grabar Video",
                icon = Icons.Default.Videocam,
                // Deshabilitar si ya estamos grabando audio
                isEnabled = !isRecordingAudio,
                onClick = {
                    val uri = createTempUri(context, MediaType.VIDEO)
                    tempVideoUri = uri
                    videoLauncher.launch(uri)
                },
                buttonColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// --- Helpers para la RecordingScreen ---
@Composable
private fun RecordingButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    buttonColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text)
    }
}

// Helper para crear un archivo temporal en la carpeta de archivos externos de la app
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
    return File.createTempFile(
        prefix,
        suffix,
        dir
    )
}

// Helper para obtener la URI content:// segura para FileProvider
private fun createTempUri(context: Context, mediaType: MediaType): Uri {
    val file = createTempFile(context, mediaType)
    val authority = "${context.packageName}.fileprovider"
    return FileProvider.getUriForFile(context, authority, file)
}
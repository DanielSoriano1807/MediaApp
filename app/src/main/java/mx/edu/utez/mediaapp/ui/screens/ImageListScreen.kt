package mx.edu.utez.mediaapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ejemplo.mediaapp.data.MediaItem
import com.ejemplo.mediaapp.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageListScreen(
    mediaViewModel: MediaViewModel
) {
    val imageList by mediaViewModel.allImages.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Mis Imágenes") })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            items(imageList) { item ->
                ImageCard(item = item, onClick = {
                    // Lógica para ver la imagen en detalle (opcional)
                })
            }
        }
    }
}

@Composable
fun ImageCard(item: MediaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f) // Mantener proporción cuadrada
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        // Uso de Coil para cargar la imagen desde la URI
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
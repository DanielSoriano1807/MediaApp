package mx.edu.utez.mediaapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definimos SOLAMENTE el esquema oscuro para forzar tu diseño propio
private val CustomColorScheme = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = DarkBackground,
    secondary = NeonSecondary,
    onSecondary = TextWhite,
    tertiary = NeonTertiary,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun MediaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivamos el color dinámico para que SIEMPRE se vea tu diseño neón
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Usamos siempre CustomColorScheme para garantizar el estilo visual propio
    val colorScheme = CustomColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Pinta la barra de estado del color de fondo
            window.statusBarColor = colorScheme.background.toArgb()
            // Iconos claros en la barra de estado
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Llama al archivo Type.kt
        content = content
    )
}
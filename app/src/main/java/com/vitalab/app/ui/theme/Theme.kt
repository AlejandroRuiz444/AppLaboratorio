package com.vitalab.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EsquemaClaro = lightColorScheme(
    primary = Turquesa40,
    onPrimary = Color.White,
    primaryContainer = Turquesa90,
    onPrimaryContainer = Turquesa10,
    secondary = Azul40,
    onSecondary = Color.White,
    secondaryContainer = Azul90,
    onSecondaryContainer = Azul10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Coral10,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = FondoClaro,
    onSurface = TextoClaro,
    surfaceVariant = VarianteClaro,
    onSurfaceVariant = TextoVarianteClaro,
    surfaceContainer = ContenedorClaro,
    surfaceContainerHighest = Color.White
)

private val EsquemaOscuro = darkColorScheme(
    primary = Turquesa80,
    onPrimary = Turquesa10,
    primaryContainer = Turquesa30,
    onPrimaryContainer = Turquesa90,
    secondary = Azul80,
    onSecondary = Azul10,
    secondaryContainer = Azul30,
    onSecondaryContainer = Azul90,
    tertiary = Coral80,
    onTertiary = Coral10,
    tertiaryContainer = Coral30,
    onTertiaryContainer = Coral90,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = TextoVarianteClaro,
    onSurfaceVariant = TextoVarianteOscuro,
    surfaceContainer = ContenedorOscuro,
    surfaceContainerHighest = TarjetaOscura
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun VitaLabTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}

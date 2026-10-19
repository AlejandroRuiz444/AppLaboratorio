package com.vitalab.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vitalab.app.R

// Poppins: tipografía redondeada y legible, da un tono cercano a la marca
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun estilo(peso: FontWeight, tamano: Int, interlineado: Int) =
    TextStyle(fontFamily = Poppins, fontWeight = peso, fontSize = tamano.sp, lineHeight = interlineado.sp)

val Tipografia = Typography(
    headlineMedium = estilo(FontWeight.Bold, 26, 34),
    headlineSmall = estilo(FontWeight.SemiBold, 22, 30),
    titleLarge = estilo(FontWeight.SemiBold, 20, 28),
    titleMedium = estilo(FontWeight.SemiBold, 16, 24),
    titleSmall = estilo(FontWeight.Medium, 14, 20),
    bodyLarge = estilo(FontWeight.Normal, 16, 24),
    bodyMedium = estilo(FontWeight.Normal, 14, 20),
    bodySmall = estilo(FontWeight.Normal, 12, 16),
    labelLarge = estilo(FontWeight.Medium, 14, 20),
    labelMedium = estilo(FontWeight.Medium, 12, 16),
    labelSmall = estilo(FontWeight.Medium, 11, 16)
)

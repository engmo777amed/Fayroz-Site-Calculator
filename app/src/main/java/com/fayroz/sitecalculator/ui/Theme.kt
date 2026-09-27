package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ExecutiveNavy = Color(0xFF04131D)
val ExecutiveNavySoft = Color(0xFF071E2C)
val ExecutiveTeal = Color(0xFF22CDB9)
val ExecutiveGold = Color(0xFFD6A84B)
val ExecutiveCanvas = Color(0xFFF2F6F7)
val ExecutiveInk = Color(0xFF17212B)

private val LightColors = lightColorScheme(
    primary = Color(0xFF22CDB9),
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFFD8F5F1),
    onPrimaryContainer = Color(0xFF063832),
    secondary = Color(0xFFD6A84B),
    onSecondary = Color(0xFF04131D),
    secondaryContainer = Color(0xFFF5EAD0),
    onSecondaryContainer = Color(0xFF493817),
    tertiary = Color(0xFF56D6AF),
    onTertiary = Color(0xFF04131D),
    tertiaryContainer = Color(0xFFDDF5EC),
    onTertiaryContainer = Color(0xFF123A31),
    background = Color(0xFFF2F6F7),
    onBackground = Color(0xFF17212B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFE8F0F2),
    onSurfaceVariant = Color(0xFF586D76),
    outline = Color(0xFF9BB0B9),
    outlineVariant = Color(0xFFD1DEE3),
    error = Color(0xFFD84C55),
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E5),
    onErrorContainer = Color(0xFF4A1318)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF22CDB9),
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF0B3A3A),
    onPrimaryContainer = Color(0xFF68E6D7),
    secondary = Color(0xFFD6A84B),
    onSecondary = Color(0xFF04131D),
    secondaryContainer = Color(0xFF382C18),
    onSecondaryContainer = Color(0xFFF0D38D),
    tertiary = Color(0xFF56D6AF),
    onTertiary = Color(0xFF04131D),
    tertiaryContainer = Color(0xFF123A31),
    onTertiaryContainer = Color(0xFFB8F3DD),
    background = Color(0xFF04131D),
    onBackground = Color(0xFFF3F7F8),
    surface = Color(0xFF091F2B),
    onSurface = Color(0xFFF3F7F8),
    surfaceVariant = Color(0xFF0D2937),
    onSurfaceVariant = Color(0xFF9CB1BB),
    surfaceTint = Color(0xFF22CDB9),
    outline = Color(0xFF416070),
    outlineVariant = Color(0xFF203E4D),
    error = Color(0xFFFF7E86),
    onError = Color(0xFF04131D),
    errorContainer = Color(0xFF402329),
    onErrorContainer = Color(0xFFFFDADD),
    scrim = Color.Black
)

enum class FayrozAppearance { System, Light, Dark }

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Black),
    headlineMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Black),
    headlineSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=19.sp,lineHeight=24.sp,fontWeight=FontWeight.ExtraBold),
    titleLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=18.sp,lineHeight=23.sp,fontWeight=FontWeight.ExtraBold),
    titleMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold),
    titleSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium),
    bodyMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Normal),
    bodySmall = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=11.sp,lineHeight=16.sp,fontWeight=FontWeight.Normal),
    labelLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=17.sp,fontWeight=FontWeight.Bold),
    labelMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold),
    labelSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.SemiBold)
)

@Composable
fun FayrozSiteTheme(
    appearance:FayrozAppearance = FayrozAppearance.System,
    content:@Composable ()->Unit
){
    val dark=when(appearance){
        FayrozAppearance.System -> isSystemInDarkTheme()
        FayrozAppearance.Light -> false
        FayrozAppearance.Dark -> true
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
        MaterialTheme(
            colorScheme=if(dark) DarkColors else LightColors,
            typography=AppTypography,
            shapes=Shapes(
                extraSmall=RoundedCornerShape(10.dp),
                small=RoundedCornerShape(14.dp),
                medium=RoundedCornerShape(18.dp),
                large=RoundedCornerShape(24.dp),
                extraLarge=RoundedCornerShape(30.dp)
            ),
            content=content
        )
    }
}
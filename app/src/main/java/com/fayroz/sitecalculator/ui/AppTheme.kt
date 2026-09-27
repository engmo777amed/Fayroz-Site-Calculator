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

val FayrozNavy = Color(0xFF04131D)
val FayrozNavy2 = Color(0xFF071E2C)
val FayrozNavy3 = Color(0xFF0D2937)
val FayrozTurquoise = Color(0xFF22CDB9)
val FayrozTurquoiseLight = Color(0xFF68E6D7)
val FayrozGold = Color(0xFFD6A84B)
val FayrozGoldLight = Color(0xFFF0D38D)
val FayrozGreen = Color(0xFF56D6AF)
val FayrozRed = Color(0xFFFF7E86)

enum class Appearance { SYSTEM, LIGHT, DARK }

private val Dark = darkColorScheme(
    primary=FayrozTurquoise,
    onPrimary=FayrozNavy,
    primaryContainer=Color(0xFF0B3A3A),
    onPrimaryContainer=FayrozTurquoiseLight,
    secondary=FayrozGold,
    onSecondary=FayrozNavy,
    secondaryContainer=Color(0xFF382C18),
    onSecondaryContainer=FayrozGoldLight,
    tertiary=FayrozGreen,
    onTertiary=FayrozNavy,
    tertiaryContainer=Color(0xFF123A31),
    onTertiaryContainer=Color(0xFFB8F3DD),
    background=FayrozNavy,
    onBackground=Color(0xFFF3F7F8),
    surface=Color(0xFF091F2B),
    onSurface=Color(0xFFF3F7F8),
    surfaceVariant=FayrozNavy3,
    onSurfaceVariant=Color(0xFF9CB1BB),
    outline=Color(0xFF416070),
    outlineVariant=Color(0xFF203E4D),
    error=FayrozRed,
    onError=FayrozNavy,
    errorContainer=Color(0xFF402329),
    onErrorContainer=Color(0xFFFFDADD)
)

private val Light = lightColorScheme(
    primary=Color(0xFF0B8D83),
    onPrimary=Color.White,
    primaryContainer=Color(0xFFD5F5F0),
    onPrimaryContainer=Color(0xFF063A35),
    secondary=Color(0xFF9A6E17),
    onSecondary=Color.White,
    secondaryContainer=Color(0xFFF6E7BE),
    onSecondaryContainer=Color(0xFF3F2F0E),
    tertiary=Color(0xFF247F67),
    onTertiary=Color.White,
    tertiaryContainer=Color(0xFFD8F1E8),
    onTertiaryContainer=Color(0xFF143B31),
    background=Color(0xFFF2F6F7),
    onBackground=Color(0xFF142028),
    surface=Color.White,
    onSurface=Color(0xFF142028),
    surfaceVariant=Color(0xFFE7EEF1),
    onSurfaceVariant=Color(0xFF536872),
    outline=Color(0xFF8CA2AC),
    outlineVariant=Color(0xFFD0DDE2),
    error=Color(0xFFB93F4B),
    onError=Color.White,
    errorContainer=Color(0xFFFBE1E4),
    onErrorContainer=Color(0xFF4A1018)
)

private val TypographyV6=Typography(
    headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=24.sp,lineHeight=30.sp,fontWeight=FontWeight.Black),
    headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=20.sp,lineHeight=25.sp,fontWeight=FontWeight.Black),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=18.sp,lineHeight=23.sp,fontWeight=FontWeight.ExtraBold),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=15.sp,lineHeight=20.sp,fontWeight=FontWeight.Bold),
    titleSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=19.sp,fontWeight=FontWeight.Normal),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=11.sp,lineHeight=16.sp,fontWeight=FontWeight.Normal),
    labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=17.sp,fontWeight=FontWeight.Bold),
    labelSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.SemiBold)
)

@Composable
fun FayrozTheme(appearance:Appearance,content:@Composable ()->Unit){
    val dark=when(appearance){
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
        MaterialTheme(
            colorScheme=if(dark)Dark else Light,
            typography=TypographyV6,
            shapes=Shapes(
                extraSmall=RoundedCornerShape(8.dp),
                small=RoundedCornerShape(12.dp),
                medium=RoundedCornerShape(16.dp),
                large=RoundedCornerShape(22.dp),
                extraLarge=RoundedCornerShape(28.dp)
            ),
            content=content
        )
    }
}

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

val NavyDeep=Color(0xFF04131D)
val Navy=Color(0xFF071E2C)
val Turquoise=Color(0xFF22CDB9)
val TurquoiseBright=Color(0xFF68E6D7)
val Gold=Color(0xFFD6A84B)
val GoldLight=Color(0xFFF0D38D)

enum class Appearance{SYSTEM,LIGHT,DARK}

private val dark=darkColorScheme(
    primary=Turquoise,onPrimary=NavyDeep,primaryContainer=Color(0xFF0B3A3A),onPrimaryContainer=TurquoiseBright,
    secondary=Gold,onSecondary=NavyDeep,secondaryContainer=Color(0xFF382C18),onSecondaryContainer=GoldLight,
    tertiary=Color(0xFF56D6AF),onTertiary=NavyDeep,background=NavyDeep,onBackground=Color(0xFFF3F7F8),
    surface=Color(0xFF091F2B),onSurface=Color(0xFFF3F7F8),surfaceVariant=Color(0xFF0D2937),onSurfaceVariant=Color(0xFF9CB1BB),
    outline=Color(0xFF416070),outlineVariant=Color(0xFF203E4D),error=Color(0xFFFF7E86),errorContainer=Color(0xFF402329),onErrorContainer=Color(0xFFFFDADD)
)
private val light=lightColorScheme(
    primary=Color(0xFF0B8D83),onPrimary=Color.White,primaryContainer=Color(0xFFD5F5F0),onPrimaryContainer=Color(0xFF063A35),
    secondary=Color(0xFF9A6E17),onSecondary=Color.White,secondaryContainer=Color(0xFFF6E7BE),onSecondaryContainer=Color(0xFF3F2F0E),
    background=Color(0xFFF2F6F7),onBackground=Color(0xFF142028),surface=Color.White,onSurface=Color(0xFF142028),surfaceVariant=Color(0xFFE7EEF1),onSurfaceVariant=Color(0xFF536872),
    outline=Color(0xFF8CA2AC),outlineVariant=Color(0xFFD0DDE2),error=Color(0xFFB93F4B),errorContainer=Color(0xFFFBE1E4),onErrorContainer=Color(0xFF4A1018)
)
private val typography=Typography(
    headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Black,fontSize=24.sp,lineHeight=30.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Black,fontSize=18.sp),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=15.sp),
    titleSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=13.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=13.sp,lineHeight=18.sp),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=11.sp,lineHeight=16.sp),
    labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=13.sp),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=12.sp),
    labelSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=10.sp)
)

@Composable
fun FayrozTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val useDark=when(appearance){Appearance.SYSTEM->isSystemInDarkTheme();Appearance.LIGHT->false;Appearance.DARK->true}
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
        MaterialTheme(
            colorScheme=if(useDark)dark else light,
            typography=typography,
            shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(22.dp)),
            content=content
        )
    }
}

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FayrozNavy=Color(0xFF04131D)
val FayrozNavy2=Color(0xFF071E2C)
val FayrozTurquoise=Color(0xFF22CDB9)
val FayrozGold=Color(0xFFD6A84B)

private val DarkScheme=darkColorScheme(
    primary=FayrozTurquoise,
    secondary=FayrozGold,
    background=FayrozNavy,
    surface=FayrozNavy2,
    primaryContainer=Color(0xFF12353F),
    secondaryContainer=Color(0xFF3E3420)
)

private val LightScheme=lightColorScheme(
    primary=Color(0xFF006C63),
    secondary=Color(0xFF8A6417),
    background=Color(0xFFF4F8F8),
    surface=Color.White,
    primaryContainer=Color(0xFFD7F5EF),
    secondaryContainer=Color(0xFFFFE8B2)
)

@Composable
fun FayrozTheme(mode:String,content:@Composable ()->Unit){
    val dark=when(mode){
        "dark"->true
        "light"->false
        else->isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme=if(dark)DarkScheme else LightScheme,
        typography=Typography(),
        content=content
    )
}


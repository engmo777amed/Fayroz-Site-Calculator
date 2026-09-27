package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

val FayrozDeepNavy = Color(0xFF04131D)
val FayrozNavy = Color(0xFF071E2C)
val FayrozGold = Color(0xFFD6A84B)
val FayrozGoldLight = Color(0xFFF0D38D)
val FayrozGoldDark = Color(0xFF8F6120)
val FayrozInk = Color(0xFF16191D)
val FayrozStone = Color(0xFFE8DFD1)

/**
 * Approved Fayroz architectural F. The whole mark stays inside a safe area
 * so launcher masks and compact UI placements do not crop any part.
 */
@Composable
fun FayrozMark(modifier: Modifier = Modifier, monochrome:Boolean=false){
    Canvas(modifier){
        val w=size.width
        val h=size.height
        val dark=if(monochrome) Color.White else FayrozInk
        val bronze=if(monochrome) Color.White else FayrozGold
        val bronzeDark=if(monochrome) Color.White else FayrozGoldDark
        val stone=if(monochrome) Color.White else FayrozStone

        drawPath(Path().apply{
            moveTo(w*.14f,h*.88f); lineTo(w*.14f,h*.55f); lineTo(w*.28f,h*.47f)
            lineTo(w*.28f,h*.33f); lineTo(w*.42f,h*.25f); lineTo(w*.42f,h*.12f)
            lineTo(w*.52f,h*.06f); lineTo(w*.52f,h*.88f); close()
        },dark)

        listOf(.27f,.305f,.34f,.375f).forEach{x->
            drawLine(bronze,Offset(w*x,h*.51f),Offset(w*x,h*.86f),(w*.014f).coerceAtLeast(1f))
        }

        drawPath(Path().apply{
            moveTo(w*.48f,h*.88f); lineTo(w*.48f,h*.11f); lineTo(w*.57f,h*.16f)
            lineTo(w*.57f,h*.88f); close()
        },stone)

        drawPath(Path().apply{
            moveTo(w*.52f,h*.06f); lineTo(w*.91f,h*.28f); lineTo(w*.87f,h*.36f)
            lineTo(w*.57f,h*.20f); lineTo(w*.57f,h*.16f); close()
        },Brush.linearGradient(listOf(bronzeDark,bronze,FayrozGoldLight)))

        val mid=Path().apply{
            moveTo(w*.57f,h*.38f); lineTo(w*.83f,h*.53f); lineTo(w*.78f,h*.62f)
            lineTo(w*.57f,h*.52f); close()
        }
        drawPath(mid,Brush.linearGradient(listOf(bronzeDark,bronze)))

        drawLine(bronzeDark,Offset(w*.08f,h*.89f),Offset(w*.92f,h*.89f),(h*.018f).coerceAtLeast(1f),StrokeCap.Round)
        if(!monochrome){
            drawLine(Color.White.copy(alpha=.42f),Offset(w*.53f,h*.10f),Offset(w*.87f,h*.29f),(w*.009f).coerceAtLeast(1f),StrokeCap.Round)
            drawPath(mid,Color.Black.copy(alpha=.20f),style=Stroke(width=(w*.009f).coerceAtLeast(1f)))
        }
    }
}

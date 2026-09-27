package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FayrozMark(modifier:Modifier=Modifier,monochrome:Boolean=false){
    val cyan=if(monochrome) Color.White else TurquoiseBright
    val gold=if(monochrome) Color.White else GoldLight
    Canvas(modifier){
        val w=size.width
        val h=size.height
        drawRoundRect(
            color=if(monochrome) Color.Transparent else NavyDeep,
            cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.18f,w*.18f)
        )
        drawRect(gold,Offset(w*.17f,h*.14f),Size(w*.16f,h*.72f))
        drawRoundRect(cyan,Offset(w*.30f,h*.14f),Size(w*.52f,h*.16f),androidx.compose.ui.geometry.CornerRadius(w*.07f,w*.07f))
        drawRoundRect(gold,Offset(w*.30f,h*.42f),Size(w*.40f,h*.14f),androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f))
        drawRect(cyan,Offset(w*.62f,h*.62f),Size(w*.08f,h*.22f))
        drawRect(gold,Offset(w*.73f,h*.53f),Size(w*.08f,h*.31f))
        drawRect(cyan,Offset(w*.84f,h*.43f),Size(w*.08f,h*.41f))
    }
}

@Composable
fun BrandHero(
    projectCount:Int,
    roomCount:Int,
    modifier:Modifier=Modifier
){
    Surface(
        modifier=modifier.fillMaxWidth(),
        color=NavyDeep,
        shape=RoundedCornerShape(22.dp),
        shadowElevation=6.dp
    ){
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(NavyDeep,Navy,NavySoft)))
                .padding(horizontal=14.dp,vertical=12.dp)
        ){
            Canvas(Modifier.matchParentSize()){
                val grid=Turquoise.copy(alpha=.08f)
                val step=size.width/11f
                var x=0f
                while(x<size.width){
                    drawLine(grid,Offset(x,0f),Offset(x,size.height),1f)
                    x+=step
                }
                var y=0f
                while(y<size.height){
                    drawLine(grid,Offset(0f,y),Offset(size.width,y),1f)
                    y+=step
                }
            }
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Surface(
                        shape=RoundedCornerShape(15.dp),
                        color=Color.White.copy(alpha=.07f),
                        border=androidx.compose.foundation.BorderStroke(1.dp,Turquoise.copy(alpha=.26f))
                    ){
                        Box(Modifier.size(52.dp).padding(7.dp),contentAlignment=Alignment.Center){
                            FayrozMark(Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(
                            "FAYROZ SITE CALCULATOR",
                            color=Color.White,
                            fontSize=17.sp,
                            fontWeight=FontWeight.Black,
                            letterSpacing=.4.sp
                        )
                        Text(
                            "حاسبة كميات الموقع",
                            color=TurquoiseBright,
                            style=MaterialTheme.typography.labelLarge
                        )
                    }
                    Surface(shape=RoundedCornerShape(999.dp),color=GoldContainer){
                        Text("V5",Modifier.padding(horizontal=9.dp,vertical=5.dp),color=GoldLight,style=MaterialTheme.typography.labelMedium)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    HeroStat(Icons.Rounded.GridOn,"المشروعات",projectCount.toString(),Modifier.weight(1f))
                    HeroStat(Icons.Rounded.Straighten,"الغرف",roomCount.toString(),Modifier.weight(1f))
                    HeroStat(Icons.Rounded.Calculate,"الوضع","موقع",Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroStat(
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    label:String,
    value:String,
    modifier:Modifier=Modifier
){
    Surface(
        modifier=modifier,
        color=Color.White.copy(alpha=.06f),
        shape=RoundedCornerShape(13.dp)
    ){
        Row(
            Modifier.padding(horizontal=8.dp,vertical=7.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Icon(icon,null,tint=GoldLight,modifier=Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Column{
                Text(label,color=Color.White.copy(alpha=.68f),style=MaterialTheme.typography.labelSmall)
                Text(value,color=Color.White,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
            }
        }
    }
}

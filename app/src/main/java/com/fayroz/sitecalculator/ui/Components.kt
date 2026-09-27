@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.R
import java.util.Locale

fun parseNum(v:String):Double=v.replace("٫",".").replace(",",".").toDoubleOrNull()?:0.0
fun fmt(v:Double):String=String.format(Locale.US,"%.2f",v)

@Composable
fun HeroHeader(projects:Int,rooms:Int){
    Surface(shape=RoundedCornerShape(22.dp),color=NavyDeep,shadowElevation=5.dp){
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(NavyDeep,Navy,Color(0xFF0D2D3E)))
            ).padding(12.dp)
        ){
            Row(verticalAlignment=Alignment.CenterVertically){
                Surface(shape=RoundedCornerShape(18.dp),color=Color.White.copy(alpha=.05f),border=BorderStroke(1.dp,Turquoise.copy(alpha=.28f))){
                    androidx.compose.foundation.Image(
                        painter=painterResource(R.drawable.fayroz_site_icon),
                        contentDescription=null,
                        modifier=Modifier.size(64.dp).padding(5.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)){
                    Text("FAYROZ",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                    Text("SITE CALCULATOR",color=GoldLight,style=MaterialTheme.typography.labelLarge)
                    Text("حاسبة كميات الموقع",color=TurquoiseBright,style=MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        StatChip(Icons.Rounded.FolderOpen,"$projects مشروع")
                        StatChip(Icons.Rounded.MeetingRoom,"$rooms غرفة")
                    }
                }
            }
        }
    }
}

@Composable private fun StatChip(icon:ImageVector,text:String){
    Surface(shape=RoundedCornerShape(999.dp),color=Color.White.copy(alpha=.07f)){
        Row(Modifier.padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(icon,null,Modifier.size(14.dp),tint=GoldLight)
            Spacer(Modifier.width(4.dp))
            Text(text,color=Color.White,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun QuickCard(title:String,subtitle:String,icon:ImageVector,accent:Boolean=false,onClick:()->Unit,modifier:Modifier=Modifier){
    val tone=if(accent)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(modifier=modifier,onClick=onClick,shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,tone.copy(alpha=.35f)),shadowElevation=1.dp){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(12.dp),color=if(accent)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(8.dp).size(20.dp),tint=tone)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)){
                Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=1)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
            Icon(Icons.Rounded.ChevronLeft,null,Modifier.size(18.dp),tint=tone)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(4.dp).height(if(subtitle==null)22.dp else 34.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(9.dp)))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
            if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
        trailing?.invoke()
    }
}

@Composable
fun CardBox(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=1.dp){
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
    }
}

@Composable
fun Txt(label:String,value:String,onValue:(String)->Unit,placeholder:String="",help:String?=null,modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldLabel(label,help)
        OutlinedTextField(
            value=value,onValueChange=onValue,modifier=Modifier.fillMaxWidth(),singleLine=true,
            placeholder={if(placeholder.isNotBlank())Text(placeholder,maxLines=1)},shape=RoundedCornerShape(11.dp)
        )
    }
}

@Composable
fun Num(label:String,value:String,onValue:(String)->Unit,unit:String="",help:String?=null,modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldLabel(label,help)
        OutlinedTextField(
            value=value,
            onValueChange={onValue(it.filter{c->c.isDigit()||c=='.'||c==','||c=='٫'})},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            trailingIcon={if(unit.isNotBlank())Text(unit,style=MaterialTheme.typography.labelSmall)},
            shape=RoundedCornerShape(11.dp),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)
        )
    }
}

@Composable private fun FieldLabel(label:String,help:String?){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(help!=null)Help(label,help)
    }
}

@Composable
fun Help(title:String,text:String){
    var open by remember{mutableStateOf(false)}
    IconButton(onClick={open=true},modifier=Modifier.size(25.dp)){
        Icon(Icons.Rounded.Info,"شرح",Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)
    }
    if(open)AlertDialog(
        onDismissRequest={open=false},
        title={Text(title,fontWeight=FontWeight.Black)},
        text={Text(text)},
        confirmButton={TextButton(onClick={open=false}){Text("فهمت")}}
    )
}

@Composable
fun Choice(label:String,value:String,options:List<String>,onValue:(String)->Unit,help:String?=null,modifier:Modifier=Modifier){
    var open by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldLabel(label,help)
        Box{
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),shape=RoundedCornerShape(11.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=7.dp)){
                Text(value,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                Icon(Icons.Rounded.KeyboardArrowDown,null)
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}){
                options.forEach{o->DropdownMenuItem(text={Text(o)},onClick={onValue(o);open=false})}
            }
        }
    }
}

@Composable
fun Tabs(labels:List<String>,selected:Int,onSelect:(Int)->Unit){
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
        Row(Modifier.fillMaxWidth().padding(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
            labels.forEachIndexed{i,label->
                Surface(modifier=Modifier.weight(1f),onClick={onSelect(i)},shape=RoundedCornerShape(10.dp),color=if(i==selected)MaterialTheme.colorScheme.primaryContainer else Color.Transparent){
                    Box(Modifier.padding(vertical=7.dp,horizontal=3.dp),contentAlignment=Alignment.Center){
                        Text(label,style=MaterialTheme.typography.labelMedium,color=if(i==selected)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                    }
                }
            }
        }
    }
}

@Composable
fun QtyLine(label:String,value:Double,unit:String,strong:Boolean=false){
    Surface(color=if(strong)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,shape=RoundedCornerShape(10.dp)){
        Row(Modifier.fillMaxWidth().padding(horizontal=if(strong)8.dp else 0.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=if(strong)MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            Text("${fmt(value)} $unit",style=if(strong)MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
fun AppearanceBar(current:Appearance,onChange:(Appearance)->Unit){
    Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
        Row(Modifier.fillMaxWidth().padding(3.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)){
            listOf(
                Triple(Appearance.DARK,"داكن",Icons.Rounded.DarkMode),
                Triple(Appearance.LIGHT,"فاتح",Icons.Rounded.LightMode),
                Triple(Appearance.SYSTEM,"الهاتف",Icons.Rounded.SettingsBrightness)
            ).forEach{(m,t,i)->
                Surface(modifier=Modifier.weight(1f),onClick={onChange(m)},shape=RoundedCornerShape(10.dp),color=if(current==m)MaterialTheme.colorScheme.primaryContainer else Color.Transparent){
                    Row(Modifier.padding(vertical=7.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
                        Icon(i,null,Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t,style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AppBar(title: String, subtitle: String = "", onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title={
            Column{
                Text(title,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
        },
        navigationIcon={if(onBack!=null)IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
        actions=actions,
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface)
    )
}

@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

fun n(text:String)=text.replace("٫",".").replace(",",".").toDoubleOrNull()?:0.0
fun fmt(v:Double)=String.format(Locale.US,"%.2f",v)

@Composable
fun BrandHeader(
    title:String,
    subtitle:String,
    stats:List<Pair<String,String>> = emptyList()
){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(24.dp),
        color=FayrozNavy,
        shadowElevation=7.dp
    ){
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(FayrozNavy,FayrozNavy2,FayrozNavy3)))
                .padding(horizontal=14.dp,vertical=14.dp)
        ){
            Column(verticalArrangement=Arrangement.spacedBy(11.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Surface(shape=RoundedCornerShape(16.dp),color=Color.White.copy(alpha=.06f),border=BorderStroke(1.dp,FayrozTurquoise.copy(alpha=.26f))){
                        Box(Modifier.size(50.dp),contentAlignment=Alignment.Center){
                            Icon(Icons.Rounded.Calculate,null,tint=FayrozGoldLight,modifier=Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)){
                        Text(title,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
                        Text(subtitle,color=FayrozTurquoiseLight,style=MaterialTheme.typography.bodySmall)
                    }
                }
                if(stats.isNotEmpty()){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        stats.take(3).forEach{(label,value)->
                            Surface(
                                modifier=Modifier.weight(1f),
                                shape=RoundedCornerShape(13.dp),
                                color=Color.White.copy(alpha=.055f)
                            ){
                                Column(Modifier.padding(horizontal=8.dp,vertical=7.dp)){
                                    Text(label,color=Color.White.copy(alpha=.65f),style=MaterialTheme.typography.labelSmall)
                                    Text(value,color=Color.White,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black,maxLines=1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTile(
    title:String,
    subtitle:String,
    icon:ImageVector,
    onClick:()->Unit,
    accent:Boolean=false,
    modifier:Modifier=Modifier
){
    val tone=if(accent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val container=if(accent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    Surface(
        modifier=modifier,
        onClick=onClick,
        shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,tone.copy(alpha=.32f)),
        shadowElevation=2.dp
    ){
        Column(
            Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Surface(shape=RoundedCornerShape(12.dp),color=container){
                Icon(icon,null,tint=tone,modifier=Modifier.padding(8.dp).size(22.dp))
            }
            Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=1)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
    }
}

@Composable
fun CardBox(content:@Composable ColumnScope.()->Unit){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(17.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        shadowElevation=1.dp
    ){
        Column(Modifier.padding(horizontal=11.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
    }
}

@Composable
fun SectionTitle(title:String,subtitle:String?=null,trailing:(@Composable ()->Unit)?=null){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(4.dp).height(if(subtitle==null)22.dp else 36.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
            if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
        trailing?.invoke()
    }
}

@Composable
fun HelpIcon(title:String,text:String){
    var show by remember{mutableStateOf(false)}
    IconButton(onClick={show=true},modifier=Modifier.size(28.dp)){
        Icon(Icons.Rounded.Info,"شرح",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
    }
    if(show){
        AlertDialog(
            onDismissRequest={show=false},
            title={Text(title,fontWeight=FontWeight.Black)},
            text={Text(text)},
            confirmButton={TextButton(onClick={show=false}){Text("إغلاق")}}
        )
    }
}

@Composable
fun LabeledText(
    label:String,
    value:String,
    onChange:(String)->Unit,
    placeholder:String="",
    help:String?=null,
    modifier:Modifier=Modifier
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(help!=null) HelpIcon(label,help)
        }
        OutlinedTextField(
            value=value,
            onValueChange=onChange,
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            placeholder={if(placeholder.isNotEmpty())Text(placeholder,maxLines=1)},
            shape=RoundedCornerShape(11.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.38f)
            )
        )
    }
}

@Composable
fun LabeledNumber(
    label:String,
    value:String,
    onChange:(String)->Unit,
    unit:String,
    help:String?=null,
    modifier:Modifier=Modifier
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(help!=null) HelpIcon(label,help)
        }
        OutlinedTextField(
            value=value,
            onValueChange={raw->onChange(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'})},
            modifier=Modifier.fillMaxWidth(),
            singleLine=true,
            suffix={Text(unit,style=MaterialTheme.typography.labelSmall)},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            shape=RoundedCornerShape(11.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.38f)
            )
        )
    }
}

@Composable
fun ChoiceField(
    label:String,
    value:String,
    options:List<String>,
    onChange:(String)->Unit,
    help:String?=null,
    modifier:Modifier=Modifier
){
    var open by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(help!=null) HelpIcon(label,help)
        }
        Box{
            OutlinedButton(
                onClick={open=true},
                modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),
                shape=RoundedCornerShape(11.dp),
                contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.38f))
            ){
                Text(value,Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Icon(Icons.Rounded.ExpandMore,null)
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}){
                options.forEach{opt->
                    DropdownMenuItem(text={Text(opt)},onClick={open=false;onChange(opt)})
                }
            }
        }
    }
}

@Composable
fun MetricRow(label:String,value:String,highlight:Boolean=false){
    Surface(
        color=if(highlight)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape=RoundedCornerShape(10.dp)
    ){
        Row(Modifier.fillMaxWidth().padding(horizontal=if(highlight)8.dp else 0.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=if(highlight)MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            Text(value,style=if(highlight)MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
fun AppBar(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,actions:@Composable RowScope.()->Unit={}){
    TopAppBar(
        title={
            Column{
                Text(title,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
        },
        navigationIcon={if(onBack!=null)IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
        actions=actions
    )
}

@Composable
fun EmptyBlock(title:String,subtitle:String,icon:ImageVector=Icons.Rounded.Inbox){
    CardBox{
        Row(verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(8.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Column{
                Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

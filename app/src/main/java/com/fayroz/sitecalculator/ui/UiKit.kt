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
fun CompactBrandHeader(
    title:String="FAYROZ SITE CALCULATOR",
    subtitle:String="حاسبة كميات الموقع",
    stats:List<Pair<String,String>> = emptyList()
){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(20.dp),
        color=NavyDeep,
        shadowElevation=5.dp
    ){
        Column(
            Modifier.background(Brush.horizontalGradient(listOf(NavyDeep,Navy,NavySoft)))
                .padding(horizontal=13.dp,vertical=11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Row(verticalAlignment=Alignment.CenterVertically){
                Surface(
                    shape=RoundedCornerShape(13.dp),
                    color=Color.White.copy(alpha=.06f),
                    border=BorderStroke(1.dp,Turquoise.copy(alpha=.25f))
                ){
                    Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){
                        androidx.compose.foundation.Image(
                            painter=androidx.compose.ui.res.painterResource(com.fayroz.sitecalculator.R.drawable.ic_site_foreground),
                            contentDescription=null,
                            modifier=Modifier.fillMaxSize().padding(3.dp)
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)){
                    Text(title,color=Color.White,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(subtitle,color=TurquoiseLight,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
            if(stats.isNotEmpty()){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    stats.take(3).forEach{(label,value)->
                        Surface(Modifier.weight(1f),shape=RoundedCornerShape(11.dp),color=Color.White.copy(alpha=.055f)){
                            Column(Modifier.padding(horizontal=7.dp,vertical=6.dp)){
                                Text(label,color=Color.White.copy(alpha=.66f),style=MaterialTheme.typography.labelSmall,maxLines=1)
                                Text(value,color=Color.White,style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ScreenHeader(
    title:String,
    subtitle:String?=null,
    action: (@Composable () -> Unit)? = null
){
    Row(
        Modifier.fillMaxWidth().padding(horizontal=2.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun DashboardCard(
    title:String,subtitle:String,icon:ImageVector,onClick:()->Unit,
    accent:Boolean=false,modifier:Modifier=Modifier
){
    val tone=if(accent)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val container=if(accent)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    Surface(
        modifier=modifier.heightIn(min=122.dp),
        onClick=onClick,
        shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,tone.copy(alpha=.30f)),
        shadowElevation=2.dp
    ){
        Column(Modifier.fillMaxSize().padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Surface(shape=RoundedCornerShape(11.dp),color=container){
                Icon(icon,null,Modifier.padding(8.dp).size(21.dp),tint=tone)
            }
            Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=1)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
    }
}

@Composable
fun CardBox(content:@Composable ColumnScope.()->Unit){
    Surface(
        Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=1.dp
    ){
        Column(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
    }
}

@Composable
fun SectionTitle(title:String,subtitle:String?=null,trailing: (@Composable () -> Unit)? = null){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(4.dp).height(if(subtitle==null)22.dp else 34.dp).background(MaterialTheme.colorScheme.primary,RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
            if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
        trailing?.invoke()
    }
}

@Composable
fun CompactHelpIcon(title:String,help:String){
    var show by remember{mutableStateOf(false)}
    IconButton(onClick={show=true},modifier=Modifier.size(40.dp)){
        Surface(
            shape=RoundedCornerShape(999.dp),
            color=MaterialTheme.colorScheme.primaryContainer
        ){
            Box(Modifier.size(22.dp),contentAlignment=Alignment.Center){
                Text("!",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
            }
        }
    }
    if(show){
        AlertDialog(
            onDismissRequest={show=false},
            title={Text(title,fontWeight=FontWeight.Black)},
            text={Text(help)},
            confirmButton={TextButton(onClick={show=false}){Text("تمام")}}
        )
    }
}

@Composable
fun HelpButton(title:String,help:String)=CompactHelpIcon(title,help)

@Composable
fun TextFieldX(label:String,value:String,onChange:(String)->Unit,help:String?=null,placeholder:String="",modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=value,onValueChange=onChange,modifier=Modifier.fillMaxWidth(),singleLine=true,
            placeholder={if(placeholder.isNotBlank())Text(placeholder,maxLines=1)},
            trailingIcon={if(help!=null){{CompactHelpIcon(label,help)}}else null},
            shape=RoundedCornerShape(11.dp)
        )
    }
}

@Composable
fun NumberFieldX(label:String,value:String,onChange:(String)->Unit,unit:String,help:String?=null,modifier:Modifier=Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=value,onValueChange={raw->onChange(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'})},
            modifier=Modifier.fillMaxWidth(),singleLine=true,
            suffix={Text(unit,style=MaterialTheme.typography.labelSmall)},
            trailingIcon={if(help!=null){{CompactHelpIcon(label,help)}}else null},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            shape=RoundedCornerShape(11.dp)
        )
    }
}

@Composable
fun ChoiceFieldX(label:String,value:String,options:List<String>,onChange:(String)->Unit,help:String?=null,modifier:Modifier=Modifier){
    var open by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box{
            OutlinedButton(
                onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                shape=RoundedCornerShape(11.dp),contentPadding=PaddingValues(horizontal=10.dp,vertical=7.dp)
            ){
                Text(value,Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurface)
                if(help!=null)CompactHelpIcon(label,help)
                Icon(Icons.Rounded.ExpandMore,null)
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}){
                options.forEach{opt->DropdownMenuItem(text={Text(opt)},onClick={open=false;onChange(opt)})}
            }
        }
    }
}

@Composable
fun MetricRow(label:String,value:String,highlight:Boolean=false){
    Surface(
        color=if(highlight)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape=RoundedCornerShape(12.dp)
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=if(highlight)10.dp else 0.dp,vertical=if(highlight)8.dp else 5.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Text(label,Modifier.weight(1f),style=if(highlight)MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            Text(
                value,
                style=if(highlight)MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                color=MaterialTheme.colorScheme.primary,
                fontWeight=FontWeight.Black
            )
        }
    }
}

@Composable
fun AppBarX(title:String,subtitle:String?=null,onBack:(()->Unit)?=null,actions: @Composable RowScope.() -> Unit = {}){
    TopAppBar(
        title={Column{
            Text(title,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
            if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
        }},
        navigationIcon={if(onBack!=null)IconButton(onClick=onBack,modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
        actions=actions
    )
}

@Composable
fun EmptyBlock(title:String,subtitle:String,icon:ImageVector=Icons.Rounded.Inbox){
    CardBox{
        Row(verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.primaryContainer){
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

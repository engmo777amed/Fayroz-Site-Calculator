package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun BrandBanner(){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(16.dp),
        color=Color.Transparent,
        border=BorderStroke(1.dp,FayrozGold.copy(alpha=.34f)),
        shadowElevation=3.dp
    ){
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF04131D),Color(0xFF071E2C),Color(0xFF0D2D3E))))
                .padding(horizontal=12.dp,vertical=8.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Surface(
                shape=RoundedCornerShape(10.dp),
                color=Color(0xFF04131D),
                border=BorderStroke(1.dp,FayrozGold.copy(alpha=.48f))
            ){
                Box(Modifier.size(44.dp).padding(5.dp),contentAlignment=Alignment.Center){
                    FayrozMark(Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)){
                Text("FAYROZ",style=MaterialTheme.typography.titleMedium,color=Color.White,fontWeight=FontWeight.ExtraBold)
                Text("SITE CALCULATOR",style=MaterialTheme.typography.labelMedium,color=FayrozGoldLight,fontWeight=FontWeight.Bold)
                Text("حاسبة كميات الموقع",style=MaterialTheme.typography.bodySmall,color=Color.White.copy(alpha=.78f))
            }
            Surface(shape=RoundedCornerShape(999.dp),color=FayrozGold.copy(alpha=.14f)){
                Text("SITE",Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=FayrozGoldLight)
            }
        }
    }
}

@Composable
fun AppearanceSelector(
    appearance:FayrozAppearance,
    onAppearance:(FayrozAppearance)->Unit
){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(13.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
    ){
        Row(
            Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement=Arrangement.spacedBy(4.dp)
        ){
            listOf(
                FayrozAppearance.System to "حسب الهاتف",
                FayrozAppearance.Light to "عادي",
                FayrozAppearance.Dark to "داكن"
            ).forEach{(mode,label)->
                val active=appearance==mode
                Surface(
                    modifier=Modifier.weight(1f),
                    onClick={onAppearance(mode)},
                    shape=RoundedCornerShape(10.dp),
                    color=if(active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ){
                    Box(
                        Modifier.padding(horizontal=5.dp,vertical=7.dp),
                        contentAlignment=Alignment.Center
                    ){
                        Text(
                            label,
                            style=MaterialTheme.typography.labelMedium,
                            color=if(active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeading(title:String,caption:String?=null){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
        Box(Modifier.width(4.dp).height(if(caption.isNullOrBlank())24.dp else 40.dp)
            .background(MaterialTheme.colorScheme.secondary,RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.titleMedium)
            if(!caption.isNullOrBlank()){
                Spacer(Modifier.height(2.dp))
                Text(caption,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SurfaceCard(content:@Composable ColumnScope.()->Unit){
    val shape=RoundedCornerShape(16.dp)
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=shape,
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        tonalElevation=0.dp,
        shadowElevation=2.dp
    ){
        Column(
            Modifier.padding(horizontal=13.dp,vertical=11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp),
            content=content
        )
    }
}

@Composable
fun ModeCard(
    title:String,
    subtitle:String,
    icon:ImageVector,
    onClick:()->Unit,
    accent:Boolean=false,
    compact:Boolean=false
){
    val accentColor=if(accent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(
        modifier=Modifier.fillMaxWidth(),
        onClick=onClick,
        shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(
            1.dp,
            if(accent) accentColor.copy(alpha=.52f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation=2.dp
    ){
        if(compact){
            Column(
                Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),
                verticalArrangement=Arrangement.spacedBy(5.dp)
            ){
                Surface(
                    shape=RoundedCornerShape(10.dp),
                    color=if(accent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ){
                    Icon(icon,null,Modifier.padding(7.dp).size(19.dp),tint=accentColor)
                }
                Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,maxLines=1)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
            }
        }else{
            Row(
                Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=9.dp),
                verticalAlignment=Alignment.CenterVertically
            ){
                Surface(
                    shape=RoundedCornerShape(10.dp),
                    color=if(accent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ){
                    Icon(icon,null,Modifier.padding(8.dp).size(20.dp),tint=accentColor)
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)){
                    Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ArrowBack,null,tint=accentColor)
            }
        }
    }
}

@Composable
fun HelpButton(title:String,help:String){
    var show by remember{mutableStateOf(false)}
    IconButton(onClick={show=true},modifier=Modifier.size(26.dp)){
        Icon(
            Icons.Rounded.Info,
            contentDescription="شرح $title",
            modifier=Modifier.size(16.dp),
            tint=MaterialTheme.colorScheme.tertiary
        )
    }
    if(show){
        AlertDialog(
            onDismissRequest={show=false},
            title={Text(title)},
            text={Text(help,style=MaterialTheme.typography.bodyMedium)},
            confirmButton={TextButton(onClick={show=false}){Text("فهمت")}}
        )
    }
}

@Composable
fun FieldTitle(label:String,help:String?=null){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(!help.isNullOrBlank()) HelpButton(label,help)
    }
}

@Composable
fun TextFieldSimple(label:String,value:String,onValue:(String)->Unit,help:String?=null,placeholder:String?=null){
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        FieldTitle(label,help)
        OutlinedTextField(
            value=value,
            onValueChange=onValue,
            modifier=Modifier.fillMaxWidth(),
            placeholder=if(placeholder!=null){{Text(placeholder)}}else null,
            singleLine=true,
            shape=RoundedCornerShape(10.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f),
                focusedBorderColor=MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun NumberField(
    label:String,
    value:String,
    onValue:(String)->Unit,
    unit:String?=null,
    help:String?=null
){
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        FieldTitle(label,help)
        OutlinedTextField(
            value=value,
            onValueChange={raw->
                val filtered=raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'}
                onValue(filtered)
            },
            modifier=Modifier.fillMaxWidth(),
            suffix=if(unit!=null){{Text(unit)}}else null,
            singleLine=true,
            shape=RoundedCornerShape(10.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f),
                focusedBorderColor=MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal)
        )
    }
}

@Composable
fun SelectField(
    label:String,
    value:String,
    options:List<String>,
    onValue:(String)->Unit,
    help:String?=null
){
    var open by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
        FieldTitle(label,help)
        Box{
            OutlinedButton(
                onClick={open=true},
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                shape=RoundedCornerShape(10.dp),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f))
            ){
                Text(value,Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Rounded.KeyboardArrowDown,null)
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}){
                options.forEach{opt->
                    DropdownMenuItem(text={Text(opt)},onClick={onValue(opt);open=false})
                }
            }
        }
    }
}

@Composable
fun StepTabs(tabs:List<String>,selected:Int,onSelect:(Int)->Unit){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(13.dp),
        color=MaterialTheme.colorScheme.surfaceVariant,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
    ){
        Row(
            Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement=Arrangement.spacedBy(4.dp)
        ){
            tabs.forEachIndexed{index,title->
                val active=index==selected
                Surface(
                    modifier=Modifier.weight(1f),
                    onClick={onSelect(index)},
                    shape=RoundedCornerShape(10.dp),
                    color=if(active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ){
                    Box(Modifier.padding(vertical=8.dp),contentAlignment=Alignment.Center){
                        Text(
                            title,
                            style=MaterialTheme.typography.labelLarge,
                            color=if(active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun num(s:String):Double=s.replace("٫",".").replace(",",".").toDoubleOrNull()?:0.0
fun qty(v:Double):String=String.format(Locale.US,"%.2f",v)

@Composable
fun QuantityRow(label:String,value:Double,unit:String){
    Row(
        Modifier.fillMaxWidth().padding(vertical=2.dp),
        horizontalArrangement=Arrangement.SpaceBetween,
        verticalAlignment=Alignment.CenterVertically
    ){
        Text(label,style=MaterialTheme.typography.bodyMedium)
        Text("${qty(value)} $unit",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
    }
}


@Composable
fun ExplainedToggle(
    title:String,
    help:String,
    checked:Boolean,
    onChecked:(Boolean)->Unit
){
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically
    ){
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
        HelpButton(title,help)
        Switch(checked=checked,onCheckedChange=onChecked)
    }
}

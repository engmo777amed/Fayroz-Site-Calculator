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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

fun num(s:String):Double=s.replace("٫",".").replace(",",".").toDoubleOrNull()?:0.0
fun qty(v:Double):String=String.format(Locale.US,"%.2f",v)

@Composable
fun AppearanceSelector(
    appearance:FayrozAppearance,
    onAppearance:(FayrozAppearance)->Unit
){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(14.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
    ){
        Row(
            Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement=Arrangement.spacedBy(3.dp)
        ){
            listOf(
                Triple(FayrozAppearance.System,"الهاتف",Icons.Rounded.SettingsBrightness),
                Triple(FayrozAppearance.Light,"فاتح",Icons.Rounded.LightMode),
                Triple(FayrozAppearance.Dark,"داكن",Icons.Rounded.DarkMode)
            ).forEach{(mode,label,icon)->
                val active=appearance==mode
                Surface(
                    modifier=Modifier.weight(1f),
                    onClick={onAppearance(mode)},
                    color=if(active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape=RoundedCornerShape(11.dp)
                ){
                    Row(
                        Modifier.padding(horizontal=6.dp,vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.Center
                    ){
                        Icon(icon,null,Modifier.size(15.dp),tint=if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            label,
                            style=MaterialTheme.typography.labelMedium,
                            color=if(active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
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
        shape=RoundedCornerShape(17.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,tone.copy(alpha=.35f)),
        shadowElevation=2.dp
    ){
        Row(
            Modifier.padding(horizontal=10.dp,vertical=10.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Surface(shape=RoundedCornerShape(12.dp),color=container){
                Icon(icon,null,Modifier.padding(8.dp).size(21.dp),tint=tone)
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
fun SectionHeader(
    title:String,
    subtitle:String?=null,
    trailing:(@Composable ()->Unit)?=null
){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(4.dp).height(if(subtitle.isNullOrBlank())22.dp else 36.dp)
            .background(MaterialTheme.colorScheme.primary,RoundedCornerShape(999.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
            if(!subtitle.isNullOrBlank()){
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SurfaceCard(
    modifier:Modifier=Modifier,
    content:@Composable ColumnScope.()->Unit
){
    Surface(
        modifier=modifier.fillMaxWidth(),
        shape=RoundedCornerShape(17.dp),
        color=MaterialTheme.colorScheme.surface,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        shadowElevation=1.dp
    ){
        Column(
            Modifier.padding(horizontal=11.dp,vertical=10.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp),
            content=content
        )
    }
}

@Composable
fun HelpButton(title:String,help:String){
    var show by remember{mutableStateOf(false)}
    IconButton(onClick={show=true},modifier=Modifier.size(26.dp)){
        Icon(Icons.Rounded.Info,"شرح $title",Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
    }
    if(show){
        AlertDialog(
            onDismissRequest={show=false},
            icon={Icon(Icons.Rounded.Info,null,tint=MaterialTheme.colorScheme.primary)},
            title={Text(title,fontWeight=FontWeight.Black)},
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
fun TextFieldSimple(
    label:String,
    value:String,
    onValue:(String)->Unit,
    help:String?=null,
    placeholder:String?=null,
    modifier:Modifier=Modifier
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldTitle(label,help)
        OutlinedTextField(
            value=value,
            onValueChange=onValue,
            modifier=Modifier.fillMaxWidth(),
            placeholder=if(placeholder!=null){{Text(placeholder,maxLines=1)}}else null,
            singleLine=true,
            shape=RoundedCornerShape(11.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),
                focusedBorderColor=MaterialTheme.colorScheme.primary,
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
    help:String?=null,
    modifier:Modifier=Modifier
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldTitle(label,help)
        OutlinedTextField(
            value=value,
            onValueChange={raw->
                onValue(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'})
            },
            modifier=Modifier.fillMaxWidth(),
            suffix=if(unit!=null){{Text(unit,style=MaterialTheme.typography.labelSmall)}}else null,
            singleLine=true,
            shape=RoundedCornerShape(11.dp),
            colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=MaterialTheme.colorScheme.surface,
                unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),
                focusedBorderColor=MaterialTheme.colorScheme.primary,
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
    help:String?=null,
    modifier:Modifier=Modifier
){
    var open by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        FieldTitle(label,help)
        Box{
            OutlinedButton(
                onClick={open=true},
                modifier=Modifier.fillMaxWidth().heightIn(min=47.dp),
                shape=RoundedCornerShape(11.dp),
                contentPadding=PaddingValues(horizontal=11.dp,vertical=7.dp),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f))
            ){
                Text(value,Modifier.weight(1f),color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                Icon(Icons.Rounded.KeyboardArrowDown,null,Modifier.size(19.dp))
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
fun StepTabs(
    tabs:List<String>,
    selected:Int,
    onSelect:(Int)->Unit
){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(14.dp),
        color=MaterialTheme.colorScheme.surfaceVariant,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
    ){
        Row(
            Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement=Arrangement.spacedBy(3.dp)
        ){
            tabs.forEachIndexed{index,title->
                val active=index==selected
                Surface(
                    modifier=Modifier.weight(1f),
                    onClick={onSelect(index)},
                    color=if(active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape=RoundedCornerShape(10.dp)
                ){
                    Box(Modifier.padding(vertical=7.dp,horizontal=3.dp),contentAlignment=Alignment.Center){
                        Text(
                            title,
                            style=MaterialTheme.typography.labelMedium,
                            color=if(active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines=1,
                            overflow=TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantityRow(
    label:String,
    value:Double,
    unit:String,
    emphasized:Boolean=false
){
    Surface(
        color=if(emphasized) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape=RoundedCornerShape(10.dp)
    ){
        Row(
            Modifier.fillMaxWidth().padding(horizontal=if(emphasized)8.dp else 0.dp,vertical=5.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Text(label,Modifier.weight(1f),style=if(emphasized) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium)
            Text(
                "${qty(value)} $unit",
                style=if(emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                color=MaterialTheme.colorScheme.primary,
                fontWeight=FontWeight.Black
            )
        }
    }
}

@Composable
fun ExplainedToggle(
    title:String,
    help:String,
    checked:Boolean,
    onChecked:(Boolean)->Unit
){
    Surface(
        shape=RoundedCornerShape(12.dp),
        color=if(checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.38f),
        border=BorderStroke(1.dp,if(checked) MaterialTheme.colorScheme.primary.copy(alpha=.28f) else MaterialTheme.colorScheme.outlineVariant)
    ){
        Row(
            Modifier.fillMaxWidth().padding(start=9.dp,end=5.dp,top=4.dp,bottom=4.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,fontWeight=if(checked) FontWeight.Bold else FontWeight.Medium)
            HelpButton(title,help)
            Switch(checked=checked,onCheckedChange=onChecked)
        }
    }
}

@Composable
fun WarningBox(messages:List<String>){
    if(messages.isEmpty()) return
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.errorContainer){
        Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.Top){
            Icon(Icons.Rounded.WarningAmber,null,tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)){
                Text("راجع المدخلات",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onErrorContainer)
                messages.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)}
            }
        }
    }
}

@Composable
fun EmptyState(
    title:String,
    subtitle:String,
    icon:ImageVector=Icons.Rounded.Inbox
){
    SurfaceCard{
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

@Composable
fun AppTopBar(
    title:String,
    subtitle:String?=null,
    onBack:(()->Unit)?=null,
    actions:@Composable RowScope.()->Unit={}
){
    TopAppBar(
        title={
            Column{
                Text(title,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(!subtitle.isNullOrBlank()){
                    Text(subtitle,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon={
            if(onBack!=null){
                IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}
            }
        },
        actions=actions,
        colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface)
    )
}

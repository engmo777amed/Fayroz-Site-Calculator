package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

fun n(raw:String):Double = raw.map{c->if(c.isDigit())c.digitToInt().digitToChar() else c}.joinToString("")
    .replace(',','.').replace('٫','.').replace("٬","").toDoubleOrNull()?.takeIf{it.isFinite()}?:0.0
fun exact(v:Double):String = java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()
fun fmt(v:Double):String=String.format(Locale.US,"%.3f",v).trimEnd('0').trimEnd('.')

data class LengthUnit(val label:String,val meters:Double)
val lengthUnits=listOf(LengthUnit("م",1.0),LengthUnit("سم",.01),LengthUnit("مم",.001))

fun lengthMeters(value:String,unit:String):Double =
    n(value)*(lengthUnits.firstOrNull{it.label==unit}?.meters?:1.0)

fun convertLength(value:String,from:String,to:String):String{
    if(value.isBlank())return ""
    val meters=lengthMeters(value,from)
    val factor=lengthUnits.firstOrNull{it.label==to}?.meters?:1.0
    return exact(meters/factor)
}

@Composable
fun PageHeader(title:String,subtitle:String?=null,action:(@Composable ()->Unit)?=null){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
            if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun BoxCard(content:@Composable ColumnScope.()->Unit){
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(18.dp),
        tonalElevation=1.dp
    ){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
    }
}

@Composable
fun HelpDot(title:String,text:String){
    var open by remember{mutableStateOf(false)}
    IconButton(onClick={open=true},modifier=Modifier.size(40.dp)){
        Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primaryContainer){
            Box(Modifier.size(22.dp),contentAlignment=Alignment.Center){
                Text("!",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelMedium)
            }
        }
    }
    if(open){
        AlertDialog(
            onDismissRequest={open=false},
            title={Text(title,fontWeight=FontWeight.Black)},
            text={Text(text)},
            confirmButton={TextButton(onClick={open=false}){Text("تمام")}}
        )
    }
}
@Composable
fun TextFieldX(
    label:String,
    value:String,
    onChange:(String)->Unit,
    modifier:Modifier=Modifier,
    placeholder:String="",
    help:String?=null
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=value,
            onValueChange=onChange,
            modifier=Modifier.fillMaxWidth().semantics{contentDescription="إدخال $label"},
            singleLine=true,
            placeholder={if(placeholder.isNotBlank())Text(placeholder)},
            trailingIcon=if(help!=null){{HelpDot(label,help)}}else null,
            shape=RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun NumberFieldX(
    label:String,
    value:String,
    onChange:(String)->Unit,
    unit:String,
    modifier:Modifier=Modifier,
    help:String?=null,
    error:String?=null
){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value=value,
            onValueChange={raw->onChange(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'||it=='-'} )},
            modifier=Modifier.fillMaxWidth().semantics{contentDescription="إدخال $label"},
            singleLine=true,
            isError=error!=null,
            supportingText=if(error!=null){{Text(error)}}else null,
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            suffix={Text(unit,style=MaterialTheme.typography.labelSmall)},
            trailingIcon=if(help!=null){{HelpDot(label,help)}}else null,
            shape=RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun NumberUnitField(
    label:String,
    value:String,
    onValue:(String)->Unit,
    unit:String,
    onUnit:(String)->Unit,
    modifier:Modifier=Modifier,
    help:String?=null
){
    var menu by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box{
            OutlinedTextField(
                value=value,
                onValueChange={raw->onValue(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'||it=='-'})},
                modifier=Modifier.fillMaxWidth().semantics{contentDescription="إدخال $label"},
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                suffix={
                    TextButton(onClick={menu=true},contentPadding=PaddingValues(horizontal=3.dp)){
                        Text(unit)
                        Icon(Icons.Rounded.ExpandMore,null,Modifier.size(16.dp))
                    }
                },
                trailingIcon=if(help!=null){{HelpDot(label,help)}}else null,
                shape=RoundedCornerShape(12.dp)
            )
            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                lengthUnits.forEach{u->
                    DropdownMenuItem(
                        text={Text(u.label)},
                        onClick={
                            menu=false
                            val converted=convertLength(value,unit,u.label)
                            onUnit(u.label)
                            onValue(converted)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChoiceFieldX(
    label:String,
    value:String,
    options:List<String>,
    onChange:(String)->Unit,
    modifier:Modifier=Modifier,
    help:String?=null
){
    var open by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box{
            OutlinedButton(
                onClick={open=true},
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                shape=RoundedCornerShape(12.dp)
            ){
                Text(value,Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
                if(help!=null)HelpDot(label,help)
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
        color=if(highlight)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(12.dp)
    ){
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
            Text(
                value,
                modifier=Modifier.weight(1f),
                textAlign=androidx.compose.ui.text.style.TextAlign.End,
                style=if(highlight)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                color=MaterialTheme.colorScheme.primary,
                fontWeight=FontWeight.Black
            )
        }
    }
}

@Composable
fun EmptyState(
    title:String,
    subtitle:String,
    actionLabel:String?=null,
    onAction:(()->Unit)?=null
){
    BoxCard{
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
        Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(actionLabel!=null&&onAction!=null){
            FilledTonalButton(onClick=onAction,modifier=Modifier.fillMaxWidth()){Text(actionLabel)}
        }
    }
}

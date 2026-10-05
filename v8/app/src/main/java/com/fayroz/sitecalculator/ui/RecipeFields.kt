package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*

@Composable
fun RecipeFields(def:CalcDef,quantity:Double,inputs:Map<String,String>,onChange:(Map<String,String>)->Unit){
    Text(def.title,style=MaterialTheme.typography.titleMedium)
    MetricRow("كمية من الحصر",fmt(quantity))
    var prices by remember{mutableStateOf(true)}
    var more by remember{mutableStateOf(false)}
    val normal=def.fields.filter{it.key !in setOf("area","length","count")&&!it.key.contains("price",true)&&it.key !in setOf("dry","density","extraRate")}
    normal.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{onChange(inputs+(f.key to it))},inputs["_unit.${f.key}"],{onChange(inputs+("_unit.${f.key}" to it))})}
    TextButton(onClick={prices=!prices}){Text("الأسعار — اختيارية")}
    if(prices)def.fields.filter{it.key.contains("price",true)}.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{onChange(inputs+(f.key to it))},inputs["_unit.${f.key}"],{onChange(inputs+("_unit.${f.key}" to it))})}
    TextButton(onClick={more=!more}){Text("إعدادات إضافية ومصنعية")}
    if(more){
        def.fields.filter{it.key in setOf("dry","density","extraRate")}.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{onChange(inputs+(f.key to it))},inputs["_unit.${f.key}"],{onChange(inputs+("_unit.${f.key}" to it))})}
        listOf("_laborRate" to "المصنعية للوحدة","_transportRate" to "النقل للوحدة","_equipmentRate" to "المعدات للوحدة").forEach{(key,label)->NumberFieldX(label,inputs[key]?:"0",{onChange(inputs+(key to it))},"جنيه")}
    }

}

@Composable
fun CalcInputField(field:CalcField,value:String,onChange:(String)->Unit,unitPreference:String?=null,onUnitChange:(String)->Unit={}){
    var displayUnit by remember(field.key,unitPreference){mutableStateOf(unitPreference?:field.unit)}
    var displayValue by remember(field.key){mutableStateOf(if(field.unit in listOf("م","سم","مم"))convertLength(value,field.unit,displayUnit)else value)}
    LaunchedEffect(value,displayUnit){
        val converted=if(field.unit in listOf("م","سم","مم"))convertLength(value,field.unit,displayUnit)else value
        if(n(displayValue)!=n(converted)||value.isBlank())displayValue=converted
    }
    if(field.unit in listOf("م","سم","مم")){
        NumberUnitField(field.label,displayValue,{raw->displayValue=raw;onChange(if(raw.isBlank())"" else convertLength(raw,displayUnit,field.unit))},displayUnit,{unit->displayUnit=unit;onUnitChange(unit)},help="الوحدة المختارة واضحة بجوار الرقم؛ التحويل يحافظ على القيمة.")
    }else NumberFieldX(field.label,value,onChange,field.unit)
}

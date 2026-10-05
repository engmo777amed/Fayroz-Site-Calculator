package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*

@Composable
fun RecipeFields(def:CalcDef,quantity:Double,inputs:Map<String,String>,onChange:(Map<String,String>)->Unit){
    Text("${def.title} — المساحة تأتي من الحصر تلقائيًا")
    def.fields.filter{it.key !in setOf("area","length","count")}.forEach{f->
        CalcInputField(f,inputs[f.key]?:f.default,{onChange(inputs+(f.key to it))})
    }
    listOf("_laborRate" to "المصنعية للوحدة","_transportRate" to "النقل للوحدة","_equipmentRate" to "المعدات للوحدة").forEach{(key,label)->
        NumberFieldX(label,inputs[key]?:"0",{onChange(inputs+(key to it))},"جنيه")
    }
    val raw=CalculatorLibrary.recipe(def,inputs,quantity)
    val answer=runCatching{CalculatorLibrary.evaluate(def,raw)}
    answer.getOrNull()?.let{a->
        a.outputs.forEach{MetricRow(it.label,"${fmt(it.value)} ${it.unit}")}
        MetricRow("تكلفة الاستهلاك","${fmt(a.consumedCost)} جنيه")
        MetricRow("شراء هذا الجزء منفردًا","${fmt(a.cost)} جنيه")
        Text("الشراء المجمع يقرب العبوات بعد جمع الأجزاء المتطابقة.",style=MaterialTheme.typography.bodySmall)
    }
    answer.exceptionOrNull()?.let{Text(it.message?:"راجع البيانات",color=MaterialTheme.colorScheme.error)}
}

@Composable
fun CalcInputField(field:CalcField,value:String,onChange:(String)->Unit){
    var displayUnit by remember(field.key){mutableStateOf(field.unit)}
    if(field.unit in listOf("م","سم","مم")){
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            NumberFieldX(field.label,if(value.isBlank())"" else convertLength(value,field.unit,displayUnit),
                {onChange(if(it.isBlank())"" else convertLength(it,displayUnit,field.unit))},displayUnit,androidx.compose.ui.Modifier.weight(3f))
            ChoiceFieldX("الوحدة",displayUnit,listOf("م","سم","مم"),{displayUnit=it},androidx.compose.ui.Modifier.weight(1f))
        }
    }else NumberFieldX(field.label,value,onChange,field.unit)
}

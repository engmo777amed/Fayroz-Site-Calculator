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
        NumberFieldX(f.label,inputs[f.key]?:f.default,{onChange(inputs+(f.key to it))},f.unit)
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

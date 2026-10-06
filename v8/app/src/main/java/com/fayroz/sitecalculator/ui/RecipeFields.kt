package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*

@Composable
fun RecipeFields(def:CalcDef,quantity:Double,inputs:Map<String,String>,onChange:(Map<String,String>)->Unit){
    Text(def.title,style=MaterialTheme.typography.titleMedium)
    val quantityUnit=if(def.fields.any{it.key=="area"})"م²" else if(def.fields.any{it.key=="length"})"م ط" else "عدد"
    Text("الكمية المعتمدة: ${fmt(quantity)} $quantityUnit",style=MaterialTheme.typography.bodySmall)
    var prices by remember{mutableStateOf(false)}
    var more by remember{mutableStateOf(false)}
    var latestInputs by remember{mutableStateOf(inputs)}
    SideEffect{latestInputs=inputs}
    fun update(fieldKey:String,value:String){
        val updated=latestInputs+(fieldKey to value)
        latestInputs=updated
        onChange(updated)
    }
    TextFieldX("اسم الصنف والمواصفة",inputs["_materialName"].orEmpty(),{update("_materialName",it)},placeholder="مثال: سيراميك أرضية بيج 60 × 60")
    if(MortarMix.isMix(def))NumberFieldX("شكاير الأسمنت على متر الرمل",inputs[MortarMix.key]?:exact(MortarMix.bags(inputs)),{update(MortarMix.key,it)},"شيكارة/م³ رمل")
    val normal=MortarMix.basic(def).filter{it.key !in setOf("area","length","count")}
    normal.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{update(f.key,it)},inputs["_unit.${f.key}"],{update("_unit.${f.key}",it)})}
    TextButton(onClick={prices=!prices}){Text("الأسعار — اختيارية")}
    if(prices)def.fields.filter{it.key.contains("price",true)}.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{update(f.key,it)},inputs["_unit.${f.key}"],{update("_unit.${f.key}",it)})}
    TextButton(onClick={more=!more}){Text("إعدادات إضافية ومصنعية")}
    if(more){
        def.fields.filter{it !in normal&&it.key !in setOf("area","length","count","cement","sand")&&!it.key.contains("price",true)}.forEach{f->CalcInputField(f,inputs[f.key]?:f.default,{update(f.key,it)},inputs["_unit.${f.key}"],{update("_unit.${f.key}",it)})}
        listOf("_laborRate" to "المصنعية للوحدة","_transportRate" to "النقل للوحدة","_equipmentRate" to "المعدات للوحدة").forEach{(key,label)->NumberFieldX(label,inputs[key]?:"0",{update(key,it)},"جنيه/$quantityUnit")}
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
    val number=MortarMix.number(value)
    val invalid=value.isNotBlank()&&(number==null||(!field.signed&&number<0)||(field.required&&number<=0)||(field.key in setOf("count","steps","layers","sides","faces","coats","conductors","pack")&&field.unit in setOf("عدد","قطعة","وجه","درجة","طبقة","شبكة","موصل","عبوة")&&number!=kotlin.math.floor(number)))
    val error=if(invalid)"راجع ${field.label}"else null
    if(field.unit in listOf("م","سم","مم")){
        NumberUnitField(field.label,displayValue,{raw->displayValue=raw;onChange(if(raw.isBlank())"" else convertLength(raw,displayUnit,field.unit))},displayUnit,{unit->displayUnit=unit;onUnitChange(unit)},help="الوحدة المختارة واضحة بجوار الرقم؛ التحويل يحافظ على القيمة.")
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }else NumberFieldX(field.label,value,onChange,field.unit,error=error)
}


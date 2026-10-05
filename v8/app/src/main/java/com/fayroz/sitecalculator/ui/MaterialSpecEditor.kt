package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.MaterialSpec

@Composable
fun MaterialSpecFields(value:MaterialSpec,onChange:(MaterialSpec)->Unit,rateUnit:String="م²"){
    Text("مواصفات الخلطة",style=MaterialTheme.typography.titleMedium)
    Text("النسب بالحجم: جزء أسمنت مقابل أجزاء الرمل.",style=MaterialTheme.typography.bodySmall)
    SpecNumber("متوسط السمك",value.thicknessMm,"مم"){onChange(value.copy(thicknessMm=it))}
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
        SpecNumber("أسمنت",value.cementParts,"جزء",Modifier.weight(1f)){onChange(value.copy(cementParts=it))}
        SpecNumber("رمل",value.sandParts,"جزء",Modifier.weight(1f)){onChange(value.copy(sandParts=it))}
    }
    SpecNumber("هالك الخامات",value.waste,"%"){onChange(value.copy(waste=it))}
    Text("الأسعار — اختيارية",style=MaterialTheme.typography.titleMedium)
    Text("يمكن حساب الكميات أولًا وإضافة الأسعار لاحقًا.",style=MaterialTheme.typography.bodySmall)
    SpecNumber("سعر شيكارة الأسمنت",value.cementPrice,"جنيه"){onChange(value.copy(cementPrice=it))}
    SpecNumber("سعر متر الرمل",value.sandPrice,"جنيه/م³"){onChange(value.copy(sandPrice=it))}
    var costs by remember{mutableStateOf(false)}
    TextButton(onClick={costs=!costs}){Text("مصنعية ونقل ومعدات — اختياري")}
    if(costs){
        SpecNumber("المصنعية",value.laborRate,"جنيه/$rateUnit"){onChange(value.copy(laborRate=it))}
        SpecNumber("النقل",value.transportRate,"جنيه/$rateUnit"){onChange(value.copy(transportRate=it))}
        SpecNumber("المعدات",value.equipmentRate,"جنيه/$rateUnit"){onChange(value.copy(equipmentRate=it))}
    }
    var more by remember{mutableStateOf(false)}
    TextButton(onClick={more=!more}){Text("مواصفات إضافية")}
    if(more){
        SpecNumber("وزن شيكارة الأسمنت",value.bagKg,"كجم"){onChange(value.copy(bagKg=it))}
        TextFieldX("اسم المادة الإضافية",value.extraName,{onChange(value.copy(extraName=it))})
        SpecNumber("معدل المادة الإضافية",value.extraRate,"وحدة/م²"){onChange(value.copy(extraRate=it))}
        SpecNumber("سعر وحدة الإضافة",value.extraPrice,"جنيه"){onChange(value.copy(extraPrice=it))}
        SpecNumber("معامل الحجم الجاف",value.dryFactor,"معامل"){onChange(value.copy(dryFactor=it))}
        SpecNumber("كثافة الأسمنت الحجمية",value.cementDensity,"كجم/م³"){onChange(value.copy(cementDensity=it))}
    }
    com.fayroz.sitecalculator.domain.MaterialReview.specError(value)?.let{Text(it,color=MaterialTheme.colorScheme.error)}
}
@Composable
fun SpecNumber(label:String,value:Double,unit:String,modifier:Modifier=Modifier,onChange:(Double)->Unit){
    var raw by remember{mutableStateOf(exact(value))}
    LaunchedEffect(value){if(n(raw)!=value)raw=exact(value)}
    val positive=label in setOf("متوسط السمك","وزن شيكارة الأسمنت","معامل الحجم الجاف","كثافة الأسمنت الحجمية")
    val invalid=raw.isNotBlank()&&(n(raw)<0||(positive&&n(raw)<=0))
    NumberFieldX(label,raw,{raw=it;onChange(n(it))},unit,modifier,error=if(invalid)"راجع $label" else null)
}

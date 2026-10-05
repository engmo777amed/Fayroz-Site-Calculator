package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.MaterialSpec

@Composable
fun MaterialSpecFields(value:MaterialSpec,onChange:(MaterialSpec)->Unit){
    SpecNumber("متوسط السمك",value.thicknessMm,"مم"){onChange(value.copy(thicknessMm=it))}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        SpecNumber("أسمنت",value.cementParts,"جزء",Modifier.weight(1f)){onChange(value.copy(cementParts=it))}
        SpecNumber("رمل",value.sandParts,"جزء",Modifier.weight(1f)){onChange(value.copy(sandParts=it))}
    }
    SpecNumber("هالك الخامات",value.waste,"%"){onChange(value.copy(waste=it))}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
        SpecNumber("وزن شيكارة الأسمنت",value.bagKg,"كجم",Modifier.weight(1f)){onChange(value.copy(bagKg=it))}
        SpecNumber("سعر الشيكارة",value.cementPrice,"جنيه",Modifier.weight(1f)){onChange(value.copy(cementPrice=it))}
    }
    SpecNumber("سعر الرمل",value.sandPrice,"جنيه/م³"){onChange(value.copy(sandPrice=it))}
    var more by remember{mutableStateOf(false)}
    TextButton(onClick={more=!more}){Text(if(more)"إخفاء الإضافات والمصنعية" else "إضافات ومصنعية ونقل ومعدات")}
    if(more){
        TextFieldX("اسم المادة الإضافية",value.extraName,{onChange(value.copy(extraName=it))})
        SpecNumber("معدل المادة الإضافية",value.extraRate,"وحدة/م²"){onChange(value.copy(extraRate=it))}
        SpecNumber("سعر وحدة الإضافة",value.extraPrice,"جنيه"){onChange(value.copy(extraPrice=it))}
        SpecNumber("المصنعية",value.laborRate,"جنيه/م²"){onChange(value.copy(laborRate=it))}
        SpecNumber("النقل",value.transportRate,"جنيه/م²"){onChange(value.copy(transportRate=it))}
        SpecNumber("المعدات",value.equipmentRate,"جنيه/م²"){onChange(value.copy(equipmentRate=it))}
        SpecNumber("معامل الحجم الجاف",value.dryFactor,"معامل"){onChange(value.copy(dryFactor=it))}
        SpecNumber("كثافة الأسمنت الحجمية",value.cementDensity,"كجم/م³"){onChange(value.copy(cementDensity=it))}
    }
}
@Composable
fun SpecNumber(label:String,value:Double,unit:String,modifier:Modifier=Modifier,onChange:(Double)->Unit){
    var raw by remember{mutableStateOf(exact(value))}
    LaunchedEffect(value){if(n(raw)!=value)raw=exact(value)}
    NumberFieldX(label,raw,{raw=it;onChange(n(it).coerceAtLeast(0.0))},unit,modifier)
}

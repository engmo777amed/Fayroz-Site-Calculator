package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.MaterialSpec
import com.fayroz.sitecalculator.domain.CostEngine
import com.fayroz.sitecalculator.domain.MortarMix

@Composable
fun MaterialSpecFields(value:MaterialSpec,onChange:(MaterialSpec)->Unit,rateUnit:String="م²"){
    Text("مواصفات التنفيذ",style=MaterialTheme.typography.titleSmall)
    Text("${fmt(MortarMix.bags(value))} شيكارة × ${fmt(value.bagKg)} كجم على ١ م³ رمل • هالك ${fmt(value.waste)}%",style=MaterialTheme.typography.bodySmall)
    SpecNumber("شكاير الأسمنت على متر الرمل",MortarMix.bags(value),"شيكارة/م³ رمل"){onChange(MortarMix.withBags(value,it))}
    SpecNumber("متوسط السمك",value.thicknessMm,"مم"){onChange(value.copy(thicknessMm=it))}
    val reference=if(value.thicknessMm>0&&value.cementParts+value.sandParts>0&&value.bagKg>0)
        runCatching{CostEngine.measure(1000/value.thicknessMm,value.copy(waste=0.0))}.getOrNull()else null
    ExpandableSection("مكونات المونة — مرجع اختياري"){
    reference?.let{r->
        MetricRow("رمل","${fmt(r.second)} م³")
        MetricRow("أسمنت","${fmt(r.first/value.bagKg)} شيكارة × ${fmt(value.bagKg)} كجم")
        MetricRow("تغطية عند السمك المدخل","${fmt(1000/value.thicknessMm)} م²")
    }
    }
    ExpandableSection("تفاصيل إضافية"){
        SpecNumber("هالك الخامات",value.waste,"%"){onChange(value.copy(waste=it))}
        SpecNumber("وزن شيكارة الأسمنت",value.bagKg,"كجم"){onChange(MortarMix.withBags(value.copy(bagKg=it),MortarMix.bags(value)))}
    }
    ExpandableSection("السعر المستخدم / تغيير خاص"){
        TextFieldX("نوع الأسمنت وماركته",value.cementName,{onChange(value.copy(cementName=it))})
        TextFieldX("نوع الرمل",value.sandName,{onChange(value.copy(sandName=it))})
        SpecNumber("سعر شيكارة الأسمنت",value.cementPrice,"جنيه"){onChange(value.copy(cementPrice=it))}
        SpecNumber("سعر متر الرمل",value.sandPrice,"جنيه/م³"){onChange(value.copy(sandPrice=it))}
    }
    ExpandableSection("مصنعية ونقل ومعدات — اختياري"){
        SpecNumber("المصنعية",value.laborRate,"جنيه/$rateUnit"){onChange(value.copy(laborRate=it))}
        SpecNumber("النقل",value.transportRate,"جنيه/$rateUnit"){onChange(value.copy(transportRate=it))}
        SpecNumber("المعدات",value.equipmentRate,"جنيه/$rateUnit"){onChange(value.copy(equipmentRate=it))}
    }
    ExpandableSection("معاملات الخلطة والمواد الإضافية"){
        TextFieldX("اسم المادة الإضافية",value.extraName,{onChange(value.copy(extraName=it))})
        SpecNumber("معدل المادة الإضافية",value.extraRate,"وحدة/م²"){onChange(value.copy(extraRate=it))}
        SpecNumber("سعر وحدة الإضافة",value.extraPrice,"جنيه"){onChange(value.copy(extraPrice=it))}
        SpecNumber("معامل الحجم الجاف",value.dryFactor,"معامل"){onChange(value.copy(dryFactor=it))}
        SpecNumber("كثافة الأسمنت الحجمية",value.cementDensity,"كجم/م³"){if(it>0)onChange(MortarMix.withBags(value.copy(cementDensity=it),MortarMix.bags(value)))else onChange(value.copy(cementDensity=it))}
    }
    com.fayroz.sitecalculator.domain.MaterialReview.specError(value)?.let{Text(it,color=MaterialTheme.colorScheme.error)}
}
@Composable
fun SpecNumber(label:String,value:Double,unit:String,modifier:Modifier=Modifier,onChange:(Double)->Unit){
    var raw by remember{mutableStateOf(exact(value))}
    LaunchedEffect(value){if(value.isFinite()&&MortarMix.number(raw)!=null&&MortarMix.number(raw)!=value)raw=exact(value)}
    val positive=label in setOf("متوسط السمك","وزن شيكارة الأسمنت","معامل الحجم الجاف","كثافة الأسمنت الحجمية")
    val parsed=MortarMix.number(raw)
    val invalid=parsed==null||parsed<0||(positive&&parsed<=0)
    NumberFieldX(label,raw,{raw=it;onChange(MortarMix.number(it)?:-1.0)},unit,modifier,error=if(invalid)"راجع $label" else null)
}

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun ToolsScreen(onRoom:()->Unit,onItem:()->Unit){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{CompactBrandHeader("الحاسبات السريعة","حصر بدون إنشاء مشروع.")}
        item{DashboardCard("حصر فراغ كامل","حوائط ومساحات وفتحات وبنود.",Icons.Rounded.MeetingRoom,onRoom,true,Modifier.fillMaxWidth())}
        item{DashboardCard("حصر بند مباشر","م² / م ط / م³ / عدد مع تعديلات يدوية.",Icons.Rounded.Calculate,onItem,false,Modifier.fillMaxWidth())}
    }
}

@Composable
fun QuickItemScreen(onBack:()->Unit){
    var name by remember{mutableStateOf("محارة الحوائط")}
    var unit by remember{mutableStateOf(MeasureUnit.AREA)}
    var method by remember{mutableStateOf(CalcMethod.DIRECT_AREA)}
    var value by remember{mutableStateOf("")}
    var length by remember{mutableStateOf("")}
    var width by remember{mutableStateOf("")}
    var height by remember{mutableStateOf("3.00")}
    var count by remember{mutableStateOf("1")}
    var waste by remember{mutableStateOf("0")}
    var addValue by remember{mutableStateOf("")}
    var deductValue by remember{mutableStateOf("")}

    val methods=CalcMethod.entries.filter{m->
        when(unit){
            MeasureUnit.AREA->m in listOf(CalcMethod.ROOM_WALLS,CalcMethod.FLOOR_SURFACES,CalcMethod.CEILING_SURFACES,CalcMethod.DIRECT_AREA)
            MeasureUnit.LENGTH->m in listOf(CalcMethod.SKIRTING,CalcMethod.DIRECT_LENGTH)
            MeasureUnit.VOLUME->m==CalcMethod.DIRECT_VOLUME
            MeasureUnit.COUNT->m==CalcMethod.DIRECT_COUNT
        }
    }
    LaunchedEffect(unit){if(method !in methods)method=methods.first()}

    val scratch=SpaceEntry(
        name="حصر سريع",type="مخصص",
        length=n(length),width=n(width),height=n(height),
        repeatCount=n(count).toInt().coerceAtLeast(1)
    )
    val adjustments=buildList{
        if(n(addValue)>0)add(Adjustment(type=AdjustmentType.ADD,amount=n(addValue),note="إضافة سريعة"))
        if(n(deductValue)>0)add(Adjustment(type=AdjustmentType.DEDUCT,amount=n(deductValue),note="خصم سريع"))
    }
    val item=TakeoffItem(
        name=name.ifBlank{"بند سريع"},category="سريع",unit=unit,method=method,
        wastePercent=n(waste),directValue=n(value),adjustments=adjustments
    )
    val result=QuantityEngine.calculate(scratch,item)

    Scaffold(topBar={AppBarX("حصر بند سريع","بدون حفظ داخل مشروع",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it).imePadding(),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    TextFieldX("اسم البند",name,{name=it},placeholder="مثال: خرسانة قاعدة جهاز")
                    ChoiceFieldX("الوحدة",unit.label,MeasureUnit.entries.map{it.label},{label->unit=MeasureUnit.entries.first{it.label==label}})
                    ChoiceFieldX("طريقة الحصر",method.label,methods.map{it.label},{label->method=methods.first{it.label==label}})
                    if(method in listOf(CalcMethod.DIRECT_AREA,CalcMethod.DIRECT_LENGTH,CalcMethod.DIRECT_VOLUME,CalcMethod.DIRECT_COUNT)){
                        NumberFieldX("الكمية",value,{value=it},unit.label)
                    }else{
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            NumberFieldX("الطول",length,{length=it},"م",modifier=Modifier.weight(1f))
                            NumberFieldX("العرض",width,{width=it},"م",modifier=Modifier.weight(1f))
                        }
                        if(method==CalcMethod.ROOM_WALLS)NumberFieldX("الارتفاع",height,{height=it},"م")
                        NumberFieldX("التكرار",count,{count=it},"عدد")
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("إضافة",addValue,{addValue=it},unit.label,modifier=Modifier.weight(1f))
                        NumberFieldX("خصم",deductValue,{deductValue=it},unit.label,modifier=Modifier.weight(1f))
                    }
                    NumberFieldX("الهالك",waste,{waste=it},"%")
                }
            }
            item{
                CardBox{
                    SectionTitle("النتيجة","الحساب يتحدث فورًا.")
                    MetricRow(name.ifBlank{"البند"},"${fmt(result.final)} ${unit.label}",true)
                    MetricRow("قبل الهالك","${fmt(result.calculated-result.waste)} ${unit.label}")
                    if(result.waste>0)MetricRow("الهالك","${fmt(result.waste)} ${unit.label}")
                    Text(result.explanation,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

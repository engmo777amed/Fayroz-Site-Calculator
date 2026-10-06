@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.Catalog
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun DirectItemScreen(
    project:Project,
    initialSectionId:String?,
    onBack:()->Unit,
    onSave:(String,Space)->Unit
){
    var itemName by remember{mutableStateOf(Catalog.items.first().name)}
    val def=Catalog.items.first{it.name==itemName}
    var sectionId by remember(project.id){mutableStateOf(initialSectionId?.takeIf{id->project.sections.any{it.id==id}} ?: project.sections.firstOrNull()?.id.orEmpty())}
    var quantity by remember(itemName){mutableStateOf("")}
    var waste by remember(itemName){mutableStateOf(fmt(def.waste))}
    var note by remember{mutableStateOf("")}

    val takeoff=def.create().copy(kind=CalcKind.DIRECT,directValue=n(quantity),waste=n(waste),note=note)
    val space=Space(
        name="حصر بند - $itemName",
        type="حصر حسب البند",
        repeatCount=1,
        takeoffs=listOf(takeoff),
        note=note,
        status=WorkStatus.DONE
    )
    val result=QuantityEngine.calculateOne(space,takeoff)

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text("إضافة كمية جاهزة",fontWeight=FontWeight.Black)
                    Text(project.name,style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}}
            )
        }
    ){padding->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            item{
                BoxCard{
                    ChoiceFieldX(
                        "البند",
                        itemName,
                        Catalog.items.map{it.name},
                        {itemName=it},
                        help="هنا بتحصر بند مباشر من غير ما تعمل غرفة كاملة."
                    )
                    if(project.sections.isNotEmpty()){
                        ChoiceFieldX(
                            "الدور / الجزء",
                            selectionLabels(project.sections.map{it.name})[project.sections.indexOfFirst{it.id==sectionId}.coerceAtLeast(0)],
                            selectionLabels(project.sections.map{it.name}),
                            {label->sectionId=project.sections[selectionLabels(project.sections.map{it.name}).indexOf(label)].id}
                        )
                    }
                    NumberFieldX("الكمية الجاهزة",quantity,{quantity=it},def.unit.label,help="اكتب الكمية اللي معاك من الموقع مباشرة.")
                    ExpandableSection("الهالك والملاحظات — اختياري"){
                    NumberFieldX("الهالك",waste,{waste=it},"%",help="خاص بالبند ده فقط.")
                    TextFieldX("ملاحظة",note,{note=it},placeholder="مثال: حصر واجهة الدور الأول")
                    }
                }
            }

            if(com.fayroz.sitecalculator.domain.MortarMix.number(waste)?.let{it>=0}!=true)item{Text("الهالك لا يمكن أن يكون سالبًا.",color=MaterialTheme.colorScheme.error)}
            if(n(quantity)>0&&n(waste)>=0){
                item{
                    BoxCard{
                        MetricRow("صافي التنفيذ","${fmt(result.repeatedFinal)} ${def.unit.label}",true)
                        if(result.waste>0)MetricRow("الهالك","${fmt(result.waste)} ${def.unit.label}")
                        MetricRow("كمية البند بعد الهالك","${fmt(result.repeatedFinal+result.waste)} ${def.unit.label}")
                        Text("الهالك للشراء فقط؛ صافي التنفيذ لا يتغير. خامات المونة تُحسب من خلطتها.",style=MaterialTheme.typography.bodySmall)
                    }
                }
                item{
                    Button(
                        onClick={ if(sectionId.isNotBlank()) onSave(sectionId,space) },
                        enabled=sectionId.isNotBlank()&&com.fayroz.sitecalculator.domain.MortarMix.number(quantity)?.let{it>0}==true&&com.fayroz.sitecalculator.domain.MortarMix.number(waste)?.let{it>=0}==true,
                        modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)
                    ){
                        Icon(Icons.Rounded.Save,null)
                        Spacer(Modifier.width(5.dp))
                        Text("حفظ داخل المشروع")
                    }
                }
            }
        }
    }
}


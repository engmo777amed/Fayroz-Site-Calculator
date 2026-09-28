@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SiteUtilityScreen(toolId:String,onBack:()->Unit){
    when(toolId){
        "slope"->SlopeUtility(onBack)
        "convert"->UnitUtility(onBack)
        "area"->IrregularAreaUtility(onBack)
    }
}

@Composable
private fun SlopeUtility(onBack:()->Unit){
    var length by remember{mutableStateOf("")}
    var lengthUnit by remember{mutableStateOf("م")}
    var slope by remember{mutableStateOf("1")}
    var mode by remember{mutableStateOf("معايا الميل")}
    var start by remember{mutableStateOf("0")}
    var end by remember{mutableStateOf("")}
    var levelUnit by remember{mutableStateOf("م")}
    val l=lengthMeters(length,lengthUnit)
    val startM=lengthMeters(start,levelUnit)
    val endM=lengthMeters(end,levelUnit)
    val diff=if(mode=="معايا الميل")l*n(slope)/100.0 else abs(endM-startM)
    val calculatedSlope=if(l>0)diff/l*100.0 else 0.0

    Scaffold(topBar={TopAppBar(title={Text("الميل والمناسيب",fontWeight=FontWeight.Black)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}})}){p->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{BoxCard{
                ChoiceFieldX("معاك إيه؟",mode,listOf("معايا الميل","معايا المنسوبين"),{mode=it})
                NumberUnitField("طول المسار",length,{length=it},lengthUnit,{lengthUnit=it},help="المسافة الأفقية اللي الميل ماشي عليها.")
                NumberUnitField("منسوب البداية",start,{start=it},levelUnit,{levelUnit=it})
                if(mode=="معايا الميل") NumberFieldX("الميل",slope,{slope=it},"%",help="1% = 1 سم لكل متر.")
                else NumberUnitField("منسوب النهاية",end,{end=it},levelUnit,{levelUnit=it})
            }}
            if(l>0)item{BoxCard{
                MetricRow("فرق المنسوب","${fmt(diff*100)} سم",true)
                MetricRow("الميل",if(mode=="معايا الميل")"${fmt(n(slope))} %" else "${fmt(calculatedSlope)} %")
                Text("فرق المنسوب = طول المسار × الميل ÷ 100.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}
        }
    }
}

@Composable
private fun UnitUtility(onBack:()->Unit){
    var value by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("طول")}
    var unit by remember{mutableStateOf("م")}
    val x=n(value)
    Scaffold(topBar={TopAppBar(title={Text("تحويل الوحدات",fontWeight=FontWeight.Black)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}})}){p->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{BoxCard{
                ChoiceFieldX("نوع التحويل",type,listOf("طول","مساحة","حجم"),{
                    type=it;unit=when(it){"طول"->"م";"مساحة"->"م²";else->"م³"}
                })
                ChoiceFieldX("الوحدة اللي معاك",unit,when(type){"طول"->listOf("م","سم","مم");"مساحة"->listOf("م²","سم²");else->listOf("م³","لتر")},{unit=it})
                NumberFieldX("القيمة",value,{value=it},unit)
            }}
            if(value.isNotBlank())item{BoxCard{
                when(type){
                    "طول"->{
                        val m=when(unit){"سم"->x/100;"مم"->x/1000;else->x}
                        MetricRow("متر","${fmt(m)} م",true);MetricRow("سنتيمتر","${fmt(m*100)} سم");MetricRow("مليمتر","${fmt(m*1000)} مم")
                    }
                    "مساحة"->{
                        val m2=if(unit=="سم²")x/10000 else x
                        MetricRow("متر مربع","${fmt(m2)} م²",true);MetricRow("سنتيمتر مربع","${fmt(m2*10000)} سم²")
                    }
                    else->{
                        val m3=if(unit=="لتر")x/1000 else x
                        MetricRow("متر مكعب","${fmt(m3)} م³",true);MetricRow("لتر","${fmt(m3*1000)} لتر")
                    }
                }
            }}
        }
    }
}

private data class AreaPart(var length:String="",var width:String="",var deduct:Boolean=false)

@Composable
private fun IrregularAreaUtility(onBack:()->Unit){
    val parts=remember{mutableStateListOf(AreaPart())}
    var unit by remember{mutableStateOf("م")}
    val total=parts.sumOf{part->
        val a=lengthMeters(part.length,unit)*lengthMeters(part.width,unit)
        if(part.deduct)-a else a
    }.coerceAtLeast(0.0)

    Scaffold(topBar={TopAppBar(title={Text("مساحة غير منتظمة",fontWeight=FontWeight.Black)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}})}){p->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(p),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{BoxCard{
                ChoiceFieldX("وحدة الأبعاد",unit,listOf("م","سم","مم"),{new->
                    if(new!=unit){
                        parts.indices.forEach{i->
                            val old=parts[i]
                            parts[i]=old.copy(length=convertLength(old.length,unit,new),width=convertLength(old.width,unit,new))
                        }
                        unit=new
                    }
                })
                Text("قسم المساحة لمستطيلات بسيطة، وأي جزء مش منفذ علّمه خصم.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                parts.forEachIndexed{i,part->
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        NumberFieldX("طول ${i+1}",part.length,{parts[i]=part.copy(length=it)},unit,Modifier.weight(1f))
                        NumberFieldX("عرض ${i+1}",part.width,{parts[i]=part.copy(width=it)},unit,Modifier.weight(1f))
                    }
                    Row{
                        Checkbox(part.deduct,{parts[i]=part.copy(deduct=it)})
                        Text("خصم الجزء ده",Modifier.padding(top=12.dp))
                    }
                }
                OutlinedButton(onClick={parts.add(AreaPart())},modifier=Modifier.fillMaxWidth()){Text("ضيف جزء")}
            }}
            item{BoxCard{MetricRow("المساحة الصافية","${fmt(total)} م²",true)}}
        }
    }
}

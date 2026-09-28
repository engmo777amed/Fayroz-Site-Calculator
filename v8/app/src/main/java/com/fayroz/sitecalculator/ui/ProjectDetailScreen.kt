@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.MaterialEngine
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun ProjectDetailScreen(
    project:Project,
    active:Boolean,
    onBack:()->Unit,
    onSetActive:()->Unit,
    onOpenSection:(String)->Unit,
    onAddSection:(String)->Unit,
    onOpenMaterials:(MaterialResult)->Unit,
    onShare:()->Unit
){
    var addSection by remember{mutableStateOf(false)}
    var materialsOpen by remember{mutableStateOf(false)}
    var drillName by remember{mutableStateOf<String?>(null)}
    val summary=QuantityEngine.summarize(project)
    val allMaterials=summary.mapNotNull(MaterialEngine::forSummary)

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text(project.name,fontWeight=FontWeight.Black)
                    Text(project.type,style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
                actions={
                    IconButton(onClick=onShare){Icon(Icons.Rounded.Share,"مشاركة")}
                }
            )
        }
    ){padding->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            item{
                BoxCard{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(if(active)"المشروع النشط دلوقتي" else "خلّي ده المشروع النشط",fontWeight=FontWeight.Black)
                            Text(
                                if(active)"الحصر والحاسبات هتربط عليه تلقائيًا." else "بعدها مش هتختار المشروع كل مرة.",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if(active)Icon(Icons.Rounded.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)
                        else FilledTonalButton(onClick=onSetActive){Text("خليه نشط")}
                    }
                }
            }

            item{
                PageHeader("كميات المشروع","اضغط على أي بند عشان تعرف جاي منين")
            }

            if(summary.isEmpty()){
                item{EmptyState("لسه مفيش كميات","ضيف دور/جزء وابدأ حصر أول غرفة.")}
            }else{
                items(summary.size){index->
                    val line=summary[index]
                    Surface(
                        onClick={drillName=line.name},
                        shape=RoundedCornerShape(14.dp),
                        tonalElevation=1.dp
                    ){
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text(line.name,fontWeight=FontWeight.Black)
                                Text("اضغط للتفاصيل",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${fmt(line.quantity)} ${line.unit.label}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.ChevronLeft,null)
                        }
                    }
                }

                if(allMaterials.isNotEmpty()){
                    item{
                        Button(onClick={materialsOpen=true},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){
                            Icon(Icons.Rounded.Inventory2,null);Spacer(Modifier.width(6.dp));Text("إجمالي خامات المشروع")
                        }
                    }
                }
            }

            item{
                PageHeader("الأدوار والأجزاء",action={
                    FilledTonalButton(onClick={addSection=true}){
                        Icon(Icons.Rounded.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("جزء")
                    }
                })
            }

            if(project.sections.isEmpty()){
                item{EmptyState("مفيش دور أو جزء","ضيف أول دور أو جزء وابدأ الحصر.","ضيف جزء"){addSection=true}}
            }else{
                items(project.sections.size){index->
                    val section=project.sections[index]
                    val total=section.spaces.size
                    val qty=QuantityEngine.summarize(section).size
                    Surface(onClick={onOpenSection(section.id)},shape=RoundedCornerShape(15.dp),tonalElevation=1.dp){
                        Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){
                            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                Icon(Icons.Rounded.Layers,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(section.name,fontWeight=FontWeight.Black)
                                Text("$total مكان • $qty بند مجمع",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronLeft,null)
                        }
                    }
                }
            }

            if(project.calculations.isNotEmpty()){
                item{PageHeader("حسابات محفوظة للمشروع")}
                items(project.calculations.sortedByDescending{it.createdAt}.take(5).size){index->
                    val calc=project.calculations.sortedByDescending{it.createdAt}.take(5)[index]
                    BoxCard{
                        Text(calc.title,fontWeight=FontWeight.Black)
                        Text(calc.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if(addSection){
        var name by remember{mutableStateOf("")}
        AlertDialog(
            onDismissRequest={addSection=false},
            title={Text("ضيف دور أو جزء")},
            text={TextFieldX("الاسم",name,{name=it},placeholder="مثال: الدور الأول")},
            confirmButton={Button(onClick={if(name.isNotBlank()){addSection=false;onAddSection(name.trim())}},enabled=name.isNotBlank()){Text("إضافة")}},
            dismissButton={TextButton(onClick={addSection=false}){Text("إلغاء")}}
        )
    }

    drillName?.let{name->
        val line=summary.firstOrNull{it.name==name}
        val contributors=QuantityEngine.contributors(project,name)
        AlertDialog(
            onDismissRequest={drillName=null},
            title={Text(name,fontWeight=FontWeight.Black)},
            text={
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(contributors.size){i->
                        val c=contributors[i]
                        Row(Modifier.fillMaxWidth()){
                            Text(c.first,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                            Text(fmt(c.second),fontWeight=FontWeight.Black)
                        }
                    }
                }
            },
            confirmButton={
                Row{
                    val material=line?.let(MaterialEngine::forSummary)
                    if(material!=null)TextButton(onClick={drillName=null;onOpenMaterials(material)}){Text("احسب الخامات")}
                    TextButton(onClick={drillName=null}){Text("إغلاق")}
                }
            }
        )
    }

    if(materialsOpen){
        AlertDialog(
            onDismissRequest={materialsOpen=false},
            title={Text("إجمالي خامات المشروع",fontWeight=FontWeight.Black)},
            text={
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    items(allMaterials.size){i->
                        val m=allMaterials[i]
                        Column{
                            Text(m.title,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                            m.lines.forEach{Text("• ${it.label}: ${it.value}",style=MaterialTheme.typography.bodySmall)}
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={materialsOpen=false}){Text("تمام")}}
        )
    }
}

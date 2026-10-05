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
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.domain.MaterialEngine

@Composable
fun SectionScreen(
    project:Project,
    section:Section,
    active:ActiveLocation?,
    onBack:()->Unit,
    onUpdateSection:(Section)->Unit,
    onAddRoom:()->Unit,
    onEditRoom:(String)->Unit,
    onSetActiveRoom:(String)->Unit,
    onDetachCopy:(String)->Unit,
    onDuplicateRoom:(String)->Unit,
    onDeleteRoom:(String)->Unit
){
    val summary=QuantityEngine.summarize(section)
    val costRows=com.fayroz.sitecalculator.domain.CostEngine.rows(project.copy(sections=listOf(section)))
    val purchase=com.fayroz.sitecalculator.domain.CostEngine.purchase(costRows)
    val materials=MaterialEngine.aggregate(summary)
    var materialsOpen by remember{mutableStateOf(false)}
    var deleteTarget by remember{mutableStateOf<Space?>(null)}

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text(section.name,fontWeight=FontWeight.Black)
                    Text(project.name,style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}},
                actions={
                    IconButton(onClick=onAddRoom){Icon(Icons.Rounded.Add,"ضيف غرفة")}
                }
            )
        }
    ){padding->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            if(summary.isNotEmpty()){
                item{PageHeader("ملخص الجزء","التكرار داخل كل مكان محسوب مرة واحدة")}
                item{
                    BoxCard{
                        summary.take(6).forEachIndexed{i,line->
                            MetricRow(line.name,"${fmt(line.quantity)} ${line.unit.label}",i==0)
                        }
                        if(materials.isNotEmpty()){
                            Button(onClick={materialsOpen=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                                Icon(Icons.Rounded.Inventory2,null)
                                Spacer(Modifier.width(5.dp))
                                Text("خامات المونة وتكلفة الجزء")
                            }
                        }
                    }
                }
            }

            item{
                PageHeader("الغرف والأماكن","اختار المكان وكمل الحصر"){
                    FilledTonalButton(onClick=onAddRoom){
                        Icon(Icons.Rounded.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("مكان")
                    }
                }
            }

            if(section.spaces.isEmpty()){
                item{EmptyState("مفيش أماكن لسه","ضيف أول غرفة أو فراغ.","ضيف مكان",onAddRoom)}
            }else{
                items(section.spaces.size){index->
                    val space=section.spaces[index]
                    val isActive=active?.spaceId==space.id
                    val itemsDone=space.takeoffs.size
                    Surface(
                        onClick={onEditRoom(space.id)},
                        shape=RoundedCornerShape(16.dp),
                        tonalElevation=1.dp
                    ){
                        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                    Icon(Icons.Rounded.MeetingRoom,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(space.name,fontWeight=FontWeight.Black)
                                    Text(
                                        "${space.type} • $itemsDone بند"+if(space.repeatCount>1)" • متكرر ×${space.repeatCount}" else "",
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if(isActive)Icon(Icons.Rounded.RadioButtonChecked,"المكان النشط",tint=MaterialTheme.colorScheme.secondary)
                                else Icon(Icons.Rounded.ChevronLeft,null)
                            }

                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                AssistChip(
                                    onClick={onSetActiveRoom(space.id)},
                                    label={Text(if(isActive)"المكان النشط" else "كمّل من هنا")},
                                    leadingIcon={Icon(if(isActive)Icons.Rounded.Check else Icons.Rounded.PlayArrow,null,Modifier.size(16.dp))}
                                )
                                AssistChip(
                                    onClick={onDuplicateRoom(space.id)},
                                    label={Text("نسخ")},
                                    leadingIcon={Icon(Icons.Rounded.ContentCopy,null,Modifier.size(16.dp))}
                                )
                                if(space.repeatCount>1){
                                    AssistChip(
                                        onClick={onDetachCopy(space.id)},
                                        label={Text("فصل نسخة")},
                                        leadingIcon={Icon(Icons.Rounded.CallSplit,null,Modifier.size(16.dp))}
                                    )
                                }
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                if(index>0)TextButton(onClick={val list=section.spaces.toMutableList();list[index]=list[index-1].also{list[index-1]=list[index]};onUpdateSection(section.copy(spaces=list))}){Text("↑")}
                                TextButton(onClick={onEditRoom(space.id)}){Text("تعديل")}
                                TextButton(onClick={deleteTarget=space}){Text("حذف",color=MaterialTheme.colorScheme.error)}
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let{space->
        AlertDialog(
            onDismissRequest={deleteTarget=null},
            title={Text("حذف ${space.name}؟",fontWeight=FontWeight.Black)},
            text={Text("هيتم حذف المكان وبنود الحصر الخاصة بيه من المشروع.")},
            confirmButton={
                TextButton(onClick={onDeleteRoom(space.id);deleteTarget=null}){
                    Text("حذف",color=MaterialTheme.colorScheme.error)
                }
            },
            dismissButton={TextButton(onClick={deleteTarget=null}){Text("إلغاء")}}
        )
    }

    if(materialsOpen){
        AlertDialog(
            onDismissRequest={materialsOpen=false},
            title={Text("إجمالي خامات الجزء",fontWeight=FontWeight.Black)},
            text={
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    item{MetricRow("تكلفة مواد","${fmt(costRows.sumOf{it.materialCost})} جنيه")}
                    items(purchase.size){i->val m=purchase[i];Column{
                        Text(m.material,fontWeight=FontWeight.Bold)
                        Text("${fmt(m.amount)} ${m.unit}"+(m.packages?.let{" • $it عبوة"}?:""))
                        Text("شراء ${fmt(m.cost)} جنيه")
                    }}
                }
            },
            confirmButton={TextButton(onClick={materialsOpen=false}){Text("تمام")}}
        )
    }

}

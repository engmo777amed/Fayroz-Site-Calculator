package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun SectionScreen(
    project:Project,
    section:Section,
    active:ActiveLocation?,
    onBack:()->Unit,
    onAddRoom:()->Unit,
    onEditRoom:(String)->Unit,
    onSetActiveRoom:(String)->Unit,
    onDetachCopy:(String)->Unit
){
    val summary=QuantityEngine.summarize(section)

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text(section.name,fontWeight=FontWeight.Black)
                    Text(project.name,style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
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
                                if(space.repeatCount>1){
                                    AssistChip(
                                        onClick={onDetachCopy(space.id)},
                                        label={Text("فصل نسخة مختلفة")},
                                        leadingIcon={Icon(Icons.Rounded.CallSplit,null,Modifier.size(16.dp))}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.Project
import java.util.UUID

@Composable
fun ProjectsScreen(
    projects:List<Project>,
    activeProjectId:String?,
    onOpen:(String)->Unit,
    onSetActive:(String)->Unit,
    onCreate:(String,String)->Unit,
    onUpdate:(Project)->Unit,
    onDelete:(String)->Unit
){
    var showArchived by remember{mutableStateOf(false)}
    var delete by remember{mutableStateOf<Project?>(null)}
    var search by remember{mutableStateOf("")}
    var createOpen by remember{mutableStateOf(false)}
    val q=search.trim()
    val eligible=projects.filter{it.archived==showArchived}
    val filtered=if(q.isBlank())eligible else eligible.filter{
        it.name.contains(q,true)||it.type.contains(q,true)||
            it.sections.any{s->s.name.contains(q,true)||s.spaces.any{x->x.name.contains(q,true)}}
    }

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{
            PageHeader("المشروعات","اختار مشروع نشط مرة واحدة"){
                FilledTonalButton(onClick={createOpen=true}){
                    Icon(Icons.Rounded.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("مشروع")
                }
            }
        }
        item{Row{Checkbox(showArchived,{showArchived=it});Text("عرض الأرشيف",Modifier.padding(top=12.dp))}}
        if(projects.isNotEmpty())item{
            TextFieldX("بحث",search,{search=it},placeholder="اسم مشروع أو دور أو غرفة")
        }
        if(filtered.isEmpty()){
            item{EmptyState(
                if(q.isBlank())"مفيش مشروعات لسه" else "مفيش نتيجة",
                if(q.isBlank())"اعمل أول مشروع وخليه المشروع النشط." else "جرّب كلمة بحث تانية.",
                if(q.isBlank())"اعمل مشروع" else null,
                if(q.isBlank()){{createOpen=true}} else null
            )}
        }else{
            items(filtered.size){index->
                val p=filtered[index]
                val total=p.sections.sumOf{it.spaces.size}
                val done=p.sections.sumOf{s->s.spaces.count{x->x.status.name=="DONE"||x.status.name=="REVIEWED"}}
                Surface(
                    onClick={onOpen(p.id)},
                    shape=RoundedCornerShape(17.dp),
                    tonalElevation=1.dp
                ){
                    Column(Modifier.fillMaxWidth().padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                Icon(Icons.Rounded.Apartment,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(p.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Text("${p.type} • ${p.sections.size} جزء • $total مكان",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if(p.id==activeProjectId){
                                AssistChip(onClick={},label={Text("نشط")},leadingIcon={Icon(Icons.Rounded.CheckCircle,null,Modifier.size(16.dp))})
                            }else{
                                TextButton(onClick={onSetActive(p.id)}){Text("خليه نشط")}
                            }
                        }
                        Row{
                            TextButton(onClick={onUpdate(p.copy(id=UUID.randomUUID().toString(),name=p.name+" — نسخة",sections=p.sections.map{it.copy(id=UUID.randomUUID().toString(),spaces=it.spaces.map(::copySpace))},calculations=emptyList(),archived=false,createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis()))}){Text("نسخ")}
                            TextButton(onClick={onUpdate(p.copy(archived=!p.archived))}){Text(if(p.archived)"استرجاع" else "أرشفة")}
                            TextButton(onClick={delete=p}){Text("حذف")}
                        }
                        if(total>0){
                            LinearProgressIndicator(
                                progress={done.toFloat()/total.toFloat()},
                                modifier=Modifier.fillMaxWidth(),
                                color=MaterialTheme.colorScheme.secondary
                            )
                            Text("خلص $done من $total",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    delete?.let{p->AlertDialog(onDismissRequest={delete=null},title={Text("حذف ${p.name}؟")},text={Text("سيُحذف المشروع وحصره. احفظ نسخة احتياطية إذا احتجته لاحقًا.")},confirmButton={TextButton(onClick={onDelete(p.id);delete=null}){Text("حذف")}},dismissButton={TextButton(onClick={delete=null}){Text("إلغاء")}})}
    if(createOpen){
        NewProjectDialog(
            onDismiss={createOpen=false},
            onCreate={name,type->createOpen=false;onCreate(name,type)}
        )
    }
}

@Composable
private fun NewProjectDialog(onDismiss:()->Unit,onCreate:(String,String)->Unit){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("مشروع جديد",fontWeight=FontWeight.Black)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                TextFieldX("اسم المشروع",name,{name=it},placeholder="مثال: شقة أحمد - سموحة")
                ChoiceFieldX("نوع المشروع",type,listOf("شقة","بيت","عمارة","محل","مشروع مخصص"),{type=it})
            }
        },
        confirmButton={
            Button(onClick={if(name.isNotBlank())onCreate(name.trim(),type)},enabled=name.isNotBlank()){Text("إنشاء")}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}}
    )
}

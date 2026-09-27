package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine

private val projectTypes=listOf("شقة","بيت","عمارة","محل","مشروع مخصص")

@Composable
fun ProjectsScreen(
    projects:List<SiteProject>,
    onNew:()->Unit,
    onOpen:(String)->Unit,
    canUndo:Boolean,
    onUndo:()->Unit
){
    var search by remember{mutableStateOf("")}
    val q=search.trim()
    val filtered=if(q.isBlank())projects else projects.filter{p->
        p.name.contains(q,true)||p.type.contains(q,true)||
            p.sections.any{s->s.name.contains(q,true)||s.spaces.any{x->x.name.contains(q,true)||x.note.contains(q,true)}}
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{
            SectionTitle(
                "المشروعات",
                "مشروع ← دور/جزء ← غرف وفراغات.",
                trailing={
                    Row{
                        if(canUndo){
                            IconButton(onClick=onUndo,modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Undo,"تراجع")}
                        }
                        FilledTonalButton(onClick=onNew,contentPadding=PaddingValues(horizontal=10.dp,vertical=7.dp)){
                            Icon(Icons.Rounded.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("مشروع")
                        }
                    }
                }
            )
        }

        if(projects.isNotEmpty()) item{
            TextFieldX("بحث",search,{search=it},"ابحث باسم المشروع أو الدور أو الغرفة.","مشروع أو غرفة...")
        }

        if(filtered.isEmpty()){
            item{EmptyBlock(if(q.isBlank())"لا توجد مشروعات" else "لا توجد نتيجة",if(q.isBlank())"أنشئ أول مشروع وابدأ الحصر." else "جرّب كلمة بحث أخرى.",Icons.Rounded.FolderOpen)}
        }else item{
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                filtered.forEach{p->
                    val total=p.sections.sumOf{it.spaces.size}
                    val complete=p.sections.sumOf{s->s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}}
                    Surface(
                        onClick={onOpen(p.id)},shape=RoundedCornerShape(16.dp),
                        color=MaterialTheme.colorScheme.surface,
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                    ){
                        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                    Icon(Icons.Rounded.Apartment,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(p.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text("${p.type} • ${p.sections.size} جزء • $total غرفة",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.primary)
                            }
                            if(total>0){
                                LinearProgressIndicator(
                                    progress={complete.toFloat()/total.toFloat()},
                                    modifier=Modifier.fillMaxWidth(),
                                    color=MaterialTheme.colorScheme.secondary,
                                    trackColor=MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text("تم $complete من $total",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewProjectScreen(onCreate:(String,String)->Unit,onBack:()->Unit){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}
    Scaffold(topBar={AppBarX("مشروع جديد","اسم ونوع فقط",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it).imePadding(),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    TextFieldX("اسم المشروع",name,{name=it},"اسم واضح يسهل الرجوع إليه.","مثال: شقة أحمد - سموحة")
                    ChoiceFieldX("نوع المشروع",type,projectTypes,{type=it},"يحدد أول تقسيم افتراضي فقط.")
                    Button(onClick={onCreate(name.trim(),type)},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                        Icon(Icons.Rounded.Check,null);Spacer(Modifier.width(5.dp));Text("إنشاء المشروع")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectDetailScreen(
    project:SiteProject,
    onBack:()->Unit,
    onOpenSection:(String)->Unit,
    onAddSection:(String)->Unit,
    onDuplicateSection:(String)->Unit,
    onMoveSection:(String,Int)->Unit,
    onRenameProject:(String)->Unit,
    onDeleteProject:()->Unit,
    onShare:()->Unit,
    onExportCsv:()->Unit,
    onExportPdf:()->Unit
){
    var add by remember{mutableStateOf(false)}
    var addName by remember{mutableStateOf("")}
    var rename by remember{mutableStateOf(false)}
    var renameText by remember(project.id){mutableStateOf(project.name)}
    var delete by remember{mutableStateOf(false)}
    var showSummary by remember{mutableStateOf(false)}
    var drillKey by remember{mutableStateOf<Pair<String,MeasureUnit>?>(null)}
    val total=project.sections.sumOf{it.spaces.size}
    val complete=project.sections.sumOf{s->s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}}

    Scaffold(
        topBar={
            AppBarX(project.name,"${project.type} • $complete/$total مكتمل",onBack,actions={
                IconButton(onClick={showSummary=!showSummary},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Assessment,"الملخص")}
                var menu by remember{mutableStateOf(false)}
                Box{
                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        DropdownMenuItem(text={Text("تعديل الاسم")},leadingIcon={Icon(Icons.Rounded.Edit,null)},onClick={menu=false;rename=true})
                        DropdownMenuItem(text={Text("مشاركة ملخص")},leadingIcon={Icon(Icons.Rounded.Share,null)},onClick={menu=false;onShare()})
                        DropdownMenuItem(text={Text("تصدير CSV")},leadingIcon={Icon(Icons.Rounded.TableView,null)},onClick={menu=false;onExportCsv()})
                        DropdownMenuItem(text={Text("تصدير PDF")},leadingIcon={Icon(Icons.Rounded.PictureAsPdf,null)},onClick={menu=false;onExportPdf()})
                        DropdownMenuItem(text={Text("حذف المشروع",color=MaterialTheme.colorScheme.error)},leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},onClick={menu=false;delete=true})
                    }
                }
            })
        },
        floatingActionButton={ExtendedFloatingActionButton(onClick={add=true},icon={Icon(Icons.Rounded.Add,null)},text={Text("دور / جزء")})}
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=10.dp,bottom=88.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("تقدم الحصر",style=MaterialTheme.typography.labelLarge)
                            Text("$complete من $total غرفة/فراغ",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if(total==0)"0%" else "${(complete*100/total)}%",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.secondary)
                    }
                    LinearProgressIndicator(
                        progress={if(total==0)0f else complete.toFloat()/total.toFloat()},
                        modifier=Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if(showSummary)item{
                CardBox{
                    SectionTitle("ملخص المشروع","اضغط اسم البند من شاشة التقارير لمعرفة مصدر الرقم.")
                    val summary=QuantityEngine.projectSummary(project)
                    if(summary.isEmpty())Text("لا توجد كميات بعد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    else summary.forEach{(k,v)->
                        Surface(
                            onClick={drillKey=k},
                            shape=RoundedCornerShape(10.dp),
                            color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.30f)
                        ){
                            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                                Text(k.first,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                                Text("${fmt(v)} ${k.second.label}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.ChevronLeft,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item{SectionTitle("الأدوار والأجزاء","افتح الجزء لإدارة الغرف داخله.")}
            if(project.sections.isEmpty()) item{EmptyBlock("لا يوجد تقسيم","أضف دورًا أو جزءًا.",Icons.Rounded.Layers)}
            else item{
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    project.sections.forEachIndexed{index,s->
                        val count=s.spaces.size
                        val reviewed=s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}
                        var sectionMenu by remember(s.id){mutableStateOf(false)}
                        Surface(
                            onClick={onOpenSection(s.id)},shape=RoundedCornerShape(15.dp),
                            color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Row(Modifier.fillMaxWidth().padding(start=10.dp,end=3.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically){
                                Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                    Icon(Icons.Rounded.Layers,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(s.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                    Text("$reviewed / $count مكتمل",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box{
                                    IconButton(onClick={sectionMenu=true},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.MoreVert,"إجراءات الدور")}
                                    DropdownMenu(expanded=sectionMenu,onDismissRequest={sectionMenu=false}){
                                        DropdownMenuItem(
                                            text={Text("نسخ الدور/الجزء")},
                                            leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},
                                            onClick={sectionMenu=false;onDuplicateSection(s.id)}
                                        )
                                        if(index>0)DropdownMenuItem(
                                            text={Text("تحريك لأعلى")},
                                            leadingIcon={Icon(Icons.Rounded.KeyboardArrowUp,null)},
                                            onClick={sectionMenu=false;onMoveSection(s.id,-1)}
                                        )
                                        if(index<project.sections.lastIndex)DropdownMenuItem(
                                            text={Text("تحريك لأسفل")},
                                            leadingIcon={Icon(Icons.Rounded.KeyboardArrowDown,null)},
                                            onClick={sectionMenu=false;onMoveSection(s.id,1)}
                                        )
                                    }
                                }
                                Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    drillKey?.let{key->
        val contributions=buildList{
            project.sections.forEach{section->
                section.spaces.forEach{space->
                    space.takeoffs.filter{it.name==key.first && it.unit==key.second}.forEach{item->
                        val value=QuantityEngine.calculate(space,item).final
                        if(value>0)add(Triple(section.name,space.name,value))
                    }
                }
            }
        }
        AlertDialog(
            onDismissRequest={drillKey=null},
            title={Text(key.first,fontWeight=FontWeight.Black)},
            text={
                LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    if(contributions.isEmpty())item{Text("لا توجد تفاصيل.")}
                    else{
                        val grouped=contributions.groupBy{it.first}
                        grouped.forEach{(sectionName,rows)->
                            item{
                                Text(sectionName,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
                                rows.forEach{row->
                                    Row(Modifier.fillMaxWidth().padding(vertical=3.dp)){
                                        Text(row.second,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                                        Text("${fmt(row.third)} ${key.second.label}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton={TextButton(onClick={drillKey=null}){Text("إغلاق")}}
        )
    }

    if(add) AlertDialog(
        onDismissRequest={add=false},title={Text("إضافة دور أو جزء")},
        text={OutlinedTextField(addName,{addName=it},singleLine=true,placeholder={Text("مثال: الدور الأول")})},
        confirmButton={TextButton(onClick={if(addName.isNotBlank()){onAddSection(addName.trim());addName="";add=false}}){Text("إضافة")}},
        dismissButton={TextButton(onClick={add=false}){Text("إلغاء")}}
    )
    if(rename) AlertDialog(
        onDismissRequest={rename=false},title={Text("تعديل اسم المشروع")},
        text={OutlinedTextField(renameText,{renameText=it},singleLine=true)},
        confirmButton={TextButton(onClick={if(renameText.isNotBlank()){onRenameProject(renameText.trim());rename=false}}){Text("حفظ")}},
        dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
    )
    if(delete) AlertDialog(
        onDismissRequest={delete=false},title={Text("حذف المشروع؟")},
        text={Text("سيتم حذف المشروع وكل بيانات الحصر داخله.")},
        confirmButton={TextButton(onClick={delete=false;onDeleteProject()}){Text("حذف")}},
        dismissButton={TextButton(onClick={delete=false}){Text("إلغاء")}}
    )
}

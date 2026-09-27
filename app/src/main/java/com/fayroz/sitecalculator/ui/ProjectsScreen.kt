package com.fayroz.sitecalculator.ui

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine

private val projectTypes=listOf("شقة","بيت","عمارة","محل","مشروع مخصص")

@Composable
fun ProjectsListScreen(
    projects:List<SiteProject>,
    onNew:()->Unit,
    onOpen:(String)->Unit
){
    var search by remember{mutableStateOf("")}
    val q=search.trim()
    val filtered=if(q.isBlank())projects else projects.filter{p->
        p.name.contains(q,true)||p.type.contains(q,true)||
            p.sections.any{s->s.name.contains(q,true)||s.rooms.any{r->r.name.contains(q,true)||r.note.contains(q,true)}}
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{
            SectionTitle(
                "المشروعات",
                "كل مشروع منفصل عن الحسابات السريعة.",
                trailing={
                    FilledTonalButton(onClick=onNew,contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){
                        Icon(Icons.Rounded.Add,null,Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مشروع")
                    }
                }
            )
        }

        if(projects.isNotEmpty()){
            item{LabeledText("بحث",search,{search=it},"اسم مشروع أو دور أو غرفة","البحث داخل بيانات المشروع كلها.")}
        }

        if(filtered.isEmpty()){
            item{
                EmptyBlock(
                    if(q.isBlank())"لا توجد مشروعات" else "لا توجد نتيجة",
                    if(q.isBlank())"أنشئ أول مشروع من زر «مشروع»." else "جرّب كلمة بحث مختلفة.",
                    Icons.Rounded.FolderOpen
                )
            }
        }else{
            item{
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                    filtered.forEach{p->
                        val rooms=p.sections.sumOf{s->s.rooms.sumOf{it.repeatCount}}
                        Surface(
                            onClick={onOpen(p.id)},
                            shape=RoundedCornerShape(16.dp),
                            color=MaterialTheme.colorScheme.surface,
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Surface(shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                    Icon(Icons.Rounded.Apartment,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(p.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text("${p.type} • ${p.sections.size} جزء • $rooms غرفة/فراغ",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewProjectScreenV6(
    onCreate:(String,String)->Unit,
    onBack:()->Unit
){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}

    Scaffold(topBar={AppBar("مشروع جديد","خطوة واحدة للبدء",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{BrandHeader("مشروع جديد","سجّل الاسم والنوع ثم ابدأ الغرف.")}
            item{
                CardBox{
                    LabeledText("اسم المشروع",name,{name=it},"مثال: شقة أحمد","اكتب اسمًا يسهل تمييزه لاحقًا.")
                    ChoiceField("نوع المشروع",type,projectTypes,{type=it},"البيت والعمارة يبدآن بالدور الأرضي.")
                    Button(
                        onClick={onCreate(name.trim(),type)},
                        enabled=name.isNotBlank(),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Icon(Icons.Rounded.Check,null)
                        Spacer(Modifier.width(5.dp))
                        Text("إنشاء وفتح المشروع")
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
    onRenameProject:(String)->Unit,
    onDeleteProject:()->Unit
){
    val context=LocalContext.current
    var add by remember{mutableStateOf(false)}
    var addName by remember{mutableStateOf("")}
    var rename by remember{mutableStateOf(false)}
    var renameText by remember(project.id){mutableStateOf(project.name)}
    var delete by remember{mutableStateOf(false)}
    var showSummary by remember{mutableStateOf(false)}
    val rooms=project.sections.sumOf{s->s.rooms.sumOf{it.repeatCount}}

    Scaffold(
        topBar={
            AppBar(
                project.name,
                "${project.type} • $rooms غرفة/فراغ",
                onBack,
                actions={
                    IconButton(onClick={showSummary=!showSummary}){Icon(Icons.Rounded.Assessment,"الملخص")}
                    var menu by remember{mutableStateOf(false)}
                    Box{
                        IconButton(onClick={menu=true}){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                            DropdownMenuItem(
                                text={Text("تعديل الاسم")},
                                leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                onClick={menu=false;rename=true}
                            )
                            DropdownMenuItem(
                                text={Text("مشاركة الملخص")},
                                leadingIcon={Icon(Icons.Rounded.Share,null)},
                                onClick={
                                    menu=false
                                    val report=buildString{
                                        appendLine("FAYROZ SITE CALCULATOR")
                                        appendLine("المشروع: ${project.name}")
                                        appendLine("النوع: ${project.type}")
                                        appendLine("--------------------")
                                        QuantityEngine.projectSummary(project).forEach{(w,v)->
                                            appendLine("$w: ${fmt(v)} ${QuantityEngine.unit(w)}")
                                        }
                                    }
                                    val intent=Intent(Intent.ACTION_SEND).apply{
                                        type="text/plain"
                                        putExtra(Intent.EXTRA_TEXT,report)
                                    }
                                    context.startActivity(Intent.createChooser(intent,"مشاركة الحصر"))
                                }
                            )
                            DropdownMenuItem(
                                text={Text("حذف المشروع",color=MaterialTheme.colorScheme.error)},
                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                onClick={menu=false;delete=true}
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton={
            ExtendedFloatingActionButton(
                onClick={add=true},
                icon={Icon(Icons.Rounded.Add,null)},
                text={Text("دور / جزء")}
            )
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=10.dp,bottom=86.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                BrandHeader(
                    project.name,
                    project.type,
                    listOf(
                        "الأجزاء" to project.sections.size.toString(),
                        "الغرف" to rooms.toString(),
                        "البنود" to QuantityEngine.projectSummary(project).size.toString()
                    )
                )
            }

            if(showSummary){
                item{
                    CardBox{
                        SectionTitle("ملخص الكميات","إجمالي المشروع حسب البند.")
                        val summary=QuantityEngine.projectSummary(project)
                        if(summary.isEmpty()){
                            Text("لا توجد كميات بعد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }else{
                            summary.forEach{(w,v)->MetricRow(w,"${fmt(v)} ${QuantityEngine.unit(w)}")}
                        }
                    }
                }
            }

            item{SectionTitle("التقسيم الداخلي","افتح الدور أو الجزء ثم أضف الغرف داخله.")}

            if(project.sections.isEmpty()){
                item{EmptyBlock("لا يوجد تقسيم بعد","أضف دورًا أو جزءًا للبدء.",Icons.Rounded.Layers)}
            }else{
                item{
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                        project.sections.forEach{s->
                            Surface(
                                onClick={onOpenSection(s.id)},
                                shape=RoundedCornerShape(15.dp),
                                color=MaterialTheme.colorScheme.surface,
                                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                            ){
                                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                                    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                        Icon(Icons.Rounded.Layers,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)){
                                        Text(s.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                        Text("${s.rooms.sumOf{it.repeatCount}} غرفة/فراغ",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if(add){
        AlertDialog(
            onDismissRequest={add=false},
            title={Text("إضافة دور أو جزء")},
            text={OutlinedTextField(addName,{addName=it},singleLine=true,placeholder={Text("مثال: الدور الأول")})},
            confirmButton={TextButton(onClick={
                if(addName.isNotBlank()){
                    onAddSection(addName.trim())
                    addName=""
                    add=false
                }
            }){Text("إضافة")}},
            dismissButton={TextButton(onClick={add=false}){Text("إلغاء")}}
        )
    }

    if(rename){
        AlertDialog(
            onDismissRequest={rename=false},
            title={Text("تعديل اسم المشروع")},
            text={OutlinedTextField(renameText,{renameText=it},singleLine=true)},
            confirmButton={TextButton(onClick={
                if(renameText.isNotBlank()){
                    onRenameProject(renameText.trim())
                    rename=false
                }
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
        )
    }

    if(delete){
        AlertDialog(
            onDismissRequest={delete=false},
            title={Text("حذف المشروع؟")},
            text={Text("سيتم حذف المشروع وكل بياناته.")},
            confirmButton={TextButton(onClick={delete=false;onDeleteProject()}){Text("حذف")}},
            dismissButton={TextButton(onClick={delete=false}){Text("إلغاء")}}
        )
    }
}

@Composable
fun SectionDetailScreen(
    projectName:String,
    section:SectionEntry,
    onBack:()->Unit,
    onAddRoom:()->Unit,
    onEditRoom:(String)->Unit,
    onCopyRoom:(String)->Unit,
    onDeleteRoom:(String)->Unit,
    onRename:(String)->Unit,
    onDeleteSection:()->Unit
){
    var rename by remember{mutableStateOf(false)}
    var name by remember(section.id){mutableStateOf(section.name)}
    var deleteSection by remember{mutableStateOf(false)}
    var summaryExpanded by remember{mutableStateOf(false)}
    val summary=QuantityEngine.sectionSummary(section)

    Scaffold(
        topBar={
            AppBar(
                section.name,
                projectName,
                onBack,
                actions={
                    IconButton(onClick={summaryExpanded=!summaryExpanded}){Icon(Icons.Rounded.Assessment,"الملخص")}
                    var menu by remember{mutableStateOf(false)}
                    Box{
                        IconButton(onClick={menu=true}){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                            DropdownMenuItem(
                                text={Text("تعديل الاسم")},
                                leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                onClick={menu=false;rename=true}
                            )
                            DropdownMenuItem(
                                text={Text("حذف الجزء",color=MaterialTheme.colorScheme.error)},
                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                onClick={menu=false;deleteSection=true}
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton={
            ExtendedFloatingActionButton(
                onClick=onAddRoom,
                icon={Icon(Icons.Rounded.Add,null)},
                text={Text("غرفة")}
            )
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=10.dp,bottom=86.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            if(summaryExpanded){
                item{
                    CardBox{
                        SectionTitle("ملخص ${section.name}","إجمالي البنود داخل هذا الجزء.")
                        if(summary.isEmpty()){
                            Text("لا توجد كميات بعد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }else{
                            summary.forEach{(w,v)->MetricRow(w,"${fmt(v)} ${QuantityEngine.unit(w)}")}
                        }
                    }
                }
            }

            item{SectionTitle("الغرف والفراغات","اضغط ⋮ للتعديل أو النسخ أو الحذف.")}

            if(section.rooms.isEmpty()){
                item{EmptyBlock("لا توجد غرف","اضغط زر «غرفة» لإضافة أول فراغ.",Icons.Rounded.MeetingRoom)}
            }else{
                item{
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                        section.rooms.forEach{r->
                            var menu by remember(r.id){mutableStateOf(false)}
                            Surface(
                                shape=RoundedCornerShape(15.dp),
                                color=MaterialTheme.colorScheme.surface,
                                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                            ){
                                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                                    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                        Icon(Icons.Rounded.MeetingRoom,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)){
                                        Text(r.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                        Text(
                                            "${r.type} • ${fmt(r.length)} × ${fmt(r.width)} م${if(r.repeatCount>1)" • ×${r.repeatCount}" else ""}",
                                            style=MaterialTheme.typography.bodySmall,
                                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines=1
                                        )
                                        if(r.note.isNotBlank()){
                                            Text(r.note,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                                        }
                                    }
                                    Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                        Text("${r.works.size} بنود",Modifier.padding(horizontal=7.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                                    }
                                    Box{
                                        IconButton(onClick={menu=true},modifier=Modifier.size(34.dp)){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                            DropdownMenuItem(text={Text("تعديل")},leadingIcon={Icon(Icons.Rounded.Edit,null)},onClick={menu=false;onEditRoom(r.id)})
                                            DropdownMenuItem(text={Text("نسخ")},leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},onClick={menu=false;onCopyRoom(r.id)})
                                            DropdownMenuItem(text={Text("حذف",color=MaterialTheme.colorScheme.error)},leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},onClick={menu=false;onDeleteRoom(r.id)})
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if(rename){
        AlertDialog(
            onDismissRequest={rename=false},
            title={Text("تعديل الاسم")},
            text={OutlinedTextField(name,{name=it},singleLine=true)},
            confirmButton={TextButton(onClick={
                if(name.isNotBlank()){
                    onRename(name.trim())
                    rename=false
                }
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
        )
    }

    if(deleteSection){
        AlertDialog(
            onDismissRequest={deleteSection=false},
            title={Text("حذف الجزء؟")},
            text={Text("سيتم حذف كل الغرف الموجودة داخله.")},
            confirmButton={TextButton(onClick={deleteSection=false;onDeleteSection()}){Text("حذف")}},
            dismissButton={TextButton(onClick={deleteSection=false}){Text("إلغاء")}}
        )
    }
}

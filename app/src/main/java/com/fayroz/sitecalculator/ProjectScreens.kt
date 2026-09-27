package com.fayroz.sitecalculator

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
import com.fayroz.sitecalculator.ui.*

private val projectTypes=listOf("شقة","بيت","عمارة","محل","مشروع مخصص")

private fun shareText(context:android.content.Context,title:String,text:String){
    val intent=Intent(Intent.ACTION_SEND).apply{
        type="text/plain"
        putExtra(Intent.EXTRA_SUBJECT,title)
        putExtra(Intent.EXTRA_TEXT,text)
    }
    context.startActivity(Intent.createChooser(intent,"مشاركة الحصر"))
}

@Composable
fun HomeScreen(
    projects:List<SiteProject>,
    appearance:FayrozAppearance,
    onAppearance:(FayrozAppearance)->Unit,
    onNewProject:()->Unit,
    onOpenProject:(String)->Unit,
    onSpace:()->Unit,
    onItem:()->Unit
){
    var search by remember{mutableStateOf("")}
    val q=search.trim()
    val filtered=if(q.isBlank()) projects else projects.filter{project->
        project.name.contains(q,true) ||
            project.type.contains(q,true) ||
            project.sections.any{section->
                section.name.contains(q,true) ||
                    section.spaces.any{space->
                        space.name.contains(q,true) ||
                            space.type.contains(q,true) ||
                            space.note.contains(q,true)
                    }
            }
    }
    val roomCount=projects.sumOf{p->p.sections.sumOf{s->s.spaces.sumOf{it.repeatCount}}}

    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{BrandHero(projects.size,roomCount)}

        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                QuickActionCard(
                    title="حساب فراغ",
                    subtitle="غرفة كاملة",
                    icon=Icons.Rounded.MeetingRoom,
                    onClick=onSpace,
                    accent=true,
                    modifier=Modifier.weight(1f)
                )
                QuickActionCard(
                    title="حساب بند",
                    subtitle="غرفة / حوائط",
                    icon=Icons.Rounded.Calculate,
                    onClick=onItem,
                    modifier=Modifier.weight(1f)
                )
            }
        }

        item{AppearanceSelector(appearance,onAppearance)}

        item{
            SectionHeader(
                title="مشروعات الموقع",
                subtitle=if(projects.isEmpty())"ابدأ مشروعًا واحفظ الغرف والكميات داخله." else "المشروع ← الدور أو الجزء ← الغرف والفراغات.",
                trailing={
                    FilledTonalButton(
                        onClick=onNewProject,
                        contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)
                    ){
                        Icon(Icons.Rounded.Add,null,Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("مشروع")
                    }
                }
            )
        }

        if(projects.isNotEmpty()){
            item{
                TextFieldSimple(
                    label="بحث",
                    value=search,
                    onValue={search=it},
                    help="ابحث باسم المشروع أو الدور أو الغرفة أو الملاحظة.",
                    placeholder="اكتب اسم مشروع أو غرفة..."
                )
            }
        }

        if(filtered.isEmpty()){
            item{
                EmptyState(
                    title=if(q.isBlank())"لا توجد مشروعات محفوظة" else "لا توجد نتيجة",
                    subtitle=if(q.isBlank())"اضغط «مشروع» وابدأ تسجيل الحصر." else "جرّب اسمًا آخر.",
                    icon=Icons.Rounded.FolderOpen
                )
            }
        }else{
            item{
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                    filtered.forEach{project->
                        val rooms=project.sections.sumOf{sec->sec.spaces.sumOf{it.repeatCount}}
                        Surface(
                            modifier=Modifier.fillMaxWidth(),
                            onClick={onOpenProject(project.id)},
                            shape=RoundedCornerShape(16.dp),
                            color=MaterialTheme.colorScheme.surface,
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Surface(shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                    Icon(Icons.Rounded.Apartment,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(project.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(
                                        "${project.type} • ${project.sections.size} جزء • $rooms غرفة/فراغ",
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines=1
                                    )
                                }
                                Icon(Icons.Rounded.ChevronLeft,"فتح",tint=MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewProjectScreen(
    onCreate:(String,String)->Unit,
    onBack:()->Unit
){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}

    Scaffold(topBar={AppTopBar("مشروع جديد","اسم واضح ثم ابدأ الأدوار والغرف",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                SurfaceCard{
                    SectionHeader("بيانات المشروع","مثال: شقة أحمد - سموحة")
                    TextFieldSimple(
                        "اسم المشروع",
                        name,
                        {name=it},
                        "اكتب اسمًا يسهل الرجوع إليه لاحقًا.",
                        "مثال: شقة أحمد"
                    )
                    SelectField(
                        "نوع المشروع",
                        type,
                        projectTypes,
                        {type=it},
                        "البيت والعمارة يبدأان بدور أرضي ويمكن إضافة أدوار أخرى."
                    )
                    Button(
                        onClick={onCreate(name.trim(),type)},
                        enabled=name.isNotBlank(),
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Icon(Icons.Rounded.Check,null)
                        Spacer(Modifier.width(5.dp))
                        Text("إنشاء المشروع")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryByWork(
    project:SiteProject?=null,
    section:ProjectSection?=null
){
    val expanded=remember{mutableStateMapOf<String,Boolean>()}
    val summary=project?.let{QuantityEngine.projectSummary(it)}
        ?: section?.let{QuantityEngine.sectionSummary(it)}
        ?: emptyMap()

    if(summary.isEmpty()){
        EmptyState("لا توجد كميات بعد","أضف غرفة وحدد البنود المطلوب حسابها.",Icons.Rounded.Assessment)
        return
    }

    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        summary.forEach{(work,total)->
            Surface(
                onClick={expanded[work]=!(expanded[work]?:false)},
                shape=RoundedCornerShape(14.dp),
                color=MaterialTheme.colorScheme.surface,
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(work,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                        Text(
                            "${qty(total)} ${QuantityEngine.unit(work)}",
                            color=MaterialTheme.colorScheme.primary,
                            style=MaterialTheme.typography.titleMedium,
                            fontWeight=FontWeight.Black
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if(expanded[work]==true) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null,
                            Modifier.size(19.dp),
                            tint=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if(expanded[work]==true){
                        HorizontalDivider()
                        val sections=project?.sections ?: listOfNotNull(section)
                        sections.forEach{sec->
                            val matching=sec.spaces.filter{work in it.works}
                            if(matching.isNotEmpty()){
                                if(project!=null){
                                    Text(sec.name,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                                }
                                matching.forEach{space->
                                    Row(Modifier.fillMaxWidth()){
                                        Text(
                                            space.name + if(space.repeatCount>1)" ×${space.repeatCount}" else "",
                                            Modifier.weight(1f),
                                            style=MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "${qty(QuantityEngine.quantity(space,work))} ${QuantityEngine.unit(work)}",
                                            style=MaterialTheme.typography.bodySmall,
                                            color=MaterialTheme.colorScheme.onSurfaceVariant
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
}

private fun projectSummaryText(project:SiteProject):String{
    val summary=QuantityEngine.projectSummary(project)
    return buildString{
        appendLine("FAYROZ SITE CALCULATOR")
        appendLine("المشروع: ${project.name}")
        appendLine("النوع: ${project.type}")
        appendLine("--------------------")
        summary.forEach{(work,total)->appendLine("$work: ${qty(total)} ${QuantityEngine.unit(work)}")}
    }
}

private fun sectionSummaryText(projectName:String,section:ProjectSection):String{
    val summary=QuantityEngine.sectionSummary(section)
    return buildString{
        appendLine("FAYROZ SITE CALCULATOR")
        appendLine("المشروع: $projectName")
        appendLine("الدور/الجزء: ${section.name}")
        appendLine("--------------------")
        summary.forEach{(work,total)->appendLine("$work: ${qty(total)} ${QuantityEngine.unit(work)}")}
    }
}

@Composable
fun ProjectScreen(
    project:SiteProject,
    onOpenSection:(String)->Unit,
    onAddSection:(String)->Unit,
    onRenameProject:(String)->Unit,
    onDeleteProject:()->Unit,
    onRenameSection:(String,String)->Unit,
    onDeleteSection:(String)->Unit,
    onBack:()->Unit
){
    val context=LocalContext.current
    var tab by remember{mutableIntStateOf(0)}
    var showAdd by remember{mutableStateOf(false)}
    var sectionName by remember{mutableStateOf("")}
    var renameProject by remember{mutableStateOf(false)}
    var newProjectName by remember(project.id){mutableStateOf(project.name)}
    var deleteProjectConfirm by remember{mutableStateOf(false)}
    var renameSectionId by remember{mutableStateOf<String?>(null)}
    var renameSectionName by remember{mutableStateOf("")}
    var deleteSectionId by remember{mutableStateOf<String?>(null)}
    var projectMenu by remember{mutableStateOf(false)}

    val multi=project.type in listOf("بيت","عمارة","مشروع مخصص")
    val totalRooms=project.sections.sumOf{it.spaces.sumOf{space->space.repeatCount}}

    Scaffold(
        topBar={
            AppTopBar(
                title=project.name,
                subtitle="${project.type} • $totalRooms غرفة/فراغ",
                onBack=onBack,
                actions={
                    Box{
                        IconButton(onClick={projectMenu=true}){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                        DropdownMenu(expanded=projectMenu,onDismissRequest={projectMenu=false}){
                            DropdownMenuItem(
                                text={Text("تعديل الاسم")},
                                leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                onClick={projectMenu=false;renameProject=true}
                            )
                            DropdownMenuItem(
                                text={Text("مشاركة ملخص الحصر")},
                                leadingIcon={Icon(Icons.Rounded.Share,null)},
                                onClick={projectMenu=false;shareText(context,project.name,projectSummaryText(project))}
                            )
                            DropdownMenuItem(
                                text={Text("حذف المشروع",color=MaterialTheme.colorScheme.error)},
                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                onClick={projectMenu=false;deleteProjectConfirm=true}
                            )
                        }
                    }
                }
            )
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{StepTabs(listOf("التقسيم","ملخص البنود"),tab){tab=it}}
            if(tab==0){
                item{
                    SectionHeader(
                        if(multi)"الأدوار والأجزاء" else "الغرف والفراغات",
                        if(multi)"افتح الدور ثم سجّل الغرف الموجودة داخله." else "افتح القسم ثم سجّل الغرف."
                    )
                }
                if(project.sections.isEmpty()){
                    item{EmptyState("لا يوجد جزء داخل المشروع","أضف دورًا أو جزءًا للبدء.",Icons.Rounded.Apartment)}
                }else{
                    item{
                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                            project.sections.forEach{sec->
                                var menu by remember(sec.id){mutableStateOf(false)}
                                Surface(
                                    onClick={onOpenSection(sec.id)},
                                    shape=RoundedCornerShape(15.dp),
                                    color=MaterialTheme.colorScheme.surface,
                                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                ){
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),
                                        verticalAlignment=Alignment.CenterVertically
                                    ){
                                        Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                            Icon(
                                                if(multi) Icons.Rounded.Layers else Icons.Rounded.MeetingRoom,
                                                null,
                                                Modifier.padding(7.dp).size(18.dp),
                                                tint=MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)){
                                            Text(sec.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                            Text(
                                                "${sec.spaces.sumOf{it.repeatCount}} غرفة/فراغ",
                                                style=MaterialTheme.typography.bodySmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box{
                                            IconButton(onClick={menu=true},modifier=Modifier.size(34.dp)){
                                                Icon(Icons.Rounded.MoreVert,"إجراءات")
                                            }
                                            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                                DropdownMenuItem(
                                                    text={Text("تعديل الاسم")},
                                                    leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                                    onClick={
                                                        menu=false
                                                        renameSectionId=sec.id
                                                        renameSectionName=sec.name
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                                    leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                                    onClick={menu=false;deleteSectionId=sec.id}
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
                if(multi){
                    item{
                        OutlinedButton(onClick={showAdd=true},modifier=Modifier.fillMaxWidth()){
                            Icon(Icons.Rounded.Add,null)
                            Spacer(Modifier.width(5.dp))
                            Text("إضافة دور أو جزء")
                        }
                    }
                }
            }else{
                item{
                    SectionHeader(
                        "إجمالي المشروع حسب البند",
                        "اضغط على أي بند لمعرفة مصدر الكمية.",
                        trailing={
                            IconButton(onClick={shareText(context,project.name,projectSummaryText(project))}){
                                Icon(Icons.Rounded.Share,"مشاركة")
                            }
                        }
                    )
                }
                item{SummaryByWork(project=project)}
            }
        }
    }

    if(showAdd){
        AlertDialog(
            onDismissRequest={showAdd=false},
            title={Text("إضافة دور أو جزء")},
            text={OutlinedTextField(sectionName,{sectionName=it},singleLine=true,placeholder={Text("مثال: الدور الأول")})},
            confirmButton={TextButton(onClick={
                if(sectionName.isNotBlank()){
                    onAddSection(sectionName.trim());sectionName="";showAdd=false
                }
            }){Text("إضافة")}},
            dismissButton={TextButton(onClick={showAdd=false}){Text("إلغاء")}}
        )
    }
    if(renameProject){
        AlertDialog(
            onDismissRequest={renameProject=false},
            title={Text("تعديل اسم المشروع")},
            text={OutlinedTextField(newProjectName,{newProjectName=it},singleLine=true)},
            confirmButton={TextButton(onClick={
                if(newProjectName.isNotBlank()){onRenameProject(newProjectName.trim());renameProject=false}
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={renameProject=false}){Text("إلغاء")}}
        )
    }
    if(renameSectionId!=null){
        AlertDialog(
            onDismissRequest={renameSectionId=null},
            title={Text("تعديل الاسم")},
            text={OutlinedTextField(renameSectionName,{renameSectionName=it},singleLine=true)},
            confirmButton={TextButton(onClick={
                val id=renameSectionId
                if(id!=null && renameSectionName.isNotBlank()){
                    onRenameSection(id,renameSectionName.trim());renameSectionId=null
                }
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={renameSectionId=null}){Text("إلغاء")}}
        )
    }
    if(deleteProjectConfirm){
        AlertDialog(
            onDismissRequest={deleteProjectConfirm=false},
            title={Text("حذف المشروع؟")},
            text={Text("سيتم حذف المشروع وكل الأدوار والغرف الموجودة داخله.")},
            confirmButton={TextButton(onClick={deleteProjectConfirm=false;onDeleteProject()}){Text("حذف")}},
            dismissButton={TextButton(onClick={deleteProjectConfirm=false}){Text("إلغاء")}}
        )
    }
    if(deleteSectionId!=null){
        AlertDialog(
            onDismissRequest={deleteSectionId=null},
            title={Text("حذف الدور أو الجزء؟")},
            text={Text("سيتم حذف الغرف الموجودة داخله.")},
            confirmButton={TextButton(onClick={deleteSectionId?.let(onDeleteSection);deleteSectionId=null}){Text("حذف")}},
            dismissButton={TextButton(onClick={deleteSectionId=null}){Text("إلغاء")}}
        )
    }
}

@Composable
fun SectionScreen(
    projectName:String,
    section:ProjectSection,
    onAddSpace:()->Unit,
    onEditSpace:(String)->Unit,
    onCopySpace:(String)->Unit,
    onDeleteSpace:(String)->Unit,
    onRenameSection:(String)->Unit,
    onBack:()->Unit
){
    val context=LocalContext.current
    var tab by remember{mutableIntStateOf(0)}
    var rename by remember{mutableStateOf(false)}
    var newName by remember(section.id){mutableStateOf(section.name)}
    var deleteSpaceId by remember{mutableStateOf<String?>(null)}
    val totalRooms=section.spaces.sumOf{it.repeatCount}

    Scaffold(
        topBar={
            AppTopBar(
                title=section.name,
                subtitle="$projectName • $totalRooms غرفة/فراغ",
                onBack=onBack,
                actions={
                    IconButton(onClick={rename=true}){Icon(Icons.Rounded.Edit,"تعديل الاسم")}
                }
            )
        },
        floatingActionButton={
            if(tab==0){
                ExtendedFloatingActionButton(
                    onClick=onAddSpace,
                    icon={Icon(Icons.Rounded.Add,null)},
                    text={Text("غرفة")}
                )
            }
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=12.dp,bottom=82.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{StepTabs(listOf("الغرف","ملخص البنود"),tab){tab=it}}
            if(tab==0){
                item{SectionHeader("الغرف والفراغات","تعديل، نسخ أو حذف من قائمة ⋮")}
                if(section.spaces.isEmpty()){
                    item{EmptyState("لا توجد غرف هنا","اضغط زر «غرفة» وأدخل المقاسات.",Icons.Rounded.MeetingRoom)}
                }else{
                    item{
                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                            section.spaces.forEach{space->
                                var menu by remember(space.id){mutableStateOf(false)}
                                Surface(
                                    shape=RoundedCornerShape(15.dp),
                                    color=MaterialTheme.colorScheme.surface,
                                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                ){
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),
                                        verticalAlignment=Alignment.CenterVertically
                                    ){
                                        Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                            Icon(Icons.Rounded.MeetingRoom,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)){
                                            Text(space.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                            Text(
                                                "${space.type} • ${qty(space.length)} × ${qty(space.width)} م" +
                                                    if(space.repeatCount>1)" • تكرار ×${space.repeatCount}" else "",
                                                style=MaterialTheme.typography.bodySmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines=1
                                            )
                                            if(space.note.isNotBlank()){
                                                Text(space.note,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                                            }
                                        }
                                        Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                            Text(
                                                "${space.works.size} بنود",
                                                Modifier.padding(horizontal=7.dp,vertical=4.dp),
                                                style=MaterialTheme.typography.labelSmall,
                                                color=MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Box{
                                            IconButton(onClick={menu=true},modifier=Modifier.size(34.dp)){
                                                Icon(Icons.Rounded.MoreVert,"إجراءات")
                                            }
                                            DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                                DropdownMenuItem(
                                                    text={Text("تعديل")},
                                                    leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                                    onClick={menu=false;onEditSpace(space.id)}
                                                )
                                                DropdownMenuItem(
                                                    text={Text("نسخ")},
                                                    leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},
                                                    onClick={menu=false;onCopySpace(space.id)}
                                                )
                                                DropdownMenuItem(
                                                    text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                                    leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                                    onClick={menu=false;deleteSpaceId=space.id}
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }else{
                item{
                    SectionHeader(
                        "إجمالي ${section.name}",
                        "اضغط على البند لمعرفة الغرف التي كوّنت الكمية.",
                        trailing={
                            IconButton(onClick={shareText(context,section.name,sectionSummaryText(projectName,section))}){
                                Icon(Icons.Rounded.Share,"مشاركة")
                            }
                        }
                    )
                }
                item{SummaryByWork(section=section)}
            }
        }
    }

    if(rename){
        AlertDialog(
            onDismissRequest={rename=false},
            title={Text("تعديل الاسم")},
            text={OutlinedTextField(newName,{newName=it},singleLine=true)},
            confirmButton={TextButton(onClick={
                if(newName.isNotBlank()){onRenameSection(newName.trim());rename=false}
            }){Text("حفظ")}},
            dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
        )
    }
    if(deleteSpaceId!=null){
        AlertDialog(
            onDismissRequest={deleteSpaceId=null},
            title={Text("حذف الغرفة؟")},
            text={Text("سيتم حذف المقاسات والكميات المحفوظة لهذه الغرفة.")},
            confirmButton={TextButton(onClick={deleteSpaceId?.let(onDeleteSpace);deleteSpaceId=null}){Text("حذف")}},
            dismissButton={TextButton(onClick={deleteSpaceId=null}){Text("إلغاء")}}
        )
    }
}

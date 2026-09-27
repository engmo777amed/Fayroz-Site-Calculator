package com.fayroz.sitecalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.ui.*

private val projectTypes=listOf("شقة","بيت","عمارة","محل","مشروع مخصص")

@Composable
fun NewProjectScreen(
    onCreate:(String,String)->Unit,
    onBack:()->Unit
){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}

    CalculatorPage(
        title="مشروع جديد",
        subtitle="ابدأ باسم واضح للمشروع ثم أضف الأدوار والغرف.",
        onBack=onBack
    ){
        SurfaceCard{
            SectionHeading("بيانات المشروع","مثال: شقة أ/ أحمد أو بيت م/ محمد.")
            TextFieldSimple(
                label="اسم المشروع",
                value=name,
                onValue={name=it},
                help="اكتب اسمًا يسهل عليك معرفة المشروع لاحقًا، مثل: شقة أحمد - سموحة، أو بيت محمد - برج العرب.",
                placeholder="مثال: شقة أحمد"
            )
            SelectField(
                label="نوع المشروع",
                value=type,
                options=projectTypes,
                onValue={type=it},
                help="البيت والعمارة يسمحان بإضافة أكثر من دور. الشقة تبدأ مباشرة بقائمة الغرف."
            )
            Button(
                onClick={if(name.isNotBlank()) onCreate(name.trim(),type)},
                enabled=name.isNotBlank(),
                modifier=Modifier.fillMaxWidth(),
                shape=MaterialTheme.shapes.medium,
                colors=ButtonDefaults.buttonColors(
                    containerColor=MaterialTheme.colorScheme.secondary,
                    contentColor=MaterialTheme.colorScheme.onSecondary
                )
            ){Text("إنشاء المشروع")}
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
        SurfaceCard{
            Text("لا توجد كميات محفوظة حتى الآن.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    summary.forEach{(work,total)->
        Surface(
            onClick={expanded[work]=!(expanded[work]?:false)},
            shape=RoundedCornerShape(14.dp),
            color=MaterialTheme.colorScheme.surface,
            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
        ){
            Column(Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=9.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text(work,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    Text("${qty(total)} ${QuantityEngine.unit(work)}",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
                }
                Text(
                    if(expanded[work]==true)"إخفاء مصدر الكمية" else "اضغط لمعرفة مصدر الكمية",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.tertiary
                )
                if(expanded[work]==true){
                    HorizontalDivider()
                    val sections=project?.sections ?: listOfNotNull(section)
                    sections.forEach{sec->
                        val matching=sec.spaces.filter{work in it.works}
                        if(matching.isNotEmpty()){
                            if(project!=null) Text(sec.name,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            matching.forEach{space->
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                                    Text(
                                        space.name+(if(space.repeatCount>1)" ×${space.repeatCount}" else ""),
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
        Spacer(Modifier.height(6.dp))
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
    var tab by remember{mutableStateOf(0)}
    var showAdd by remember{mutableStateOf(false)}
    var sectionName by remember{mutableStateOf("")}
    var renameProject by remember{mutableStateOf(false)}
    var projectName by remember{mutableStateOf(project.name)}
    var renameSectionId by remember{mutableStateOf<String?>(null)}
    var renameSectionName by remember{mutableStateOf("")}
    var deleteProjectConfirm by remember{mutableStateOf(false)}
    var deleteSectionId by remember{mutableStateOf<String?>(null)}
    val multi=project.type in listOf("بيت","عمارة","مشروع مخصص")
    val totalRooms=project.sections.sumOf{it.spaces.sumOf{space->space.repeatCount}}

    CalculatorPage(
        title=project.name,
        subtitle="${project.type} • $totalRooms غرفة/فراغ",
        onBack=onBack
    ){
        StepTabs(listOf("التقسيم","ملخص البنود"),tab){tab=it}

        if(tab==0){
            SurfaceCard{
                SectionHeading(
                    if(multi)"الأدوار والأجزاء" else "الغرف والفراغات",
                    if(multi)"افتح الدور ثم أضف الغرف الموجودة داخله." else "افتح القسم ثم أضف الغرف."
                )

                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(
                        "${project.sections.size} جزء • $totalRooms غرفة/فراغ",
                        Modifier.weight(1f),
                        style=MaterialTheme.typography.labelMedium,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    var projectMenu by remember{mutableStateOf(false)}
                    Box{
                        IconButton(onClick={projectMenu=true},modifier=Modifier.size(34.dp)){
                            Icon(Icons.Rounded.MoreVert,"إجراءات المشروع")
                        }
                        DropdownMenu(expanded=projectMenu,onDismissRequest={projectMenu=false}){
                            DropdownMenuItem(
                                text={Text("تعديل الاسم")},
                                leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                onClick={projectMenu=false;renameProject=true}
                            )
                            DropdownMenuItem(
                                text={Text("حذف المشروع",color=MaterialTheme.colorScheme.error)},
                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                onClick={projectMenu=false;deleteProjectConfirm=true}
                            )
                        }
                    }
                }

                project.sections.forEach{sec->
                    var sectionMenu by remember(sec.id){mutableStateOf(false)}
                    Surface(
                        onClick={onOpenSection(sec.id)},
                        modifier=Modifier.fillMaxWidth(),
                        shape=MaterialTheme.shapes.medium,
                        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                    ){
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ){
                            Icon(if(multi) Icons.Rounded.Apartment else Icons.Rounded.MeetingRoom,null,tint=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(sec.name,style=MaterialTheme.typography.titleMedium)
                                Text(
                                    "${sec.spaces.sumOf{it.repeatCount}} غرفة/فراغ",
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box{
                                IconButton(onClick={sectionMenu=true},modifier=Modifier.size(34.dp)){
                                    Icon(Icons.Rounded.MoreVert,"إجراءات")
                                }
                                DropdownMenu(expanded=sectionMenu,onDismissRequest={sectionMenu=false}){
                                    DropdownMenuItem(
                                        text={Text("تعديل الاسم")},
                                        leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                        onClick={
                                            sectionMenu=false
                                            renameSectionId=sec.id
                                            renameSectionName=sec.name
                                        }
                                    )
                                    DropdownMenuItem(
                                        text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                        leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                        onClick={sectionMenu=false;deleteSectionId=sec.id}
                                    )
                                }
                            }
                        }
                    }
                }

                if(multi){
                    OutlinedButton(onClick={showAdd=true},modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(5.dp));Text("إضافة دور أو جزء")
                    }
                }
            }
        }else{
            SurfaceCard{
                SectionHeading("إجمالي المشروع حسب البند","اضغط على أي بند لمعرفة الأدوار والغرف التي كوّنت الرقم.")
            }
            SummaryByWork(project=project)
        }

        if(showAdd){
            AlertDialog(
                onDismissRequest={showAdd=false},
                title={Text("إضافة دور أو جزء")},
                text={
                    OutlinedTextField(
                        value=sectionName,
                        onValueChange={sectionName=it},
                        placeholder={Text("مثال: الدور الأول")},
                        singleLine=true
                    )
                },
                confirmButton={
                    TextButton(onClick={
                        if(sectionName.isNotBlank()){
                            onAddSection(sectionName.trim())
                            sectionName=""
                            showAdd=false
                        }
                    }){Text("إضافة")}
                },
                dismissButton={TextButton(onClick={showAdd=false}){Text("إلغاء")}}
            )
        }

        if(renameProject){
            AlertDialog(
                onDismissRequest={renameProject=false},
                title={Text("تعديل اسم المشروع")},
                text={OutlinedTextField(value=projectName,onValueChange={projectName=it},singleLine=true)},
                confirmButton={TextButton(onClick={
                    if(projectName.isNotBlank()){onRenameProject(projectName.trim());renameProject=false}
                }){Text("حفظ")}},
                dismissButton={TextButton(onClick={renameProject=false}){Text("إلغاء")}}
            )
        }

        if(renameSectionId!=null){
            AlertDialog(
                onDismissRequest={renameSectionId=null},
                title={Text("تعديل اسم الدور أو الجزء")},
                text={OutlinedTextField(value=renameSectionName,onValueChange={renameSectionName=it},singleLine=true)},
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
                text={Text("سيتم حذف كل الغرف الموجودة داخله.")},
                confirmButton={TextButton(onClick={
                    deleteSectionId?.let(onDeleteSection)
                    deleteSectionId=null
                }){Text("حذف")}},
                dismissButton={TextButton(onClick={deleteSectionId=null}){Text("إلغاء")}}
            )
        }
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
    var tab by remember{mutableStateOf(0)}
    var deleteSpaceId by remember{mutableStateOf<String?>(null)}
    var rename by remember{mutableStateOf(false)}
    var newName by remember{mutableStateOf(section.name)}
    val totalFloorArea=section.spaces.sumOf{QuantityEngine.floor(it)}

    CalculatorPage(
        title=section.name,
        subtitle="$projectName • ${section.spaces.sumOf{it.repeatCount}} غرفة/فراغ",
        onBack=onBack
    ){
        StepTabs(listOf("الغرف","ملخص البنود"),tab){tab=it}

        if(tab==0){
            SurfaceCard{
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    SectionHeading("الغرف والفراغات","تعديل ونسخ وحذف من قائمة ⋮")
                    IconButton(onClick={rename=true},modifier=Modifier.size(34.dp)){
                        Icon(Icons.Rounded.Edit,"تعديل اسم الدور أو الجزء")
                    }
                }

                if(section.spaces.isEmpty()){
                    Text("لا توجد غرف مسجلة هنا حتى الآن.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                }else{
                    section.spaces.forEach{space->
                        var spaceMenu by remember(space.id){mutableStateOf(false)}
                        Surface(
                            shape=MaterialTheme.shapes.medium,
                            color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f),
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Icon(Icons.Rounded.MeetingRoom,null,tint=MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)){
                                        Text(space.name,style=MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${space.type} • ${qty(space.length)} × ${qty(space.width)} م"+
                                                if(space.repeatCount>1)" • تكرار ×${space.repeatCount}" else "",
                                            style=MaterialTheme.typography.bodySmall,
                                            color=MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text("${space.works.size} بنود",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)
                                    Box{
                                        IconButton(onClick={spaceMenu=true},modifier=Modifier.size(34.dp)){
                                            Icon(Icons.Rounded.MoreVert,"إجراءات")
                                        }
                                        DropdownMenu(expanded=spaceMenu,onDismissRequest={spaceMenu=false}){
                                            DropdownMenuItem(
                                                text={Text("تعديل")},
                                                leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                                onClick={spaceMenu=false;onEditSpace(space.id)}
                                            )
                                            DropdownMenuItem(
                                                text={Text("نسخ")},
                                                leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},
                                                onClick={spaceMenu=false;onCopySpace(space.id)}
                                            )
                                            DropdownMenuItem(
                                                text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                                onClick={spaceMenu=false;deleteSpaceId=space.id}
                                            )
                                        }
                                    }
                                }
                                if(space.note.isNotBlank()) Text("ملاحظة: ${space.note}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                            }
                        }
                    }
                    HorizontalDivider()
                    QuantityRow("إجمالي مساحات الأرضيات",totalFloorArea,"م²")
                }

                Button(
                    onClick=onAddSpace,
                    modifier=Modifier.fillMaxWidth(),
                    shape=MaterialTheme.shapes.medium,
                    colors=ButtonDefaults.buttonColors(
                        containerColor=MaterialTheme.colorScheme.secondary,
                        contentColor=MaterialTheme.colorScheme.onSecondary
                    )
                ){
                    Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(6.dp));Text("إضافة غرفة أو فراغ")
                }
            }
        }else{
            SurfaceCard{
                SectionHeading("إجمالي ${section.name} حسب البند","اضغط على البند لمعرفة الغرف التي كوّنت الكمية.")
            }
            SummaryByWork(section=section)
        }

        if(rename){
            AlertDialog(
                onDismissRequest={rename=false},
                title={Text("تعديل الاسم")},
                text={OutlinedTextField(value=newName,onValueChange={newName=it},singleLine=true)},
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
                confirmButton={TextButton(onClick={
                    deleteSpaceId?.let(onDeleteSpace)
                    deleteSpaceId=null
                }){Text("حذف")}},
                dismissButton={TextButton(onClick={deleteSpaceId=null}){Text("إلغاء")}}
            )
        }
    }
}

@Composable
fun CalculatorPage(
    title:String,
    subtitle:String,
    onBack:()->Unit,
    content:@Composable ColumnScope.()->Unit
){
    Scaffold(
        topBar={
            Surface(
                shadowElevation=3.dp,
                color=MaterialTheme.colorScheme.surface,
                border=BorderStroke(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    TextButton(onClick=onBack){Text("رجوع")}
                    Surface(
                        shape=MaterialTheme.shapes.small,
                        color=FayrozDeepNavy,
                        border=BorderStroke(1.dp,FayrozGold.copy(alpha=.45f))
                    ){
                        Box(
                            Modifier.size(34.dp).padding(4.dp),
                            contentAlignment=Alignment.Center
                        ){
                            FayrozMark(Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)){
                        Text(title,style=MaterialTheme.typography.titleMedium)
                        Text(
                            subtitle,
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ){padding->
        LazyColumn(
            modifier=Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(horizontal=11.dp,vertical=8.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp)
        ){
            item{Column(verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}
        }
    }
}

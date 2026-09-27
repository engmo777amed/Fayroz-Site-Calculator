@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.fayroz.sitecalculator.data.*

private val projectTypes=listOf("شقة","بيت","عمارة","محل","مشروع مخصص")

@Composable
fun HomeScreen(
    projects:List<Project>,
    appearance:Appearance,
    onAppearance:(Appearance)->Unit,
    onNew:()->Unit,
    onProject:(String)->Unit,
    onQuickRoom:()->Unit,
    onQuickItem:()->Unit
){
    var search by remember{mutableStateOf("")}
    val roomCount=projects.sumOf{p->p.sections.sumOf{s->s.rooms.sumOf{it.repeat}}}
    val filtered=projects.filter{p->
        search.isBlank() ||
            p.name.contains(search,true) ||
            p.type.contains(search,true) ||
            p.sections.any{s->
                s.name.contains(search,true) ||
                    s.rooms.any{r->r.name.contains(search,true)||r.note.contains(search,true)}
            }
    }

    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{HeroHeader(projects.size,roomCount)}

        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                QuickCard(
                    title="حساب فراغ",
                    subtitle="غرفة كاملة",
                    icon=Icons.Rounded.MeetingRoom,
                    accent=true,
                    onClick=onQuickRoom,
                    modifier=Modifier.weight(1f)
                )
                QuickCard(
                    title="حساب بند",
                    subtitle="غرفة أو حوائط",
                    icon=Icons.Rounded.Calculate,
                    onClick=onQuickItem,
                    modifier=Modifier.weight(1f)
                )
            }
        }

        item{AppearanceBar(appearance,onAppearance)}

        item{
            SectionTitle(
                title="مشروعات الموقع",
                subtitle="المشروع ← الدور أو الجزء ← الغرف",
                trailing={
                    FilledTonalButton(
                        onClick=onNew,
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
            item{Txt("بحث",search,{search=it},"اسم مشروع أو غرفة")}
        }

        if(filtered.isEmpty()){
            item{
                CardBox{
                    Text(
                        if(projects.isEmpty())"لا توجد مشروعات محفوظة. ابدأ مشروعًا جديدًا."
                        else "لا توجد نتيجة مطابقة.",
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }else{
            item{
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    filtered.forEach{p->
                        val count=p.sections.sumOf{s->s.rooms.sumOf{it.repeat}}
                        Surface(
                            onClick={onProject(p.id)},
                            shape=RoundedCornerShape(16.dp),
                            color=MaterialTheme.colorScheme.surface,
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Surface(
                                    shape=RoundedCornerShape(11.dp),
                                    color=MaterialTheme.colorScheme.secondaryContainer
                                ){
                                    Icon(
                                        Icons.Rounded.Apartment,
                                        null,
                                        Modifier.padding(7.dp).size(19.dp),
                                        tint=MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(
                                        p.name,
                                        style=MaterialTheme.typography.titleMedium,
                                        fontWeight=FontWeight.Black,
                                        maxLines=1,
                                        overflow=TextOverflow.Ellipsis
                                    )
                                    Text(
                                        p.type+" • "+p.sections.size+" جزء • "+count+" غرفة/فراغ",
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines=1
                                    )
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
fun NewProjectScreen(
    onBack:()->Unit,
    onCreate:(String,String)->Unit
){
    var name by remember{mutableStateOf("")}
    var type by remember{mutableStateOf("شقة")}

    Scaffold(
        topBar={AppBar("مشروع جديد","ابدأ باسم واضح للمشروع",onBack)}
    ){pad->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    SectionTitle("بيانات المشروع","مثال: شقة أحمد - سموحة")
                    Txt("اسم المشروع",name,{name=it},"اكتب اسم المشروع")
                    Choice("نوع المشروع",type,projectTypes,{type=it})
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

private fun projectSummaryText(p:Project)=buildString{
    appendLine("FAYROZ SITE CALCULATOR")
    appendLine("المشروع: "+p.name)
    appendLine("----------------")
    QtyEngine.projectSummary(p).forEach{(w,q)->
        appendLine(w+": "+fmt(q)+" "+QtyEngine.unit(w))
    }
}

private fun shareText(ctx:android.content.Context,title:String,text:String){
    ctx.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply{
                type="text/plain"
                putExtra(Intent.EXTRA_SUBJECT,title)
                putExtra(Intent.EXTRA_TEXT,text)
            },
            "مشاركة الحصر"
        )
    )
}

@Composable
fun ProjectScreen(
    project:Project,
    onBack:()->Unit,
    onOpenSection:(String)->Unit,
    onAddSection:(String)->Unit,
    onRename:(String)->Unit,
    onDelete:()->Unit,
    onRenameSection:(String,String)->Unit,
    onDeleteSection:(String)->Unit
){
    val ctx=LocalContext.current
    var tab by remember{mutableIntStateOf(0)}
    var menu by remember{mutableStateOf(false)}
    var add by remember{mutableStateOf(false)}
    var addName by remember{mutableStateOf("")}
    var rename by remember{mutableStateOf(false)}
    var renameText by remember(project.id){mutableStateOf(project.name)}
    var confirmDelete by remember{mutableStateOf(false)}
    var editSection by remember{mutableStateOf<Section?>(null)}
    var deleteSection by remember{mutableStateOf<Section?>(null)}

    Scaffold(
        topBar={
            AppBar(
                title=project.name,
                subtitle=project.type+" • "+project.sections.sumOf{s->s.rooms.sumOf{it.repeat}}+" غرفة/فراغ",
                onBack=onBack,
                actions={
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
                                onClick={menu=false;shareText(ctx,project.name,projectSummaryText(project))}
                            )
                            DropdownMenuItem(
                                text={Text("حذف المشروع",color=MaterialTheme.colorScheme.error)},
                                leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                onClick={menu=false;confirmDelete=true}
                            )
                        }
                    }
                }
            )
        }
    ){pad->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{Tabs(listOf("التقسيم","ملخص البنود"),tab){tab=it}}

            if(tab==0){
                item{
                    SectionTitle(
                        "الأدوار والأجزاء",
                        "افتح الجزء ثم سجّل الغرف",
                        trailing={
                            IconButton(onClick={add=true}){Icon(Icons.Rounded.AddCircle,"إضافة")}
                        }
                    )
                }

                if(project.sections.isEmpty()){
                    item{CardBox{Text("لا يوجد دور أو جزء بعد.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
                }else{
                    item{
                        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                            project.sections.forEach{s->
                                var smenu by remember(s.id){mutableStateOf(false)}
                                Surface(
                                    onClick={onOpenSection(s.id)},
                                    shape=RoundedCornerShape(15.dp),
                                    color=MaterialTheme.colorScheme.surface,
                                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                ){
                                    Row(
                                        Modifier.fillMaxWidth().padding(9.dp),
                                        verticalAlignment=Alignment.CenterVertically
                                    ){
                                        Surface(
                                            shape=RoundedCornerShape(10.dp),
                                            color=MaterialTheme.colorScheme.primaryContainer
                                        ){
                                            Icon(
                                                Icons.Rounded.Layers,
                                                null,
                                                Modifier.padding(7.dp).size(18.dp),
                                                tint=MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)){
                                            Text(s.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                            Text(
                                                s.rooms.sumOf{it.repeat}.toString()+" غرفة/فراغ",
                                                style=MaterialTheme.typography.bodySmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box{
                                            IconButton(onClick={smenu=true},modifier=Modifier.size(34.dp)){
                                                Icon(Icons.Rounded.MoreVert,"إجراءات")
                                            }
                                            DropdownMenu(expanded=smenu,onDismissRequest={smenu=false}){
                                                DropdownMenuItem(
                                                    text={Text("تعديل الاسم")},
                                                    leadingIcon={Icon(Icons.Rounded.Edit,null)},
                                                    onClick={smenu=false;editSection=s}
                                                )
                                                DropdownMenuItem(
                                                    text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                                    leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                                    onClick={smenu=false;deleteSection=s}
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
            }else{
                item{SummaryList(project=project)}
            }
        }
    }

    if(add){
        AlertDialog(
            onDismissRequest={add=false},
            title={Text("إضافة دور أو جزء")},
            text={OutlinedTextField(addName,{addName=it},singleLine=true,placeholder={Text("مثال: الدور الأول")})},
            confirmButton={
                TextButton(onClick={
                    if(addName.isNotBlank()){
                        onAddSection(addName.trim())
                        addName=""
                        add=false
                    }
                }){Text("إضافة")}
            },
            dismissButton={TextButton(onClick={add=false}){Text("إلغاء")}}
        )
    }

    if(rename){
        AlertDialog(
            onDismissRequest={rename=false},
            title={Text("تعديل اسم المشروع")},
            text={OutlinedTextField(renameText,{renameText=it},singleLine=true)},
            confirmButton={
                TextButton(onClick={
                    if(renameText.isNotBlank()){
                        onRename(renameText.trim())
                        rename=false
                    }
                }){Text("حفظ")}
            },
            dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
        )
    }

    if(confirmDelete){
        AlertDialog(
            onDismissRequest={confirmDelete=false},
            title={Text("حذف المشروع؟")},
            text={Text("سيتم حذف المشروع وكل الأدوار والغرف الموجودة داخله.")},
            confirmButton={TextButton(onClick={confirmDelete=false;onDelete()}){Text("حذف")}},
            dismissButton={TextButton(onClick={confirmDelete=false}){Text("إلغاء")}}
        )
    }

    editSection?.let{s->
        var n by remember(s.id){mutableStateOf(s.name)}
        AlertDialog(
            onDismissRequest={editSection=null},
            title={Text("تعديل الاسم")},
            text={OutlinedTextField(n,{n=it},singleLine=true)},
            confirmButton={
                TextButton(onClick={
                    if(n.isNotBlank()){
                        onRenameSection(s.id,n.trim())
                        editSection=null
                    }
                }){Text("حفظ")}
            },
            dismissButton={TextButton(onClick={editSection=null}){Text("إلغاء")}}
        )
    }

    deleteSection?.let{s->
        AlertDialog(
            onDismissRequest={deleteSection=null},
            title={Text("حذف "+s.name+"؟")},
            text={Text("سيتم حذف الغرف الموجودة داخله.")},
            confirmButton={
                TextButton(onClick={
                    onDeleteSection(s.id)
                    deleteSection=null
                }){Text("حذف")}
            },
            dismissButton={TextButton(onClick={deleteSection=null}){Text("إلغاء")}}
        )
    }
}

@Composable
private fun SummaryList(project:Project?=null,section:Section?=null){
    val summary=if(project!=null)QtyEngine.projectSummary(project) else QtyEngine.sectionSummary(section!!)
    if(summary.isEmpty()){
        CardBox{Text("لا توجد كميات بعد.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        return
    }

    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        summary.forEach{(work,total)->
            var open by remember(work){mutableStateOf(false)}
            Surface(
                onClick={open=!open},
                shape=RoundedCornerShape(14.dp),
                color=MaterialTheme.colorScheme.surface,
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(
                    Modifier.fillMaxWidth().padding(9.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)
                ){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(work,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                        Text(
                            fmt(total)+" "+QtyEngine.unit(work),
                            color=MaterialTheme.colorScheme.primary,
                            fontWeight=FontWeight.Black
                        )
                        Icon(if(open)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null)
                    }

                    if(open){
                        HorizontalDivider()
                        val sections=project?.sections?:listOf(section!!)
                        sections.forEach{s->
                            s.rooms.filter{work in it.works}.forEach{r->
                                Row{
                                    Text(
                                        r.name+(if(r.repeat>1)" ×"+r.repeat else ""),
                                        Modifier.weight(1f),
                                        style=MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        fmt(QtyEngine.quantity(r,work))+" "+QtyEngine.unit(work),
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

@Composable
fun SectionScreen(
    projectName:String,
    section:Section,
    onBack:()->Unit,
    onAdd:()->Unit,
    onEdit:(String)->Unit,
    onCopy:(String)->Unit,
    onDelete:(String)->Unit,
    onRename:(String)->Unit
){
    var tab by remember{mutableIntStateOf(0)}
    var rename by remember{mutableStateOf(false)}
    var name by remember(section.id){mutableStateOf(section.name)}
    var del by remember{mutableStateOf<Room?>(null)}

    Scaffold(
        topBar={
            AppBar(
                title=section.name,
                subtitle=projectName+" • "+section.rooms.sumOf{it.repeat}+" غرفة/فراغ",
                onBack=onBack,
                actions={
                    IconButton(onClick={rename=true}){Icon(Icons.Rounded.Edit,"تعديل الاسم")}
                }
            )
        },
        floatingActionButton={
            if(tab==0){
                ExtendedFloatingActionButton(
                    onClick=onAdd,
                    icon={Icon(Icons.Rounded.Add,null)},
                    text={Text("غرفة")}
                )
            }
        }
    ){pad->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=12.dp,bottom=82.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{Tabs(listOf("الغرف","ملخص البنود"),tab){tab=it}}

            if(tab==0){
                item{SectionTitle("الغرف والفراغات","تعديل، نسخ أو حذف من ⋮")}

                if(section.rooms.isEmpty()){
                    item{CardBox{Text("لا توجد غرف هنا بعد.",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
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
                                    Row(
                                        Modifier.fillMaxWidth().padding(9.dp),
                                        verticalAlignment=Alignment.CenterVertically
                                    ){
                                        Surface(
                                            shape=RoundedCornerShape(10.dp),
                                            color=MaterialTheme.colorScheme.secondaryContainer
                                        ){
                                            Icon(
                                                Icons.Rounded.MeetingRoom,
                                                null,
                                                Modifier.padding(7.dp).size(18.dp),
                                                tint=MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)){
                                            Text(r.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                            Text(
                                                r.type+" • "+fmt(r.length)+" × "+fmt(r.width)+" م"+
                                                    (if(r.repeat>1)" • ×"+r.repeat else ""),
                                                style=MaterialTheme.typography.bodySmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines=1
                                            )
                                            if(r.note.isNotBlank()){
                                                Text(
                                                    r.note,
                                                    style=MaterialTheme.typography.labelSmall,
                                                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines=1,
                                                    overflow=TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            shape=RoundedCornerShape(999.dp),
                                            color=MaterialTheme.colorScheme.primaryContainer
                                        ){
                                            Text(
                                                r.works.size.toString()+" بنود",
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
                                                    onClick={menu=false;onEdit(r.id)}
                                                )
                                                DropdownMenuItem(
                                                    text={Text("نسخ")},
                                                    leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},
                                                    onClick={menu=false;onCopy(r.id)}
                                                )
                                                DropdownMenuItem(
                                                    text={Text("حذف",color=MaterialTheme.colorScheme.error)},
                                                    leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},
                                                    onClick={menu=false;del=r}
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
                item{SummaryList(section=section)}
            }
        }
    }

    if(rename){
        AlertDialog(
            onDismissRequest={rename=false},
            title={Text("تعديل الاسم")},
            text={OutlinedTextField(name,{name=it},singleLine=true)},
            confirmButton={
                TextButton(onClick={
                    if(name.isNotBlank()){
                        onRename(name.trim())
                        rename=false
                    }
                }){Text("حفظ")}
            },
            dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
        )
    }

    del?.let{r->
        AlertDialog(
            onDismissRequest={del=null},
            title={Text("حذف "+r.name+"؟")},
            text={Text("سيتم حذف المقاسات والكميات المحفوظة لهذه الغرفة.")},
            confirmButton={
                TextButton(onClick={
                    onDelete(r.id)
                    del=null
                }){Text("حذف")}
            },
            dismissButton={TextButton(onClick={del=null}){Text("إلغاء")}}
        )
    }
}

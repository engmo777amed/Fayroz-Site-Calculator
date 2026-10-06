@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.*
import com.fayroz.sitecalculator.reports.ReportExport
import java.util.UUID

@Composable
fun ProjectDetailScreen(project:Project,active:Boolean,onBack:()->Unit,onSetActive:()->Unit,onOpenSection:(String)->Unit,
    onAddSection:(String)->Unit,onOpenMaterials:(MaterialResult)->Unit,onShare:()->Unit,
    onUpdate:(Project)->Unit,onEditSource:(String,String,String)->Unit,onOpenCalc:(SavedCalculation)->Unit,onDirectItem:()->Unit = {},onAddPlace:(String)->Unit = {}){
    val context=LocalContext.current
    var tab by remember{mutableStateOf("نظرة عامة")}
    var sectionId by remember{mutableStateOf<String?>(null)}
    var spaceId by remember{mutableStateOf<String?>(null)}
    var itemFilter by remember{mutableStateOf("كل البنود")}
    var add by remember{mutableStateOf(false)}
    var addPlace by remember{mutableStateOf(false)}
    var sectionName by remember{mutableStateOf("")}
    var deleteSection by remember{mutableStateOf<Section?>(null)}
    var editSection by remember{mutableStateOf<Section?>(null)}
    var renameProject by remember{mutableStateOf(false)}
    var projectName by remember{mutableStateOf(project.name)}
    var exportOpen by remember{mutableStateOf(false)}
    var reportKind by remember{mutableStateOf("حصر وتكلفة تفصيلي")}
    var defaultsTool by remember{mutableStateOf("tile")}
    var defaults by remember(project.defaults){mutableStateOf(project.defaults)}
    val scoped=project.copy(
        sections=project.sections.filter{sectionId==null||it.id==sectionId}.map{section->
            section.copy(spaces=section.spaces.filter{spaceId==null||it.id==spaceId}.map{space->
                space.copy(takeoffs=space.takeoffs.filter{itemFilter=="كل البنود"||it.name==itemFilter})
            })
        },
        calculations=project.calculations.filter{(sectionId==null||it.sectionId==sectionId)&&(spaceId==null||it.spaceId==spaceId)&&(itemFilter=="كل البنود"||it.title==itemFilter)}
    )
    val rows=CostEngine.rows(scoped)
    val summary=QuantityEngine.summarize(scoped)
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->
        if(uri!=null)runCatching{context.contentResolver.openOutputStream(uri)?.use{ReportExport.xlsx(scoped,reportKind,it)}?:error("تعذر فتح الملف")}.onSuccess{Toast.makeText(context,"تم حفظ Excel",Toast.LENGTH_SHORT).show()}.onFailure{Toast.makeText(context,"فشل الحفظ: ${it.message}",Toast.LENGTH_LONG).show()}
    }
    Scaffold(topBar={TopAppBar(title={Text(project.name,fontWeight=FontWeight.Bold)},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}},actions={ActionMenu(listOf("تعديل اسم المشروع" to {renameProject=true},"إعدادات المشروع" to {tab="إعدادات المشروع والتصدير"},"تصدير" to {exportOpen=true}))})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{Row{Text(project.type,Modifier.weight(1f));TextButton(onClick=onSetActive){Text(if(active)"المشروع النشط ✓" else "تعيين نشط")}}}
            if(tab=="نظرة عامة"){
                item{PageHeader("اختار المطلوب","${project.sections.size} دور / جزء • ${project.sections.sumOf{it.spaces.size}} مكان")}
                val links=listOf("الأدوار والأماكن" to "الأدوار","حصر الأعمال" to "الحصر والتكلفة","طلب الخامات" to "شراء الخامات","التكلفة" to "التكلفة")
                items(2){i->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    links.drop(i*2).take(2).forEach{(label,target)->ElevatedCard(onClick={tab=target},modifier=Modifier.weight(1f)){
                        Column(Modifier.padding(16.dp).heightIn(min=64.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text(label,fontWeight=FontWeight.Bold);Text("فتح ←",color=MaterialTheme.colorScheme.primary)
                        }
                    }}
                }}
                item{Button(onClick=onDirectItem,modifier=Modifier.fillMaxWidth()){Text("إضافة كمية جاهزة")}}
                item{OutlinedButton(onClick={if(project.sections.size==1)onAddPlace(project.sections.first().id)else if(project.sections.isEmpty()){tab="الأدوار";add=true}else addPlace=true},modifier=Modifier.fillMaxWidth()){Text("إضافة مكان بالمقاسات")}}
                item{TextButton(onClick={tab="حسابات محفوظة"}){Text("حسابات المشروع المحفوظة")}}
            }else{
                item{TextButton(onClick={tab="نظرة عامة"}){Text("رجوع لملخص المشروع")}}
                item{PageHeader(when(tab){"الحصر والتكلفة"->"حصر الأعمال";"شراء الخامات"->"طلب الخامات";"الأدوار"->"الأدوار والأماكن";else->tab})}
            }
            if(tab in listOf("الحصر والتكلفة","شراء الخامات","التكلفة","حسابات محفوظة"))item{BoxCard{ExpandableSection("تحديد نطاق العرض"){
                val sectionLabels=selectionLabels(project.sections.map{it.name})
                ChoiceFieldX("الدور / الجزء",project.sections.indexOfFirst{it.id==sectionId}.takeIf{it>=0}?.let{sectionLabels[it]}?:"المشروع كله",listOf("المشروع كله")+sectionLabels,{name->sectionId=project.sections.getOrNull(sectionLabels.indexOf(name))?.id;spaceId=null})
                val spaces=project.sections.filter{sectionId==null||it.id==sectionId}.flatMap{it.spaces}
                val spaceLabels=selectionLabels(spaces.map{sp->"${project.sections.first{sec->sec.spaces.any{it.id==sp.id}}.name} / ${sp.name}"})
                ChoiceFieldX("المكان",spaces.indexOfFirst{it.id==spaceId}.takeIf{it>=0}?.let{spaceLabels[it]}?:"كل الأماكن",listOf("كل الأماكن")+spaceLabels,{name->spaceId=spaces.getOrNull(spaceLabels.indexOf(name))?.id})
                val names=project.sections.flatMap{it.spaces}.flatMap{it.takeoffs}.map{it.name}.plus(project.calculations.filter{it.inputs["_includeInProject"]=="true"}.map{it.title}).distinct()
                ChoiceFieldX("البند",itemFilter,listOf("كل البنود")+names,{itemFilter=it})
            }}}
            if(tab in listOf("الحصر والتكلفة","شراء الخامات","التكلفة"))item{Text("النطاق: "+(project.sections.firstOrNull{it.id==sectionId}?.name?:"المشروع كله")+(spaceId?.let{id->" / "+project.sections.flatMap{it.spaces}.firstOrNull{it.id==id}?.name}.orEmpty()),style=MaterialTheme.typography.bodySmall)}
            when(tab){
                "نظرة عامة"->{}
                "التكلفة"->{item{CostSummary(rows.sumOf{it.materialCost},rows.sumOf{it.labor},rows.sumOf{it.transport},rows.sumOf{it.equipment},rows.flatMap{MaterialReview.missing(it)}.distinct())}
                    item{val selling=CostEngine.selling(project,rows);BoxCard{
                        Text("سعر البيع للنطاق المختار",fontWeight=FontWeight.Bold)
                        MetricRow("مصاريف عامة (${project.defaults["overheadPercent"]?:"0"}%)","${money(selling.overhead)} جنيه")
                        MetricRow("ربح (${project.defaults["profitPercent"]?:"0"}%)","${money(selling.profit)} جنيه")
                        MetricRow("ضريبة (${project.defaults["taxPercent"]?:"0"}%)","${money(selling.tax)} جنيه")
                        MetricRow(if(rows.flatMap{MaterialReview.missing(it)}.isEmpty())"إجمالي سعر البيع المدخل"else "سعر بيع جزئي — أسعار ناقصة","${money(selling.total)} جنيه",true)
                        Text("المصاريف على التكلفة المباشرة، والربح بعد المصاريف، والضريبة بعد الربح. أي مصنعية أو نقل أو معدات غير مدخلة غير مشمولة.",style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={tab="إعدادات المشروع والتصدير"}){Text("تعديل نسب المصاريف والربح والضريبة")}
                    }}
                }
                "الأدوار"->{
                    item{Button(onClick={sectionName="";add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة دور / جزء")}}
                    items(project.sections.size){i->val s=project.sections[i];BoxCard{
                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Column(Modifier.weight(1f)){
                        TextButton(onClick={onOpenSection(s.id)}){Text(s.name,fontWeight=FontWeight.Bold)}
                        Text("${s.spaces.size} مكان")
                        }
                        ActionMenu(buildList{
                            add("تعديل الاسم" to {sectionName=s.name;editSection=s})
                            add("نسخ الدور" to {
                                val copy=s.copy(id=UUID.randomUUID().toString(),name=s.name+" — نسخة",spaces=s.spaces.map{copySpace(it)})
                                onUpdate(project.copy(sections=project.sections+copy,updatedAt=System.currentTimeMillis()))
                            })
                            add("حذف الدور" to {deleteSection=s})
                            if(i>0)add("تحريك لأعلى" to {val list=project.sections.toMutableList();list[i]=list[i-1].also{list[i-1]=list[i]};onUpdate(project.copy(sections=list))})
                        })
                        }
                    }}
                }
                "الحصر والتكلفة"->{
                    item{BoxCard{Text("ملخص النطاق المختار",fontWeight=FontWeight.Bold);summary.forEach{MetricRow(it.name,"${fmt(it.quantity)} ${it.unit.label}")}}}
                    val groups=rows.groupBy{it.spaceId to it.itemId}.values.toList()
                    items(groups.size){i->val group=groups[i];BoxCard{ExpandableSection("${group.first().item} — ${group.first().location}"){
                        Text(group.first().item,fontWeight=FontWeight.Bold)
                        Text(group.first().location,style=MaterialTheme.typography.bodySmall)
                        group.forEach{MetricRow(it.part,"${fmt(it.quantity)} ${it.unit}")}
                        var show by remember(group.first().itemId){mutableStateOf(false)}
                        TextButton(onClick={show=!show}){Text(if(show)"إخفاء التفاصيل" else "عرض المواد والتكلفة")}
                        if(show)WorkResultCards(group,"${group.first().location} / ${group.first().item}")
                        TextButton(onClick={onEditSource(group.first().sectionId,group.first().spaceId,group.first().itemId)}){Text("تعديل البند")}
                    }}}
                    if(rows.isEmpty())item{EmptyState("لا يوجد حصر في النطاق المختار","اختار نطاقًا آخر أو أضف بنودًا للمكان.")}
                }
                "شراء الخامات"->{
                    item{PurchaseCards(CostEngine.purchase(rows),rows.flatMap{MaterialReview.missing(it)}.isEmpty())}
                    val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
                    if(missing.isNotEmpty())item{Text("التكلفة غير مكتملة: "+missing.joinToString("، "),color=MaterialTheme.colorScheme.error)}
                }
                "حسابات محفوظة"->{items(scoped.calculations.size){i->val c=scoped.calculations[i];BoxCard{Text(c.title,fontWeight=FontWeight.Bold);Text(c.summary.substringBefore(" • "));Text(if(c.inputs["_includeInProject"]=="true")"مضاف لإجمالي المشروع"else "مرجع فقط — غير داخل الإجمالي",style=MaterialTheme.typography.bodySmall);Text(dated(c.createdAt),style=MaterialTheme.typography.bodySmall);MetricRow("شراء الخامات عند الحفظ","${money(c.cost)} جنيه");TextButton(onClick={onOpenCalc(c)}){Text("فتح بنفس المدخلات")}}}}
                else->{
                    item{BoxCard{
                        Text("المصاريف العامة والربح",fontWeight=FontWeight.Bold)
                        listOf("overheadPercent" to "مصاريف عامة","profitPercent" to "ربح","taxPercent" to "ضريبة إن انطبقت").forEach{(key,label)->NumberFieldX(label,defaults[key]?:"0",{defaults=defaults+(key to it)},"%")}
                        Text("النسب للمشروع كله؛ القيمة صفر تعني عدم إضافة هذه النسبة.",style=MaterialTheme.typography.bodySmall)
                        Button(onClick={if(listOf("overheadPercent","profitPercent","taxPercent").all{key->defaults[key].isNullOrBlank()||defaults[key]!!.map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()?.let{it.isFinite()&&it>=0}==true})onUpdate(project.copy(defaults=defaults))else Toast.makeText(context,"أدخل نسبًا صحيحة صفر أو أكبر",Toast.LENGTH_SHORT).show()}){Text("حفظ نسب التسعير")}
                    }}
                    item{BoxCard{
                        Text("إعدادات المونة الافتراضية",fontWeight=FontWeight.Bold)
                        ExpandableSection("خلطات المونة والسمك الافتراضي"){
                        Text("تُستخدم للأجزاء بدون إعدادات خاصة. الحسابات المحفوظة بالخامات الخاصة تحتفظ بقيمها.",style=MaterialTheme.typography.bodySmall)
                        listOf(Triple("plaster","المحارة","15"),Triple("screed","تسوية الأرضيات","50"),Triple("splash","الطرطشة","5")).forEach{(kind,label,thick)->
                            NumberFieldX("سمك $label",defaults["${kind}Thickness"]?:thick,{defaults=defaults+("${kind}Thickness" to it)},"مم")
                            val legacy=n(defaults["${kind}Sand"]?:if(kind=="splash")"2"else "4")
                            val bags=defaults["${kind}BagsPerSand"]?:exact(1440.0/50/legacy)
                            NumberFieldX("شكاير $label على متر الرمل",bags,{defaults=defaults+("${kind}BagsPerSand" to it)},"شيكارة × 50 كجم/م³ رمل")
                        }
                        NumberFieldX("هالك المونة",defaults["mortarWaste"]?:"5",{defaults=defaults+("mortarWaste" to it)},"%")
                        }
                        HorizontalDivider()
                        val recipes=CalculatorLibrary.all.filter{it.id in setOf("tile","skirting","paint","waterproof","gypsum","masonry")}
                        ChoiceFieldX("إعدادات باقي خامات المشروع",recipes.first{it.id==defaultsTool}.title,recipes.map{it.title},{title->defaultsTool=recipes.first{it.title==title}.id})
                        val recipe=recipes.first{it.id==defaultsTool}
                        ExpandableSection("تعديل مواصفات الخامة المختارة"){
                        if(com.fayroz.sitecalculator.domain.MortarMix.isMix(recipe)){
                            val values=CalculatorLibrary.defaults(recipe,defaults)
                            NumberFieldX("شكاير الأسمنت على متر الرمل",defaults["recipe.${recipe.id}._bagsPerSand"]?:exact(com.fayroz.sitecalculator.domain.MortarMix.bags(values)),{defaults=defaults+("recipe.${recipe.id}._bagsPerSand" to it)},"شيكارة × 50 كجم/م³ رمل")
                        }
                        recipe.fields.filter{it.key !in setOf("area","length","count","cement","sand")&&!it.key.contains("price",true)}.forEach{field->
                            val key="recipe.${recipe.id}.${field.key}"
                            NumberFieldX(field.label,defaults[key]?:field.default,{defaults=defaults+(key to it)},field.unit)
                        }
                        }
                        Button(onClick={
                            val valid=defaults.filterKeys{it !in setOf("cementName","sandName")}.all{(_,v)->v.isBlank()||v.map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()?.let{it>=0&&it.isFinite()}==true}&&listOf("plasterThickness","splashThickness","screedThickness","plasterSand","splashSand","screedSand","plasterBagsPerSand","screedBagsPerSand","splashBagsPerSand").all{defaults[it]==null||n(defaults[it].orEmpty())>0}
                            if(valid){onUpdate(project.copy(defaults=defaults,updatedAt=System.currentTimeMillis()));Toast.makeText(context,"تم حفظ الإعدادات",Toast.LENGTH_SHORT).show()}
                            else Toast.makeText(context,"راجع الأسعار والأسماك والخلطات",Toast.LENGTH_LONG).show()
                        }){Text("اعتماد إعدادات المشروع")}
                    }}
                }
            }
            if(tab!="نظرة عامة")item{OutlinedButton(onClick={exportOpen=true},modifier=Modifier.fillMaxWidth()){Text("تصدير النطاق المعروض")}}

        }
    }
    if(addPlace)AlertDialog(onDismissRequest={addPlace=false},title={Text("إضافة المكان في أي دور؟")},text={Column{project.sections.forEach{s->TextButton(onClick={addPlace=false;onAddPlace(s.id)}){Text(s.name)}}}},confirmButton={TextButton(onClick={addPlace=false}){Text("إلغاء")}})
    if(exportOpen)AlertDialog(onDismissRequest={exportOpen=false},title={Text("تصدير الكشف")},text={Column{
        ChoiceFieldX("نوع الكشف",reportKind,listOf("حصر وتكلفة تفصيلي","ملخص الكميات","شراء الخامات","تحليل أسعار البنود"),{reportKind=it})
        Text("سيُصدّر النطاق المحدد في العرض.",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={export.launch("Fayroz-${project.name}.xlsx");exportOpen=false}){Text("Excel")}},dismissButton={Row{
        TextButton(onClick={ReportExport.printPdf(context,scoped,reportKind);exportOpen=false}){Text("PDF")}
        TextButton(onClick={onShare();exportOpen=false}){Text("مشاركة")}
    }})
    deleteSection?.let{section->AlertDialog(onDismissRequest={deleteSection=null},title={Text("حذف ${section.name}؟")},text={Text("سيُحذف الدور وأماكنه وحصره. الحسابات المحفوظة التابعة له تُحذف أيضًا.")},
        confirmButton={TextButton(onClick={onUpdate(project.copy(sections=project.sections.filterNot{it.id==section.id},calculations=project.calculations.filterNot{it.sectionId==section.id},updatedAt=System.currentTimeMillis()));deleteSection=null}){Text("حذف")}},dismissButton={TextButton(onClick={deleteSection=null}){Text("إلغاء")}})}
    if(add||editSection!=null)AlertDialog(onDismissRequest={add=false;editSection=null},title={Text("اسم الدور / الجزء")},text={TextFieldX("الاسم",sectionName,{sectionName=it})},confirmButton={TextButton(onClick={
        if(sectionName.isNotBlank()){
            val old=editSection
            if(old==null)onAddSection(sectionName.trim())else onUpdate(project.copy(sections=project.sections.map{if(it.id==old.id)it.copy(name=sectionName.trim())else it}))
            add=false;editSection=null
        }
    }){Text("حفظ")}},dismissButton={TextButton(onClick={add=false;editSection=null}){Text("إلغاء")}})
    if(renameProject)AlertDialog(onDismissRequest={renameProject=false},title={Text("اسم المشروع")},text={TextFieldX("الاسم",projectName,{projectName=it})},confirmButton={TextButton(onClick={if(projectName.isNotBlank()){onUpdate(project.copy(name=projectName.trim()));renameProject=false}}){Text("حفظ")}})
}
fun copySpace(s:Space)=s.copy(id=UUID.randomUUID().toString(),name=s.name+" — نسخة",takeoffs=s.takeoffs.map{it.copy(id=UUID.randomUUID().toString(),parts=it.parts.map{p->p.copy(id=UUID.randomUUID().toString())})},updatedAt=System.currentTimeMillis())


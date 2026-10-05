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
    onUpdate:(Project)->Unit,onEditSource:(String,String,String)->Unit,onOpenCalc:(SavedCalculation)->Unit){
    val context=LocalContext.current
    var tab by remember{mutableStateOf("الأدوار")}
    var sectionId by remember{mutableStateOf<String?>(null)}
    var spaceId by remember{mutableStateOf<String?>(null)}
    var itemFilter by remember{mutableStateOf("كل البنود")}
    var add by remember{mutableStateOf(false)}
    var sectionName by remember{mutableStateOf("")}
    var deleteSection by remember{mutableStateOf<Section?>(null)}
    var editSection by remember{mutableStateOf<Section?>(null)}
    var renameProject by remember{mutableStateOf(false)}
    var projectName by remember{mutableStateOf(project.name)}
    var reportKind by remember{mutableStateOf("حصر وتكلفة تفصيلي")}
    var defaultsTool by remember{mutableStateOf("tile")}
    var defaults by remember(project.defaults){mutableStateOf(project.defaults)}
    val scoped=project.copy(
        sections=project.sections.filter{sectionId==null||it.id==sectionId}.map{section->
            section.copy(spaces=section.spaces.filter{spaceId==null||it.id==spaceId}.map{space->
                space.copy(takeoffs=space.takeoffs.filter{itemFilter=="كل البنود"||it.name==itemFilter})
            })
        },
        calculations=project.calculations.filter{(sectionId==null||it.sectionId==sectionId)&&(spaceId==null||it.spaceId==spaceId)}
    )
    val rows=CostEngine.rows(scoped)
    val summary=QuantityEngine.summarize(scoped)
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){uri->
        if(uri!=null)runCatching{context.contentResolver.openOutputStream(uri)?.use{ReportExport.xlsx(scoped,reportKind,it)}?:error("تعذر فتح الملف")}.onSuccess{Toast.makeText(context,"تم حفظ Excel",Toast.LENGTH_SHORT).show()}.onFailure{Toast.makeText(context,"فشل الحفظ: ${it.message}",Toast.LENGTH_LONG).show()}
    }
    Scaffold(topBar={TopAppBar(title={Text(project.name,fontWeight=FontWeight.Bold)},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}},actions={TextButton(onClick={renameProject=true}){Text("الاسم")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{Row{Text(project.type,Modifier.weight(1f));TextButton(onClick=onSetActive){Text(if(active)"المشروع النشط ✓" else "تعيين نشط")}}}
            item{
                val tabs=listOf("الأدوار","الحصر والتكلفة","شراء الخامات","حسابات محفوظة","إعدادات المشروع والتصدير")
                val labels=listOf("الأدوار","النتائج","الشراء","المحفوظات","الإعدادات")
                ScrollableTabRow(selectedTabIndex=tabs.indexOf(tab),edgePadding=0.dp){tabs.forEachIndexed{i,t->Tab(selected=tab==t,onClick={tab=t},text={Text(labels[i])})}}
            }
            if(tab!="الأدوار"&&tab!="إعدادات المشروع والتصدير")item{BoxCard{
                ChoiceFieldX("الدور / الجزء",project.sections.firstOrNull{it.id==sectionId}?.name?:"المشروع كله",listOf("المشروع كله")+project.sections.map{it.name},{name->sectionId=project.sections.firstOrNull{it.name==name}?.id;spaceId=null})
                val spaces=project.sections.filter{sectionId==null||it.id==sectionId}.flatMap{it.spaces}
                ChoiceFieldX("المكان",spaces.firstOrNull{it.id==spaceId}?.name?:"كل الأماكن",listOf("كل الأماكن")+spaces.map{it.name},{name->spaceId=spaces.firstOrNull{it.name==name}?.id})
                val names=project.sections.flatMap{it.spaces}.flatMap{it.takeoffs}.map{it.name}.distinct()
                ChoiceFieldX("البند",itemFilter,listOf("كل البنود")+names,{itemFilter=it})
            }}
            when(tab){
                "الأدوار"->{
                    item{Button(onClick={sectionName="";add=true},modifier=Modifier.fillMaxWidth()){Text("إضافة دور / جزء")}}
                    items(project.sections.size){i->val s=project.sections[i];BoxCard{
                        TextButton(onClick={onOpenSection(s.id)}){Text(s.name,fontWeight=FontWeight.Bold)}
                        Text("${s.spaces.size} مكان")
                        Row{
                            TextButton(onClick={sectionName=s.name;editSection=s}){Text("تعديل")}
                            TextButton(onClick={
                                val copy=s.copy(id=UUID.randomUUID().toString(),name=s.name+" — نسخة",spaces=s.spaces.map{copySpace(it)})
                                onUpdate(project.copy(sections=project.sections+copy,updatedAt=System.currentTimeMillis()))
                            }){Text("نسخ")}
                            TextButton(onClick={deleteSection=s}){Text("حذف") }
                            if(i>0)TextButton(onClick={val list=project.sections.toMutableList();list[i]=list[i-1].also{list[i-1]=list[i]};onUpdate(project.copy(sections=list))}){Text("↑")}
                        }
                    }}
                }
                "الحصر والتكلفة"->{
                    item{BoxCard{Text("ملخص النطاق المختار",fontWeight=FontWeight.Bold);summary.forEach{MetricRow(it.name,"${fmt(it.quantity)} ${it.unit.label}")}}}
                    item{CostSummary(rows.sumOf{it.materialCost},rows.sumOf{it.labor},rows.sumOf{it.transport},rows.sumOf{it.equipment},rows.flatMap{MaterialReview.missing(it)}.distinct())}
                    val groups=rows.groupBy{it.spaceId to it.itemId}.values.toList()
                    items(groups.size){i->val group=groups[i];BoxCard{
                        Text(group.first().item,fontWeight=FontWeight.Bold)
                        Text(group.first().location,style=MaterialTheme.typography.bodySmall)
                        group.forEach{MetricRow(it.part,"${fmt(it.quantity)} ${it.unit}")}
                        var show by remember(group.first().itemId){mutableStateOf(false)}
                        TextButton(onClick={show=!show}){Text(if(show)"إخفاء التفاصيل" else "عرض المواد والتكلفة")}
                        if(show)WorkResultCards(group,"${group.first().location} / ${group.first().item}")
                        TextButton(onClick={onEditSource(group.first().sectionId,group.first().spaceId,group.first().itemId)}){Text("تعديل البند")}
                    }}
                    if(rows.isEmpty())item{EmptyState("لا يوجد حصر في النطاق المختار","اختار نطاقًا آخر أو أضف بنودًا للمكان.")}
                }
                "شراء الخامات"->{
                    item{PurchaseCards(CostEngine.purchase(rows),rows.flatMap{MaterialReview.missing(it)}.isEmpty())}
                    val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
                    if(missing.isNotEmpty())item{Text("التكلفة غير مكتملة: "+missing.joinToString("، "),color=MaterialTheme.colorScheme.error)}
                }
                "حسابات محفوظة"->{items(scoped.calculations.size){i->val c=scoped.calculations[i];BoxCard{Text(c.title,fontWeight=FontWeight.Bold);Text(c.summary);MetricRow("التكلفة عند الحفظ","${fmt(c.cost)} جنيه");TextButton(onClick={onOpenCalc(c)}){Text("فتح بنفس المدخلات")}}}}
                else->{
                    item{BoxCard{
                        Text("إعدادات المونة الافتراضية",fontWeight=FontWeight.Bold)
                        Text("تُستخدم للأجزاء بدون إعدادات خاصة. الحسابات المحفوظة بالخامات الخاصة تحتفظ بقيمها.",style=MaterialTheme.typography.bodySmall)
                        listOf(Triple("cementPrice","سعر شيكارة الأسمنت 50 كجم","جنيه"),Triple("sandPrice","سعر متر الرمل","جنيه/م³"),Triple("plasterThickness","سمك المحارة","مم"),Triple("plasterSand","رمل مقابل جزء أسمنت للمحارة","جزء"),Triple("splashThickness","سمك الطرطشة","مم"),Triple("splashSand","رمل مقابل جزء أسمنت للطرطشة","جزء"),Triple("mortarWaste","هالك المونة","%")).forEach{(key,label,unit)->
                            val default=when(key){"plasterThickness"->"15";"plasterSand"->"4";"splashThickness","mortarWaste"->"5";"splashSand"->"2";else->"0"}
                            NumberFieldX(label,defaults[key]?:default,{defaults=defaults+(key to it)},unit)
                        }
                        HorizontalDivider()
                        val recipes=CalculatorLibrary.all.filter{it.id in setOf("tile","skirting","paint","waterproof","gypsum","masonry")}
                        ChoiceFieldX("إعدادات باقي خامات المشروع",recipes.first{it.id==defaultsTool}.title,recipes.map{it.title},{title->defaultsTool=recipes.first{it.title==title}.id})
                        val recipe=recipes.first{it.id==defaultsTool}
                        recipe.fields.filter{it.key !in setOf("area","length","count")}.forEach{field->
                            val key="recipe.${recipe.id}.${field.key}"
                            NumberFieldX(field.label,defaults[key]?:field.default,{defaults=defaults+(key to it)},field.unit)
                        }
                        Button(onClick={
                            val valid=defaults.all{(_,v)->v.toDoubleOrNull()?.let{it>=0&&it.isFinite()}==true}&&listOf("plasterThickness","splashThickness","plasterSand","splashSand").all{defaults[it]?.toDoubleOrNull()?.let{x->x>0}?:true}
                            if(valid){onUpdate(project.copy(defaults=defaults,updatedAt=System.currentTimeMillis()));Toast.makeText(context,"تم حفظ الإعدادات",Toast.LENGTH_SHORT).show()}
                            else Toast.makeText(context,"راجع الأسعار والأسماك والخلطات",Toast.LENGTH_LONG).show()
                        }){Text("اعتماد إعدادات المشروع")}
                    }}
                }
            }
            item{BoxCard{
                Text("تصدير النطاق المختار",fontWeight=FontWeight.Bold)
                ChoiceFieldX("نوع الكشف",reportKind,listOf("حصر وتكلفة تفصيلي","ملخص الكميات","شراء الخامات"),{reportKind=it})
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={export.launch("Fayroz-${project.name}.xlsx")},modifier=Modifier.weight(1f)){Text("Excel")}
                    OutlinedButton(onClick={ReportExport.printPdf(context,scoped,reportKind)},modifier=Modifier.weight(1f)){Text("PDF / طباعة")}
                }
                TextButton(onClick=onShare){Text("مشاركة ملخص المشروع")}
            }}
        }
    }
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

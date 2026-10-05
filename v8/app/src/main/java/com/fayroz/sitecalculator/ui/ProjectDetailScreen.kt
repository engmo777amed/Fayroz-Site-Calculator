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
            item{ChoiceFieldX("عرض",tab,listOf("الأدوار","الحصر والتكلفة","شراء الخامات","حسابات محفوظة","إعدادات المشروع والتصدير"),{tab=it})}
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
                            if(i>0)TextButton(onClick={val list=project.sections.toMutableList();list[i]=list[i-1].also{list[i-1]=list[i]};onUpdate(project.copy(sections=list))}){Text("↑")}
                        }
                    }}
                }
                "الحصر والتكلفة"->{
                    item{BoxCard{summary.forEach{MetricRow(it.name,"${fmt(it.quantity)} ${it.unit.label}")};MetricRow("تكلفة المواد","${fmt(rows.sumOf{it.materialCost})} جنيه");MetricRow("الإجمالي شامل التكاليف الإضافية","${fmt(rows.sumOf{it.total})} جنيه",true)}}
                    items(rows.size){i->val row=rows[i];BoxCard{
                        Text(row.item,fontWeight=FontWeight.Bold)
                        Text("${row.location} / ${row.part}",style=MaterialTheme.typography.bodySmall)
                        MetricRow("صافي","${fmt(row.quantity)} ${row.unit}")
                        if(row.spec!=null){
                            Text("سمك ${fmt(row.spec.thicknessMm)} مم • خلطة ${fmt(row.spec.cementParts)} : ${fmt(row.spec.sandParts)} • هالك خامات ${fmt(row.spec.waste)}%",style=MaterialTheme.typography.bodySmall)
                            MetricRow("أسمنت","${fmt(row.cementKg)} كجم ≈ ${fmt(row.cementKg/row.spec.bagKg)} شيكارة")
                            MetricRow("رمل","${fmt(row.sandM3)} م³")
                            MetricRow("تكلفة المواد","${fmt(row.materialCost)} جنيه")
                            MetricRow("مصنعية / نقل / معدات","${fmt(row.labor)} / ${fmt(row.transport)} / ${fmt(row.equipment)} جنيه")
                            MetricRow("تكلفة الجزء","${fmt(row.total)} جنيه",true)
                            if(row.quantity>0)MetricRow("تكلفة المواد للوحدة","${fmt(row.materialCost/row.quantity)} جنيه/${row.unit}")
                        }
                        row.materialLines.forEach{MetricRow(it.label,it.value)}
                        if(row.spec==null)MetricRow("تكلفة مواد الجزء","${fmt(row.materialCost)} جنيه",true)
                        var show by remember(row.itemId,row.partId){mutableStateOf(false)}
                        TextButton(onClick={show=!show}){Text("طريقة الحساب")};if(show)Text(row.formula,style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={onEditSource(row.sectionId,row.spaceId,row.itemId)}){Text("فتح المصدر للتعديل")}
                    }}
                }
                "شراء الخامات"->{
                    item{Text("تجميع خامات الأجزاء المتطابقة بالمواصفات والأسعار، ثم تقريب العبوات مرة واحدة.",style=MaterialTheme.typography.bodySmall)}
                    val purchases=CostEngine.purchase(rows)
                    items(purchases.size){i->val p=purchases[i];BoxCard{Text(p.material,fontWeight=FontWeight.Bold);MetricRow("احتياج فعلي","${fmt(p.amount)} ${p.unit}");p.packages?.let{MetricRow("شراء","$it عبوة")};MetricRow("تكلفة شراء","${fmt(p.cost)} جنيه",true)}}
                    item{MetricRow("إجمالي شراء الخامات","${fmt(purchases.sumOf{it.cost})} جنيه",true)}
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

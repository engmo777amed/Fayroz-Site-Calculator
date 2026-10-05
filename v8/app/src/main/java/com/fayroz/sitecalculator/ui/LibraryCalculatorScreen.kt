@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.CalculatorLibrary
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.data.V8Repository

@Composable
fun LibraryCalculatorScreen(toolId:String,seed:MaterialResult?,repository:V8Repository,project:Project?,
    onBack:()->Unit,onSave:(MaterialResult,String?,String?)->Unit){
    val def=CalculatorLibrary.all.firstOrNull{it.id==toolId}?:return
    val raw=remember(toolId,seed){mutableStateMapOf<String,String>().apply{
        def.fields.forEach{put(it.key,seed?.inputs?.get(it.key)?:repository.pref("calc.$toolId.${it.key}",it.default))}
        if(seed!=null&&seed.inputs.isEmpty()&&seed.sourceQuantity>0&&def.fields.any{it.key=="area"})put("area",exact(seed.sourceQuantity))
    }}
    val context=LocalContext.current
    var detailed by remember{mutableStateOf(false)}
    var baseline by remember{mutableStateOf<MaterialResult?>(null)}
    var label by remember{mutableStateOf(seed?.inputs?.get("_label")?:seed?.title?:def.title)}
    var source by remember{mutableStateOf("حساب مستقل")}
    var sectionId by remember{mutableStateOf<String?>(null)}
    var spaceId by remember{mutableStateOf<String?>(null)}
    val attempt=runCatching{CalculatorLibrary.evaluate(def,raw)}
    val answer=attempt.getOrNull()
    val result=answer?.let{CalculatorLibrary.result(def,raw.toMap(),it).copy(title=label)}
    val sources=remember(project){buildList{
        add(Triple("حساب مستقل",null as String?,null as String?))
        project?.let{p->
            add(Triple("المشروع كله",null,null))
            p.sections.forEach{s->
                add(Triple(s.name,s.id,null))
                s.spaces.forEach{sp->add(Triple("${s.name} / ${sp.name}",s.id,sp.id))}
            }
        }
    }}
    var sourceItem by remember{mutableStateOf("")}
    val summary=project?.let{p->
        val filtered=p.copy(sections=p.sections.filter{sectionId==null||it.id==sectionId}.map{s->s.copy(spaces=s.spaces.filter{spaceId==null||it.id==spaceId})})
        QuantityEngine.summarize(filtered)
    }.orEmpty()
    fun chooseSource(item:String){
        sourceItem=item
        val line=summary.firstOrNull{"${it.name} (${it.unit.label})"==item}
        if(line!=null&&line.unit==UnitType.AREA&&def.fields.any{it.key=="area"})raw["area"]=exact(line.quantity)
        if(line!=null&&line.unit==UnitType.LENGTH&&def.fields.any{it.key=="length"})raw["length"]=exact(line.quantity)
    }
    Scaffold(topBar={TopAppBar(title={Text(def.title,fontWeight=FontWeight.Bold)},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{Text(def.group,style=MaterialTheme.typography.labelMedium)}
            item{BoxCard{
                TextFieldX("اسم الحساب",label,{label=it})
                if(project!=null){
                    Text("المشروع: ${project.name}",fontWeight=FontWeight.Bold)
                    ChoiceFieldX("مكان الحفظ ومصدر الحصر",source,sources.map{it.first},{x->
                        source=x;val target=sources.first{it.first==x};sectionId=target.second;spaceId=target.third;sourceItem=""
                    })
                    if(source!="حساب مستقل"&&summary.isNotEmpty())ChoiceFieldX("تعبئة كمية من بند",sourceItem.ifBlank{"اختار بندًا"},summary.map{"${it.name} (${it.unit.label})"},::chooseSource)
                }
                Row{
                    FilterChip(!detailed,{detailed=false},label={Text("سريع")})
                    Spacer(Modifier.width(8.dp));FilterChip(detailed,{detailed=true},label={Text("تفصيلي")})
                    TextButton(onClick={def.fields.forEach{raw[it.key]=it.default}}){Text("القيم الأصلية")}
                }
                def.fields.filter{detailed||it.required||it.key in setOf("waste","price","thickness","coats","cementPrice","sandPrice")}.forEach{field->
                    NumberFieldX(field.label,raw[field.key].orEmpty(),{raw[field.key]=it},field.unit)
                }
                if(!detailed){
                    val hidden=def.fields.filter{!it.required&&it.key !in setOf("waste","price","thickness","coats","cementPrice","sandPrice")}
                    if(hidden.isNotEmpty())Text("القيم المستخدمة: "+hidden.joinToString(" • "){"${it.label} ${raw[it.key]} ${it.unit}"},style=MaterialTheme.typography.bodySmall)
                }
            }}
            if(answer!=null&&result!=null){
                item{BoxCard{
                    Text("النتيجة",fontWeight=FontWeight.Bold)
                    result.lines.forEach{MetricRow(it.label,it.value)}
                    MetricRow("تكلفة الشراء / الحساب","${fmt(result.cost)} جنيه",true)
                    if(result.cost==0.0)Text("أدخل أسعارًا لإظهار التكلفة.",style=MaterialTheme.typography.bodySmall)
                    var explain by remember{mutableStateOf(false)}
                    TextButton(onClick={explain=!explain}){Text("اتحسبت إزاي؟")}
                    if(explain)Text(result.explanation,style=MaterialTheme.typography.bodySmall)
                    baseline?.let{old->
                        HorizontalDivider();Text("مقارنة البديل المحفوظ",fontWeight=FontWeight.Bold)
                        old.lines.forEach{MetricRow("سابق: ${it.label}",it.value)}
                        MetricRow("تكلفة السابق","${fmt(old.cost)} جنيه")
                        MetricRow("فرق التكلفة الحالي − السابق","${fmt(result.cost-old.cost)} جنيه",true)
                    }
                    Row{
                        TextButton(onClick={baseline=result}){Text("ثبّت للمقارنة")}
                        TextButton(onClick={baseline=null}){Text("مسح المقارنة")}
                    }
                    Button(onClick={
                        val next=result.copy(inputs=result.inputs+mapOf("_label" to label,"_source" to source))
                        def.fields.forEach{repository.setPref("calc.$toolId.${it.key}",raw[it.key].orEmpty())}
                        if(project!=null&&source!="حساب مستقل")onSave(next,sectionId,spaceId)
                        else repository.saveRecentCalc(SavedCalculation(toolId=next.toolId,title=next.title,summary=next.lines.joinToString(" • "){"${it.label}: ${it.value}"},sourceQuantity=next.sourceQuantity,unit=next.sourceUnit,inputs=next.inputs,cost=next.cost,explanation=next.explanation))
                        Toast.makeText(context,"تم حفظ الحساب بنجاح ✓",Toast.LENGTH_SHORT).show()
                    },modifier=Modifier.fillMaxWidth()){Text(if(project!=null&&source!="حساب مستقل")"حفظ داخل المشروع" else "حفظ الحساب")}
                    OutlinedButton(onClick={
                        val text=buildString{appendLine(label);appendLine(source);result.lines.forEach{appendLine("${it.label}: ${it.value}")};appendLine("التكلفة: ${fmt(result.cost)} جنيه");appendLine(result.explanation)}
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"مشاركة الحساب"))
                    },modifier=Modifier.fillMaxWidth()){Text("مشاركة النتيجة")}
                }}
            }else item{Text(attempt.exceptionOrNull()?.message?:"أكمل البيانات المطلوبة",color=MaterialTheme.colorScheme.error)}
        }
    }
}

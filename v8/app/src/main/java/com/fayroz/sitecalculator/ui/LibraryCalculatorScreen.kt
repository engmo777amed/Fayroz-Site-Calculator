@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
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
    var stage by remember{mutableIntStateOf(if(seed!=null)1 else 0)}
    var error by remember{mutableStateOf<String?>(null)}
    val listState=androidx.compose.foundation.lazy.rememberLazyListState()
    val scope=rememberCoroutineScope()
    var baseline by remember{mutableStateOf<MaterialResult?>(null)}
    var label by remember{mutableStateOf(seed?.inputs?.get("_label")?:seed?.title?:def.title)}
    var source by remember{mutableStateOf(seed?.inputs?.get("_source")?:"حساب مستقل")}
    var sectionId by remember{mutableStateOf(seed?.inputs?.get("_sectionId")?.takeIf{it.isNotBlank()})}
    var spaceId by remember{mutableStateOf(seed?.inputs?.get("_spaceId")?.takeIf{it.isNotBlank()})}
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
    fun showResult(){
        error=attempt.exceptionOrNull()?.message
        if(error==null)stage=1
        else{
            val fields=def.fields.filter{detailed||it.key !in setOf("dry","density","extraRate","extraPrice")}
            val index=fields.indexOfFirst{error.orEmpty().contains(it.label)}
            if(index<0)detailed=true
            val target=if(index>=0)index else def.fields.indexOfFirst{error.orEmpty().contains(it.label)}
            scope.launch{listState.animateScrollToItem(if(target<0)0 else target+3)}
        }
    }
    fun saveResult(){
        if(result==null)return
        val next=result.copy(inputs=result.inputs+mapOf("_label" to label,"_source" to source,"_sectionId" to sectionId.orEmpty(),"_spaceId" to spaceId.orEmpty()))
        def.fields.forEach{repository.setPref("calc.$toolId.${it.key}",raw[it.key].orEmpty())}
        if(project!=null&&source!="حساب مستقل")onSave(next,sectionId,spaceId)
        else repository.saveRecentCalc(SavedCalculation(toolId=next.toolId,title=next.title,summary=next.lines.joinToString(" • "){"${it.label}: ${it.value}"},sourceQuantity=next.sourceQuantity,unit=next.sourceUnit,inputs=next.inputs,cost=next.cost,explanation=next.explanation))
        Toast.makeText(context,"تم حفظ النتيجة ✓",Toast.LENGTH_SHORT).show()
    }
    BackHandler{if(stage==1){stage=0;error=null}else onBack()}
    LaunchedEffect(stage){listState.scrollToItem(0)}
    Scaffold(topBar={TopAppBar(title={Column{Text(def.title,fontWeight=FontWeight.Bold);Text("الحاسبات / ${def.group}",style=MaterialTheme.typography.labelSmall)}},navigationIcon={TextButton(onClick={if(stage==1){stage=0;error=null}else onBack()}){Text("رجوع")}})},
        bottomBar={Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                if(stage==1)OutlinedButton(onClick={stage=0},modifier=Modifier.weight(1f)){Text("تعديل المدخلات")}
                Button(onClick={if(stage==0)showResult() else saveResult()},modifier=Modifier.weight(1f)){Text(if(stage==0)"احسب واعرض النتيجة" else "حفظ النتيجة")}
            }
        }}}
    ){padding->Column(Modifier.fillMaxSize().padding(padding)){
        StageNavigation(stage,listOf("إدخال البيانات","النتيجة")){i->if(i==0){stage=0;error=null}else showResult()}
        LazyColumn(state=listState,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(stage==0){
                item{BoxCard{
                    TextFieldX("اسم الحساب",label,{label=it})
                    if(project!=null){
                        Text("المشروع: ${project.name}",fontWeight=FontWeight.Bold)
                        ChoiceFieldX("مكان الحفظ ومصدر الحصر",source,sources.map{it.first},{x->source=x;val target=sources.first{it.first==x};sectionId=target.second;spaceId=target.third;sourceItem=""})
                        if(source!="حساب مستقل"&&summary.isNotEmpty())ChoiceFieldX("تعبئة كمية من بند",sourceItem.ifBlank{"اختار بندًا"},summary.map{"${it.name} (${it.unit.label})"},::chooseSource)
                    }
                }}
                item{Text("البيانات المطلوبة",fontWeight=FontWeight.Bold);Text("الأسعار اختيارية؛ يمكن استخراج الكميات قبل التسعير.",style=MaterialTheme.typography.bodySmall)}
                item{TextButton(onClick={detailed=!detailed}){Text(if(detailed)"إخفاء المواصفات الإضافية" else "مواصفات إضافية")}}
                val visible=def.fields.filter{detailed||it.key !in setOf("dry","density","extraRate","extraPrice")}
                items(visible.size){i->val field=visible[i];BoxCard{
                    CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null})
                    if(error.orEmpty().contains(field.label)&&error!=null)Text(error.orEmpty(),color=MaterialTheme.colorScheme.error)
                }}
                if(!detailed)item{Text("مواصفات مستخدمة: "+def.fields.filter{it.key in setOf("dry","density","extraRate","extraPrice")}.joinToString(" • "){"${it.label}: ${raw[it.key]} ${it.unit}"},style=MaterialTheme.typography.bodySmall)}
                item{OutlinedButton(onClick={def.fields.forEach{raw[it.key]=it.default};error=null},modifier=Modifier.fillMaxWidth()){Text("استعادة القيم الأصلية")}}
            }else if(answer!=null&&result!=null){
                item{CalculatorResultCards(def,raw.toMap(),answer,"$label • $source")}
                item{OutlinedButton(onClick={
                    val text=buildString{appendLine(label);appendLine(source);result.lines.forEach{appendLine("${it.label}: ${it.value}")};appendLine("تكلفة الشراء: ${fmt(result.cost)} جنيه");val missing=com.fayroz.sitecalculator.domain.MaterialReview.missing(def,raw);if(missing.isNotEmpty())appendLine("التكلفة غير مكتملة: "+missing.joinToString("، "));appendLine(result.explanation)}
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"مشاركة النتيجة"))
                },modifier=Modifier.fillMaxWidth()){Text("تصدير / مشاركة النتيجة")}}
                item{BoxCard{
                    baseline?.let{old->Text("مقارنة البديل",fontWeight=FontWeight.Bold);old.lines.forEach{MetricRow("سابق: ${it.label}",it.value)};MetricRow("فرق تكلفة الشراء","${fmt(result.cost-old.cost)} جنيه")}
                    TextButton(onClick={baseline=result}){Text("ثبّت النتيجة للمقارنة")}
                    if(baseline!=null)TextButton(onClick={baseline=null}){Text("مسح المقارنة")}
                }}
            }else item{Text("عدّل البيانات لإظهار نتيجة صالحة.",color=MaterialTheme.colorScheme.error)}
        }
    }}
}

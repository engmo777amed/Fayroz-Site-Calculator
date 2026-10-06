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
        def.fields.forEach{field->
            put(field.key,seed?.inputs?.get(field.key)?:if(project!=null)CalculatorLibrary.defaults(def,project.defaults)[field.key]?:field.default else repository.pref("calc.$toolId.${field.key}",field.default))
            seed?.inputs?.get("_unit.${field.key}")?.let{unit->put("_unit.${field.key}",unit)}
        }
        seed?.inputs?.filterKeys{it.startsWith("_")}?.forEach{(k,v)->put(k,v)}
        put("_mortarMode",seed?.inputs?.get("_mortarMode")?:"area")
        put("_previousMode",seed?.inputs?.get("_mortarMode")?:"area")
        put("_volume",seed?.inputs?.get("_volume")?:"1")
        if(seed!=null&&seed.inputs.isEmpty()&&seed.sourceQuantity>0&&def.fields.any{it.key=="area"})put("area",exact(seed.sourceQuantity))
    }}
    val context=LocalContext.current
    var detailed by remember{mutableStateOf(false)}
    var savedId by remember{mutableStateOf(seed?.inputs?.get("_savedId")?:java.util.UUID.randomUUID().toString())}
    var includeInProject by remember{mutableStateOf(seed?.inputs?.get("_includeInProject")=="true")}
    val mortar=toolId in CalculatorLibrary.mortarIds
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
            p.sections.forEachIndexed{si,s->
                val sectionLabel=selectionLabels(p.sections.map{it.name})[si]
                add(Triple(sectionLabel,s.id,null))
                s.spaces.forEachIndexed{spi,sp->add(Triple("$sectionLabel / ${selectionLabels(s.spaces.map{it.name})[spi]}",s.id,sp.id))}
            }
        }
    }}
    var sourceItem by remember{mutableStateOf(seed?.inputs?.get("_sourceItem").orEmpty())}
    val summary=project?.let{p->
        val filtered=p.copy(sections=p.sections.filter{sectionId==null||it.id==sectionId}.map{s->s.copy(spaces=s.spaces.filter{spaceId==null||it.id==spaceId})})
        QuantityEngine.summarize(filtered)
    }.orEmpty()
    fun chooseSource(item:String){
        sourceItem=item
        raw["_quantityCopiedAt"]=System.currentTimeMillis().toString()
        includeInProject=false
        val line=summary.firstOrNull{"${it.name} (${it.unit.label})"==item}
        if(line!=null&&line.unit==UnitType.AREA&&def.fields.any{it.key=="area"})raw["area"]=exact(line.quantity)
        if(line!=null&&line.unit==UnitType.LENGTH&&def.fields.any{it.key=="length"})raw["length"]=exact(line.quantity)
    }
    fun showResult(){
        error=attempt.exceptionOrNull()?.message
        if(error==null)stage=1
        else scope.launch{listState.animateScrollToItem(0)}
    }
    fun saveResult(){
        if(result==null)return
        val next=result.copy(inputs=result.inputs+mapOf("_label" to label,"_source" to source,"_sectionId" to sectionId.orEmpty(),"_spaceId" to spaceId.orEmpty(),"_savedId" to savedId,"_savedAt" to System.currentTimeMillis().toString(),"_sourceItem" to sourceItem,"_includeInProject" to (includeInProject&&sourceItem.isBlank()&&source!="حساب مستقل").toString()))
        def.fields.forEach{repository.setPref("calc.$toolId.${it.key}",raw[it.key].orEmpty())}
        if(project!=null&&(source!="حساب مستقل"||project.calculations.any{it.id==savedId}))onSave(next,sectionId,spaceId)
        else repository.saveRecentCalc(SavedCalculation(id=savedId,toolId=next.toolId,title=next.title,summary=next.lines.joinToString(" • "){"${it.label}: ${it.value}"},sourceQuantity=next.sourceQuantity,unit=next.sourceUnit,inputs=next.inputs,cost=next.cost,explanation=next.explanation))
        Toast.makeText(context,"تم حفظ النتيجة ✓",Toast.LENGTH_SHORT).show()
    }
    BackHandler{if(stage==1){stage=0;error=null}else onBack()}
    LaunchedEffect(stage){listState.scrollToItem(0)}
    Scaffold(topBar={TopAppBar(title={Column{Text(def.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis);Text("الحاسبات / ${def.group}",style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}},navigationIcon={TextButton(onClick={if(stage==1){stage=0;error=null}else onBack()}){Text("رجوع")}})},
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
                item{Text(if(seed!=null)"قيم محفوظة بتاريخ الحساب؛ تعديل السعر لا يغيّر الكمية." else if(project!=null)"مواصفات وأسعار البداية من مشروع: ${project.name}" else "حساب مستقل؛ راجع مواصفات الخامة وأسعارها.",style=MaterialTheme.typography.bodySmall)}
                if(mortar)item{BoxCard{
                    ChoiceFieldX("عايز تحسب إيه؟",when(raw["_mortarMode"]){"unit"->"مكونات ١ م³ مونة";"coverage"->"المونة المتاحة تفرد كام؟";else->"خامات لمساحة"},
                        listOf("خامات لمساحة","مكونات ١ م³ مونة","المونة المتاحة تفرد كام؟"),{choice->
                            raw["_mortarMode"]=when(choice){"مكونات ١ م³ مونة"->"unit";"المونة المتاحة تفرد كام؟"->"coverage";else->"area"}
                            val previous=raw["_previousMode"]?:"area"
                            raw["_waste.$previous"]=raw["waste"]?:"5"
                            val next=raw["_mortarMode"].orEmpty()
                            raw["waste"]=raw["_waste.$next"]?:if(next=="unit")"0"else "5"
                            raw["_previousMode"]=next
                            error=null
                        })
                    if(raw["_mortarMode"]=="coverage")NumberFieldX("حجم المونة",raw["_volume"].orEmpty(),{raw["_volume"]=it;error=null},"م³")
                    Text("الحجم هنا مونة ناتجة، وليس حجم الرمل وحده.",style=MaterialTheme.typography.bodySmall)
                }}
                val advancedKeys=setOf("dry","extraRate","cement","sand","bag")
                val basic=def.fields.filter{f->!f.key.contains("price",true)&&f.key !in advancedKeys&&
                    (f.key!="density"||!mortar)&&(f.required||f.key in setOf("waste","faces","upstand","perimeter","overlapL","overlapW","tail","deduct"))&&!(mortar&&f.key=="area"&&raw["_mortarMode"]!="area")}
                item{BoxCard{
                    Text("البيانات المطلوبة",fontWeight=FontWeight.Bold)
                    basic.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null},raw["_unit.${field.key}"],{raw["_unit.${field.key}"]=it})}
                }}
                if(mortar)item{BoxCard{
                    Text("الخلطة المستخدمة",fontWeight=FontWeight.Bold)
                    val cement=n(raw["cement"].orEmpty());val sand=n(raw["sand"].orEmpty());val total=cement+sand
                    if(total>0&&n(raw["bag"].orEmpty())>0){
                        val dry=n(raw["dry"].orEmpty());val kg=dry*cement/total*n(raw["density"].orEmpty())
                        Text("لكل ١ م³ مونة قبل الهالك: ${fmt(dry*sand/total)} م³ رمل + ${fmt(kg/n(raw["bag"].orEmpty()))} شيكارة × ${raw["bag"]} كجم.")
                    }
                    Text("يمكن تغيير الخلطة من الإعدادات الإضافية.",style=MaterialTheme.typography.bodySmall)
                }}
                item{BoxCard{ExpandableSection(if(mortar)"تغيير الخلطة والهالك والإضافات" else "تفاصيل إضافية وهالك",error!=null){
                    def.fields.filter{f->!f.key.contains("price",true)&&f !in basic&&!(f.key=="area"&&mortar&&raw["_mortarMode"]!="area")}.forEach{field->
                        CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null},raw["_unit.${field.key}"],{raw["_unit.${field.key}"]=it})
                    }
                }}}
                if(def.fields.any{it.key.contains("price",true)})item{BoxCard{ExpandableSection("الأسعار — اختيارية",error!=null){
                    def.fields.filter{it.key.contains("price",true)}.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null},raw["_unit.${field.key}"],{raw["_unit.${field.key}"]=it})}
                }}}
                item{BoxCard{ExpandableSection("اسم الصنف ومواصفاته"){
                    TextFieldX("اسم الصنف والمواصفة",raw["_materialName"].orEmpty(),{raw["_materialName"]=it},placeholder="النوع / الماركة / اللون / المقاس")
                    if(mortar||def.id in setOf("masonry","blocks")){
                        TextFieldX("نوع الأسمنت وماركته",raw["_cementName"]?:"أسمنت",{raw["_cementName"]=it})
                        TextFieldX("نوع الرمل",raw["_sandName"]?:"رمل",{raw["_sandName"]=it})
                    }
                }}}
                if(project!=null)item{BoxCard{ExpandableSection("تعبئة كمية من المشروع"){
                    Text("تعبئة الكمية تنسخ الحصر الحالي؛ لا تتحدث تلقائيًا بعد تعديل المقاسات.",style=MaterialTheme.typography.bodySmall)
                    Text("المشروع: ${project.name}",fontWeight=FontWeight.Bold)
                    ChoiceFieldX("مكان الحفظ ومصدر الحصر",source,sources.map{it.first},{x->source=x;val target=sources.first{it.first==x};sectionId=target.second;spaceId=target.third;sourceItem="";raw["_mortarMode"]="area"})
                    if(source!="حساب مستقل"&&summary.isNotEmpty())ChoiceFieldX("تعبئة كمية من بند",sourceItem.ifBlank{"اختار بندًا"},summary.map{"${it.name} (${it.unit.label})"},::chooseSource)
                }}}
                item{TextButton(onClick={def.fields.forEach{raw[it.key]=it.default;raw.remove("_unit.${it.key}")};raw["_mortarMode"]="area";error=null}){Text("استعادة القيم الأصلية")}}
            }else if(answer!=null&&result!=null){
                item{CalculatorResultCards(def,raw.toMap(),answer,"$label • $source")}
                if(sourceItem.isNotBlank())item{Text("مصدر الكمية: $sourceItem • نسخة من الحصر وقت التعبئة",style=MaterialTheme.typography.bodySmall)}
                item{BoxCard{ExpandableSection("اسم الحساب ومكان الحفظ"){
                    TextFieldX("اسم الحساب",label,{label=it})
                    if(project!=null&&source!="حساب مستقل"){
                        Row{Checkbox(includeInProject,{includeInProject=it},enabled=sourceItem.isBlank());Text("إضافة الحساب لحصر وتكلفة وشراء المشروع",Modifier.weight(1f))}
                        Text(if(sourceItem.isBlank())"اترك الاختيار مغلقًا لحفظ حساب مرجعي فقط." else "الكمية من بند موجود بالفعل؛ يُحفظ الحساب كمرجع لمنع حسابها مرتين.",style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick={savedId=java.util.UUID.randomUUID().toString();saveResult()}){Text("حفظ نسخة جديدة")}
                    raw["_quantityCopiedAt"]?.toLongOrNull()?.let{Text("كمية من الحصر بتاريخ: ${dated(it)}",style=MaterialTheme.typography.bodySmall)}
                    if(project!=null)ChoiceFieldX("مكان الحفظ ومصدر الحصر",source,sources.map{it.first},{x->source=x;val target=sources.first{it.first==x};sectionId=target.second;spaceId=target.third})
                }}}
                item{OutlinedButton(onClick={
                    val text=buildString{appendLine(label);appendLine(source);result.lines.forEach{appendLine("${it.label}: ${it.value}")};appendLine("تكلفة الشراء: ${fmt(result.cost)} جنيه");val missing=com.fayroz.sitecalculator.domain.MaterialReview.missing(def,raw);if(missing.isNotEmpty())appendLine("التكلفة غير مكتملة: "+missing.joinToString("، "));appendLine(result.explanation)}
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"مشاركة النتيجة"))
                },modifier=Modifier.fillMaxWidth()){Text("تصدير / مشاركة النتيجة")}}
                item{BoxCard{ExpandableSection("مقارنة نتيجة أخرى"){
                    baseline?.let{old->Text("مقارنة البديل",fontWeight=FontWeight.Bold);old.lines.forEach{MetricRow("سابق: ${it.label}",it.value)};MetricRow("فرق تكلفة الشراء","${fmt(result.cost-old.cost)} جنيه")}
                    TextButton(onClick={baseline=result}){Text("ثبّت النتيجة للمقارنة")}
                    if(baseline!=null)TextButton(onClick={baseline=null}){Text("مسح المقارنة")}
                }}}
            }else item{Text("عدّل البيانات لإظهار نتيجة صالحة.",color=MaterialTheme.colorScheme.error)}
        }
    }}
}


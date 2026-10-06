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
import com.fayroz.sitecalculator.domain.MortarMix
import com.fayroz.sitecalculator.data.V8Repository

@Composable
fun LibraryCalculatorScreen(toolId:String,seed:MaterialResult?,repository:V8Repository,project:Project?,
    onBack:()->Unit,onSave:(MaterialResult,String?,String?)->Unit){
    val def=CalculatorLibrary.all.firstOrNull{it.id==toolId}?:return
    val raw=remember(toolId,seed){mutableStateMapOf<String,String>().apply{
        def.fields.forEach{field->
            put(field.key,seed?.inputs?.get(field.key)?:if(project!=null)CalculatorLibrary.defaults(def,project.defaults)[field.key]?:field.default else if(field.key in setOf("area","length","count","bags")||field.default.isBlank())field.default else repository.pref("calc.$toolId.${field.key}",field.default))
            seed?.inputs?.get("_unit.${field.key}")?.let{unit->put("_unit.${field.key}",unit)}
        }
        seed?.inputs?.filterKeys{it.startsWith("_")}?.forEach{(k,v)->put(k,v)}
        if(MortarMix.isMix(def))put(MortarMix.key,seed?.let{it.inputs[MortarMix.key]?:exact(MortarMix.bags(this))}?:project?.let{CalculatorLibrary.defaults(def,it.defaults)[MortarMix.key]?:exact(MortarMix.bags(this))}?:repository.pref("calc.$toolId.${MortarMix.key}",exact(MortarMix.bags(this))))
        put("_mortarMode",seed?.inputs?.get("_mortarMode")?:"area")
        put("_previousMode",seed?.inputs?.get("_mortarMode")?:"area")
        put("_volume",seed?.inputs?.get("_volume")?:"1")
        put("_sandAvailable",seed?.inputs?.get("_sandAvailable")?:"1")
        if(seed!=null&&seed.inputs.isEmpty()&&seed.sourceQuantity>0&&def.fields.any{it.key=="area"})put("area",exact(seed.sourceQuantity))
    }}
    val context=LocalContext.current
    var detailed by remember{mutableStateOf(false)}
    var quickEdit by remember{mutableStateOf(false)}
    var templateDialog by remember{mutableStateOf(false)}
    var templateName by remember{mutableStateOf("")}
    var templates by remember{mutableStateOf(repository.calculatorTemplates(toolId))}
    fun specification()=raw.filterKeys{key->key !in setOf("area","length","count","_areaLength","_areaWidth","_areaMethod","_mortarMode","_previousMode","_volume","_sandAvailable","_bagsAvailable")&&!key.contains("price",true)&&!key.startsWith("_waste.")&&(def.fields.any{it.key==key}||key==MortarMix.key||key.startsWith("_unit."))}
    fun rememberSpecification(){specification().forEach{(key,value)->repository.setPref("calc.$toolId.$key",value)}}
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
        if(error==null){rememberSpecification();stage=1;quickEdit=false}
        else scope.launch{listState.animateScrollToItem(0)}
    }
    fun saveResult(){
        if(result==null)return
        val next=result.copy(inputs=result.inputs+mapOf("_label" to label,"_source" to source,"_sectionId" to sectionId.orEmpty(),"_spaceId" to spaceId.orEmpty(),"_savedId" to savedId,"_savedAt" to System.currentTimeMillis().toString(),"_sourceItem" to sourceItem,"_includeInProject" to (includeInProject&&sourceItem.isBlank()&&source!="حساب مستقل").toString()))
        rememberSpecification()
        if(project!=null&&(source!="حساب مستقل"||project.calculations.any{it.id==savedId}))onSave(next,sectionId,spaceId)
        else repository.saveRecentCalc(SavedCalculation(id=savedId,toolId=next.toolId,title=next.title,summary=next.lines.joinToString(" • "){"${it.label}: ${it.value}"},sourceQuantity=next.sourceQuantity,unit=next.sourceUnit,inputs=next.inputs,cost=next.cost,explanation=next.explanation))
        Toast.makeText(context,"تم حفظ النتيجة ✓",Toast.LENGTH_SHORT).show()
    }
    if(templateDialog)AlertDialog(onDismissRequest={templateDialog=false},title={Text("حفظ إعداداتي")},text={TextFieldX("اسم الإعداد",templateName,{templateName=it})},confirmButton={TextButton(enabled=templateName.isNotBlank()&&answer!=null,onClick={repository.saveCalculatorTemplate(toolId,templateName.trim(),specification());templates=repository.calculatorTemplates(toolId);templateDialog=false;Toast.makeText(context,"تم حفظ الإعداد ✓",Toast.LENGTH_SHORT).show()}){Text("حفظ الإعداد")}},dismissButton={TextButton(onClick={templateDialog=false}){Text("إلغاء")}})
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
                item{BasicCalculatorInputs(def,raw){key,value->raw[key]=value;error=null}}
                item{BoxCard{ExpandableSection("إعداداتي وتفاصيل إضافية",error!=null){
                    if(templates.isNotEmpty())ChoiceFieldX("خلطة أو إعداد محفوظ","اختار إعدادًا",templates.map{it.first},{name->templates.firstOrNull{it.first==name}?.second?.forEach{(key,value)->raw[key]=value}})
                    TextButton(onClick={templateName=def.title;templateDialog=true}){Text("حفظ إعداداتي باسم")}
                    Text("الهالك ${raw["waste"]?:"0"}%"+(if(MortarMix.isMix(def))" • الشيكارة ${raw["bag"]?:"50"} كجم" else ""),style=MaterialTheme.typography.bodySmall)
                    val basic=MortarMix.basic(def)
                    def.fields.filter{f->f !in basic&&!f.key.contains("price",true)&&f.key !in setOf("cement","sand")}.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null},raw["_unit.${field.key}"],{raw["_unit.${field.key}"]=it})}
                    TextFieldX("اسم الصنف والمواصفة",raw["_materialName"].orEmpty(),{raw["_materialName"]=it},placeholder="اختياري")
                    if(MortarMix.isMix(def)){
                        TextFieldX("نوع الأسمنت وماركته",raw["_cementName"]?:"أسمنت",{raw["_cementName"]=it})
                        TextFieldX("نوع الرمل",raw["_sandName"]?:"رمل",{raw["_sandName"]=it})
                    }
                    if(def.fields.any{it.key.contains("price",true)})ExpandableSection("الأسعار — اختيارية"){
                        Text("يمكن إدخال الأسعار بعد ظهور النتيجة أيضًا.",style=MaterialTheme.typography.bodySmall)
                        def.fields.filter{it.key.contains("price",true)}.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null})}
                    }
                    if(project!=null)ExpandableSection("تعبئة كمية من المشروع"){
                        Text("نسخة من الحصر الحالي؛ لا تتحدث تلقائيًا.",style=MaterialTheme.typography.bodySmall)
                        ChoiceFieldX("مكان الحفظ ومصدر الحصر",source,sources.map{it.first},{x->source=x;val target=sources.first{it.first==x};sectionId=target.second;spaceId=target.third;sourceItem="";raw["_mortarMode"]="area";raw["_areaMethod"]="ready"})
                        if(source!="حساب مستقل"&&summary.isNotEmpty())ChoiceFieldX("تعبئة كمية من بند",sourceItem.ifBlank{"اختار بندًا"},summary.map{"${it.name} (${it.unit.label})"},::chooseSource)
                    }
                    TextButton(onClick={raw.clear();def.fields.forEach{raw[it.key]=it.default};if(MortarMix.isMix(def))raw[MortarMix.key]=exact(MortarMix.bags(raw));raw["_mortarMode"]="area";error=null}){Text("استعادة القيم الأصلية")}
                }}}
            }else if(answer!=null&&result!=null){
                item{BoxCard{
                    TextButton(onClick={quickEdit=!quickEdit}){Text("تعديل سريع للنتيجة")}
                    if(quickEdit)BasicCalculatorInputs(def,raw){key,value->raw[key]=value;error=null;rememberSpecification()}
                }}
                item{CalculatorResultCards(def,raw.toMap(),answer,"$label • $source")}
                if(def.fields.any{it.key.contains("price",true)})item{BoxCard{ExpandableSection("احسب التكلفة"){
                    def.fields.filter{it.key.contains("price",true)}.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{raw[field.key]=it;error=null})}
                    Text("تكلفة الاستهلاك: ${money(answer.consumedCost)} جنيه • الشراء: ${money(answer.cost)} جنيه",style=MaterialTheme.typography.bodySmall)
                }}}
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
            }else item{BoxCard{
                Text(attempt.exceptionOrNull()?.message?:"راجع البيانات",color=MaterialTheme.colorScheme.error)
                BasicCalculatorInputs(def,raw){key,value->raw[key]=value;error=null}
            }}
        }
    }}
}


@Composable
private fun BasicCalculatorInputs(def:com.fayroz.sitecalculator.domain.CalcDef,raw:Map<String,String>,set:(String,String)->Unit){
    val mortar=def.id in CalculatorLibrary.mortarIds
    val mode=raw["_mortarMode"]?:"area"
    if(mortar){
        val modes=listOf("خامات لمساحة" to "area","مكونات ١ م³ مونة" to "unit","المونة المتاحة تفرد كام؟" to "coverage","معايا كمية رمل" to "sand","الرمل والأسمنت الموجودين يكفوا كام؟" to "stock")
        ChoiceFieldX("عايز تحسب إيه؟",modes.firstOrNull{it.second==mode}?.first?:modes.first().first,modes.map{it.first},{choice->
            val next=modes.first{it.first==choice}.second
            set("_waste.$mode",raw["waste"]?:"5");set("waste",raw["_waste.$next"]?:if(next=="unit")"0"else "5");set("_mortarMode",next)
        })
        if(mode=="coverage")NumberFieldX("حجم المونة",raw["_volume"]?:"1",{set("_volume",it)},"م³ مونة جاهزة")
        if(mode in setOf("sand","stock"))NumberFieldX("الرمل المتاح",raw["_sandAvailable"]?:"1",{set("_sandAvailable",it)},"م³ رمل")
        if(mode=="stock")NumberFieldX("شكاير الأسمنت المتاحة",raw["_bagsAvailable"].orEmpty(),{set("_bagsAvailable",it)},"شيكارة")
    }
    if(def.fields.any{it.key=="area"}&&(!mortar||mode=="area")){
        val dimensions=raw["_areaMethod"]=="dimensions"
        ChoiceFieldX("إدخال المساحة",if(dimensions)"من الطول والعرض" else "مساحة جاهزة",listOf("مساحة جاهزة","من الطول والعرض"),{choice->
            set("_areaMethod",if(choice=="من الطول والعرض")"dimensions"else "ready")
            if(choice=="من الطول والعرض")set("area",exact(n(raw["_areaLength"].orEmpty())*n(raw["_areaWidth"].orEmpty())))
        })
        if(dimensions){
            NumberFieldX("طول المسطح",raw["_areaLength"].orEmpty(),{set("_areaLength",it);set("area",exact(n(it)*n(raw["_areaWidth"].orEmpty())))},"م")
            NumberFieldX("عرض المسطح",raw["_areaWidth"].orEmpty(),{set("_areaWidth",it);set("area",exact(n(it)*n(raw["_areaLength"].orEmpty())))},"م")
            Text("المساحة: ${raw["area"]?:"0"} م²")
        }else def.fields.first{it.key=="area"}.let{CalcInputField(it,raw["area"].orEmpty(),{set("area",it)})}
    }
    MortarMix.basic(def).filter{it.key!="area"}.forEach{field->CalcInputField(field,raw[field.key].orEmpty(),{set(field.key,it)},raw["_unit.${field.key}"],{set("_unit.${field.key}",it)})}
    if(MortarMix.isMix(def)){
        NumberFieldX("شكاير الأسمنت على متر الرمل",raw[MortarMix.key]?:exact(MortarMix.bags(raw)),{set(MortarMix.key,it)},"شيكارة/م³ رمل")
        Text("الشيكارة ${raw["bag"]?:"50"} كجم • هالك ${raw["waste"]?:"5"}%",style=MaterialTheme.typography.bodySmall)
        if(mode in setOf("sand","stock","unit","coverage"))Text("ناتج المونة تقديري بمعامل ${raw["dry"]?:"1.33"}؛ الرمل والمونة حجمان مختلفان.",style=MaterialTheme.typography.bodySmall)
        if(mode=="stock"&&n(raw["_bagsAvailable"].orEmpty())>0)Text(if(n(raw["_bagsAvailable"].orEmpty())/MortarMix.bags(raw)<n(raw["_sandAvailable"]?:"1"))"الأسمنت هو المادة المحدِّدة للتنفيذ"else "الرمل هو المادة المحدِّدة للتنفيذ",style=MaterialTheme.typography.bodySmall)
        val bags=MortarMix.bags(raw)
        if(bags.isFinite()&&(bags<2||bags>15))Text("عدد الشكاير غير معتاد؛ راجع الخلطة المطلوبة لشغلك.",color=MaterialTheme.colorScheme.tertiary)
    }
    if(n(raw["thickness"].orEmpty())>100)Text("السمك المدخل أكبر من ١٠ سم؛ راجع الرقم والوحدة.",color=MaterialTheme.colorScheme.tertiary)
}

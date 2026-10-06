@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.Project
import com.fayroz.sitecalculator.domain.*
import com.fayroz.sitecalculator.data.V8Repository

@Composable
fun PricesScreen(projects:List<Project>,activeId:String?,onBack:()->Unit,onSave:(Project)->Unit,repository:V8Repository){
    val context=LocalContext.current
    var target by remember{mutableStateOf<String?>(null)}
    var central by remember{mutableStateOf(repository.centralPrices())}
    val project=projects.firstOrNull{it.id==target}
    var tool by remember{mutableStateOf("tile")}
    val def=CalculatorLibrary.all.first{it.id==tool}
    var values by remember(target){mutableStateOf(if(target==null)central else project?.defaults.orEmpty())}
    val inherited=if(target==null)values else central+values
    var raw by remember(target,tool){mutableStateOf(CalculatorLibrary.defaults(def,inherited))}
    var error by remember{mutableStateOf<String?>(null)}
    var reprice by remember{mutableStateOf(false)}
    var saved by remember{mutableStateOf(false)}
    fun update(key:String,value:String){raw=raw+(key to value);if(key in PriceBook.stockKeys||key=="_materialName")raw=PriceBook.apply(def,raw+def.fields.filter{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}.associate{it.key to "0"},inherited);saved=false}
    fun save(){
        val prices=raw.filterKeys{it.contains("price",true)}+values.filterKeys{it in setOf("cementPrice","sandPrice")||it.endsWith("Rate")}
        if(prices.values.any{it.isNotBlank()&&MortarMix.number(it)?.let{n->n>=0}!=true}){error="راجع الأسعار: اكتب أرقامًا صفر أو أكبر.";return}
        if(def.fields.filter{it.key in PriceBook.stockKeys}.any{f->MortarMix.number(raw[f.key])?.let{it>0}!=true&&f.required}){error="راجع مقاس الصنف وحجم عبوة الشراء.";return}
        val normalized=raw.mapValues{(k,v)->if(k.contains("price",true))exact(MortarMix.number(v)?:0.0)else v}
        val next=values.filterKeys{it !in def.fields.filter{f->f.key.contains("price",true)}.map{f->"recipe.${def.id}.${f.key}"}}+PriceBook.record(def,normalized)+("_pricesUpdatedAt" to System.currentTimeMillis().toString())
        values=next
        if(project==null){repository.saveCentralPrices(next);central=next}
        else onSave(project.copy(defaults=next,updatedAt=System.currentTimeMillis()))
        error=null;saved=true;Toast.makeText(context,"تم حفظ الأسعار ✓",Toast.LENGTH_SHORT).show()
    }
    Scaffold(topBar={TopAppBar(title={Text("الأسعار")},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})},bottomBar={Surface(shadowElevation=6.dp){Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp)){
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick=::save,modifier=Modifier.fillMaxWidth()){Text(if(target==null)"حفظ الأسعار العامة"else "حفظ أسعار المشروع")}
    }}}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{val labels=selectionLabels(projects.map{it.name});ChoiceFieldX("تطبيق الأسعار",if(target==null)"كل المشروعات — أسعار عامة"else labels[projects.indexOfFirst{it.id==target}],listOf("كل المشروعات — أسعار عامة")+labels,{name->target=projects.getOrNull(labels.indexOf(name))?.id;saved=false})}
        item{Text("تُستخدم تلقائيًا للبنود والحسابات الجديدة. المحفوظ يظل بسعره القديم حتى تختار إعادة التسعير.",style=MaterialTheme.typography.bodySmall)}
        item{BoxCard{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            NumberFieldX("شيكارة أسمنت 50 كجم",values["cementPrice"]?:central["cementPrice"]?:"0",{values=values+("cementPrice" to it);saved=false},"جنيه",Modifier.weight(1f))
            NumberFieldX("متر الرمل",values["sandPrice"]?:central["sandPrice"]?:"0",{values=values+("sandPrice" to it);saved=false},"جنيه/م³",Modifier.weight(1f))
        }}}
        item{val priced=CalculatorLibrary.all.filter{d->d.fields.any{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}};ChoiceFieldX("الصنف / نوع العمل",def.title,priced.map{it.title},{name->tool=priced.first{it.title==name}.id;saved=false})}
        item{BoxCard{
            TextFieldX("اسم الصنف والمواصفة",raw["_materialName"].orEmpty(),{update("_materialName",it)},placeholder="مثال: سيراميك بيج — ماركة محددة")
            def.fields.filter{it.key in PriceBook.stockKeys}.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){pair.forEach{f->Column(Modifier.weight(1f)){CalcInputField(f,raw[f.key]?:f.default,{update(f.key,it)})}}}}
            Text("السعر لهذه المواصفة ووحدة الشراء فقط.",style=MaterialTheme.typography.labelSmall)
            def.fields.filter{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}.forEach{f->CalcInputField(f,raw[f.key]?:"0",{update(f.key,it)})}
        }}
        item{ExpandableSection("المصنعية والنقل والمعدات"){
            listOf("_laborRate" to "المصنعية للوحدة","_transportRate" to "النقل للوحدة","_equipmentRate" to "المعدات للوحدة").forEach{(key,label)->NumberFieldX(label,values["recipe.$tool.$key"]?:central["recipe.$tool.$key"]?:"0",{values=values+("recipe.$tool.$key" to it);saved=false},"جنيه/وحدة تنفيذ")}
        }}
        if(project!=null)item{OutlinedButton(onClick={reprice=true},enabled=saved,modifier=Modifier.fillMaxWidth()){Text("إعادة تسعير البنود المحفوظة")}}
        if(saved)item{Text("تم حفظ الأسعار ✓",color=MaterialTheme.colorScheme.primary)}
    }}
    if(reprice&&project!=null)AlertDialog(onDismissRequest={reprice=false},title={Text("تحديث أسعار البنود؟")},text={Text("ستُطبق الأسعار المطابقة للمواصفات على البنود والحسابات المضافة للمشروع. لن تتغير الكميات والخلطات. الحسابات المرجعية تظل بأسعارها القديمة.")},confirmButton={TextButton(onClick={onSave(CostEngine.reprice(project.copy(defaults=central+values)));reprice=false}){Text("إعادة التسعير")}},dismissButton={TextButton(onClick={reprice=false}){Text("إلغاء")}})
}

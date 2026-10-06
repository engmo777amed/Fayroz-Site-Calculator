@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.Project
import com.fayroz.sitecalculator.domain.CalculatorLibrary
@Composable
fun PricesScreen(projects:List<Project>,activeId:String?,onBack:()->Unit,onSave:(Project)->Unit){
    var projectId by remember{mutableStateOf(activeId?:projects.firstOrNull()?.id)}
    val project=projects.firstOrNull{it.id==projectId}
    var groupId by remember{mutableStateOf("tile")}
    var error by remember{mutableStateOf<String?>(null)}
    var saved by remember{mutableStateOf(false)}
    var reprice by remember{mutableStateOf(false)}
    var values by remember(projectId){mutableStateOf(project?.defaults.orEmpty())}
    val priced=CalculatorLibrary.all.filter{it.id !in setOf("plaster","splash","screed","bedding","custom_mortar")&&it.fields.any{f->f.key.contains("price",true)}}
    fun price(raw:String):Double?=raw.map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()
    val def=CalculatorLibrary.all.first{it.id==groupId}
    Scaffold(topBar={TopAppBar(title={Text("أسعار الخامات")},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})},bottomBar={if(project!=null)Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()){
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={if(values.filterKeys{it.contains("price",true)}.values.any{it.isNotBlank()&&price(it)?.let{n->!n.isFinite()||n<0}!=false}){error="راجع الأسعار: اكتب أرقامًا صفر أو أكبر."}else{val normalized=values.mapValues{(key,value)->if(key.contains("price",true))exact(price(value)?:0.0)else value};onSave(project.copy(defaults=normalized+("_pricesUpdatedAt" to System.currentTimeMillis().toString()),updatedAt=System.currentTimeMillis()));saved=true;error=null}},modifier=Modifier.fillMaxWidth()){Text("حفظ أسعار المشروع")}
    }}}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(project==null)item{EmptyState("أنشئ مشروعًا أولًا","تُحفظ الأسعار لكل مشروع بشكل مستقل.")}
        else{
            item{val labels=selectionLabels(projects.map{it.name});ChoiceFieldX("المشروع",labels[projects.indexOfFirst{it.id==projectId}],labels,{name->projectId=projects[labels.indexOf(name)].id;saved=false})}
            item{Text("الأسعار الجديدة للبنود الجديدة. لتحديث البنود المحفوظة استخدم إعادة التسعير. مواصفات وكميات البنود لا تتغير.",style=MaterialTheme.typography.bodySmall)}
            item{BoxCard{NumberFieldX("سعر شيكارة الأسمنت 50 كجم",values["cementPrice"]?:"0",{values=values.filterKeys{!it.endsWith(".cementPrice")}+("cementPrice" to it);saved=false},"جنيه");NumberFieldX("سعر متر الرمل",values["sandPrice"]?:"0",{values=values.filterKeys{!it.endsWith(".sandPrice")}+("sandPrice" to it);saved=false},"جنيه/م³")}}
            item{ChoiceFieldX("أسعار حاسبة",def.title,priced.map{it.title},{name->groupId=CalculatorLibrary.all.first{it.title==name}.id})}
            item{BoxCard{def.fields.filter{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}.forEach{f->val k="recipe.${def.id}.${f.key}";NumberFieldX(f.label,values[k]?:values[f.key]?.takeIf{f.key in setOf("cementPrice","sandPrice")}?:"0",{values=values+(k to it);saved=false},f.unit)}}}
            item{project.defaults["_pricesUpdatedAt"]?.toLongOrNull()?.let{Text("آخر تحديث: ${dated(it)}",style=MaterialTheme.typography.bodySmall)}}
            item{OutlinedButton(onClick={reprice=true},modifier=Modifier.fillMaxWidth(),enabled=saved){Text("إعادة تسعير البنود المحفوظة")};if(!saved)Text("احفظ الأسعار أولًا لتفعيل إعادة التسعير.",style=MaterialTheme.typography.bodySmall)}
            if(saved)item{Text("تم حفظ الأسعار ✓",color=MaterialTheme.colorScheme.primary)}
        }
    }}
    if(reprice&&project!=null)AlertDialog(onDismissRequest={reprice=false},title={Text("تحديث أسعار البنود؟")},text={Text("سيُطبق سعر الأسمنت والرمل وأسعار الحاسبات المدخلة على البنود وأجزائها والحسابات المضافة للحصر، بما فيها الأسعار الخاصة. الحسابات المرجعية تحتفظ بأسعارها التاريخية. لن تتغير المقاسات أو الخلطات.")},confirmButton={TextButton(onClick={onSave(com.fayroz.sitecalculator.domain.CostEngine.reprice(project));reprice=false}){Text("إعادة التسعير")}},dismissButton={TextButton(onClick={reprice=false}){Text("إلغاء")}})
}


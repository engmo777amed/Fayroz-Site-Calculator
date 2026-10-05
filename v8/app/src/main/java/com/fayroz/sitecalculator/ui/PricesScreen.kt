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
    var values by remember(projectId){mutableStateOf(project?.defaults.orEmpty())}
    val priced=CalculatorLibrary.all.filter{it.id !in setOf("plaster","splash","screed","bedding","custom_mortar")&&it.fields.any{f->f.key.contains("price",true)}}
    fun price(raw:String):Double?=raw.map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()
    val def=CalculatorLibrary.all.first{it.id==groupId}
    Scaffold(topBar={TopAppBar(title={Text("أسعار الخامات")},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})},bottomBar={if(project!=null)Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()){
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={if(values.filterKeys{it.contains("price",true)}.values.any{it.isNotBlank()&&price(it)?.let{n->!n.isFinite()||n<0}!=false}){error="راجع الأسعار: اكتب أرقامًا صفر أو أكبر."}else{val normalized=values.mapValues{(key,value)->if(key.contains("price",true))exact(price(value)?:0.0)else value};onSave(project.copy(defaults=normalized,updatedAt=System.currentTimeMillis()));saved=true;error=null}},modifier=Modifier.fillMaxWidth()){Text("حفظ أسعار المشروع")}
    }}}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(project==null)item{EmptyState("أنشئ مشروعًا أولًا","تُحفظ الأسعار لكل مشروع بشكل مستقل.")}
        else{
            item{ChoiceFieldX("المشروع",project.name,projects.map{it.name},{name->projectId=projects.first{it.name==name}.id;saved=false})}
            item{Text("أسعار عامة للمشروع. يمكن تخصيص سعر لبند عند الحاجة.",style=MaterialTheme.typography.bodySmall)}
            item{BoxCard{NumberFieldX("سعر شيكارة الأسمنت 50 كجم",values["cementPrice"]?:"0",{values=values.filterKeys{!it.endsWith(".cementPrice")}+("cementPrice" to it);saved=false},"جنيه");NumberFieldX("سعر متر الرمل",values["sandPrice"]?:"0",{values=values.filterKeys{!it.endsWith(".sandPrice")}+("sandPrice" to it);saved=false},"جنيه/م³")}}
            item{ChoiceFieldX("أسعار حاسبة",def.title,priced.map{it.title},{name->groupId=CalculatorLibrary.all.first{it.title==name}.id})}
            item{BoxCard{def.fields.filter{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}.forEach{f->val k="recipe.${def.id}.${f.key}";NumberFieldX(f.label,values[k]?:values[f.key]?.takeIf{f.key in setOf("cementPrice","sandPrice")}?:"0",{values=values+(k to it);saved=false},f.unit)}}}
            if(saved)item{Text("تم حفظ الأسعار ✓",color=MaterialTheme.colorScheme.primary)}
        }
    }}
}


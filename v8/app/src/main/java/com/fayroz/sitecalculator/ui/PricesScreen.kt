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
    var groupId by remember{mutableStateOf("plaster")}
    var error by remember{mutableStateOf<String?>(null)}
    var saved by remember{mutableStateOf(false)}
    var values by remember(projectId){mutableStateOf(project?.defaults.orEmpty())}
    val def=CalculatorLibrary.all.first{it.id==groupId}
    Scaffold(topBar={TopAppBar(title={Text("أسعار الخامات")},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})},bottomBar={if(project!=null)Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()){
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={if(values.filterKeys{it.contains("price",true)}.values.any{it.toDoubleOrNull()?.let{n->!n.isFinite()||n<0}!=false}){error="راجع الأسعار: اكتب أرقامًا صفر أو أكبر."}else{onSave(project.copy(defaults=values,updatedAt=System.currentTimeMillis()));saved=true;error=null}},modifier=Modifier.fillMaxWidth()){Text("حفظ أسعار المشروع")}
    }}}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(project==null)item{EmptyState("أنشئ مشروعًا أولًا","تُحفظ الأسعار لكل مشروع بشكل مستقل.")}
        else{
            item{ChoiceFieldX("المشروع",project.name,projects.map{it.name},{name->projectId=projects.first{it.name==name}.id;saved=false})}
            item{Text("الأسعار تُستخدم للمدخلات الجديدة. الحسابات والأجزاء ذات الأسعار الخاصة تحتفظ بقيمها.")}
            item{BoxCard{NumberFieldX("سعر شيكارة الأسمنت 50 كجم",values["cementPrice"]?:"0",{values=values+("cementPrice" to it);saved=false},"جنيه");NumberFieldX("سعر متر الرمل",values["sandPrice"]?:"0",{values=values+("sandPrice" to it);saved=false},"جنيه/م³")}}
            item{ChoiceFieldX("أسعار حاسبة",def.title,CalculatorLibrary.all.map{it.title},{name->groupId=CalculatorLibrary.all.first{it.title==name}.id})}
            item{BoxCard{def.fields.filter{it.key.contains("price",true)}.forEach{f->val k="recipe.${def.id}.${f.key}";NumberFieldX(f.label,values[k]?:"0",{values=values+(k to it);saved=false},f.unit)}}}
            if(saved)item{Text("تم حفظ الأسعار ✓",color=MaterialTheme.colorScheme.primary)}
        }
    }}
}

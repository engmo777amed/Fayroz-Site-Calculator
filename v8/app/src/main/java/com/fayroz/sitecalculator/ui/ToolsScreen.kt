package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.CalculatorLibrary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolDef(val id:String,val title:String,val subtitle:String,val icon:ImageVector)
val utilityTools=listOf(ToolDef("convert","تحويل الوحدات","طول ومساحة وحجم ووزن",Icons.Rounded.Calculate),ToolDef("area","تجميع مساحات","إضافة وخصم أجزاء",Icons.Rounded.Calculate))
val materialTools=CalculatorLibrary.all.map{ToolDef(it.id,it.title,it.group,Icons.Rounded.Calculate)}
@Composable
fun ToolsScreen(recent:List<SavedCalculation>,hasActiveProject:Boolean,onOpen:(String)->Unit,
    repository:V8Repository?=null,onOpenSaved:(SavedCalculation)->Unit={onOpen(it.toolId)}){
    var search by remember{mutableStateOf("")}
    var group by remember{mutableStateOf("الكل")}
    var favorites by remember{mutableStateOf(repository?.pref("favorites","")?.split('|')?.filter{it.isNotBlank()}?.toSet().orEmpty())}
    var hidden by remember{mutableStateOf(repository?.pref("hiddenGroups","")?.split('|')?.filter{it.isNotBlank()}?.toSet().orEmpty())}
    var settings by remember{mutableStateOf(false)}
    val filtered=CalculatorLibrary.all.filter{d->d.group !in hidden&&(group=="الكل"||group==d.group||(group=="المفضلة"&&d.id in favorites))&&(search.isBlank()||d.title.contains(search)||d.group.contains(search))}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{PageHeader("مكتبة الحاسبات","${CalculatorLibrary.all.size+2} حاسبة • حساب مستقل أو من المشروع")}
        item{TextFieldX("بحث",search,{search=it},placeholder="محارة، أسفلت، حديد…")}
        item{ChoiceFieldX("المجموعة",group,listOf("الكل","المفضلة")+CalculatorLibrary.groups.filter{it !in hidden},{group=it})}
        item{TextButton(onClick={settings=!settings}){Text("إظهار وإخفاء المجموعات")}}
        if(settings)item{BoxCard{CalculatorLibrary.groups.forEach{g->Row{
            Checkbox(g !in hidden,{yes->hidden=if(yes)hidden-g else hidden+g;repository?.setPref("hiddenGroups",hidden.joinToString("|"));group="الكل"});Text(g,Modifier.padding(top=12.dp))
        }}}}
        if(recent.isNotEmpty()&&search.isBlank()&&group=="الكل"){
            item{PageHeader("حسابات محفوظة")}
            items(recent.size){i->val c=recent[i];Surface(onClick={onOpenSaved(c)},tonalElevation=1.dp){Column(Modifier.fillMaxWidth().padding(10.dp)){Text(c.title,fontWeight=FontWeight.Bold);Text(c.summary,maxLines=2,style=MaterialTheme.typography.bodySmall);Text("${fmt(c.cost)} جنيه",style=MaterialTheme.typography.labelSmall)}}}
        }
        if(group=="الكل"&&search.isBlank())item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            utilityTools.forEach{d->OutlinedButton(onClick={onOpen(d.id)},modifier=Modifier.weight(1f)){Text(d.title)}}
        }}
        items(filtered.size){i->val d=filtered[i];Surface(onClick={onOpen(d.id)},tonalElevation=1.dp){Row(Modifier.fillMaxWidth().padding(10.dp)){
            Column(Modifier.weight(1f)){Text(d.title,fontWeight=FontWeight.Bold);Text(d.group,style=MaterialTheme.typography.bodySmall)}
            TextButton(onClick={favorites=if(d.id in favorites)favorites-d.id else favorites+d.id;repository?.setPref("favorites",favorites.joinToString("|"))}){Text(if(d.id in favorites)"★" else "☆")}
        }}}
        if(filtered.isEmpty())item{EmptyState("مفيش نتائج","غير البحث أو المجموعة.")}
    }
}

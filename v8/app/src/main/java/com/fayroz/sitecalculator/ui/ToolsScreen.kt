package com.fayroz.sitecalculator.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var search by rememberSaveable{mutableStateOf("")}
    var group by rememberSaveable{mutableStateOf<String?>(null)}
    var favorites by remember{mutableStateOf(repository?.pref("favorites","")?.split('|')?.filter{it.isNotBlank()}?.toSet().orEmpty())}
    BackHandler(enabled=group!=null||search.isNotBlank()){group=null;search=""}
    val filtered=CalculatorLibrary.all.filter{d->(group==null||group==d.group||(group=="المفضلة"&&d.id in favorites))&&(search.isBlank()||d.title.contains(search)||d.group.contains(search))}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{PageHeader(group?:"أقسام الحاسبات",if(group==null)"${CalculatorLibrary.all.size+2} حاسبة • اختار القسم ثم الحاسبة" else "الحاسبات / $group")}
        if(group!=null)item{TextButton(onClick={group=null;search=""}){Text("رجوع للأقسام")}}
        item{TextFieldX("بحث في الحاسبات",search,{search=it},placeholder="اسم الحاسبة أو نوع العمل")}
        if(group==null&&search.isBlank()){
            item{OutlinedButton(onClick={group="المفضلة"},modifier=Modifier.fillMaxWidth()){Text("المفضلة ★ (${favorites.size})")}}
            val sections=CalculatorLibrary.groups.chunked(2)
            items(sections.size){i->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                sections[i].forEach{g->Surface(onClick={group=g},modifier=Modifier.weight(1f).heightIn(min=116.dp),shape=RoundedCornerShape(18.dp),tonalElevation=2.dp){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text(g,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                    Text("${CalculatorLibrary.all.count{it.group==g}} حاسبة",style=MaterialTheme.typography.bodySmall)
                    Text("فتح القسم ←",color=MaterialTheme.colorScheme.primary)
                }}}
            }}
        }else{
            items(filtered.size){i->val d=filtered[i];Surface(onClick={onOpen(d.id)},shape=RoundedCornerShape(16.dp),tonalElevation=1.dp){Row(Modifier.fillMaxWidth().padding(14.dp)){
                Column(Modifier.weight(1f)){Text(d.title,fontWeight=FontWeight.Bold);Text(d.group,style=MaterialTheme.typography.bodySmall);Text("إدخال البيانات ← النتيجة",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
                TextButton(onClick={favorites=if(d.id in favorites)favorites-d.id else favorites+d.id;repository?.setPref("favorites",favorites.joinToString("|"))}){Text(if(d.id in favorites)"★" else "☆")}
            }}}
            if(filtered.isEmpty())item{EmptyState("مفيش نتائج","اختار قسمًا آخر أو غير البحث.")}
        }
        if((group==null&&search.isBlank())||group==CalculatorLibrary.groups[4]||search.isNotBlank()){
            utilityTools.filter{search.isBlank()||it.title.contains(search)}.forEach{d->item{OutlinedButton(onClick={onOpen(d.id)},modifier=Modifier.fillMaxWidth()){Text(d.title)}}}
        }
    }
}

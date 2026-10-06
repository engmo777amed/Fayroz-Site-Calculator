package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.fayroz.sitecalculator.core.SavedCalculation
@Composable
fun SavedCalculationsScreen(recent:List<SavedCalculation>,onDelete:(String)->Unit={},onOpen:(SavedCalculation)->Unit){
    var search by remember{mutableStateOf("")}
    var delete by remember{mutableStateOf<SavedCalculation?>(null)}
    val values=recent.filter{search.isBlank()||it.title.contains(search)||it.summary.contains(search)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{PageHeader("الحسابات المحفوظة","افتح الحساب لمراجعة مدخلاته أو تعديله")}
        item{TextFieldX("بحث",search,{search=it})}
        items(values.size){i->val c=values[i];ElevatedCard(onClick={onOpen(c)}){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(c.title,fontWeight=FontWeight.Bold);Text(c.summary.substringBefore(" • "),maxLines=2,style=MaterialTheme.typography.bodySmall);Text(dated(c.createdAt)+" • "+if(c.inputs["_includeInProject"]=="true")"مضاف لحصر المشروع"else "حساب مرجعي",style=MaterialTheme.typography.bodySmall)
            Row{TextButton(onClick={onOpen(c)},modifier=Modifier.weight(1f)){Text("فتح / تعديل")};TextButton(onClick={delete=c}){Text("حذف")}}
        }}}
        if(values.isEmpty())item{EmptyState("لا توجد حسابات محفوظة","احسب ثم اضغط حفظ النتيجة.")}
    }
    delete?.let{c->AlertDialog(onDismissRequest={delete=null},title={Text("حذف الحساب؟")},text={Text(c.title)},confirmButton={TextButton(onClick={onDelete(c.id);delete=null}){Text("حذف")}},dismissButton={TextButton(onClick={delete=null}){Text("إلغاء")}})}
}


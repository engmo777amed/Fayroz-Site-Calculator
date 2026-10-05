@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fayroz.sitecalculator.ui
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository

private data class AreaPart(val length:String="",val width:String="",val deduct:Boolean=false)
@Composable
fun SiteUtilityScreen(toolId:String,onBack:()->Unit,repository:V8Repository?=null,seed:MaterialResult?=null){
    val context=LocalContext.current
    val saved=seed?.inputs.orEmpty()
    var stage by remember{mutableIntStateOf(if(seed!=null)1 else 0)}
    var error by remember{mutableStateOf<String?>(null)}
    var label by remember{mutableStateOf(seed?.title?:if(toolId=="convert")"تحويل الوحدات" else "تجميع مساحات")}
    var value by remember{mutableStateOf(saved["value"].orEmpty())}
    var type by remember{mutableStateOf(saved["type"]?:"طول")}
    var unit by remember{mutableStateOf(saved["unit"]?:"م")}
    val parts=remember{mutableStateListOf<AreaPart>().apply{repeat(saved["parts"]?.toIntOrNull()?:1){i->add(AreaPart(saved["length.$i"].orEmpty(),saved["width.$i"].orEmpty(),saved["deduct.$i"]=="true"))}}}
    val total=parts.sumOf{val a=lengthMeters(it.length,unit)*lengthMeters(it.width,unit);if(it.deduct)-a else a}
    val inputs=buildMap{put("_label",label);put("value",value);put("type",type);put("unit",unit);put("parts",parts.size.toString());parts.forEachIndexed{i,p->put("length.$i",p.length);put("width.$i",p.width);put("deduct.$i",p.deduct.toString())}}
    val lines=if(toolId=="area")listOf(MaterialLine("المساحة الصافية","${fmt(total)} م²"))else buildList{
        val x=n(value)
        when(type){
            "طول"->{val m=when(unit){"سم"->x/100;"مم"->x/1000;else->x};add(MaterialLine("متر","${fmt(m)} م"));add(MaterialLine("سنتيمتر","${fmt(m*100)} سم"));add(MaterialLine("مليمتر","${fmt(m*1000)} مم"))}
            "مساحة"->{val a=if(unit=="سم²")x/10000 else x;add(MaterialLine("متر مربع","${fmt(a)} م²"));add(MaterialLine("سنتيمتر مربع","${fmt(a*10000)} سم²"))}
            "وزن"->{val kg=when(unit){"طن"->x*1000;"جرام"->x/1000;else->x};add(MaterialLine("كيلوجرام","${fmt(kg)} كجم"));add(MaterialLine("طن","${fmt(kg/1000)} طن"));add(MaterialLine("جرام","${fmt(kg*1000)} جرام"))}
            else->{val volume=if(unit=="لتر")x/1000 else x;add(MaterialLine("متر مكعب","${fmt(volume)} م³"));add(MaterialLine("لتر","${fmt(volume*1000)} لتر"))}
        }
    }
    fun calculate(){error=when{
        toolId=="convert"&&(value.isBlank()||n(value)<0||!(n(value)*10000).isFinite())->"أدخل قيمة صحيحة صفر أو أكبر."
        toolId=="area"&&(parts.isEmpty()||parts.any{n(it.length)<=0||n(it.width)<=0})->"أدخل طولًا وعرضًا أكبر من صفر لكل جزء."
        toolId=="area"&&(!total.isFinite()||total<0)->"الخصومات أكبر من المساحات المضافة."
        else->null
    };if(error==null)stage=1}
    BackHandler{if(stage==1)stage=0 else onBack()}
    Scaffold(topBar={TopAppBar(title={Text(label)},navigationIcon={TextButton(onClick={if(stage==1)stage=0 else onBack()}){Text("رجوع")}})},bottomBar={Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding().imePadding()){
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            if(stage==1)OutlinedButton(onClick={stage=0},modifier=Modifier.weight(1f)){Text("تعديل المدخلات")}
            Button(onClick={if(stage==0)calculate()else{repository?.saveRecentCalc(SavedCalculation(toolId=toolId,title=label,summary=lines.joinToString(" • "){"${it.label}: ${it.value}"},sourceQuantity=if(toolId=="area")total else n(value),unit=if(toolId=="area")"م²" else unit,inputs=inputs,explanation=if(toolId=="area")"جمع المساحات المضافة وطرح أجزاء الخصم." else "تحويل الوحدات ضمن نفس النوع."));Toast.makeText(context,"تم حفظ النتيجة ✓",Toast.LENGTH_SHORT).show()}},modifier=Modifier.weight(1f)){Text(if(stage==0)"احسب واعرض النتيجة" else "حفظ النتيجة")}
        }
    }}}){padding->Column(Modifier.fillMaxSize().padding(padding)){
        StageNavigation(stage,listOf("إدخال البيانات","النتيجة")){if(it==0)stage=0 else calculate()}
        key(stage){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(stage==0){
                item{ExpandableSection("اسم الحساب — اختياري"){TextFieldX("اسم الحساب",label,{label=it})}}
                if(toolId=="convert")item{BoxCard{
                    ChoiceFieldX("نوع التحويل",type,listOf("طول","مساحة","حجم","وزن"),{type=it;unit=when(it){"طول"->"م";"مساحة"->"م²";"وزن"->"كجم";else->"م³"}})
                    ChoiceFieldX("الوحدة",unit,when(type){"طول"->listOf("م","سم","مم");"مساحة"->listOf("م²","سم²");"وزن"->listOf("كجم","طن","جرام");else->listOf("م³","لتر")},{unit=it})
                    NumberFieldX("القيمة",value,{value=it;error=null},unit)
                }}else{
                    item{ChoiceFieldX("وحدة الأبعاد",unit,listOf("م","سم","مم"),{next->parts.indices.forEach{i->val p=parts[i];parts[i]=p.copy(length=convertLength(p.length,unit,next),width=convertLength(p.width,unit,next))};unit=next})}
                    items(parts.size){i->val part=parts[i];BoxCard{
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        NumberFieldX("طول ${i+1}",part.length,{parts[i]=part.copy(length=it);error=null},unit,Modifier.weight(1f))
                        NumberFieldX("عرض ${i+1}",part.width,{parts[i]=part.copy(width=it);error=null},unit,Modifier.weight(1f))
                        }
                        Row{Checkbox(part.deduct,{parts[i]=part.copy(deduct=it)});Text("خصم هذا الجزء",Modifier.weight(1f).padding(top=12.dp));TextButton(onClick={parts.removeAt(i)}){Text("حذف")}}
                    }}
                    item{OutlinedButton(onClick={parts.add(AreaPart())},modifier=Modifier.fillMaxWidth()){Text("إضافة جزء")}}
                }
            }else{
                item{BoxCard{Text("ملخص الحساب");Text(label);Text("حساب مستقل");lines.forEach{MetricRow(it.label,it.value,true)}}}
                if(toolId=="area")item{BoxCard{Text("الأجزاء والخصومات");parts.forEachIndexed{i,p->MetricRow("جزء ${i+1}"+(if(p.deduct)" — خصم" else " — إضافة"),"${fmt(lengthMeters(p.length,unit)*lengthMeters(p.width,unit))} م²")}}}
                item{OutlinedButton(onClick={context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,label+"\n"+lines.joinToString("\n"){"${it.label}: ${it.value}"})},"مشاركة النتيجة"))},modifier=Modifier.fillMaxWidth()){Text("تصدير / مشاركة النتيجة")}}
            }
        }}
    }}
}


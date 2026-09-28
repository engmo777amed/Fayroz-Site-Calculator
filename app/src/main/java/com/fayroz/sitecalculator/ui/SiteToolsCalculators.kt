package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.ProjectRepository
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun SlopeLevelsScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="slope_levels"
    var mode by remember{mutableStateOf("معايا الميل")}
    var length by remember{mutableStateOf(repository.getToolValue("$toolId.length",""))}
    var slope by remember{mutableStateOf(repository.getToolValue("$toolId.slope","1"))}
    var startLevel by remember{mutableStateOf(repository.getToolValue("$toolId.startLevel","0.00"))}
    var endLevel by remember{mutableStateOf(repository.getToolValue("$toolId.endLevel",""))}
    var direction by remember{mutableStateOf("نازل")}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}

    val l=n(length)
    val diff=if(mode=="معايا الميل")l*n(slope)/100.0 else abs(n(endLevel)-n(startLevel))
    val calculatedSlope=if(l>0)diff/l*100.0 else 0.0
    val calculatedEnd=if(direction=="نازل")n(startLevel)-diff else n(startLevel)+diff

    ToolPage("الميل والمناسيب","فرق منسوب أو نسبة ميل في ثواني",repository,toolId,onBack){
        CardBox{
            ChoiceFieldX("معاك إيه؟",mode,listOf("معايا الميل","معايا المنسوبين"),{mode=it})
            NumberFieldX("طول المسار",length,{length=it;set("length",it)},"م","المسافة الأفقية اللي الميل ماشي عليها.")
            NumberFieldX("منسوب البداية",startLevel,{startLevel=it;set("startLevel",it)},"م")
            if(mode=="معايا الميل"){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("الميل",slope,{slope=it;set("slope",it)},"%",modifier=Modifier.weight(1f))
                    ChoiceFieldX("الاتجاه",direction,listOf("نازل","طالع"),{direction=it},modifier=Modifier.weight(1f))
                }
            }else{
                NumberFieldX("منسوب النهاية",endLevel,{endLevel=it;set("endLevel",it)},"م")
            }
        }
        if(l>0){
            val finalSlope=if(mode=="معايا الميل")n(slope) else calculatedSlope
            val finalEnd=if(mode=="معايا الميل")calculatedEnd else n(endLevel)
            ToolResultCard(
                rows=listOf(
                    "فرق المنسوب" to "${fmt(diff)} م = ${fmt(diff*100)} سم",
                    "الميل" to "${fmt(finalSlope)} %",
                    "منسوب النهاية" to "${fmt(finalEnd)} م"
                ),
                explanation=if(mode=="معايا الميل")
                    "فرق المنسوب = طول المسار × الميل ÷ 100 = ${fmt(l)} × ${fmt(n(slope))}% = ${fmt(diff)} م."
                else
                    "فرق المنسوب = الفرق بين منسوب البداية والنهاية = ${fmt(diff)} م. الميل = فرق المنسوب ÷ طول المسار × 100 = ${fmt(calculatedSlope)}%.",
                copyText="طول ${fmt(l)} م — فرق منسوب ${fmt(diff*100)} سم — ميل ${fmt(finalSlope)}% — نهاية ${fmt(finalEnd)} م"
            )
        }
    }
}

private data class AreaPart(
    val id:String=UUID.randomUUID().toString(),
    val type:String="مستطيل",
    val sign:String="إضافة",
    val a:String="",
    val b:String=""
)

@Composable
fun IrregularAreaScreen(onBack:()->Unit){
    val parts=remember{mutableStateListOf(AreaPart())}
    val values=parts.map{p->
        val raw=if(p.type=="مثلث")n(p.a)*n(p.b)/2.0 else n(p.a)*n(p.b)
        if(p.sign=="خصم")-raw else raw
    }
    val total=values.sum()

    Scaffold(topBar={AppBarX("مساحة غير منتظمة","قسم المكان لأجزاء بسيطة",onBack)}){
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(it).imePadding(),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    SectionTitle("الأجزاء","ضيف مستطيلات أو مثلثات، واعمل خصم لأي فراغ أو عمود.")
                    parts.forEachIndexed{index,p->
                        Surface(
                            shape=RoundedCornerShape(13.dp),
                            color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f),
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Text("جزء ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                                    Text("${fmt(abs(values[index]))} م²",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                    if(parts.size>1)IconButton(onClick={parts.removeAt(index)},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف")}
                                }
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                    ChoiceFieldX("الشكل",p.type,listOf("مستطيل","مثلث"),{v->parts[index]=p.copy(type=v)},modifier=Modifier.weight(1f))
                                    ChoiceFieldX("يتحسب",p.sign,listOf("إضافة","خصم"),{v->parts[index]=p.copy(sign=v)},modifier=Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                    NumberFieldX(if(p.type=="مثلث")"القاعدة" else "الطول",p.a,{v->parts[index]=p.copy(a=v)},"م",modifier=Modifier.weight(1f))
                                    NumberFieldX(if(p.type=="مثلث")"الارتفاع" else "العرض",p.b,{v->parts[index]=p.copy(b=v)},"م",modifier=Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick={parts.add(AreaPart())},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                        Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("إضافة جزء")
                    }
                }
            }
            item{
                ToolResultCard(
                    rows=listOf("المساحة الصافية" to "${fmt(total.coerceAtLeast(0.0))} م²"),
                    explanation="المستطيل = الطول × العرض، والمثلث = القاعدة × الارتفاع ÷ 2. الأجزاء اللي اخترت لها «خصم» اتخصمت من الإجمالي.",
                    copyText="المساحة غير المنتظمة = ${fmt(total.coerceAtLeast(0.0))} م²"
                )
            }
        }
    }
}

@Composable
fun RepetitionScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="repetitions"
    var quantity by remember{mutableStateOf(repository.getToolValue("$toolId.quantity",""))}
    var unit by remember{mutableStateOf(repository.getToolValue("$toolId.unit","م²"))}
    var unitsPerFloor by remember{mutableStateOf(repository.getToolValue("$toolId.unitsPerFloor","1"))}
    var floors by remember{mutableStateOf(repository.getToolValue("$toolId.floors","1"))}
    var extra by remember{mutableStateOf(repository.getToolValue("$toolId.extra","0"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val total=n(quantity)*n(unitsPerFloor).toInt().coerceAtLeast(0)*n(floors).toInt().coerceAtLeast(0)+n(extra)

    ToolPage("تكرار الكميات","غرفة أو شقة نموذجية × عدد الأدوار",repository,toolId,onBack){
        CardBox{
            NumberFieldX("كمية الوحدة الواحدة",quantity,{quantity=it;set("quantity",it)},unit,"مثال: سيراميك حمام واحد 24 م².")
            ChoiceFieldX("الوحدة",unit,listOf("م²","م ط","م³","عدد"),{unit=it;set("unit",it)})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("وحدات في الدور",unitsPerFloor,{unitsPerFloor=it;set("unitsPerFloor",it)},"عدد",modifier=Modifier.weight(1f))
                NumberFieldX("عدد الأدوار",floors,{floors=it;set("floors",it)},"دور",modifier=Modifier.weight(1f))
            }
            NumberFieldX("كمية إضافية",extra,{extra=it;set("extra",it)},unit,"أي كمية مش داخلة في التكرار.")
        }
        if(n(quantity)>0){
            ToolResultCard(
                rows=listOf("الإجمالي" to "${fmt(total)} $unit"),
                explanation="الإجمالي = كمية الوحدة × عدد الوحدات في الدور × عدد الأدوار + الكمية الإضافية.",
                copyText="كمية متكررة — ${fmt(n(quantity))} $unit × $unitsPerFloor × $floors + ${fmt(n(extra))} = ${fmt(total)} $unit"
            )
        }
    }
}

@Composable
fun ProgressProductivityScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="progress_productivity"
    var totalQty by remember{mutableStateOf(repository.getToolValue("$toolId.total",""))}
    var doneQty by remember{mutableStateOf(repository.getToolValue("$toolId.done",""))}
    var daily by remember{mutableStateOf(repository.getToolValue("$toolId.daily",""))}
    var workers by remember{mutableStateOf(repository.getToolValue("$toolId.workers","1"))}
    var unit by remember{mutableStateOf(repository.getToolValue("$toolId.unit","م²"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val total=n(totalQty)
    val done=n(doneQty)
    val remain=(total-done).coerceAtLeast(0.0)
    val progress=if(total>0)(done/total*100.0).coerceIn(0.0,100.0) else 0.0
    val days=if(n(daily)>0)ceil(remain/n(daily)).toInt() else 0
    val perWorker=if(n(workers)>0)n(daily)/n(workers) else 0.0

    ToolPage("الإنجاز والإنتاجية","اعرف المتبقي والمدة وإنتاجية العامل",repository,toolId,onBack){
        CardBox{
            ChoiceFieldX("الوحدة",unit,listOf("م²","م ط","م³","عدد"),{unit=it;set("unit",it)})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("الكمية الكلية",totalQty,{totalQty=it;set("total",it)},unit,modifier=Modifier.weight(1f))
                NumberFieldX("المنفذ",doneQty,{doneQty=it;set("done",it)},unit,modifier=Modifier.weight(1f))
            }
            NumberFieldX("إنتاج الطاقم في اليوم",daily,{daily=it;set("daily",it)},"$unit/يوم","متوسط فعلي أو مستهدف.")
            NumberFieldX("عدد العمال",workers,{workers=it;set("workers",it)},"عامل","لو عايز تعرف إنتاجية العامل.")
        }
        if(total>0){
            ToolResultCard(
                rows=listOf(
                    "نسبة الإنجاز" to "${fmt(progress)} %",
                    "المتبقي" to "${fmt(remain)} $unit",
                    "المدة المتوقعة للمتبقي" to if(n(daily)>0)"$days يوم" else "اكتب إنتاج اليوم",
                    "إنتاجية العامل" to if(n(workers)>0 && n(daily)>0)"${fmt(perWorker)} $unit/عامل/يوم" else "-"
                ),
                explanation="نسبة الإنجاز = المنفذ ÷ الإجمالي × 100.\nالمتبقي = الإجمالي - المنفذ.\nالمدة = المتبقي ÷ إنتاج الطاقم اليومي مع التقريب لأعلى.",
                copyText="إنجاز ${fmt(progress)}% — متبقي ${fmt(remain)} $unit"+if(n(daily)>0)" — مدة تقريبية $days يوم" else ""
            )
        }
    }
}

@Composable
fun UnitConversionScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="unit_conversion"
    var type by remember{mutableStateOf("طول")}
    var value by remember{mutableStateOf(repository.getToolValue("$toolId.value",""))}
    var inputUnit by remember{mutableStateOf("م")}
    fun set(v:String){repository.setToolValue("$toolId.value",v)}
    val x=n(value)

    val rows=when(type){
        "طول"->{
            val meters=when(inputUnit){"مم"->x/1000;"سم"->x/100;else->x}
            listOf("متر" to "${fmt(meters)} م","سنتيمتر" to "${fmt(meters*100)} سم","مليمتر" to "${fmt(meters*1000)} مم")
        }
        "مساحة"->{
            val m2=when(inputUnit){"سم²"->x/10000;else->x}
            listOf("متر مربع" to "${fmt(m2)} م²","سنتيمتر مربع" to "${fmt(m2*10000)} سم²")
        }
        else->{
            val m3=when(inputUnit){"لتر"->x/1000;else->x}
            listOf("متر مكعب" to "${fmt(m3)} م³","لتر" to "${fmt(m3*1000)} لتر")
        }
    }

    ToolPage("تحويل الوحدات","طول ومساحة وحجم من غير آلة حاسبة",repository,toolId,onBack){
        CardBox{
            ChoiceFieldX("نوع التحويل",type,listOf("طول","مساحة","حجم"),{
                type=it
                inputUnit=when(it){"طول"->"م";"مساحة"->"م²";else->"م³"}
            })
            val units=when(type){"طول"->listOf("م","سم","مم");"مساحة"->listOf("م²","سم²");else->listOf("م³","لتر")}
            ChoiceFieldX("الوحدة اللي معاك",inputUnit,units,{inputUnit=it})
            NumberFieldX("القيمة",value,{value=it;set(it)},inputUnit)
        }
        if(value.isNotBlank()){
            ToolResultCard(
                rows=rows,
                explanation="البرنامج بيحوّل لنفس الوحدة الأساسية الأول، وبعدها يطلع باقي الوحدات.",
                copyText=rows.joinToString(" — "){"${it.first}: ${it.second}"}
            )
        }
    }
}

package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.fayroz.sitecalculator.domain.*
@Composable
fun CostSummary(material:Double,labor:Double=0.0,transport:Double=0.0,equipment:Double=0.0,missing:List<String> = emptyList()){
    BoxCard{
        Text("التكلفة",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        if(missing.isNotEmpty()){
            Text("التكلفة غير مكتملة",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)
            Text("أدخل: "+missing.distinct().joinToString("، "))
            MetricRow("تكلفة المواد المسعرة فقط","${fmt(material)} جنيه")
        }else MetricRow("تكلفة المواد","${fmt(material)} جنيه")
        MetricRow("المصنعية","${fmt(labor)} جنيه");MetricRow("النقل","${fmt(transport)} جنيه");MetricRow("المعدات","${fmt(equipment)} جنيه")
        MetricRow(if(missing.isEmpty())"إجمالي التكلفة" else "إجمالي جزئي","${fmt(material+labor+transport+equipment)} جنيه",true)
    }
}
@Composable
fun WorkResultCards(rows:List<CostEngine.Row>,scope:String){
    BoxCard{Text("ملخص الحساب",fontWeight=FontWeight.Bold);Text(scope);rows.forEach{MetricRow(it.part,"${fmt(it.quantity)} ${it.unit}")}}
    rows.forEach{row->BoxCard{
        Text("خامات ${row.part}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        row.spec?.let{s->
            MetricRow("الأسمنت الفعلي","${fmt(row.cementKg)} كجم")
            MetricRow("ما يعادل شكاير","${fmt(row.cementKg/s.bagKg)} شيكارة")
            MetricRow("تكلفة الأسمنت",if(s.cementPrice>0)"${fmt(row.cementKg/s.bagKg*s.cementPrice)} جنيه" else "السعر لم يُدخل")
            MetricRow("الرمل","${fmt(row.sandM3)} م³")
            MetricRow("تكلفة الرمل",if(s.sandPrice>0)"${fmt(row.sandM3*s.sandPrice)} جنيه" else "السعر لم يُدخل")
            if(row.extra>0){MetricRow(s.extraName,fmt(row.extra));MetricRow("تكلفة الإضافة",if(s.extraPrice>0)"${fmt(row.extra*s.extraPrice)} جنيه" else "السعر لم يُدخل")}
        }
        row.materialLines.filterNot{it.value.endsWith("جنيه")}.forEach{MetricRow(it.label,it.value)}
        if(row.spec==null&&row.materialLines.isEmpty())Text("هذا البند له حصر كمية فقط؛ لا توجد وصفة خامات مرتبطة به.")
    }}
    val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
    CostSummary(rows.sumOf{it.materialCost},rows.sumOf{it.labor},rows.sumOf{it.transport},rows.sumOf{it.equipment},missing)
    if(rows.sumOf{it.quantity}>0&&rows.map{it.unit}.distinct().size==1&&missing.isEmpty())BoxCard{MetricRow("تكلفة الوحدة شامل التكاليف","${fmt(rows.sumOf{it.total}/rows.sumOf{it.quantity})} جنيه/${rows.first().unit}",true)}
    PurchaseCards(CostEngine.purchase(rows),missing.isEmpty())
    BoxCard{var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Text("تفاصيل الحساب والمدخلات")};if(expanded)rows.forEach{Text("${it.part}: ${it.formula}")}}
}
@Composable
fun PurchaseCards(purchases:List<CostEngine.Purchase>,complete:Boolean){
    BoxCard{
        Text("قائمة الشراء",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text("تجميع الأجزاء المتطابقة ثم تقريب الشكاير والعبوات مرة واحدة.",style=MaterialTheme.typography.bodySmall)
        purchases.forEach{p->
            HorizontalDivider();Text(p.material,fontWeight=FontWeight.Bold)
            MetricRow("الكمية",if(p.packages!=null)"${p.packages} عبوة • احتياج ${fmt(p.amount)} ${p.unit}" else "${fmt(p.amount)} ${p.unit}")
            if(p.price>0||p.cost>0)MetricRow("تكلفة الشراء","${fmt(p.cost)} جنيه")
        }
        MetricRow(if(complete)"إجمالي الشراء" else "تكلفة شراء جزئية","${fmt(purchases.sumOf{it.cost})} جنيه",true)
    }
}
@Composable
fun CalculatorResultCards(def:CalcDef,inputs:Map<String,String>,answer:CalcAnswer,scope:String){
    val missing=MaterialReview.missing(def,inputs)
    BoxCard{Text("ملخص الحساب",fontWeight=FontWeight.Bold);Text(scope);Text(def.title);def.fields.filter{it.key in setOf("area","length","count")}.forEach{MetricRow(it.label,"${inputs[it.key]} ${it.unit}")}}
    BoxCard{Text("الكميات والمواد",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);answer.outputs.filter{it.unit!="جنيه"&&it.unit!="جنيه/م²"}.forEach{MetricRow(it.label,"${fmt(it.value)} ${it.unit}")}}
    if(def.fields.any{it.key.contains("price",true)}){
        CostSummary(answer.consumedCost,missing=missing)
        BoxCard{Text("الشراء بعد تقريب العبوات",fontWeight=FontWeight.Bold);MetricRow(if(missing.isEmpty())"تكلفة الشراء" else "تكلفة شراء جزئية","${fmt(answer.cost)} جنيه",true);Text("هذه نتيجة حساب مستقل. شراء المشروع يجمع الأجزاء المتطابقة قبل التقريب.",style=MaterialTheme.typography.bodySmall)}
    }
    BoxCard{var explain by remember{mutableStateOf(false)};TextButton(onClick={explain=!explain}){Text("المدخلات وطريقة الحساب")};if(explain){def.fields.forEach{MetricRow(it.label,"${inputs[it.key]} ${it.unit}")};Text(def.formula);Text(answer.explanation)}}
}

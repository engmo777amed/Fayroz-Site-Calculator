package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fayroz.sitecalculator.domain.*

@Composable
fun CostSummary(material:Double,labor:Double=0.0,transport:Double=0.0,equipment:Double=0.0,missing:List<String> = emptyList()){
    BoxCard{
        Text("تكلفة الاستهلاك",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        if(missing.isNotEmpty()){
            Text("التكلفة غير مكتملة",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)
            Text("غير مسعر: "+missing.distinct().joinToString("، "),style=MaterialTheme.typography.bodySmall)
        }
        MetricRow(if(missing.isEmpty())"الخامات" else "الخامات المسعرة فقط","${fmt(material)} جنيه")
        if(labor>0)MetricRow("المصنعية","${fmt(labor)} جنيه")
        if(transport>0)MetricRow("النقل","${fmt(transport)} جنيه")
        if(equipment>0)MetricRow("المعدات","${fmt(equipment)} جنيه")
        MetricRow(if(missing.isEmpty())"إجمالي المدخل" else "إجمالي جزئي","${fmt(material+labor+transport+equipment)} جنيه",true)
        if(labor==0.0&&transport==0.0&&equipment==0.0)Text("المصنعية والنقل والمعدات لم تُضف.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun ResultTabs(selected:Int,labels:List<String>,onSelect:(Int)->Unit){
    TabRow(selectedTabIndex=selected){labels.forEachIndexed{i,label->Tab(selected=selected==i,onClick={onSelect(i)},text={Text(label,maxLines=1,style=MaterialTheme.typography.labelMedium)})}}
}
@Composable
fun WorkResultCards(rows:List<CostEngine.Row>,scope:String){
    var view by remember{mutableIntStateOf(0)}
    val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
    BoxCard{
        Text(scope,style=MaterialTheme.typography.bodySmall)
        rows.groupBy{it.unit}.forEach{(unit,group)->MetricRow("صافي التنفيذ","${fmt(group.sumOf{it.quantity})} $unit",true)}
    }
    ResultTabs(view,listOf("الخامات","الشراء","التكلفة")){view=it}
    when(view){
        0->rows.forEach{row->BoxCard{
            Text(if(rows.size>1)row.part else "الخامات المطلوبة",fontWeight=FontWeight.Bold)
            row.spec?.let{s->
                MetricRow("رمل","${fmt(row.sandM3)} م³",true)
                MetricRow("أسمنت فعلي","${fmt(row.cementKg/s.bagKg)} شيكارة × ${fmt(s.bagKg)} كجم",true)
                if(row.extra>0)MetricRow(s.extraName,fmt(row.extra))
                ExpandableSection("وزن الأسمنت وتفاصيل الجزء"){
                    MetricRow("وزن الأسمنت","${fmt(row.cementKg)} كجم")
                    MetricRow("مساحة الجزء","${fmt(row.quantity)} ${row.unit}")
                    Text("الشكاير الكاملة تظهر في الشراء بعد تجميع الأجزاء.",style=MaterialTheme.typography.bodySmall)
                }
            }
            row.materialLines.filterNot{it.value.endsWith("جنيه")||it.value.startsWith("0 ")}.forEach{MetricRow(it.label,it.value)}
            if(row.spec==null&&row.materialLines.isEmpty())Text("هذا البند حصر كمية فقط؛ لم تُربط به خامات.")
        }}
        1->PurchaseCards(CostEngine.purchase(rows),missing.isEmpty())
        2->CostSummary(rows.sumOf{it.materialCost},rows.sumOf{it.labor},rows.sumOf{it.transport},rows.sumOf{it.equipment},missing)
    }
    BoxCard{ExpandableSection("مصدر الكمية وطريقة الحساب"){
        rows.forEach{MetricRow(it.part,"${fmt(it.quantity)} ${it.unit}");Text(it.formula,style=MaterialTheme.typography.bodySmall)}
    }}
}
@Composable
fun PurchaseCards(purchases:List<CostEngine.Purchase>,complete:Boolean){
    BoxCard{
        Text("طلب الخامات",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        if(purchases.isEmpty())Text("لا توجد خامات شراء محسوبة في هذا النطاق.")
        purchases.forEach{p->
            HorizontalDivider()
            MetricRow(p.material,if(p.packages!=null)"${p.packages} ${p.packageUnit}" else "${fmt(p.amount)} ${p.unit}",true)
            if(p.packages!=null)Text("احتياج فعلي: ${fmt(p.amount)} ${p.unit}",style=MaterialTheme.typography.bodySmall)
            if(p.price>0||p.cost>0)MetricRow("تكلفة الشراء","${fmt(p.cost)} جنيه")else Text("السعر غير مدخل",style=MaterialTheme.typography.bodySmall)
        }
        if(purchases.isNotEmpty()){
            MetricRow(if(complete)"إجمالي الشراء" else "تكلفة شراء جزئية","${fmt(purchases.sumOf{it.cost})} جنيه")
            Text("تم تجميع الخامات المتطابقة وتقريب الشكاير والعبوات مرة واحدة.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun CalculatorResultCards(def:CalcDef,inputs:Map<String,String>,answer:CalcAnswer,scope:String){
    var view by remember{mutableIntStateOf(0)}
    val missing=MaterialReview.missing(def,inputs)
    val lines=answer.outputs.filter{it.unit !in setOf("جنيه","جنيه/م²")&&!(it.value==0.0&&it.label in setOf("مادة إضافية","وزن عند الكثافة المدخلة","فائض التعبئة"))}
    val mortar=def.id in CalculatorLibrary.mortarIds
    BoxCard{
        Text(scope,style=MaterialTheme.typography.bodySmall)
        val main=if(mortar)lines.firstOrNull{it.label=="مونة منفذة"}else
            lines.firstOrNull{it.label.contains("شراء")&&!it.label.contains("فائض")}?:
            lines.firstOrNull{it.label.contains("توريد")}?:
            lines.firstOrNull{it.label in setOf("استهلاك دهان","كمية فعلية","عبوات","عبوات كاملة","ألواح","منسوب النهاية","الميل الموجّه","وزن","صافي الكسوة","المساحة","سعة")}?:
            lines.firstOrNull{it.label.contains("صافي")}?:lines.firstOrNull{!it.unit.contains("/")}?:lines.firstOrNull()
        main?.let{MetricRow(it.label,"${fmt(it.value)} ${it.unit}",true)}
        if(mortar){
            MetricRow("الرمل المطلوب","${fmt(lines.first{it.label=="رمل"}.value)} م³",true)
            val kg=lines.first{it.label=="أسمنت فعلي"}.value
            MetricRow("الأسمنت الفعلي","${fmt(kg/n(inputs["bag"].orEmpty()))} شيكارة × ${inputs["bag"]} كجم",true)
            MetricRow("تغطي عند سمك ${inputs["thickness"]} مم","${fmt(CalculatorLibrary.mortarArea(inputs))} م²")
            if(def.id=="splash")Text("تغطية الطرطشة تقدير حجمي؛ راجع معدل الاستهلاك الفعلي بالموقع.",style=MaterialTheme.typography.bodySmall)
        }else def.fields.filter{it.key in setOf("area","length","count")}.forEach{MetricRow(it.label,"${inputs[it.key]} ${it.unit}")}
    }
    val priced=def.fields.any{it.key.contains("price",true)}
    ResultTabs(view,if(priced)listOf("التفاصيل","الشراء","التكلفة")else listOf("التفاصيل","طريقة الحساب")){view=it}
    when{
        view==0->BoxCard{
            Text("نتائج الحساب",fontWeight=FontWeight.Bold)
            lines.forEach{MetricRow(it.label,"${fmt(it.value)} ${it.unit}")}
        }
        view==1&&priced->{
            val purchase=CostEngine.calculatorPurchases(def,inputs,answer)
            if(purchase.isNotEmpty())PurchaseCards(purchase,missing.isEmpty())else BoxCard{
                Text("الكمية المطلوبة / التوريد",fontWeight=FontWeight.Bold)
                lines.filter{it.label.contains("شراء")||it.label.contains("توريد")||it.unit in setOf("عبوة","لفة","لوح","ماسورة")}.forEach{MetricRow(it.label,"${fmt(it.value)} ${it.unit}")}
                MetricRow(if(missing.isEmpty())"تكلفة الشراء" else "تكلفة شراء جزئية","${fmt(answer.cost)} جنيه",true)
                if(missing.isNotEmpty())Text("غير مسعر: "+missing.joinToString("، "))
            }
        }
        view==2&&priced->{CostSummary(answer.consumedCost,missing=missing);BoxCard{MetricRow("شراء بعد التقريب","${fmt(answer.cost)} جنيه");Text("قد تزيد تكلفة الشراء عن الاستهلاك بسبب العبوات الكاملة.",style=MaterialTheme.typography.bodySmall)}}
        else->BoxCard{Text(def.formula);Text(answer.explanation)}
    }
    BoxCard{ExpandableSection("المدخلات والخلطة وطريقة الحساب"){
        def.fields.forEach{MetricRow(it.label,if(mortar&&it.key=="area")"${fmt(CalculatorLibrary.mortarArea(inputs))} م²" else "${inputs[it.key]} ${it.unit}")}
        Text(def.formula);Text(answer.explanation)
    }}
}

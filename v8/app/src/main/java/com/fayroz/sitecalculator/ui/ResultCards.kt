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
        MetricRow(if(missing.isEmpty())"الخامات" else "الخامات المسعرة فقط","${money(material)} جنيه")
        MetricRow("المصنعية",if(labor>0)"${money(labor)} جنيه"else "لم تُضف / صفر")
        MetricRow("النقل",if(transport>0)"${money(transport)} جنيه"else "لم يُضف / صفر")
        MetricRow("المعدات",if(equipment>0)"${money(equipment)} جنيه"else "لم تُضف / صفر")
        MetricRow(if(missing.isEmpty())"التكلفة المباشرة المدخلة" else "إجمالي جزئي","${money(material+labor+transport+equipment)} جنيه",true)
        Text("التكلفة المباشرة قبل المصاريف العامة والربح والضريبة.",style=MaterialTheme.typography.bodySmall)
    }
}
@Composable
fun WorkResultCards(rows:List<CostEngine.Row>,scope:String,showQuantity:Boolean=true){
    val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
    val stock=CostEngine.purchase(rows)
    BoxCard{
        Text("المطلوب للتنفيذ",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
        if(showQuantity)rows.groupBy{it.unit}.forEach{(unit,group)->MetricRow("صافي التنفيذ","${fmt(group.sumOf{it.quantity})} $unit")}
        stock.forEach{p->
            MetricRow(p.material,if(p.packages!=null)"${p.packages} ${p.packageUnit}"else "${fmt(p.amount)} ${p.unit}",true)
            if(p.packages!=null)Text("استهلاك فعلي: ${displayAmount(p.amount,p.unit)} ${p.unit}",style=MaterialTheme.typography.labelSmall)
        }
        if(stock.isEmpty())Text(if(rows.any{it.issue!=null})"راجع بيانات الحساب"else "حصر كمية فقط",style=MaterialTheme.typography.bodySmall)
        if(missing.isNotEmpty()){Text("التكلفة غير مكتملة",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);Text(missing.joinToString("، "),style=MaterialTheme.typography.labelSmall)}
        if(rows.sumOf{it.total}>0){
            HorizontalDivider()
            MetricRow(if(missing.isEmpty())"تكلفة التنفيذ"else "تكلفة جزئية","${money(rows.sumOf{it.total})} جنيه",true)
            MetricRow("شراء الخامات بعد التقريب","${money(stock.sumOf{it.cost})} جنيه")
        }
        ExpandableSection("تفاصيل التكلفة وطريقة الحساب"){
            MetricRow("مواد","${money(rows.sumOf{it.materialCost})} جنيه")
            if(rows.sumOf{it.labor}>0)MetricRow("مصنعية","${money(rows.sumOf{it.labor})} جنيه")
            if(rows.sumOf{it.transport}>0)MetricRow("نقل","${money(rows.sumOf{it.transport})} جنيه")
            if(rows.sumOf{it.equipment}>0)MetricRow("معدات","${money(rows.sumOf{it.equipment})} جنيه")
            rows.forEach{r->Text(r.part+": "+r.formula,style=MaterialTheme.typography.bodySmall)}
        }
    }
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
            if(p.price>0||p.cost>0)MetricRow("تكلفة الشراء","${money(p.cost)} جنيه")else Text("السعر غير مدخل",style=MaterialTheme.typography.bodySmall)
        }
        if(purchases.isNotEmpty()){
            MetricRow(if(complete)"إجمالي الشراء" else "تكلفة شراء جزئية","${money(purchases.sumOf{it.cost})} جنيه")
            Text("تم تجميع الخامات المتطابقة وتقريب الشكاير والعبوات مرة واحدة.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun CalculatorResultCards(def:CalcDef,inputs:Map<String,String>,answer:CalcAnswer,scope:String){
    val missing=MaterialReview.missing(def,inputs)
    val mortar=def.id in CalculatorLibrary.mortarIds
    val lines=answer.outputs.filter{it.unit !in setOf("جنيه","جنيه/م²")&&it.value!=0.0}
    val stock=CostEngine.calculatorPurchases(def,inputs,answer)
    BoxCard{
        Text("النتيجة",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
        if(mortar){
            MetricRow("الرمل المطلوب","${fmt(lines.first{it.label=="رمل"}.value)} م³",true)
            MetricRow("شراء الأسمنت","${fmt(lines.first{it.label=="شراء أسمنت"}.value)} شيكارة",true)
            val kg=lines.first{it.label=="أسمنت فعلي"}.value
            Text("استهلاك فعلي: ${displayAmount(kg,"كجم")} كجم = ${displayAmount(kg/n(inputs["bag"].orEmpty()),"شيكارة")} شيكارة × ${inputs["bag"]} كجم",style=MaterialTheme.typography.bodySmall)
            if((inputs["_mortarMode"]?:"area")!="area")MetricRow("تغطية المونة","${fmt(CalculatorLibrary.mortarArea(inputs))} م²")
        }else if(stock.isNotEmpty()){
            stock.forEach{p->
                MetricRow(p.material,if(p.packages!=null)"${p.packages} ${p.packageUnit}"else "${displayAmount(p.amount,p.unit)} ${p.unit}",true)
                if(p.packages!=null)Text("احتياج فعلي: ${displayAmount(p.amount,p.unit)} ${p.unit}",style=MaterialTheme.typography.labelSmall)
            }
        }else lines.filterNot{it.label.contains("فائض")||it.label.contains("وزن المتر")}.take(3).forEach{MetricRow(it.label,"${displayAmount(it.value,it.unit)} ${it.unit}",true)}
        if(answer.cost>0){HorizontalDivider();MetricRow(if(missing.isEmpty())"تكلفة شراء الخامات"else "تكلفة شراء جزئية","${money(answer.cost)} جنيه",true)}
        if(missing.isNotEmpty()){Text("التكلفة غير مكتملة",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);Text(missing.joinToString("، "),style=MaterialTheme.typography.labelSmall)}
        ExpandableSection("التفاصيل وطريقة الحساب"){
            if(mortar){MetricRow("مونة منفذة","${fmt(lines.first{it.label=="مونة منفذة"}.value)} م³");Text("الهالك المستخدم: ${inputs["waste"]}%",style=MaterialTheme.typography.bodySmall)}
            if(stock.isNotEmpty()&&!mortar)lines.filter{it.label.contains("فائض")||it.unit.contains("/")}.forEach{MetricRow(it.label,"${displayAmount(it.value,it.unit)} ${it.unit}")}
            if(stock.isEmpty()&&lines.size>3)lines.drop(3).forEach{MetricRow(it.label,"${displayAmount(it.value,it.unit)} ${it.unit}")}
            if(answer.consumedCost>0)MetricRow("تكلفة الاستهلاك قبل تقريب العبوات","${money(answer.consumedCost)} جنيه")
            if(MortarMix.isMix(def))Text(MortarMix.summary(inputs),style=MaterialTheme.typography.bodySmall)
            Text(def.formula,style=MaterialTheme.typography.bodySmall);Text(answer.explanation,style=MaterialTheme.typography.bodySmall)
        }
    }
}

private fun displayAmount(value:Double,unit:String):String=if(unit in setOf("كجم","شيكارة","جنيه","جنيه/م²"))java.math.BigDecimal.valueOf(value).setScale(if(unit=="كجم")1 else 2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()else fmt(value)

package com.fayroz.sitecalculator.reports

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.MaterialReview
import com.fayroz.sitecalculator.domain.CostEngine
import com.fayroz.sitecalculator.domain.QuantityEngine
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExport {
    private fun format(v:Double)=java.math.BigDecimal.valueOf(v).setScale(3,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    private fun money(v:Double)=String.format(Locale.US,"%.2f",v)
    private fun x(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
    fun table(p:Project,kind:String):List<List<String>>{
        val rows=CostEngine.rows(p)
        val date=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date())
        val out=mutableListOf(listOf("FAYROZ SITE CALCULATOR",p.name,kind,date))
        if(kind=="شراء الخامات"){
            out+=listOf("المادة","الوحدة","الكمية الفعلية","كمية الشراء","سعر العبوة / الوحدة","تكلفة الشراء")
            CostEngine.purchase(rows).forEach{out+=listOf(it.material,it.unit,it.amount.toString(),it.packages?.let{n->"$n ${it.packageUnit}"}?:"",it.price.toString(),it.cost.toString())}
        }else if(kind=="ملخص الكميات"){
            out+=listOf("البند","الوحدة","صافي الكمية")
            QuantityEngine.summarize(p).forEach{out+=listOf(it.name,it.unit.label,it.quantity.toString())}
        }else if(kind=="تحليل أسعار البنود"){
            out+=listOf("المكان","البند","الكمية","الوحدة","مواد / وحدة","معدات / وحدة","عمالة / وحدة","نقل / وحدة","تكلفة مباشرة / وحدة","مصاريف عامة / وحدة","ربح / وحدة","ضريبة / وحدة","سعر بيع / وحدة","حالة التسعير")
            rows.forEach{r->val price=CostEngine.selling(p,listOf(r));val q=r.quantity.takeIf{it>0}?:1.0
                out+=listOf(r.location,r.item,r.quantity.toString(),r.unit,(r.materialCost/q).toString(),(r.equipment/q).toString(),(r.labor/q).toString(),(r.transport/q).toString(),(price.direct/q).toString(),(price.overhead/q).toString(),(price.profit/q).toString(),(price.tax/q).toString(),(price.total/q).toString(),if(MaterialReview.missing(r).isEmpty())"حسب الأسعار المدخلة"else "أسعار ناقصة")
            }
            out+=listOf("إجمالي سعر البيع المدخل",CostEngine.selling(p,rows).total.toString(),"جنيه")
            out+=listOf("الأساس","المصاريف على التكلفة المباشرة؛ الربح بعد المصاريف؛ الضريبة بعد الربح. التكاليف غير المدخلة غير مشمولة.")
        }else{
            out+=listOf("المكان","البند","الجزء","صافي الكمية","الوحدة","الخامات المطلوبة","تكلفة مباشرة","حالة التسعير")
            rows.forEach{r->
                val materials=if(r.spec!=null)"${r.spec.cementName}: ${format(r.cementKg)} كجم • ${r.spec.sandName}: ${format(r.sandM3)} م³ • سمك ${format(r.spec.thicknessMm)} مم"else CostEngine.purchase(listOf(r)).joinToString(" • "){"${it.material}: ${format(it.amount)} ${it.unit}"}
                out+=listOf(r.location,r.item,r.part,r.quantity.toString(),r.unit,materials,r.total.toString(),if(MaterialReview.missing(r).isEmpty())"حسب الأسعار المدخلة"else "أسعار ناقصة: "+MaterialReview.missing(r).joinToString("، "))
            }
            out+=listOf("إجمالي التكلفة المباشرة",rows.sumOf{it.total}.toString(),"جنيه")
        }
        val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
        if(missing.isNotEmpty())out+=listOf("التكلفة غير مكتملة — المبالغ جزئية","أسعار ناقصة: "+missing.joinToString("، "))
        if(p.calculations.isNotEmpty()&&kind=="حصر وتكلفة تفصيلي"){
            out+=listOf("حسابات محفوظة بأسعار تاريخ الحفظ")
            p.calculations.forEach{c->out+=listOf(c.title,if(c.inputs["_includeInProject"]=="true")"مضاف للإجمالي أعلاه"else "مرجع فقط — خارج الإجمالي",c.summary,c.cost.toString())}
        }
        return out
    }
    fun xlsx(p:Project,kind:String,stream:OutputStream){
        fun column(n:Int):String{var num=n+1;var out="";while(num>0){num--;out=('A'.code+num%26).toChar()+out;num/=26};return out}
        val table=table(p,kind)
        ZipOutputStream(stream).use{zip->
            fun add(path:String,text:String){zip.putNextEntry(ZipEntry(path));zip.write(text.toByteArray(Charsets.UTF_8));zip.closeEntry()}
            add("[Content_Types].xml","""<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""")
            add("_rels/.rels","""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            add("xl/workbook.xml","""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="كشف المشروع" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            add("xl/_rels/workbook.xml.rels","""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
            add("xl/styles.xml","""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="1"><numFmt numFmtId="164" formatCode="0.###"/></numFmts><fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="right" vertical="top" wrapText="1" readingOrder="2"/></xf><xf numFmtId="4" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment horizontal="right" vertical="top" wrapText="1" readingOrder="2"/></xf></cellXfs></styleSheet>""")
            add("xl/worksheets/sheet1.xml",buildString{
                append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"><pane ySplit=\"2\" topLeftCell=\"A3\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"15\" width=\"22\" customWidth=\"1\"/></cols><sheetData>")
                table.forEachIndexed{r,row->append("<row r=\"${r+1}\">");row.forEachIndexed{c,value->
                    val numeric=value.toDoubleOrNull()
                    if(numeric!=null&&numeric.isFinite()){
                        val heading=table.getOrNull(1)?.getOrNull(c).orEmpty()
                        val currency=listOf("سعر","تكلفة","مواد /","معدات /","عمالة /","نقل /","ربح","ضريبة","مصاريف").any{heading.contains(it)}||(row.firstOrNull()?.contains("إجمالي") == true&&c==1)
                        append("<c r=\"${column(c)}${r+1}\" s=\"${if(currency)1 else 0}\"><v>$numeric</v></c>")
                    }
                    else append("<c r=\"${column(c)}${r+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${x(value)}</t></is></c>")
                };append("</row>")}
                append("</sheetData><pageSetup orientation=\"landscape\" paperSize=\"9\"/></worksheet>")
            })
        }
    }
    fun printPdf(context:Context,p:Project,kind:String){
        val rows=table(p,kind)
        val body=if(kind in setOf("حصر وتكلفة تفصيلي","تحليل أسعار البنود"))buildString{
            CostEngine.rows(p).forEach{r->
                append("<article><h2>${x(r.item)} — ${x(r.location)}</h2><p>${x(r.part)} • الكمية: ${format(r.quantity)} ${x(r.unit)}</p>")
                val missing=MaterialReview.missing(r)
                if(missing.isNotEmpty())append("<p class='missing'>أسعار ناقصة: ${x(missing.joinToString("، "))}</p>")
                if(kind=="تحليل أسعار البنود"){
                    val price=CostEngine.selling(p,listOf(r));val q=r.quantity.takeIf{it>0}?:1.0
                    append("<table><thead><tr><th>عنصر السعر</th><th>جنيه / ${x(r.unit)}</th></tr></thead><tbody>")
                    listOf("مواد" to r.materialCost,"معدات" to r.equipment,"عمالة" to r.labor,"نقل" to r.transport,"تكلفة مباشرة" to price.direct,"مصاريف عامة" to price.overhead,"ربح" to price.profit,"ضريبة" to price.tax,"سعر البيع المدخل" to price.total).forEach{(label,value)->append("<tr><td>${x(label)}</td><td>${money(value/q)}</td></tr>")}
                    append("</tbody></table>")
                }else{
                    append("<table><thead><tr><th>الخامة</th><th>الاحتياج الفعلي</th><th>الشراء</th></tr></thead><tbody>")
                    CostEngine.purchase(listOf(r)).forEach{m->append("<tr><td>${x(m.material)}</td><td>${format(m.amount)} ${x(m.unit)}</td><td>${m.packages?.let{"$it ${x(m.packageUnit)}"}?:format(m.amount)}</td></tr>")}
                    append("</tbody></table><p>مواد: ${money(r.materialCost)} • معدات: ${money(r.equipment)} • عمالة: ${money(r.labor)} • نقل: ${money(r.transport)} جنيه</p>")
                }
                append("<p>التكلفة المباشرة: ${money(r.total)} جنيه</p><details open><summary>طريقة الحصر</summary><p>${x(r.formula).replace("\n","<br>")}</p></details></article>")
            }
            append("<h2>إجمالي التكلفة المباشرة: ${money(CostEngine.rows(p).sumOf{it.total})} جنيه</h2>")
            if(kind=="تحليل أسعار البنود")append("<h2>إجمالي سعر البيع المدخل: ${money(CostEngine.selling(p,CostEngine.rows(p)).total)} جنيه</h2><p>المصاريف على التكلفة المباشرة، والربح بعد المصاريف، والضريبة بعد الربح. أي تكلفة غير مدخلة غير مشمولة.</p>")
            p.calculations.filter{it.inputs["_includeInProject"]!="true"}.forEach{c->append("<article><h2>${x(c.title)} — مرجع خارج الإجمالي</h2><p>${x(c.summary)}</p></article>")}
        }else "<table><thead><tr>"+rows.getOrElse(1){emptyList()}.joinToString(""){"<th>${x(it)}</th>"}+"</tr></thead><tbody>"+rows.drop(2).joinToString(""){row->"<tr>"+row.joinToString(""){v->"<td>${x(v.toDoubleOrNull()?.let{format(it)}?:v)}</td>"}+"</tr>"}+"</tbody></table>"
        val html="""<!doctype html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"><style>body{font-family:sans-serif;font-size:12px;color:#11263a;line-height:1.6}h1{font-size:20px}h2{font-size:15px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #bac7cf;padding:7px;overflow-wrap:anywhere}th{background:#d9efed}thead{display:table-header-group}tr{break-inside:avoid}article{margin:12px 0;padding:12px;border:1px solid #bac7cf;break-inside:avoid}.missing{color:#a33020}@page{size:A4 portrait;margin:12mm}</style></head><body><h1>${x(p.name)} — ${x(kind)}</h1><p>${x(rows.first().last())}</p>$body</body></html>"""

        val web=WebView(context)
        web.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView,url:String){
            val print=context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            print.print("${p.name} - $kind",view.createPrintDocumentAdapter("Fayroz-report"),PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait()).build())
        }}
        web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null)
    }
}


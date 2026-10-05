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
    private fun x(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
    fun table(p:Project,kind:String):List<List<String>>{
        val rows=CostEngine.rows(p)
        val date=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(Date())
        val out=mutableListOf(listOf("FAYROZ SITE CALCULATOR",p.name,kind,date))
        if(kind=="شراء الخامات"){
            out+=listOf("المادة","الوحدة","الكمية الفعلية","عبوات شراء","سعر العبوة / الوحدة","تكلفة الشراء")
            CostEngine.purchase(rows).forEach{out+=listOf(it.material,it.unit,it.amount.toString(),it.packages?.toString()?:"",it.price.toString(),it.cost.toString())}
        }else if(kind=="ملخص الكميات"){
            out+=listOf("البند","الوحدة","صافي الكمية")
            QuantityEngine.summarize(p).forEach{out+=listOf(it.name,it.unit.label,it.quantity.toString())}
        }else{
            out+=listOf("المكان","البند","الجزء","صافي الكمية","الوحدة","سمك مم","أسمنت كجم","رمل م³","تكلفة المواد","مصنعية","نقل","معدات","الإجمالي","طريقة الحساب","حالة التسعير")
            rows.forEach{out+=listOf(it.location,it.item,it.part,it.quantity.toString(),it.unit,it.spec?.thicknessMm?.toString()?:"",it.cementKg.toString(),it.sandM3.toString(),it.materialCost.toString(),it.labor.toString(),it.transport.toString(),it.equipment.toString(),it.total.toString(),it.formula,if(MaterialReview.missing(it).isEmpty())"مكتمل" else "أسعار ناقصة: "+MaterialReview.missing(it).joinToString("، "))}
            out+=listOf("إجمالي التكلفة",rows.sumOf{it.total}.toString(),"جنيه")
        }
        val missing=rows.flatMap{MaterialReview.missing(it)}.distinct()
        if(missing.isNotEmpty())out+=listOf("التكلفة غير مكتملة — المبالغ جزئية","أسعار ناقصة: "+missing.joinToString("، "))
        if(p.calculations.isNotEmpty()){
            out+=listOf("حسابات محفوظة بأسعار تاريخ الحفظ")
            p.calculations.forEach{c->out+=listOf(c.title,c.summary,c.cost.toString(),c.explanation,c.inputs.entries.joinToString(" • "){"${it.key}=${it.value}"})}
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
            add("xl/styles.xml","""<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment horizontal="right" vertical="top" wrapText="1" readingOrder="2"/></xf></cellXfs></styleSheet>""")
            add("xl/worksheets/sheet1.xml",buildString{
                append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"><pane ySplit=\"2\" topLeftCell=\"A3\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"15\" width=\"22\" customWidth=\"1\"/></cols><sheetData>")
                table.forEachIndexed{r,row->append("<row r=\"${r+1}\">");row.forEachIndexed{c,value->
                    val numeric=value.toDoubleOrNull()
                    if(numeric!=null&&numeric.isFinite())append("<c r=\"${column(c)}${r+1}\"><v>$numeric</v></c>")
                    else append("<c r=\"${column(c)}${r+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${x(value)}</t></is></c>")
                };append("</row>")}
                append("</sheetData><pageSetup orientation=\"landscape\" paperSize=\"9\"/></worksheet>")
            })
        }
    }
    fun printPdf(context:Context,p:Project,kind:String){
        val rows=table(p,kind)
        val html="""<!doctype html><html lang="ar" dir="rtl"><head><meta charset="UTF-8"><style>body{font-family:sans-serif;font-size:10px;color:#11263a}h1{font-size:18px}table{border-collapse:collapse;width:100%;table-layout:auto}td{border:1px solid #bac7cf;padding:5px;overflow-wrap:anywhere}tr{break-inside:avoid}tr:nth-child(2){background:#d9efed;font-weight:bold}@page{size:A4 landscape;margin:12mm}</style></head><body><h1>${x(p.name)} — ${x(kind)}</h1><table>${rows.joinToString(""){row->"<tr>"+row.joinToString(""){"<td>${x(it)}</td>"}+"</tr>"}}</table></body></html>"""
        val web=WebView(context)
        web.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView,url:String){
            val print=context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            print.print("${p.name} - $kind",view.createPrintDocumentAdapter("Fayroz-report"),PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build())
        }}
        web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null)
    }
}

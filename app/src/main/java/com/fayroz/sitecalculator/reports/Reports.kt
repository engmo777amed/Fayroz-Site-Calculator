package com.fayroz.sitecalculator.reports

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fayroz.sitecalculator.core.SiteProject
import com.fayroz.sitecalculator.domain.QuantityEngine
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object Reports {
    private fun fmt(v:Double)=String.format(Locale.US,"%.2f",v)

    fun summaryText(project:SiteProject)=buildString{
        appendLine("FAYROZ SITE CALCULATOR")
        appendLine("المشروع: ${project.name}")
        appendLine("النوع: ${project.type}")
        appendLine("--------------------")
        QuantityEngine.projectSummary(project).forEach{(key,value)->
            appendLine("${key.first}: ${fmt(value)} ${key.second.label}")
        }
    }

    fun csv(project:SiteProject)=buildString{
        appendLine("Project,Section,Space,Status,Item,Quantity,Unit,Note")
        project.sections.forEach{section->
            section.spaces.forEach{space->
                space.takeoffs.forEach{item->
                    val q=QuantityEngine.calculate(space,item)
                    appendLine(listOf(
                        project.name,section.name,space.name,space.status.label,item.name,
                        fmt(q.final),item.unit.label,space.note
                    ).joinToString(","){csvCell(it)})
                }
            }
        }
    }

    private fun csvCell(s:String):String {
        val quote=34.toChar().toString()
        return quote+s.replace(quote,quote+quote)+quote
    }

    fun shareText(context:Context,title:String,text:String){
        val intent=Intent(Intent.ACTION_SEND).apply{
            type="text/plain"
            putExtra(Intent.EXTRA_SUBJECT,title)
            putExtra(Intent.EXTRA_TEXT,text)
        }
        context.startActivity(Intent.createChooser(intent,"مشاركة الحصر"))
    }

    fun shareCsv(context:Context,project:SiteProject){
        val dir=File(context.cacheDir,"exports").apply{mkdirs()}
        val file=File(dir,safe(project.name)+"-takeoff.csv")
        file.writeText(csv(project),Charsets.UTF_8)
        shareFile(context,file,"text/csv","تصدير CSV")
    }

    fun sharePdf(context:Context,project:SiteProject){
        val dir=File(context.cacheDir,"exports").apply{mkdirs()}
        val file=File(dir,safe(project.name)+"-takeoff.pdf")
        val doc=PdfDocument()
        val paint=Paint().apply{isAntiAlias=true;textSize=12f}
        val title=Paint().apply{isAntiAlias=true;textSize=18f;isFakeBoldText=true}
        var pageNo=1
        var page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create())
        var y=45f
        fun line(text:String,bold:Boolean=false){
            if(y>805){
                doc.finishPage(page)
                pageNo++
                page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create())
                y=45f
            }
            page.canvas.drawText(text,35f,y,if(bold)title else paint)
            y+=if(bold)28f else 20f
        }
        line("FAYROZ SITE CALCULATOR",true)
        line("Project: ${project.name}")
        line("Type: ${project.type}")
        line(" ")
        QuantityEngine.projectSummary(project).forEach{(key,value)->
            line("${key.first}: ${fmt(value)} ${key.second.label}")
        }
        project.sections.forEach{section->
            line(" ")
            line(section.name,true)
            section.spaces.forEach{space->
                line("  ${space.name} - ${space.status.label}")
                space.takeoffs.forEach{item->
                    val q=QuantityEngine.calculate(space,item)
                    line("    ${item.name}: ${fmt(q.final)} ${item.unit.label}")
                }
            }
        }
        doc.finishPage(page)
        FileOutputStream(file).use{doc.writeTo(it)}
        doc.close()
        shareFile(context,file,"application/pdf","تصدير PDF")
    }

    fun shareBackup(context:Context,raw:String)=shareText(context,"Fayroz Site Calculator Backup",raw)

    private fun shareFile(context:Context,file:File,mime:String,title:String){
        val uri=FileProvider.getUriForFile(context,context.packageName+".files",file)
        val intent=Intent(Intent.ACTION_SEND).apply{
            type=mime
            putExtra(Intent.EXTRA_STREAM,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent,title))
    }

    private fun safe(s:String)=s.replace(Regex("[^A-Za-z0-9_-]"),"_").take(40).ifBlank{"project"}
}

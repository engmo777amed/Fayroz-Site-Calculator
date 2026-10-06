package com.fayroz.sitecalculator.data
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.fayroz.sitecalculator.core.*
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

object BackupArchive {
 private const val LIMIT=128*1024*1024
 fun write(context:Context,repository:V8Repository,stream:OutputStream){
    val photos=repository.loadProjects().flatMap{it.sections}.flatMap{it.spaces}.flatMap{it.photoUris}.distinct()
    ZipOutputStream(stream).use{zip->
       val mapping=JSONObject()
       photos.forEachIndexed{i,uri->
          val path="photos/$i.bin"
          context.contentResolver.openInputStream(Uri.parse(uri))?.use{input->
              zip.putNextEntry(ZipEntry(path));input.copyTo(zip);zip.closeEntry();mapping.put(uri,path)
          }?:error("الصورة غير متاحة: احذف المرجع غير المتاح أو أعد إرفاقها ثم حاول مجددًا")
       }
       val root=JSONObject(repository.exportBackup()).put("photoFiles",mapping)
       zip.putNextEntry(ZipEntry("backup.json"));zip.write(root.toString().toByteArray());zip.closeEntry()
    }
 }
 fun preview(bytes:ByteArray):String=runCatching{
    val raw=if(bytes.firstOrNull()=='P'.code.toByte())ZipInputStream(bytes.inputStream()).use{zip->
        var entry=zip.nextEntry;var result:String?=null
        while(entry!=null){if(entry.name=="backup.json"){result=zip.readBytes().toString(Charsets.UTF_8);break};entry=zip.nextEntry};result?:error("بيانات مفقودة")
    }else bytes.toString(Charsets.UTF_8)
    val projects=V8Codec.decodeProjects(JSONObject(raw).getJSONArray("projects").toString())
    "${projects.size} مشروع • ${projects.sumOf{it.sections.sumOf{s->s.spaces.size}}} مكان • ${projects.sumOf{it.calculations.size}} حساب محفوظ"
 }.getOrDefault("تعذر معاينة النسخة؛ لن تُقبل نسخة غير صالحة.")
 fun restore(context:Context,repository:V8Repository,bytes:ByteArray,merge:Boolean):Boolean {
    val staged=mutableListOf<File>()
    return runCatching{
       if(bytes.size>LIMIT)error("نسخة أكبر من الحد")
       if(bytes.firstOrNull()!='P'.code.toByte())return@runCatching repository.importBackup(bytes.toString(Charsets.UTF_8),merge)
       val entries=mutableMapOf<String,ByteArray>()
       var total=0
       ZipInputStream(bytes.inputStream()).use{zip->
          var entry=zip.nextEntry
          while(entry!=null){
              require(!entry.isDirectory&&(entry.name=="backup.json"||Regex("photos/[0-9]+\\.bin").matches(entry.name))){"ملف غير صالح داخل النسخة"}
              require(entry.name !in entries){"ملف مكرر"}
              val buffer=java.io.ByteArrayOutputStream();val block=ByteArray(8192)
              while(true){val n=zip.read(block);if(n<0)break;total+=n;require(total<=LIMIT);buffer.write(block,0,n)}
              entries[entry.name]=buffer.toByteArray();entry=zip.nextEntry
          }
       }
       val json=JSONObject(entries["backup.json"]?.toString(Charsets.UTF_8)?:error("ملف البيانات مفقود"))
       val mapping=json.optJSONObject("photoFiles")?:JSONObject()
       val uriMap=mutableMapOf<String,String>()
       val dir=File(context.filesDir,"restored-photos").apply{mkdirs()}
       mapping.keys().forEach{old->
           val file=File(dir,"${UUID.randomUUID()}.jpg")
           file.writeBytes(entries[mapping.getString(old)]?:error("صورة مفقودة"));staged+=file
           uriMap[old]=FileProvider.getUriForFile(context,context.packageName+".files",file).toString()
       }
       val projects=V8Codec.decodeProjects(json.getJSONArray("projects").toString())
       require(projects.size==json.getJSONArray("projects").length())
       val changed=projects.map{p->p.copy(sections=p.sections.map{s->s.copy(spaces=s.spaces.map{sp->sp.copy(photoUris=sp.photoUris.map{uriMap[it]?:it})})})}
       json.put("projects",org.json.JSONArray(V8Codec.encodeProjects(changed)))
       require(repository.importBackup(json.toString(),merge))
       true
    }.getOrElse{staged.forEach{it.delete()};false}
 }
}


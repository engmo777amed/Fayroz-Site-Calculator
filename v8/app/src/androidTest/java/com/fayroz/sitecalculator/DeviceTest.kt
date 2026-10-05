package com.fayroz.sitecalculator

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.core.content.FileProvider
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.*
import com.fayroz.sitecalculator.reports.ReportExport
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
 private fun sample():Project {
    val t=Takeoff(name="محارة الأسقف",unit=UnitType.AREA,kind=CalcKind.CEILING,material=MaterialSpec(cementPrice=200.0,sandPrice=300.0),parts=listOf(WorkPart(name="جزء السقف",length=2.0,width=3.0)))
    return Project(name="اختبار الحصر",sections=listOf(Section(name="الدور الأول",spaces=listOf(Space(name="غرفة الاختبار",type="غرفة نوم",length=4.0,width=3.0,takeoffs=listOf(t))))))
 }
 @Before fun reset(){context.getSharedPreferences("fayroz_site_v8",Context.MODE_PRIVATE).edit().clear().commit();context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE).edit().clear().commit()}
 private fun shot(name:String){val file=File(context.getExternalFilesDir(null),"screenshots/$name.png");file.parentFile!!.mkdirs();assertTrue(device.takeScreenshot(file))}
 @Test fun libraryAndProjectScreensOpen(){
    V8Repository(context).saveProjects(listOf(sample()))
    ActivityScenario.launch(MainActivity::class.java).use{
       assertTrue(device.wait(Until.hasObject(By.text("الرئيسية")),10000));shot("home")
       device.findObject(By.text("الحاسبات")).click()
       assertTrue(device.wait(Until.hasObject(By.text("مكتبة الحاسبات")),10000));shot("calculators")
       device.findObject(By.text("محارة حوائط / أسقف / واجهات")).click()
       assertTrue(device.wait(Until.hasObject(By.text("اسم الحساب")),10000));shot("mortar-calculator")
       device.pressBack()
       device.findObject(By.text("المشروعات")).click()
       assertTrue(device.wait(Until.hasObject(By.text("اختبار الحصر")),10000))
       device.findObject(By.text("اختبار الحصر")).click()
       assertTrue(device.wait(Until.hasObject(By.text("الدور الأول")),10000));shot("project")
    }
 }
 @Test fun backupIncludesPhotosAndRestoresSpecs(){
    val dir=File(context.filesDir,"restored-photos").apply{mkdirs()}
    val file=File(dir,"test.jpg").apply{writeBytes(byteArrayOf(1,2,3,4))}
    val uri=FileProvider.getUriForFile(context,context.packageName+".files",file).toString()
    val p=sample().let{p->p.copy(sections=p.sections.map{s->s.copy(spaces=s.spaces.map{it.copy(photoUris=listOf(uri))})})}
    val repo=V8Repository(context);repo.saveProjects(listOf(p));repo.setPref("favorites","plaster|tile")
    val out=ByteArrayOutputStream();BackupArchive.write(context,repo,out)
    repo.saveProjects(emptyList());assertTrue(BackupArchive.restore(context,repo,out.toByteArray(),false))
    val restored=repo.loadProjects().single();assertEquals(p.name,restored.name)
    assertEquals("جزء السقف",restored.sections[0].spaces[0].takeoffs[0].parts[0].name)
    val photo=restored.sections[0].spaces[0].photoUris.single()
    assertArrayEquals(byteArrayOf(1,2,3,4),context.contentResolver.openInputStream(android.net.Uri.parse(photo))!!.use{it.readBytes()})
    assertEquals("plaster|tile",repo.pref("favorites",""))
 }
 @Test fun restoreMergePreservesCurrentAndRejectsMalformed(){
    val repo=V8Repository(context);val p=sample();repo.saveProjects(listOf(p));val backup=repo.exportBackup()
    repo.saveProjects(listOf(p.copy(name="الاسم الحالي"),Project(name="جديد")))
    assertTrue(repo.importBackup(backup,true));assertEquals(2,repo.loadProjects().size);assertEquals("الاسم الحالي",repo.loadProjects().first{it.id==p.id}.name)
    val before=repo.loadProjects();assertFalse(repo.importBackup("{\"version\":9,\"projects\":[5]}",false));assertEquals(before,repo.loadProjects())
 }
 @Test fun excelContainsDetailedArabicRows(){
    val output=ByteArrayOutputStream();ReportExport.xlsx(sample(),"حصر وتكلفة تفصيلي",output)
    val entries=mutableMapOf<String,String>();ZipInputStream(output.toByteArray().inputStream()).use{zip->var e=zip.nextEntry;while(e!=null){entries[e.name]=zip.readBytes().toString(Charsets.UTF_8);e=zip.nextEntry}}
    assertTrue(entries.containsKey("[Content_Types].xml"));assertTrue(entries["xl/worksheets/sheet1.xml"]!!.contains("جزء السقف"));assertTrue(entries["xl/worksheets/sheet1.xml"]!!.contains("rightToLeft=\"1\""))
 }
}

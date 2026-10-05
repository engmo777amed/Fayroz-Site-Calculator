package com.fayroz.sitecalculator

import android.content.Context
import android.content.Intent
import com.fayroz.sitecalculator.domain.CalculatorLibrary
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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTest {
 @get:Rule val compose = createEmptyComposeRule()
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 private val device get()=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
 private fun sample():Project {
    val t=Takeoff(name="محارة الأسقف",unit=UnitType.AREA,kind=CalcKind.CEILING,material=MaterialSpec(cementPrice=200.0,sandPrice=300.0),parts=listOf(WorkPart(name="جزء السقف",length=2.0,width=3.0)))
    return Project(name="اختبار الحصر",sections=listOf(Section(name="الدور الأول",spaces=listOf(Space(name="غرفة الاختبار",type="غرفة نوم",length=4.0,width=3.0,takeoffs=listOf(t))))))
 }
 @Before fun reset(){context.getSharedPreferences("fayroz_site_v8",Context.MODE_PRIVATE).edit().clear().commit();context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE).edit().clear().commit()}
 private fun shot(name:String){val file=File(context.getExternalFilesDir(null),"screenshots/$name.png");file.parentFile!!.mkdirs();assertTrue(device.takeScreenshot(file));device.executeShellCommand("mkdir -p /sdcard/Download/site-v9-screenshots");device.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/site-v9-screenshots/$name.png")}
 @After fun captureFinalState(){shot("last-state");device.dumpWindowHierarchy(File(context.getExternalFilesDir(null),"screenshots/window.xml"))}
 @Test fun libraryAndProjectScreensOpen(){
    V8Repository(context).saveProjects(listOf(sample()))
    ActivityScenario.launch(MainActivity::class.java).use{
       assertTrue(device.wait(Until.hasObject(By.text("الرئيسية")),10000));shot("home")
       device.findObject(By.text("الحاسبات")).click()
       assertTrue(device.wait(Until.hasObject(By.text("أقسام الحاسبات")),10000));shot("calculators")
       clickText("المونة والتشطيبات")
       clickText("محارة حوائط / أسقف / واجهات")
       assertTrue(device.wait(Until.hasObject(By.text("اسم الحساب")),10000));shot("mortar-calculator")
       device.pressBack();device.pressBack()
       device.findObject(By.text("المشروعات")).click()
       assertTrue(device.wait(Until.hasObject(By.text("اختبار الحصر")),10000))
       device.findObject(By.text("اختبار الحصر")).click()
       assertTrue(device.wait(Until.hasObject(By.text("الدور الأول")),10000));shot("project")
       device.findObject(By.text("الدور الأول")).click()
       assertTrue(device.wait(Until.hasObject(By.text("غرفة الاختبار")),10000));shot("section")
       device.findObject(By.text("غرفة الاختبار")).click()
       assertTrue(device.wait(Until.hasObject(By.text("اسم المكان")),10000));shot("room-input")
       device.findObject(By.text("التالي")).click();device.waitForIdle();device.findObject(By.text("التالي")).click();device.waitForIdle()
       assertTrue(device.wait(Until.hasObject(By.desc("فتح البند")),10000));shot("room-results")
       device.findObject(By.desc("فتح البند")).click();device.waitForIdle();shot("part-editor");assertTrue(device.wait(Until.hasObject(By.text("التالي: المواد")),10000))
       device.findObject(By.text("التالي: المواد")).click();device.waitForIdle();shot("item-materials")
       device.findObject(By.text("كامل البند")).click();device.findObject(By.text("جزء السقف (1)")).click();device.waitForIdle()
       device.findObject(By.clazz("android.widget.Switch")).click();device.waitForIdle()
       type("متوسط السمك","20.5")
       type("سعر شيكارة الأسمنت","250")
       device.findObject(By.text("احسب واعرض النتيجة")).click();assertTrue(device.wait(Until.hasObject(By.text("حفظ البند")),10000));shot("item-result")
       assertTrue(device.wait(Until.hasObject(By.text("6 م²")),10000))
       device.findObject(By.text("حفظ البند")).click()
       device.findObject(By.text("حفظ")).click();device.waitForIdle()
       val saved=V8Repository(context).loadProjects().single().sections[0].spaces[0].takeoffs[0]
       assertEquals(15.0,saved.material!!.thicknessMm,0.0)
       assertEquals(200.0,saved.material!!.cementPrice,0.0)
       assertEquals(20.5,saved.parts.single().material!!.thicknessMm,0.0)
       assertEquals(250.0,saved.parts.single().material!!.cementPrice,0.0)
    }
 }
 private fun field(label:String):SemanticsNodeInteraction {
    val matcher=hasSetTextAction() and (hasContentDescription("إدخال $label") or hasAnyAncestor(hasContentDescription("إدخال $label")))
    repeat(15){
        compose.waitForIdle()
        if(compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()) {
            val node=compose.onNode(matcher,useUnmergedTree=true)
            runCatching{node.performScrollTo()}
            return node
        }
        device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,20)
    }
    fail("Field missing: $label");throw IllegalStateException()
 }
 private fun type(label:String,value:String){field(label).performTextReplacement(value);compose.waitForIdle();field(label).assertTextEquals(value)}
 private fun clickText(text:String){
    compose.waitForIdle()
    val node=compose.onNodeWithText(text)
    runCatching{node.performScrollTo()}
    node.performClick();compose.waitForIdle()
 }
 @Test fun calculatorComputesSavesAndReopensDecimalInputs(){
    val intent=Intent(context,MainActivity::class.java).putExtra("calculator","plaster")
    ActivityScenario.launch<MainActivity>(intent).use{
       assertTrue(device.wait(Until.hasObject(By.text("اسم الحساب")),10000))
       type("اسم الحساب","اختبار محارة 12.5")
       type("المساحة الصافية","12.5")
       type("متوسط السمك","15.5")
       clickText("مم");clickText("سم")
       field("متوسط السمك").assertTextEquals("1.55")
       type("سعر شيكارة الأسمنت","200")
       type("سعر متر الرمل","300")
       shot("calculator-filled")
       device.findObject(By.text("احسب واعرض النتيجة")).click();device.waitForIdle();shot("calculator-result")
       assertTrue(device.wait(Until.hasObject(By.text("12.5 م²")),10000))
       device.findObject(By.text("حفظ النتيجة")).click()
       val saved=V8Repository(context).recentCalcs().first()
       assertEquals("12.5",saved.inputs["area"]);assertEquals("15.5",saved.inputs["thickness"]);assertTrue(saved.cost>0)
    }
    ActivityScenario.launch(MainActivity::class.java).use{
       device.findObject(By.text("المحفوظات")).click()
       assertTrue(device.wait(Until.hasObject(By.textContains("اختبار محارة 12.5")),10000));shot("saved-calculations")
       device.findObject(By.textContains("اختبار محارة 12.5")).click()
       assertTrue(device.wait(Until.hasObject(By.text("12.5 م²")),10000))
       device.findObject(By.text("تعديل المدخلات")).click()
       field("المساحة الصافية").assertTextEquals("12.5")
       field("متوسط السمك").assertTextEquals("1.55")
    }
 }
 @Test fun quantitiesWorkWithoutPricesAndErrorsAreVisible(){
    V8Repository(context).setPref("calc.paint.area","10")
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","paint")).use{
       assertTrue(device.wait(Until.hasObject(By.text("اسم الحساب")),10000))
       device.findObject(By.text("احسب واعرض النتيجة")).click()
       assertTrue(device.wait(Until.hasObject(By.text("10 م²")),10000))
       var found=false
       repeat(6){if(device.findObject(By.text("التكلفة غير مكتملة"))!=null)found=true else device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,20)}
       assertTrue(found);shot("incomplete-prices")
       device.findObject(By.text("تعديل المدخلات")).click()
       type("المساحة الصافية","0")
       device.findObject(By.text("احسب واعرض النتيجة")).click()
       device.waitForIdle();shot("invalid-input")
       assertTrue(device.wait(Until.hasObject(By.textContains("المساحة الصافية يجب")),10000))
    }
 }
 @Test fun utilityComputesAndSavesResult(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","convert")).use{
       assertTrue(device.wait(Until.hasObject(By.text("اسم الحساب")),10000))
       type("القيمة","1.25")
       device.findObject(By.text("احسب واعرض النتيجة")).click()
       assertTrue(device.wait(Until.hasObject(By.text("125 سم")),10000));shot("unit-result")
       device.findObject(By.text("حفظ النتيجة")).click()
       assertEquals("1.25",V8Repository(context).recentCalcs().first().inputs["value"])
    }
 }
 @Test fun pdfPrintPreviewOpens(){
    ActivityScenario.launch(MainActivity::class.java).use{scenario->
       assertTrue(device.wait(Until.hasObject(By.text("الرئيسية")),10000))
       scenario.onActivity{ReportExport.printPdf(it,sample(),"حصر وتكلفة تفصيلي")}
       assertTrue(device.wait(Until.hasObject(By.pkg("com.android.printspooler")),15000));shot("pdf-preview")
       device.pressBack()
    }
 }
 @Test fun everyCalculatorOpensOnDevice(){
    CalculatorLibrary.all.forEach{def->
        ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator",def.id)).use{
            assertTrue(def.id,device.wait(Until.hasObject(By.text("اسم الحساب")),10000))
        }
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

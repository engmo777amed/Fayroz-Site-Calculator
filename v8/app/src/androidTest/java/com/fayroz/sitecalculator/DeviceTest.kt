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
       assertText("الرئيسية");shot("home")
       clickText("الحاسبات")
       assertText("أقسام الحاسبات");shot("calculators")
       clickText("المونة والمحارة")
       clickText("مونة المحارة")
       assertText("البيانات المطلوبة");shot("mortar-calculator")
       device.pressBack();device.pressBack()
       clickText("المشروعات")
       assertText("اختبار الحصر")
       clickText("اختبار الحصر")
       assertText("الأماكن");shot("project");
       assertText("الدور الأول")
       clickText("الدور الأول")
       assertText("غرفة الاختبار");shot("section")
       clickText("غرفة الاختبار")
       clickText("المقاسات");assertText("اسم المكان");shot("room-input");clickText("المقاسات")
       device.waitForIdle()
       assertText("تعديل");shot("room-results")
       clickText("تعديل");device.waitForIdle();shot("part-editor");assertText("مواصفات التنفيذ")
       device.waitForIdle();shot("item-materials")
       clickText("كامل البند");clickText("جزء السقف (1)");device.waitForIdle()
       assertText("خلطة خاصة لهذا الجزء")
       compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn();compose.waitForIdle()
       type("متوسط السمك","20.5")
       clickText("السعر المستخدم / تغيير خاص");type("سعر شيكارة الأسمنت","250")
       assertText("حفظ البند");shot("item-result")
       assertText("6 م²")
       clickText("حفظ البند")
       clickText("حفظ");device.waitForIdle()
       val saved=V8Repository(context).loadProjects().single().sections[0].spaces[0].takeoffs[0]
       assertEquals(15.0,saved.material!!.thicknessMm,0.0)
       assertEquals(200.0,saved.material!!.cementPrice,0.0)
       assertEquals(20.5,saved.parts.single().material!!.thicknessMm,0.0)
       assertEquals(250.0,saved.parts.single().material!!.cementPrice,0.0)
    }
 }
 private fun field(label:String):SemanticsNodeInteraction {
    val matcher=hasSetTextAction() and (hasContentDescription("إدخال $label") or hasAnyAncestor(hasContentDescription("إدخال $label")))
    repeat(30){attempt->
        compose.waitForIdle()
        if(compose.onAllNodes(matcher,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()) {
            val node=compose.onNode(matcher,useUnmergedTree=true)
            runCatching{node.performScrollTo()}
            return node
        }
        device.swipe(device.displayWidth/2,if(attempt<15)device.displayHeight/3 else device.displayHeight*3/4,device.displayWidth/2,if(attempt<15)device.displayHeight*3/4 else device.displayHeight/3,20)
    }
    fail("Field missing: $label");throw IllegalStateException()
 }
 private fun calculate(){if(compose.onAllNodes(hasText("احسب واعرض النتيجة") and hasClickAction()).fetchSemanticsNodes().isNotEmpty())clickText("احسب واعرض النتيجة")else {
    val imePackage=device.executeShellCommand("settings get secure default_input_method").trim().substringBefore("/")
    if(imePackage.isNotBlank()&&device.hasObject(By.pkg(imePackage))){device.pressBack();device.waitForIdle()}
    assertText("حفظ النتيجة")
 }}
 private fun type(label:String,value:String){field(label).performTextReplacement(value);compose.waitForIdle();field(label).assertTextEquals(value)}
 private fun assertText(text:String){
    repeat(30){attempt->
        compose.waitForIdle()
        val nodes=compose.onAllNodesWithText(text,useUnmergedTree=true)
        if(nodes.fetchSemanticsNodes().isNotEmpty()){
            val node=nodes.onFirst()
            runCatching{node.performScrollTo()}
            node.assertExists();return
        }
        device.swipe(device.displayWidth/2,if(attempt<15)device.displayHeight/3 else device.displayHeight*3/4,device.displayWidth/2,if(attempt<15)device.displayHeight*3/4 else device.displayHeight/3,20)
        device.waitForIdle()
    }
    fail("Text missing after scrolling: $text")
 }
 private fun clickText(text:String){
    val imePackage=device.executeShellCommand("settings get secure default_input_method").trim().substringBefore("/")
    if(imePackage.isNotBlank()&&device.hasObject(By.pkg(imePackage))){device.pressBack();device.waitForIdle()}
    compose.waitForIdle()
    assertText(text)
    val node=compose.onAllNodes(hasText(text) and hasClickAction()).onLast()
    runCatching{node.performScrollTo()}
    node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick){assertTrue(it())};compose.waitForIdle()
 }
 @Test fun calculatorComputesSavesAndReopensDecimalInputs(){
    val intent=Intent(context,MainActivity::class.java).putExtra("calculator","plaster")
    ActivityScenario.launch<MainActivity>(intent).use{
       assertText("البيانات المطلوبة")
       type("المساحة الصافية","12.5")
       type("متوسط السمك","15.5")
       clickText("مم");clickText("سم")
       field("متوسط السمك").assertTextEquals("1.55")
       clickText("إعداداتي وتفاصيل إضافية");clickText("السعر المستخدم / تغيير خاص");type("سعر شيكارة الأسمنت","200")
       type("سعر متر الرمل","300")
       shot("calculator-filled")
       calculate();device.waitForIdle();shot("calculator-result")
       field("المساحة الصافية").assertTextEquals("12.5")
       clickText("اسم الحساب ومكان الحفظ");type("اسم الحساب","اختبار محارة 12.5")
       clickText("حفظ النتيجة")
       val saved=V8Repository(context).recentCalcs().first()
       assertEquals("12.5",saved.inputs["area"]);assertEquals("15.5",saved.inputs["thickness"]);assertTrue(saved.cost>0)
    }
    ActivityScenario.launch(MainActivity::class.java).use{
       clickText("المحفوظات")
       assertTrue(device.wait(Until.hasObject(By.textContains("اختبار محارة 12.5")),10000));shot("saved-calculations")
       device.findObject(By.textContains("اختبار محارة 12.5")).click()
       field("المساحة الصافية").assertTextEquals("12.5")
       
       field("المساحة الصافية").assertTextEquals("12.5")
       field("متوسط السمك").assertTextEquals("1.55")
    }
 }
 @Test fun quantitiesWorkWithoutPricesAndErrorsAreVisible(){
    V8Repository(context).setPref("calc.paint.area","10")
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","paint")).use{
       assertText("البيانات المطلوبة")
       type("المساحة الصافية","10")
       calculate()
       field("المساحة الصافية").assertTextEquals("10")
       assertText("التكلفة غير مكتملة");shot("incomplete-prices")
       
       type("المساحة الصافية","0")
       calculate()
       device.waitForIdle();shot("invalid-input")
       assertTrue(device.wait(Until.hasObject(By.textContains("المساحة الصافية يجب")),10000))
    }
 }
 @Test fun utilityComputesAndSavesResult(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","convert")).use{
       assertText("نوع التحويل")
       type("القيمة","1.25")
       calculate()
       assertText("125 سم");shot("unit-result")
       clickText("حفظ النتيجة")
       assertEquals("1.25",V8Repository(context).recentCalcs().first().inputs["value"])
    }
 }
 @Test fun pdfPrintPreviewOpens(){
    ActivityScenario.launch(MainActivity::class.java).use{scenario->
       assertText("الرئيسية")
       scenario.onActivity{ReportExport.printPdf(it,sample(),"حصر وتكلفة تفصيلي")}
       assertTrue(device.wait(Until.hasObject(By.pkg("com.android.printspooler")),15000));shot("pdf-preview")
       device.pressBack()
    }
 }
 @Test fun everyCalculatorOpensOnDevice(){
    CalculatorLibrary.all.forEach{def->
        ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator",def.id)).use{
            assertTrue(def.id,device.wait(Until.hasObject(By.text("البيانات المطلوبة")),10000))
        }
    }
 }
 @Test fun mortarVolumeModesShowPracticalResultsAndPersist(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","plaster")).use{
        clickText("خامات لمساحة");clickText("مكونات ١ م³ مونة")
        type("متوسط السمك","20")
        calculate()
        clickText("التفاصيل وطريقة الحساب");assertText("1 م³");assertText("50 م²");shot("mortar-one-cubic-meter")
        clickText("حفظ النتيجة")
        assertEquals("unit",V8Repository(context).recentCalcs().first().inputs["_mortarMode"])
        
        clickText("مكونات ١ م³ مونة");clickText("المونة المتاحة تفرد كام؟")
        type("حجم المونة","2");clickText("إعداداتي وتفاصيل إضافية");field("الهالك").assertTextEquals("5")
        calculate();assertText("95.238 م²");shot("mortar-coverage")
    }
 }
 @Test fun projectShoppingShowsBagsAndKeepsCostSeparate(){
    V8Repository(context).saveProjects(listOf(sample()))
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("المشروعات");clickText("اختبار الحصر");clickText("الخامات")
        assertText("طلب الخامات");assertText("1 شيكارة");shot("project-shopping")
        clickText("الأماكن");clickText("التكلفة")
        assertText("تكلفة الاستهلاك");shot("project-cost")
    }
 }
 @Test fun readyQuantitySavesNetAndCanReopenWithoutDimensions(){
    val project=sample().copy(sections=listOf(Section(name="الرئيسي")))
    V8Repository(context).saveProjects(listOf(project))
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("المشروعات");clickText("اختبار الحصر");clickText("كمية جاهزة")
        type("الكمية الجاهزة","100")
        clickText("الهالك والملاحظات — اختياري");type("الهالك","5")
        assertText("100 م²");assertText("105 م²");shot("ready-quantity")
        clickText("حفظ داخل المشروع")
        val space=V8Repository(context).loadProjects().single().sections.single().spaces.single()
        assertEquals(100.0,space.takeoffs.single().directValue,0.0)
        assertEquals(5.0,space.takeoffs.single().waste,0.0)
        clickText(space.name);assertText("البنود");shot("ready-quantity-reopened")
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

 @Test fun mortarModesRetainTheirOwnWaste(){
    val intent=Intent(context,MainActivity::class.java).putExtra("calculator","plaster")
    ActivityScenario.launch<MainActivity>(intent).use{
        type("المساحة الصافية","100");clickText("إعداداتي وتفاصيل إضافية");type("الهالك","8")
        clickText("خامات لمساحة");clickText("مكونات ١ م³ مونة")
        field("الهالك").assertTextEquals("0")
        clickText("مكونات ١ م³ مونة");clickText("خامات لمساحة")
        field("الهالك").assertTextEquals("8")
        calculate();clickText("التفاصيل وطريقة الحساب");assertText("الهالك المستخدم: 8%");shot("mortar-waste-preserved")
    }
 }
 @Test fun explicitlyIncludedCalculatorSavesOnceAndEntersProjectTotals(){
    val project=Project(name="اختبار إضافة الحساب",sections=listOf(Section(name="الرئيسي")))
    val repository=V8Repository(context);repository.saveProjects(listOf(project));repository.setActive(ActiveLocation(project.id))
    val intent=Intent(context,MainActivity::class.java).putExtra("calculator","paint")
    ActivityScenario.launch<MainActivity>(intent).use{
        type("المساحة الصافية","10")
        clickText("إعداداتي وتفاصيل إضافية");clickText("السعر المستخدم / تغيير خاص");type("سعر العبوة","100")
        calculate()
        clickText("اسم الحساب ومكان الحفظ")
        clickText("حساب مستقل");clickText("المشروع كله")
        assertText("إضافة الحساب لحصر وتكلفة وشراء المشروع")
        compose.onNode(isToggleable()).performClick()
        clickText("حفظ النتيجة");clickText("حفظ النتيجة")
        val updated=repository.loadProjects().single()
        assertEquals(1,updated.calculations.size);assertEquals(1,repository.recentCalcs().size)
        assertEquals(10.0,com.fayroz.sitecalculator.domain.CostEngine.rows(updated).single().quantity,0.0)
        assertEquals(21.0,com.fayroz.sitecalculator.domain.CostEngine.rows(updated).single().materialCost,1e-9)
        shot("included-calculation")
        clickText("المشروع كله");clickText("حساب مستقل");clickText("حفظ النتيجة")
        val reference=repository.loadProjects().single()
        assertEquals(1,reference.calculations.size)
        assertTrue(com.fayroz.sitecalculator.domain.CostEngine.rows(reference).isEmpty())
    }
 }
 @Test fun repricingScreenUpdatesStoredPriceWithoutChangingThickness(){
    val project=sample();val repository=V8Repository(context);repository.saveProjects(listOf(project));repository.setActive(ActiveLocation(project.id))
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("الأسعار")
        clickText("كل المشروعات — أسعار عامة");clickText(project.name)
        type("شيكارة أسمنت 50 كجم","300");type("متر الرمل","400")
        clickText("حفظ أسعار المشروع");clickText("إعادة تسعير البنود المحفوظة");clickText("إعادة التسعير")
        val spec=repository.loadProjects().single().sections.single().spaces.single().takeoffs.single().material!!
        assertEquals(300.0,spec.cementPrice,0.0);assertEquals(400.0,spec.sandPrice,0.0);assertEquals(15.0,spec.thicknessMm,0.0)
        shot("updated-prices")
    }
 }

 @Test fun deletingStandaloneCalculationRefreshesTheListImmediately(){
    val repository=V8Repository(context)
    repository.saveRecentCalc(SavedCalculation(toolId="rectangle",title="حساب للحذف",summary="مساحة 12 م²"))
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("المحفوظات");assertText("حساب للحذف")
        clickText("حذف");clickText("حذف")
        assertTrue(repository.recentCalcs().isEmpty())
        assertText("لا توجد حسابات محفوظة");shot("deleted-saved-calculation")
    }
 }

 @Test fun practicalMortarUsesBagsTemplatesAndQuickEditing(){
    val repo=V8Repository(context)
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","plaster")).use{
        type("المساحة الصافية","100");type("متوسط السمك","20");type("شكاير الأسمنت على متر الرمل","5")
        assertTrue(compose.onAllNodesWithText("جزء",substring=true).fetchSemanticsNodes().isEmpty())
        shot("practical-mortar-input")
        clickText("إعداداتي وتفاصيل إضافية");clickText("حفظ إعداداتي باسم");type("اسم الإعداد","محارة الموقع");clickText("حفظ الإعداد")
        assertEquals("5",repo.calculatorTemplates("plaster").single().second["_bagsPerSand"])
        assertFalse(repo.calculatorTemplates("plaster").single().second.containsKey("area"))
        calculate();shot("practical-mortar-result")
        ;type("المساحة الصافية","200");clickText("حفظ النتيجة")
        assertEquals(200.0,repo.recentCalcs().first().sourceQuantity,0.0)
        assertEquals("5",repo.recentCalcs().first().inputs["_bagsPerSand"])
    }
 }
 @Test fun areaDimensionsAndAvailableMaterialsArePractical(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","plaster")).use{
        clickText("من الطول والعرض")
        type("طول المسطح","5");type("عرض المسطح","4");type("شكاير الأسمنت على متر الرمل","5")
        calculate();clickText("حفظ النتيجة")
        assertEquals(20.0,V8Repository(context).recentCalcs().first().sourceQuantity,0.0)
        ;clickText("خامات لمساحة");clickText("الرمل والأسمنت الموجودين يكفوا كام؟")
        type("الرمل المتاح","2");type("شكاير الأسمنت المتاحة","5")
        calculate();assertText("1 م³");shot("available-materials")
    }
 }

 @Test fun recentSyncDoesNotReorderUnchangedCalculations(){
    val repo=V8Repository(context)
    val a=SavedCalculation(id="a",toolId="points",title="a",summary="")
    val z=a.copy(id="z",title="z")
    repo.saveRecentCalc(z);repo.saveRecentCalc(a)
    repo.syncCalculations(listOf(a,z),listOf(a,z))
    assertEquals(listOf("a","z"),repo.recentCalcs().map{it.id})
    repo.syncCalculations(listOf(a,z),listOf(a));assertEquals(listOf("a"),repo.recentCalcs().map{it.id})
 }
 @Test fun projectCopyPreservesCalculationsAndScopes(){
    val p=sample();val section=p.sections.single();val space=section.spaces.single()
    val d=CalculatorLibrary.all.first{it.id=="points"}
    val raw=d.fields.associate{it.key to it.default}+mapOf("count" to "3","price" to "100","_includeInProject" to "true")
    val source=p.copy(calculations=listOf(SavedCalculation(toolId="points",title="نقاط",summary="",sectionId=section.id,spaceId=space.id,inputs=raw)))
    val copy=com.fayroz.sitecalculator.ui.copyProject(source)
    assertNotEquals(source.id,copy.id);assertNotEquals(source.calculations.single().id,copy.calculations.single().id)
    assertEquals(copy.sections.single().id,copy.calculations.single().sectionId)
    assertEquals(copy.sections.single().spaces.single().id,copy.calculations.single().spaceId)
    assertEquals(com.fayroz.sitecalculator.domain.CostEngine.rows(source).sumOf{it.total},com.fayroz.sitecalculator.domain.CostEngine.rows(copy).sumOf{it.total},1e-9)
 }
 @Test fun restoreClearsStaleDrafts(){
    val repo=V8Repository(context);val original=sample();repo.saveProjects(listOf(original));val backup=repo.exportBackup()
    repo.saveDraft("room",original.sections.single().spaces.single().copy(name="مسودة قديمة"))
    repo.saveCalculatorDraft("tile",mapOf("area" to "999"))
    assertTrue(repo.importBackup(backup));assertNull(repo.loadDraft("room"));assertTrue(repo.calculatorDraft("tile").isEmpty())
 }
 @Test fun utilityRepeatedSaveUpdatesOneRecord(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","convert")).use{
        type("القيمة","10");calculate();clickText("حفظ النتيجة");clickText("حفظ النتيجة")
        assertEquals(1,V8Repository(context).recentCalcs().size)
    }
 }

 @Test fun mortarWasteAndUnfinishedInputsSurviveLeavingCalculator(){
    val intent=Intent(context,MainActivity::class.java).putExtra("calculator","plaster")
    ActivityScenario.launch<MainActivity>(intent).use{
        type("المساحة الصافية","23.5");type("شكاير الأسمنت على متر الرمل","6.5")
        clickText("إعداداتي وتفاصيل إضافية");type("الهالك","8")
        device.pressBack();compose.waitForIdle()
    }
    ActivityScenario.launch<MainActivity>(intent).use{
        field("المساحة الصافية").assertTextEquals("23.5");field("شكاير الأسمنت على متر الرمل").assertTextEquals("6.5")
        clickText("إعداداتي وتفاصيل إضافية");field("الهالك").assertTextEquals("8")
        clickText("خامات لمساحة");clickText("مكونات ١ م³ مونة");calculate();clickText("حفظ النتيجة")
        device.pressBack();device.pressBack();compose.waitForIdle()
    }
    ActivityScenario.launch<MainActivity>(intent).use{
        clickText("إعداداتي وتفاصيل إضافية");field("الهالك").assertTextEquals("8")
    }
 }

 @Test fun centralPricesWorkWithoutAProjectAndDoNotRewriteHistory(){
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("الأسعار");type("شيكارة أسمنت 50 كجم","200");type("متر الرمل","300");clickText("حفظ الأسعار العامة")
        assertEquals("200",V8Repository(context).centralPrices()["cementPrice"])
        device.pressBack();clickText("الحاسبات");clickText("المونة والمحارة");clickText("مونة المحارة")
        type("المساحة الصافية","100");calculate();clickText("حفظ النتيجة")
        val saved=V8Repository(context).recentCalcs().single();assertEquals(200.0,saved.inputs["cementPrice"]!!.toDouble(),0.0);assertTrue(saved.cost>0)
        V8Repository(context).saveCentralPrices(mapOf("cementPrice" to "999","sandPrice" to "999"))
        assertEquals(200.0,V8Repository(context).recentCalcs().single().inputs["cementPrice"]!!.toDouble(),0.0)
        shot("central-prices-mortar")
    }
 }
 @Test fun calculatorEditsUpdateResultInPlace(){
    ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).putExtra("calculator","plaster")).use{
        type("المساحة الصافية","100");calculate()
        assertTrue(compose.onAllNodesWithText("تعديل المدخلات").fetchSemanticsNodes().isEmpty())
        type("متوسط السمك","20");clickText("حفظ النتيجة")
        val c=V8Repository(context).recentCalcs().single();assertEquals("20",c.inputs["thickness"])
        assertEquals(100.0,c.sourceQuantity,0.0);shot("single-page-calculator")
    }
 }
 @Test fun projectOpensOnPlacesAndRoomHasOneSaveAction(){
    V8Repository(context).saveProjects(listOf(sample()))
    ActivityScenario.launch(MainActivity::class.java).use{
        clickText("المشروعات");clickText("اختبار الحصر");assertText("غرفة الاختبار");shot("project-workspace")
        clickText("غرفة الاختبار");assertText("البنود");assertText("تعديل")
        assertEquals(1,compose.onAllNodes(hasText("حفظ") and hasClickAction()).fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodesWithText("التالي").fetchSemanticsNodes().isEmpty());shot("single-page-room")
    }
 }
}


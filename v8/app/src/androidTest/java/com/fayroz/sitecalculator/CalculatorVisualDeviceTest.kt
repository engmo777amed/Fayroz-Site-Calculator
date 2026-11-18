package com.fayroz.sitecalculator

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.fayroz.sitecalculator.data.V8Repository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import androidx.test.uiautomator.By
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CalculatorVisualDeviceTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun prepare(){
        context.getSharedPreferences("fayroz_site_v8",android.content.Context.MODE_PRIVATE).edit().clear().commit()
        V8Repository(context).setAppearance("dark")
        dismissSystemDialog()
    }
    private fun dismissSystemDialog(){
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        if(device.findObject(By.text("System UI isn't responding"))!=null)device.findObject(By.text("Wait"))?.click()
    }
    private fun revealNode(matcher:SemanticsMatcher):SemanticsNodeInteraction{
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(matcher)
        return compose.onNode(matcher).assertIsDisplayed()
    }
    private fun reveal(text:String){revealNode(hasText(text))}
    private fun launch(tool:String?=null)=ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java).apply{tool?.let{putExtra("calculator",it)}})
    private fun shot(name:String){val f=File(context.getExternalFilesDir(null),"screenshots/v98-$name.png");f.parentFile!!.mkdirs();assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(f))}
    private fun captureApp(name:String){
        val file=File(context.getExternalFilesDir(null),"screenshots/v991-app-$name.png")
        file.parentFile!!.mkdirs()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
    }
    @Test fun startsAtCalculatorSections(){launch().use{compose.onNodeWithText("أقسام الحاسبات").assertIsDisplayed();captureApp("sections")}}
    @Test fun rampIsVisibleFromCatalogBeforeEnteringDimensions(){
        launch().use {
            compose.onNodeWithText("القياسات والمناسيب").performClick()
            revealNode(hasText("ميل مدخل الجراج")).performClick()
            compose.onNodeWithContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية").assertIsDisplayed()
            compose.onNodeWithText("المجسم يستخدم أبعاد عرض توضيحية للخانات الفارغة. عدّلها من المدخلات.").assertIsDisplayed()
            captureApp("ramp-first-open")
        }
    }
    @Test fun rampNumbersUpdateDrawingAndPersist(){
        val repo=V8Repository(context);repo.clearCalculatorDraft("garage_ramp.independent")
        launch("garage_ramp").use{
            revealNode(hasContentDescription("إدخال فرق المنسوب نزولًا")).performTextReplacement("2")
            compose.onNodeWithText("المعاينة").performClick()
            compose.onNodeWithContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية").assertIsDisplayed()
            dismissSystemDialog()
            compose.onNodeWithText("احسب واعرض النتيجة").performClick()
            shot("ramp-after-calculate")
            compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("20 %"));assertTrue(compose.onAllNodesWithText("20 %").fetchSemanticsNodes().isNotEmpty());compose.waitForIdle();shot("ramp-result")
            compose.onNodeWithText("المعاينة").performClick()
            compose.onNodeWithContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية").assertIsDisplayed()
            shot("ramp-diagram")
            compose.onNodeWithText("حفظ النتيجة").performClick()
        }
        val saved=repo.recentCalcs().first{it.toolId=="garage_ramp"}
        assertEquals("2",saved.inputs["rise"])
        assertTrue(saved.summary.contains("20"))
    }
    @Test fun rampModeHidesSolvedDimension(){launch("garage_ramp").use{
        reveal("طريقة حساب الرامب")
        compose.onNodeWithText("الميل من فرق المنسوب والطول").performClick()
        compose.onNodeWithText("الطول من فرق المنسوب والميل").performClick()
        compose.onAllNodesWithContentDescription("إدخال الطول الأفقي الرئيسي").assertCountEquals(0)
        revealNode(hasContentDescription("إدخال الميل الرئيسي")).assertIsDisplayed()
        shot("ramp-mode")
    }}
    @Test fun rampSolidRespondsToEditsAndCameraButtons(){launch("garage_ramp").use{
        revealNode(hasContentDescription("إدخال عرض الرامب للمجسم")).performTextReplacement("3")
        revealNode(hasContentDescription("إدخال سمك البلاطة الرأسي للمجسم")).performTextReplacement("20")
        revealNode(hasContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية"))
        revealNode(hasText("علوي")).performClick()
        revealNode(hasText("منظور")).performClick()
        revealNode(hasContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية"))
        captureApp("ramp-solid")
    }}
    @Test fun sieveShowsLabImageAndMeasuredCurve(){
        V8Repository(context).saveCalculatorDraft("sieve.independent",mapOf(
            "sampleMass" to "100","_rows" to "5",
            "_row.0.size" to "9.5","_row.0.mass" to "0",
            "_row.1.size" to "4.75","_row.1.mass" to "11",
            "_row.2.size" to "2.36","_row.2.mass" to "27",
            "_row.3.size" to "0.6","_row.3.mass" to "30",
            "_row.4.size" to "0","_row.4.mass" to "32"
        ))
        launch("sieve").use {
            revealNode(hasContentDescription("صورة توضيحية لمناخل معمل التدرج الحبيبي"))
            revealNode(hasContentDescription("نسبة المار (%) حسب فتحة المنخل (مم) · مقياس لوغاريتمي"))
            captureApp("sieve")
            compose.onNodeWithText("احسب واعرض النتيجة").performClick()
            reveal("62 %")
        }
    }
    @Test fun moistureCalculationDisplaysKnownAnswer(){launch("soil_moisture").use{
        compose.onNodeWithText("احسب واعرض النتيجة").performClick()
        reveal("كتلة الماء 20 جم ÷ كتلة التربة الجافة 100 جم × ١٠٠")
        assertTrue(compose.onAllNodesWithText("20 %").fetchSemanticsNodes().isNotEmpty())
        captureApp("moisture")
    }}
}

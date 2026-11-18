package com.fayroz.sitecalculator

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
    @Test fun startsAtCalculatorSections(){launch().use{compose.onNodeWithText("أقسام الحاسبات").assertIsDisplayed();shot("sections")}}
    @Test fun rampNumbersUpdateDrawingAndPersist(){
        val repo=V8Repository(context);repo.clearCalculatorDraft("garage_ramp.independent")
        launch("garage_ramp").use{
            compose.onNodeWithContentDescription("إدخال فرق المنسوب نزولًا").performScrollTo().performTextReplacement("2")
            reveal("الرسم التوضيحي المتغير")
            dismissSystemDialog()
            compose.onNodeWithText("احسب واعرض النتيجة").performClick()
            shot("ramp-after-calculate")
            reveal("20 %");compose.waitForIdle();shot("ramp-result")
            reveal("الرسم التوضيحي المتغير");shot("ramp-diagram")
            compose.onNodeWithText("حفظ النتيجة").performClick()
        }
        val saved=repo.recentCalcs().first{it.toolId=="garage_ramp"}
        assertEquals("2",saved.inputs["rise"])
        assertTrue(saved.summary.contains("20"))
    }
    @Test fun rampModeHidesSolvedDimension(){launch("garage_ramp").use{
        compose.onNodeWithText("طريقة حساب الرامب").performScrollTo()
        compose.onNodeWithText("الميل من فرق المنسوب والطول").performClick()
        compose.onNodeWithText("الطول من فرق المنسوب والميل").performClick()
        compose.onAllNodesWithContentDescription("إدخال الطول الأفقي الرئيسي").assertCountEquals(0)
        compose.onNodeWithContentDescription("إدخال الميل الرئيسي").performScrollTo().assertIsDisplayed()
        shot("ramp-mode")
    }}
    @Test fun rampSolidRespondsToEditsAndCameraButtons(){launch("garage_ramp").use{
        compose.onNodeWithContentDescription("إدخال عرض الرامب للمجسم").performScrollTo().performTextReplacement("3")
        compose.onNodeWithContentDescription("إدخال سمك البلاطة الرأسي للمجسم").performScrollTo().performTextReplacement("20")
        revealNode(hasContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية"))
        revealNode(hasText("علوي")).performClick()
        revealNode(hasText("منظور")).performClick()
        revealNode(hasContentDescription("مجسم ثلاثي الأبعاد من المدخلات الحالية"))
        shot("ramp-solid")
    }}
    @Test fun moistureCalculationDisplaysKnownAnswer(){launch("soil_moisture").use{
        compose.onNodeWithText("احسب واعرض النتيجة").performClick()
        reveal("20 %");shot("moisture")
    }}
}

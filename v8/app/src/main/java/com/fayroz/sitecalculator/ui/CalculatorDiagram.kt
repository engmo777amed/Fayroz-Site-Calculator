package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*

private val visualExamples = mapOf(
    "garage_ramp" to mapOf("rampWidth" to "3","rampThickness" to "20"),
    "tile_layout" to mapOf("tileThickness" to "10"),
    "sieve" to mapOf("sampleMass" to "100","_rows" to "5",
        "_row.0.size" to "9.5","_row.0.mass" to "0",
        "_row.1.size" to "4.75","_row.1.mass" to "11",
        "_row.2.size" to "2.36","_row.2.mass" to "27",
        "_row.3.size" to "0.6","_row.3.mass" to "30",
        "_row.4.size" to "0","_row.4.mass" to "32"),
    "proctor" to mapOf("moldVolume" to "1000","_rows" to "3",
        "_row.0.moisture" to "8","_row.0.mass" to "1944",
        "_row.1.moisture" to "10","_row.1.mass" to "2200",
        "_row.2.moisture" to "12","_row.2.mass" to "2128"),
)

/** Project models use calculator inputs. Reference scenes use independent demonstration geometry. */
@Composable
fun CalculatorDiagram(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer?,update:(String,String)->Unit){
    val id=def.id
    val dims=def.fields.filter{diagramInputVisible(id,it.key,raw)&&it.unit in setOf("م","سم","مم","%","عدد","وجه","درجة","طبقة","شبكة","موصل")&&!it.key.contains("price",true)}
    var editing by remember(id){mutableStateOf<CalcField?>(null)}
    BoxCard{
        val displayable=dims.all{(MortarMix.number(raw[it.key])?:0.0).let{v->kotlin.math.abs(v)<=1e8&&(v==0.0||kotlin.math.abs(v)>=1e-8)}}
        val modelInput=remember(id,raw) {
            if(id in setOf("garage_ramp","tile_layout")) {
                raw + visualExamples.getValue(id).filterKeys { raw[it].isNullOrBlank() }
            } else raw
        }
        val solid=remember(id,modelInput,answer,displayable) {
            if(answer==null || !displayable || id !in CalculatorGeometry.supported) null
            else CalculatorGeometry.create(id,modelInput,answer)
        }
        val reference=remember(id,solid!=null) { if(solid==null) CalculatorReferenceScenes.forTool(id) else null }
        val chartExample=remember(id,answer) {
            if(answer==null && id in setOf("sieve","proctor")) {
                val data=visualExamples.getValue(id)
                runCatching { CalculatorLibrary.evaluate(def,data) }.getOrNull()?.let { data }
            } else null
        }
        Text("المجسم ثلاثي الأبعاد",style=MaterialTheme.typography.titleSmall)
        if(solid!=null && answer!=null) {
            if(modelInput!=raw) Text("المجسم يستخدم أبعاد عرض توضيحية للخانات الفارغة. عدّلها من المدخلات.",
                style=MaterialTheme.typography.bodySmall)
            CalculatorSolidView(solid,answer,isRamp=id=="garage_ramp")
        } else if(reference!=null) {
            CalculatorSolidView(reference,isRamp=id=="garage_ramp")
        }
        if(id=="sieve") SieveLabIllustration()
        when {
            id in setOf("sieve","proctor") && answer!=null -> {
                MeasurementChart(id,raw)
                answer.outputs.firstOrNull { id!="sieve" || "الوعاء" !in it.label }?.let {
                    VisualMetric(it.label,"${fmt(it.value)} ${it.unit}",Modifier.fillMaxWidth())
                }
            }
            chartExample!=null -> {
                Text("منحنى بقراءات مثال. أدخل قياساتك لرسم منحنى المشروع.",style=MaterialTheme.typography.bodySmall)
                MeasurementChart(id,chartExample)
            }
            solid==null && answer!=null -> CalculationFlow(def,raw,answer)
            answer==null -> Text("أكمل المدخلات لعرض نتيجة المشروع.",style=MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            dims.forEach{f->OutlinedButton(onClick={editing=f}){Text("${f.label}: ${raw[f.key].orEmpty()} ${f.unit}")}}
        }

    }
    editing?.let{f->AlertDialog(onDismissRequest={editing=null},title={Text("تعديل ${f.label}")},text={CalcInputField(f,raw[f.key].orEmpty(),{update(f.key,it)})},confirmButton={TextButton(onClick={editing=null}){Text("تم")}})}
}

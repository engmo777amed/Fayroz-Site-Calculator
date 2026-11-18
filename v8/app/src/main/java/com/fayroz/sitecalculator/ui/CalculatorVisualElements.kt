package com.fayroz.sitecalculator.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.fayroz.sitecalculator.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.CalcAnswer
import com.fayroz.sitecalculator.domain.CalcDef
import com.fayroz.sitecalculator.domain.ExtraCalculators
import com.fayroz.sitecalculator.domain.MortarMix
import kotlin.math.log10

@Composable
internal fun VisualMetric(label: String,value: String,modifier: Modifier=Modifier) {
    Surface(modifier,shape=RoundedCornerShape(12.dp),color=Color(0xFF123B43),contentColor=Color(0xFFE3F4F2)) {
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(label,style=MaterialTheme.typography.labelMedium)
            Text(value,color=Color(0xFF59E3D4),style=MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun CalculationFlow(def: CalcDef,raw: Map<String,String>,answer: CalcAnswer) {
    if(def.id=="soil_moisture") {
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("tare" to "العلبة", "wet" to "العلبة والرطب", "dry" to "العلبة والجاف").forEach { (id,label) ->
                VisualMetric(label,"${fmt(MortarMix.number(raw[id]) ?: 0.0)} جم",Modifier.weight(1f))
            }
        }
        val water=(MortarMix.number(raw["wet"])?:0.0)-(MortarMix.number(raw["dry"])?:0.0)
        val soil=(MortarMix.number(raw["dry"])?:0.0)-(MortarMix.number(raw["tare"])?:0.0)
        Text("كتلة الماء ${fmt(water)} جم ÷ كتلة التربة الجافة ${fmt(soil)} جم × ١٠٠")
    } else {
        val inputs=def.fields.filter { ExtraCalculators.activeField(def.id,it.key,raw) && !it.key.contains("price",true) }
            .mapNotNull { f -> MortarMix.number(raw[f.key])?.let { f.label to "${fmt(it)} ${f.unit}" } }.take(3)
        inputs.chunked(2).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                row.forEach { (label,value)->VisualMetric(label,value,Modifier.weight(1f)) }
            }
        }
        Text(def.formula,style=MaterialTheme.typography.bodyMedium)
    }
    val results=answer.outputs.take(3)
    results.forEach { VisualMetric(it.label,"${fmt(it.value)} ${it.unit}",Modifier.fillMaxWidth()) }
}

@Composable
internal fun SieveLabIllustration() {
    Image(
        painterResource(R.drawable.sieve_lab_reference),
        contentDescription="صورة توضيحية لمناخل معمل التدرج الحبيبي",
        modifier=Modifier.fillMaxWidth().height(122.dp).clip(RoundedCornerShape(12.dp)),
        contentScale=ContentScale.Crop
    )
}

@Composable
internal fun MeasurementChart(id: String,raw: Map<String,String>) {
    val rows=ExtraCalculators.tableValues(id,raw)
    val sieve=id=="sieve"
    var cumulative=0.0
    val points=if(sieve) {
        val mass=MortarMix.number(raw["sampleMass"]) ?: return
        rows.mapNotNull { row ->
            cumulative+=row.getValue("mass")
            row.getValue("size").takeIf { it>0 }?.let { it to (100-cumulative/mass*100).coerceIn(0.0,100.0) }
        }.sortedBy { it.first }
    } else {
        val volume=MortarMix.number(raw["moldVolume"]) ?: return
        rows.map { it.getValue("moisture") to (it.getValue("mass")/volume/(1+it.getValue("moisture")/100)) }.sortedBy { it.first }
    }
    if(points.isEmpty()) return
    val xLabel=if(sieve)"فتحة المنخل (مم) · مقياس لوغاريتمي" else "محتوى الرطوبة (%)"
    val yLabel=if(sieve)"نسبة المار (%)" else "الكثافة الجافة (جم/سم³)"
    Text(yLabel,style=MaterialTheme.typography.labelLarge)
    Canvas(Modifier.fillMaxWidth().height(190.dp).semantics{contentDescription="$yLabel حسب $xLabel"}) {
        drawRect(Color(0xFF082B37))
        val minY=if(sieve)0.0 else (points.minOf{it.second}*.96)
        val maxY=if(sieve)100.0 else (points.maxOf{it.second}*1.04)
        fun x(v:Double)=if(sieve)log10(v) else v
        val minX=x(points.first().first);val maxX=x(points.last().first)
        val left=48.dp.toPx(); val right=size.width-21.dp.toPx()
        val top=16.dp.toPx();val bottom=size.height-25.dp.toPx()
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=0xFFE1F1F0.toInt();textSize=10.dp.toPx() }
        fun py(v:Double)=bottom-((v-minY)/(maxY-minY)).toFloat()*(bottom-top)
        fun px(v:Double)=if(maxX==minX)(left+right)/2 else left+((x(v)-minX)/(maxX-minX)).toFloat()*(right-left)
        for(i in 0..4) {
            val value=minY+(maxY-minY)*i/4
            val y=py(value)
            drawLine(Color(0xFF39606B),Offset(left,y),Offset(right,y),1f)
            drawContext.canvas.nativeCanvas.drawText(fmt(value),4.dp.toPx(),y+3.dp.toPx(),text)
        }
        text.textAlign=Paint.Align.CENTER
        val stride=(points.size/5).coerceAtLeast(1)
        var lastLabelX=-1000f
        points.forEachIndexed { index,(value,_) ->
            val x=px(value)
            if(index%stride==0 && x-lastLabelX>=37.dp.toPx()) {
                lastLabelX=x
                drawContext.canvas.nativeCanvas.drawText(fmt(value),x,bottom+16.dp.toPx(),text)
            }
        }
        val path=Path()
        points.forEachIndexed { i,(x,y) -> if(i==0)path.moveTo(px(x),py(y)) else path.lineTo(px(x),py(y)) }
        val line=Color(0xFF33DFCB)
        drawPath(path,line,style=androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
        points.forEach{(x,y)->drawCircle(line,3.5.dp.toPx(),Offset(px(x),py(y)))}
    }
    Text(xLabel,style=MaterialTheme.typography.labelMedium)
}

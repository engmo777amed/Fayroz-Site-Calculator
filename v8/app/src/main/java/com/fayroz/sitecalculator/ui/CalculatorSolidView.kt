package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*
import kotlin.math.*

private data class ProjectedFace(val face: Face3,val points: List<Offset>,val depth: Double) {
    fun hit(point: Offset): Boolean {
        var inside=false
        points.indices.forEach { i ->
            val a=points[i]
            val b=points[(i+1)%points.size]
            if((a.y>point.y)!=(b.y>point.y) && point.x<(b.x-a.x)*(point.y-a.y)/(b.y-a.y)+a.x) inside=!inside
        }
        return inside
    }
}

private fun project(solid: CalculatorSolid,width: Float,height: Float,yaw: Double,pitch: Double,zoom: Float): List<ProjectedFace> {
    val points=solid.faces.flatMap { it.points }
    if(points.isEmpty()) return emptyList()
    val min=Point3(points.minOf{it.x},points.minOf{it.y},points.minOf{it.z})
    val max=Point3(points.maxOf{it.x},points.maxOf{it.y},points.maxOf{it.z})
    val center=Point3((min.x+max.x)/2,(min.y+max.y)/2,(min.z+max.z)/2)
    val diagonal=hypot(hypot(max.x-min.x,max.y-min.y),max.z-min.z)
    if(!diagonal.isFinite() || diagonal<=0) return emptyList()
    val c=cos(yaw);val s=sin(yaw);val cp=cos(pitch);val sp=sin(pitch)
    val scale=min(width,height)*.96f*zoom
    return solid.faces.map { face ->
        val camera=face.points.map {
            val p=it-center
            val u=(p.x*c-p.y*s)/diagonal
            val v=(p.x*s+p.y*c)/diagonal
            val z=p.z/diagonal
            Point3(u,z*cp-v*sp,v*cp+z*sp)
        }
        ProjectedFace(face,camera.map { p ->
            val perspective=1.0/(1.0-p.z*.33)
            Offset(width/2+(p.x*perspective*scale).toFloat(),height/2-(p.y*perspective*scale).toFloat())
        },camera.map { it.z }.average())
    }.sortedBy { it.depth }
}

@Composable
internal fun CalculatorSolidView(solid: CalculatorSolid,answer: CalcAnswer) {
    var yaw by remember { mutableDoubleStateOf(-.65) }
    var pitch by remember { mutableDoubleStateOf(.60) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var shell by remember { mutableStateOf(true) }
    var liquid by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<String?>(null) }
    val hasTank=solid.faces.any { it.surface==Surface3.Tank }
    val hasWater=solid.faces.any { it.surface==Surface3.Water }
    val visible=remember(solid,shell,liquid) {
        solid.copy(faces=solid.faces.filter { (shell||it.surface!=Surface3.Tank)&&(liquid||it.surface!=Surface3.Water) })
    }
    val currentShape by rememberUpdatedState(visible)
    val transform=rememberTransformableState { magnification, pan, rotation ->
        zoom=(zoom*magnification).coerceIn(.7f,4f)
        yaw+=pan.x*.008+rotation*.012
        pitch=(pitch+pan.y*.008).coerceIn(-PI/2,PI/2)
    }
    val outline=MaterialTheme.colorScheme.onSurface
    val previewBackground=MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(220.dp)
        .semantics { contentDescription="مجسم ثلاثي الأبعاد من المدخلات الحالية" }
        .transformable(transform)
        .pointerInput(Unit) {
            detectTapGestures { point ->
                val hit=project(currentShape,size.width.toFloat(),size.height.toFloat(),yaw,pitch,zoom)
                    .lastOrNull { it.face.surface!=Surface3.Tank && it.hit(point) }
                selected=hit?.face?.part
            }
        }) {
        drawRect(previewBackground)
        project(visible,size.width,size.height,yaw,pitch,zoom).forEach { projected ->
            val face=projected.face
            val path=Path().apply {
                moveTo(projected.points.first().x,projected.points.first().y)
                projected.points.drop(1).forEach { lineTo(it.x,it.y) }
                close()
            }
            val base=when(face.surface) {
                Surface3.Concrete -> Color(0xFF9AA5AB)
                Surface3.Excavation -> Color(0xFFBD8A58)
                Surface3.Tank -> Color(0xFF7AA5BD)
                Surface3.Water -> Color(0xFF239DBF)
                Surface3.Tile -> Color(0xFFDEBC91)
            }
            val normal=(face.points[1]-face.points[0]).cross(face.points[2]-face.points[0])
            val norm=sqrt(normal.dot(normal))
            val light=if(norm>0) abs(normal.dot(Point3(-.36,-.48,.8))/norm).toFloat().coerceIn(0f,1f) else 1f
            val fill=lerp(base,Color.Black,(1-light)*.42f)
            val highlighted=selected==face.part
            val alpha=if(face.surface==Surface3.Tank) .13f else 1f
            drawPath(path,(if(highlighted)lerp(fill,Color(0xFFF5BD51),.65f)else fill).copy(alpha=alpha))
            drawPath(path,outline.copy(alpha=if(face.surface==Surface3.Tank).12f else .38f),
                style=Stroke(if(highlighted)2.dp.toPx()else .65.dp.toPx()))
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        TextButton(onClick={yaw=-.65;pitch=.60;zoom=1f;shell=true;liquid=true}){Text("منظور")}
        TextButton(onClick={yaw=0.0;pitch=PI/2;zoom=1f}){Text("علوي")}
        TextButton(onClick={yaw=0.0;pitch=0.0;zoom=1f}){Text("جانبي")}
        if(hasTank) TextButton(onClick={yaw=0.0;pitch=0.0;zoom=1f;shell=false;liquid=true}){Text("كشف السائل")}
        if(hasTank) FilterChip(selected=shell,onClick={shell=!shell},label={Text("حدود الخزان")})
        if(hasWater) FilterChip(selected=liquid,onClick={liquid=!liquid},label={Text("السائل")})
    }
    val selectedPoints=visible.faces.filter { it.part==selected }.flatMap { it.points }
    if(selectedPoints.isNotEmpty()) {
        val width=selectedPoints.maxOf{it.x}-selectedPoints.minOf{it.x}
        val depth=selectedPoints.maxOf{it.y}-selectedPoints.minOf{it.y}
        val height=selectedPoints.maxOf{it.z}-selectedPoints.minOf{it.z}
        Text(selected.orEmpty(),style=MaterialTheme.typography.labelLarge)
        Text("امتداد الجزء: ${fmt(width)} × ${fmt(depth)} × ${fmt(height)} م",style=MaterialTheme.typography.labelMedium)
    }
    if(solid.note.isNotBlank()) Text(solid.note,style=MaterialTheme.typography.bodySmall)
    val results=if(hasTank)answer.outputs.filter{it.label in setOf("سعة كلية","حجم السائل","حجم فارغ","حجم مفيد")}else answer.outputs.take(3)
    results.forEach { Text("${it.label}: ${fmt(it.value)} ${it.unit}",style=MaterialTheme.typography.labelMedium) }
}

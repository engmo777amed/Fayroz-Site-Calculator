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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
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
internal fun CalculatorSolidView(solid: CalculatorSolid,answer: CalcAnswer,isRamp: Boolean=false) {
    val initialYaw=if(isRamp).65 else -.65
    val initialZoom=if(isRamp)1.14f else 1f
    var yaw by remember { mutableDoubleStateOf(initialYaw) }
    var pitch by remember { mutableDoubleStateOf(.60) }
    var zoom by remember { mutableFloatStateOf(initialZoom) }
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
    val studio=Brush.verticalGradient(listOf(Color(0xFF123D48),Color(0xFF061C26)))
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
        drawRect(studio)
        val projectedFaces=project(visible,size.width,size.height,yaw,pitch,zoom)
        if(projectedFaces.isNotEmpty() && abs(pitch)<1.24) {
            val all=projectedFaces.flatMap { it.points }
            val span=(all.maxOf{it.x}-all.minOf{it.x}).coerceAtMost(size.width)
            val baseline=(all.maxOf{it.y}+8.dp.toPx()).coerceAtMost(size.height*.94f)
            drawOval(Color.Black.copy(alpha=.23f),Offset(center.x-span*.48f,baseline-8.dp.toPx()),Size(span*.96f,19.dp.toPx()))
            drawOval(Color.Black.copy(alpha=.12f),Offset(center.x-span*.54f,baseline-12.dp.toPx()),Size(span*1.08f,27.dp.toPx()))
        }
        projectedFaces.forEach { projected ->
            val face=projected.face
            val path=Path().apply {
                moveTo(projected.points.first().x,projected.points.first().y)
                projected.points.drop(1).forEach { lineTo(it.x,it.y) }
                close()
            }
            val base=when(face.surface) {
                Surface3.Concrete -> Color(0xFFB7ACA0)
                Surface3.Excavation -> Color(0xFFBD8A58)
                Surface3.Tank -> Color(0xFF7AA5BD)
                Surface3.Water -> Color(0xFF32AEBA)
                Surface3.Tile -> Color(0xFFDEBC91)
                Surface3.Steel -> Color(0xFFA8B8BD)
                Surface3.Insulation -> Color(0xFFB4BE5D)
                Surface3.Plywood -> Color(0xFFD0A169)
            }
            val normal=(face.points[1]-face.points[0]).cross(face.points[2]-face.points[0])
            val norm=sqrt(normal.dot(normal))
            val light=if(norm>0) abs(normal.dot(Point3(-.36,-.48,.8))/norm).toFloat().coerceIn(0f,1f) else 1f
            val fill=lerp(base,Color.Black,(1-light)*.48f)
            val highlighted=selected==face.part
            val alpha=if(face.surface==Surface3.Tank) .24f else 1f
            val material=if(highlighted)lerp(fill,Color(0xFFF5BD51),.50f)else fill
            val top=projected.points.minOf { it.y }
            val bottom=projected.points.maxOf { it.y }
            val low=material.copy(alpha=alpha)
            val high=lerp(material,Color.White,if(face.surface==Surface3.Water).32f else .18f).copy(alpha=alpha)
            if(bottom-top>1f) drawPath(path,Brush.verticalGradient(listOf(high,low),top,bottom))
            else drawPath(path,low)
            if(face.surface in setOf(Surface3.Concrete,Surface3.Excavation,Surface3.Plywood) && face.points.size==4 &&
                projectedFaces.size<=160 && (bottom-top)>8.dp.toPx()) {
                val a=projected.points[0]
                val x=projected.points[1]-a
                val y=projected.points[3]-a
                val area=abs(x.x*y.y-x.y*y.x)
                val samples=(area/95).toInt().coerceIn(0,420)
                val random=kotlin.random.Random(face.points.hashCode())
                clipPath(path) {
                    repeat(samples) {
                        val spot=a+x*random.nextFloat()+y*random.nextFloat()
                        drawCircle(if(it%3==0) Color.White.copy(alpha=.17f) else Color(0xFF332719).copy(alpha=.15f),
                            .35.dp.toPx()+random.nextFloat()*.55.dp.toPx(),spot)
                    }
                }
            }
            val edge=if(highlighted)Color(0xFFF5BD51) else if(face.surface==Surface3.Tank) Color(0xFF9ECBD0) else outline
            drawPath(path,edge.copy(alpha=if(highlighted).9f else .25f),style=Stroke(if(highlighted)1.5.dp.toPx()else .55.dp.toPx()))
        }
        if(isRamp && projectedFaces.isNotEmpty()) {
            val points=projectedFaces.flatMap { it.face.points.zip(it.points) }
            fun endpoint(x: Double): Offset {
                val side=points.filter { it.first.x==x }
                val top=side.maxOf { it.first.z }
                val edge=side.filter { it.first.z==top }.map { it.second }.distinct()
                return edge.reduce { a,b->a+b } / edge.size.toFloat()
            }
            val start=endpoint(points.minOf { it.first.x })
            val end=endpoint(points.maxOf { it.first.x })
            val from=start+(end-start)*.27f
            val to=start+(end-start)*.74f
            val angle=atan2((to.y-from.y).toDouble(),(to.x-from.x).toDouble())
            val arrow=Color(0xFFFFBD52)
            drawLine(arrow,from,to,2.8.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round)
            for(delta in listOf(-.55,.55)) {
                val tail=Offset(cos(angle+delta).toFloat(),sin(angle+delta).toFloat())*9.dp.toPx()
                drawLine(arrow,to,to-tail,2.6.dp.toPx())
            }
            val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color=android.graphics.Color.WHITE
                textAlign=android.graphics.Paint.Align.CENTER
                textSize=12.dp.toPx()
                isFakeBoldText=true
            }
            listOf(start to "البداية",end to "النهاية").forEachIndexed { i,(point,label) ->
                val dx=point.x.coerceIn(32.dp.toPx(),size.width-32.dp.toPx())
                val dy=(point.y+if(i==0)-23.dp.toPx() else 27.dp.toPx()).coerceIn(16.dp.toPx(),size.height-12.dp.toPx())
                val half=paint.measureText(label)/2+7.dp.toPx()
                drawLine(Color.White.copy(alpha=.85f),point,Offset(dx,dy-4.dp.toPx()),1.dp.toPx())
                drawRoundRect(Color(0xFF071C26),Offset(dx-half,dy-15.dp.toPx()),Size(half*2,21.dp.toPx()),CornerRadius(5.dp.toPx()))
                drawContext.canvas.nativeCanvas.drawText(label,dx,dy,paint)
            }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        TextButton(onClick={yaw=initialYaw;pitch=.60;zoom=initialZoom;shell=true;liquid=true}){Text("منظور")}
        TextButton(onClick={yaw=0.0;pitch=PI/2;zoom=1f}){Text("علوي")}
        TextButton(onClick={yaw=0.0;pitch=0.0;zoom=1f}){Text("جانبي")}
        TextButton(onClick={zoom=(zoom*1.25f).coerceAtMost(4f)}){Text("تكبير +")}
        TextButton(onClick={zoom=(zoom/1.25f).coerceAtLeast(.7f)}){Text("تصغير −")}
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
    val shown=if(isRamp)answer.outputs.filter { it.label in setOf("ميل الجزء الرئيسي","الطول المائل الكلي","فرق المنسوب نزولًا") } else results
    shown.chunked(2).forEach { pair ->
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            pair.forEach { VisualMetric(it.label,"${fmt(it.value)} ${it.unit}",Modifier.weight(1f)) }
        }
    }
}

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*
import kotlin.math.*

private val geometryDiagrams=setOf(
    "compaction","consumption_check","flood_test","sand_cone","asphalt_core","steel","pipes",
    "wires","cables","conduits","skirting","kerb","pipe_insulation","garage_ramp",
    "slope","levels","floor_slope","stairs_concrete","stairs_finish","stair_layout","mesh",
    "stirrups","bar_schedule","cutting","mixed_cutting","coordinate_area","level_survey","level_closure",
    "section_volumes","tile_layout","skirting_cut","cylindrical_tank","tank","sloped_excavation","variable_volume",
    "circle","round_column","triangle","trapezoid","rectangle","diagonal","faces",
    "spacing","trays","masonry","blocks","footing","raft","beam",
    "column","wall_concrete","excavation","form_column","form_beam","insulation","tile",
    "rolls","sealant","grout","concrete_compression","mortar_compression","slump",
)

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

internal fun firstOpenVisual(toolId: String) =
    toolId in CalculatorGeometry.supported || toolId in setOf("sieve","proctor","soil_moisture")

/** Geometry is derived from the same inputs/answer used by the calculator, never a second solver. */
@Composable
fun CalculatorDiagram(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer?,update:(String,String)->Unit){
    val id=def.id
    val dims=def.fields.filter{diagramInputVisible(id,it.key,raw)&&it.unit in setOf("م","سم","مم","%","عدد","وجه","درجة","طبقة","شبكة","موصل")&&!it.key.contains("price",true)}
    var editing by remember(id){mutableStateOf<CalcField?>(null)}
    val color=Color(0xFF42DFCE)
    val accent=Color(0xFFFFC164)
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
        val solidExample=remember(id,answer,solid) {
            if(solid==null && answer==null && id in CalculatorGeometry.supported) {
                val defaults=def.fields.associate { field ->
                    field.key to field.default.ifBlank {
                        if(field.required) when(field.key) {
                            "length" -> "4"
                            "width" -> "2"
                            "height" -> "0.5"
                            "diameter" -> if(id=="round_column")"0.4" else "12"
                            "area" -> "12"
                            else -> "1"
                        } else ""
                    }
                }
                val specific=if(id=="stairs_concrete") mapOf("width" to "1.1","run" to "1.2",
                    "rise" to ".68","steps" to "4","tread" to "30","riser" to "17") else emptyMap()
                val data=defaults+specific+visualExamples[id].orEmpty()
                runCatching {
                    val calculated=CalculatorLibrary.evaluate(def,data)
                    CalculatorGeometry.create(id,data,calculated)?.let { it to calculated }
                }.getOrNull()
            } else null
        }
        val chartExample=remember(id,answer) {
            if(answer==null && id in setOf("sieve","proctor")) {
                val data=visualExamples.getValue(id)
                runCatching { CalculatorLibrary.evaluate(def,data) }.getOrNull()?.let { data to it }
            } else null
        }
        Text(if(solid==null && solidExample==null)"الرسم التوضيحي المتغير"else "المجسم ثلاثي الأبعاد",style=MaterialTheme.typography.titleSmall)
        if(id=="sieve") SieveLabIllustration()
        if(chartExample!=null) {
            Text("منحنى بقراءات مثال. أدخل قياساتك لرسم منحنى المشروع.",style=MaterialTheme.typography.bodySmall)
            MeasurementChart(id,chartExample.first)
        }else if(solidExample!=null){
            Text("مجسم مثال. أدخل قياساتك لعرض مجسم المشروع.",style=MaterialTheme.typography.bodySmall)
            CalculatorSolidView(solidExample.first,solidExample.second,isRamp=id=="garage_ramp")
        }else if(answer==null||!displayable){
            Text("راجع المدخلات لظهور المعاينة.",style=MaterialTheme.typography.bodySmall)
        }else if(solid!=null){
            if(modelInput!=raw) Text("المجسم يستخدم أبعاد عرض توضيحية للخانات الفارغة. عدّلها من المدخلات.",
                style=MaterialTheme.typography.bodySmall)
            CalculatorSolidView(solid,answer,isRamp=id=="garage_ramp")
        }else{
            if(id=="garage_ramp")Text("أدخل عرض الرامب وسمك البلاطة لعرض المجسم.")
            if(id=="tile_layout")Text("المجسم يحتاج سمك البلاطة، ويعرض شبكات حتى ٤٠٠ قطعة.")
            when(id) {
                "sieve","proctor" -> {
                    MeasurementChart(id,raw)
                    val result=answer.outputs.firstOrNull { id!="sieve" || "الوعاء" !in it.label }
                    result?.let { VisualMetric(it.label,"${fmt(it.value)} ${it.unit}",Modifier.fillMaxWidth()) }
                }
                in geometryDiagrams -> {
                    Canvas(Modifier.fillMaxWidth().height(200.dp)
                        .semantics{contentDescription="رسم ${def.title} من المدخلات الحالية؛ الأبعاد متاحة في الأزرار أسفل الرسم"}){
                        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF113B47),Color(0xFF071F29))),cornerRadius=CornerRadius(10.dp.toPx()))
                        drawCalculator(id,raw,answer,color,accent)
                    }
                    answer.outputs.firstOrNull()?.let { VisualMetric(it.label,"${fmt(it.value)} ${it.unit}",Modifier.fillMaxWidth()) }
                }
                else -> CalculationFlow(def,raw,answer)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            dims.forEach{f->OutlinedButton(onClick={editing=f}){Text("${f.label}: ${raw[f.key].orEmpty()} ${f.unit}")}}
        }
        if(id=="sand_cone")Text("شكل الحفرة توضيحي؛ الحجم محسوب بكتلة الرمل وكثافته، وليس بأبعاد الشكل.",style=MaterialTheme.typography.labelSmall)
        if(id=="asphalt_core")Text("السمك مقاس؛ قطر نموذج الكور في الرسم توضيحي وغير مستخدم في طريقة الإزاحة.",style=MaterialTheme.typography.labelSmall)
        if(id=="compaction")Text("الحقلية مقابل القصوى المعملية؛ النسبة ليست حكم قبول تلقائي.",style=MaterialTheme.typography.labelSmall)
        if(id=="consumption_check")Text("العمود الأول: المتوقع؛ الثاني: الفعلي، بوحدة خامة موحدة.",style=MaterialTheme.typography.labelSmall)
        if(id=="flood_test")Text("الأفقي: الساعات؛ الرأسي: عمق الماء مم. خط بين قراءتين فقط، وليس رصدًا مستمرًا أو إثبات تسريب.",style=MaterialTheme.typography.labelSmall)
        if(id=="garage_ramp")Text("اتجاه النزول من البداية للنهاية. المعاينة توضّح الأبعاد والميل الهندسي.",style=MaterialTheme.typography.bodySmall)
        if(id=="mesh")Text("الألوان تميز الاتجاهين؛ العرض مختصر عند كثرة الأسياخ. أعداد الحصر في النتيجة كاملة.",style=MaterialTheme.typography.labelSmall)
        if(id=="stirrups")Text("الرسم لجسم الكانة؛ إضافات الثني والخطافات من التفريدة المدخلة ولا يحدد شكلها هذا الرسم.",style=MaterialTheme.typography.labelSmall)
        if(id=="sieve")Text("الأفقي: فتحة المنخل مم بمقياس لوغاريتمي؛ الرأسي: المار %. الوعاء غير مرسوم.",style=MaterialTheme.typography.labelSmall)
        if(id=="proctor")Text("الأفقي: الرطوبة %؛ الرأسي: الكثافة الجافة جم/سم³.",style=MaterialTheme.typography.labelSmall)
        if(id=="section_volumes")Text("الأفقي: المحطة م؛ الرأسي: مساحة الحفر م². الردم منفصل في النتيجة.",style=MaterialTheme.typography.labelSmall)
        if(id in setOf("level_survey","level_closure"))Text("الأفقي: ترتيب النقاط؛ الرأسي: المنسوب م، وليست المسافة بين النقاط.",style=MaterialTheme.typography.labelSmall)
        if(id=="cutting"||id=="mixed_cutting")Text("عرض مختصر لأول 6 أسياخ وحتى 100 قطعة بالسيخ؛ الحصر العددي يشمل الخطة كاملة.",style=MaterialTheme.typography.labelSmall)
        if(id=="proctor")Text("نقاط مقاسة وخطوط وصل؛ لا استنتاج لقمة معملية مؤكدة.",style=MaterialTheme.typography.bodySmall)
        if(id in setOf("tile","rolls","layer","fill","asphalt","base_course","interlock"))Text("المساحة وحدها لا تحدد شكل المسطح أو اتجاه توزيع الخامات.",style=MaterialTheme.typography.labelSmall)
    }
    editing?.let{f->AlertDialog(onDismissRequest={editing=null},title={Text("تعديل ${f.label}")},text={CalcInputField(f,raw[f.key].orEmpty(),{update(f.key,it)})},confirmButton={TextButton(onClick={editing=null}){Text("تم")}})}
}

private fun DrawScope.drawCalculator(id:String,raw:Map<String,String>,answer:CalcAnswer,c:Color,accent:Color){
    fun v(k:String,d:Double=0.0)=MortarMix.number(raw[k])?:d
    fun out(label:String)=answer.outputs.firstOrNull{it.label==label}?.value?:0.0
    val w=size.width;val h=size.height;val left=w*.1f;val top=h*.13f;val rw=w*.8f;val rh=h*.7f
    fun poly(points:List<Pair<Double,Double>>,fill:Boolean=true){
        if(points.size<2)return
        val minX=points.minOf{it.first};val minY=points.minOf{it.second};val dx=(points.maxOf{it.first}-minX).coerceAtLeast(.0001);val dy=(points.maxOf{it.second}-minY).coerceAtLeast(.0001)
        val scale=min(rw/dx,rh/dy).toFloat()
        val ox=left+(rw-dx.toFloat()*scale)/2;val oy=top+(rh-dy.toFloat()*scale)/2
        val path=Path();points.forEachIndexed{i,p->val x=ox+(p.first-minX).toFloat()*scale;val y=oy+(p.second-minY).toFloat()*scale;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
        if(fill){path.close();drawPath(path,c.copy(alpha=.14f))};drawPath(path,c,style=Stroke(3f))
        points.take(120).forEach{p->drawCircle(accent,4f,Offset(ox+(p.first-minX).toFloat()*scale,oy+(p.second-minY).toFloat()*scale))}
    }
    fun rectangle(l:Double,b:Double){poly(listOf(0.0 to 0.0,l to 0.0,l to b,0.0 to b))}
    fun graph(points:List<Pair<Double,Double>>){
        if(points.isEmpty())return
        val x0=points.minOf{it.first};val x1=points.maxOf{it.first};val y0=min(0.0,points.minOf{it.second});val y1=points.maxOf{it.second}
        drawLine(c,Offset(left,top),Offset(left,top+rh),2f);drawLine(c,Offset(left,top+rh),Offset(left+rw,top+rh),2f)
        val path=Path();points.sortedBy{it.first}.forEachIndexed{i,p->val pt=Offset(left+((p.first-x0)/(x1-x0).coerceAtLeast(.0001)).toFloat()*rw,top+rh-((p.second-y0)/(y1-y0).coerceAtLeast(.0001)).toFloat()*rh);if(i==0)path.moveTo(pt.x,pt.y)else path.lineTo(pt.x,pt.y);drawCircle(accent,5f,pt)};drawPath(path,c,style=Stroke(3f))
        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=0xFFDAEFEB.toInt();textSize=11.dp.toPx()}
        fun tick(value:Double,x:Float,y:Float){drawContext.canvas.nativeCanvas.drawText(java.lang.String.format(java.util.Locale.US,"%.2f",value),x,y,paint)}
        tick(x0,left,top+rh+20f);tick(x1,left+rw-40f,top+rh+20f);tick(y0,0f,top+rh);tick(y1,0f,top+12f)
    }
    when(id){
        "compaction","consumption_check"->{
            val first=if(id=="compaction")v("fieldDensity")else out("استهلاك متوقع")
            val second=if(id=="compaction")v("maxDensity")else out("استهلاك فعلي")
            val mx=max(first,second).coerceAtLeast(.0001)
            listOf(first,second).forEachIndexed{i,value->val height=(value/mx).toFloat()*rh;drawRect(if(i==0)c else accent,Offset(left+rw*(.15f+i*.4f),top+rh-height),Size(rw*.25f,height))}
        }
        "flood_test"->graph(listOf(0.0 to v("startLevel"),v("hours") to v("endLevel")))
        "sand_cone"->{
            poly(listOf(0.0 to 0.0,2.0 to 0.0,1.0 to 1.0,1.0 to 1.5,.5 to 1.5,.6 to 2.3,1.4 to 2.3,1.5 to 1.5,1.0 to 1.5),false)
        }
        "asphalt_core"->{
            val width=rw*.35f;val height=(v("thickness")/100).toFloat().coerceIn(.1f,1f)*rh;val x=w/2-width/2;val y=h/2-height/2
            drawRect(c.copy(alpha=.15f),Offset(x,y),Size(width,height));drawRect(c,Offset(x,y),Size(width,height),style=Stroke(3f));drawOval(accent,Offset(x,y-height*.1f),Size(width,height*.2f),style=Stroke(3f))
        }
        "steel"->{drawLine(c,Offset(left,h/2),Offset(left+rw*.7f,h/2),6f);drawCircle(accent,18f,Offset(left+rw*.9f,h/2),style=Stroke(3f))}
        "pipes","wires","cables","conduits","skirting","kerb"->{
            val total=when(id){"wires"->out("طول مطلوب");"cables"->out("طول شراء");else->v("length")*(1+v("waste")/100)}
            val piece=when(id){"wires"->v("roll");"cables"->total;else->v("piece",1.0)}.coerceAtLeast(.0001)
            val count=ceil(total/piece).toInt().coerceIn(1,20)
            repeat(count){i->val y=top+rh*i/count;val fraction=min(piece,max(0.0,total-i*piece))/piece;drawLine(c.copy(alpha=.2f),Offset(left,y),Offset(left+rw,y),8f);drawLine(if(i%2==0)c else accent,Offset(left,y),Offset(left+rw*fraction.toFloat(),y),5f)}
        }
        "pipe_insulation"->{val r=min(rw,rh)*.25f;drawCircle(c,r,Offset(left+rw*.25f,h/2),style=Stroke(6f));drawRect(accent.copy(alpha=.2f),Offset(left+rw*.6f,top),Size(rw*.35f,rh));drawRect(accent,Offset(left+rw*.6f,top),Size(rw*.35f,rh),style=Stroke(2f))}
        "garage_ramp"->{
            val mode=raw["_rampMode"]?:"slope";val parts=mode=="parts";val all=out("الطول الأفقي الكلي");val drop=out("فرق المنسوب نزولًا");val mainSlope=out("ميل الجزء الرئيسي")
            var x=0.0;var y=0.0;val points=mutableListOf(x to y)
            if(parts){x=v("topLength");y=x*v("topSlope")/100;points+=x to y}
            val mainLength=all-if(parts)v("topLength")+v("bottomLength")else 0.0
            x+=mainLength;y+=mainLength*mainSlope/100;points+=x to y
            if(parts){x+=v("bottomLength");y=drop;points+=x to y}
            // Equal scale retains the actual gradient; no misleading vertical exaggeration.
            poly(listOf(-all*.05 to 0.0)+points+listOf(all*1.05 to drop),false)
            drawLine(accent.copy(alpha=.6f),Offset(left,top+rh),Offset(left+rw,top+rh),1f)
        }
        "slope","levels","floor_slope"->{val l=v("length");val rise=when(id){"slope"->l*v("slope")/100;"levels"->v("end")-v("start");else->-l*v("slope")/100};poly(listOf(0.0 to rise,l to 0.0),false)}
        "stairs_concrete","stairs_finish","stair_layout"->{
            val count=(if(id=="stair_layout")v("steps")-1 else v("steps")).toInt().coerceIn(1,60)
            val tread=when(id){"stair_layout"->out("عرض النائمة")/100;else->v("tread")/100}.coerceAtLeast(.01)
            val rise=when(id){"stair_layout"->out("ارتفاع القائمة")/100;else->v("riser")/100}.coerceAtLeast(.01)
            val pts=mutableListOf(0.0 to count*rise);repeat(count){i->pts+=(i*tread) to (count-i-1)*rise;pts+=((i+1)*tread) to (count-i-1)*rise};poly(pts,false)
        }
        "mesh"->{
            rectangle(v("length"),v("width"));val nx=out("أسياخ اتجاه الطول للشبكة").toInt().coerceIn(2,40);val ny=out("أسياخ اتجاه العرض للشبكة").toInt().coerceIn(2,40)
            val scale=min(rw/v("length"),rh/v("width")).toFloat();val dw=v("length").toFloat()*scale;val dh=v("width").toFloat()*scale;val ox=left+(rw-dw)/2;val oy=top+(rh-dh)/2;val cover=(v("cover")/100).toFloat()*scale
            repeat(nx){i->val y=oy+cover+(dh-2*cover)*i/(nx-1);drawLine(accent,Offset(ox+cover,y),Offset(ox+dw-cover,y),1.8f)}
            repeat(ny){i->val x=ox+cover+(dw-2*cover)*i/(ny-1);drawLine(c,Offset(x,oy+cover),Offset(x,oy+dh-cover),1.8f)}
        }
        "stirrups"->rectangle(v("a"),v("b"))
        "bar_schedule"->{val a=v("a");val b=v("b");val d=v("c");when(raw["_shape"]?:"straight"){"L"->poly(listOf(0.0 to 0.0,0.0 to a,b to a),false);"U"->poly(listOf(0.0 to 0.0,0.0 to a,b to a,b to (a-d)),false);"rectangle"->rectangle(a,b);else->poly(listOf(0.0 to 0.0,a to 0.0),false)}}
        "cutting","mixed_cutting"->{
            val stock=v("stock");val kerf=v("kerf")/1000
            val bins=if(id=="mixed_cutting")runCatching{ExtraCalculators.cutPlan(raw,stock,kerf)}.getOrDefault(emptyList())else{
                val per=floor((stock+kerf)/(v("piece")+kerf)).toInt().coerceAtLeast(1);val total=v("count").toInt().coerceAtMost(2000);(0 until min(6,ceil(total.toDouble()/per).toInt())).map{i->List(min(per,total-i*per)){v("piece")}}
            }
            val visible=bins.take(6);visible.forEachIndexed{i,bin->val y=top+i*rh/(visible.size.coerceAtLeast(1));drawRect(c.copy(alpha=.1f),Offset(left,y),Size(rw,14f));var x=0.0;bin.take(100).forEachIndexed{j,p->drawRect(if(j%2==0)c else accent,Offset(left+(x/stock).toFloat()*rw,y),Size((p/stock).toFloat()*rw,14f));x+=p+kerf}}
        }
        "coordinate_area"->poly(ExtraCalculators.tableValues(id,raw).map{it.getValue("x") to -it.getValue("y")})
        "level_survey"->graph(answer.outputs.drop(1).mapIndexed{i,o->i.toDouble() to o.value})
        "level_closure"->graph(answer.outputs.drop(1).mapIndexed{i,o->i.toDouble() to o.value})
        "section_volumes"->graph(ExtraCalculators.tableValues(id,raw).map{it.getValue("station") to it.getValue("cut")})
        "tile_layout"->{
            val l=v("length");val b=v("width");val gap=v("joint")/1000;val centered=raw["_tileStart"]=="center"
            val xs=ExtraCalculators.tileAxis(l,v("tileL")/100,gap,centered);val ys=ExtraCalculators.tileAxis(b,v("tileW")/100,gap,centered)
            val scale=min(rw/l,rh/b).toFloat();val ox=left+(rw-l.toFloat()*scale)/2;val oy=top+(rh-b.toFloat()*scale)/2
            if(xs.size*ys.size<=2500){var y=0.0;ys.forEach{hh->var x=0.0;xs.forEach{ww->drawRect(c.copy(alpha=.14f),Offset(ox+x.toFloat()*scale,oy+y.toFloat()*scale),Size(ww.toFloat()*scale,hh.toFloat()*scale));drawRect(c,Offset(ox+x.toFloat()*scale,oy+y.toFloat()*scale),Size(ww.toFloat()*scale,hh.toFloat()*scale),style=Stroke(1f));x+=ww+gap};y+=hh+gap}}
            else rectangle(l,b)
        }
        "skirting_cut"->{val l=v("tileL");val b=v("tileW");rectangle(l,b);val strips=out("شرائح من البلاطة").toInt().coerceIn(1,100);val scale=min(rw/l,rh/b).toFloat();val ox=left+(rw-l.toFloat()*scale)/2;val oy=top+(rh-b.toFloat()*scale)/2;repeat(strips){i->val yy=oy+((i+1)*v("strip")+i*v("kerf")/10).toFloat()*scale;drawLine(accent,Offset(ox,yy),Offset(ox+l.toFloat()*scale,yy),2f)}}
        "cylindrical_tank"->{
            if(raw["_tankOrientation"]=="horizontal"){
                val r=min(rw,rh)/2;val center=Offset(w/2,h/2);val frac=(v("fill")/v("diameter")).toFloat().coerceIn(0f,1f);val waterY=center.y+r-2*r*frac
                val water=Path();val steps=100;val theta=acos((1-2*frac).toDouble());val pts=(0..steps).map{i->val angle=PI/2-theta+2*theta*i/steps;Offset(center.x+(r*cos(angle)).toFloat(),center.y+(r*sin(angle)).toFloat())};pts.forEachIndexed{i,p->if(i==0)water.moveTo(p.x,p.y)else water.lineTo(p.x,p.y)};water.close();drawPath(water,c.copy(alpha=.35f));drawCircle(c,r,center,style=Stroke(3f));drawLine(accent,Offset(center.x-r*sqrt(max(0f,1-(1-2*frac).pow(2))),waterY),Offset(center.x+r*sqrt(max(0f,1-(1-2*frac).pow(2))),waterY),2f)
            }else{drawRect(c,Offset(left+rw*.25f,top),Size(rw*.5f,rh),style=Stroke(3f));val fill=(v("fill")/v("length")).toFloat();drawRect(c.copy(alpha=.3f),Offset(left+rw*.25f,top+rh*(1-fill)),Size(rw*.5f,rh*fill))}
        }
        "tank"->{val ratio=((v("height")-v("freeboard"))/v("height")).toFloat();drawRect(c,Offset(left,top),Size(rw,rh),style=Stroke(3f));drawRect(c.copy(alpha=.3f),Offset(left,top+rh*(1-ratio)),Size(rw,rh*ratio))}
        "sloped_excavation"->{val bottom=v("width")+2*v("working");val depth=v("depth");val side=v("sideY")*depth;poly(listOf(0.0 to 0.0,side to depth,side+bottom to depth,bottom+2*side to 0.0),false)}
        "variable_volume"->{val b=v("baseL");val t=v("topL");val hh=v("height");poly(listOf(0.0 to hh,b to hh,(b+t)/2 to 0.0,(b-t)/2 to 0.0))}
        "circle","round_column"->{val radius=min(rw,rh)/2;drawCircle(c.copy(alpha=.15f),radius,Offset(w/2,h/2));drawCircle(c,radius,Offset(w/2,h/2),style=Stroke(3f));if(id=="circle"&&v("inner")>0)drawCircle(accent,radius*(v("inner")/v("diameter")).toFloat(),Offset(w/2,h/2),style=Stroke(3f));drawLine(accent,Offset(w/2-radius,h/2),Offset(w/2+radius,h/2),2f)}
        "triangle"->poly(listOf(0.0 to v("height"),v("base") to v("height"),v("base")/2 to 0.0))
        "trapezoid"->poly(listOf(0.0 to v("height"),v("a") to v("height"),(v("a")+v("b"))/2 to 0.0,(v("a")-v("b"))/2 to 0.0))
        "rectangle","diagonal","faces"->{val l=v("length");val b=if(id=="faces")v("height")else v("width");rectangle(l,b);if(id=="diagonal"){val sc=min(rw/l,rh/b).toFloat();val ox=left+(rw-l.toFloat()*sc)/2;val oy=top+(rh-b.toFloat()*sc)/2;drawLine(accent,Offset(ox,oy),Offset(ox+l.toFloat()*sc,oy+b.toFloat()*sc),3f)}}
        "spacing","trays"->{val count=(if(id=="spacing")v("intervals")+1 else out("دعامات لمسار مستقيم")).toInt().coerceIn(2,100);drawLine(c,Offset(left,h/2),Offset(left+rw,h/2),3f);repeat(count){i->drawCircle(accent,5f,Offset(left+rw*i/(count-1),h/2))}}
        "masonry","blocks"->{val bw=v("brickL");val bh=v("brickH");val ratio=(bh/bw).toFloat().coerceIn(.1f,2f);val cellW=rw/4;val cellH=min(rh/3,cellW*ratio);repeat(3){y->repeat(4){x->drawRect(c.copy(alpha=.25f),Offset(left+x*cellW+2,top+y*cellH+2),Size(cellW-4,cellH-4));drawRect(c,Offset(left+x*cellW+2,top+y*cellH+2),Size(cellW-4,cellH-4),style=Stroke(2f))}}}
        "footing","raft","beam","column","wall_concrete","excavation","form_column","form_beam","insulation"->{val l=if(id=="insulation")v("boardL")else v("length");val b=if(id=="insulation")v("boardW")else v("width");rectangle(l,b);drawLine(accent,Offset(left+rw*.15f,top+rh*.8f),Offset(left+rw*.15f,top+rh*.2f),3f)}
        "tile"->rectangle(v("tileW"),v("tileH"))
        "rolls"->{val l=v("rollL");val b=v("rollW");rectangle(l,b);val sc=min(rw/l,rh/b).toFloat();val ox=left+(rw-l.toFloat()*sc)/2;val oy=top+(rh-b.toFloat()*sc)/2;drawRect(accent.copy(alpha=.3f),Offset(ox,oy),Size((v("overlapL")/100).toFloat()*sc,b.toFloat()*sc));drawRect(accent.copy(alpha=.3f),Offset(ox,oy),Size(l.toFloat()*sc,(v("overlapW")/100).toFloat()*sc))}
        "sealant","grout"->rectangle(v("jointW"),v("jointD"))
        "concrete_compression","mortar_compression"->{if(raw["_specimen"]=="cylinder")drawCircle(c,min(rw,rh)*.4f,Offset(w/2,h/2),style=Stroke(3f))else rectangle(v("a"),v("b"));drawLine(accent,Offset(w/2,0f),Offset(w/2,top),4f);drawLine(accent,Offset(w/2,top+rh),Offset(w/2,h),4f)}
        "slump"->{val ratio=(v("sampleHeight")/v("reference")).toFloat();drawLine(c,Offset(left,top),Offset(left,top+rh),3f);val base=top+rh;drawRect(c.copy(alpha=.2f),Offset(left+rw*.3f,base-rh*ratio),Size(rw*.5f,rh*ratio));drawLine(accent,Offset(left,top),Offset(left+rw,top),2f)}

    }
}

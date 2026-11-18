package com.fayroz.sitecalculator.domain

import kotlin.math.*

internal data class Point3(val x: Double, val y: Double, val z: Double) {
    operator fun minus(p: Point3) = Point3(x-p.x,y-p.y,z-p.z)
    fun cross(p: Point3) = Point3(y*p.z-z*p.y,z*p.x-x*p.z,x*p.y-y*p.x)
    fun dot(p: Point3) = x*p.x+y*p.y+z*p.z
}
internal enum class Surface3 { Concrete, Excavation, Tank, Water, Tile, Steel, Insulation, Plywood }
internal data class Face3(val points: List<Point3>, val surface: Surface3, val part: String)
internal data class CalculatorSolid(val faces: List<Face3>, val note: String = "")

internal object CalculatorGeometry {
    private val boxes = setOf("raft","footing","beam","column","wall_concrete","excavation")
    val supported = boxes + setOf("round_column","garage_ramp","tank","cylindrical_tank","sloped_excavation","variable_volume","tile_layout",
        "steel","mesh","stairs_concrete","stairs_finish","insulation","pipe_insulation","sealant","form_beam","form_column")
    private fun rect(x: Double,y: Double,l: Double,w: Double,z: Double) =
        listOf(Point3(x,y,z),Point3(x+l,y,z),Point3(x+l,y+w,z),Point3(x,y+w,z))

    private fun connect(a: List<Point3>, b: List<Point3>, surface: Surface3, part: String, cap: Boolean = true): List<Face3> = buildList {
        if (cap) {
            add(Face3(a.reversed(),surface,part))
            add(Face3(b,surface,part))
        }
        a.indices.forEach { i ->
            val j=(i+1)%a.size
            add(Face3(listOf(a[i],a[j],b[j],b[i]),surface,part))
        }
    }

    private fun box(x: Double,y: Double,l: Double,w: Double,h: Double,surface: Surface3,part: String) =
        connect(rect(x,y,l,w,0.0),rect(x,y,l,w,h),surface,part)

    fun create(id: String, raw: Map<String,String>, answer: CalcAnswer): CalculatorSolid? {
        fun v(key: String) = MortarMix.number(raw[key]) ?: 0.0
        fun out(label: String) = answer.outputs.first { it.label == label }.value
        val l=v("length")
        val w=v("width")
        val h=v("height")
        val count=v("count")
        if(id in boxes) return CalculatorSolid(
            box(0.0,0.0,l,w,h,if(id=="excavation")Surface3.Excavation else Surface3.Concrete,"العنصر"),
            if(count>1) "المجسم لعنصر واحد؛ الحصر يشمل العدد المدخل." else ""
        )
        return when(id) {
            "steel" -> CalculatorSolid(
                rod(l,v("diameter")/2000,0.0,0.0,false,"السيخ"),
                "المجسم لسيخ واحد. الحصر يشمل العدد المدخل."
            )
            "mesh" -> {
                val cover=v("cover")/100
                val steelL=l-2*cover
                val steelW=w-2*cover
                val nL=out("أسياخ اتجاه الطول للشبكة").toInt().coerceAtLeast(2)
                val nW=out("أسياخ اتجاه العرض للشبكة").toInt().coerceAtLeast(2)
                val rL=v("diameter")/2000
                val rW=(v("diameterY").takeIf { it>0 } ?: v("diameter"))/2000
                fun visibleIndices(count: Int): List<Int> {
                    val shown=count.coerceAtMost(28)
                    return List(shown) { (it.toDouble()*(count-1)/(shown-1)).roundToInt() }
                }
                val bars=visibleIndices(nL).flatMap { rod(steelL,rL,steelW*it/(nL-1),rL,false,"اتجاه الطول") } +
                    visibleIndices(nW).flatMap { rod(steelW,rW,steelL*it/(nW-1),2*rL+rW,true,"اتجاه العرض") }
                val note=buildList {
                    add("شبكة واحدة بين حدود الغطاء. طول التراكب مسجل في الحصر؛ موضعه غير مدخل.")
                    if(nL>28||nW>28) add("عرض مختصر للأسياخ؛ العدد الكامل في النتيجة.")
                    if(v("layers")>1) add("توزيع الشبكات رأسيًا يحتاج مناسيبها.")
                }.joinToString(" ")
                CalculatorSolid(bars,note)
            }
            "stairs_concrete" -> {
                val run=v("run"); val rise=v("rise"); val steps=v("steps").toInt()
                val tread=v("tread")/100; val riser=v("riser")/100
                // Draw the exact profile only when both dimension sets describe the same flight.
                if(steps !in 1..120 || abs(run-tread*steps)>1e-6 || abs(rise-riser*steps)>1e-6) return null
                val waist=v("waist")/100
                val slant=hypot(run,rise)
                val dx=waist*rise/slant
                val dz=-waist*run/slant
                fun across(profile: List<Pair<Double,Double>>, label: String): List<Face3> {
                    val side=profile.map { (x,z)->Point3(x,0.0,z) }
                    return connect(side,side.map { it.copy(y=w) },Surface3.Concrete,label)
                }
                val base=across(listOf(0.0 to 0.0,run to rise,run+dx to rise+dz,dx to dz),"بطنية السلم")
                val stairs=(0 until steps).flatMap { i ->
                    val x=i*tread; val z=i*riser
                    across(listOf(x to z,x to z+riser,x+tread to z+riser),"درجة ${i+1}")
                }
                val note=if(v("landingVolume")>0)"البسطات في الحصر. أبعادها ومواقعها غير مدخلة في المجسم." else ""
                CalculatorSolid(base+stairs,note)
            }
            "stairs_finish" -> {
                val steps=v("steps").toInt()
                if(steps !in 1..120) return null
                val tread=v("tread")/100; val riser=v("riser")/100
                val faces=(0 until steps).flatMap { i ->
                    val x=i*tread; val z=i*riser
                    val riserFace=listOf(Point3(x,0.0,z),Point3(x,w,z),Point3(x,w,z+riser),Point3(x,0.0,z+riser))
                    val treadFace=rect(x,0.0,tread,w,z+riser)
                    listOf(Face3(riserFace,Surface3.Plywood,"قائمة ${i+1}"),Face3(treadFace,Surface3.Tile,"نائمة ${i+1}"))
                }
                CalculatorSolid(faces,"يعرض أسطح الكسوة. البسطات تحتاج أبعادًا ومواقع.")
            }
            "pipe_insulation" -> CalculatorSolid(
                connect(ring(v("diameter")/2000,0.0,true),ring(v("diameter")/2000,l,true),
                    Surface3.Insulation,"السطح الخارجي",cap=false),
                "السطح الخارجي فقط. سمك العزل غير مدخل."
            )
            "sealant" -> CalculatorSolid(
                box(0.0,0.0,l,v("jointW")/1000,v("jointD")/1000,Surface3.Tile,"مادة ملء الفاصل"),
                "يمثل الجزء المملوء بالعرض والعمق المدخلين."
            )
            "insulation" -> CalculatorSolid(
                box(0.0,0.0,v("boardL"),v("boardW"),v("thickness")/100,Surface3.Insulation,"لوح العزل"),
                "المجسم للوح واحد؛ توزيع الألواح يحتاج شكل المسطح."
            )
            "form_beam" -> {
                val p=rect(0.0,0.0,l,w,0.0); val q=rect(0.0,0.0,l,w,h)
                CalculatorSolid(listOf(Face3(p,Surface3.Plywood,"قاع الشدة")) +
                    listOf(0,2).map { i -> Face3(listOf(p[i],p[i+1],q[i+1],q[i]),Surface3.Plywood,"جانب الشدة") },
                    "يعرض الأوجه المحتسبة لكمرة واحدة. سماكة ألواح الشدة غير مدخلة.")
            }
            "form_column" -> {
                val wall=ExtraCalculators.formMember(raw)=="wall"
                if(wall && v("faces")!=1.0) return null
                val p=rect(0.0,0.0,l,w,0.0); val q=rect(0.0,0.0,l,w,h)
                val faces=if(wall) listOf(Face3(listOf(p[0],p[1],q[1],q[0]),Surface3.Plywood,"وجه الحائط"))
                    else connect(p,q,Surface3.Plywood,"وجه العمود",cap=false)
                CalculatorSolid(faces,"يعرض الأوجه المحتسبة لعنصر واحد.")
            }
            "round_column" -> {
                val r=v("diameter")/2
                CalculatorSolid(connect(ring(r,0.0,false),ring(r,h,false),Surface3.Concrete,"العمود"),
                    if(count>1) "المجسم لعمود واحد؛ الحصر يشمل العدد المدخل." else "")
            }
            "variable_volume" -> {
                val bl=v("baseL"); val bw=v("baseW"); val tl=v("topL"); val tw=v("topW")
                CalculatorSolid(connect(rect(-bl/2,-bw/2,bl,bw,0.0),rect(-tl/2,-tw/2,tl,tw,h),Surface3.Concrete,"الحجم المدرج"))
            }
            "sloped_excavation" -> {
                val bl=l+2*v("working"); val bw=w+2*v("working")
                val tl=out("طول السطح"); val tw=out("عرض السطح")
                val top=rect(-tl/2,-tw/2,tl,tw,v("depth"))
                val bottom=rect(-bl/2,-bw/2,bl,bw,0.0)
                CalculatorSolid(connect(bottom,top,Surface3.Excavation,"جوانب الحفر",cap=false) +
                    Face3(bottom,Surface3.Excavation,"قاع الحفر"),"كل زوج جوانب له الميل المدخل نفسه.")
            }
            "garage_ramp" -> {
                val width=v("rampWidth")
                val thickness=v("rampThickness")/100
                if(width<=0 || thickness<=0) return null
                val segmented=raw["_rampMode"]=="parts"
                val top=if(segmented)v("topLength") else 0.0
                val bottom=if(segmented)v("bottomLength") else 0.0
                val lengths=listOf(top,out("الطول الأفقي الكلي")-top-bottom,bottom)
                val slopes=listOf(v("topSlope"),out("ميل الجزء الرئيسي"),v("bottomSlope"))
                val labels=listOf("الانتقال العلوي","الجزء الرئيسي","الانتقال السفلي")
                var x=0.0
                var z=0.0
                val faces=buildList {
                    lengths.forEachIndexed { i,length ->
                        if(length<=0) return@forEachIndexed
                        val endZ=z-length*slopes[i]/100
                        val surface=listOf(Point3(x,0.0,z),Point3(x+length,0.0,endZ),
                            Point3(x+length,width,endZ),Point3(x,width,z))
                        addAll(connect(surface.map{it.copy(z=it.z-thickness)},surface,Surface3.Concrete,labels[i]))
                        x+=length
                        z=endZ
                    }
                }
                CalculatorSolid(faces,"السمك مقاس رأسيًا. المعاينة تعرض البلاطة فقط.")
            }
            "tank" -> {
                val liquid=h-v("freeboard")
                val walls=connect(rect(0.0,0.0,l,w,0.0),rect(0.0,0.0,l,w,h),Surface3.Tank,"حدود الخزان",cap=false)
                val water=if(liquid>0) box(0.0,0.0,l,w,liquid,Surface3.Water,"السائل") else emptyList()
                CalculatorSolid(walls+water,"الأبعاد داخلية. سمك الجدار غير ممثل.")
            }
            "cylindrical_tank" -> {
                val r=v("diameter")/2
                val fill=v("fill")
                val horizontal=raw["_tankOrientation"]=="horizontal"
                val walls=connect(ring(r,0.0,horizontal),ring(r,l,horizontal),Surface3.Tank,"حدود الخزان")
                val water=when {
                    fill<=0 -> emptyList()
                    !horizontal -> connect(ring(r,0.0,false),ring(r,fill,false),Surface3.Water,"السائل")
                    else -> {
                        val halfArc=acos(((r-fill)/r).coerceIn(-1.0,1.0))
                        fun segment(x: Double) = (0..48).map { index ->
                            val angle=-PI/2-halfArc+2*halfArc*index/48
                            Point3(x,r*cos(angle),r*sin(angle))
                        }
                        connect(segment(0.0),segment(l),Surface3.Water,"السائل")
                    }
                }
                CalculatorSolid(walls+water,"الأبعاد داخلية. سمك الجدار غير ممثل.")
            }
            "tile_layout" -> {
                val thickness=v("tileThickness")/1000
                if(thickness<=0) return null
                val joint=v("joint")/1000
                val center=raw["_tileStart"]=="center"
                val xs=ExtraCalculators.tileAxis(l,v("tileL")/100,joint,center)
                val ys=ExtraCalculators.tileAxis(w,v("tileW")/100,joint,center)
                if(xs.size*ys.size>400) return null
                val faces=buildList {
                    var y=0.0
                    ys.forEachIndexed { row, width ->
                        var x=0.0
                        xs.forEachIndexed { col, length ->
                            addAll(box(x,y,length,width,thickness,Surface3.Tile,"صف ${row+1} • عمود ${col+1}"))
                            x+=length+joint
                        }
                        y+=width+joint
                    }
                }
                CalculatorSolid(faces)
            }
            else -> null
        }
    }

    private fun rod(length: Double,radius: Double,cross: Double,z: Double,alongY: Boolean,part: String): List<Face3> {
        fun end(position: Double) = List(12) { i ->
            val angle=2*PI*i/12
            val across=cross+radius*cos(angle)
            val height=z+radius*sin(angle)
            if(alongY) Point3(across,position,height) else Point3(position,across,height)
        }
        return connect(end(0.0),end(length),Surface3.Steel,part)
    }

    private fun ring(r: Double, along: Double, horizontal: Boolean): List<Point3> = (0 until 48).map { index ->
        val theta=2*PI*index/48
        val a=r*cos(theta)
        val b=r*sin(theta)
        if(horizontal) Point3(along,a,b) else Point3(a,b,along)
    }
}

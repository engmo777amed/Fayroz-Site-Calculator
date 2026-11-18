package com.fayroz.sitecalculator.domain

import kotlin.math.*

internal data class Point3(val x: Double, val y: Double, val z: Double) {
    operator fun minus(p: Point3) = Point3(x-p.x,y-p.y,z-p.z)
    fun cross(p: Point3) = Point3(y*p.z-z*p.y,z*p.x-x*p.z,x*p.y-y*p.x)
    fun dot(p: Point3) = x*p.x+y*p.y+z*p.z
}
internal enum class Surface3 { Concrete, Excavation, Tank, Water, Tile }
internal data class Face3(val points: List<Point3>, val surface: Surface3, val part: String)
internal data class CalculatorSolid(val faces: List<Face3>, val note: String = "")

internal object CalculatorGeometry {
    private val boxes = setOf("raft","footing","beam","column","wall_concrete","excavation")
    val supported = boxes + setOf("round_column","garage_ramp","tank","cylindrical_tank","sloped_excavation","variable_volume","tile_layout")
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

    private fun ring(r: Double, along: Double, horizontal: Boolean): List<Point3> = (0 until 48).map { index ->
        val theta=2*PI*index/48
        val a=r*cos(theta)
        val b=r*sin(theta)
        if(horizontal) Point3(along,a,b) else Point3(a,b,along)
    }
}

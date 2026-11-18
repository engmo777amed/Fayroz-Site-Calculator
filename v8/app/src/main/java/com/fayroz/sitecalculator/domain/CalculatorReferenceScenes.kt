package com.fayroz.sitecalculator.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Schematic examples use their own coordinates, never quantities or dimensions from a project. */
internal object CalculatorReferenceScenes {
    fun forTool(id: String): CalculatorSolid? {
        val model=Model()
        val explanation=with(model) {
            when(id) {
                "rectangle","area","diagonal" -> {
                    box(0.0,0.0,0.0,3.3,2.1,.10,Surface3.Tile,"المسطح")
                    if(id=="area")box(2.0,1.25,.1,1.1,.65,.06,Surface3.Accent,"جزء مخصوم")
                    if(id=="diagonal") beam(0.0,0.0,3.3,2.1,.12,.055,Surface3.Accent,"القطر")
                    when(id){
                        "area"->"الملون يوضح فكرة طرح جزء من المساحة."
                        "diagonal"->"القطر يمتد بين ركنين متقابلين."
                        else->"المساحة تُقاس على الوجه العلوي بين الطول والعرض."
                    }
                }
                "triangle","trapezoid","coordinate_area" -> {
                    val outline=when(id){
                        "triangle"->listOf(0.0 to 0.0,3.2 to 0.0,1.3 to 2.2)
                        "trapezoid"->listOf(0.0 to 0.0,3.2 to 0.0,2.65 to 2.1,.6 to 2.1)
                        else->listOf(0.0 to 0.0,3.0 to .3,3.2 to 1.5,1.7 to 2.3,.15 to 1.6)
                    }
                    polygon(outline,0.0,.11,Surface3.Tile,"سطح القياس")
                    if(id=="coordinate_area")outline.forEachIndexed { i,(x,y)->cylinder(x,y,.11,.08,.24,Surface3.Accent,"نقطة ${i+1}") }
                    "تتحسب المساحة داخل الحد الخارجي للمسطح."
                }
                "circle" -> {
                    tube(0.0,0.0,0.0,1.25,.53,.2,Surface3.Tile,"المسطح الحلقي")
                    "الحلقة هي المساحة بين القطر الخارجي والداخلي."
                }
                "spacing","level_survey","level_closure","levels","earth_levels" -> {
                    box(0.0,0.0,0.0,3.7,1.45,.15,Surface3.Excavation,"أرض القياس")
                    repeat(4){i->
                        val height=if(id=="spacing").65 else .55+i*.12
                        box(.35+i,.63,.15,.10,.10,height,Surface3.Plywood,"نقطة قياس ${i+1}")
                        box(.35+i,.63,.15+height-.13,.10,.10,.13,Surface3.Accent,"علامة النقطة")
                    }
                    if(id in setOf("level_survey","level_closure")) {
                        listOf(0.2 to .23,1.0 to .29,.45 to 1.08).forEach { (x,y)->
                            pillar(x,y,.16,.65,.63,1.0,.035,Surface3.Steel,"حامل جهاز الميزان")
                        }
                        box(.48,.49,1.0,.46,.26,.22,Surface3.Accent,"جهاز الميزان")
                        box(.56,.38,1.05,.18,.12,.13,Surface3.Steel,"العدسة")
                    }
                    if(id=="spacing")"علامات موزعة على مسافات متساوية."
                    else "علامات على الأرض لتوضيح مواضع قياس المناسيب."
                }
                "slope","floor_slope","garage_ramp" -> {
                    incline(3.5,2.0,.65,.23)
                    if(id=="floor_slope") box(2.9,.64,.22,.35,.61,.07,Surface3.Steel,"صفاية")
                    "مقطع يوضح فرق المنسوب بين طرفي السطح."
                }
                "layer","screed","bedding","fill","asphalt","base_course","spray","marking","interlock","section_volumes" -> {
                    val cap=when(id){
                        "asphalt","spray","marking"->Surface3.Asphalt
                        "interlock"->Surface3.Tile
                        "fill","section_volumes"->Surface3.Excavation
                        else->Surface3.Concrete
                    }
                    box(0.0,0.0,0.0,3.2,2.0,.33,Surface3.Excavation,"الأرض")
                    box(0.0,0.0,.33,3.2,2.0,.23,Surface3.Gravel,"طبقة أسفل سطح العمل")
                    if(id=="interlock") grid(.56,3.2,2.0,Surface3.Tile)
                    else box(0.0,0.0,.56,3.2,2.0,if(id=="spray").035 else .13,cap,"الطبقة المدخلة")
                    if(id=="marking")repeat(3){box(.25+it*.95,.93,.69,.56,.13,.013,Surface3.Accent,"علامة طريق")}
                    if(id=="section_volumes")repeat(3){box(.3+it*1.18,.1,.69,.065,1.8,.065,Surface3.Accent,"محطة ${it+1}")}
                    "مقطع يوضح ترتيب الطبقات ومكان قياس السمك."
                }
                "masonry","blocks","block_adhesive","plaster","splash","faces" -> {
                    val clay=if(id in setOf("blocks","block_adhesive"))Surface3.Concrete else Surface3.Brick
                    repeat(4){r->
                        for(c in 0..3){
                            val x=c*.77+(r%2)*.34
                            if(x<3.05)box(x,0.0,r*.32,minOf(.72,3.13-x),.48,.28,clay,"وحدة مباني")
                        }
                    }
                    if(id in setOf("plaster","splash","faces")) {
                        box(.04,-.075,0.0,1.95,.075,1.25,Surface3.Plaster,"وجه البياض")
                    }
                    if(id=="block_adhesive")box(.35,-.018,.60,2.39,.47,.036,Surface3.Plaster,"طبقة لاصق")
                    "يوضح وجه الحائط والوحدات أو الطبقة التي يُقاس عليها البند."
                }
                "tile","tile_layout","grout","adhesive","skirting","skirting_cut" -> {
                    box(0.0,0.0,0.0,2.72,2.0,.18,Surface3.Concrete,"أساس التركيب")
                    if(id=="adhesive")repeat(14){i->box(i*.18,.02,.18,.055,1.94,.04,Surface3.Plaster,"أسنان اللاصق")}
                    grid(if(id=="adhesive").22 else .18,if(id=="adhesive")1.34 else 2.72,2.0,Surface3.Tile)
                    if(id in setOf("skirting","skirting_cut")) {
                        box(0.0,1.86,.18,2.72,.16,1.15,Surface3.Plaster,"حائط")
                        repeat(4){i->box(i*.68,1.78,.27,.64,.081,.23,Surface3.Tile,"وزرة")}
                    }
                    if(id=="grout") repeat(3){i->box(.65+i*.68,.01,.24,.03,1.98,.035,Surface3.Accent,"فاصل الروبة")}
                    "سطوح البلاط وفواصله أو طبقة التركيب مبينة في مقطع نموذجي."
                }
                "paint","putty","primer","woodpaint","waterproof","rolls","flood_test" -> {
                    val base=if(id=="woodpaint")Surface3.Plywood else Surface3.Concrete
                    box(0.0,0.0,0.0,3.1,1.8,.29,base,"السطح الأساسي")
                    box(.07,.07,.29,2.93,1.66,.055,Surface3.Plaster,"طبقة التجهيز")
                    box(.58,.1,.345,2.1,1.6,.045,if(id in setOf("waterproof","rolls","flood_test"))Surface3.Asphalt else Surface3.Accent,"طبقة التطبيق")
                    if(id=="rolls")cylinder(2.28,1.15,.39,.41,.72,Surface3.Asphalt,"لفة العزل")
                    if(id=="flood_test") {
                        box(.60,.12,.39,2.02,1.51,.12,Surface3.Water,"مياه اختبار الغمر")
                        box(2.43,.68,.39,.09,.14,.57,Surface3.Steel,"مقياس العمق")
                    }
                    "مقطع يوضح السطح الأساسي وطبقة التطبيق المطروحة في الحساب."
                }
                "gypsum","gypsum_system","light_cove" -> {
                    box(.0,.0,.10,3.0,2.05,.11,Surface3.Plaster,"لوح الجبس")
                    repeat(4){i->box(.06,.17+i*.51,.23,2.9,.06,.13,Surface3.Steel,"قطاع تعليق")}
                    for(x in listOf(.55,2.45)) for(y in listOf(.18,1.73)) {
                        cylinder(x,y,.30,.034,.65,Surface3.Steel,"علاقة")
                    }
                    if(id=="light_cove") {
                        box(0.0,0.0,.21,3.0,.16,.26,Surface3.Plaster,"جانب بيت النور")
                        box(.08,.15,.27,2.82,.065,.042,Surface3.Accent,"شريط الإضاءة")
                    }
                    "يوضح اللوح والقطاعات. أماكن التعليق هنا مثال بصري."
                }
                "pipes","fittings","conduits","wires","cables","trays" -> {
                    when(id) {
                        "trays"-> {
                            box(0.0,0.0,.36,3.3,1.32,.08,Surface3.Steel,"قاع حامل الكابلات")
                            for(y in listOf(0.0,1.25))box(0.0,y,.44,3.3,.07,.24,Surface3.Steel,"جانب الحامل")
                            repeat(3){i->box(.26+i*1.35,-.08,.05,.12,1.48,.29,Surface3.Steel,"دعامة")}
                        }
                        "wires","cables"-> {
                            axisTube(0.0,0.0,.25,2.2,.38,.32,Surface3.Asphalt,"الغلاف")
                            for(i in 0..2){
                                val color=listOf(Surface3.Accent,Surface3.Brick,Surface3.Insulation)[i]
                                axisTube(.1,(i-1)*.17,.25,2.75,.075,0.0,color,"موصل ${i+1}")
                                axisTube(2.7,(i-1)*.17,.25,.39,.043,0.0,Surface3.Copper,"طرف الموصل")
                            }
                        }
                        else->{
                            axisTube(0.0,.35,.45,2.6,.43,.33,Surface3.Plaster,"ماسورة")
                            if(id=="fittings") {
                                axisTube(1.87,.35,.45,.76,.53,.43,Surface3.Accent,"وصلة")
                                tube(2.22,.35,.45,.31,.24,.8,Surface3.Accent,"فرع الوصلة")
                            }
                            if(id=="conduits")box(2.24,-.17,.12,.85,1.01,.8,Surface3.Steel,"علبة")
                        }
                    }
                    "الجزء المجسم يوضح شكل البند. مسار المشروع وقطره غير مفترضين."
                }
                "steel","stirrups","bar_schedule","cutting","mixed_cutting","mesh" -> {
                    if(id=="stirrups"||id=="bar_schedule"){
                        val corners=listOf(0.0 to 0.0,2.6 to 0.0,2.6 to 1.65,0.0 to 1.65)
                        corners.indices.forEach { i->
                            val(a,b)=corners[i];val(c,d)=corners[(i+1)%4]
                            beam(a,b,c,d,.12,.14,Surface3.Steel,"ضلع توضيحي")
                        }
                    } else {
                        for(i in 0..3) {
                            axisTube(0.0,i*.37,.12,3.1,.09,0.0,Surface3.Steel,"سيخ خام")
                            if(id.contains("cutting"))for(x in listOf(.95,2.06))
                                axisTube(x,i*.37,.12,.045,.105,0.0,Surface3.Accent,"موضع قص")
                        }
                        if(id=="mesh")repeat(4){i->beam(i*.84,0.0,i*.84,1.13,.27,.11,Surface3.Steel,"اتجاه متقاطع")}
                    }
                    "يوضح شكل الحديد أو موضع القص. شكل الثني والتقطيع يتحدد من تفاصيل التنفيذ."
                }
                "stairs_concrete","stairs_finish","stair_layout" -> {
                    for(i in 0..4)box(i*.54,0.0,0.0,.54,1.55,.28*(i+1),
                        if(id=="stairs_finish")Surface3.Tile else Surface3.Concrete,"درجة ${i+1}")
                    "شكل الدرجات يوضح الفرق بين عرض النائمة وارتفاع القائمة."
                }
                "excavation","sloped_excavation","footing","raft","beam","column","wall_concrete","round_column","variable_volume","form_column","form_beam","insulation","sealant","pipe_insulation","tank","cylindrical_tank" ->
                    engineeringExample(id)
                "concrete_supply","haulage","kerb" -> {
                    if(id=="kerb") repeat(4){i->box(i*.85,0.0,0.0,.80,.48,.43,Surface3.Concrete,"بردورة")}
                    else {
                        box(0.0,0.0,.37,3.1,1.18,.22,Surface3.Steel,"شاسيه السيارة")
                        box(0.0,.04,.59,.84,1.10,.86,Surface3.Accent,"كابينة")
                        box(.03,.08,1.16,.4,1.02,.17,Surface3.Steel,"زجاج الكابينة")
                        if(id=="concrete_supply")axisTube(.84,.59,1.11,1.91,.55,0.0,Surface3.Concrete,"خلاطة الخرسانة")
                        else box(.93,.025,.62,2.15,1.14,.64,Surface3.Excavation,"صندوق الحمولة")
                        for(x in listOf(.53,2.49)) for(y in listOf(-.09,1.27))
                            axisTube(y-.15,x,.32,.30,.33,0.0,Surface3.Asphalt,"عجلة",alongY=true)
                    }
                    "عنصر توضيحي يبين البند الذي يُحسب بالعدد أو الحمولة."
                }
                "soil_moisture","aggregate","asphalt_voids","asphalt_core","sand_cone","compaction","proctor","sieve","slump","concrete_compression","mortar_compression" ->
                    labExample(id)
                "reverse_mortar","custom_mortar","reverse_coverage","consumption_check","points","loads","convert" -> {
                    when(id) {
                        "points","loads" -> repeat(3){ i->
                            box(i*.83,0.0,0.0,.65,.66,.16,Surface3.Plaster,"وحدة كهرباء")
                            for(x in listOf(.18,.43))box(i*.83+x,.21,.16,.058,.22,.009,Surface3.Asphalt,"فتحة التوصيل")
                        }
                        "convert" -> {
                            box(0.0,0.0,0.0,3.1,.52,.12,Surface3.Plywood,"أداة قياس")
                            repeat(16){i->box(.11+i*.18,0.0,.12,.025,if(i%5==0).34 else .19,.018,Surface3.Accent,"تدريج")}
                        }
                        "reverse_coverage","consumption_check" -> repeat(3){ i->
                            tube(i*.88,.52,0.0,.38,.35,.62,Surface3.Accent,"عبوة")
                            cylinder(i*.88,.52,.62,.39,.07,Surface3.Steel,"غطاء")
                        }
                        else-> {
                            for(i in 0..2)box(i*.64,0.0,0.0,.57,1.07,.27,Surface3.Plywood,"شيكارة")
                            cylinder(1.08,1.57,0.0,.54,.29,Surface3.Excavation,"رمل")
                        }
                    }
                    "مثال بصري للوحدة أو الخامة المستخدمة في الحساب."
                }
                else -> return null
            }
        }
        return CalculatorSolid(model.faces,"مثال توضيحي مستقل عن قياسات المشروع. $explanation")
    }

    private class Model {
        val faces=mutableListOf<Face3>()
        private fun connect(a: List<Point3>,b: List<Point3>,kind: Surface3,part: String,caps: Boolean=true) {
            if(caps) {
                faces+=Face3(a.reversed(),kind,part)
                faces+=Face3(b,kind,part)
            }
            a.indices.forEach { i->
                val j=(i+1)%a.size
                faces+=Face3(listOf(a[i],a[j],b[j],b[i]),kind,part)
            }
        }
        fun polygon(outline: List<Pair<Double,Double>>,z: Double,h: Double,kind: Surface3,part: String) {
            val base=outline.map { (x,y)->Point3(x,y,z) }
            connect(base,base.map { it.copy(z=z+h) },kind,part)
        }
        fun box(x: Double,y: Double,z: Double,l: Double,w: Double,h: Double,kind: Surface3,part: String) =
            polygon(listOf(x to y,x+l to y,x+l to y+w,x to y+w),z,h,kind,part)
        fun beam(x: Double,y: Double,endX: Double,endY: Double,z: Double,width: Double,kind: Surface3,part: String) {
            val dx=endX-x;val dy=endY-y;val d=kotlin.math.hypot(dx,dy)
            val a=-dy/d*width/2;val b=dx/d*width/2
            polygon(listOf(x+a to y+b,endX+a to endY+b,endX-a to endY-b,x-a to y-b),z,width,kind,part)
        }
        fun pillar(x: Double,y: Double,z: Double,x2: Double,y2: Double,z2: Double,side: Double,kind: Surface3,part: String) {
            fun square(a: Double,b: Double,c: Double)=listOf(Point3(a,b,c),Point3(a+side,b,c),Point3(a+side,b+side,c),Point3(a,b+side,c))
            connect(square(x,y,z),square(x2,y2,z2),kind,part)
        }
        fun cylinder(x: Double,y: Double,z: Double,r: Double,h: Double,kind: Surface3,part: String) =
            tube(x,y,z,r,0.0,h,kind,part)
        fun tube(x: Double,y: Double,z: Double,outer: Double,inner: Double,h: Double,kind: Surface3,part: String) {
            fun ring(r: Double,elevation: Double)=List(20){i->
                val angle=i*PI/10;Point3(x+r*cos(angle),y+r*sin(angle),elevation)
            }
            annulus(ring(outer,z),ring(outer,z+h),ring(inner,z),ring(inner,z+h),inner>0,kind,part)
        }
        fun axisTube(x: Double,y: Double,z: Double,l: Double,outer: Double,inner: Double,kind: Surface3,part: String,alongY: Boolean=false) {
            fun ring(r: Double,along: Double)=List(20){i->
                val angle=i*PI/10
                val cross=y+r*cos(angle)
                val height=z+r*sin(angle)
                if(alongY)Point3(cross,along,height) else Point3(along,cross,height)
            }
            annulus(ring(outer,x),ring(outer,x+l),ring(inner,x),ring(inner,x+l),inner>0,kind,part)
        }
        private fun annulus(a: List<Point3>,b: List<Point3>,c: List<Point3>,d: List<Point3>,hollow: Boolean,kind: Surface3,part: String) {
            connect(a,b,kind,part,caps=!hollow)
            if(!hollow)return
            connect(d,c,kind,part,caps=false)
            listOf(a to c,b to d).forEach{ (outside,inside)->outside.indices.forEach { i->
                val j=(i+1)%outside.size
                faces+=Face3(listOf(outside[i],outside[j],inside[j],inside[i]),kind,part)
            }}
        }
        fun grid(z: Double,width: Double,length: Double,kind: Surface3) {
            for(i in 0 until (width/.67).toInt())for(j in 0 until (length/.63).toInt())
                box(i*.67+.014,j*.63+.014,z,.642,.602,.10,kind,"بلاطة")
        }
        fun incline(l: Double,w: Double,drop: Double,h: Double) {
            val deck=listOf(Point3(0.0,0.0,drop+h),Point3(l,0.0,h),Point3(l,w,h),Point3(0.0,w,drop+h))
            connect(deck.map{it.copy(z=it.z-h)},deck,Surface3.Concrete,"البلاطة المائلة")
        }
        fun labExample(id: String): String {
            when(id) {
                "sieve" -> repeat(3){i->
                    tube(0.0,0.0,i*.31,1.01,.93,.30,Surface3.Steel,"منخل")
                    cylinder(0.0,0.0,i*.31+.04,.92,.06,Surface3.Gravel,"ركام")
                }
                "soil_moisture","aggregate","asphalt_voids" -> repeat(3){i->
                    val x=i*1.45;val kind=if(id=="soil_moisture")Surface3.Excavation else Surface3.Gravel
                    cylinder(x,.0,.0,.63,.07,Surface3.Steel,"قاع علبة")
                    tube(x,.0,.07,.63,.56,.42,Surface3.Steel,"علبة قياس")
                    if(i>0)cylinder(x,.0,.07,.53,if(i==1).34 else .24,
                        if(id=="soil_moisture"&&i==1)Surface3.WetSoil else kind,
                        if(id=="soil_moisture"){if(i==1)"تربة رطبة" else "تربة جافة"} else "عينة ${i+1}")
                }
                "slump","sand_cone" -> {
                    box(0.0,0.0,0.0,2.6,2.1,.13,Surface3.Steel,"لوحة القياس")
                    fun ring(r: Double,z: Double)=List(20){i->
                        val a=i*PI/10;Point3(1.0+r*cos(a),.95+r*sin(a),z)
                    }
                    connect(ring(.68,.13),ring(.37,1.72),Surface3.Steel,"المخروط")
                    box(1.99,.93,.13,.08,.12,1.59,Surface3.Accent,"مقياس الارتفاع")
                }
                "concrete_compression","mortar_compression" -> {
                    box(-.27,-.26,0.0,1.87,1.65,.17,Surface3.Steel,"قاعدة جهاز الضغط")
                    box(0.0,0.0,.17,1.3,1.1,1.25,Surface3.Concrete,"العينة")
                    box(-.23,-.21,1.42,1.74,1.5,.19,Surface3.Steel,"لوح التحميل")
                    cylinder(.65,.55,1.61,.25,.58,Surface3.Steel,"مكبس")
                }
                else->{
                    val asphalt=id=="asphalt_core"
                    tube(0.0,0.0,0.0,.75,.68,.70,Surface3.Steel,"القالب")
                    cylinder(0.0,0.0,0.0,.67,.56,if(asphalt)Surface3.Asphalt else Surface3.Excavation,"عينة")
                    if(id in setOf("proctor","compaction")) {
                        cylinder(0.0,0.0,.74,.39,.12,Surface3.Steel,"قدم الدك")
                        cylinder(0.0,0.0,.86,.12,.98,Surface3.Steel,"دكاك")
                    }
                }
            }
            return "الأداة والعينة توضّحان موضع القياس. النتيجة تأتي من القراءات الفعلية."
        }
        fun engineeringExample(id: String): String {
            when(id) {
                "tank","cylindrical_tank"->{
                    if(id=="tank") {
                        box(0.0,0.0,0.0,3.0,2.0,.16,Surface3.Concrete,"قاع الخزان")
                        box(0.0,1.82,.16,3.0,.18,1.5,Surface3.Concrete,"الجدار الخلفي")
                        for(x in listOf(0.0,2.81))box(x,0.0,.16,.19,1.82,1.5,Surface3.Concrete,"الجدار الجانبي")
                        box(.19,.06,.16,2.62,1.76,.77,Surface3.Water,"مياه الخزان")
                    } else {
                        tube(0.0,0.0,0.0,1.03,.91,1.45,Surface3.Tank,"حدود الخزان")
                        cylinder(0.0,0.0,0.0,.91,.80,Surface3.Water,"مياه الخزان")
                    }
                }
                "excavation","sloped_excavation"-> {
                    box(0.0,0.0,0.0,3.0,2.15,.18,Surface3.Excavation,"قاع الحفر")
                    box(0.0,1.98,.18,3.0,.17,1.25,Surface3.Excavation,"جانب الحفر")
                    box(0.0,0.0,.18,.24,2.0,1.25,Surface3.Excavation,"جانب الحفر")
                }
                "form_beam","form_column"-> {
                    box(0.0,0.0,0.0,2.66,1.65,.13,Surface3.Plywood,"قاع الشدة")
                    for(y in listOf(0.0,1.53))box(0.0,y,.13,2.66,.12,1.13,Surface3.Plywood,"وجه الشدة")
                }
                "round_column"->cylinder(0.0,0.0,0.0,.52,2.7,Surface3.Concrete,"العمود")
                "pipe_insulation"->axisTube(0.0,0.0,.8,2.7,.65,.47,Surface3.Insulation,"العزل")
                else-> {
                    val kind=if(id=="insulation")Surface3.Insulation else Surface3.Concrete
                    val (l,w,h)=when(id) {
                        "column"->Triple(.76,.68,2.7)
                        "wall_concrete"->Triple(2.8,.38,1.8)
                        "beam"->Triple(3.1,.51,.72)
                        "footing"->Triple(2.4,1.88,.61)
                        "sealant"->Triple(2.6,.16,.17)
                        else->Triple(3.1,1.92,.38)
                    }
                    box(0.0,0.0,0.0,l,w,h,kind,"العنصر")
                }
            }
            return "أدخل الأبعاد الفعلية لعرض شكل المشروع إذا كانت القياسات تكفي لبنائه."
        }
    }
}

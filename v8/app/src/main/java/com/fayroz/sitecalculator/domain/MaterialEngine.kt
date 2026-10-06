package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.ceil

object MaterialEngine {
    data class MortarPreset(
        val name:String,
        val cement:Double,
        val sand:Double,
        val thicknessMm:Double,
        val waste:Double
    )

    val plasterPresets=listOf(
        MortarPreset("محارة عادية 1:4",1.0,4.0,15.0,5.0),
        MortarPreset("محارة أغنى 1:3",1.0,3.0,15.0,5.0),
        MortarPreset("محارة اقتصادية 1:5",1.0,5.0,15.0,5.0)
    )

    val splashPresets=listOf(
        MortarPreset("طرطشة 1:2",1.0,2.0,5.0,10.0),
        MortarPreset("طرطشة غنية 1:1.5",1.0,1.5,5.0,10.0)
    )

    val screedPresets=listOf(
        MortarPreset("تسوية 1:4",1.0,4.0,50.0,5.0),
        MortarPreset("تسوية أقوى 1:3",1.0,3.0,50.0,5.0)
    )

    fun mortar(
        toolId:String,
        title:String,
        area:Double,
        preset:MortarPreset
    ):MaterialResult{
        val wet=area*(preset.thicknessMm/1000.0)
        val dry=wet*1.33*(1+preset.waste/100.0)
        val parts=preset.cement+preset.sand
        val cementKg=dry*(preset.cement/parts)*1440.0
        val bags=ceil(cementKg/50.0).toInt()
        val sand=dry*(preset.sand/parts)

        return MaterialResult(
            toolId=toolId,
            title=title,
            sourceQuantity=area,
            sourceUnit="م²",
            lines=listOf(
                MaterialLine("أسمنت","$bags شيكارة ≈ ${f(cementKg)} كجم"),
                MaterialLine("رمل","${f(sand)} م³"),
                MaterialLine("مونة منفذة","${f(wet)} م³")
            ),
            explanation="${preset.name} • سمك ${f(preset.thicknessMm)} مم • هالك ${f(preset.waste)}%. القيم دي بداية للحساب وتقدر تغيرها."
        )
    }

    fun masonry(
        area:Double,
        wallThickness:Double=.12,
        brickL:Double=.25,
        brickW:Double=.12,
        brickH:Double=.06,
        joint:Double=.01,
        waste:Double=5.0
    ):MaterialResult{
        val module=(brickL+joint)*(brickH+joint)
        val layers=kotlin.math.max(1.0,kotlin.math.round((wallThickness+joint)/(brickW+joint)))
        val net=if(module>0)area/module*layers else 0.0
        val bricks=ceil(net*(1+waste/100.0)).toInt()
        val wet=(area*wallThickness-brickL*brickW*brickH*net).coerceAtLeast(0.0)
        val dry=wet*1.33
        val cementKg=dry/6.0*1440.0
        val sand=dry*5.0/6.0

        return MaterialResult(
            "masonry","مباني وطوب",area,"م²",
            listOf(
                MaterialLine("طوب","$bricks طوبة"),
                MaterialLine("أسمنت المونة","${ceil(cementKg/50).toInt()} شيكارة"),
                MaterialLine("رمل المونة","${f(sand)} م³")
            ),
            "افتراضي: حائط 12 سم، طوبة 25×12×6 سم، لحام 10 مم، مونة 1:5، هالك طوب ${f(waste)}%."
        )
    }

    fun tile(area:Double,waste:Double=7.0,tileW:Double=.60,tileH:Double=.60):MaterialResult{
        val purchase=area*(1+waste/100.0)
        val piece=tileW*tileH
        val pieces=if(piece>0)ceil(purchase/piece).toInt() else 0
        return MaterialResult(
            "tile","البلاط والكراتين",area,"م²",
            listOf(
                MaterialLine("مساحة الشراء","${f(purchase)} م²"),
                MaterialLine("عدد البلاطات","$pieces بلاطة")
            ),
            "افتراضي 60×60 سم وهالك ${f(waste)}%. عدد الكراتين يتحدد من عدد القطع المكتوب على الكرتونة."
        )
    }

    fun adhesive(area:Double,rate:Double=5.0,bagKg:Double=20.0,waste:Double=5.0):MaterialResult{
        val kg=area*rate*(1+waste/100.0)
        return MaterialResult(
            "adhesive","لاصق السيراميك",area,"م²",
            listOf(
                MaterialLine("لاصق","${f(kg)} كجم"),
                MaterialLine("شكاير","${ceil(kg/bagKg).toInt()} شيكارة")
            ),
            "معدل بداية ${f(rate)} كجم/م². نشرة المنتج هي المرجع."
        )
    }

    fun paint(area:Double,coverage:Double=10.0,coats:Int=2,waste:Double=5.0):MaterialResult{
        val liters=if(coverage>0)area*coats/coverage*(1+waste/100.0) else 0.0
        return MaterialResult(
            "paint","دهان",area,"م²",
            listOf(MaterialLine("دهان","${f(liters)} لتر")),
            "افتراضي تغطية ${f(coverage)} م²/لتر، $coats وجه، هالك ${f(waste)}%."
        )
    }

    fun waterproof(area:Double,rate:Double=1.5,coats:Int=2,waste:Double=5.0):MaterialResult{
        val kg=area*rate*coats*(1+waste/100.0)
        return MaterialResult(
            "waterproof","عزل أسمنتي / دهان",area,"م²",
            listOf(MaterialLine("خامة عزل","${f(kg)} كجم")),
            "افتراضي ${f(rate)} كجم/م²/وجه، $coats وجه. راجع نشرة المنتج."
        )
    }

    fun gypsum(area:Double,waste:Double=10.0):MaterialResult{
        val purchase=area*(1+waste/100.0)
        val boards=ceil(purchase/(1.2*2.4)).toInt()
        return MaterialResult(
            "gypsum","جبس بورد",area,"م²",
            listOf(
                MaterialLine("ألواح","$boards لوح"),
                MaterialLine("مساحة شراء","${f(purchase)} م²")
            ),
            "افتراضي لوح 1.20×2.40 م وهالك ${f(waste)}%. القطاعات حسب نظام التركيب."
        )
    }

    fun aggregate(summary:List<SummaryLine>):List<MaterialResult>{
        val out=mutableListOf<MaterialResult>()
        fun qty(vararg names:String)=summary.filter{it.name in names}.sumOf{it.quantity}

        val plasterQty=qty("محارة الحوائط","محارة السقف")
        if(plasterQty>0)out+=mortar("plaster","خامات المحارة",plasterQty,plasterPresets.first())

        val masonryQty=qty("مباني")
        if(masonryQty>0)out+=masonry(masonryQty)

        val screedQty=qty("مونة تسوية الأرضيات")
        if(screedQty>0)out+=mortar("screed","مونة تسوية الأرضيات",screedQty,screedPresets.first())

        val floorQty=qty("الأرضيات")
        if(floorQty>0)out+=tile(floorQty).copy(title="بلاط الأرضيات")

        val wallTileQty=qty("سيراميك الحوائط")
        if(wallTileQty>0)out+=tile(wallTileQty).copy(title="سيراميك الحوائط")

        val paintQty=qty("دهان الحوائط","دهان السقف")
        if(paintQty>0)out+=paint(paintQty).copy(title="دهانات الحوائط والسقف")

        val waterproofQty=qty("عزل الأرضية")
        if(waterproofQty>0)out+=waterproof(waterproofQty)

        val gypsumQty=qty("سقف جبس بورد")
        if(gypsumQty>0)out+=gypsum(gypsumQty)

        return out
    }

    fun forSummary(line:SummaryLine):MaterialResult?=when(line.name){
        "محارة الحوائط","محارة السقف" -> mortar("plaster","خامات المحارة",line.quantity,plasterPresets.first())
        "طرطشة الحوائط","طرطشة الأسقف","مونة الطرطشة" -> mortar("splash","مونة الطرطشة",line.quantity,splashPresets.first())
        "مونة تسوية الأرضيات" -> mortar("screed","مونة تسوية الأرضيات",line.quantity,screedPresets.first())
        "مباني" -> masonry(line.quantity)
        "الأرضيات","سيراميك الحوائط" -> tile(line.quantity)
        "دهان الحوائط","دهان السقف" -> paint(line.quantity)
        "عزل الأرضية" -> waterproof(line.quantity)
        "سقف جبس بورد" -> gypsum(line.quantity)
        else -> null
    }

    private fun f(v:Double)=String.format(java.util.Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
}


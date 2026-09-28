package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*

object Catalog {
    data class ItemDef(
        val name:String,
        val unit:UnitType,
        val kind:CalcKind,
        val waste:Double=0.0,
        val tileHeight:Double=2.4,
        val upstand:Double=.20,
        val layerThickness:Double=0.0
    ){
        fun create()=Takeoff(
            name=name,unit=unit,kind=kind,waste=waste,
            tileHeight=tileHeight,upstand=upstand,layerThickness=layerThickness
        )
    }

    val items=listOf(
        ItemDef("مباني",UnitType.AREA,CalcKind.WALLS),
        ItemDef("محارة الحوائط",UnitType.AREA,CalcKind.WALLS),
        ItemDef("محارة السقف",UnitType.AREA,CalcKind.CEILING),
        ItemDef("دهان الحوائط",UnitType.AREA,CalcKind.WALLS),
        ItemDef("دهان السقف",UnitType.AREA,CalcKind.CEILING),
        ItemDef("الأرضيات",UnitType.AREA,CalcKind.FLOOR,7.0),
        ItemDef("سيراميك الحوائط",UnitType.AREA,CalcKind.WALL_TILES,5.0,2.4),
        ItemDef("الوزرات",UnitType.LENGTH,CalcKind.SKIRTING,5.0),
        ItemDef("عزل الأرضية",UnitType.AREA,CalcKind.WATERPROOF,5.0,upstand=.20),
        ItemDef("سقف جبس بورد",UnitType.AREA,CalcKind.FLOOR,10.0),
        ItemDef("مونة تسوية الأرضيات",UnitType.VOLUME,CalcKind.SCREED_VOLUME,5.0,layerThickness=.05)
    )

    val roomTypes=listOf("غرفة نوم","معيشة","صالة","حمام","مطبخ","بلكونة","ممر","غرفة مخصصة")

    fun suggested(type:String):Set<String> = when(type){
        "حمام"->setOf("محارة الحوائط","محارة السقف","سيراميك الحوائط","الأرضيات","عزل الأرضية","مونة تسوية الأرضيات")
        "مطبخ"->setOf("محارة الحوائط","محارة السقف","سيراميك الحوائط","الأرضيات","مونة تسوية الأرضيات")
        "بلكونة"->setOf("محارة الحوائط","دهان الحوائط","الأرضيات","عزل الأرضية","الوزرات")
        else->setOf("محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف","الأرضيات","الوزرات")
    }
}

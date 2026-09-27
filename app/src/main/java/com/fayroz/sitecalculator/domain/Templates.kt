package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*

object Templates {
    private fun walls(name:String,waste:Double=0.0)=TakeoffItem(
        name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.ROOM_WALLS,wastePercent=waste
    )
    private fun floor(name:String,waste:Double)=TakeoffItem(
        name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.FLOOR_SURFACES,wastePercent=waste
    )

    fun forRoom(type:String,defaults:AppDefaults):List<TakeoffItem> = when(type){
        "حمام" -> listOf(
            TakeoffItem("سيراميك الحوائط","تشطيبات",MeasureUnit.AREA,CalcMethod.WALL_TILES,defaults.wallTileWaste,defaults.defaultTileHeight),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem("عزل الأرضية","عزل",MeasureUnit.AREA,CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand),
            TakeoffItem("سقف جبس بورد","جبس",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES,defaults.gypsumWaste)
        )
        "مطبخ" -> listOf(
            walls("محارة الحوائط"),
            TakeoffItem("سيراميك الحوائط","تشطيبات",MeasureUnit.AREA,CalcMethod.WALL_TILES,defaults.wallTileWaste,1.50),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem("دهان السقف","دهانات",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES)
        )
        "بلكونة" -> listOf(
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem("عزل الأرضية","عزل",MeasureUnit.AREA,CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand),
            walls("محارة الحوائط"),
            walls("دهان الحوائط")
        )
        else -> listOf(
            walls("محارة الحوائط"),
            TakeoffItem("محارة السقف","تشطيبات",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES),
            walls("دهان الحوائط"),
            TakeoffItem("دهان السقف","دهانات",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem("الوزرات","تشطيبات",MeasureUnit.LENGTH,CalcMethod.SKIRTING,defaults.skirtingWaste)
        )
    }

    fun defaultForName(name:String,defaults:AppDefaults):TakeoffItem = when(name){
        "مباني" -> TakeoffItem(name,"مباني",MeasureUnit.AREA,CalcMethod.ROOM_WALLS)
        "محارة الحوائط" -> TakeoffItem(name,"محارة",MeasureUnit.AREA,CalcMethod.ROOM_WALLS)
        "محارة السقف" -> TakeoffItem(name,"محارة",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES)
        "دهان الحوائط" -> TakeoffItem(name,"دهانات",MeasureUnit.AREA,CalcMethod.ROOM_WALLS)
        "دهان السقف" -> TakeoffItem(name,"دهانات",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES)
        "الأرضيات" -> TakeoffItem(name,"أرضيات",MeasureUnit.AREA,CalcMethod.FLOOR_SURFACES,defaults.floorWaste)
        "سيراميك الحوائط" -> TakeoffItem(name,"سيراميك",MeasureUnit.AREA,CalcMethod.WALL_TILES,defaults.wallTileWaste,defaults.defaultTileHeight)
        "الوزرات" -> TakeoffItem(name,"أرضيات",MeasureUnit.LENGTH,CalcMethod.SKIRTING,defaults.skirtingWaste)
        "عزل الأرضية" -> TakeoffItem(name,"عزل",MeasureUnit.AREA,CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand)
        "سقف جبس بورد" -> TakeoffItem(name,"جبس",MeasureUnit.AREA,CalcMethod.CEILING_SURFACES,defaults.gypsumWaste)
        "سكريد / مونة تسوية" -> TakeoffItem(name,"أرضيات",MeasureUnit.AREA,CalcMethod.FLOOR_SURFACES)
        "خرسانة بسيطة" -> TakeoffItem(name,"خرسانة",MeasureUnit.VOLUME,CalcMethod.DIRECT_VOLUME)
        "رخام / جرانيت" -> TakeoffItem(name,"رخام",MeasureUnit.AREA,CalcMethod.FLOOR_SURFACES,defaults.floorWaste)
        "واجهات / كسوات" -> TakeoffItem(name,"واجهات",MeasureUnit.AREA,CalcMethod.WALL_SEGMENTS)
        "كرانيش / حليات" -> TakeoffItem(name,"حليات",MeasureUnit.LENGTH,CalcMethod.DIRECT_LENGTH)
        else -> TakeoffItem(name,"مخصص",MeasureUnit.COUNT,CalcMethod.DIRECT_COUNT)
    }
}

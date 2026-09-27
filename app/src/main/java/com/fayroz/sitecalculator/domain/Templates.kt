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
            TakeoffItem(
                name="سيراميك الحوائط",category="تشطيبات",unit=MeasureUnit.AREA,
                method=CalcMethod.WALL_TILES,wastePercent=defaults.wallTileWaste,tileHeight=defaults.defaultTileHeight
            ),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem(
                name="عزل الأرضية",category="عزل",unit=MeasureUnit.AREA,
                method=CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand
            ),
            TakeoffItem(
                name="سقف جبس بورد",category="جبس",unit=MeasureUnit.AREA,
                method=CalcMethod.CEILING_SURFACES,wastePercent=defaults.gypsumWaste
            )
        )
        "مطبخ" -> listOf(
            walls("محارة الحوائط"),
            TakeoffItem(
                name="سيراميك الحوائط",category="تشطيبات",unit=MeasureUnit.AREA,
                method=CalcMethod.WALL_TILES,wastePercent=defaults.wallTileWaste,tileHeight=1.50
            ),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem(name="دهان السقف",category="دهانات",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES)
        )
        "بلكونة" -> listOf(
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem(
                name="عزل الأرضية",category="عزل",unit=MeasureUnit.AREA,
                method=CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand
            ),
            walls("محارة الحوائط"),
            walls("دهان الحوائط")
        )
        else -> listOf(
            walls("محارة الحوائط"),
            TakeoffItem(name="محارة السقف",category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES),
            walls("دهان الحوائط"),
            TakeoffItem(name="دهان السقف",category="دهانات",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES),
            floor("الأرضيات",defaults.floorWaste),
            TakeoffItem(
                name="الوزرات",category="تشطيبات",unit=MeasureUnit.LENGTH,
                method=CalcMethod.SKIRTING,wastePercent=defaults.skirtingWaste
            )
        )
    }

    fun defaultForName(name:String,defaults:AppDefaults):TakeoffItem = when(name){
        "مباني" -> TakeoffItem(name=name,category="مباني",unit=MeasureUnit.AREA,method=CalcMethod.ROOM_WALLS)
        "محارة الحوائط" -> TakeoffItem(name=name,category="محارة",unit=MeasureUnit.AREA,method=CalcMethod.ROOM_WALLS)
        "محارة السقف" -> TakeoffItem(name=name,category="محارة",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES)
        "دهان الحوائط" -> TakeoffItem(name=name,category="دهانات",unit=MeasureUnit.AREA,method=CalcMethod.ROOM_WALLS)
        "دهان السقف" -> TakeoffItem(name=name,category="دهانات",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES)
        "الأرضيات" -> TakeoffItem(
            name=name,category="أرضيات",unit=MeasureUnit.AREA,
            method=CalcMethod.FLOOR_SURFACES,wastePercent=defaults.floorWaste
        )
        "سيراميك الحوائط" -> TakeoffItem(
            name=name,category="سيراميك",unit=MeasureUnit.AREA,
            method=CalcMethod.WALL_TILES,wastePercent=defaults.wallTileWaste,tileHeight=defaults.defaultTileHeight
        )
        "الوزرات" -> TakeoffItem(
            name=name,category="أرضيات",unit=MeasureUnit.LENGTH,
            method=CalcMethod.SKIRTING,wastePercent=defaults.skirtingWaste
        )
        "عزل الأرضية" -> TakeoffItem(
            name=name,category="عزل",unit=MeasureUnit.AREA,
            method=CalcMethod.WATERPROOFING,waterproofUpstand=defaults.waterproofUpstand
        )
        "سقف جبس بورد" -> TakeoffItem(
            name=name,category="جبس",unit=MeasureUnit.AREA,
            method=CalcMethod.CEILING_SURFACES,wastePercent=defaults.gypsumWaste
        )
        "سكريد / مونة تسوية" -> TakeoffItem(
            name=name,category="أرضيات",unit=MeasureUnit.VOLUME,
            method=CalcMethod.FLOOR_LAYER_VOLUME,layerThickness=0.05
        )
        "خرسانة بسيطة" -> TakeoffItem(name=name,category="خرسانة",unit=MeasureUnit.VOLUME,method=CalcMethod.DIRECT_VOLUME)
        "رخام / جرانيت" -> TakeoffItem(
            name=name,category="رخام",unit=MeasureUnit.AREA,
            method=CalcMethod.FLOOR_SURFACES,wastePercent=defaults.floorWaste
        )
        "واجهات / كسوات" -> TakeoffItem(name=name,category="واجهات",unit=MeasureUnit.AREA,method=CalcMethod.WALL_SEGMENTS)
        "كرانيش / حليات" -> TakeoffItem(name=name,category="حليات",unit=MeasureUnit.LENGTH,method=CalcMethod.DIRECT_LENGTH)
        "جبس بورد جوانب ساقطة" -> TakeoffItem(name=name,category="جبس",unit=MeasureUnit.AREA,method=CalcMethod.DIRECT_AREA,wastePercent=defaults.gypsumWaste)
        "بيت نور / كوف" -> TakeoffItem(name=name,category="جبس",unit=MeasureUnit.LENGTH,method=CalcMethod.DIRECT_LENGTH)
        "سقف معلق بلاطات" -> TakeoffItem(
            name=name,category="أسقف",unit=MeasureUnit.AREA,
            method=CalcMethod.CEILING_SURFACES,wastePercent=defaults.gypsumWaste,
            pieceWidth=0.60,pieceHeight=0.60
        )
        else -> TakeoffItem(name=name,category="مخصص",unit=MeasureUnit.COUNT,method=CalcMethod.DIRECT_COUNT)
    }
}

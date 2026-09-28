package com.fayroz.sitecalculator.data

import com.fayroz.sitecalculator.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object LegacyMigration {
    fun fromV7(raw:String):MutableList<Project> = runCatching{
        val arr=JSONArray(raw)
        MutableList(arr.length()){i->project(arr.getJSONObject(i))}
    }.getOrElse{mutableListOf()}

    private fun project(o:JSONObject):Project{
        val sections=mutableListOf<Section>()
        val sa=o.optJSONArray("sections")?:JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val spaces=mutableListOf<Space>()
            val spa=so.optJSONArray("spaces")?:JSONArray()
            for(j in 0 until spa.length())spaces+=space(spa.getJSONObject(j))
            sections+=Section(
                id=so.optString("id",UUID.randomUUID().toString()),
                name=so.optString("name","دور / جزء"),
                spaces=spaces
            )
        }
        return Project(
            id=o.optString("id",UUID.randomUUID().toString()),
            name=o.optString("name","مشروع"),
            type=o.optString("type","شقة"),
            sections=sections,
            createdAt=o.optLong("createdAt",System.currentTimeMillis()),
            updatedAt=o.optLong("updatedAt",System.currentTimeMillis())
        )
    }

    private fun space(o:JSONObject):Space{
        val walls=mutableListOf<WallPart>()
        val wa=o.optJSONArray("walls")?:JSONArray()
        for(i in 0 until wa.length()){
            val w=wa.getJSONObject(i)
            walls+=WallPart(
                id=w.optString("id",UUID.randomUUID().toString()),
                name=w.optString("name","حائط"),
                length=w.optDouble("length"),
                height=w.optDouble("height",o.optDouble("height",3.0))
            )
        }

        val openings=mutableListOf<Opening>()
        val oa=o.optJSONArray("openings")?:JSONArray()
        for(i in 0 until oa.length()){
            val x=oa.getJSONObject(i)
            openings+=Opening(
                id=x.optString("id",UUID.randomUUID().toString()),
                kind=runCatching{OpeningKind.valueOf(x.optString("type","DOOR"))}.getOrDefault(OpeningKind.DOOR),
                width=x.optDouble("width"),
                height=x.optDouble("height"),
                sill=x.optDouble("sillHeight",0.0),
                count=x.optInt("count",1).coerceAtLeast(1)
            )
        }

        val takeoffs=mutableListOf<Takeoff>()
        val ta=o.optJSONArray("takeoffs")
        if(ta!=null){
            for(i in 0 until ta.length())takeoffs+=takeoff(ta.getJSONObject(i))
        }else{
            val works=o.optJSONArray("works")?:JSONArray()
            for(i in 0 until works.length()){
                legacyItem(works.optString(i))?.let{takeoffs+=it}
            }
        }

        return Space(
            id=o.optString("id",UUID.randomUUID().toString()),
            name=o.optString("name","مكان"),
            type=o.optString("type","غرفة"),
            length=o.optDouble("length"),width=o.optDouble("width"),height=o.optDouble("height",3.0),
            repeatCount=o.optInt("repeatCount",1).coerceAtLeast(1),
            walls=walls,openings=openings,takeoffs=takeoffs,note=o.optString("note"),
            status=runCatching{WorkStatus.valueOf(o.optString("status","IN_PROGRESS"))}.getOrDefault(WorkStatus.IN_PROGRESS),
            updatedAt=o.optLong("updatedAt",System.currentTimeMillis())
        )
    }

    private fun takeoff(x:JSONObject):Takeoff{
        val kind=when(x.optString("method")){
            "ROOM_WALLS","WALL_SEGMENTS"->CalcKind.WALLS
            "CEILING_SURFACES"->CalcKind.CEILING
            "FLOOR_SURFACES"->CalcKind.FLOOR
            "WALL_TILES"->CalcKind.WALL_TILES
            "SKIRTING"->CalcKind.SKIRTING
            "WATERPROOFING"->CalcKind.WATERPROOF
            "FLOOR_LAYER_VOLUME"->CalcKind.SCREED_VOLUME
            else->CalcKind.DIRECT
        }
        return Takeoff(
            id=x.optString("id",UUID.randomUUID().toString()),
            name=normalize(x.optString("name","بند")),
            unit=runCatching{UnitType.valueOf(x.optString("unit","AREA"))}.getOrDefault(UnitType.AREA),
            kind=kind,
            waste=x.optDouble("wastePercent",0.0),
            tileHeight=x.optDouble("tileHeight",2.4),
            upstand=x.optDouble("waterproofUpstand",.20),
            layerThickness=x.optDouble("layerThickness"),
            directValue=x.optDouble("directValue"),
            manualValue=if(x.isNull("manualOverride"))null else x.optDouble("manualOverride")
        )
    }

    private fun legacyItem(raw:String):Takeoff?=when(normalize(raw)){
        "محارة الحوائط"->Takeoff(name="محارة الحوائط",unit=UnitType.AREA,kind=CalcKind.WALLS)
        "محارة السقف"->Takeoff(name="محارة السقف",unit=UnitType.AREA,kind=CalcKind.CEILING)
        "دهان الحوائط"->Takeoff(name="دهان الحوائط",unit=UnitType.AREA,kind=CalcKind.WALLS)
        "دهان السقف"->Takeoff(name="دهان السقف",unit=UnitType.AREA,kind=CalcKind.CEILING)
        "الأرضيات"->Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.FLOOR,waste=7.0)
        "سيراميك الحوائط"->Takeoff(name="سيراميك الحوائط",unit=UnitType.AREA,kind=CalcKind.WALL_TILES,waste=5.0)
        "الوزرات"->Takeoff(name="الوزرات",unit=UnitType.LENGTH,kind=CalcKind.SKIRTING,waste=5.0)
        "عزل الأرضية"->Takeoff(name="عزل الأرضية",unit=UnitType.AREA,kind=CalcKind.WATERPROOF,waste=5.0)
        "سقف جبس بورد"->Takeoff(name="سقف جبس بورد",unit=UnitType.AREA,kind=CalcKind.FLOOR,waste=10.0)
        "مباني"->Takeoff(name="مباني",unit=UnitType.AREA,kind=CalcKind.WALLS)
        else->null
    }

    private fun normalize(name:String)=
        if(name=="سكريد / مونة تسوية")"مونة تسوية الأرضيات" else name
}

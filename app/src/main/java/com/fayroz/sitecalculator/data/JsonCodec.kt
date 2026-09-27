package com.fayroz.sitecalculator.data

import com.fayroz.sitecalculator.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object JsonCodec {
    fun projectsToJson(projects:List<SiteProject>):String =
        JSONArray().apply{ projects.forEach{put(projectToJson(it))} }.toString()

    fun projectsFromJson(raw:String):MutableList<SiteProject> = runCatching{
        val arr=JSONArray(raw)
        MutableList(arr.length()){i->projectFromJson(arr.getJSONObject(i))}
    }.getOrElse{mutableListOf()}

    fun spaceToJson(space:SpaceEntry):String = spaceObject(space).toString()
    fun spaceFromJson(raw:String):SpaceEntry? = runCatching{spaceFromObject(JSONObject(raw))}.getOrNull()

    private fun projectToJson(p:SiteProject)=JSONObject().apply{
        put("id",p.id);put("name",p.name);put("type",p.type)
        put("createdAt",p.createdAt);put("updatedAt",p.updatedAt)
        put("sections",JSONArray().apply{
            p.sections.forEach{s->
                put(JSONObject().apply{
                    put("id",s.id);put("name",s.name)
                    put("spaces",JSONArray().apply{s.spaces.forEach{put(spaceObject(it))}})
                })
            }
        })
    }

    private fun spaceObject(s:SpaceEntry)=JSONObject().apply{
        put("id",s.id);put("name",s.name);put("type",s.type)
        put("length",s.length);put("width",s.width);put("height",s.height)
        put("repeatCount",s.repeatCount);put("note",s.note)
        put("status",s.status.name);put("updatedAt",s.updatedAt)
        put("photoUris",JSONArray(s.photoUris))
        put("walls",JSONArray().apply{s.walls.forEach{w->
            put(JSONObject().apply{
                put("id",w.id);put("name",w.name);put("length",w.length);put("height",w.height);put("note",w.note)
            })
        }})
        put("floorSurfaces",surfacesArray(s.floorSurfaces))
        put("ceilingSurfaces",surfacesArray(s.ceilingSurfaces))
        put("openings",JSONArray().apply{s.openings.forEach{o->
            put(JSONObject().apply{
                put("id",o.id);put("wallId",o.wallId ?: JSONObject.NULL);put("type",o.type.name)
                put("width",o.width);put("height",o.height);put("sillHeight",o.sillHeight)
                put("revealDepth",o.revealDepth);put("count",o.count);put("note",o.note)
            })
        }})
        put("takeoffs",JSONArray().apply{s.takeoffs.forEach{t->put(takeoffObject(t))}})
    }

    private fun surfacesArray(list:List<SurfaceSegment>)=JSONArray().apply{list.forEach{x->
        put(JSONObject().apply{
            put("id",x.id);put("name",x.name);put("length",x.length);put("width",x.width)
            put("deductionArea",x.deductionArea);put("note",x.note)
        })
    }}

    private fun takeoffObject(t:TakeoffItem)=JSONObject().apply{
        put("id",t.id);put("name",t.name);put("category",t.category)
        put("unit",t.unit.name);put("method",t.method.name)
        put("wastePercent",t.wastePercent);put("tileHeight",t.tileHeight)
        put("waterproofUpstand",t.waterproofUpstand);put("layerThickness",t.layerThickness)
        put("includeOpeningReveals",t.includeOpeningReveals);put("wallIds",JSONArray(t.wallIds))
        put("directValue",t.directValue)
        put("pieceWidth",t.pieceWidth);put("pieceHeight",t.pieceHeight);put("piecesPerPack",t.piecesPerPack)
        if(t.manualOverride!=null) put("manualOverride",t.manualOverride) else put("manualOverride",JSONObject.NULL)
        put("overrideReason",t.overrideReason);put("note",t.note)
        put("adjustments",JSONArray().apply{t.adjustments.forEach{a->
            put(JSONObject().apply{
                put("id",a.id);put("type",a.type.name);put("amount",a.amount);put("note",a.note)
            })
        }})
    }

    private fun projectFromJson(o:JSONObject):SiteProject{
        val sections=mutableListOf<SectionEntry>()
        val sa=o.optJSONArray("sections") ?: JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val spaces=mutableListOf<SpaceEntry>()
            val spa=so.optJSONArray("spaces") ?: JSONArray()
            for(j in 0 until spa.length()) spaces+=spaceFromObject(spa.getJSONObject(j))
            sections+=SectionEntry(
                id=so.optString("id",UUID.randomUUID().toString()),
                name=so.optString("name","جزء"),
                spaces=spaces
            )
        }
        return SiteProject(
            id=o.optString("id",UUID.randomUUID().toString()),
            name=o.optString("name","مشروع"),
            type=o.optString("type","شقة"),
            sections=sections,
            createdAt=o.optLong("createdAt",System.currentTimeMillis()),
            updatedAt=o.optLong("updatedAt",System.currentTimeMillis())
        )
    }

    private fun spaceFromObject(o:JSONObject):SpaceEntry{
        val walls=mutableListOf<WallSegment>()
        val wa=o.optJSONArray("walls") ?: JSONArray()
        for(i in 0 until wa.length()){
            val x=wa.getJSONObject(i)
            walls+=WallSegment(
                id=x.optString("id",UUID.randomUUID().toString()),
                name=x.optString("name","حائط"),
                length=x.optDouble("length",0.0),
                height=x.optDouble("height",o.optDouble("height",3.0)),
                note=x.optString("note","")
            )
        }
        val floors=surfacesFrom(o.optJSONArray("floorSurfaces"))
        val ceilings=surfacesFrom(o.optJSONArray("ceilingSurfaces"))

        val openings=mutableListOf<Opening>()
        val oa=o.optJSONArray("openings") ?: JSONArray()
        for(i in 0 until oa.length()){
            val x=oa.getJSONObject(i)
            openings+=Opening(
                id=x.optString("id",UUID.randomUUID().toString()),
                wallId=if(x.isNull("wallId"))null else x.optString("wallId"),
                type=runCatching{OpeningType.valueOf(x.optString("type","DOOR"))}.getOrDefault(OpeningType.DOOR),
                width=x.optDouble("width",0.0),height=x.optDouble("height",0.0),
                sillHeight=x.optDouble("sillHeight",0.0),revealDepth=x.optDouble("revealDepth",0.0),
                count=x.optInt("count",1).coerceAtLeast(1),note=x.optString("note","")
            )
        }

        // Migration from older schemas where openings were totals only.
        if(openings.isEmpty()){
            val doorArea=o.optDouble("doorArea",0.0)
            val windowArea=o.optDouble("windowArea",0.0)
            val doorWidth=o.optDouble("doorWidth",0.0)
            if(doorArea>0){
                val dw=if(doorWidth>0)doorWidth else 1.0
                openings+=Opening(type=OpeningType.DOOR,width=dw,height=doorArea/dw)
            }
            if(windowArea>0) openings+=Opening(type=OpeningType.WINDOW,width=1.0,height=windowArea,sillHeight=0.9)
        }

        val takeoffs=mutableListOf<TakeoffItem>()
        val ta=o.optJSONArray("takeoffs")
        if(ta!=null){
            for(i in 0 until ta.length()) takeoffs+=takeoffFrom(ta.getJSONObject(i))
        }else{
            // Migration from old works array.
            val works=o.optJSONArray("works") ?: JSONArray()
            for(i in 0 until works.length()){
                val name=works.optString(i)
                if(name.isNotBlank()) takeoffs+=legacyTakeoff(name,o)
            }
        }

        return SpaceEntry(
            id=o.optString("id",UUID.randomUUID().toString()),
            name=o.optString("name","فراغ"),
            type=o.optString("type","فراغ"),
            length=o.optDouble("length",0.0),width=o.optDouble("width",0.0),height=o.optDouble("height",3.0),
            repeatCount=o.optInt("repeatCount",1).coerceAtLeast(1),
            walls=walls,floorSurfaces=floors,ceilingSurfaces=ceilings,
            openings=openings,takeoffs=takeoffs,note=o.optString("note",""),
            photoUris=buildList{
                val pa=o.optJSONArray("photoUris") ?: JSONArray()
                for(i in 0 until pa.length()) add(pa.optString(i))
            },
            status=runCatching{CaptureStatus.valueOf(o.optString("status","IN_PROGRESS"))}.getOrDefault(CaptureStatus.IN_PROGRESS),
            updatedAt=o.optLong("updatedAt",System.currentTimeMillis())
        )
    }

    private fun surfacesFrom(arr:JSONArray?):List<SurfaceSegment>{
        if(arr==null)return emptyList()
        return buildList{
            for(i in 0 until arr.length()){
                val x=arr.getJSONObject(i)
                add(SurfaceSegment(
                    id=x.optString("id",UUID.randomUUID().toString()),
                    name=x.optString("name","مسطح"),
                    length=x.optDouble("length",0.0),width=x.optDouble("width",0.0),
                    deductionArea=x.optDouble("deductionArea",0.0),note=x.optString("note","")
                ))
            }
        }
    }

    private fun takeoffFrom(x:JSONObject):TakeoffItem{
        val adjustments=mutableListOf<Adjustment>()
        val aa=x.optJSONArray("adjustments") ?: JSONArray()
        for(i in 0 until aa.length()){
            val a=aa.getJSONObject(i)
            adjustments+=Adjustment(
                id=a.optString("id",UUID.randomUUID().toString()),
                type=runCatching{AdjustmentType.valueOf(a.optString("type","ADD"))}.getOrDefault(AdjustmentType.ADD),
                amount=a.optDouble("amount",0.0),note=a.optString("note","")
            )
        }
        return TakeoffItem(
            id=x.optString("id",UUID.randomUUID().toString()),
            name=x.optString("name","بند"),category=x.optString("category","مخصص"),
            unit=runCatching{MeasureUnit.valueOf(x.optString("unit","AREA"))}.getOrDefault(MeasureUnit.AREA),
            method=runCatching{CalcMethod.valueOf(x.optString("method","DIRECT_AREA"))}.getOrDefault(CalcMethod.DIRECT_AREA),
            wastePercent=x.optDouble("wastePercent",0.0),tileHeight=x.optDouble("tileHeight",2.4),
            waterproofUpstand=x.optDouble("waterproofUpstand",0.20),
            layerThickness=x.optDouble("layerThickness",0.0),
            includeOpeningReveals=x.optBoolean("includeOpeningReveals",false),
            wallIds=buildList{
                val ids=x.optJSONArray("wallIds") ?: JSONArray()
                for(i in 0 until ids.length())add(ids.optString(i))
            },
            directValue=x.optDouble("directValue",0.0),
            pieceWidth=x.optDouble("pieceWidth",0.0),
            pieceHeight=x.optDouble("pieceHeight",0.0),
            piecesPerPack=x.optInt("piecesPerPack",0),
            manualOverride=if(x.isNull("manualOverride"))null else x.optDouble("manualOverride"),
            overrideReason=x.optString("overrideReason",""),adjustments=adjustments,note=x.optString("note","")
        )
    }

    private fun legacyTakeoff(name:String,o:JSONObject):TakeoffItem{
        val waste=o.optDouble("waste",7.0)
        val tileHeight=o.optDouble("tileHeight",2.4)
        return when(name){
            "محارة الحوائط","دهان الحوائط","مباني" -> TakeoffItem(name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.ROOM_WALLS)
            "محارة السقف","دهان السقف" -> TakeoffItem(name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.CEILING_SURFACES)
            "الأرضيات","سقف جبس بورد" -> TakeoffItem(name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.FLOOR_SURFACES,wastePercent=waste)
            "سيراميك الحوائط" -> TakeoffItem(name=name,category="تشطيبات",unit=MeasureUnit.AREA,method=CalcMethod.WALL_TILES,wastePercent=waste,tileHeight=tileHeight)
            "الوزرات" -> TakeoffItem(name=name,category="تشطيبات",unit=MeasureUnit.LENGTH,method=CalcMethod.SKIRTING,wastePercent=waste)
            "عزل الأرضية" -> TakeoffItem(name=name,category="عزل",unit=MeasureUnit.AREA,method=CalcMethod.WATERPROOFING,wastePercent=waste,waterproofUpstand=0.20)
            else -> TakeoffItem(name=name,category="مخصص",unit=MeasureUnit.AREA,method=CalcMethod.DIRECT_AREA)
        }
    }
}

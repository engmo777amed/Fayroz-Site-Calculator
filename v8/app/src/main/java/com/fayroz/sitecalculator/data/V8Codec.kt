package com.fayroz.sitecalculator.data

import com.fayroz.sitecalculator.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object V8Codec {
    fun encodeProjects(list:List<Project>):String =
        JSONArray().apply{list.forEach{put(projectObj(it))}}.toString()

    fun decodeProjects(raw:String):MutableList<Project> = runCatching{
        val a=JSONArray(raw)
        MutableList(a.length()){i->projectFrom(a.getJSONObject(i))}
    }.getOrElse{mutableListOf()}

    fun encodeSpace(s:Space)=spaceObj(s).toString()
    fun decodeSpace(raw:String)=runCatching{spaceFrom(JSONObject(raw))}.getOrNull()

    private fun projectObj(p:Project)=JSONObject().apply{
        put("id",p.id);put("name",p.name);put("type",p.type)
        put("createdAt",p.createdAt);put("updatedAt",p.updatedAt)
        put("sections",JSONArray().apply{p.sections.forEach{s->
            put(JSONObject().apply{
                put("id",s.id);put("name",s.name)
                put("spaces",JSONArray().apply{s.spaces.forEach{put(spaceObj(it))}})
            })
        }})
        put("calculations",JSONArray().apply{p.calculations.forEach{c->
            put(JSONObject().apply{
                put("id",c.id);put("toolId",c.toolId);put("title",c.title);put("summary",c.summary)
                put("sourceQuantity",c.sourceQuantity);put("unit",c.unit)
                put("sectionId",c.sectionId?:JSONObject.NULL);put("spaceId",c.spaceId?:JSONObject.NULL)
                put("createdAt",c.createdAt)
            })
        }})
    }

    private fun surfaceObj(s:SurfacePart)=JSONObject().apply{
        put("id",s.id);put("name",s.name);put("length",s.length);put("width",s.width)
        put("deductionArea",s.deductionArea);put("note",s.note)
    }

    private fun spaceObj(s:Space)=JSONObject().apply{
        put("id",s.id);put("name",s.name);put("type",s.type)
        put("length",s.length);put("width",s.width);put("height",s.height)
        put("repeatCount",s.repeatCount);put("note",s.note);put("status",s.status.name);put("updatedAt",s.updatedAt)
        put("photoUris",JSONArray(s.photoUris))
        put("walls",JSONArray().apply{s.walls.forEach{w->put(JSONObject().apply{
            put("id",w.id);put("name",w.name);put("length",w.length);put("height",w.height);put("note",w.note)
        })}})
        put("floorSurfaces",JSONArray().apply{s.floorSurfaces.forEach{put(surfaceObj(it))}})
        put("ceilingSurfaces",JSONArray().apply{s.ceilingSurfaces.forEach{put(surfaceObj(it))}})
        put("openings",JSONArray().apply{s.openings.forEach{o->put(JSONObject().apply{
            put("id",o.id);put("kind",o.kind.name);put("width",o.width);put("height",o.height)
            put("sill",o.sill);put("count",o.count);put("wallId",o.wallId?:JSONObject.NULL)
            put("revealDepth",o.revealDepth);put("note",o.note)
        })}})
        put("takeoffs",JSONArray().apply{s.takeoffs.forEach{t->put(JSONObject().apply{
            put("id",t.id);put("name",t.name);put("unit",t.unit.name);put("kind",t.kind.name)
            put("waste",t.waste);put("tileHeight",t.tileHeight);put("upstand",t.upstand);put("layerThickness",t.layerThickness)
            put("directValue",t.directValue)
            if(t.manualValue==null)put("manualValue",JSONObject.NULL)else put("manualValue",t.manualValue)
            put("includeOpeningReveals",t.includeOpeningReveals)
            put("wallIds",JSONArray(t.wallIds))
            put("pieceWidth",t.pieceWidth);put("pieceHeight",t.pieceHeight);put("piecesPerPack",t.piecesPerPack)
            put("overrideReason",t.overrideReason);put("note",t.note)
            put("adjustments",JSONArray().apply{t.adjustments.forEach{a->put(JSONObject().apply{
                put("id",a.id);put("kind",a.kind.name);put("amount",a.amount);put("note",a.note)
            })}})
        })}})
    }

    private fun projectFrom(o:JSONObject):Project{
        val sections=mutableListOf<Section>()
        val sa=o.optJSONArray("sections")?:JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val spaces=mutableListOf<Space>()
            val spa=so.optJSONArray("spaces")?:JSONArray()
            for(j in 0 until spa.length())spaces+=spaceFrom(spa.getJSONObject(j))
            sections+=Section(
                id=so.optString("id",UUID.randomUUID().toString()),
                name=so.optString("name","دور / جزء"),
                spaces=spaces
            )
        }

        val calcs=mutableListOf<SavedCalculation>()
        val ca=o.optJSONArray("calculations")?:JSONArray()
        for(i in 0 until ca.length()){
            val x=ca.getJSONObject(i)
            calcs+=SavedCalculation(
                id=x.optString("id",UUID.randomUUID().toString()),
                toolId=x.optString("toolId"),title=x.optString("title"),summary=x.optString("summary"),
                sourceQuantity=x.optDouble("sourceQuantity"),unit=x.optString("unit"),
                sectionId=if(x.isNull("sectionId"))null else x.optString("sectionId"),
                spaceId=if(x.isNull("spaceId"))null else x.optString("spaceId"),
                createdAt=x.optLong("createdAt",System.currentTimeMillis())
            )
        }
        return Project(
            id=o.optString("id",UUID.randomUUID().toString()),name=o.optString("name","مشروع"),
            type=o.optString("type","شقة"),sections=sections,calculations=calcs,
            createdAt=o.optLong("createdAt",System.currentTimeMillis()),updatedAt=o.optLong("updatedAt",System.currentTimeMillis())
        )
    }

    private fun surfacesFrom(arr:JSONArray?):List<SurfacePart>{
        if(arr==null)return emptyList()
        return buildList{
            for(i in 0 until arr.length()){
                val x=arr.getJSONObject(i)
                add(SurfacePart(
                    id=x.optString("id",UUID.randomUUID().toString()),
                    name=x.optString("name","مسطح"),
                    length=x.optDouble("length"),width=x.optDouble("width"),
                    deductionArea=x.optDouble("deductionArea"),note=x.optString("note")
                ))
            }
        }
    }

    private fun spaceFrom(o:JSONObject):Space{
        val walls=mutableListOf<WallPart>()
        val wa=o.optJSONArray("walls")?:JSONArray()
        for(i in 0 until wa.length()){
            val w=wa.getJSONObject(i)
            walls+=WallPart(
                id=w.optString("id",UUID.randomUUID().toString()),
                name=w.optString("name","حائط"),
                length=w.optDouble("length"),height=w.optDouble("height",3.0),note=w.optString("note")
            )
        }

        val openings=mutableListOf<Opening>()
        val oa=o.optJSONArray("openings")?:JSONArray()
        for(i in 0 until oa.length()){
            val x=oa.getJSONObject(i)
            openings+=Opening(
                id=x.optString("id",UUID.randomUUID().toString()),
                kind=runCatching{OpeningKind.valueOf(x.optString("kind","DOOR"))}.getOrDefault(OpeningKind.DOOR),
                width=x.optDouble("width"),height=x.optDouble("height"),sill=x.optDouble("sill"),
                count=x.optInt("count",1).coerceAtLeast(1),
                wallId=if(x.isNull("wallId"))null else x.optString("wallId"),
                revealDepth=x.optDouble("revealDepth"),note=x.optString("note")
            )
        }

        val takeoffs=mutableListOf<Takeoff>()
        val ta=o.optJSONArray("takeoffs")?:JSONArray()
        for(i in 0 until ta.length()){
            val t=ta.getJSONObject(i)
            val adjustments=mutableListOf<Adjustment>()
            val aa=t.optJSONArray("adjustments")?:JSONArray()
            for(j in 0 until aa.length()){
                val a=aa.getJSONObject(j)
                adjustments+=Adjustment(
                    id=a.optString("id",UUID.randomUUID().toString()),
                    kind=runCatching{AdjustKind.valueOf(a.optString("kind","ADD"))}.getOrDefault(AdjustKind.ADD),
                    amount=a.optDouble("amount"),note=a.optString("note")
                )
            }
            val wallIds=buildList{
                val ids=t.optJSONArray("wallIds")?:JSONArray()
                for(j in 0 until ids.length())add(ids.optString(j))
            }
            takeoffs+=Takeoff(
                id=t.optString("id",UUID.randomUUID().toString()),name=t.optString("name","بند"),
                unit=runCatching{UnitType.valueOf(t.optString("unit","AREA"))}.getOrDefault(UnitType.AREA),
                kind=runCatching{CalcKind.valueOf(t.optString("kind","DIRECT"))}.getOrDefault(CalcKind.DIRECT),
                waste=t.optDouble("waste"),tileHeight=t.optDouble("tileHeight",2.4),upstand=t.optDouble("upstand",.20),
                layerThickness=t.optDouble("layerThickness"),directValue=t.optDouble("directValue"),
                manualValue=if(t.isNull("manualValue"))null else t.optDouble("manualValue"),
                adjustments=adjustments,
                includeOpeningReveals=t.optBoolean("includeOpeningReveals",false),
                wallIds=wallIds,
                pieceWidth=t.optDouble("pieceWidth"),pieceHeight=t.optDouble("pieceHeight"),
                piecesPerPack=t.optInt("piecesPerPack",0),
                overrideReason=t.optString("overrideReason"),note=t.optString("note")
            )
        }

        val photos=buildList{
            val pa=o.optJSONArray("photoUris")?:JSONArray()
            for(i in 0 until pa.length())add(pa.optString(i))
        }

        return Space(
            id=o.optString("id",UUID.randomUUID().toString()),name=o.optString("name","مكان"),type=o.optString("type","غرفة"),
            length=o.optDouble("length"),width=o.optDouble("width"),height=o.optDouble("height",3.0),
            repeatCount=o.optInt("repeatCount",1).coerceAtLeast(1),walls=walls,openings=openings,takeoffs=takeoffs,
            note=o.optString("note"),status=runCatching{WorkStatus.valueOf(o.optString("status","IN_PROGRESS"))}.getOrDefault(WorkStatus.IN_PROGRESS),
            updatedAt=o.optLong("updatedAt",System.currentTimeMillis()),
            floorSurfaces=surfacesFrom(o.optJSONArray("floorSurfaces")),
            ceilingSurfaces=surfacesFrom(o.optJSONArray("ceilingSurfaces")),
            photoUris=photos
        )
    }
}

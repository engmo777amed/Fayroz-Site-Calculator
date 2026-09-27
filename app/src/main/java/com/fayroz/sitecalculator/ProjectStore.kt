package com.fayroz.sitecalculator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

data class Opening(
    val id:String = UUID.randomUUID().toString(),
    val type:String,
    val width:Double,
    val height:Double,
    val count:Int = 1
){
    val area:Double get() = width * height * count
    val totalWidth:Double get() = width * count
}

data class SavedSpace(
    val id:String = UUID.randomUUID().toString(),
    val name:String,
    val type:String,
    val length:Double,
    val width:Double,
    val height:Double,
    val doorArea:Double = 0.0,
    val windowArea:Double = 0.0,
    val doorWidth:Double = 0.0,
    val tileHeight:Double = 2.4,
    val waste:Double = 7.0,
    val works:List<String> = emptyList(),
    val openings:List<Opening> = emptyList(),
    val repeatCount:Int = 1,
    val note:String = ""
){
    val effectiveDoorArea:Double get() =
        if(openings.isNotEmpty()) openings.filter{it.type=="باب"}.sumOf{it.area} else doorArea
    val effectiveWindowArea:Double get() =
        if(openings.isNotEmpty()) openings.filter{it.type=="شباك"}.sumOf{it.area} else windowArea
    val effectiveDoorWidth:Double get() =
        if(openings.isNotEmpty()) openings.filter{it.type=="باب"}.sumOf{it.totalWidth} else doorWidth
}

data class ProjectSection(
    val id:String = UUID.randomUUID().toString(),
    var name:String,
    val spaces:MutableList<SavedSpace> = mutableListOf()
)

data class SiteProject(
    val id:String = UUID.randomUUID().toString(),
    var name:String,
    var type:String,
    val sections:MutableList<ProjectSection> = mutableListOf()
)

object QuantityEngine {
    val workOrder=listOf(
        "محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف",
        "الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد"
    )

    fun floor(space:SavedSpace):Double =
        space.length * space.width * space.repeatCount

    fun netWalls(space:SavedSpace):Double {
        val oneGross=2*(space.length+space.width)*space.height
        val oneOpenings=space.effectiveDoorArea+space.effectiveWindowArea
        return (oneGross-oneOpenings).coerceAtLeast(0.0)*space.repeatCount
    }

    fun wallTiles(space:SavedSpace):Double {
        val factor=1+space.waste/100.0
        val one=(
            2*(space.length+space.width)*min(space.tileHeight,space.height)
                -space.effectiveDoorArea-space.effectiveWindowArea
        ).coerceAtLeast(0.0)
        return one*space.repeatCount*factor
    }

    fun skirting(space:SavedSpace):Double {
        val factor=1+space.waste/100.0
        val one=(2*(space.length+space.width)-space.effectiveDoorWidth).coerceAtLeast(0.0)
        return one*space.repeatCount*factor
    }

    fun waterproof(space:SavedSpace):Double {
        val factor=1+space.waste/100.0
        val one=space.length*space.width + 2*(space.length+space.width)*0.20
        return one*space.repeatCount*factor
    }

    fun quantity(space:SavedSpace,work:String):Double=when(work){
        "محارة الحوائط" -> netWalls(space)
        "محارة السقف" -> floor(space)
        "دهان الحوائط" -> netWalls(space)
        "دهان السقف" -> floor(space)
        "الأرضيات" -> floor(space)*(1+space.waste/100.0)
        "سيراميك الحوائط" -> wallTiles(space)
        "الوزرات" -> skirting(space)
        "عزل الأرضية" -> waterproof(space)
        "سقف جبس بورد" -> floor(space)*(1+space.waste/100.0)
        else -> 0.0
    }

    fun unit(work:String)=if(work=="الوزرات") "م ط" else "م²"

    fun projectSummary(project:SiteProject):Map<String,Double> =
        workOrder.associateWith{work->
            project.sections.sumOf{section->
                section.spaces.filter{work in it.works}.sumOf{quantity(it,work)}
            }
        }.filterValues{it>0.0}

    fun sectionSummary(section:ProjectSection):Map<String,Double> =
        workOrder.associateWith{work->
            section.spaces.filter{work in it.works}.sumOf{quantity(it,work)}
        }.filterValues{it>0.0}
}

class ProjectStore(context:Context){
    private val prefs=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)
    private val uiPrefs=context.getSharedPreferences("fayroz_site_ui",Context.MODE_PRIVATE)

    fun load():MutableList<SiteProject>{
        val raw=prefs.getString("projects","[]") ?: "[]"
        return runCatching{
            val arr=JSONArray(raw)
            MutableList(arr.length()){i->projectFromJson(arr.getJSONObject(i))}
        }.getOrElse{mutableListOf()}
    }

    fun save(projects:List<SiteProject>){
        val arr=JSONArray()
        projects.forEach{arr.put(projectToJson(it))}
        prefs.edit().putString("projects",arr.toString()).apply()
    }

    fun lastHeight():Double=uiPrefs.getFloat("last_height",3f).toDouble()
    fun setLastHeight(v:Double){ if(v in 1.5..8.0) uiPrefs.edit().putFloat("last_height",v.toFloat()).apply() }

    fun saveDraft(key:String,json:String){uiPrefs.edit().putString("draft_$key",json).apply()}
    fun loadDraft(key:String):String?=uiPrefs.getString("draft_$key",null)
    fun clearDraft(key:String){uiPrefs.edit().remove("draft_$key").apply()}

    private fun projectToJson(p:SiteProject)=JSONObject().apply{
        put("id",p.id); put("name",p.name); put("type",p.type)
        val sectionsArray=JSONArray()
        p.sections.forEach{section->
            sectionsArray.put(JSONObject().apply{
                put("id",section.id); put("name",section.name)
                val spacesArray=JSONArray()
                section.spaces.forEach{space->
                    spacesArray.put(JSONObject().apply{
                        put("id",space.id); put("name",space.name); put("type",space.type)
                        put("length",space.length); put("width",space.width); put("height",space.height)
                        put("doorArea",space.effectiveDoorArea); put("windowArea",space.effectiveWindowArea)
                        put("doorWidth",space.effectiveDoorWidth); put("tileHeight",space.tileHeight); put("waste",space.waste)
                        put("repeatCount",space.repeatCount); put("note",space.note)
                        put("works",JSONArray(space.works))
                        val openingsArray=JSONArray()
                        space.openings.forEach{o->
                            openingsArray.put(JSONObject().apply{
                                put("id",o.id);put("type",o.type);put("width",o.width);put("height",o.height);put("count",o.count)
                            })
                        }
                        put("openings",openingsArray)
                    })
                }
                put("spaces",spacesArray)
            })
        }
        put("sections",sectionsArray)
    }

    private fun projectFromJson(o:JSONObject):SiteProject{
        val sections=mutableListOf<ProjectSection>()
        val sa=o.optJSONArray("sections") ?: JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val spaces=mutableListOf<SavedSpace>()
            val spa=so.optJSONArray("spaces") ?: JSONArray()
            for(j in 0 until spa.length()){
                val x=spa.getJSONObject(j)
                val works=mutableListOf<String>()
                val wa=x.optJSONArray("works") ?: JSONArray()
                for(k in 0 until wa.length()) works += wa.optString(k)
                val openings=mutableListOf<Opening>()
                val oa=x.optJSONArray("openings") ?: JSONArray()
                for(k in 0 until oa.length()){
                    val q=oa.getJSONObject(k)
                    openings += Opening(
                        id=q.optString("id",UUID.randomUUID().toString()),
                        type=q.optString("type","باب"),
                        width=q.optDouble("width",0.0),
                        height=q.optDouble("height",0.0),
                        count=q.optInt("count",1).coerceAtLeast(1)
                    )
                }
                spaces += SavedSpace(
                    id=x.optString("id",UUID.randomUUID().toString()),
                    name=x.optString("name","فراغ"),
                    type=x.optString("type","فراغ"),
                    length=x.optDouble("length",0.0),
                    width=x.optDouble("width",0.0),
                    height=x.optDouble("height",3.0),
                    doorArea=x.optDouble("doorArea",0.0),
                    windowArea=x.optDouble("windowArea",0.0),
                    doorWidth=x.optDouble("doorWidth",0.0),
                    tileHeight=x.optDouble("tileHeight",2.4),
                    waste=x.optDouble("waste",7.0),
                    works=works,
                    openings=openings,
                    repeatCount=x.optInt("repeatCount",1).coerceAtLeast(1),
                    note=x.optString("note","")
                )
            }
            sections += ProjectSection(
                id=so.optString("id",UUID.randomUUID().toString()),
                name=so.optString("name","جزء"),
                spaces=spaces
            )
        }
        return SiteProject(
            id=o.optString("id",UUID.randomUUID().toString()),
            name=o.optString("name","مشروع"),
            type=o.optString("type","شقة"),
            sections=sections
        )
    }
}

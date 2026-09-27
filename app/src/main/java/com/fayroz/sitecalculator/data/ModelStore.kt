package com.fayroz.sitecalculator.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

data class Opening(
    val id:String=UUID.randomUUID().toString(),
    val type:String="باب",
    val width:Double=0.0,
    val height:Double=0.0,
    val count:Int=1
){
    val area:Double get()=width*height*count
    val totalWidth:Double get()=width*count
}

data class Room(
    val id:String=UUID.randomUUID().toString(),
    var name:String="غرفة",
    var type:String="غرفة نوم",
    var length:Double=0.0,
    var width:Double=0.0,
    var height:Double=3.0,
    var tileHeight:Double=2.4,
    var waste:Double=7.0,
    var repeat:Int=1,
    var note:String="",
    val works:MutableList<String> = mutableListOf(),
    val openings:MutableList<Opening> = mutableListOf()
)

data class Section(
    val id:String=UUID.randomUUID().toString(),
    var name:String="الدور الأرضي",
    val rooms:MutableList<Room> = mutableListOf()
)

data class Project(
    val id:String=UUID.randomUUID().toString(),
    var name:String="مشروع",
    var type:String="شقة",
    val sections:MutableList<Section> = mutableListOf()
)

object QtyEngine {
    val works=listOf(
        "محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف",
        "الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد"
    )
    fun floor(r:Room)=r.length*r.width*r.repeat
    fun openingArea(r:Room)=r.openings.sumOf{it.area}
    fun doorWidth(r:Room)=r.openings.filter{it.type=="باب"}.sumOf{it.totalWidth}
    fun walls(r:Room)=(2*(r.length+r.width)*r.height-openingArea(r)).coerceAtLeast(0.0)*r.repeat
    fun tileWalls(r:Room):Double{
        val one=(2*(r.length+r.width)*min(r.tileHeight,r.height)-openingArea(r)).coerceAtLeast(0.0)
        return one*r.repeat*(1+r.waste/100.0)
    }
    fun skirting(r:Room)=((2*(r.length+r.width)-doorWidth(r)).coerceAtLeast(0.0))*r.repeat*(1+r.waste/100.0)
    fun waterproof(r:Room)=((r.length*r.width)+2*(r.length+r.width)*0.20)*r.repeat*(1+r.waste/100.0)
    fun quantity(r:Room,work:String)=when(work){
        "محارة الحوائط","دهان الحوائط"->walls(r)
        "محارة السقف","دهان السقف"->floor(r)
        "الأرضيات"->floor(r)*(1+r.waste/100.0)
        "سيراميك الحوائط"->tileWalls(r)
        "الوزرات"->skirting(r)
        "عزل الأرضية"->waterproof(r)
        "سقف جبس بورد"->floor(r)*(1+r.waste/100.0)
        else->0.0
    }
    fun unit(work:String)=if(work=="الوزرات")"م ط" else "م²"
    fun projectSummary(p:Project):Map<String,Double> = works.mapNotNull{w->
        val q=p.sections.sumOf{s->s.rooms.filter{w in it.works}.sumOf{quantity(it,w)}}
        if(q>0) w to q else null
    }.toMap()
    fun sectionSummary(s:Section):Map<String,Double> = works.mapNotNull{w->
        val q=s.rooms.filter{w in it.works}.sumOf{quantity(it,w)}
        if(q>0) w to q else null
    }.toMap()
}

class ProjectStore(context:Context){
    private val prefs=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)
    private val ui=context.getSharedPreferences("fayroz_site_ui",Context.MODE_PRIVATE)

    fun load():MutableList<Project>{
        val raw=prefs.getString("projects","[]")?:"[]"
        return runCatching{
            val a=JSONArray(raw)
            MutableList(a.length()){i->projectFromJson(a.getJSONObject(i))}
        }.getOrElse{mutableListOf()}
    }

    fun save(projects:List<Project>){
        val a=JSONArray()
        projects.forEach{a.put(projectToJson(it))}
        prefs.edit().putString("projects",a.toString()).apply()
    }

    fun lastHeight():Double=ui.getFloat("last_height",3f).toDouble()
    fun setLastHeight(v:Double){
        if(v in 1.5..8.0)ui.edit().putFloat("last_height",v.toFloat()).apply()
    }

    private fun projectToJson(p:Project)=JSONObject().apply{
        put("id",p.id);put("name",p.name);put("type",p.type)
        put("sections",JSONArray().apply{
            p.sections.forEach{s->
                put(JSONObject().apply{
                    put("id",s.id);put("name",s.name)
                    put("spaces",JSONArray().apply{
                        s.rooms.forEach{r->
                            put(JSONObject().apply{
                                put("id",r.id);put("name",r.name);put("type",r.type)
                                put("length",r.length);put("width",r.width);put("height",r.height)
                                put("tileHeight",r.tileHeight);put("waste",r.waste);put("repeatCount",r.repeat);put("note",r.note)
                                put("works",JSONArray(r.works))
                                put("openings",JSONArray().apply{
                                    r.openings.forEach{o->
                                        put(JSONObject().apply{
                                            put("id",o.id);put("type",o.type);put("width",o.width);put("height",o.height);put("count",o.count)
                                        })
                                    }
                                })
                                put("doorArea",r.openings.filter{it.type=="باب"}.sumOf{it.area})
                                put("windowArea",r.openings.filter{it.type=="شباك"}.sumOf{it.area})
                                put("doorWidth",r.openings.filter{it.type=="باب"}.sumOf{it.totalWidth})
                            })
                        }
                    })
                })
            }
        })
    }

    private fun projectFromJson(o:JSONObject):Project{
        val sections=mutableListOf<Section>()
        val sa=o.optJSONArray("sections")?:JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val rooms=mutableListOf<Room>()
            val ra=so.optJSONArray("spaces")?:JSONArray()
            for(j in 0 until ra.length()){
                val ro=ra.getJSONObject(j)
                val works=mutableListOf<String>()
                val wa=ro.optJSONArray("works")?:JSONArray()
                for(k in 0 until wa.length()) works+=wa.optString(k)

                val opens=mutableListOf<Opening>()
                val oa=ro.optJSONArray("openings")?:JSONArray()
                for(k in 0 until oa.length()){
                    val q=oa.getJSONObject(k)
                    opens+=Opening(
                        q.optString("id",UUID.randomUUID().toString()),
                        q.optString("type","باب"),
                        q.optDouble("width",0.0),
                        q.optDouble("height",0.0),
                        q.optInt("count",1).coerceAtLeast(1)
                    )
                }

                if(opens.isEmpty()){
                    val doorArea=ro.optDouble("doorArea",0.0)
                    val doorWidth=ro.optDouble("doorWidth",0.0)
                    if(doorArea>0 && doorWidth>0) opens+=Opening(type="باب",width=doorWidth,height=doorArea/doorWidth,count=1)
                    val win=ro.optDouble("windowArea",0.0)
                    if(win>0) opens+=Opening(type="شباك",width=win,height=1.0,count=1)
                }

                rooms+=Room(
                    id=ro.optString("id",UUID.randomUUID().toString()),
                    name=ro.optString("name","غرفة"),
                    type=ro.optString("type","غرفة نوم"),
                    length=ro.optDouble("length",0.0),
                    width=ro.optDouble("width",0.0),
                    height=ro.optDouble("height",3.0),
                    tileHeight=ro.optDouble("tileHeight",2.4),
                    waste=ro.optDouble("waste",7.0),
                    repeat=ro.optInt("repeatCount",1).coerceAtLeast(1),
                    note=ro.optString("note",""),
                    works=works,
                    openings=opens
                )
            }
            sections+=Section(
                so.optString("id",UUID.randomUUID().toString()),
                so.optString("name","الدور الأرضي"),
                rooms
            )
        }
        return Project(
            o.optString("id",UUID.randomUUID().toString()),
            o.optString("name","مشروع"),
            o.optString("type","شقة"),
            sections
        )
    }
}

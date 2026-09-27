package com.fayroz.sitecalculator.data

import android.content.Context
import com.fayroz.sitecalculator.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(context:Context){
    // Same storage key as older versions so existing projects remain readable.
    private val prefs=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)
    private val ui=context.getSharedPreferences("fayroz_site_ui",Context.MODE_PRIVATE)

    fun load():MutableList<SiteProject>{
        val raw=prefs.getString("projects","[]") ?: "[]"
        return runCatching{
            val arr=JSONArray(raw)
            MutableList(arr.length()){i->projectFromJson(arr.getJSONObject(i))}
        }.getOrElse{ mutableListOf() }
    }

    fun save(projects:List<SiteProject>){
        val arr=JSONArray()
        projects.forEach{arr.put(projectToJson(it))}
        prefs.edit().putString("projects",arr.toString()).apply()
    }

    fun lastHeight()=ui.getFloat("last_height",3f).toDouble()
    fun setLastHeight(v:Double){
        if(v in 1.5..8.0) ui.edit().putFloat("last_height",v.toFloat()).apply()
    }

    fun saveDraft(key:String,value:String)=ui.edit().putString("draft_$key",value).apply()
    fun loadDraft(key:String)=ui.getString("draft_$key",null)
    fun clearDraft(key:String)=ui.edit().remove("draft_$key").apply()

    private fun projectToJson(p:SiteProject)=JSONObject().apply{
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
                                put("tileHeight",r.tileHeight);put("waste",r.waste)
                                put("repeatCount",r.repeatCount);put("note",r.note)
                                put("doorArea",r.openings.filter{it.type=="باب"}.sumOf{it.area})
                                put("windowArea",r.openings.filter{it.type=="شباك"}.sumOf{it.area})
                                put("doorWidth",r.openings.filter{it.type=="باب"}.sumOf{it.totalWidth})
                                put("works",JSONArray(r.works))
                                put("openings",JSONArray().apply{
                                    r.openings.forEach{o->
                                        put(JSONObject().apply{
                                            put("id",o.id);put("type",o.type)
                                            put("width",o.width);put("height",o.height);put("count",o.count)
                                        })
                                    }
                                })
                            })
                        }
                    })
                })
            }
        })
    }

    private fun projectFromJson(o:JSONObject):SiteProject{
        val sections=mutableListOf<SectionEntry>()
        val sa=o.optJSONArray("sections") ?: JSONArray()
        for(i in 0 until sa.length()){
            val so=sa.getJSONObject(i)
            val rooms=mutableListOf<RoomEntry>()
            val ra=so.optJSONArray("spaces") ?: JSONArray()
            for(j in 0 until ra.length()){
                val r=ra.getJSONObject(j)
                val works=mutableListOf<String>()
                val wa=r.optJSONArray("works") ?: JSONArray()
                for(k in 0 until wa.length()) works+=wa.optString(k)

                val openings=mutableListOf<Opening>()
                val oa=r.optJSONArray("openings")
                if(oa!=null && oa.length()>0){
                    for(k in 0 until oa.length()){
                        val q=oa.getJSONObject(k)
                        openings+=Opening(
                            id=q.optString("id",UUID.randomUUID().toString()),
                            type=q.optString("type","باب"),
                            width=q.optDouble("width",0.0),
                            height=q.optDouble("height",0.0),
                            count=q.optInt("count",1).coerceAtLeast(1)
                        )
                    }
                }else{
                    // Migration for older saved projects that only stored total opening areas.
                    val doorArea=r.optDouble("doorArea",0.0)
                    val windowArea=r.optDouble("windowArea",0.0)
                    val doorWidth=r.optDouble("doorWidth",0.0)
                    if(doorArea>0){
                        val w=if(doorWidth>0)doorWidth else 1.0
                        openings+=Opening(type="باب",width=w,height=doorArea/w,count=1)
                    }
                    if(windowArea>0) openings+=Opening(type="شباك",width=1.0,height=windowArea,count=1)
                }

                rooms+=RoomEntry(
                    id=r.optString("id",UUID.randomUUID().toString()),
                    name=r.optString("name","فراغ"),
                    type=r.optString("type","فراغ"),
                    length=r.optDouble("length",0.0),
                    width=r.optDouble("width",0.0),
                    height=r.optDouble("height",3.0),
                    tileHeight=r.optDouble("tileHeight",2.4),
                    waste=r.optDouble("waste",7.0),
                    repeatCount=r.optInt("repeatCount",1).coerceAtLeast(1),
                    note=r.optString("note",""),
                    openings=openings,
                    works=works
                )
            }
            sections+=SectionEntry(
                id=so.optString("id",UUID.randomUUID().toString()),
                name=so.optString("name","جزء"),
                rooms=rooms
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

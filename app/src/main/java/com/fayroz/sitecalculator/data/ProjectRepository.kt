package com.fayroz.sitecalculator.data

import android.content.Context
import com.fayroz.sitecalculator.core.*
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(context:Context){
    private val projectPrefs=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)
    private val appPrefs=context.getSharedPreferences("fayroz_site_v7",Context.MODE_PRIVATE)

    fun loadProjects():MutableList<SiteProject> =
        JsonCodec.projectsFromJson(projectPrefs.getString("projects","[]") ?: "[]")

    fun saveProjects(projects:List<SiteProject>,reason:String="تعديل"){
        val previous=projectPrefs.getString("projects","[]") ?: "[]"
        pushHistory(previous,reason)
        projectPrefs.edit().putString("projects",JsonCodec.projectsToJson(projects)).apply()
    }

    fun canUndo():Boolean=(appPrefs.getString("history","[]")?.let{JSONArray(it).length()}?:0)>0

    fun undo():MutableList<SiteProject>?{
        val arr=JSONArray(appPrefs.getString("history","[]") ?: "[]")
        if(arr.length()==0)return null
        val last=arr.getJSONObject(arr.length()-1)
        val raw=last.optString("snapshot","[]")
        val next=JSONArray()
        for(i in 0 until arr.length()-1)next.put(arr.getJSONObject(i))
        appPrefs.edit().putString("history",next.toString()).apply()
        projectPrefs.edit().putString("projects",raw).apply()
        return JsonCodec.projectsFromJson(raw)
    }

    fun historyLabels():List<String>{
        val arr=JSONArray(appPrefs.getString("history","[]") ?: "[]")
        return buildList{
            for(i in arr.length()-1 downTo 0){
                val x=arr.getJSONObject(i)
                add(x.optString("reason","تعديل"))
            }
        }
    }

    private fun pushHistory(snapshot:String,reason:String){
        val old=JSONArray(appPrefs.getString("history","[]") ?: "[]")
        val next=JSONArray()
        val start=(old.length()-19).coerceAtLeast(0)
        for(i in start until old.length())next.put(old.getJSONObject(i))
        next.put(JSONObject().apply{
            put("reason",reason);put("time",System.currentTimeMillis());put("snapshot",snapshot)
        })
        appPrefs.edit().putString("history",next.toString()).apply()
    }

    fun saveDraft(key:String,space:SpaceEntry)=
        appPrefs.edit().putString("draft_$key",JsonCodec.spaceToJson(space)).apply()
    fun loadDraft(key:String)=appPrefs.getString("draft_$key",null)?.let(JsonCodec::spaceFromJson)
    fun clearDraft(key:String)=appPrefs.edit().remove("draft_$key").apply()

    fun getDefaults():AppDefaults=AppDefaults(
        defaultHeight=appPrefs.getString("defaultHeight","3.0")?.toDoubleOrNull()?:3.0,
        defaultTileHeight=appPrefs.getString("defaultTileHeight","2.4")?.toDoubleOrNull()?:2.4,
        waterproofUpstand=appPrefs.getString("waterproofUpstand","0.20")?.toDoubleOrNull()?:0.20,
        floorWaste=appPrefs.getString("floorWaste","7")?.toDoubleOrNull()?:7.0,
        wallTileWaste=appPrefs.getString("wallTileWaste","5")?.toDoubleOrNull()?:5.0,
        gypsumWaste=appPrefs.getString("gypsumWaste","10")?.toDoubleOrNull()?:10.0,
        skirtingWaste=appPrefs.getString("skirtingWaste","5")?.toDoubleOrNull()?:5.0
    )

    fun saveDefaults(d:AppDefaults){
        appPrefs.edit()
            .putString("defaultHeight",d.defaultHeight.toString())
            .putString("defaultTileHeight",d.defaultTileHeight.toString())
            .putString("waterproofUpstand",d.waterproofUpstand.toString())
            .putString("floorWaste",d.floorWaste.toString())
            .putString("wallTileWaste",d.wallTileWaste.toString())
            .putString("gypsumWaste",d.gypsumWaste.toString())
            .putString("skirtingWaste",d.skirtingWaste.toString())
            .apply()
    }

    fun getFavorites():Set<String> =
        appPrefs.getStringSet("favorites",setOf("محارة الحوائط","دهان الحوائط","الأرضيات","سيراميك الحوائط")) ?: emptySet()
    fun saveFavorites(items:Set<String>)=appPrefs.edit().putStringSet("favorites",items).apply()

    fun getToolFavorites():Set<String> =
        appPrefs.getStringSet("tool_favorites",setOf("plaster_materials","splash_materials","tile_purchase","slope_levels")) ?: emptySet()

    fun toggleToolFavorite(id:String){
        val next=getToolFavorites().toMutableSet()
        if(id in next) next.remove(id) else next.add(id)
        appPrefs.edit().putStringSet("tool_favorites",next).apply()
    }

    fun getRecentTools():List<String>{
        val raw=appPrefs.getString("recent_tools","") ?: ""
        return raw.split("|").filter{it.isNotBlank()}.take(6)
    }

    fun recordToolResult(id:String,title:String,summary:String){
        if(summary.isBlank())return
        val today=java.time.LocalDate.now().toString()
        val storedDay=appPrefs.getString("tool_results_day","")
        val old=if(storedDay==today)JSONArray(appPrefs.getString("tool_results","[]") ?: "[]") else JSONArray()
        val next=JSONArray()
        next.put(JSONObject().apply{
            put("id",id);put("title",title);put("summary",summary);put("time",System.currentTimeMillis())
        })
        var kept=0
        for(i in 0 until old.length()){
            val x=old.optJSONObject(i) ?: continue
            if(x.optString("id")==id)continue
            if(kept>=4)break
            next.put(x);kept++
        }
        appPrefs.edit().putString("tool_results_day",today).putString("tool_results",next.toString()).apply()
    }

    fun getToolResults():List<Triple<String,String,String>>{
        val today=java.time.LocalDate.now().toString()
        if(appPrefs.getString("tool_results_day","")!=today)return emptyList()
        val arr=JSONArray(appPrefs.getString("tool_results","[]") ?: "[]")
        return buildList{
            for(i in 0 until arr.length()){
                val x=arr.optJSONObject(i) ?: continue
                val id=x.optString("id")
                val title=x.optString("title")
                val summary=x.optString("summary")
                if(id.isNotBlank()&&summary.isNotBlank())add(Triple(id,title,summary))
            }
        }.take(5)
    }


    fun recordToolUse(id:String){
        val next=(listOf(id)+getRecentTools().filter{it!=id}).take(6)
        appPrefs.edit().putString("recent_tools",next.joinToString("|")).apply()
    }

    fun getToolValue(key:String,default:String):String =
        appPrefs.getString("tool_value_$key",default) ?: default

    fun setToolValue(key:String,value:String){
        appPrefs.edit().putString("tool_value_$key",value).apply()
    }

    fun exportBackup():String=JSONObject().apply{
        put("version",7);put("projects",JSONArray(projectPrefs.getString("projects","[]")?:"[]"))
        put("defaults",JSONObject().apply{
            val d=getDefaults()
            put("defaultHeight",d.defaultHeight);put("defaultTileHeight",d.defaultTileHeight)
            put("waterproofUpstand",d.waterproofUpstand);put("floorWaste",d.floorWaste)
            put("wallTileWaste",d.wallTileWaste);put("gypsumWaste",d.gypsumWaste);put("skirtingWaste",d.skirtingWaste)
        })
    }.toString()

    fun importBackup(raw:String):Boolean=runCatching{
        val root=JSONObject(raw)
        val arr=root.getJSONArray("projects")
        projectPrefs.edit().putString("projects",arr.toString()).apply()
        true
    }.getOrDefault(false)
}

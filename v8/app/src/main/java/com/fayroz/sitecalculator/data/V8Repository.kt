package com.fayroz.sitecalculator.data

import android.content.Context
import com.fayroz.sitecalculator.core.*
import org.json.JSONObject

class V8Repository(context:Context){
    private val prefs=context.getSharedPreferences("fayroz_site_v8",Context.MODE_PRIVATE)
    private val legacy=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)

    init { migrateOnce() }

    private fun migrateOnce(){
        if(prefs.getBoolean("migration_done",false))return
        if(prefs.getString("projects",null).isNullOrBlank()){
            val old=legacy.getString("projects","[]")?:"[]"
            val migrated=LegacyMigration.fromV7(old)
            if(migrated.isNotEmpty()){
                prefs.edit().putString("projects",V8Codec.encodeProjects(migrated)).apply()
            }
        }
        prefs.edit().putBoolean("migration_done",true).apply()
    }

    fun loadProjects():MutableList<Project> =
        V8Codec.decodeProjects(prefs.getString("projects","[]")?:"[]")

    fun saveProjects(projects:List<Project>) =
        prefs.edit().putString("projects",V8Codec.encodeProjects(projects)).apply()

    fun activeLocation():ActiveLocation?{
        val raw=prefs.getString("active_location",null)?:return null
        return runCatching{
            val o=JSONObject(raw)
            ActiveLocation(
                projectId=o.getString("projectId"),
                sectionId=o.optString("sectionId").takeIf{it.isNotBlank()},
                spaceId=o.optString("spaceId").takeIf{it.isNotBlank()}
            )
        }.getOrNull()
    }

    fun setActive(location:ActiveLocation){
        prefs.edit().putString("active_location",JSONObject().apply{
            put("projectId",location.projectId)
            put("sectionId",location.sectionId?:"")
            put("spaceId",location.spaceId?:"")
        }.toString()).apply()
    }

    fun clearActive()=prefs.edit().remove("active_location").apply()

    fun saveDraft(key:String,space:Space)=
        prefs.edit().putString("draft_$key",V8Codec.encodeSpace(space)).apply()

    fun loadDraft(key:String)=
        prefs.getString("draft_$key",null)?.let(V8Codec::decodeSpace)

    fun clearDraft(key:String)=prefs.edit().remove("draft_$key").apply()

    fun saveRecentCalc(calc:SavedCalculation){
        val next=(listOf(calc)+recentCalcs().filter{it.toolId!=calc.toolId}).take(5)
        val holder=Project(name="_recent",calculations=next)
        prefs.edit().putString("recent_calcs",V8Codec.encodeProjects(listOf(holder))).apply()
    }

    fun recentCalcs():List<SavedCalculation>{
        val raw=prefs.getString("recent_calcs",null)?:return emptyList()
        return V8Codec.decodeProjects(raw).firstOrNull()?.calculations?:emptyList()
    }

    fun appearance():String=prefs.getString("appearance","system")?:"system"
    fun setAppearance(value:String)=prefs.edit().putString("appearance",value).apply()

    fun pref(key:String,default:String):String=prefs.getString("pref_$key",default)?:default
    fun setPref(key:String,value:String)=prefs.edit().putString("pref_$key",value).apply()
}

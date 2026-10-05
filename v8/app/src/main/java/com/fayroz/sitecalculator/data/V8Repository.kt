package com.fayroz.sitecalculator.data

import android.content.Context
import com.fayroz.sitecalculator.core.*
import org.json.JSONObject

class V8Repository(context:Context){
    private val prefs=context.getSharedPreferences("fayroz_site_v8",Context.MODE_PRIVATE)
    private val legacy=context.getSharedPreferences("fayroz_site_projects",Context.MODE_PRIVATE)

    init { migrateOnce(); migrateV81Once() }

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

    private fun migrateV81Once(){
        if(prefs.getBoolean("migration_v81_done",false))return
        val legacyProjects=LegacyMigration.fromV7(legacy.getString("projects","[]")?:"[]")
        if(legacyProjects.isNotEmpty()){
            val current=V8Codec.decodeProjects(prefs.getString("projects","[]")?:"[]")
            val merged=current.toMutableList()
            legacyProjects.forEach{old->
                val index=merged.indexOfFirst{it.id==old.id}
                if(index<0)merged+=old
                else merged[index]=mergeProject(merged[index],old)
            }
            prefs.edit().putString("projects",V8Codec.encodeProjects(merged)).apply()
        }
        prefs.edit().putBoolean("migration_v81_done",true).apply()
    }

    private fun mergeProject(current:Project,legacyProject:Project):Project{
        val sections=current.sections.toMutableList()
        legacyProject.sections.forEach{oldSection->
            val si=sections.indexOfFirst{it.id==oldSection.id}
            if(si<0)sections+=oldSection
            else{
                val cur=sections[si]
                val spaces=cur.spaces.toMutableList()
                oldSection.spaces.forEach{oldSpace->
                    val spi=spaces.indexOfFirst{it.id==oldSpace.id}
                    if(spi<0)spaces+=oldSpace
                    else spaces[spi]=mergeSpace(spaces[spi],oldSpace)
                }
                sections[si]=cur.copy(spaces=spaces)
            }
        }
        return current.copy(sections=sections,updatedAt=maxOf(current.updatedAt,legacyProject.updatedAt))
    }

    private fun mergeSpace(current:Space,old:Space):Space{
        val oldOpenings=old.openings.associateBy{it.id}
        val openings=current.openings.map{o->
            val x=oldOpenings[o.id]
            if(x==null)o else o.copy(
                wallId=o.wallId ?: x.wallId,
                revealDepth=if(o.revealDepth>0)o.revealDepth else x.revealDepth,
                note=if(o.note.isNotBlank())o.note else x.note
            )
        }
        val oldTakeoffs=old.takeoffs.associateBy{it.id}
        val takeoffs=current.takeoffs.map{t->
            val x=oldTakeoffs[t.id]
            if(x==null)t else t.copy(
                adjustments=if(t.adjustments.isNotEmpty())t.adjustments else x.adjustments,
                includeOpeningReveals=t.includeOpeningReveals||x.includeOpeningReveals,
                wallIds=if(t.wallIds.isNotEmpty())t.wallIds else x.wallIds,
                pieceWidth=if(t.pieceWidth>0)t.pieceWidth else x.pieceWidth,
                pieceHeight=if(t.pieceHeight>0)t.pieceHeight else x.pieceHeight,
                piecesPerPack=if(t.piecesPerPack>0)t.piecesPerPack else x.piecesPerPack,
                overrideReason=if(t.overrideReason.isNotBlank())t.overrideReason else x.overrideReason,
                note=if(t.note.isNotBlank())t.note else x.note
            )
        }
        return current.copy(
            walls=if(current.walls.isNotEmpty())current.walls else old.walls,
            openings=if(current.openings.isNotEmpty())openings else old.openings,
            takeoffs=if(current.takeoffs.isNotEmpty())takeoffs else old.takeoffs,
            floorSurfaces=if(current.floorSurfaces.isNotEmpty())current.floorSurfaces else old.floorSurfaces,
            ceilingSurfaces=if(current.ceilingSurfaces.isNotEmpty())current.ceilingSurfaces else old.ceilingSurfaces,
            photoUris=(current.photoUris+old.photoUris).distinct()
        )
    }

    fun loadProjects():MutableList<Project> {
        val raw=prefs.getString("projects","[]")?:"[]"
        val parsed=V8Codec.decodeProjects(raw)
        if(parsed.isEmpty()&&raw!="[]"){
            prefs.edit().putString("corrupt_projects",raw).commit()
            return V8Codec.decodeProjects(prefs.getString("last_good_projects","[]")?:"[]")
        }
        return parsed
    }

    fun saveProjects(projects:List<Project>) {
        val raw=V8Codec.encodeProjects(projects)
        check(prefs.edit().putString("last_good_projects",prefs.getString("projects","[]"))
            .putString("projects",raw).commit()){ "تعذر حفظ المشروعات" }
    }

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
        val next=(listOf(calc)+recentCalcs()).take(100)
        val holder=Project(name="_recent",calculations=next)
        prefs.edit().putString("recent_calcs",V8Codec.encodeProjects(listOf(holder))).apply()
    }

    fun recentCalcs():List<SavedCalculation>{
        val raw=prefs.getString("recent_calcs",null)?:return emptyList()
        return V8Codec.decodeProjects(raw).firstOrNull()?.calculations?:emptyList()
    }

    fun exportBackup():String=JSONObject().apply{
        put("version",9)
        put("projects",org.json.JSONArray(prefs.getString("projects","[]")?:"[]"))
        put("settings",JSONObject().apply{prefs.all.forEach{(key,value)->if(key!="projects"&&value is String)put(key,value)}})
    }.toString()

    fun importBackup(raw:String,merge:Boolean=false):Boolean=runCatching{
        val root=JSONObject(raw)
        require(root.optInt("version",0) in 8..9)
        val arr=root.getJSONArray("projects")
        val parsed=V8Codec.decodeProjects(arr.toString())
        require(parsed.size==arr.length())
        require(parsed.map{it.id}.distinct().size==parsed.size)
        require(parsed.all{it.id.isNotBlank()&&it.name.isNotBlank()})
        val before=exportBackup()
        val next=if(merge){
            val current=loadProjects().toMutableList()
            parsed.forEach{incoming->
                val index=current.indexOfFirst{it.id==incoming.id}
                if(index<0)current+=incoming
                else{
                    val local=current[index]
                    val sections=local.sections.toMutableList()
                    incoming.sections.forEach{s->
                        val si=sections.indexOfFirst{it.id==s.id}
                        if(si<0)sections+=s
                        else sections[si]=sections[si].copy(spaces=sections[si].spaces+s.spaces.filter{remote->sections[si].spaces.none{it.id==remote.id}})
                    }
                    current[index]=local.copy(sections=sections,calculations=local.calculations+incoming.calculations.filter{remote->local.calculations.none{it.id==remote.id}})
                }
            }
            current
        }else parsed
        val editor=prefs.edit().putString("before_import",before).putString("last_good_projects",prefs.getString("projects","[]"))
            .putString("projects",V8Codec.encodeProjects(next))
        val settings=root.optJSONObject("settings")
        settings?.keys()?.forEach{key->if(key!="projects"&&!key.startsWith("draft_")&&key !in setOf("before_import","corrupt_projects","last_good_projects")){
            if(!merge||!prefs.contains(key))editor.putString(key,settings.getString(key))
        }}
        if(!merge)editor.remove("active_location")
        editor.commit()
    }.getOrDefault(false)

    fun appearance():String=prefs.getString("appearance","system")?:"system"
    fun setAppearance(value:String)=prefs.edit().putString("appearance",value).apply()

    fun pref(key:String,default:String):String=prefs.getString("pref_$key",default)?:default
    fun setPref(key:String,value:String)=prefs.edit().putString("pref_$key",value).apply()
}

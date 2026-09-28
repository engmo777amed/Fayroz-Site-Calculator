package com.fayroz.sitecalculator

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class RootTab{ HOME,PROJECTS,TODAY,TOOLS }

private sealed interface Route{
    data object Root:Route
    data object Settings:Route
    data class ProjectDetail(val projectId:String):Route
    data class SectionDetail(val projectId:String,val sectionId:String):Route
    data class RoomEdit(val projectId:String,val sectionId:String,val spaceId:String?):Route
    data class Tool(val toolId:String,val seed:MaterialResult?=null,val projectId:String?=null):Route
}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            val repository=remember{V8Repository(this)}
            val projects=remember{
                mutableStateListOf<Project>().apply{addAll(repository.loadProjects())}
            }
            var appearance by remember{mutableStateOf(repository.appearance())}
            var active by remember{mutableStateOf(repository.activeLocation())}
            var tab by remember{mutableStateOf(RootTab.HOME)}
            var route by remember{mutableStateOf<Route>(Route.Root)}

            fun persist(){
                repository.saveProjects(projects.toList())
            }

            fun replaceProject(updated:Project){
                val i=projects.indexOfFirst{it.id==updated.id}
                if(i>=0)projects[i]=updated
                else projects.add(updated)
                persist()
            }

            fun activeProject():Project? =
                active?.let{a->projects.firstOrNull{it.id==a.projectId}}

            fun setActive(location:ActiveLocation){
                active=location
                repository.setActive(location)
            }

            fun routeToActive(){
                val a=active ?: run{tab=RootTab.PROJECTS;route=Route.Root;return}
                val p=projects.firstOrNull{it.id==a.projectId} ?: run{
                    repository.clearActive();active=null;tab=RootTab.PROJECTS;route=Route.Root;return
                }
                if(a.sectionId!=null&&a.spaceId!=null&&p.sections.any{s->s.id==a.sectionId&&s.spaces.any{it.id==a.spaceId}}){
                    route=Route.RoomEdit(a.projectId,a.sectionId,a.spaceId)
                }else if(a.sectionId!=null&&p.sections.any{it.id==a.sectionId}){
                    route=Route.SectionDetail(a.projectId,a.sectionId)
                }else route=Route.ProjectDetail(a.projectId)
            }

            fun openNewRoomFromActive(){
                val p=activeProject()
                if(p==null){tab=RootTab.PROJECTS;route=Route.Root;return}
                val sectionId=active?.sectionId?.takeIf{id->p.sections.any{it.id==id}}
                    ?:p.sections.firstOrNull()?.id
                if(sectionId==null)route=Route.ProjectDetail(p.id)
                else route=Route.RoomEdit(p.id,sectionId,null)
            }

            fun saveCalculation(
                targetProjectId:String,
                result:MaterialResult,
                sectionId:String?,
                spaceId:String?
            ){
                val summary=result.lines.joinToString(" • "){"${it.label}: ${it.value}"}
                val calc=SavedCalculation(
                    toolId=result.toolId,
                    title=result.title,
                    summary=summary,
                    sourceQuantity=result.sourceQuantity,
                    unit=result.sourceUnit,
                    sectionId=sectionId,
                    spaceId=spaceId
                )
                repository.saveRecentCalc(calc)
                val p=projects.firstOrNull{it.id==targetProjectId}?:return
                replaceProject(p.copy(
                    calculations=(listOf(calc)+p.calculations).take(50),
                    updatedAt=System.currentTimeMillis()
                ))
            }

            fun shareProject(p:Project){
                val summary=QuantityEngine.summarize(p)
                val text=buildString{
                    appendLine("FAYROZ SITE CALCULATOR")
                    appendLine(p.name)
                    appendLine()
                    summary.forEach{appendLine("${it.name}: ${fmt(it.quantity)} ${it.unit.label}")}
                }
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{
                    type="text/plain"
                    putExtra(Intent.EXTRA_TEXT,text)
                },"مشاركة ملخص المشروع"))
            }

            FayrozTheme(appearance){
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
                BackHandler(enabled=route !is Route.Root){
                    route=when(val r=route){
                        is Route.Settings->Route.Root
                        is Route.ProjectDetail->Route.Root
                        is Route.SectionDetail->Route.ProjectDetail(r.projectId)
                        is Route.RoomEdit->Route.SectionDetail(r.projectId,r.sectionId)
                        is Route.Tool->if(r.projectId!=null)Route.ProjectDetail(r.projectId) else Route.Root
                        Route.Root->Route.Root
                    }
                }

                when(val r=route){
                    Route.Root->{
                        Scaffold(
                            bottomBar={
                                NavigationBar{
                                    NavigationBarItem(
                                        selected=tab==RootTab.HOME,onClick={tab=RootTab.HOME},
                                        icon={Icon(Icons.Rounded.Home,null)},label={Text("الرئيسية")}
                                    )
                                    NavigationBarItem(
                                        selected=tab==RootTab.PROJECTS,onClick={tab=RootTab.PROJECTS},
                                        icon={Icon(Icons.Rounded.FolderOpen,null)},label={Text("المشروعات")}
                                    )
                                    NavigationBarItem(
                                        selected=tab==RootTab.TODAY,onClick={tab=RootTab.TODAY},
                                        icon={Icon(Icons.Rounded.Today,null)},label={Text("شغل اليوم")}
                                    )
                                    NavigationBarItem(
                                        selected=tab==RootTab.TOOLS,onClick={tab=RootTab.TOOLS},
                                        icon={Icon(Icons.Rounded.Calculate,null)},label={Text("الحاسبات")}
                                    )
                                }
                            }
                        ){padding->
                            Surface(Modifier.padding(padding)){
                                when(tab){
                                    RootTab.HOME->HomeScreen(
                                        projects=projects,
                                        active=active,
                                        recentCalcs=repository.recentCalcs(),
                                        onContinue=::routeToActive,
                                        onProjects={tab=RootTab.PROJECTS},
                                        onToday={tab=RootTab.TODAY},
                                        onTools={tab=RootTab.TOOLS},
                                        onNewRoom=::openNewRoomFromActive,
                                        onSettings={route=Route.Settings}
                                    )

                                    RootTab.PROJECTS->ProjectsScreen(
                                        projects=projects,
                                        activeProjectId=active?.projectId,
                                        onOpen={route=Route.ProjectDetail(it)},
                                        onSetActive={pid->
                                            val p=projects.first{it.id==pid}
                                            setActive(ActiveLocation(pid,p.sections.firstOrNull()?.id))
                                        },
                                        onCreate={name,type->
                                            val first=Section(name="الرئيسي")
                                            val p=Project(name=name,type=type,sections=listOf(first))
                                            projects.add(p);persist()
                                            setActive(ActiveLocation(p.id,first.id))
                                            route=Route.ProjectDetail(p.id)
                                        }
                                    )

                                    RootTab.TODAY->TodayScreen(
                                        projects=projects,
                                        active=active,
                                        recent=repository.recentCalcs(),
                                        onOpenSpace={pid,sid,spid->
                                            if(active?.projectId==pid)setActive(ActiveLocation(pid,sid,spid))
                                            route=Route.RoomEdit(pid,sid,spid)
                                        },
                                        onOpenCalc={id->
                                            route=Route.Tool(id,null,active?.projectId)
                                        }
                                    )

                                    RootTab.TOOLS->ToolsScreen(
                                        recent=repository.recentCalcs(),
                                        hasActiveProject=activeProject()!=null,
                                        onOpen={id->route=Route.Tool(id,null,active?.projectId)}
                                    )
                                }
                            }
                        }
                    }

                    Route.Settings->SettingsScreen(
                        repository=repository,
                        appearance=appearance,
                        onAppearance={appearance=it;repository.setAppearance(it)},
                        onBack={route=Route.Root},
                        onReload={
                            projects.clear();projects.addAll(repository.loadProjects())
                            active=repository.activeLocation()
                        }
                    )

                    is Route.ProjectDetail->{
                        val p=projects.firstOrNull{it.id==r.projectId}
                        if(p==null){route=Route.Root}
                        else ProjectDetailScreen(
                            project=p,
                            active=active?.projectId==p.id,
                            onBack={route=Route.Root},
                            onSetActive={
                                setActive(ActiveLocation(p.id,p.sections.firstOrNull()?.id))
                            },
                            onOpenSection={sid->
                                if(active?.projectId==p.id)setActive(ActiveLocation(p.id,sid))
                                route=Route.SectionDetail(p.id,sid)
                            },
                            onAddSection={name->
                                val s=Section(name=name)
                                replaceProject(p.copy(sections=p.sections+s,updatedAt=System.currentTimeMillis()))
                                if(active?.projectId==p.id)setActive(ActiveLocation(p.id,s.id))
                            },
                            onOpenMaterials={material->
                                route=Route.Tool(material.toolId,material,p.id)
                            },
                            onShare={shareProject(p)}
                        )
                    }

                    is Route.SectionDetail->{
                        val p=projects.firstOrNull{it.id==r.projectId}
                        val s=p?.sections?.firstOrNull{it.id==r.sectionId}
                        if(p==null||s==null)route=Route.ProjectDetail(r.projectId)
                        else SectionScreen(
                            project=p,
                            section=s,
                            active=active,
                            onBack={route=Route.ProjectDetail(p.id)},
                            onAddRoom={route=Route.RoomEdit(p.id,s.id,null)},
                            onEditRoom={spid->
                                if(active?.projectId==p.id)setActive(ActiveLocation(p.id,s.id,spid))
                                route=Route.RoomEdit(p.id,s.id,spid)
                            },
                            onSetActiveRoom={spid->setActive(ActiveLocation(p.id,s.id,spid))},
                            onDetachCopy={spid->
                                val source=s.spaces.firstOrNull{it.id==spid}?:return@SectionScreen
                                if(source.repeatCount>1){
                                    val original=source.copy(repeatCount=source.repeatCount-1,updatedAt=System.currentTimeMillis())
                                    val detached=source.copy(
                                        id=UUID.randomUUID().toString(),
                                        name=source.name+" - نسخة مختلفة",
                                        repeatCount=1,
                                        updatedAt=System.currentTimeMillis()
                                    )
                                    val spaces=s.spaces.map{if(it.id==spid)original else it}+detached
                                    val updatedSection=s.copy(spaces=spaces)
                                    replaceProject(p.copy(
                                        sections=p.sections.map{if(it.id==s.id)updatedSection else it},
                                        updatedAt=System.currentTimeMillis()
                                    ))
                                    setActive(ActiveLocation(p.id,s.id,detached.id))
                                    route=Route.RoomEdit(p.id,s.id,detached.id)
                                }
                            }
                        )
                    }

                    is Route.RoomEdit->{
                        val p=projects.firstOrNull{it.id==r.projectId}
                        val s=p?.sections?.firstOrNull{it.id==r.sectionId}
                        val initial=s?.spaces?.firstOrNull{it.id==r.spaceId}
                        if(p==null||s==null)route=Route.Root
                        else RoomCaptureScreen(
                            title=if(initial==null)"حصر مكان جديد" else initial.name,
                            draftKey="${p.id}.${s.id}.${r.spaceId?:"new"}",
                            repository=repository,
                            initial=initial,
                            onBack={route=Route.SectionDetail(p.id,s.id)},
                            onSave={space->
                                val nextSpaces=if(s.spaces.any{it.id==space.id})
                                    s.spaces.map{if(it.id==space.id)space else it}
                                else s.spaces+space
                                val updatedSection=s.copy(spaces=nextSpaces)
                                replaceProject(p.copy(
                                    sections=p.sections.map{if(it.id==s.id)updatedSection else it},
                                    updatedAt=System.currentTimeMillis()
                                ))
                                setActive(ActiveLocation(p.id,s.id,space.id))
                                route=Route.SectionDetail(p.id,s.id)
                            }
                        )
                    }

                    is Route.Tool->{
                        val back={
                            route=if(r.projectId!=null)Route.ProjectDetail(r.projectId) else Route.Root
                        }
                        if(r.toolId in setOf("slope","convert","area")){
                            SiteUtilityScreen(r.toolId,onBack=back)
                        }else{
                            val projectId=r.projectId ?: active?.projectId
                            val targetProject=projectId?.let{id->projects.firstOrNull{it.id==id}}
                            MaterialCalculatorScreen(
                                toolId=r.toolId,
                                seed=r.seed,
                                repository=repository,
                                activeProject=targetProject,
                                onBack=back,
                                onSave={result,sectionId,spaceId->
                                    if(targetProject!=null)saveCalculation(targetProject.id,result,sectionId,spaceId)
                                }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

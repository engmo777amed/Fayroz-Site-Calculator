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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.domain.CalculatorLibrary
import android.widget.Toast
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class RootTab{ HOME,PROJECTS,TODAY,TOOLS,SAVED }

private sealed interface Route{
    data object Root:Route
    data object Settings:Route
    data object Prices:Route
    data class ProjectDetail(val projectId:String):Route
    data class SectionDetail(val projectId:String,val sectionId:String):Route
    data class RoomEdit(val projectId:String,val sectionId:String,val spaceId:String?,val session:Long=System.nanoTime(),val itemId:String?=null):Route
    data class DirectItem(val projectId:String):Route
    data class Tool(val toolId:String,val seed:MaterialResult?=null,val projectId:String?=null,val returnToProject:Boolean=false):Route
}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            val repository=remember{V8Repository(this)}
            val rootState=rememberSaveableStateHolder()
            val projects=remember{
                mutableStateListOf<Project>().apply{addAll(repository.loadProjects())}
            }
            var appearance by remember{mutableStateOf(repository.appearance())}
            var active by remember{mutableStateOf(repository.activeLocation())}
            var tab by remember{mutableStateOf(RootTab.HOME)}
            var route by remember{mutableStateOf<Route>(
                intent.getStringExtra("calculator")?.takeIf{id->(CalculatorLibrary.all.any{it.id==id}||id in setOf("convert","area"))}?.let{Route.Tool(it)}?:Route.Root
            )}

            fun persist(){
                repository.saveProjects(projects.toList())
            }

            fun replaceProject(updated:Project){
                val previous=projects.firstOrNull{it.id==updated.id}
                val i=projects.indexOfFirst{it.id==updated.id}
                if(i>=0)projects[i]=updated
                else projects.add(updated)
                persist()
                repository.syncCalculations(previous?.calculations.orEmpty(),updated.calculations)
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
                    id=result.inputs["_savedId"]?:java.util.UUID.randomUUID().toString(),
                    toolId=result.toolId,
                    title=result.title,
                    summary=summary,
                    sourceQuantity=result.sourceQuantity,
                    unit=result.sourceUnit,
                    sectionId=sectionId,
                    spaceId=spaceId,inputs=result.inputs,cost=result.cost,explanation=result.explanation
                )
                repository.saveRecentCalc(calc)
                val p=projects.firstOrNull{it.id==targetProjectId}?:return
                replaceProject(p.copy(
                    calculations=listOf(calc)+p.calculations.filterNot{it.id==calc.id},
                    updatedAt=System.currentTimeMillis()
                ))
            }

            fun openSaved(calc:SavedCalculation){
                val pid=projects.firstOrNull{p->p.calculations.any{it.id==calc.id}}?.id
                route=Route.Tool(calc.toolId,MaterialResult(calc.toolId,calc.title,calc.sourceQuantity,calc.unit,emptyList(),calc.explanation,calc.inputs+("_savedId" to calc.id),calc.cost),pid)
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
                        Route.Prices->Route.Root
                        is Route.ProjectDetail->Route.Root
                        is Route.SectionDetail->Route.ProjectDetail(r.projectId)
                        is Route.RoomEdit->Route.SectionDetail(r.projectId,r.sectionId)
                        is Route.DirectItem->Route.ProjectDetail(r.projectId)
                        is Route.Tool->if(r.returnToProject&&r.projectId!=null)Route.ProjectDetail(r.projectId) else Route.Root
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
                                        selected=tab==RootTab.SAVED,onClick={tab=RootTab.SAVED},
                                        icon={Icon(Icons.Rounded.History,null)},label={Text("المحفوظات")}
                                    )
                                    NavigationBarItem(
                                        selected=tab==RootTab.TOOLS,onClick={tab=RootTab.TOOLS},
                                        icon={Icon(Icons.Rounded.Calculate,null)},label={Text("الحاسبات")}
                                    )
                                }
                            }
                        ){padding->
                            Surface(Modifier.padding(padding)){
                                rootState.SaveableStateProvider("root-${tab.name}"){when(tab){
                                    RootTab.HOME->HomeScreen(
                                        projects=projects,
                                        active=active,
                                        recentCalcs=repository.recentCalcs(),
                                        onContinue=::routeToActive,
                                        onProjects={tab=RootTab.PROJECTS},
                                        onToday={tab=RootTab.TODAY},
                                        onTools={tab=RootTab.TOOLS},
                                        onNewRoom=::openNewRoomFromActive,
                                        onDirectItem={
                                            val p=activeProject()
                                            if(p==null){tab=RootTab.PROJECTS}
                                            else route=Route.DirectItem(p.id)
                                        },
                                        onSettings={route=Route.Settings},
                                        onSaved={tab=RootTab.SAVED},onPrices={route=Route.Prices},
                                        onOpenSaved=::openSaved
                                    )

                                    RootTab.PROJECTS->ProjectsScreen(
                                        projects=projects,
                                        activeProjectId=active?.projectId,
                                        onOpen={route=Route.ProjectDetail(it)},
                                        onSetActive={pid->
                                            val p=projects.first{it.id==pid}
                                            setActive(ActiveLocation(pid,p.sections.firstOrNull()?.id))
                                        },
                                        onUpdate={replaceProject(it)},
                                        onDelete={id->
                                            projects.firstOrNull{it.id==id}?.calculations?.forEach{repository.deleteRecentCalc(it.id)}
                                            projects.removeAll{it.id==id};persist()
                                            if(active?.projectId==id){active=null;repository.clearActive()}
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
                                        onOpenCalc=::openSaved
                                    )

                                    RootTab.SAVED->SavedCalculationsScreen(repository.recentCalcs(),onDelete={id->
                                        repository.deleteRecentCalc(id)
                                        projects.toList().filter{p->p.calculations.any{it.id==id}}.forEach{p->replaceProject(p.copy(calculations=p.calculations.filterNot{it.id==id}))}
                                        route=Route.Root
                                    }){calc->
                                        val pid=projects.firstOrNull{p->p.calculations.any{it.id==calc.id}}?.id
                                        route=Route.Tool(calc.toolId,MaterialResult(calc.toolId,calc.title,calc.sourceQuantity,calc.unit,emptyList(),calc.explanation,calc.inputs+("_savedId" to calc.id),calc.cost),pid)
                                    }
                                    RootTab.TOOLS->ToolsScreen(
                                        recent=repository.recentCalcs(),
                                        hasActiveProject=activeProject()!=null,
                                        onOpen={id->route=Route.Tool(id)},
                                        repository=repository,
                                        onOpenSaved=::openSaved
                                    )
                                }}
                            }
                        }
                    }

                    Route.Prices->PricesScreen(projects.toList(),active?.projectId,{route=Route.Root},{replaceProject(it)})
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
                                route=Route.Tool(material.toolId,material,p.id,true)
                            },
                            onDirectItem={route=Route.DirectItem(p.id)},
                            onShare=::shareProject,
                            onUpdate={replaceProject(it)},
                            onEditSource={sid,spid,itemId->route=if(itemId in p.calculations.map{it.id}){val c=p.calculations.first{it.id==itemId};Route.Tool(c.toolId,MaterialResult(c.toolId,c.title,c.sourceQuantity,c.unit,emptyList(),c.explanation,c.inputs+("_savedId" to c.id),c.cost),p.id)}else Route.RoomEdit(p.id,sid,spid,itemId=itemId)},
                            onAddPlace={sid->route=Route.RoomEdit(p.id,sid,null)},
                            onOpenCalc={calc->route=Route.Tool(calc.toolId,MaterialResult(calc.toolId,calc.title,calc.sourceQuantity,calc.unit,emptyList(),calc.explanation,calc.inputs+("_savedId" to calc.id),calc.cost),p.id,true)}
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
                            onUpdateSection={updated->replaceProject(p.copy(sections=p.sections.map{if(it.id==updated.id)updated else it},updatedAt=System.currentTimeMillis()))},
                            onAddRoom={route=Route.RoomEdit(p.id,s.id,null)},
                            onEditRoom={spid->
                                if(active?.projectId==p.id)setActive(ActiveLocation(p.id,s.id,spid))
                                route=Route.RoomEdit(p.id,s.id,spid)
                            },
                            onSetActiveRoom={spid->setActive(ActiveLocation(p.id,s.id,spid))},
                            onDuplicateRoom={spid->
                                val source=s.spaces.firstOrNull{it.id==spid}?:return@SectionScreen
                                val copy=source.copy(
                                    id=UUID.randomUUID().toString(),
                                    name=source.name+" - نسخة",
                                    updatedAt=System.currentTimeMillis()
                                )
                                val updatedSection=s.copy(spaces=s.spaces+copy)
                                replaceProject(p.copy(
                                    sections=p.sections.map{if(it.id==s.id)updatedSection else it},
                                    updatedAt=System.currentTimeMillis()
                                ))
                            },
                            onDeleteRoom={spid->
                                val updatedSection=s.copy(spaces=s.spaces.filterNot{it.id==spid})
                                replaceProject(p.copy(
                                    calculations=p.calculations.filterNot{it.spaceId==spid},
                                    sections=p.sections.map{if(it.id==s.id)updatedSection else it},
                                    updatedAt=System.currentTimeMillis()
                                ))
                                if(active?.spaceId==spid)setActive(ActiveLocation(p.id,s.id))
                            },
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
                        val section=p?.sections?.firstOrNull{it.id==r.sectionId}
                        val initial=section?.spaces?.firstOrNull{it.id==r.spaceId}
                        if(p==null||section==null)route=Route.Root
                        else {
                            fun saveSpace(space:Space,next:Boolean){
                                val current=projects.first{it.id==p.id}
                                val sec=current.sections.first{it.id==section.id}
                                val spaces=if(sec.spaces.any{it.id==space.id})sec.spaces.map{if(it.id==space.id)space else it}else sec.spaces+space
                                replaceProject(current.copy(sections=current.sections.map{if(it.id==sec.id)it.copy(spaces=spaces)else it},updatedAt=System.currentTimeMillis()))
                                setActive(ActiveLocation(p.id,sec.id,space.id))
                                route=if(next)Route.RoomEdit(p.id,sec.id,null)else Route.SectionDetail(p.id,sec.id)
                            }
                            key(r.session){RoomCaptureScreen(
                                title=initial?.name?:"حصر مكان جديد",
                                draftKey="${p.id}.${section.id}.${r.spaceId?:"new"}",repository=repository,initial=initial,defaults=p.defaults,initialItemId=r.itemId,
                                onBack={route=Route.SectionDetail(p.id,section.id)},
                                onSave={saveSpace(it,false)},onSaveNext={saveSpace(it,true)}
                            )}
                        }
                    }

                    is Route.DirectItem->{
                        val p=projects.firstOrNull{it.id==r.projectId}
                        if(p==null)route=Route.Root
                        else if(p.sections.isEmpty())replaceProject(p.copy(sections=listOf(Section(name="الرئيسي"))))
                        else DirectItemScreen(
                            project=p,
                            initialSectionId=active?.sectionId,
                            onBack={route=Route.ProjectDetail(p.id)},
                            onSave={sectionId,space->
                                val section=p.sections.firstOrNull{it.id==sectionId} ?: return@DirectItemScreen
                                val updatedSection=section.copy(spaces=section.spaces+space)
                                replaceProject(p.copy(
                                    sections=p.sections.map{if(it.id==sectionId)updatedSection else it},
                                    updatedAt=System.currentTimeMillis()
                                ))
                                setActive(ActiveLocation(p.id,sectionId,space.id))
                                route=Route.SectionDetail(p.id,sectionId)
                            }
                        )
                    }

                    is Route.Tool->{
                        val back={
                            route=if(r.returnToProject&&r.projectId!=null)Route.ProjectDetail(r.projectId) else Route.Root
                        }
                        if(r.toolId in setOf("convert","area")){
                            SiteUtilityScreen(r.toolId,onBack=back,repository=repository,seed=r.seed)
                        }else{
                            val projectId=r.projectId ?: if(r.seed==null)active?.projectId else null
                            val targetProject=projectId?.let{id->projects.firstOrNull{it.id==id}}
                            key(r.toolId,r.seed){LibraryCalculatorScreen(
                                toolId=r.toolId,
                                seed=r.seed,
                                repository=repository,
                                project=targetProject,
                                onBack=back,
                                onSave={result,sectionId,spaceId->
                                    if(targetProject!=null)saveCalculation(targetProject.id,result,sectionId,spaceId)
                                }
                            )}
                        }
                    }
                }
                }
            }
        }
    }
}


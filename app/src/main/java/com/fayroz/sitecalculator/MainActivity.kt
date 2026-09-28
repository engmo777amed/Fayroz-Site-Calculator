package com.fayroz.sitecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.ProjectRepository
import com.fayroz.sitecalculator.reports.Reports
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class RootTab { HOME,PROJECTS,TODAY,TOOLS,SETTINGS }
private enum class Route { ROOT,NEW_PROJECT,PROJECT,SECTION,ADD_ROOM,EDIT_ROOM,ADD_ITEM,QUICK_ROOM,QUICK_ITEM,QUICK_STAIR,QUICK_TOOL }

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            val context=LocalContext.current
            val repository=remember{ProjectRepository(context.applicationContext)}
            val appearancePrefs=remember{context.getSharedPreferences("fayroz_v7_appearance",MODE_PRIVATE)}
            var appearance by remember{
                mutableStateOf(
                    runCatching{
                        Appearance.valueOf(appearancePrefs.getString("mode",Appearance.DARK.name)?:Appearance.DARK.name)
                    }.getOrDefault(Appearance.DARK)
                )
            }
            fun setAppearance(value:Appearance){
                appearance=value
                appearancePrefs.edit().putString("mode",value.name).apply()
            }

            FayrozTheme(appearance){
                var projects by remember{mutableStateOf(repository.loadProjects())}
                var tab by remember{mutableStateOf(RootTab.HOME)}
                var route by remember{mutableStateOf(Route.ROOT)}
                var projectId by remember{mutableStateOf<String?>(null)}
                var sectionId by remember{mutableStateOf<String?>(null)}
                var roomId by remember{mutableStateOf<String?>(null)}
                var quickSession by remember{mutableIntStateOf(1)}
                var selectedToolId by remember{mutableStateOf<String?>(null)}
                var toolSeedArea by remember{mutableStateOf<Double?>(null)}
                var toolBackRoute by remember{mutableStateOf(Route.ROOT)}

                fun reload(){projects=repository.loadProjects()}
                fun persist(reason:String){
                    projects.firstOrNull{it.id==projectId}?.updatedAt=System.currentTimeMillis()
                    repository.saveProjects(projects,reason)
                    reload()
                }

                val project=projects.firstOrNull{it.id==projectId}
                val section=project?.sections?.firstOrNull{it.id==sectionId}
                val room=section?.spaces?.firstOrNull{it.id==roomId}

                BackHandler(enabled=route!=Route.ROOT){
                    route=when(route){
                        Route.NEW_PROJECT->Route.ROOT
                        Route.PROJECT->Route.ROOT
                        Route.SECTION->Route.PROJECT
                        Route.ADD_ROOM,Route.EDIT_ROOM,Route.ADD_ITEM->Route.SECTION
                        Route.QUICK_ROOM,Route.QUICK_ITEM,Route.QUICK_STAIR->Route.ROOT
                        Route.QUICK_TOOL->toolBackRoute
                        Route.ROOT->Route.ROOT
                    }
                    if(route==Route.ROOT && tab==RootTab.HOME)tab=RootTab.PROJECTS
                }

                if(route==Route.ROOT){
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
                                    icon={Icon(Icons.Rounded.Today,null)},label={Text("اليوم")}
                                )
                                NavigationBarItem(
                                    selected=tab==RootTab.TOOLS,onClick={tab=RootTab.TOOLS},
                                    icon={Icon(Icons.Rounded.Calculate,null)},label={Text("الحاسبات")}
                                )
                                NavigationBarItem(
                                    selected=tab==RootTab.SETTINGS,onClick={tab=RootTab.SETTINGS},
                                    icon={Icon(Icons.Rounded.Settings,null)},label={Text("الإعدادات")}
                                )
                            }
                        }
                    ){padding->
                        Box(Modifier.padding(padding)){
                            when(tab){
                                RootTab.HOME->HomeScreen(
                                    projects,
                                    onNewProject={route=Route.NEW_PROJECT},
                                    onProjects={tab=RootTab.PROJECTS},
                                    onQuickRoom={route=Route.QUICK_ROOM},
                                    onQuickItem={route=Route.QUICK_ITEM},
                                    onToday={tab=RootTab.TODAY},
                                    onOpenLast={id->projectId=id;route=Route.PROJECT}
                                )

                                RootTab.PROJECTS->ProjectsScreen(
                                    projects,
                                    onNew={route=Route.NEW_PROJECT},
                                    onOpen={id->projectId=id;route=Route.PROJECT},
                                    canUndo=repository.canUndo(),
                                    onUndo={repository.undo()?.let{projects=it}}
                                )

                                RootTab.TODAY->TodayScreen(
                                    projects,
                                    onOpenSpace={ref->
                                        projectId=ref.projectId;sectionId=ref.sectionId;roomId=ref.spaceId;route=Route.EDIT_ROOM
                                    },
                                    onQuickRoom={route=Route.QUICK_ROOM},
                                    onQuickItem={route=Route.QUICK_ITEM}
                                )

                                RootTab.TOOLS->CalculatorHubScreen(
                                    repository=repository,
                                    onRoom={route=Route.QUICK_ROOM},
                                    onItem={route=Route.QUICK_ITEM},
                                    onStair={route=Route.QUICK_STAIR},
                                    onTool={id->
                                        selectedToolId=id
                                        toolSeedArea=null
                                        toolBackRoute=Route.ROOT
                                        route=Route.QUICK_TOOL
                                    }
                                )

                                RootTab.SETTINGS->SettingsScreen(
                                    appearance=appearance,
                                    onAppearance=::setAppearance,
                                    repository=repository,
                                    onShareBackup={Reports.shareBackup(context,repository.exportBackup())},
                                    onImportBackup={raw->
                                        val ok=repository.importBackup(raw)
                                        if(ok)reload()
                                        ok
                                    }
                                )
                            }
                        }
                    }
                }else{
                    when(route){
                        Route.NEW_PROJECT->NewProjectScreen(
                            onCreate={name,type->
                                val first=when(type){
                                    "شقة"->"الشقة"
                                    "محل"->"المحل"
                                    "بيت","عمارة"->"الدور الأرضي"
                                    else->"الجزء الأول"
                                }
                                val p=SiteProject(
                                    id=UUID.randomUUID().toString(),
                                    name=name,type=type,
                                    sections=mutableListOf(SectionEntry(name=first))
                                )
                                projects.add(0,p)
                                repository.saveProjects(projects,"إنشاء مشروع")
                                reload()
                                projectId=p.id
                                route=Route.PROJECT
                            },
                            onBack={route=Route.ROOT;tab=RootTab.PROJECTS}
                        )

                        Route.PROJECT->{
                            if(project==null){
                                route=Route.ROOT;tab=RootTab.PROJECTS
                            }else ProjectDetailScreen(
                                project=project,
                                onBack={route=Route.ROOT;tab=RootTab.PROJECTS},
                                onOpenSection={id->sectionId=id;route=Route.SECTION},
                                onAddSection={name->
                                    project.sections.add(SectionEntry(name=name))
                                    persist("إضافة دور أو جزء")
                                },
                                onDuplicateSection={id->
                                    val src=project.sections.firstOrNull{it.id==id}
                                    if(src!=null){
                                        project.sections.add(deepCopySection(src,src.name+" - نسخة"))
                                        persist("نسخ دور أو جزء")
                                    }
                                },
                                onMoveSection={id,delta->
                                    moveById(project.sections,id,delta)
                                    persist("ترتيب الأدوار")
                                },
                                onRenameProject={name->project.name=name;persist("تعديل اسم المشروع")},
                                onDeleteProject={
                                    projects.removeAll{it.id==project.id}
                                    repository.saveProjects(projects,"حذف مشروع")
                                    reload()
                                    projectId=null;sectionId=null;roomId=null
                                    route=Route.ROOT;tab=RootTab.PROJECTS
                                },
                                onShare={Reports.shareText(context,project.name,Reports.summaryText(project))},
                                onExportCsv={Reports.shareCsv(context,project)},
                                onExportPdf={Reports.sharePdf(context,project)},
                                onOpenMaterialTool={id,area->
                                    repository.recordToolUse(id)
                                    selectedToolId=id
                                    toolSeedArea=area
                                    toolBackRoute=Route.PROJECT
                                    route=Route.QUICK_TOOL
                                }
                            )
                        }

                        Route.SECTION->{
                            if(project==null||section==null){
                                route=Route.PROJECT
                            }else SectionScreen(
                                projectName=project.name,
                                section=section,
                                onBack={route=Route.PROJECT},
                                onAddRoom={roomId=null;route=Route.ADD_ROOM},
                                onEditRoom={id->roomId=id;route=Route.EDIT_ROOM},
                                onCopyRoom={id->
                                    val src=section.spaces.firstOrNull{it.id==id}
                                    if(src!=null){
                                        section.spaces.add(deepCopySpace(src,src.name+" - نسخة"))
                                        persist("نسخ غرفة")
                                    }
                                },
                                onDeleteRoom={id->
                                    section.spaces.removeAll{it.id==id}
                                    persist("حذف غرفة")
                                },
                                onMoveRoom={id,delta->
                                    moveById(section.spaces,id,delta)
                                    persist("ترتيب الغرف")
                                },
                                onAddDirectItem={route=Route.ADD_ITEM},
                                onRename={name->section.name=name;persist("تعديل اسم الدور")},
                                onDeleteSection={
                                    project.sections.removeAll{it.id==section.id}
                                    persist("حذف دور أو جزء")
                                    sectionId=null
                                    route=Route.PROJECT
                                }
                            )
                        }

                        Route.ADD_ROOM->{
                            if(project==null||section==null){
                                route=Route.SECTION
                            }else key("add_"+quickSession){
                                RoomEditorScreen(
                                    repository=repository,
                                    draftKey="project_${project.id}_section_${section.id}_new",
                                    title="إضافة غرفة / فراغ",
                                    existing=section.spaces,
                                    onBack={route=Route.SECTION},
                                    onSave={newSpace->
                                        section.spaces.add(newSpace)
                                        persist("إضافة غرفة")
                                        route=Route.SECTION
                                    },
                                    onSaveAndContinue={newSpace->
                                        section.spaces.add(newSpace)
                                        persist("إضافة غرفة")
                                    },
                                    onReset={quickSession++}
                                )
                            }
                        }

                        Route.EDIT_ROOM->{
                            if(project==null||section==null||room==null){
                                route=Route.SECTION
                            }else RoomEditorScreen(
                                repository=repository,
                                draftKey="room_${room.id}",
                                title="تعديل ${room.name}",
                                initial=room,
                                existing=section.spaces.filter{it.id!=room.id},
                                onBack={route=Route.SECTION},
                                onSave={edited->
                                    val index=section.spaces.indexOfFirst{it.id==edited.id}
                                    if(index>=0)section.spaces[index]=edited
                                    persist("تعديل غرفة")
                                    route=Route.SECTION
                                }
                            )
                        }

                        Route.ADD_ITEM->{
                            if(project==null||section==null){
                                route=Route.SECTION
                            }else QuickItemScreen(
                                onBack={route=Route.SECTION},
                                projectContext="${project.name} ← ${section.name}",
                                onSave={entry->
                                    section.spaces.add(entry)
                                    persist("حصر بند مباشر")
                                    route=Route.SECTION
                                }
                            )
                        }

                        Route.QUICK_ROOM->key("quick_"+quickSession){
                            RoomEditorScreen(
                                repository=repository,
                                draftKey="quick_room",
                                title="حصر فراغ سريع",
                                onBack={route=Route.ROOT},
                                onReset={quickSession++}
                            )
                        }

                        Route.QUICK_ITEM->QuickItemScreen(onBack={route=Route.ROOT})
                        Route.QUICK_STAIR->StairCalculatorScreen(onBack={route=Route.ROOT})
                        Route.QUICK_TOOL->{
                            val id=selectedToolId
                            if(id==null){
                                route=Route.ROOT
                                tab=RootTab.TOOLS
                            }else SiteToolCalculatorScreen(
                                toolId=id,
                                repository=repository,
                                seedArea=toolSeedArea,
                                onBack={
                                    route=toolBackRoute
                                    if(toolBackRoute==Route.ROOT)tab=RootTab.TOOLS
                                },
                                onOpenTool={nextId,area->
                                    repository.recordToolUse(nextId)
                                    selectedToolId=nextId
                                    toolSeedArea=area
                                }
                            )
                        }
                        Route.ROOT->Unit
                    }
                }
            }
        }
    }
}

private fun <T> moveById(list:MutableList<T>,id:String,delta:Int){
    val index=list.indexOfFirst{item->
        when(item){
            is SectionEntry->item.id==id
            is SpaceEntry->item.id==id
            else->false
        }
    }
    if(index<0)return
    val target=(index+delta).coerceIn(0,list.lastIndex)
    if(target==index)return
    val item=list.removeAt(index)
    list.add(target,item)
}

private fun deepCopySection(src:SectionEntry,newName:String)=SectionEntry(
    id=UUID.randomUUID().toString(),
    name=newName,
    spaces=src.spaces.map{deepCopySpace(it,it.name)}.toMutableList()
)

private fun deepCopySpace(src:SpaceEntry,newName:String):SpaceEntry{
    val wallMap=src.walls.associate{it.id to UUID.randomUUID().toString()}
    return src.copy(
        id=UUID.randomUUID().toString(),
        name=newName,
        walls=src.walls.map{it.copy(id=wallMap[it.id] ?: UUID.randomUUID().toString())},
        floorSurfaces=src.floorSurfaces.map{it.copy(id=UUID.randomUUID().toString())},
        ceilingSurfaces=src.ceilingSurfaces.map{it.copy(id=UUID.randomUUID().toString())},
        openings=src.openings.map{
            it.copy(
                id=UUID.randomUUID().toString(),
                wallId=it.wallId?.let{oldId->wallMap[oldId]}
            )
        },
        takeoffs=src.takeoffs.map{t->
            t.copy(
                id=UUID.randomUUID().toString(),
                adjustments=t.adjustments.map{it.copy(id=UUID.randomUUID().toString())}
            )
        },
        updatedAt=System.currentTimeMillis()
    )
}

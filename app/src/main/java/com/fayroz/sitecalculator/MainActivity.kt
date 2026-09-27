package com.fayroz.sitecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.ProjectRepository
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class RootTab { DASHBOARD, PROJECTS, TOOLS, SETTINGS }
private enum class Route { ROOT, NEW_PROJECT, PROJECT, SECTION, ADD_ROOM, EDIT_ROOM, QUICK_ROOM, QUICK_ITEM }

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            val context=LocalContext.current
            val appearancePrefs=remember{context.getSharedPreferences("fayroz_v6_appearance",MODE_PRIVATE)}
            var appearance by remember{
                mutableStateOf(
                    runCatching{
                        Appearance.valueOf(appearancePrefs.getString("mode",Appearance.DARK.name)?:Appearance.DARK.name)
                    }.getOrDefault(Appearance.DARK)
                )
            }
            fun setAppearance(a:Appearance){
                appearance=a
                appearancePrefs.edit().putString("mode",a.name).apply()
            }

            FayrozTheme(appearance){
                val repository=remember{ProjectRepository(context.applicationContext)}
                var projects by remember{mutableStateOf(repository.load())}
                var tab by remember{mutableStateOf(RootTab.DASHBOARD)}
                var route by remember{mutableStateOf(Route.ROOT)}
                var projectId by remember{mutableStateOf<String?>(null)}
                var sectionId by remember{mutableStateOf<String?>(null)}
                var roomId by remember{mutableStateOf<String?>(null)}

                fun reload(){
                    projects=repository.load()
                }
                fun persist(){
                    repository.save(projects)
                    reload()
                }

                val project=projects.firstOrNull{it.id==projectId}
                val section=project?.sections?.firstOrNull{it.id==sectionId}
                val room=section?.rooms?.firstOrNull{it.id==roomId}

                if(route==Route.ROOT){
                    Scaffold(
                        bottomBar={
                            NavigationBar{
                                NavigationBarItem(
                                    selected=tab==RootTab.DASHBOARD,
                                    onClick={tab=RootTab.DASHBOARD},
                                    icon={Icon(Icons.Rounded.Home,null)},
                                    label={Text("الرئيسية")}
                                )
                                NavigationBarItem(
                                    selected=tab==RootTab.PROJECTS,
                                    onClick={tab=RootTab.PROJECTS},
                                    icon={Icon(Icons.Rounded.FolderOpen,null)},
                                    label={Text("المشروعات")}
                                )
                                NavigationBarItem(
                                    selected=tab==RootTab.TOOLS,
                                    onClick={tab=RootTab.TOOLS},
                                    icon={Icon(Icons.Rounded.Calculate,null)},
                                    label={Text("الحاسبات")}
                                )
                                NavigationBarItem(
                                    selected=tab==RootTab.SETTINGS,
                                    onClick={tab=RootTab.SETTINGS},
                                    icon={Icon(Icons.Rounded.Settings,null)},
                                    label={Text("الإعدادات")}
                                )
                            }
                        }
                    ){padding->
                        androidx.compose.foundation.layout.Box(Modifier.padding(padding)){
                            when(tab){
                                RootTab.DASHBOARD -> DashboardScreen(
                                    projects=projects,
                                    onNewProject={route=Route.NEW_PROJECT},
                                    onRoomCalc={route=Route.QUICK_ROOM},
                                    onItemCalc={route=Route.QUICK_ITEM},
                                    onProjects={tab=RootTab.PROJECTS},
                                    onOpenLast={id->projectId=id;route=Route.PROJECT}
                                )
                                RootTab.PROJECTS -> ProjectsListScreen(
                                    projects=projects,
                                    onNew={route=Route.NEW_PROJECT},
                                    onOpen={id->projectId=id;route=Route.PROJECT}
                                )
                                RootTab.TOOLS -> ToolsLandingScreen(
                                    onRoom={route=Route.QUICK_ROOM},
                                    onItem={route=Route.QUICK_ITEM}
                                )
                                RootTab.SETTINGS -> SettingsScreen(
                                    appearance=appearance,
                                    onAppearance=::setAppearance
                                )
                            }
                        }
                    }
                }else{
                    when(route){
                        Route.NEW_PROJECT -> NewProjectScreenV6(
                            onCreate={name,type->
                                val first=when(type){
                                    "شقة"->"الشقة"
                                    "محل"->"المحل"
                                    "بيت","عمارة"->"الدور الأرضي"
                                    else->"الجزء الأول"
                                }
                                val p=SiteProject(
                                    id=UUID.randomUUID().toString(),
                                    name=name,
                                    type=type,
                                    sections=mutableListOf(SectionEntry(name=first))
                                )
                                projects.add(0,p)
                                persist()
                                projectId=p.id
                                route=Route.PROJECT
                            },
                            onBack={route=Route.ROOT}
                        )

                        Route.PROJECT -> {
                            if(project==null){
                                route=Route.ROOT
                                tab=RootTab.PROJECTS
                            }else{
                                ProjectDetailScreen(
                                    project=project,
                                    onBack={route=Route.ROOT;tab=RootTab.PROJECTS},
                                    onOpenSection={id->sectionId=id;route=Route.SECTION},
                                    onAddSection={name->
                                        project.sections.add(SectionEntry(name=name))
                                        persist()
                                    },
                                    onRenameProject={name->
                                        project.name=name
                                        persist()
                                    },
                                    onDeleteProject={
                                        projects.removeAll{it.id==project.id}
                                        persist()
                                        projectId=null
                                        sectionId=null
                                        roomId=null
                                        route=Route.ROOT
                                        tab=RootTab.PROJECTS
                                    }
                                )
                            }
                        }

                        Route.SECTION -> {
                            if(project==null||section==null){
                                route=Route.PROJECT
                            }else{
                                SectionDetailScreen(
                                    projectName=project.name,
                                    section=section,
                                    onBack={route=Route.PROJECT},
                                    onAddRoom={roomId=null;route=Route.ADD_ROOM},
                                    onEditRoom={id->roomId=id;route=Route.EDIT_ROOM},
                                    onCopyRoom={id->
                                        val src=section.rooms.firstOrNull{it.id==id}
                                        if(src!=null){
                                            section.rooms.add(src.copy(id=UUID.randomUUID().toString(),name=src.name+" - نسخة"))
                                            persist()
                                        }
                                    },
                                    onDeleteRoom={id->
                                        section.rooms.removeAll{it.id==id}
                                        persist()
                                    },
                                    onRename={name->
                                        section.name=name
                                        persist()
                                    },
                                    onDeleteSection={
                                        project.sections.removeAll{it.id==section.id}
                                        persist()
                                        sectionId=null
                                        route=Route.PROJECT
                                    }
                                )
                            }
                        }

                        Route.ADD_ROOM -> {
                            if(project==null||section==null){
                                route=Route.SECTION
                            }else{
                                RoomEditorScreen(
                                    repository=repository,
                                    title="إضافة غرفة",
                                    existing=section.rooms,
                                    onBack={route=Route.SECTION},
                                    onSave={newRoom->
                                        section.rooms.add(newRoom)
                                        persist()
                                        route=Route.SECTION
                                    },
                                    onSaveAndContinue={newRoom->
                                        section.rooms.add(newRoom)
                                        persist()
                                    }
                                )
                            }
                        }

                        Route.EDIT_ROOM -> {
                            if(project==null||section==null||room==null){
                                route=Route.SECTION
                            }else{
                                RoomEditorScreen(
                                    repository=repository,
                                    title="تعديل ${room.name}",
                                    initial=room,
                                    existing=section.rooms.filter{it.id!=room.id},
                                    onBack={route=Route.SECTION},
                                    onSave={edited->
                                        val index=section.rooms.indexOfFirst{it.id==edited.id}
                                        if(index>=0)section.rooms[index]=edited
                                        persist()
                                        route=Route.SECTION
                                    }
                                )
                            }
                        }

                        Route.QUICK_ROOM -> RoomEditorScreen(
                            repository=repository,
                            title="حصر فراغ سريع",
                            onBack={route=Route.ROOT}
                        )

                        Route.QUICK_ITEM -> ItemCalculatorScreenV6(
                            repository=repository,
                            onBack={route=Route.ROOT}
                        )

                        Route.ROOT -> Unit
                    }
                }
            }
        }
    }
}

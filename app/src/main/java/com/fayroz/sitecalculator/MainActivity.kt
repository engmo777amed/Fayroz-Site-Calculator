package com.fayroz.sitecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.fayroz.sitecalculator.data.*
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class Screen{HOME,NEW_PROJECT,PROJECT,SECTION,ADD_ROOM,EDIT_ROOM,QUICK_ROOM,QUICK_ITEM}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{
            val appContext=applicationContext
            val store=remember{ProjectStore(appContext)}
            val appearancePrefs=remember{appContext.getSharedPreferences("fayroz_clean_appearance",MODE_PRIVATE)}
            var appearance by remember{
                mutableStateOf(
                    runCatching{
                        Appearance.valueOf(
                            appearancePrefs.getString("mode",Appearance.DARK.name)?:Appearance.DARK.name
                        )
                    }.getOrDefault(Appearance.DARK)
                )
            }
            fun setAppearance(v:Appearance){
                appearance=v
                appearancePrefs.edit().putString("mode",v.name).apply()
            }

            var projects by remember{mutableStateOf(store.load())}
            var screen by remember{mutableStateOf(Screen.HOME)}
            var projectId by remember{mutableStateOf<String?>(null)}
            var sectionId by remember{mutableStateOf<String?>(null)}
            var roomId by remember{mutableStateOf<String?>(null)}

            fun reload(){projects=store.load()}
            fun save(){store.save(projects);reload()}

            val project=projects.firstOrNull{it.id==projectId}
            val section=project?.sections?.firstOrNull{it.id==sectionId}
            val room=section?.rooms?.firstOrNull{it.id==roomId}

            FayrozTheme(appearance){
                when(screen){
                    Screen.HOME->HomeScreen(
                        projects,appearance,::setAppearance,
                        {screen=Screen.NEW_PROJECT},
                        {id->projectId=id;sectionId=null;roomId=null;screen=Screen.PROJECT},
                        {screen=Screen.QUICK_ROOM},
                        {screen=Screen.QUICK_ITEM}
                    )

                    Screen.NEW_PROJECT->NewProjectScreen(
                        onBack={screen=Screen.HOME},
                        onCreate={name,type->
                            val first=when(type){
                                "شقة"->"الشقة"
                                "محل"->"المحل"
                                "بيت","عمارة"->"الدور الأرضي"
                                else->"الجزء الأول"
                            }
                            val p=Project(name=name,type=type,sections=mutableListOf(Section(name=first)))
                            projects.add(0,p)
                            save()
                            projectId=p.id
                            screen=Screen.PROJECT
                        }
                    )

                    Screen.PROJECT->if(project==null){
                        screen=Screen.HOME
                    }else ProjectScreen(
                        project=project,
                        onBack={screen=Screen.HOME},
                        onOpenSection={id->sectionId=id;roomId=null;screen=Screen.SECTION},
                        onAddSection={name->project.sections.add(Section(name=name));save()},
                        onRename={name->project.name=name;save()},
                        onDelete={
                            projects.removeAll{it.id==project.id}
                            save()
                            projectId=null
                            screen=Screen.HOME
                        },
                        onRenameSection={id,name->
                            project.sections.firstOrNull{it.id==id}?.name=name
                            save()
                        },
                        onDeleteSection={id->
                            project.sections.removeAll{it.id==id}
                            save()
                        }
                    )

                    Screen.SECTION->if(project==null||section==null){
                        screen=Screen.PROJECT
                    }else SectionScreen(
                        projectName=project.name,
                        section=section,
                        onBack={screen=Screen.PROJECT},
                        onAdd={roomId=null;screen=Screen.ADD_ROOM},
                        onEdit={id->roomId=id;screen=Screen.EDIT_ROOM},
                        onCopy={id->
                            section.rooms.firstOrNull{it.id==id}?.let{src->
                                section.rooms.add(
                                    src.copy(
                                        id=UUID.randomUUID().toString(),
                                        name=src.name+" - نسخة",
                                        works=src.works.toMutableList(),
                                        openings=src.openings.toMutableList()
                                    )
                                )
                                save()
                            }
                        },
                        onDelete={id->section.rooms.removeAll{it.id==id};save()},
                        onRename={name->section.name=name;save()}
                    )

                    Screen.ADD_ROOM->if(project==null||section==null){
                        screen=Screen.SECTION
                    }else RoomEditor(
                        title="إضافة غرفة",
                        initial=null,
                        existing=section.rooms,
                        lastHeight=store.lastHeight(),
                        onHeight=store::setLastHeight,
                        onBack={screen=Screen.SECTION},
                        onSave={r->
                            section.rooms.add(r)
                            save()
                            screen=Screen.SECTION
                        },
                        onSaveMore={r->
                            section.rooms.add(r)
                            save()
                        }
                    )

                    Screen.EDIT_ROOM->if(project==null||section==null||room==null){
                        screen=Screen.SECTION
                    }else RoomEditor(
                        title="تعديل ${room.name}",
                        initial=room,
                        existing=section.rooms.filter{it.id!=room.id},
                        lastHeight=store.lastHeight(),
                        onHeight=store::setLastHeight,
                        onBack={screen=Screen.SECTION},
                        onSave={edited->
                            val i=section.rooms.indexOfFirst{it.id==edited.id}
                            if(i>=0)section.rooms[i]=edited
                            save()
                            screen=Screen.SECTION
                        }
                    )

                    Screen.QUICK_ROOM->QuickRoomScreen(
                        store.lastHeight(),
                        store::setLastHeight
                    ){screen=Screen.HOME}

                    Screen.QUICK_ITEM->ItemCalculatorScreen(
                        store.lastHeight(),
                        store::setLastHeight
                    ){screen=Screen.HOME}
                }
            }
        }
    }
}

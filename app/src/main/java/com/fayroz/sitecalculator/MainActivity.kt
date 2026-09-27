package com.fayroz.sitecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class Screen {
    HOME, NEW_PROJECT, PROJECT, SECTION, ADD_SPACE, EDIT_SPACE, QUICK_SPACE, QUICK_ITEM
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context=LocalContext.current
            val appearancePrefs=remember{
                context.getSharedPreferences("fayroz_appearance",MODE_PRIVATE)
            }
            var appearance by remember{
                mutableStateOf(
                    runCatching{
                        FayrozAppearance.valueOf(
                            appearancePrefs.getString("mode",FayrozAppearance.Dark.name)
                                ?: FayrozAppearance.Dark.name
                        )
                    }.getOrDefault(FayrozAppearance.Dark)
                )
            }
            fun setAppearance(mode:FayrozAppearance){
                appearance=mode
                appearancePrefs.edit().putString("mode",mode.name).apply()
            }

            FayrozSiteTheme(appearance){
                val store=remember{ProjectStore(context.applicationContext)}
                var projects by remember{mutableStateOf(store.load())}
                var screen by remember{mutableStateOf(Screen.HOME)}
                var selectedProjectId by remember{mutableStateOf<String?>(null)}
                var selectedSectionId by remember{mutableStateOf<String?>(null)}
                var selectedSpaceId by remember{mutableStateOf<String?>(null)}

                fun reload(){
                    projects=store.load()
                }
                fun save(){
                    store.save(projects)
                    reload()
                }

                val selectedProject=projects.firstOrNull{it.id==selectedProjectId}
                val selectedSection=selectedProject?.sections?.firstOrNull{it.id==selectedSectionId}
                val selectedSpace=selectedSection?.spaces?.firstOrNull{it.id==selectedSpaceId}

                when(screen){
                    Screen.HOME -> HomeScreen(
                        projects=projects,
                        appearance=appearance,
                        onAppearance=::setAppearance,
                        onNewProject={screen=Screen.NEW_PROJECT},
                        onOpenProject={id->
                            selectedProjectId=id
                            selectedSectionId=null
                            selectedSpaceId=null
                            screen=Screen.PROJECT
                        },
                        onSpace={screen=Screen.QUICK_SPACE},
                        onItem={screen=Screen.QUICK_ITEM}
                    )

                    Screen.NEW_PROJECT -> NewProjectScreen(
                        onCreate={name,type->
                            val firstSection=when(type){
                                "شقة" -> "الشقة"
                                "محل" -> "المحل"
                                "بيت","عمارة" -> "الدور الأرضي"
                                else -> "الجزء الأول"
                            }
                            val project=SiteProject(
                                id=UUID.randomUUID().toString(),
                                name=name,
                                type=type,
                                sections=mutableListOf(ProjectSection(name=firstSection))
                            )
                            projects.add(0,project)
                            save()
                            selectedProjectId=project.id
                            screen=Screen.PROJECT
                        },
                        onBack={screen=Screen.HOME}
                    )

                    Screen.PROJECT -> {
                        if(selectedProject==null){
                            screen=Screen.HOME
                        }else{
                            ProjectScreen(
                                project=selectedProject,
                                onOpenSection={id->
                                    selectedSectionId=id
                                    selectedSpaceId=null
                                    screen=Screen.SECTION
                                },
                                onAddSection={name->
                                    selectedProject.sections.add(ProjectSection(name=name))
                                    save()
                                },
                                onRenameProject={name->
                                    selectedProject.name=name
                                    save()
                                },
                                onDeleteProject={
                                    projects.removeAll{it.id==selectedProject.id}
                                    save()
                                    selectedProjectId=null
                                    selectedSectionId=null
                                    selectedSpaceId=null
                                    screen=Screen.HOME
                                },
                                onRenameSection={id,name->
                                    selectedProject.sections.firstOrNull{it.id==id}?.name=name
                                    save()
                                },
                                onDeleteSection={id->
                                    selectedProject.sections.removeAll{it.id==id}
                                    save()
                                },
                                onBack={screen=Screen.HOME}
                            )
                        }
                    }

                    Screen.SECTION -> {
                        if(selectedProject==null || selectedSection==null){
                            screen=Screen.PROJECT
                        }else{
                            SectionScreen(
                                projectName=selectedProject.name,
                                section=selectedSection,
                                onAddSpace={
                                    selectedSpaceId=null
                                    screen=Screen.ADD_SPACE
                                },
                                onEditSpace={id->
                                    selectedSpaceId=id
                                    screen=Screen.EDIT_SPACE
                                },
                                onCopySpace={id->
                                    val src=selectedSection.spaces.firstOrNull{it.id==id}
                                    if(src!=null){
                                        selectedSection.spaces.add(
                                            src.copy(
                                                id=UUID.randomUUID().toString(),
                                                name=src.name+" - نسخة"
                                            )
                                        )
                                        save()
                                    }
                                },
                                onDeleteSpace={id->
                                    selectedSection.spaces.removeAll{it.id==id}
                                    save()
                                },
                                onRenameSection={name->
                                    selectedSection.name=name
                                    save()
                                },
                                onBack={screen=Screen.PROJECT}
                            )
                        }
                    }

                    Screen.ADD_SPACE -> {
                        if(selectedProject==null || selectedSection==null){
                            screen=Screen.SECTION
                        }else{
                            Scaffold(
                                topBar={
                                    AppTopBar(
                                        title="إضافة غرفة",
                                        subtitle="${selectedProject.name} • ${selectedSection.name}",
                                        onBack={screen=Screen.SECTION}
                                    )
                                }
                            ){padding->
                                Box(Modifier.fillMaxSize().padding(padding)){
                                    SpaceCalculatorScreen(
                                        projectContext="${selectedProject.name} ← ${selectedSection.name}",
                                        existingSpaces=selectedSection.spaces,
                                        onSave={space->
                                            selectedSection.spaces.add(space)
                                            save()
                                            screen=Screen.SECTION
                                        },
                                        onSaveAndContinue={space->
                                            selectedSection.spaces.add(space)
                                            save()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Screen.EDIT_SPACE -> {
                        if(selectedProject==null || selectedSection==null || selectedSpace==null){
                            screen=Screen.SECTION
                        }else{
                            Scaffold(
                                topBar={
                                    AppTopBar(
                                        title="تعديل ${selectedSpace.name}",
                                        subtitle="${selectedProject.name} • ${selectedSection.name}",
                                        onBack={screen=Screen.SECTION}
                                    )
                                }
                            ){padding->
                                Box(Modifier.fillMaxSize().padding(padding)){
                                    SpaceCalculatorScreen(
                                        projectContext="${selectedProject.name} ← ${selectedSection.name}",
                                        initialSpace=selectedSpace,
                                        existingSpaces=selectedSection.spaces.filter{it.id!=selectedSpace.id},
                                        onSave={edited->
                                            val index=selectedSection.spaces.indexOfFirst{it.id==edited.id}
                                            if(index>=0) selectedSection.spaces[index]=edited
                                            save()
                                            screen=Screen.SECTION
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Screen.QUICK_SPACE -> Scaffold(
                        topBar={
                            AppTopBar(
                                title="حساب فراغ",
                                subtitle="حصر كامل بدون إنشاء مشروع",
                                onBack={screen=Screen.HOME}
                            )
                        }
                    ){padding->
                        Box(Modifier.fillMaxSize().padding(padding)){
                            SpaceCalculatorScreen()
                        }
                    }

                    Screen.QUICK_ITEM -> Scaffold(
                        topBar={
                            AppTopBar(
                                title="حساب بند",
                                subtitle="غرفة كاملة أو عدة حوائط أو كمية جاهزة",
                                onBack={screen=Screen.HOME}
                            )
                        }
                    ){padding->
                        Box(Modifier.fillMaxSize().padding(padding)){
                            ItemCalculatorScreen()
                        }
                    }
                }
            }
        }
    }
}

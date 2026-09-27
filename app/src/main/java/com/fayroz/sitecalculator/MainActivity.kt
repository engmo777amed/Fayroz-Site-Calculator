package com.fayroz.sitecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.ui.*
import java.util.UUID

private enum class Screen { HOME, SPACE, ITEM, NEW_PROJECT, PROJECT, SECTION, ADD_SPACE, EDIT_SPACE }

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
                            appearancePrefs.getString("mode",FayrozAppearance.System.name)
                                ?: FayrozAppearance.System.name
                        )
                    }.getOrDefault(FayrozAppearance.System)
                )
            }
            fun setAppearance(mode:FayrozAppearance){
                appearance=mode
                appearancePrefs.edit().putString("mode",mode.name).apply()
            }

            FayrozSiteTheme(appearance=appearance) {
                val store=remember{ProjectStore(context.applicationContext)}
                var projects by remember{mutableStateOf(store.load())}
                var screen by remember{mutableStateOf(Screen.HOME)}
                var selectedProjectId by remember{mutableStateOf<String?>(null)}
                var selectedSectionId by remember{mutableStateOf<String?>(null)}
                var selectedSpaceId by remember{mutableStateOf<String?>(null)}

                fun refresh(){projects=store.load()}
                fun save(){
                    store.save(projects)
                    refresh()
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
                        onSpace={screen=Screen.SPACE},
                        onItem={screen=Screen.ITEM}
                    )

                    Screen.NEW_PROJECT -> NewProjectScreen(
                        onCreate={name,type->
                            val sectionName=when(type){
                                "شقة"->"الشقة"
                                "محل"->"المحل"
                                "بيت","عمارة"->"الدور الأرضي"
                                else->"الجزء الأول"
                            }
                            val project=SiteProject(
                                name=name,
                                type=type,
                                sections=mutableListOf(ProjectSection(name=sectionName))
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
                            CalculatorScaffold(
                                title="إضافة غرفة",
                                subtitle="${selectedProject.name} • ${selectedSection.name}",
                                onBack={screen=Screen.SECTION}
                            ){
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

                    Screen.EDIT_SPACE -> {
                        if(selectedProject==null || selectedSection==null || selectedSpace==null){
                            screen=Screen.SECTION
                        }else{
                            CalculatorScaffold(
                                title="تعديل ${selectedSpace.name}",
                                subtitle="${selectedProject.name} • ${selectedSection.name}",
                                onBack={screen=Screen.SECTION}
                            ){
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

                    Screen.SPACE -> CalculatorScaffold(
                        title="حساب غرفة أو فراغ",
                        subtitle="أدخل المقاسات خطوة بخطوة",
                        onBack={screen=Screen.HOME}
                    ){ SpaceCalculatorScreen() }

                    Screen.ITEM -> CalculatorScaffold(
                        title="حساب بند",
                        subtitle="غرفة كاملة أو عدة حوائط أو كمية جاهزة",
                        onBack={screen=Screen.HOME}
                    ){ ItemCalculatorScreen() }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    projects:List<SiteProject>,
    appearance:FayrozAppearance,
    onAppearance:(FayrozAppearance)->Unit,
    onNewProject:()->Unit,
    onOpenProject:(String)->Unit,
    onSpace:()->Unit,
    onItem:()->Unit
){
    var search by remember{mutableStateOf("")}
    val q=search.trim()
    val filtered=if(q.isBlank()) projects else projects.filter{project->
        project.name.contains(q,true) ||
        project.type.contains(q,true) ||
        project.sections.any{section->
            section.name.contains(q,true) ||
            section.spaces.any{space->
                space.name.contains(q,true) ||
                space.type.contains(q,true) ||
                space.note.contains(q,true)
            }
        }
    }

    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=13.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{ BrandBanner() }

        item{
            AppearanceSelector(
                appearance=appearance,
                onAppearance=onAppearance
            )
        }

        item{
            SectionHeading(
                "مشروعات الموقع",
                "احفظ البيت أو الشقة ورتّب الحصر حسب الدور والغرفة."
            )
        }

        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Box(Modifier.weight(1f)){
                    TextFieldSimple(
                        "بحث",
                        search,
                        {search=it},
                        "ابحث باسم المشروع أو الدور أو الغرفة أو الملاحظة.",
                        "مثال: حمام رئيسي"
                    )
                }
                Button(
                    onClick=onNewProject,
                    modifier=Modifier.align(Alignment.Bottom).heightIn(min=48.dp),
                    shape=MaterialTheme.shapes.medium,
                    colors=ButtonDefaults.buttonColors(
                        containerColor=MaterialTheme.colorScheme.secondary,
                        contentColor=MaterialTheme.colorScheme.onSecondary
                    )
                ){
                    Icon(Icons.Rounded.Add,null)
                    Spacer(Modifier.width(5.dp))
                    Text("مشروع")
                }
            }
        }

        if(filtered.isEmpty()){
            item{
                SurfaceCard{
                    Text(
                        if(q.isBlank())"لا توجد مشروعات محفوظة. ابدأ مشروعًا جديدًا." else "لا توجد نتيجة مطابقة للبحث.",
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }else{
            item{
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    filtered.forEach{project->
                        Surface(
                            modifier=Modifier.fillMaxWidth(),
                            onClick={onOpenProject(project.id)},
                            shape=MaterialTheme.shapes.large,
                            color=MaterialTheme.colorScheme.surface,
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation=1.dp
                        ){
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=9.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ){
                                Icon(Icons.Rounded.Apartment,null,tint=MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)){
                                    Text(project.name,style=MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${project.type} • ${project.sections.sumOf{sec->sec.spaces.sumOf{it.repeatCount}}} غرفة/فراغ",
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("فتح",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }

        item{
            SectionHeading("حساب سريع","بدون إنشاء مشروع.")
        }

        item{
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ){
                Box(Modifier.weight(1f)){
                    ModeCard(
                        title="غرفة أو فراغ",
                        subtitle="حصر كامل",
                        icon=Icons.Rounded.Apartment,
                        onClick=onSpace,
                        accent=true,
                        compact=true
                    )
                }
                Box(Modifier.weight(1f)){
                    ModeCard(
                        title="بند فقط",
                        subtitle="كمية مباشرة",
                        icon=Icons.Rounded.Construction,
                        onClick=onItem,
                        compact=true
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorScaffold(
    title:String,
    subtitle:String,
    onBack:()->Unit,
    content:@Composable ()->Unit
){
    Scaffold(
        topBar={
            Surface(
                shadowElevation=3.dp,
                color=MaterialTheme.colorScheme.surface,
                border=BorderStroke(0.5.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    TextButton(onClick=onBack){Text("رجوع")}
                    Spacer(Modifier.width(2.dp))
                    Surface(
                        shape=MaterialTheme.shapes.small,
                        color=FayrozDeepNavy,
                        border=BorderStroke(1.dp,FayrozGold.copy(alpha=.45f))
                    ){
                        Box(
                            Modifier.size(34.dp).padding(4.dp),
                            contentAlignment=Alignment.Center
                        ){
                            FayrozMark(Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)){
                        Text(title,style=MaterialTheme.typography.titleMedium)
                        Text(
                            subtitle,
                            style=MaterialTheme.typography.labelSmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ){padding->
        Box(Modifier.fillMaxSize().padding(padding)){ content() }
    }
}

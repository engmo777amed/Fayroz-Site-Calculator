package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.R
import com.fayroz.sitecalculator.core.*

@Composable
fun HomeScreen(
    projects:List<Project>,
    active:ActiveLocation?,
    recentCalcs:List<SavedCalculation>,
    onContinue:()->Unit,
    onProjects:()->Unit,
    onToday:()->Unit,
    onTools:()->Unit,
    onNewRoom:()->Unit,
    onDirectItem:()->Unit,
    onSettings:()->Unit,
    onOpenSaved:(SavedCalculation)->Unit = {}
){
    val project=active?.let{a->projects.firstOrNull{it.id==a.projectId}}
    val section=project?.sections?.firstOrNull{it.id==active?.sectionId}
    val space=section?.spaces?.firstOrNull{it.id==active?.spaceId}

    androidx.compose.foundation.lazy.LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        item{
            Surface(shape=RoundedCornerShape(22.dp),color=FayrozNavy,shadowElevation=4.dp){
                Column(
                    Modifier.background(Brush.horizontalGradient(listOf(FayrozNavy,FayrozNavy2))).padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Image(painter=painterResource(R.drawable.ic_site_foreground),contentDescription=null,modifier=Modifier.size(46.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){
                            Text("FAYROZ SITE CALCULATOR",color=Color.White,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                            Text("V9 • شغل الموقع من غير لف كتير",color=FayrozTurquoise,style=MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick=onSettings){Icon(Icons.Rounded.Settings,"الإعدادات",tint=Color.White)}
                    }

                    if(project!=null){
                        Surface(onClick=onContinue,shape=RoundedCornerShape(15.dp),color=Color.White.copy(alpha=.07f)){
                            Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){
                                Column(Modifier.weight(1f)){
                                    Text("المشروع النشط",color=Color.White.copy(alpha=.65f),style=MaterialTheme.typography.labelSmall)
                                    Text(project.name,color=Color.White,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                    Text(
                                        when{
                                            space!=null->"${section?.name ?: ""} ← ${space.name}"
                                            section!=null->section.name
                                            else->"كمّل المشروع"
                                        },
                                        color=Color.White.copy(alpha=.72f),
                                        style=MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment=Alignment.CenterHorizontally){
                                    Text("كمّل",color=FayrozTurquoise,fontWeight=FontWeight.Black)
                                    Icon(Icons.Rounded.ChevronLeft,null,tint=FayrozTurquoise)
                                }
                            }
                        }
                    }else{
                        Text("اختار مشروع نشط مرة واحدة، وبعدها البرنامج هيفتكره.",color=Color.White.copy(alpha=.75f),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item{PageHeader("ابدأ بسرعة","الحاجات اللي بتستخدمها فعلاً في الموقع")}
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                HomeAction("حصر حسب الفراغ","غرفة أو صالة أو حمام",Icons.Rounded.MeetingRoom,onNewRoom,Modifier.weight(1f))
                HomeAction("حصر حسب البند","كمية جاهزة مباشرة",Icons.Rounded.Checklist,onDirectItem,Modifier.weight(1f))
            }
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                HomeAction("شغل اليوم","المفتوح وآخر الحسابات",Icons.Rounded.Today,onToday,Modifier.weight(1f))
                HomeAction("احسب خامات","من حصر أو مساحة جاهزة",Icons.Rounded.Calculate,onTools,Modifier.weight(1f))
            }
        }
        item{
            OutlinedButton(onClick=onProjects,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                Icon(Icons.Rounded.FolderOpen,null)
                Spacer(Modifier.width(5.dp))
                Text("المشروعات")
            }
        }

        if(recentCalcs.isNotEmpty()){
            item{PageHeader("آخر حساباتك")}
            items(recentCalcs.take(3).size){index->
                val calc=recentCalcs[index]
                Surface(onClick={onOpenSaved(calc)},shape=RoundedCornerShape(14.dp),tonalElevation=1.dp){
                    Column(Modifier.fillMaxWidth().padding(10.dp)){
                        Text(calc.title,fontWeight=FontWeight.Black)
                        Text(calc.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAction(
    title:String,
    subtitle:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit,
    modifier:Modifier=Modifier
){
    Surface(onClick=onClick,modifier=modifier.heightIn(min=104.dp),shape=RoundedCornerShape(18.dp),tonalElevation=1.dp){
        Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(7.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
    }
}

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.R
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.ProjectRepository

@Composable
fun HomeScreen(
    projects:List<SiteProject>,
    repository:ProjectRepository,
    onNewProject:()->Unit,
    onProjects:()->Unit,
    onQuickRoom:()->Unit,
    onToday:()->Unit,
    onTools:()->Unit,
    onSettings:()->Unit,
    onOpenLast:(String)->Unit,
    onTool:(String)->Unit
){
    val last=projects.maxByOrNull{it.updatedAt}
    val favorites=repository.getToolFavorites().mapNotNull(::toolMeta).take(2)

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        item{
            Surface(
                shape=RoundedCornerShape(22.dp),
                color=NavyDeep,
                shadowElevation=5.dp
            ){
                Column(
                    Modifier.background(Brush.horizontalGradient(listOf(NavyDeep,Navy,NavySoft)))
                        .padding(horizontal=14.dp,vertical=12.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Surface(
                            shape=RoundedCornerShape(14.dp),
                            color=Color.White.copy(alpha=.06f),
                            border=BorderStroke(1.dp,Turquoise.copy(alpha=.22f))
                        ){
                            Image(
                                painter=androidx.compose.ui.res.painterResource(R.drawable.ic_site_foreground),
                                contentDescription=null,
                                modifier=Modifier.size(46.dp).padding(3.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){
                            Text("FAYROZ SITE CALCULATOR",color=Color.White,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                            Text("شغل الموقع والحصر في مكان واحد",color=TurquoiseLight,style=MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick=onSettings,modifier=Modifier.size(44.dp)){
                            Icon(Icons.Rounded.Settings,"الإعدادات",tint=Color.White.copy(alpha=.9f))
                        }
                    }

                    if(last!=null){
                        val total=last.sections.sumOf{it.spaces.size}
                        val done=last.sections.sumOf{s->s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}}
                        val ratio=if(total==0)0f else done.toFloat()/total.toFloat()
                        Surface(
                            onClick={onOpenLast(last.id)},
                            shape=RoundedCornerShape(14.dp),
                            color=Color.White.copy(alpha=.06f)
                        ){
                            Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    Column(Modifier.weight(1f)){
                                        Text("كمّل آخر مشروع",color=Color.White.copy(alpha=.68f),style=MaterialTheme.typography.labelSmall)
                                        Text(last.name,color=Color.White,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                        Text(last.type+" • "+done+" / "+total+" مكان خلص",color=Color.White.copy(alpha=.68f),style=MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Rounded.ChevronLeft,null,tint=TurquoiseLight)
                                }
                                LinearProgressIndicator(
                                    progress={ratio},
                                    modifier=Modifier.fillMaxWidth(),
                                    color=Turquoise,
                                    trackColor=Color.White.copy(alpha=.10f)
                                )
                            }
                        }
                    }else{
                        Text("ابدأ أول مشروع أو استخدم الحصر السريع.",color=Color.White.copy(alpha=.72f),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item{SectionTitle("ابدأ بسرعة","أقصر طريق للشغل اللي بتعمله كل يوم.")}
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                HomeActionCard("مشروع جديد","شقة • بيت • عمارة",Icons.Rounded.AddBusiness,onNewProject,true,Modifier.weight(1f))
                HomeActionCard("شغل اليوم","كمّل اللي شغال عليه",Icons.Rounded.Today,onToday,false,Modifier.weight(1f))
            }
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                HomeActionCard("حصر سريع","غرفة كاملة من غير مشروع",Icons.Rounded.MeetingRoom,onQuickRoom,false,Modifier.weight(1f))
                HomeActionCard("احسب خامات","محارة • طوب • دهان...",Icons.Rounded.Calculate,onTools,false,Modifier.weight(1f))
            }
        }

        if(favorites.isNotEmpty()){
            item{SectionTitle("الحاسبات المفضلة","افتح أكتر أدواتك استخدامًا مباشرة.")}
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    favorites.forEach{tool->
                        OutlinedButton(
                            onClick={onTool(tool.id)},
                            modifier=Modifier.weight(1f).heightIn(min=48.dp),
                            contentPadding=PaddingValues(horizontal=8.dp,vertical=7.dp)
                        ){
                            Icon(tool.icon,null,Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(tool.title,maxLines=1)
                        }
                    }
                }
            }
        }

        item{
            OutlinedButton(onClick=onProjects,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                Icon(Icons.Rounded.FolderOpen,null)
                Spacer(Modifier.width(6.dp))
                Text("كل المشروعات")
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title:String,
    subtitle:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit,
    accent:Boolean,
    modifier:Modifier=Modifier
){
    val tone=if(accent)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(
        onClick=onClick,
        modifier=modifier.heightIn(min=104.dp),
        shape=RoundedCornerShape(18.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=1.dp
    ){
        Column(Modifier.padding(11.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Surface(shape=RoundedCornerShape(11.dp),color=tone.copy(alpha=.12f)){
                Icon(icon,null,Modifier.padding(8.dp).size(21.dp),tint=tone)
            }
            Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
    }
}

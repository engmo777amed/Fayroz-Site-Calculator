package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*

@Composable
fun HomeScreen(
    projects:List<SiteProject>,
    onNewProject:()->Unit,
    onProjects:()->Unit,
    onQuickRoom:()->Unit,
    onQuickItem:()->Unit,
    onToday:()->Unit,
    onOpenLast:(String)->Unit
){
    val spaces=projects.sumOf{p->p.sections.sumOf{s->s.spaces.sumOf{it.repeatCount}}}
    val done=projects.sumOf{p->p.sections.sumOf{s->s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}}}
    val last=projects.maxByOrNull{it.updatedAt}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{
            CompactBrandHeader(
                stats=listOf(
                    "مشروعات" to projects.size.toString(),
                    "فراغات" to spaces.toString(),
                    "تم الحصر" to done.toString()
                )
            )
        }

        item{SectionTitle("ابدأ من الموقع","أقصر طريق للحصر بدون قوائم طويلة.")}

        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                DashboardCard("مشروع جديد","ابدأ شقة أو بيت أو عمارة",Icons.Rounded.AddBusiness,onNewProject,true,Modifier.weight(1f))
                DashboardCard("حصر اليوم","كمّل الشغل الجاري بسرعة",Icons.Rounded.Today,onToday,false,Modifier.weight(1f))
            }
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                DashboardCard("حصر فراغ","غرفة كاملة + فتحات + بنود",Icons.Rounded.MeetingRoom,onQuickRoom,false,Modifier.weight(1f))
                DashboardCard("حصر بند","م² / م ط / م³ / عدد",Icons.Rounded.Calculate,onQuickItem,false,Modifier.weight(1f))
            }
        }

        if(last!=null){
            item{SectionTitle("آخر مشروع","فتح مباشر لمكان الشغل الأخير.")}
            item{
                val total=last.sections.sumOf{it.spaces.size}
                val reviewed=last.sections.sumOf{s->s.spaces.count{it.status==CaptureStatus.DONE||it.status==CaptureStatus.REVIEWED}}
                Surface(
                    onClick={onOpenLast(last.id)},
                    shape=RoundedCornerShape(16.dp),
                    color=MaterialTheme.colorScheme.surface,
                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.secondary.copy(alpha=.32f))
                ){
                    Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){
                        Surface(shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                            Icon(Icons.Rounded.Apartment,null,Modifier.padding(8.dp).size(20.dp),tint=MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)){
                            Text(last.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                            Text("${last.type} • $reviewed / $total غرفة مكتملة",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.secondary)
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

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

data class SpaceRef(val projectId:String,val sectionId:String,val spaceId:String,val project:String,val section:String,val space:SpaceEntry)

@Composable
fun TodayScreen(
    projects:List<SiteProject>,
    onOpenSpace:(SpaceRef)->Unit,
    onQuickRoom:()->Unit,
    onQuickItem:()->Unit
){
    val active=buildList{
        projects.forEach{p->p.sections.forEach{s->s.spaces.forEach{x->
            if(x.status==CaptureStatus.IN_PROGRESS)add(SpaceRef(p.id,s.id,x.id,p.name,s.name,x))
        }}}
    }.sortedByDescending{it.space.updatedAt}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{
            CompactBrandHeader(
                title="حصر اليوم",
                subtitle="كمّل العناصر الجاري حصرها أو افتح حصرًا سريعًا.",
                stats=listOf("جاري" to active.size.toString(),"مشروعات" to projects.size.toString(),"الوضع" to "موقع")
            )
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                DashboardCard("فراغ سريع","حصر كامل بدون مشروع",Icons.Rounded.MeetingRoom,onQuickRoom,true,Modifier.weight(1f))
                DashboardCard("بند سريع","كمية مباشرة أو هندسية",Icons.Rounded.Calculate,onQuickItem,false,Modifier.weight(1f))
            }
        }
        item{SectionTitle("الجاري حصره","الغرف بحالة «جاري» تظهر هنا تلقائيًا.")}
        if(active.isEmpty())item{EmptyBlock("لا يوجد حصر جاري","غيّر حالة أي غرفة إلى «جاري» أو ابدأ حصرًا سريعًا.",Icons.Rounded.Today)}
        else item{
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                active.forEach{ref->
                    Surface(
                        onClick={onOpenSpace(ref)},
                        shape=RoundedCornerShape(15.dp),
                        color=MaterialTheme.colorScheme.surface,
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.secondary.copy(alpha=.30f))
                    ){
                        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                                Icon(Icons.Rounded.EditNote,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(ref.space.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                Text("${ref.project} ← ${ref.section} • ${ref.space.takeoffs.size} بند",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

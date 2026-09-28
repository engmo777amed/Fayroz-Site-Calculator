package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
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

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{ScreenHeader("شغل اليوم",if(active.isEmpty())"مفيش حاجة شغال عليها دلوقتي" else "${active.size} مكان شغال عليه")}

        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TodayAction("حصر غرفة","من غير مشروع",Icons.Rounded.MeetingRoom,onQuickRoom,Modifier.weight(1f))
                TodayAction("حصر بند","كمية سريعة",Icons.Rounded.Calculate,onQuickItem,Modifier.weight(1f))
            }
        }

        item{SectionTitle("كمّل شغلك","أي مكان حالته «شغال عليها» هيظهر هنا.")}
        if(active.isEmpty()){
            item{
                EmptyBlock(
                    "مفيش شغل مفتوح",
                    "ابدأ حصر سريع أو افتح مشروع وخلي حالة المكان «شغال عليها».",
                    Icons.Rounded.Today,
                    "ابدأ حصر غرفة",
                    onQuickRoom
                )
            }
        }else{
            items(active.size){index->
                val ref=active[index]
                Surface(
                    onClick={onOpenSpace(ref)},
                    shape=RoundedCornerShape(15.dp),
                    color=MaterialTheme.colorScheme.surface,
                    tonalElevation=1.dp
                ){
                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                            Icon(Icons.Rounded.EditNote,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)){
                            Text(ref.space.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                            Text(
                                ref.project+" ← "+ref.section+" • "+ref.space.takeoffs.size+" بند",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.ChevronLeft,null,tint=MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayAction(
    title:String,
    subtitle:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    onClick:()->Unit,
    modifier:Modifier=Modifier
){
    Surface(onClick=onClick,modifier=modifier.heightIn(min=84.dp),shape=RoundedCornerShape(16.dp),tonalElevation=1.dp){
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Column{
                Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

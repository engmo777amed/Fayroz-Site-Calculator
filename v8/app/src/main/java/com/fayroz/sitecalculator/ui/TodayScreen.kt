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

data class WorkRef(
    val projectId:String,
    val sectionId:String,
    val spaceId:String,
    val projectName:String,
    val sectionName:String,
    val space:Space
)

@Composable
fun TodayScreen(
    projects:List<Project>,
    active:ActiveLocation?,
    recent:List<SavedCalculation>,
    onOpenSpace:(String,String,String)->Unit,
    onOpenCalc:(SavedCalculation)->Unit
){
    val activeItems=buildList{
        projects.forEach{p->
            p.sections.forEach{s->
                s.spaces.forEach{sp->
                    if(sp.status==WorkStatus.IN_PROGRESS){
                        add(WorkRef(p.id,s.id,sp.id,p.name,s.name,sp))
                    }
                }
            }
        }
    }.sortedByDescending{it.space.updatedAt}

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{PageHeader("الحصر الجاري","المفتوح وآخر الحسابات في مكان واحد")}

        val exact=active?.let{a->
            activeItems.firstOrNull{it.projectId==a.projectId&&it.sectionId==a.sectionId&&it.spaceId==a.spaceId}
        }
        if(exact!=null){
            item{
                Surface(
                    onClick={onOpenSpace(exact.projectId,exact.sectionId,exact.spaceId)},
                    shape=RoundedCornerShape(17.dp),
                    color=MaterialTheme.colorScheme.primaryContainer
                ){
                    Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Rounded.PlayCircle,null,tint=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)){
                            Text("كمّل من آخر مكان",fontWeight=FontWeight.Black)
                            Text("${exact.projectName} ← ${exact.sectionName} ← ${exact.space.name}",style=MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Rounded.ChevronLeft,null)
                    }
                }
            }
        }

        item{PageHeader("أماكن شغال عليها")}
        if(activeItems.isEmpty()){
            item{EmptyState("مفيش حصر مفتوح","أي مكان حالته «شغال عليها» هيظهر هنا.")}
        }else{
            items(activeItems.size){i->
                val ref=activeItems[i]
                Surface(
                    onClick={onOpenSpace(ref.projectId,ref.sectionId,ref.spaceId)},
                    shape=RoundedCornerShape(15.dp),
                    tonalElevation=1.dp
                ){
                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                            Icon(Icons.Rounded.EditNote,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)){
                            Text(ref.space.name,fontWeight=FontWeight.Black)
                            Text(
                                "${ref.projectName} ← ${ref.sectionName} • ${ref.space.takeoffs.size} بند"+if(ref.space.repeatCount>1)" • ×${ref.space.repeatCount}" else "",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.ChevronLeft,null)
                    }
                }
            }
        }

        if(recent.isNotEmpty()){
            item{PageHeader("آخر حسابات الخامات")}
            items(recent.take(5).size){i->
                val calc=recent[i]
                Surface(
                    onClick={onOpenCalc(calc)},
                    shape=RoundedCornerShape(14.dp),
                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)
                ){
                    Column(Modifier.fillMaxWidth().padding(9.dp)){
                        Text(calc.title,fontWeight=FontWeight.Black)
                        Text(calc.summary.substringBefore(" • "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
                    }
                }
            }
        }
    }
}


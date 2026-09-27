package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.SiteProject

@Composable
fun DashboardScreen(
    projects:List<SiteProject>,
    onNewProject:()->Unit,
    onRoomCalc:()->Unit,
    onItemCalc:()->Unit,
    onProjects:()->Unit,
    onOpenLast:(String)->Unit
){
    val roomCount=projects.sumOf{p->p.sections.sumOf{s->s.rooms.sumOf{it.repeatCount}}}
    val last=projects.firstOrNull()

    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{
            BrandHeader(
                title="FAYROZ SITE CALCULATOR",
                subtitle="حاسبة كميات الموقع",
                stats=listOf(
                    "مشروعات" to projects.size.toString(),
                    "غرف" to roomCount.toString(),
                    "الوضع" to "موقع"
                )
            )
        }
        item{SectionTitle("ابدأ الآن","أهم العمليات قدامك مباشرة بدون قوائم طويلة.")}
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                DashboardTile("مشروع جديد","ابدأ شقة أو بيت أو مشروع",Icons.Rounded.AddBusiness,onNewProject,true,Modifier.weight(1f))
                DashboardTile("حصر فراغ","غرفة كاملة بالفتحات والبنود",Icons.Rounded.MeetingRoom,onRoomCalc,false,Modifier.weight(1f))
            }
        }
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                DashboardTile("حصر بند","غرفة أو عدة حوائط أو كمية جاهزة",Icons.Rounded.Calculate,onItemCalc,false,Modifier.weight(1f))
                DashboardTile("المشروعات","بحث ومتابعة الحصر المحفوظ",Icons.Rounded.FolderOpen,onProjects,false,Modifier.weight(1f))
            }
        }
        if(last!=null){
            item{SectionTitle("آخر مشروع","ادخل مباشرة من غير ما تدور.")}
            item{
                Surface(
                    onClick={onOpenLast(last.id)},
                    shape=MaterialTheme.shapes.medium,
                    color=MaterialTheme.colorScheme.surface,
                    border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.secondary.copy(alpha=.35f))
                ){
                    Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically){
                        Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.secondaryContainer){
                            Icon(Icons.Rounded.Apartment,null,Modifier.padding(8.dp).size(21.dp),tint=MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){
                            Text(last.name,style=MaterialTheme.typography.titleMedium)
                            Text(
                                "${last.type} • ${last.sections.size} جزء • ${last.sections.sumOf{s->s.rooms.sumOf{it.repeatCount}}} غرفة/فراغ",
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

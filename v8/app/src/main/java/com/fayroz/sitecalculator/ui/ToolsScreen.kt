package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.SavedCalculation

data class ToolDef(
    val id:String,
    val title:String,
    val subtitle:String,
    val icon:ImageVector
)

val materialTools=listOf(
    ToolDef("plaster","خامات المحارة","أسمنت + رمل",Icons.Rounded.FormatPaint),
    ToolDef("splash","مونة الطرطشة","قبل المحارة",Icons.Rounded.Grain),
    ToolDef("screed","مونة تسوية الأرضيات","أسمنت + رمل",Icons.Rounded.Layers),
    ToolDef("masonry","مباني وطوب","طوب + مونة",Icons.Rounded.ViewModule),
    ToolDef("tile","البلاط والكراتين","مساحة شراء + عدد",Icons.Rounded.GridView),
    ToolDef("adhesive","لاصق السيراميك","كجم + شكاير",Icons.Rounded.Inventory2),
    ToolDef("paint","دهان","كمية حسب التغطية",Icons.Rounded.FormatColorFill),
    ToolDef("waterproof","عزل","كمية الخامة",Icons.Rounded.WaterDrop),
    ToolDef("gypsum","جبس بورد","ألواح + هالك",Icons.Rounded.Dashboard)
)

@Composable
fun ToolsScreen(
    recent:List<SavedCalculation>,
    hasActiveProject:Boolean,
    onOpen:(String)->Unit
){
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{
            PageHeader(
                "الحاسبات",
                if(hasActiveProject)"أي نتيجة تقدر تحفظها في المشروع النشط." else "اختار مشروع نشط لو عايز تحفظ النتائج داخله."
            )
        }

        if(recent.isNotEmpty()){
            item{PageHeader("حسابات اليوم")}
            items(recent.take(3).size){i->
                val r=recent[i]
                Surface(
                    onClick={onOpen(r.toolId)},
                    shape=RoundedCornerShape(14.dp),
                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)
                ){
                    Column(Modifier.fillMaxWidth().padding(9.dp)){
                        Text(r.title,fontWeight=FontWeight.Black)
                        Text(r.summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
                    }
                }
            }
        }

        item{PageHeader("خامات التنفيذ","الحاسبات الأساسية اللي مرتبطة بالحصر")}
        item{
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                materialTools.chunked(2).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        row.forEach{tool->
                            ToolCard(tool,{onOpen(tool.id)},Modifier.weight(1f))
                        }
                        if(row.size==1)Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(tool:ToolDef,onClick:()->Unit,modifier:Modifier=Modifier){
    Surface(
        onClick=onClick,
        modifier=modifier.heightIn(min=108.dp),
        shape=RoundedCornerShape(17.dp),
        tonalElevation=1.dp
    ){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(tool.icon,null,Modifier.padding(7.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Text(tool.title,fontWeight=FontWeight.Black,maxLines=2)
            Text(tool.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        }
    }
}

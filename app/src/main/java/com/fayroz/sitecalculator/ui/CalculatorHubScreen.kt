package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.ProjectRepository

data class SiteToolMeta(
    val id:String,
    val title:String,
    val subtitle:String,
    val category:String,
    val icon:ImageVector
)

val siteTools=listOf(
    SiteToolMeta("plaster_materials","خامات المحارة","أسمنت + رمل من مساحة المحارة","خامات التشطيبات",Icons.Rounded.FormatPaint),
    SiteToolMeta("splash_materials","مونة الطرطشة","أسمنت + رمل قبل المحارة","خامات التشطيبات",Icons.Rounded.Grain),
    SiteToolMeta("screed_materials","مونة تسوية الأرضيات","أسمنت + رمل حسب المساحة والسمك","خامات التشطيبات",Icons.Rounded.Layers),
    SiteToolMeta("tile_purchase","البلاط والكراتين","عدد البلاطات والكراتين مع الهالك","خامات التشطيبات",Icons.Rounded.GridView),
    SiteToolMeta("tile_adhesive","لاصق السيراميك","كمية اللاصق وعدد الشكاير","خامات التشطيبات",Icons.Rounded.Inventory2),
    SiteToolMeta("paint_materials","دهان ومعجون","دهان + برايمر + معجون","خامات التشطيبات",Icons.Rounded.FormatColorFill),
    SiteToolMeta("waterproof_materials","خامات العزل","عبوات أو رولات حسب نوع العزل","خامات التشطيبات",Icons.Rounded.WaterDrop),
    SiteToolMeta("gypsum_materials","خامات الجبس بورد","ألواح وقطاعات وخامات تقريبية","خامات التشطيبات",Icons.Rounded.ViewModule),

    SiteToolMeta("masonry_materials","مباني وطوب","عدد الطوب + مونة المباني","مباني وخرسانة",Icons.Rounded.Wallpaper),
    SiteToolMeta("concrete_materials","خرسانة بسيطة","أسمنت + رمل + سن + مياه تقريبية","مباني وخرسانة",Icons.Rounded.Foundation),

    SiteToolMeta("slope_levels","الميل والمناسيب","فرق المنسوب والميل ونقطة النهاية","أدوات الموقع",Icons.Rounded.TrendingDown),
    SiteToolMeta("irregular_area","مساحة غير منتظمة","اجمع أو اخصم كذا جزء","أدوات الموقع",Icons.Rounded.AspectRatio),
    SiteToolMeta("repetitions","تكرار الكميات","كمية × شقق × أدوار","أدوات الموقع",Icons.Rounded.ContentCopy),
    SiteToolMeta("progress_productivity","الإنجاز والإنتاجية","نسبة الإنجاز والمدة المتبقية","أدوات الموقع",Icons.Rounded.Speed),
    SiteToolMeta("unit_conversion","تحويل الوحدات","متر وسم ومم وم² ولتر","أدوات الموقع",Icons.Rounded.SwapHoriz)
)

fun toolMeta(id:String)=siteTools.firstOrNull{it.id==id}

private data class QuickCalcMeta(val title:String,val subtitle:String,val icon:ImageVector,val action:()->Unit)

@Composable
fun CalculatorHubScreen(
    repository:ProjectRepository,
    onRoom:()->Unit,
    onItem:()->Unit,
    onStair:()->Unit,
    onTool:(String)->Unit
){
    var favorites by remember{mutableStateOf(repository.getToolFavorites())}
    var recentTick by remember{mutableIntStateOf(0)}
    val recent=remember(favorites,recentTick){repository.getRecentTools()}
    var query by remember{mutableStateOf("")}
    val q=query.trim()
    val expanded=remember{mutableStateMapOf(
        "خامات التشطيبات" to true,
        "مباني وخرسانة" to false,
        "أدوات الموقع" to false
    )}

    val quick=listOf(
        QuickCalcMeta("حصر غرفة","غرفة كاملة + فتحات + بنود",Icons.Rounded.MeetingRoom,onRoom),
        QuickCalcMeta("حصر بند","م² / م ط / م³ / عدد",Icons.Rounded.Calculate,onItem),
        QuickCalcMeta("حساب سلم","نوايم + قوائم + بسطات",Icons.Rounded.Stairs,onStair)
    )
    val filtered=if(q.isBlank())siteTools else siteTools.filter{
        it.title.contains(q,true)||it.subtitle.contains(q,true)||it.category.contains(q,true)
    }
    val quickFiltered=if(q.isBlank())quick else quick.filter{
        it.title.contains(q,true)||it.subtitle.contains(q,true)||"حصر سريع".contains(q,true)
    }

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{ScreenHeader("الحاسبات","حصر وخامات وأدوات موقع سريعة")}

        item{
            TextFieldX("دوّر على حاسبة",query,{query=it},placeholder="مثال: محارة، طوب، ميل، سلم")
        }

        if(q.isBlank()){
            item{SectionTitle("حصر سريع")}
            item{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    CompactToolCard("حصر غرفة","غرفة كاملة",Icons.Rounded.MeetingRoom,onRoom,Modifier.weight(1f))
                    CompactToolCard("حصر بند","كمية أو أبعاد",Icons.Rounded.Calculate,onItem,Modifier.weight(1f))
                }
            }
            item{CompactToolCard("حساب سلم","نوايم + قوائم + بسطات + وزرات",Icons.Rounded.Stairs,onStair,Modifier.fillMaxWidth())}

            val favoriteTools=siteTools.filter{it.id in favorites}.take(2)
            if(favoriteTools.isNotEmpty()){
                item{
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text("المفضلة ★",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            favoriteTools.forEach{tool->
                                AssistChip(
                                    onClick={
                                        repository.recordToolUse(tool.id);recentTick++;onTool(tool.id)
                                    },
                                    label={Text(tool.title,maxLines=1)},
                                    leadingIcon={Icon(tool.icon,null,Modifier.size(16.dp))}
                                )
                            }
                        }
                    }
                }
            }

            val recentTools=recent.mapNotNull(::toolMeta).filter{it.id !in favoriteTools.map{x->x.id}}.take(2)
            if(recentTools.isNotEmpty()){
                item{
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text("آخر استخدام",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            recentTools.forEach{tool->
                                SuggestionChip(
                                    onClick={repository.recordToolUse(tool.id);recentTick++;onTool(tool.id)},
                                    label={Text(tool.title,maxLines=1)}
                                )
                            }
                        }
                    }
                }
            }
        }else if(quickFiltered.isNotEmpty()){
            item{SectionTitle("حصر سريع")}
            item{
                Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                    quickFiltered.forEach{tool->
                        CompactToolCard(tool.title,tool.subtitle,tool.icon,tool.action,Modifier.fillMaxWidth())
                    }
                }
            }
        }

        val groups=if(q.isBlank())listOf("خامات التشطيبات","مباني وخرسانة","أدوات الموقع") else filtered.map{it.category}.distinct()
        groups.forEach{category->
            val tools=filtered.filter{it.category==category}
            if(tools.isNotEmpty()){
                item{
                    Surface(
                        onClick={if(q.isBlank())expanded[category]=!(expanded[category]?:false)},
                        shape=RoundedCornerShape(14.dp),
                        color=MaterialTheme.colorScheme.surface,
                        tonalElevation=1.dp
                    ){
                        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text(category,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                                Text(
                                    when(category){
                                        "خامات التشطيبات"->"محارة، طرطشة، أرضيات، دهان، عزل وجبس"
                                        "مباني وخرسانة"->"طوب ومونة وخرسانة بسيطة"
                                        else->"مناسيب، مساحات، تكرار وإنتاجية"
                                    },
                                    style=MaterialTheme.typography.bodySmall,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if(q.isBlank())Icon(
                                if(expanded[category]==true)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null
                            )
                        }
                    }
                }
                if(q.isNotBlank() || expanded[category]==true){
                    item{
                        ToolGrid(
                            tools=tools,
                            favorites=favorites,
                            onFavorite={id->
                                repository.toggleToolFavorite(id)
                                favorites=repository.getToolFavorites()
                            },
                            onOpen={id->
                                repository.recordToolUse(id)
                                recentTick++
                                onTool(id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGrid(
    tools:List<SiteToolMeta>,
    favorites:Set<String>,
    onFavorite:(String)->Unit,
    onOpen:(String)->Unit
){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        tools.chunked(2).forEach{rowTools->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                rowTools.forEach{tool->
                    ToolCard(tool,tool.id in favorites,{onFavorite(tool.id)},{onOpen(tool.id)},Modifier.weight(1f))
                }
                if(rowTools.size==1)Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool:SiteToolMeta,
    favorite:Boolean,
    onFavorite:()->Unit,
    onClick:()->Unit,
    modifier:Modifier=Modifier
){
    Surface(
        onClick=onClick,
        modifier=modifier.heightIn(min=112.dp),
        shape=RoundedCornerShape(17.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=1.dp
    ){
        Box(Modifier.fillMaxSize()){
            Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                    Icon(tool.icon,null,Modifier.padding(7.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
                }
                Text(tool.title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(tool.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            IconButton(onClick=onFavorite,modifier=Modifier.align(Alignment.TopEnd).size(40.dp)){
                Icon(
                    if(favorite)Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if(favorite)"شيل من المفضلة" else "ضيف للمفضلة",
                    Modifier.size(18.dp),
                    tint=if(favorite)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactToolCard(
    title:String,
    subtitle:String,
    icon:ImageVector,
    onClick:()->Unit,
    modifier:Modifier=Modifier
){
    Surface(
        onClick=onClick,
        modifier=modifier.heightIn(min=82.dp),
        shape=RoundedCornerShape(16.dp),
        color=MaterialTheme.colorScheme.surface,
        tonalElevation=1.dp
    ){
        Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(7.dp).size(19.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)){
                Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=2)
                Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
            }
        }
    }
}

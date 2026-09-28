package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.ProjectRepository

@Composable
fun SiteToolCalculatorScreen(
    toolId:String,
    repository:ProjectRepository,
    seedArea:Double?=null,
    onBack:()->Unit,
    onOpenTool:(String,Double?)->Unit
){
    when(toolId){
        "plaster_materials"->MortarMaterialsScreen("plaster_materials","خامات المحارة","من مساحة المحارة لأسمنت ورمل",repository,seedArea,onBack,1.0,4.0,15.0,5.0,onOpenTool)
        "splash_materials"->MortarMaterialsScreen("splash_materials","مونة الطرطشة","خامات الطرطشة قبل المحارة",repository,seedArea,onBack,1.0,1.5,5.0,10.0,onOpenTool)
        "screed_materials"->MortarMaterialsScreen("screed_materials","مونة تسوية الأرضيات","أسمنت ورمل حسب المساحة ومتوسط السمك",repository,seedArea,onBack,1.0,4.0,50.0,5.0,onOpenTool)
        "masonry_materials"->MasonryMaterialsScreen(repository,seedArea,onBack)
        "concrete_materials"->ConcreteMaterialsScreen(repository,onBack)

        "tile_purchase"->TilePurchaseScreen(repository,seedArea,onBack)
        "tile_adhesive"->TileAdhesiveScreen(repository,seedArea,onBack)
        "paint_materials"->PaintMaterialsScreen(repository,seedArea,onBack)
        "waterproof_materials"->WaterproofMaterialsScreen(repository,seedArea,onBack)
        "gypsum_materials"->GypsumMaterialsScreen(repository,seedArea,onBack)

        "slope_levels"->SlopeLevelsScreen(repository,onBack)
        "irregular_area"->IrregularAreaScreen(onBack)
        "repetitions"->RepetitionScreen(repository,onBack)
        "progress_productivity"->ProgressProductivityScreen(repository,onBack)
        "unit_conversion"->UnitConversionScreen(repository,onBack)
        else->SimpleUnknownToolScreen(onBack)
    }
}

@Composable
fun ToolPage(
    title:String,
    subtitle:String,
    repository:ProjectRepository,
    toolId:String,
    onBack:()->Unit,
    content:@Composable ColumnScope.()->Unit
){
    var favorite by remember{mutableStateOf(toolId in repository.getToolFavorites())}
    Scaffold(
        topBar={
            AppBarX(title,subtitle,onBack,actions={
                IconButton(
                    onClick={
                        repository.toggleToolFavorite(toolId)
                        favorite=toolId in repository.getToolFavorites()
                    },
                    modifier=Modifier.size(48.dp)
                ){
                    Icon(
                        if(favorite)Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        if(favorite)"إزالة من المفضلة" else "إضافة للمفضلة",
                        tint=if(favorite)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                Column(verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
            }
        }
    }
}

@Composable
fun ToolResultCard(
    title:String="هتحتاج تقريبًا",
    rows:List<Pair<String,String>>,
    explanation:String,
    copyText:String,
    links:List<Pair<String,()->Unit>> = emptyList()
){
    var explain by remember{mutableStateOf(false)}
    val clipboard=LocalClipboardManager.current
    CardBox{
        SectionTitle(title,"الأرقام تقريبية وبتتغير حسب الخامة والتنفيذ.")
        rows.forEachIndexed{i,(label,value)->MetricRow(label,value,highlight=i==0)}
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={explain=true},modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                Text("اتحسبت إزاي؟")
            }
            OutlinedButton(
                onClick={clipboard.setText(AnnotatedString(copyText))},
                modifier=Modifier.weight(1f).heightIn(min=48.dp)
            ){
                Icon(Icons.Rounded.ContentCopy,null,Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("نسخ النتيجة")
            }
        }
        links.forEach{(label,action)->
            Button(onClick=action,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(label)}
        }
    }
    if(explain){
        AlertDialog(
            onDismissRequest={explain=false},
            title={Text("اتحسبت إزاي؟",fontWeight=FontWeight.Black)},
            text={Text(explanation)},
            confirmButton={TextButton(onClick={explain=false}){Text("تمام")}}
        )
    }
}

@Composable
fun QuickDetailedSwitch(detailed:Boolean,onChange:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        FilterChip(
            selected=!detailed,
            onClick={onChange(false)},
            label={Text("سريع")},
            modifier=Modifier.weight(1f)
        )
        FilterChip(
            selected=detailed,
            onClick={onChange(true)},
            label={Text("تفصيلي")},
            modifier=Modifier.weight(1f)
        )
    }
}

@Composable
fun RememberedNumber(
    repository:ProjectRepository,
    toolId:String,
    key:String,
    label:String,
    default:String,
    unit:String,
    help:String?=null,
    modifier:Modifier=Modifier
):MutableState<String>{
    val state=remember(toolId,key){mutableStateOf(repository.getToolValue("$toolId.$key",default))}
    NumberFieldX(
        label=label,
        value=state.value,
        onChange={
            state.value=it
            repository.setToolValue("$toolId.$key",it)
        },
        unit=unit,
        help=help,
        modifier=modifier
    )
    return state
}

@Composable
private fun SimpleUnknownToolScreen(onBack:()->Unit){
    Scaffold(topBar={AppBarX("الحاسبة","الأداة غير متاحة",onBack)}){
        Box(Modifier.fillMaxSize().padding(it).padding(12.dp)){
            EmptyBlock("الأداة غير متاحة","ارجع لشاشة الحاسبات واختار أداة تانية.")
        }
    }
}

package com.fayroz.sitecalculator.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.AppDefaults
import com.fayroz.sitecalculator.data.ProjectRepository
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun SettingsScreen(
    appearance:Appearance,
    onAppearance:(Appearance)->Unit,
    repository:ProjectRepository,
    onShareBackup:()->Unit,
    onImportBackup:(String)->Boolean
){
    val context=LocalContext.current
    val initial=remember{repository.getDefaults()}
    var height by remember{mutableStateOf(fmt(initial.defaultHeight))}
    var tile by remember{mutableStateOf(fmt(initial.defaultTileHeight))}
    var upstand by remember{mutableStateOf(fmt(initial.waterproofUpstand))}
    var floorWaste by remember{mutableStateOf(fmt(initial.floorWaste))}
    var tileWaste by remember{mutableStateOf(fmt(initial.wallTileWaste))}
    var gypsumWaste by remember{mutableStateOf(fmt(initial.gypsumWaste))}
    var skirtingWaste by remember{mutableStateOf(fmt(initial.skirtingWaste))}
    var favorites by remember{mutableStateOf(repository.getFavorites())}
    var importResult by remember{mutableStateOf<String?>(null)}
    var openSection by remember{mutableStateOf<String?>("شكل البرنامج")}

    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            val raw=runCatching{
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: ""
            }.getOrDefault("")
            val ok=raw.isNotBlank() && onImportBackup(raw)
            importResult=if(ok)"النسخة رجعت بنجاح." else "الملف مش نسخة احتياطية صالحة."
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{ScreenHeader("الإعدادات","شكل البرنامج والقيم اللي يبدأ بيها")}

        item{
            SettingsSectionHeader("شكل البرنامج",Icons.Rounded.Palette,openSection=="شكل البرنامج"){
                openSection=if(openSection=="شكل البرنامج")null else "شكل البرنامج"
            }
        }
        if(openSection=="شكل البرنامج")item{
            CardBox{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf(
                        Triple(Appearance.SYSTEM,"الهاتف",Icons.Rounded.SettingsBrightness),
                        Triple(Appearance.LIGHT,"فاتح",Icons.Rounded.LightMode),
                        Triple(Appearance.DARK,"داكن",Icons.Rounded.DarkMode)
                    ).forEach{(mode,label,icon)->
                        val active=appearance==mode
                        Surface(
                            modifier=Modifier.weight(1f),
                            onClick={onAppearance(mode)},
                            shape=RoundedCornerShape(12.dp),
                            color=if(active)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ){
                            Column(Modifier.padding(vertical=9.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                Icon(icon,null,tint=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(label,style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item{
            SettingsSectionHeader("قيم الحساب الأساسية",Icons.Rounded.Tune,openSection=="قيم الحساب الأساسية"){
                openSection=if(openSection=="قيم الحساب الأساسية")null else "قيم الحساب الأساسية"
            }
        }
        if(openSection=="قيم الحساب الأساسية")item{
            CardBox{
                Text("دي قيم بداية فقط، وكل حاسبة تقدر تغيرها لوحدها.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("ارتفاع الغرفة",height,{height=it},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("ارتفاع السيراميك",tile,{tile=it},"م",modifier=Modifier.weight(1f))
                }
                NumberFieldX("رجوع العزل",upstand,{upstand=it},"م")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("هالك أرضيات",floorWaste,{floorWaste=it},"%",modifier=Modifier.weight(1f))
                    NumberFieldX("هالك حوائط",tileWaste,{tileWaste=it},"%",modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("هالك جبس",gypsumWaste,{gypsumWaste=it},"%",modifier=Modifier.weight(1f))
                    NumberFieldX("هالك وزرات",skirtingWaste,{skirtingWaste=it},"%",modifier=Modifier.weight(1f))
                }
                Button(
                    onClick={
                        repository.saveDefaults(
                            AppDefaults(
                                defaultHeight=n(height),defaultTileHeight=n(tile),waterproofUpstand=n(upstand),
                                floorWaste=n(floorWaste),wallTileWaste=n(tileWaste),
                                gypsumWaste=n(gypsumWaste),skirtingWaste=n(skirtingWaste)
                            )
                        )
                    },
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
                ){Icon(Icons.Rounded.Save,null);Spacer(Modifier.width(5.dp));Text("حفظ القيم")}
            }
        }

        item{
            SettingsSectionHeader("بنود الحصر المفضلة",Icons.Rounded.Star,openSection=="بنود الحصر المفضلة"){
                openSection=if(openSection=="بنود الحصر المفضلة")null else "بنود الحصر المفضلة"
            }
        }
        if(openSection=="بنود الحصر المفضلة")item{
            CardBox{
                Text("دي مفضلة بنود الحصر جوه الغرفة، غير مفضلة الحاسبات.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                QuantityEngine.standardItems.forEach{item->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text(item,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked=item in favorites,
                            onCheckedChange={checked->
                                favorites=if(checked)favorites+item else favorites-item
                                repository.saveFavorites(favorites)
                            }
                        )
                    }
                }
            }
        }

        item{
            SettingsSectionHeader("النسخة الاحتياطية",Icons.Rounded.Backup,openSection=="النسخة الاحتياطية"){
                openSection=if(openSection=="النسخة الاحتياطية")null else "النسخة الاحتياطية"
            }
        }
        if(openSection=="النسخة الاحتياطية")item{
            CardBox{
                Text("احفظ نسخة من المشروعات والكميات، أو رجّع ملف محفوظ قبل كده.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    OutlinedButton(onClick=onShareBackup,modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                        Icon(Icons.Rounded.Share,null);Spacer(Modifier.width(4.dp));Text("احفظ نسخة")
                    }
                    OutlinedButton(
                        onClick={importLauncher.launch(arrayOf("application/json","text/plain","*/*"))},
                        modifier=Modifier.weight(1f).heightIn(min=48.dp)
                    ){
                        Icon(Icons.Rounded.Restore,null);Spacer(Modifier.width(4.dp));Text("رجّع نسخة")
                    }
                }
                importResult?.let{
                    Text(it,style=MaterialTheme.typography.bodySmall,color=if(it.contains("بنجاح"))MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,open:Boolean,onClick:()->Unit){
    Surface(onClick=onClick,shape=RoundedCornerShape(14.dp),tonalElevation=1.dp){
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){
                Icon(icon,null,Modifier.padding(7.dp).size(18.dp),tint=MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(8.dp))
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
            Icon(if(open)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null)
        }
    }
}

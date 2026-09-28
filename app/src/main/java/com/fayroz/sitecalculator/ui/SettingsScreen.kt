package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val initial=remember{repository.getDefaults()}
    var height by remember{mutableStateOf(fmt(initial.defaultHeight))}
    var tile by remember{mutableStateOf(fmt(initial.defaultTileHeight))}
    var upstand by remember{mutableStateOf(fmt(initial.waterproofUpstand))}
    var floorWaste by remember{mutableStateOf(fmt(initial.floorWaste))}
    var tileWaste by remember{mutableStateOf(fmt(initial.wallTileWaste))}
    var gypsumWaste by remember{mutableStateOf(fmt(initial.gypsumWaste))}
    var skirtingWaste by remember{mutableStateOf(fmt(initial.skirtingWaste))}
    var favorites by remember{mutableStateOf(repository.getFavorites())}
    var importDialog by remember{mutableStateOf(false)}
    var importText by remember{mutableStateOf("")}
    var importResult by remember{mutableStateOf<String?>(null)}

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{CompactBrandHeader("الإعدادات","القيم اللي البرنامج يبدأ بيها وشكل البرنامج.")}

        item{
            CardBox{
                SectionTitle("شكل البرنامج")
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
                            shape=MaterialTheme.shapes.small,
                            color=if(active)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border=androidx.compose.foundation.BorderStroke(1.dp,if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Column(Modifier.padding(vertical=9.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                Icon(icon,null,tint=if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp));Text(label,style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        item{
            CardBox{
                SectionTitle("القيم اللي البرنامج يبدأ بيها","تقدر تغير أي قيمة جوه الحاسبة أو البند بعد كده.")
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
                ){Icon(Icons.Rounded.Save,null);Spacer(Modifier.width(5.dp));Text("حفظ الافتراضات")}
            }
        }

        item{
            CardBox{
                SectionTitle("البنود اللي بستخدمها كتير","هتظهر لك في أول قائمة البنود.")
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
            CardBox{
                SectionTitle("نسخة احتياطية","تحتوي المشروعات والكميات.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    OutlinedButton(onClick=onShareBackup,modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                        Icon(Icons.Rounded.Share,null);Spacer(Modifier.width(4.dp));Text("طلّع نسخة")
                    }
                    OutlinedButton(onClick={importDialog=true},modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                        Icon(Icons.Rounded.Restore,null);Spacer(Modifier.width(4.dp));Text("رجّع نسخة")
                    }
                }
                importResult?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)}
            }
        }
    }

    if(importDialog){
        AlertDialog(
            onDismissRequest={importDialog=false},
            title={Text("رجّع نسخة محفوظة")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("الصق النسخة اللي حفظتها قبل كده هنا.",style=MaterialTheme.typography.bodySmall)
                    OutlinedTextField(importText,{importText=it},minLines=6,maxLines=10)
                }
            },
            confirmButton={TextButton(onClick={
                val ok=onImportBackup(importText)
                importResult=if(ok)"تم الاستيراد بنجاح." else "النسخة غير صالحة."
                if(ok)importDialog=false
            }){Text("استيراد")}},
            dismissButton={TextButton(onClick={importDialog=false}){Text("إلغاء")}}
        )
    }
}

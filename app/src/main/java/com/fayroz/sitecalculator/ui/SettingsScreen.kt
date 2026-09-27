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

@Composable
fun SettingsScreen(
    appearance:Appearance,
    onAppearance:(Appearance)->Unit
){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{BrandHeader("الإعدادات","هوية Fayroz موحدة على كل البرامج.")}
        item{
            CardBox{
                SectionTitle("شكل البرنامج","اختار الوضع المناسب لك.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    val modes=listOf(
                        Triple(Appearance.SYSTEM,"الهاتف",Icons.Rounded.SettingsBrightness),
                        Triple(Appearance.LIGHT,"فاتح",Icons.Rounded.LightMode),
                        Triple(Appearance.DARK,"داكن",Icons.Rounded.DarkMode)
                    )
                    modes.forEach{(mode,label,icon)->
                        val active=appearance==mode
                        Surface(
                            modifier=Modifier.weight(1f),
                            onClick={onAppearance(mode)},
                            shape=MaterialTheme.shapes.small,
                            color=if(active)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border=androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ){
                            Column(
                                Modifier.padding(vertical=9.dp,horizontal=4.dp),
                                horizontalAlignment=Alignment.CenterHorizontally
                            ){
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
            CardBox{
                SectionTitle("عن البرنامج")
                MetricRow("الإصدار","6.0.0")
                Text(
                    "Fayroz Site Calculator — حاسبة كميات الموقع للمشروعات والغرف والبنود.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

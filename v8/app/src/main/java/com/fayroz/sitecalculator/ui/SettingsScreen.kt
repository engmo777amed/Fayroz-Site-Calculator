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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.V8Repository

@Composable
fun SettingsScreen(
    repository:V8Repository,
    appearance:String,
    onAppearance:(String)->Unit,
    onBack:()->Unit,
    onReload:()->Unit
){
    val context=LocalContext.current
    var importMessage by remember{mutableStateOf<String?>(null)}
    val saveLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null){
            runCatching{
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(repository.exportBackup())}
            }
            importMessage="النسخة اتحفظت."
        }
    }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            val raw=runCatching{
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:""
            }.getOrDefault("")
            val ok=raw.isNotBlank()&&repository.importBackup(raw)
            importMessage=if(ok)"النسخة رجعت بنجاح." else "الملف مش نسخة Fayroz V8 صالحة."
            if(ok)onReload()
        }
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Text("الإعدادات",fontWeight=FontWeight.Black)},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}}
            )
        }
    ){padding->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)
        ){
            item{PageHeader("شكل البرنامج")}
            item{
                BoxCard{
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf(
                            Triple("system","الهاتف",Icons.Rounded.SettingsBrightness),
                            Triple("light","فاتح",Icons.Rounded.LightMode),
                            Triple("dark","داكن",Icons.Rounded.DarkMode)
                        ).forEach{(mode,label,icon)->
                            Surface(
                                onClick={onAppearance(mode)},
                                modifier=Modifier.weight(1f),
                                shape=RoundedCornerShape(13.dp),
                                color=if(appearance==mode)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ){
                                Column(Modifier.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                    Icon(icon,null)
                                    Spacer(Modifier.height(4.dp))
                                    Text(label,style=MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item{PageHeader("النسخة الاحتياطية")}
            item{
                BoxCard{
                    Text("احفظ المشروعات والحصر والحسابات في ملف، أو رجّع نسخة محفوظة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        OutlinedButton(
                            onClick={saveLauncher.launch("Fayroz-Site-V8-Backup.json")},
                            modifier=Modifier.weight(1f).heightIn(min=48.dp)
                        ){
                            Icon(Icons.Rounded.Share,null);Spacer(Modifier.width(4.dp));Text("احفظ نسخة")
                        }
                        OutlinedButton(
                            onClick={launcher.launch(arrayOf("application/json","text/plain","*/*"))},
                            modifier=Modifier.weight(1f).heightIn(min=48.dp)
                        ){
                            Icon(Icons.Rounded.Restore,null);Spacer(Modifier.width(4.dp));Text("رجّع نسخة")
                        }
                    }
                    importMessage?.let{
                        Text(it,style=MaterialTheme.typography.bodySmall,color=if(it.contains("بنجاح"))MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            }

            item{PageHeader("عن الحسابات")}
            item{
                BoxCard{
                    Text("قواعد V8",fontWeight=FontWeight.Black)
                    Text("• التكرار بيتطبق مرة واحدة فقط في التجميع.
• خصم الفتحات بيتم من الحوائط تلقائيًا.
• القيم الافتراضية في الخامات قيم بداية وقابلة للتعديل.
• مواصفة المشروع أو نشرة المنتج المعتمدة هي المرجع.",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

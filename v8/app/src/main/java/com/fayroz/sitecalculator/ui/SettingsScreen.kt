@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.fayroz.sitecalculator.data.BackupArchive

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
    var pending by remember{mutableStateOf<ByteArray?>(null)}
    val saveLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->
        if(uri!=null){
            importMessage=runCatching{
                context.contentResolver.openOutputStream(uri)?.use{BackupArchive.write(context,repository,it)}?:error("تعذر فتح الملف")
                "تم حفظ النسخة الاحتياطية والصور بنجاح."
            }.getOrElse{"فشل حفظ النسخة: ${it.message}"}
        }
    }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.openInputStream(uri)?.use{input->
                val output=java.io.ByteArrayOutputStream();val block=ByteArray(8192);var total=0
                while(true){val n=input.read(block);if(n<0)break;total+=n;require(total<=128*1024*1024);output.write(block,0,n)}
                output.toByteArray()
            }?:error("تعذر فتح الملف")}.onSuccess{pending=it}.onFailure{importMessage="تعذر قراءة النسخة أو حجمها أكبر من 128 ميجابايت."}
        }
    }
    pending?.let{bytes->AlertDialog(onDismissRequest={pending=null},title={Text("استرجاع نسخة احتياطية")},
        text={Text("دمج يضيف المشروعات والأماكن والحسابات غير الموجودة، ويحافظ على النسخة الحالية عند تكرارها. استبدال يرجّع بيانات النسخة مكان المشروعات الحالية.")},
        confirmButton={TextButton(onClick={val ok=BackupArchive.restore(context,repository,bytes,true);importMessage=if(ok)"تم الدمج بنجاح." else "النسخة غير صالحة؛ لم تُستبدل بياناتك.";pending=null;if(ok)onReload()}){Text("دمج")}},
        dismissButton={TextButton(onClick={val ok=BackupArchive.restore(context,repository,bytes,false);importMessage=if(ok)"تم الاستبدال بنجاح." else "النسخة غير صالحة؛ لم تُستبدل بياناتك.";pending=null;if(ok)onReload()}){Text("استبدال")}})}

    Scaffold(
        topBar={
            TopAppBar(
                title={Text("الإعدادات",fontWeight=FontWeight.Black)},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}}
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
                            onClick={saveLauncher.launch("Fayroz-Site-V9-Backup.zip")},
                            modifier=Modifier.weight(1f).heightIn(min=48.dp)
                        ){
                            Icon(Icons.Rounded.Share,null);Spacer(Modifier.width(4.dp));Text("احفظ نسخة")
                        }
                        OutlinedButton(
                            onClick={launcher.launch(arrayOf("application/zip","application/json","application/octet-stream"))},
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
                    Text("Fayroz Site Calculator 9.2.0",fontWeight=FontWeight.Black)
                    ExpandableSection("ملاحظات الحسابات"){
                    Text("""• التكرار بيتطبق مرة واحدة فقط في التجميع.
• صافي الحصر منفصل عن هالك الخامات والشراء.
• خصم الفتحات بيتم من الحوائط تلقائيًا.
• القيم الافتراضية في الخامات قيم بداية وقابلة للتعديل.
• مواصفة المشروع أو نشرة المنتج المعتمدة هي المرجع.""".trimIndent(),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


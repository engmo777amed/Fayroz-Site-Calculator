package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine

@Composable
fun SectionScreen(
    projectName:String,
    section:SectionEntry,
    onBack:()->Unit,
    onAddRoom:()->Unit,
    onEditRoom:(String)->Unit,
    onCopyRoom:(String)->Unit,
    onDeleteRoom:(String)->Unit,
    onRename:(String)->Unit,
    onDeleteSection:()->Unit
){
    var rename by remember{mutableStateOf(false)}
    var name by remember(section.id){mutableStateOf(section.name)}
    var deleteSection by remember{mutableStateOf(false)}
    var deleteRoomId by remember{mutableStateOf<String?>(null)}
    var summary by remember{mutableStateOf(false)}
    val totals=QuantityEngine.sectionSummary(section)

    Scaffold(
        topBar={
            AppBarX(section.name,projectName,onBack,actions={
                IconButton(onClick={summary=!summary},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Assessment,"الملخص")}
                var menu by remember{mutableStateOf(false)}
                Box{
                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        DropdownMenuItem(text={Text("تعديل الاسم")},leadingIcon={Icon(Icons.Rounded.Edit,null)},onClick={menu=false;rename=true})
                        DropdownMenuItem(text={Text("حذف الجزء",color=MaterialTheme.colorScheme.error)},leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},onClick={menu=false;deleteSection=true})
                    }
                }
            })
        },
        floatingActionButton={
            ExtendedFloatingActionButton(onClick=onAddRoom,icon={Icon(Icons.Rounded.Add,null)},text={Text("غرفة / فراغ")})
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(start=12.dp,end=12.dp,top=10.dp,bottom=88.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            if(summary)item{
                CardBox{
                    SectionTitle("ملخص ${section.name}","إجمالي الكميات داخل الجزء.")
                    if(totals.isEmpty())Text("لا توجد كميات بعد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    else totals.forEach{(key,value)->MetricRow(key.first,"${fmt(value)} ${key.second.label}")}
                }
            }

            item{SectionTitle("الغرف والفراغات","الحالة تظهر قبل فتح الغرفة، والإجراءات من ⋮.")}

            if(section.spaces.isEmpty()){
                item{EmptyBlock("لا توجد غرف","أضف أول غرفة أو فراغ للبدء.",Icons.Rounded.MeetingRoom)}
            }else item{
                Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                    section.spaces.forEach{space->
                        var menu by remember(space.id){mutableStateOf(false)}
                        Surface(
                            onClick={onEditRoom.bind(space.id)},
                            shape=RoundedCornerShape(15.dp),
                            color=MaterialTheme.colorScheme.surface,
                            border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                        ){
                            Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                                Surface(
                                    shape=RoundedCornerShape(10.dp),
                                    color=when(space.status){
                                        CaptureStatus.REVIEWED->MaterialTheme.colorScheme.tertiaryContainer
                                        CaptureStatus.DONE->MaterialTheme.colorScheme.primaryContainer
                                        CaptureStatus.IN_PROGRESS->MaterialTheme.colorScheme.secondaryContainer
                                        else->MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ){
                                    Icon(
                                        when(space.status){
                                            CaptureStatus.REVIEWED->Icons.Rounded.Verified
                                            CaptureStatus.DONE->Icons.Rounded.CheckCircle
                                            CaptureStatus.IN_PROGRESS->Icons.Rounded.EditNote
                                            else->Icons.Rounded.RadioButtonUnchecked
                                        },
                                        null,Modifier.padding(7.dp).size(18.dp),
                                        tint=when(space.status){
                                            CaptureStatus.REVIEWED->MaterialTheme.colorScheme.tertiary
                                            CaptureStatus.DONE->MaterialTheme.colorScheme.primary
                                            CaptureStatus.IN_PROGRESS->MaterialTheme.colorScheme.secondary
                                            else->MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)){
                                    Text(space.name,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(
                                        "${space.type} • ${space.status.label} • ${space.takeoffs.size} بند",
                                        style=MaterialTheme.typography.bodySmall,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines=1
                                    )
                                    if(space.note.isNotBlank())Text(space.note,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                                }
                                if(space.repeatCount>1){
                                    Surface(shape=RoundedCornerShape(999.dp),color=MaterialTheme.colorScheme.primaryContainer){
                                        Text("×${space.repeatCount}",Modifier.padding(horizontal=7.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Box{
                                    IconButton(onClick={menu=true},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.MoreVert,"إجراءات")}
                                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                                        DropdownMenuItem(text={Text("تعديل")},leadingIcon={Icon(Icons.Rounded.Edit,null)},onClick={menu=false;onEditRoom(space.id)})
                                        DropdownMenuItem(text={Text("نسخ")},leadingIcon={Icon(Icons.Rounded.ContentCopy,null)},onClick={menu=false;onCopyRoom(space.id)})
                                        DropdownMenuItem(text={Text("حذف",color=MaterialTheme.colorScheme.error)},leadingIcon={Icon(Icons.Rounded.Delete,null,tint=MaterialTheme.colorScheme.error)},onClick={menu=false;deleteRoomId=space.id})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if(rename)AlertDialog(
        onDismissRequest={rename=false},title={Text("تعديل الاسم")},
        text={OutlinedTextField(name,{name=it},singleLine=true)},
        confirmButton={TextButton(onClick={if(name.isNotBlank()){onRename(name.trim());rename=false}}){Text("حفظ")}},
        dismissButton={TextButton(onClick={rename=false}){Text("إلغاء")}}
    )
    if(deleteSection)AlertDialog(
        onDismissRequest={deleteSection=false},title={Text("حذف الجزء؟")},
        text={Text("سيتم حذف كل الغرف والكميات الموجودة داخله.")},
        confirmButton={TextButton(onClick={deleteSection=false;onDeleteSection()}){Text("حذف")}},
        dismissButton={TextButton(onClick={deleteSection=false}){Text("إلغاء")}}
    )
    if(deleteRoomId!=null)AlertDialog(
        onDismissRequest={deleteRoomId=null},title={Text("حذف الغرفة؟")},
        text={Text("سيتم حذف المقاسات والفتحات والبنود المسجلة لهذه الغرفة.")},
        confirmButton={TextButton(onClick={deleteRoomId?.let(onDeleteRoom);deleteRoomId=null}){Text("حذف")}},
        dismissButton={TextButton(onClick={deleteRoomId=null}){Text("إلغاء")}}
    )
}

private fun ((String)->Unit).bind(value:String):()->Unit={this(value)}

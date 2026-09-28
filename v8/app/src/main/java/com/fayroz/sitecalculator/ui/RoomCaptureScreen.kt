package com.fayroz.sitecalculator.ui

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
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.Catalog
import com.fayroz.sitecalculator.domain.QuantityEngine
import java.util.UUID

@Composable
fun RoomCaptureScreen(
    title:String,
    draftKey:String,
    repository:V8Repository,
    initial:Space?,
    onBack:()->Unit,
    onSave:(Space)->Unit
){
    val restored=remember(draftKey,initial?.id){initial ?: repository.loadDraft(draftKey)}
    val stableId=remember(draftKey,initial?.id){initial?.id ?: restored?.id ?: UUID.randomUUID().toString()}

    var step by remember{mutableIntStateOf(0)}
    var name by remember{mutableStateOf(restored?.name ?: "")}
    var type by remember{mutableStateOf(restored?.type ?: "غرفة نوم")}
    var dimUnit by remember{mutableStateOf("م")}
    var length by remember{mutableStateOf(restored?.length?.takeIf{it>0}?.let(::fmt) ?: "")}
    var width by remember{mutableStateOf(restored?.width?.takeIf{it>0}?.let(::fmt) ?: "")}
    var height by remember{mutableStateOf(restored?.height?.let(::fmt) ?: "3")}
    var repeat by remember{mutableStateOf((restored?.repeatCount ?: 1).toString())}
    var note by remember{mutableStateOf(restored?.note ?: "")}
    var status by remember{mutableStateOf(restored?.status ?: WorkStatus.IN_PROGRESS)}
    var advanced by remember{mutableStateOf(restored?.walls?.isNotEmpty()==true)}

    val walls=remember{mutableStateListOf<WallPart>().apply{addAll(restored?.walls ?: emptyList())}}
    val openings=remember{mutableStateListOf<Opening>().apply{addAll(restored?.openings ?: emptyList())}}
    val takeoffs=remember{
        mutableStateListOf<Takeoff>().apply{
            if(restored!=null)addAll(restored.takeoffs)
            else Catalog.items.filter{it.name in Catalog.suggested("غرفة نوم")}.forEach{add(it.create())}
        }
    }

    fun current():Space=Space(
        id=stableId,
        name=name.trim().ifBlank{type},
        type=type,
        length=lengthMeters(length,dimUnit),
        width=lengthMeters(width,dimUnit),
        height=lengthMeters(height,dimUnit).takeIf{it>0}?:3.0,
        repeatCount=n(repeat).toInt().coerceAtLeast(1),
        walls=if(advanced)walls.toList() else emptyList(),
        openings=openings.toList(),
        takeoffs=takeoffs.toList(),
        note=note.trim(),
        status=status,
        updatedAt=System.currentTimeMillis()
    )

    LaunchedEffect(
        name,type,dimUnit,length,width,height,repeat,note,status,advanced,
        walls.toList(),openings.toList(),takeoffs.toList()
    ){
        repository.saveDraft(draftKey,current())
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text(title,fontWeight=FontWeight.Black)
                    Text(listOf("المكان والمقاسات","الأبواب والشبابيك","البنود والنتيجة")[step],style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}}
            )
        },
        bottomBar={
            Surface(tonalElevation=4.dp){
                Column(Modifier.fillMaxWidth().imePadding().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    LinearProgressIndicator(progress={(step+1)/3f},modifier=Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        if(step>0)OutlinedButton(onClick={step--},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("السابق")}
                        if(step<2)Button(onClick={step++},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("التالي")}
                        else Button(
                            onClick={
                                repository.clearDraft(draftKey)
                                onSave(current().copy(status=if(status==WorkStatus.NOT_STARTED)WorkStatus.IN_PROGRESS else status))
                            },
                            modifier=Modifier.weight(1f).heightIn(min=48.dp)
                        ){
                            Icon(Icons.Rounded.Save,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("حفظ الحصر")
                        }
                    }
                }
            }
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            when(step){
                0->item{
                    BoxCard{
                        TextFieldX("اسم المكان",name,{name=it},placeholder="مثال: حمام رئيسي")
                        ChoiceFieldX("نوع المكان",type,Catalog.roomTypes,{newType->
                            val oldSuggested=Catalog.suggested(type)
                            val userChanged=takeoffs.any{it.name !in oldSuggested}
                            type=newType
                            if(initial==null && !userChanged){
                                takeoffs.clear()
                                Catalog.items.filter{it.name in Catalog.suggested(newType)}.forEach{takeoffs.add(it.create())}
                            }
                        })
                        ChoiceFieldX(
                            "وحدة المقاسات",dimUnit,listOf("م","سم","مم"),{newUnit->
                                if(newUnit!=dimUnit){
                                    length=convertLength(length,dimUnit,newUnit)
                                    width=convertLength(width,dimUnit,newUnit)
                                    height=convertLength(height,dimUnit,newUnit)
                                    dimUnit=newUnit
                                }
                            },
                            help="لما تغير الوحدة، البرنامج بيحوّل الأرقام تلقائيًا."
                        )
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            NumberFieldX("الطول",length,{length=it},dimUnit,Modifier.weight(1f))
                            NumberFieldX("العرض",width,{width=it},dimUnit,Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            NumberFieldX("الارتفاع",height,{height=it},dimUnit,Modifier.weight(1f))
                            NumberFieldX("عدد التكرارات",repeat,{repeat=it},"مرة",Modifier.weight(1f),
                                help="لو نفس المكان متكرر 12 مرة، اكتب 12. التكرار بيتحسب مرة واحدة فقط في ملخص المشروع.")
                        }
                        ChoiceFieldX("وصلت لفين؟",status.label,WorkStatus.entries.map{it.label},{label->
                            status=WorkStatus.entries.first{it.label==label}
                        })
                        TextFieldX("ملاحظة",note,{note=it},placeholder="اختياري")

                        HorizontalDivider()
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("المكان مش مستطيل؟",fontWeight=FontWeight.Black)
                                Text("فعّلها بس لو محتاج تدخل الحوائط واحد واحد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked=advanced,onCheckedChange={
                                advanced=it
                                if(it&&walls.isEmpty())walls.add(WallPart(name="حائط 1",height=lengthMeters(height,dimUnit).takeIf{x->x>0}?:3.0))
                            })
                        }

                        if(advanced){
                            walls.forEachIndexed{i,w->
                                var wl by remember(w.id,dimUnit){mutableStateOf(fmt(w.length/(lengthUnits.first{it.label==dimUnit}.meters)))}
                                var wh by remember(w.id,dimUnit){mutableStateOf(fmt(w.height/(lengthUnits.first{it.label==dimUnit}.meters)))}
                                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text("حائط ${i+1}",Modifier.weight(1f),fontWeight=FontWeight.Black)
                                            if(walls.size>1)IconButton(onClick={walls.removeAt(i)},modifier=Modifier.size(40.dp)){Icon(Icons.Rounded.Delete,"حذف")}
                                        }
                                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                            NumberFieldX("الطول",wl,{v->wl=v;walls[i]=w.copy(length=lengthMeters(v,dimUnit))},dimUnit,Modifier.weight(1f))
                                            NumberFieldX("الارتفاع",wh,{v->wh=v;walls[i]=w.copy(height=lengthMeters(v,dimUnit))},dimUnit,Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick={walls.add(WallPart(name="حائط ${walls.size+1}",height=lengthMeters(height,dimUnit).takeIf{it>0}?:3.0))},
                                modifier=Modifier.fillMaxWidth()
                            ){Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("ضيف حائط")}
                        }
                    }
                }

                1->item{
                    BoxCard{
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("الأبواب والشبابيك",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                                Text("البرنامج بيخصم الفتحات من الحوائط تلقائيًا.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HelpDot("خصم الفتحات","المحارة والدهان بيخصموا مساحة الباب أو الشباك. سيراميك الحائط بيخصم فقط الجزء اللي داخل ارتفاع السيراميك.")
                        }

                        if(openings.isEmpty())Text("مفيش فتحات مضافة.",style=MaterialTheme.typography.bodySmall)

                        openings.forEachIndexed{i,o->
                            OpeningRow(
                                index=i,
                                opening=o,
                                onChange={openings[i]=it},
                                onDelete={openings.removeAt(i)}
                            )
                        }

                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(
                                onClick={openings.add(Opening(kind=OpeningKind.DOOR,width=.90,height=2.10))},
                                modifier=Modifier.weight(1f)
                            ){Icon(Icons.Rounded.DoorFront,null);Spacer(Modifier.width(4.dp));Text("باب")}
                            OutlinedButton(
                                onClick={openings.add(Opening(kind=OpeningKind.WINDOW,width=1.20,height=1.20,sill=.90))},
                                modifier=Modifier.weight(1f)
                            ){Icon(Icons.Rounded.Window,null);Spacer(Modifier.width(4.dp));Text("شباك")}
                        }
                    }
                }

                else->{
                    item{
                        BoxCard{
                            Text("اختار البنود",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
                            Text("الاقتراحات حسب نوع المكان، وتقدر تضيف أو تشيل أي بند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Catalog.items.forEach{def->
                                val selected=takeoffs.any{it.name==def.name}
                                FilterChip(
                                    selected=selected,
                                    onClick={
                                        if(selected)takeoffs.removeAll{it.name==def.name}
                                        else takeoffs.add(def.create())
                                    },
                                    label={Text(def.name)},
                                    leadingIcon={if(selected){{Icon(Icons.Rounded.Check,null,Modifier.size(16.dp))}}else null}
                                )
                            }
                        }
                    }

                    if(takeoffs.isNotEmpty()){
                        item{PageHeader("النتيجة","المكان الواحد أولًا، والتكرار ظاهر لو موجود")}
                        item{
                            val space=current()
                            BoxCard{
                                takeoffs.forEachIndexed{i,item->
                                    val q=QuantityEngine.calculateOne(space,item)
                                    Column(verticalArrangement=Arrangement.spacedBy(3.dp)){
                                        MetricRow(
                                            item.name,
                                            if(space.repeatCount>1)"${fmt(q.repeatedFinal)} ${item.unit.label}" else "${fmt(q.oneSpaceFinal)} ${item.unit.label}",
                                            i==0
                                        )
                                        if(space.repeatCount>1){
                                            Text(
                                                "المكان الواحد ${fmt(q.oneSpaceFinal)} × ${space.repeatCount}",
                                                style=MaterialTheme.typography.labelSmall,
                                                color=MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpeningRow(
    index:Int,
    opening:Opening,
    onChange:(Opening)->Unit,
    onDelete:()->Unit
){
    var width by remember(opening.id){mutableStateOf(if(opening.width>0)fmt(opening.width) else "")}
    var height by remember(opening.id){mutableStateOf(if(opening.height>0)fmt(opening.height) else "")}
    var sill by remember(opening.id){mutableStateOf(fmt(opening.sill))}
    var count by remember(opening.id){mutableStateOf(opening.count.toString())}

    Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
        Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("${opening.kind.label} ${index+1}",Modifier.weight(1f),fontWeight=FontWeight.Black)
                Text("${fmt(opening.width*opening.height*opening.count)} م²",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
                IconButton(onClick=onDelete,modifier=Modifier.size(40.dp)){Icon(Icons.Rounded.Delete,"حذف")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("العرض",width,{width=it;onChange(opening.copy(width=n(it)))},"م",Modifier.weight(1f))
                NumberFieldX("الارتفاع",height,{height=it;onChange(opening.copy(height=n(it)))},"م",Modifier.weight(1f))
            }
            if(opening.kind==OpeningKind.WINDOW){
                NumberFieldX("جلسة الشباك",sill,{sill=it;onChange(opening.copy(sill=n(it)))},"م",help="من الأرض لأسفل الشباك.")
            }
            NumberFieldX("العدد",count,{count=it;onChange(opening.copy(count=n(it).toInt().coerceAtLeast(1)))},"عدد")
        }
    }
}

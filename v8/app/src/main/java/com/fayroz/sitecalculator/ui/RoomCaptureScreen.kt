@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
    var geometryMode by remember{
        mutableStateOf(
            when{
                restored?.walls?.isNotEmpty()==true -> "أكتر من حائط ورا بعض"
                restored?.floorSurfaces?.isNotEmpty()==true -> "قسم المساحة لكذا جزء"
                else -> "مستطيل بسيط"
            }
        )
    }
    var ceilingDetails by remember{mutableStateOf(restored?.ceilingSurfaces?.isNotEmpty()==true)}
    var editIndex by remember{mutableStateOf<Int?>(null)}

    val walls=remember{mutableStateListOf<WallPart>().apply{addAll(restored?.walls ?: emptyList())}}
    val floors=remember{mutableStateListOf<SurfacePart>().apply{addAll(restored?.floorSurfaces ?: emptyList())}}
    val ceilings=remember{mutableStateListOf<SurfacePart>().apply{addAll(restored?.ceilingSurfaces ?: emptyList())}}
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
        walls=if(geometryMode=="أكتر من حائط ورا بعض")walls.toList() else emptyList(),
        openings=openings.toList(),
        takeoffs=takeoffs.toList(),
        note=note.trim(),
        status=status,
        updatedAt=System.currentTimeMillis(),
        floorSurfaces=if(geometryMode=="قسم المساحة لكذا جزء")floors.toList() else emptyList(),
        ceilingSurfaces=if(ceilingDetails)ceilings.toList() else emptyList()
    )

    LaunchedEffect(
        name,type,dimUnit,length,width,height,repeat,note,status,geometryMode,ceilingDetails,
        walls.toList(),floors.toList(),ceilings.toList(),openings.toList(),takeoffs.toList()
    ){
        repository.saveDraft(draftKey,current())
    }

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{
                    Text(title,fontWeight=FontWeight.Black)
                    Text(listOf("المكان والمقاسات","الشكل والفتحات","البنود والنتيجة")[step],style=MaterialTheme.typography.labelSmall)
                }},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowForward,"رجوع")}}
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
                        ChoiceFieldX(
                            "هتحسب المكان إزاي؟",
                            geometryMode,
                            listOf("مستطيل بسيط","أكتر من حائط ورا بعض","قسم المساحة لكذا جزء"),
                            {geometryMode=it},
                            help="للصالة أو الشكل غير المنتظم استخدم الحوائط المتتالية أو قسم المساحة لأجزاء."
                        )

                        if(geometryMode=="أكتر من حائط ورا بعض"){
                            if(walls.isEmpty())walls.add(WallPart(name="حائط 1",height=lengthMeters(height,dimUnit).takeIf{it>0}?:3.0))
                            walls.forEachIndexed{i,w->
                                var wl by remember(w.id,dimUnit){mutableStateOf(if(w.length>0)fmt(w.length/(lengthUnits.first{it.label==dimUnit}.meters)) else "")}
                                var wh by remember(w.id,dimUnit){mutableStateOf(fmt(w.height/(lengthUnits.first{it.label==dimUnit}.meters)))}
                                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text(w.name,Modifier.weight(1f),fontWeight=FontWeight.Black)
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

                        if(geometryMode=="قسم المساحة لكذا جزء"){
                            if(floors.isEmpty())floors.add(SurfacePart(name="مسطح 1"))
                            Text("مسطحات الأرضية",fontWeight=FontWeight.Black)
                            floors.forEachIndexed{i,s->
                                SurfacePartRow(i,s,dimUnit,{floors[i]=it},{if(floors.size>1)floors.removeAt(i)})
                            }
                            OutlinedButton(onClick={floors.add(SurfacePart(name="مسطح ${floors.size+1}"))},modifier=Modifier.fillMaxWidth()){
                                Text("ضيف مسطح أرضية")
                            }
                        }

                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("السقف مختلف عن الأرضية؟",fontWeight=FontWeight.Black)
                                Text("فعّلها لو السقف متقسم أو فيه خصومات مختلفة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(ceilingDetails,{ceilingDetails=it})
                        }
                        if(ceilingDetails){
                            if(ceilings.isEmpty())ceilings.add(SurfacePart(name="سقف 1"))
                            ceilings.forEachIndexed{i,s->
                                SurfacePartRow(i,s,dimUnit,{ceilings[i]=it},{if(ceilings.size>1)ceilings.removeAt(i)})
                            }
                            OutlinedButton(onClick={ceilings.add(SurfacePart(name="سقف ${ceilings.size+1}"))},modifier=Modifier.fillMaxWidth()){
                                Text("ضيف مسطح سقف")
                            }
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
                                walls=if(geometryMode=="أكتر من حائط ورا بعض")walls else emptyList(),
                                onChange={openings[i]=it},
                                onDelete={openings.removeAt(i)}
                            )
                        }

                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(
                                onClick={openings.add(Opening(kind=OpeningKind.DOOR,width=.90,height=2.10,wallId=walls.firstOrNull()?.id))},
                                modifier=Modifier.weight(1f)
                            ){Icon(Icons.Rounded.DoorFront,null);Spacer(Modifier.width(4.dp));Text("باب")}
                            OutlinedButton(
                                onClick={openings.add(Opening(kind=OpeningKind.WINDOW,width=1.20,height=1.20,sill=.90,wallId=walls.firstOrNull()?.id))},
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
                                    leadingIcon=if(selected){{ Icon(Icons.Rounded.Check,null,Modifier.size(16.dp)) }}else null
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
                                    Surface(
                                        shape=RoundedCornerShape(12.dp),
                                        color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.28f)
                                    ){
                                        Column(Modifier.fillMaxWidth().padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                            Row(verticalAlignment=Alignment.CenterVertically){
                                                Column(Modifier.weight(1f)){
                                                    Text(item.name,fontWeight=FontWeight.Black)
                                                    Text(item.kind.label+" • هالك "+fmt(item.waste)+"%",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text("${fmt(q.repeatedFinal)} ${item.unit.label}",fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.primary)
                                                IconButton(onClick={editIndex=i}){Icon(Icons.Rounded.Tune,"ضبط")}
                                            }
                                            if(space.repeatCount>1){
                                                Text("المكان الواحد ${fmt(q.oneSpaceFinal)} × ${space.repeatCount}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                                TextButton(onClick={editIndex=i}){Text("ضبط / اتحسبت إزاي؟")}
                                                TextButton(onClick={takeoffs.removeAt(i)}){Text("حذف",color=MaterialTheme.colorScheme.error)}
                                            }
                                            QuantityEngine.purchaseInfo(space,item)?.let{purchase->
                                                Text("شراء تقريبي: ${purchase.pieces} قطعة"+(purchase.packs?.let{" • $it كرتونة"}?:""),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.tertiary)
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

    editIndex?.let{i->
        if(i in takeoffs.indices){
            TakeoffEditorDialog(
                item=takeoffs[i],
                walls=if(geometryMode=="أكتر من حائط ورا بعض")walls else emptyList(),
                space=current(),
                onDismiss={editIndex=null},
                onSave={takeoffs[i]=it;editIndex=null}
            )
        }
    }
}

@Composable
private fun SurfacePartRow(
    index:Int,
    surface:SurfacePart,
    unit:String,
    onChange:(SurfacePart)->Unit,
    onDelete:()->Unit
){
    var l by remember(surface.id,unit){mutableStateOf(if(surface.length>0)fmt(surface.length/(lengthUnits.first{it.label==unit}.meters)) else "")}
    var w by remember(surface.id,unit){mutableStateOf(if(surface.width>0)fmt(surface.width/(lengthUnits.first{it.label==unit}.meters)) else "")}
    var d by remember(surface.id){mutableStateOf(if(surface.deductionArea>0)fmt(surface.deductionArea) else "")}
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
        Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(surface.name,Modifier.weight(1f),fontWeight=FontWeight.Black)
                IconButton(onClick=onDelete,modifier=Modifier.size(40.dp)){Icon(Icons.Rounded.Delete,"حذف")}
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("الطول",l,{l=it;onChange(surface.copy(length=lengthMeters(it,unit)))},unit,Modifier.weight(1f))
                NumberFieldX("العرض",w,{w=it;onChange(surface.copy(width=lengthMeters(it,unit)))},unit,Modifier.weight(1f))
            }
            NumberFieldX("خصومات المسطح",d,{d=it;onChange(surface.copy(deductionArea=n(it)))},"م²",help="مثال: عمود أو منور أو جزء مش هيتنفذ.")
        }
    }
}

@Composable
private fun OpeningRow(
    index:Int,
    opening:Opening,
    walls:List<WallPart>,
    onChange:(Opening)->Unit,
    onDelete:()->Unit
){
    var width by remember(opening.id){mutableStateOf(if(opening.width>0)fmt(opening.width) else "")}
    var height by remember(opening.id){mutableStateOf(if(opening.height>0)fmt(opening.height) else "")}
    var sill by remember(opening.id){mutableStateOf(fmt(opening.sill))}
    var reveal by remember(opening.id){mutableStateOf(if(opening.revealDepth>0)fmt(opening.revealDepth) else "")}
    var count by remember(opening.id){mutableStateOf(opening.count.toString())}

    Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
        Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("${opening.kind.label} ${index+1}",Modifier.weight(1f),fontWeight=FontWeight.Black)
                Text("${fmt(opening.width*opening.height*opening.count)} م²",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Black)
                IconButton(onClick=onDelete,modifier=Modifier.size(40.dp)){Icon(Icons.Rounded.Delete,"حذف")}
            }
            if(walls.isNotEmpty()){
                ChoiceFieldX(
                    "الحائط",
                    walls.firstOrNull{it.id==opening.wallId}?.name ?: walls.first().name,
                    walls.map{it.name},
                    {label->onChange(opening.copy(wallId=walls.first{it.name==label}.id))},
                    help="اربط الفتحة بالحائط الصحيح."
                )
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("العرض",width,{width=it;onChange(opening.copy(width=n(it)))},"م",Modifier.weight(1f))
                NumberFieldX("الارتفاع",height,{height=it;onChange(opening.copy(height=n(it)))},"م",Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("جلسة الشباك",sill,{sill=it;onChange(opening.copy(sill=n(it)))},"م",Modifier.weight(1f),help="من الأرض لأسفل الفتحة.")
                NumberFieldX("عمق الجنب",reveal,{reveal=it;onChange(opening.copy(revealDepth=n(it)))},"م",Modifier.weight(1f),help="لو هتحسب جوانب الباب أو الشباك.")
            }
            NumberFieldX("العدد",count,{count=it;onChange(opening.copy(count=n(it).toInt().coerceAtLeast(1)))},"عدد")
        }
    }
}

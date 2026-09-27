package com.fayroz.sitecalculator.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.ProjectRepository
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.domain.Templates
import java.util.UUID

private val roomTypes=listOf("غرفة نوم","ريسبشن","حمام","مطبخ","ممر","بلكونة","مخزن","واجهة","سلم","فراغ آخر")

@Composable
fun RoomEditorScreen(
    repository:ProjectRepository,
    draftKey:String,
    title:String,
    initial:SpaceEntry?=null,
    existing:List<SpaceEntry> = emptyList(),
    onBack:()->Unit,
    onSave:((SpaceEntry)->Unit)?=null,
    onSaveAndContinue:((SpaceEntry)->Unit)?=null,
    onReset:(()->Unit)?=null
){
    val defaults=remember{repository.getDefaults()}
    val restored=remember(draftKey,initial?.id){initial ?: repository.loadDraft(draftKey)}
    val seed=restored
    val stableSpaceId=remember(draftKey,initial?.id){initial?.id ?: seed?.id ?: UUID.randomUUID().toString()}

    var page by remember{mutableIntStateOf(0)}
    var type by remember{mutableStateOf(seed?.type ?: "غرفة نوم")}
    var autoName by remember{mutableStateOf(seed==null)}
    var name by remember{mutableStateOf(seed?.name ?: nextRoomName(type,existing))}
    var length by remember{mutableStateOf(seed?.length?.let(::fmt) ?: "")}
    var width by remember{mutableStateOf(seed?.width?.let(::fmt) ?: "")}
    var height by remember{mutableStateOf(seed?.height?.let(::fmt) ?: fmt(defaults.defaultHeight))}
    var repeatCount by remember{mutableStateOf(seed?.repeatCount?.toString() ?: "1")}
    var note by remember{mutableStateOf(seed?.note ?: "")}
    var status by remember{mutableStateOf(seed?.status ?: CaptureStatus.IN_PROGRESS)}
    var geometryMode by remember{
        mutableStateOf(
            when{
                seed?.walls?.isNotEmpty()==true -> "حوائط متتابعة"
                seed?.floorSurfaces?.isNotEmpty()==true -> "مسطحات متعددة"
                else -> "مستطيل بسيط"
            }
        )
    }

    val walls=remember{mutableStateListOf<WallSegment>().apply{addAll(seed?.walls ?: emptyList())}}
    val floors=remember{mutableStateListOf<SurfaceSegment>().apply{addAll(seed?.floorSurfaces ?: emptyList())}}
    val ceilings=remember{mutableStateListOf<SurfaceSegment>().apply{addAll(seed?.ceilingSurfaces ?: emptyList())}}
    val openings=remember{mutableStateListOf<Opening>().apply{addAll(seed?.openings ?: emptyList())}}
    val takeoffs=remember{mutableStateListOf<TakeoffItem>().apply{addAll(seed?.takeoffs ?: emptyList())}}
    val photos=remember{mutableStateListOf<String>().apply{addAll(seed?.photoUris ?: emptyList())}}

    var editItemIndex by remember{mutableStateOf<Int?>(null)}
    var customDialog by remember{mutableStateOf(false)}
    var addStandardOpen by remember{mutableStateOf(false)}
    var applyTemplateConfirm by remember{mutableStateOf(false)}

    fun currentSpace():SpaceEntry=SpaceEntry(
        id=stableSpaceId,
        name=name.trim().ifBlank{type},
        type=type,
        length=n(length),width=n(width),height=n(height),
        repeatCount=n(repeatCount).toInt().coerceAtLeast(1),
        walls=if(geometryMode=="حوائط متتابعة")walls.toList() else emptyList(),
        floorSurfaces=if(geometryMode=="مسطحات متعددة")floors.toList() else emptyList(),
        ceilingSurfaces=ceilings.toList(),
        openings=openings.toList(),
        takeoffs=takeoffs.toList(),
        note=note.trim(),
        photoUris=photos.toList(),
        status=status,
        updatedAt=System.currentTimeMillis()
    )

    LaunchedEffect(
        name,type,length,width,height,repeatCount,note,status,geometryMode,
        walls.toList(),floors.toList(),ceilings.toList(),openings.toList(),takeoffs.toList(),photos.toList()
    ){
        repository.saveDraft(draftKey,currentSpace())
    }

    val context=LocalContext.current
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            runCatching{context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            if(uri.toString() !in photos)photos.add(uri.toString())
        }
    }

    val space=currentSpace()
    val warnings=buildList{
        if(space.height<=0)add("ارتفاع الفراغ غير صحيح.")
        if(geometryMode=="مستطيل بسيط" && (space.length<=0||space.width<=0))add("أدخل طول وعرض الفراغ.")
        if(geometryMode=="حوائط متتابعة" && walls.none{it.length>0})add("أضف أطوال الحوائط.")
        if(geometryMode=="مسطحات متعددة" && floors.none{it.length>0&&it.width>0})add("أضف مسطح أرضية واحد على الأقل.")
        openings.forEachIndexed{i,o->
            if(o.width<=0||o.height<=0)add("مقاس الفتحة ${i+1} غير مكتمل.")
            if(o.type==OpeningType.WINDOW && o.sillHeight<0)add("جلسة الشباك ${i+1} غير صحيحة.")
        }
        if(takeoffs.isEmpty())add("لم يتم اختيار أي بند حصر.")
    }

    Scaffold(
        topBar={AppBarX(title,"${page+1} من 6 • حفظ تلقائي",onBack)},
        bottomBar={
            Surface(tonalElevation=4.dp){
                Row(
                    Modifier.fillMaxWidth().imePadding().padding(10.dp),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ){
                    if(page>0)OutlinedButton(onClick={page--},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("السابق")}
                    if(page<5){
                        Button(onClick={page++},modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                            Text(listOf("الهندسة","الفتحات","البنود","مراجع","النتيجة")[page])
                            Spacer(Modifier.width(4.dp));Icon(Icons.Rounded.ChevronLeft,null)
                        }
                    }else{
                        if(onSave!=null){
                            Button(
                                onClick={repository.clearDraft(draftKey);onSave(space)},
                                enabled=warnings.none{it.contains("غير")||it.contains("أدخل")},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp)
                            ){
                                Icon(Icons.Rounded.Save,null);Spacer(Modifier.width(4.dp));Text(if(initial==null)"حفظ" else "حفظ التعديل")
                            }
                            if(onSaveAndContinue!=null && initial==null){
                                OutlinedButton(
                                    onClick={repository.clearDraft(draftKey);onSaveAndContinue(space);onReset?.invoke()},
                                    enabled=warnings.none{it.contains("غير")||it.contains("أدخل")},
                                    modifier=Modifier.weight(1f).heightIn(min=48.dp)
                                ){Text("حفظ + غرفة")}
                            }
                        }else{
                            Button(onClick={repository.clearDraft(draftKey);onReset?.invoke()},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("حساب جديد")}
                        }
                    }
                }
            }
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                LinearProgressIndicator(
                    progress={(page+1)/6f},
                    modifier=Modifier.fillMaxWidth(),
                    color=MaterialTheme.colorScheme.secondary
                )
            }

            item{
                when(page){
                    0 -> BasicsCard(
                        name=name,onName={name=it;autoName=false},
                        type=type,onType={type=it;if(autoName)name=nextRoomName(it,existing)},
                        length=length,onLength={length=it},
                        width=width,onWidth={width=it},
                        height=height,onHeight={height=it},
                        repeatCount=repeatCount,onRepeat={repeatCount=it},
                        note=note,onNote={note=it},
                        status=status,onStatus={status=it}
                    )

                    1 -> GeometryCard(
                        mode=geometryMode,onMode={geometryMode=it},
                        walls=walls,floors=floors,ceilings=ceilings,
                        defaultHeight=n(height).takeIf{it>0}?:defaults.defaultHeight
                    )

                    2 -> OpeningsCard(openings,if(geometryMode=="حوائط متتابعة")walls else emptyList())

                    3 -> CardBox{
                        SectionTitle(
                            "بنود الحصر",
                            "الهالك والـOverride والتعديلات محفوظة لكل بند لوحده.",
                            trailing={
                                Row{
                                    TextButton(onClick={applyTemplateConfirm=true}){Text("قالب")}
                                    Box{
                                        IconButton(onClick={addStandardOpen=true},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Add,"إضافة بند")}
                                        DropdownMenu(expanded=addStandardOpen,onDismissRequest={addStandardOpen=false}){
                                            val favorites=repository.getFavorites()
                                            val ordered=QuantityEngine.standardItems.sortedBy{if(it in favorites)0 else 1}
                                            ordered.forEach{itemName->
                                                DropdownMenuItem(
                                                    text={Text(if(itemName in favorites)"★ $itemName" else itemName)},
                                                    onClick={
                                                        addStandardOpen=false
                                                        if(takeoffs.none{it.name==itemName})takeoffs.add(Templates.defaultForName(itemName,defaults))
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text={Text("بند مخصص")},
                                                leadingIcon={Icon(Icons.Rounded.AddCircle,null)},
                                                onClick={addStandardOpen=false;customDialog=true}
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        if(takeoffs.isEmpty())Text("أضف بندًا أو طبّق قالب نوع الغرفة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        takeoffs.forEachIndexed{i,item->
                            val q=QuantityEngine.calculate(space.copy(takeoffs=takeoffs.toList()),item)
                            Surface(
                                shape=RoundedCornerShape(13.dp),
                                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f),
                                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                            ){
                                Row(Modifier.fillMaxWidth().padding(start=9.dp,end=3.dp,top=6.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically){
                                    Column(Modifier.weight(1f)){
                                        Text(item.name,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                                        Text("${item.method.label} • هالك ${fmt(item.wastePercent)}%",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${fmt(q.final)} ${item.unit.label}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                    IconButton(onClick={editItemIndex=i},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Tune,"ضبط")}
                                    IconButton(onClick={takeoffs.removeAt(i)},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف",tint=MaterialTheme.colorScheme.error)}
                                }
                            }
                        }
                    }

                    4 -> CardBox{
                        SectionTitle("مراجع وحالة الحصر","صور الموقع والملاحظات لا تدخل في الحساب.")
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                            OutlinedButton(onClick={photoPicker.launch(arrayOf("image/*"))},modifier=Modifier.weight(1f).heightIn(min=48.dp)){
                                Icon(Icons.Rounded.AddAPhoto,null);Spacer(Modifier.width(4.dp));Text("إضافة صورة")
                            }
                            Surface(Modifier.weight(1f),shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.surfaceVariant){
                                Box(Modifier.fillMaxWidth().padding(12.dp),contentAlignment=Alignment.Center){
                                    Text("${photos.size} صورة",style=MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        if(photos.isNotEmpty())TextButton(onClick={photos.clear()}){Text("إزالة كل الصور",color=MaterialTheme.colorScheme.error)}
                        ChoiceFieldX("حالة الحصر",status.label,CaptureStatus.entries.map{it.label},{label->
                            status=CaptureStatus.entries.first{it.label==label}
                        },"استخدم «تمت المراجعة» بعد مراجعة الكميات.")
                        TextFieldX("ملاحظات تنفيذية",note,{note=it},"ملاحظة تحفظ مع الغرفة.","مثال: استبعاد خلف وحدات المطبخ")
                    }

                    else -> ReviewCard(space,warnings)
                }
            }
        }
    }

    editItemIndex?.let{i->
        TakeoffEditorDialog(item=takeoffs[i],walls=space.walls,onDismiss={editItemIndex=null},onSave={takeoffs[i]=it;editItemIndex=null})
    }

    if(customDialog)CustomItemDialog(onDismiss={customDialog=false},onAdd={takeoffs.add(it);customDialog=false})

    if(applyTemplateConfirm)AlertDialog(
        onDismissRequest={applyTemplateConfirm=false},
        title={Text("تطبيق قالب $type؟")},
        text={Text("سيتم إضافة البنود الافتراضية غير الموجودة فقط، ولن يتم حذف تعديلاتك الحالية.")},
        confirmButton={TextButton(onClick={
            Templates.forRoom(type,defaults).forEach{preset->if(takeoffs.none{it.name==preset.name})takeoffs.add(preset)}
            applyTemplateConfirm=false
        }){Text("تطبيق")}},
        dismissButton={TextButton(onClick={applyTemplateConfirm=false}){Text("إلغاء")}}
    )
}

private fun nextRoomName(type:String,existing:List<SpaceEntry>):String{
    val count=existing.count{it.type==type}+1
    return if(count==1)type else "$type $count"
}

@Composable
private fun BasicsCard(
    name:String,onName:(String)->Unit,type:String,onType:(String)->Unit,
    length:String,onLength:(String)->Unit,width:String,onWidth:(String)->Unit,
    height:String,onHeight:(String)->Unit,repeatCount:String,onRepeat:(String)->Unit,
    note:String,onNote:(String)->Unit,status:CaptureStatus,onStatus:(CaptureStatus)->Unit
){
    CardBox{
        SectionTitle("بيانات الفراغ","المستطيل البسيط اختصار فقط؛ الأشكال غير المنتظمة في الخطوة التالية.")
        TextFieldX("اسم الغرفة أو الفراغ",name,onName,"اسم واضح في المشروع.","مثال: صالة رئيسية")
        ChoiceFieldX("نوع الفراغ",type,roomTypes,onType,"يستخدم في القوالب والبحث.")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            NumberFieldX("الطول",length,onLength,"م","للمستطيل البسيط.",Modifier.weight(1f))
            NumberFieldX("العرض",width,onWidth,"م","للمستطيل البسيط.",Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            NumberFieldX("الارتفاع",height,onHeight,"م","ارتفاع افتراضي للحوائط.",Modifier.weight(1f))
            NumberFieldX("التكرار",repeatCount,onRepeat,"عدد","مثال غرفة نموذجية ×6.",Modifier.weight(1f))
        }
        ChoiceFieldX("حالة الحصر",status.label,CaptureStatus.entries.map{it.label},{label->onStatus(CaptureStatus.entries.first{it.label==label})})
        TextFieldX("ملاحظة",note,onNote,"مرجع للموقع.","مثال: يوجد عمود 30×60")
    }
}

@Composable
private fun GeometryCard(
    mode:String,onMode:(String)->Unit,
    walls:MutableList<WallSegment>,floors:MutableList<SurfaceSegment>,ceilings:MutableList<SurfaceSegment>,
    defaultHeight:Double
){
    CardBox{
        SectionTitle("شكل الفراغ","اختار الطريقة الأقرب للموقع، بدون إجبار الفراغ على مستطيل.")
        ChoiceFieldX("طريقة القياس",mode,listOf("مستطيل بسيط","حوائط متتابعة","مسطحات متعددة"),onMode,"الصالة L-Shape استخدم حوائط متتابعة أو مسطحات متعددة.")
        when(mode){
            "مستطيل بسيط" -> Text("سيتم استخدام الطول × العرض، ومحيط 2×(الطول+العرض).",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            "حوائط متتابعة" -> {
                SegmentWallsEditor(walls,defaultHeight)
                OutlinedButton(onClick={walls.add(WallSegment(name="حائط ${walls.size+1}",height=defaultHeight))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                    Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("إضافة حائط")
                }
            }
            else -> {
                Text("الأرضيات",style=MaterialTheme.typography.labelLarge)
                SurfacesEditor(floors,"مسطح")
                OutlinedButton(onClick={floors.add(SurfaceSegment(name="مسطح ${floors.size+1}"))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("إضافة مسطح أرضية")}
            }
        }
        HorizontalDivider()
        Text("مسطحات سقف خاصة - اختياري",style=MaterialTheme.typography.labelLarge)
        Text("اتركها فارغة لو السقف مطابق للأرضية. أضفها للجبس الساقط أو تقسيمات السقف.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        SurfacesEditor(ceilings,"سقف")
        OutlinedButton(onClick={ceilings.add(SurfaceSegment(name="سقف ${ceilings.size+1}"))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("إضافة مسطح سقف")}
    }
}

@Composable
private fun SegmentWallsEditor(walls:MutableList<WallSegment>,defaultHeight:Double){
    walls.forEachIndexed{i,w->
        var name by remember(w.id){mutableStateOf(w.name)}
        var length by remember(w.id){mutableStateOf(if(w.length==0.0)"" else fmt(w.length))}
        var height by remember(w.id){mutableStateOf(fmt(w.height))}
        Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    TextFieldX("الاسم",name,{name=it;walls[i]=walls[i].copy(name=it)},placeholder="مثال: حائط التلفزيون",modifier=Modifier.weight(1f))
                    IconButton(onClick={walls.removeAt(i)},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف",tint=MaterialTheme.colorScheme.error)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("الطول",length,{length=it;walls[i]=walls[i].copy(length=n(it))},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("الارتفاع",height,{height=it;walls[i]=walls[i].copy(height=n(it).takeIf{x->x>0}?:defaultHeight)},"م",modifier=Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SurfacesEditor(items:MutableList<SurfaceSegment>,prefix:String){
    items.forEachIndexed{i,s->
        var length by remember(s.id){mutableStateOf(if(s.length==0.0)"" else fmt(s.length))}
        var width by remember(s.id){mutableStateOf(if(s.width==0.0)"" else fmt(s.width))}
        var deduction by remember(s.id){mutableStateOf(if(s.deductionArea==0.0)"" else fmt(s.deductionArea))}
        Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("$prefix ${i+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                    IconButton(onClick={items.removeAt(i)},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف",tint=MaterialTheme.colorScheme.error)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("الطول",length,{length=it;items[i]=items[i].copy(length=n(it))},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("العرض",width,{width=it;items[i]=items[i].copy(width=n(it))},"م",modifier=Modifier.weight(1f))
                }
                NumberFieldX("خصومات المسطح",deduction,{deduction=it;items[i]=items[i].copy(deductionArea=n(it))},"م²","أعمدة، مناور أو مناطق غير منفذة.")
            }
        }
    }
}

@Composable
private fun OpeningsCard(openings:MutableList<Opening>,walls:List<WallSegment>){
    CardBox{
        SectionTitle("الأبواب والشبابيك","جلسة الشباك تمنع خصم جزء غير موجود داخل ارتفاع السيراميك.")
        if(openings.isEmpty())Text("لا توجد فتحات.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        openings.forEachIndexed{i,o->OpeningEditorRow(i,o,walls,{openings[i]=it},{openings.removeAt(i)})}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={openings.add(Opening(type=OpeningType.DOOR,wallId=walls.firstOrNull()?.id))},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Icon(Icons.Rounded.DoorFront,null);Spacer(Modifier.width(4.dp));Text("باب")}
            OutlinedButton(onClick={openings.add(Opening(type=OpeningType.WINDOW,sillHeight=.9,wallId=walls.firstOrNull()?.id))},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Icon(Icons.Rounded.Window,null);Spacer(Modifier.width(4.dp));Text("شباك")}
        }
    }
}

@Composable
private fun OpeningEditorRow(index:Int,o:Opening,walls:List<WallSegment>,onChange:(Opening)->Unit,onDelete:()->Unit){
    var width by remember(o.id){mutableStateOf(if(o.width==0.0)"" else fmt(o.width))}
    var height by remember(o.id){mutableStateOf(if(o.height==0.0)"" else fmt(o.height))}
    var sill by remember(o.id){mutableStateOf(fmt(o.sillHeight))}
    var reveal by remember(o.id){mutableStateOf(if(o.revealDepth==0.0)"" else fmt(o.revealDepth))}
    var count by remember(o.id){mutableStateOf(o.count.toString())}
    Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
        Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("${o.type.label} ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                Text("${fmt(o.width*o.height*o.count)} م²",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                IconButton(onClick=onDelete,modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف",tint=MaterialTheme.colorScheme.error)}
            }
            if(walls.isNotEmpty()){
                ChoiceFieldX("الحائط",walls.firstOrNull{it.id==o.wallId}?.name ?: walls.first().name,walls.map{it.name},{label->
                    onChange(o.copy(wallId=walls.first{it.name==label}.id))
                },"اربط الفتحة بالحائط الصحيح.")
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("العرض",width,{width=it;onChange(o.copy(width=n(it)))},"م",modifier=Modifier.weight(1f))
                NumberFieldX("الارتفاع",height,{height=it;onChange(o.copy(height=n(it)))},"م",modifier=Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("جلسة",sill,{sill=it;onChange(o.copy(sillHeight=n(it)))},"م","من الأرض حتى أسفل الفتحة.",Modifier.weight(1f))
                NumberFieldX("عمق الجنب",reveal,{reveal=it;onChange(o.copy(revealDepth=n(it)))},"م","لحساب جوانب الفتحات.",Modifier.weight(1f))
                NumberFieldX("العدد",count,{count=it;onChange(o.copy(count=n(it).toInt().coerceAtLeast(1)))},"",modifier=Modifier.weight(.8f))
            }
        }
    }
}

@Composable
private fun ReviewCard(space:SpaceEntry,warnings:List<String>){
    CardBox{
        SectionTitle("المراجعة النهائية","راجع التحذيرات ومصدر كل كمية قبل الاعتماد.")
        if(warnings.isNotEmpty()){
            Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.errorContainer){
                Column(Modifier.padding(9.dp)){warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)}}
            }
        }
        MetricRow("حالة الحصر",space.status.label)
        MetricRow("الفتحات","${space.openings.size} فتحة")
        MetricRow("الصور","${space.photoUris.size} صورة")
        HorizontalDivider()
        if(space.takeoffs.isEmpty())Text("لا توجد بنود.",style=MaterialTheme.typography.bodySmall)
        else space.takeoffs.forEach{item->
            val q=QuantityEngine.calculate(space,item)
            Column{
                MetricRow(item.name,"${fmt(q.final)} ${item.unit.label}",item.manualOverride!=null)
                Text(q.explanation,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(q.overridden)Text("كمية فعلية معتمدة: ${item.overrideReason}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun CustomItemDialog(onDismiss:()->Unit,onAdd:(TakeoffItem)->Unit){
    var name by remember{mutableStateOf("")}
    var unit by remember{mutableStateOf(MeasureUnit.AREA)}
    var method by remember{mutableStateOf(CalcMethod.DIRECT_AREA)}
    AlertDialog(
        onDismissRequest=onDismiss,title={Text("بند مخصص")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                TextFieldX("اسم البند",name,{name=it},placeholder="مثال: كلادينج")
                ChoiceFieldX("الوحدة",unit.label,MeasureUnit.entries.map{it.label},{label->
                    unit=MeasureUnit.entries.first{it.label==label}
                    method=when(unit){
                        MeasureUnit.AREA->CalcMethod.DIRECT_AREA
                        MeasureUnit.LENGTH->CalcMethod.DIRECT_LENGTH
                        MeasureUnit.VOLUME->CalcMethod.DIRECT_VOLUME
                        MeasureUnit.COUNT->CalcMethod.DIRECT_COUNT
                    }
                })
                val methods=CalcMethod.entries.filter{m->
                    when(unit){
                        MeasureUnit.AREA->m in listOf(CalcMethod.ROOM_WALLS,CalcMethod.WALL_SEGMENTS,CalcMethod.FLOOR_SURFACES,CalcMethod.CEILING_SURFACES,CalcMethod.WALL_TILES,CalcMethod.WATERPROOFING,CalcMethod.DIRECT_AREA)
                        MeasureUnit.LENGTH->m in listOf(CalcMethod.SKIRTING,CalcMethod.DIRECT_LENGTH)
                        MeasureUnit.VOLUME->m==CalcMethod.DIRECT_VOLUME
                        MeasureUnit.COUNT->m==CalcMethod.DIRECT_COUNT
                    }
                }
                ChoiceFieldX("طريقة الحصر",method.label,methods.map{it.label},{label->method=methods.first{it.label==label}})
            }
        },
        confirmButton={TextButton(onClick={
            if(name.isNotBlank())onAdd(TakeoffItem(name=name.trim(),category="مخصص",unit=unit,method=method))
        }){Text("إضافة")}},
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}}
    )
}

@Composable
private fun TakeoffEditorDialog(item:TakeoffItem,walls:List<WallSegment>,onDismiss:()->Unit,onSave:(TakeoffItem)->Unit){
    var waste by remember{mutableStateOf(fmt(item.wastePercent))}
    var tileHeight by remember{mutableStateOf(fmt(item.tileHeight))}
    var upstand by remember{mutableStateOf(fmt(item.waterproofUpstand))}
    var layerThickness by remember{mutableStateOf(if(item.layerThickness==0.0)"" else fmt(item.layerThickness))}
    var direct by remember{mutableStateOf(if(item.directValue==0.0)"" else fmt(item.directValue))}
    var pieceWidth by remember{mutableStateOf(if(item.pieceWidth==0.0)"" else fmt(item.pieceWidth))}
    var pieceHeight by remember{mutableStateOf(if(item.pieceHeight==0.0)"" else fmt(item.pieceHeight))}
    var piecesPerPack by remember{mutableStateOf(if(item.piecesPerPack==0)"" else item.piecesPerPack.toString())}
    var wallIds by remember{mutableStateOf(item.wallIds.toSet())}
    var reveals by remember{mutableStateOf(item.includeOpeningReveals)}
    var overrideEnabled by remember{mutableStateOf(item.manualOverride!=null)}
    var overrideValue by remember{mutableStateOf(item.manualOverride?.let(::fmt) ?: "")}
    var overrideReason by remember{mutableStateOf(item.overrideReason)}
    var adjustments by remember{mutableStateOf(item.adjustments)}
    var newAdjValue by remember{mutableStateOf("")}
    var newAdjNote by remember{mutableStateOf("")}
    var newAdjType by remember{mutableStateOf(AdjustmentType.ADD)}

    AlertDialog(
        onDismissRequest=onDismiss,title={Text(item.name)},
        text={
            LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
                item{MetricRow("الوحدة",item.unit.label)}
                if(item.method in listOf(CalcMethod.DIRECT_AREA,CalcMethod.DIRECT_LENGTH,CalcMethod.DIRECT_VOLUME,CalcMethod.DIRECT_COUNT)){
                    item{NumberFieldX("الكمية",direct,{direct=it},item.unit.label,"كمية مباشرة.")}
                }
                if(item.method==CalcMethod.WALL_TILES)item{NumberFieldX("ارتفاع الكسوة",tileHeight,{tileHeight=it},"م","الخصم من الشباك يتم حسب جلسة الشباك.")}
                if(item.method==CalcMethod.WATERPROOFING)item{NumberFieldX("رجوع العزل",upstand,{upstand=it},"م","ارتفاع رجوع العزل على الحائط.")}
                if(item.method==CalcMethod.FLOOR_LAYER_VOLUME)item{NumberFieldX("متوسط السمك",layerThickness,{layerThickness=it},"م","مثال 5 سم = 0.05 م.")}
                item{NumberFieldX("الهالك",waste,{waste=it},"%","خاص بهذا البند فقط.")}
                if(item.unit==MeasureUnit.AREA){
                    item{
                        HorizontalDivider()
                        Text("مقاس القطعة / العبوة - اختياري",style=MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            NumberFieldX("عرض القطعة",pieceWidth,{pieceWidth=it},"م",modifier=Modifier.weight(1f))
                            NumberFieldX("طول القطعة",pieceHeight,{pieceHeight=it},"م",modifier=Modifier.weight(1f))
                        }
                        NumberFieldX("عدد القطع/كرتونة",piecesPerPack,{piecesPerPack=it},"قطعة","لو معروف سيظهر عدد الكراتين التقريبي.")
                    }
                }
                if(item.method in listOf(CalcMethod.ROOM_WALLS,CalcMethod.WALL_SEGMENTS,CalcMethod.WALL_TILES)){
                    item{
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("احتساب جوانب الفتحات",Modifier.weight(1f));Switch(checked=reveals,onCheckedChange={reveals=it})
                        }
                        if(walls.isNotEmpty()){
                            Text("تطبيق على حوائط محددة - اترك الكل غير محدد لتطبيقه على جميع الحوائط.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            walls.forEach{wall->
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                                    Checkbox(
                                        checked=wall.id in wallIds,
                                        onCheckedChange={checked->wallIds=if(checked)wallIds+wall.id else wallIds-wall.id}
                                    )
                                    Text(wall.name,style=MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item{HorizontalDivider();Text("إضافات وخصومات يدوية",style=MaterialTheme.typography.labelLarge)}
                item{
                    Column{
                        adjustments.forEachIndexed{i,a->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text((if(a.type==AdjustmentType.ADD)"+ " else "- ")+fmt(a.amount)+" "+item.unit.label,Modifier.weight(1f),color=if(a.type==AdjustmentType.ADD)MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                Text(a.note,Modifier.weight(1.4f),style=MaterialTheme.typography.bodySmall)
                                IconButton(onClick={adjustments=adjustments.filterIndexed{idx,_->idx!=i}},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف")}
                            }
                        }
                    }
                }
                item{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        ChoiceFieldX("النوع",if(newAdjType==AdjustmentType.ADD)"إضافة" else "خصم",listOf("إضافة","خصم"),{newAdjType=if(it=="إضافة")AdjustmentType.ADD else AdjustmentType.DEDUCT},modifier=Modifier.weight(1f))
                        NumberFieldX("الكمية",newAdjValue,{newAdjValue=it},item.unit.label,modifier=Modifier.weight(1f))
                    }
                    TextFieldX("السبب",newAdjNote,{newAdjNote=it},placeholder="مثال: استبعاد خلف الدولاب")
                    OutlinedButton(onClick={
                        if(n(newAdjValue)>0){
                            adjustments=adjustments+Adjustment(type=newAdjType,amount=n(newAdjValue),note=newAdjNote.trim())
                            newAdjValue="";newAdjNote=""
                        }
                    },modifier=Modifier.fillMaxWidth()){Text("إضافة التعديل")}
                }
                item{
                    HorizontalDivider()
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("اعتماد كمية فعلية بدل المحسوبة",Modifier.weight(1f));Switch(checked=overrideEnabled,onCheckedChange={overrideEnabled=it})
                    }
                    if(overrideEnabled){
                        NumberFieldX("الكمية الفعلية",overrideValue,{overrideValue=it},item.unit.label,"يظل الرقم النظري محفوظًا للمقارنة.")
                        TextFieldX("سبب الاعتماد",overrideReason,{overrideReason=it},placeholder="مثال: حصر فعلي من الموقع")
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={
            onSave(item.copy(
                wastePercent=n(waste),tileHeight=n(tileHeight),waterproofUpstand=n(upstand),
                layerThickness=n(layerThickness),directValue=n(direct),includeOpeningReveals=reveals,
                wallIds=wallIds.toList(),
                pieceWidth=n(pieceWidth),pieceHeight=n(pieceHeight),
                piecesPerPack=n(piecesPerPack).toInt().coerceAtLeast(0),
                manualOverride=if(overrideEnabled)n(overrideValue) else null,
                overrideReason=if(overrideEnabled)overrideReason.trim() else "",
                adjustments=adjustments
            ))
        }){Text("حفظ")}},
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}}
    )
}

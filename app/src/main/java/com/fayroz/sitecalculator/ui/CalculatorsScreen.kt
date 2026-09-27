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
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.ProjectRepository
import com.fayroz.sitecalculator.domain.QuantityEngine
import java.util.UUID
import kotlin.math.min

private val roomTypes=listOf("غرفة نوم","ريسبشن","حمام","مطبخ","ممر","بلكونة","مخزن","فراغ آخر")
private val workHelp=mapOf(
    "محارة الحوائط" to "مساحة الحوائط الصافية بعد خصم الأبواب والشبابيك.",
    "محارة السقف" to "مساحة السقف = الطول × العرض.",
    "دهان الحوائط" to "مساحة الحوائط الصافية بعد خصم الفتحات.",
    "دهان السقف" to "مساحة السقف.",
    "الأرضيات" to "مساحة الأرضية مع نسبة القص والهالك.",
    "سيراميك الحوائط" to "محيط الفراغ × ارتفاع السيراميك - الفتحات + الهالك.",
    "الوزرات" to "محيط الغرفة - عروض الأبواب + الهالك.",
    "عزل الأرضية" to "الأرضية + رجوع 20 سم على الحوائط + الهالك.",
    "سقف جبس بورد" to "مساحة السقف مع نسبة القص والهالك."
)

private data class OpenDraft(
    val id:String=UUID.randomUUID().toString(),
    val type:String="باب",
    val width:String="",
    val height:String="",
    val count:String="1"
)

private data class WallDraft(
    val id:String=UUID.randomUUID().toString(),
    val length:String="",
    val height:String="3.00",
    val openingArea:String=""
)

private fun autoRoomName(type:String,existing:List<RoomEntry>):String{
    val n=existing.count{it.type==type}+1
    return if(n==1)type else "$type $n"
}

@Composable
private fun OpeningRows(openings:MutableList<OpenDraft>){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        openings.forEachIndexed{i,o->
            Surface(
                shape=RoundedCornerShape(13.dp),
                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.40f),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("${o.type} ${i+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                        Text("${fmt(n(o.width)*n(o.height)*n(o.count).toInt().coerceAtLeast(1))} م²",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        IconButton(onClick={openings.removeAt(i)},modifier=Modifier.size(30.dp)){Icon(Icons.Rounded.Delete,"حذف",Modifier.size(17.dp))}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        LabeledNumber("العرض",o.width,{v->openings[i]=o.copy(width=v)},"م","عرض الفتحة.",Modifier.weight(1f))
                        LabeledNumber("الارتفاع",o.height,{v->openings[i]=o.copy(height=v)},"م","ارتفاع الفتحة.",Modifier.weight(1f))
                        LabeledNumber("العدد",o.count,{v->openings[i]=o.copy(count=v)},"","عدد الفتحات.",Modifier.weight(.78f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(onClick={openings.add(OpenDraft(type="باب"))},modifier=Modifier.weight(1f)){
                Icon(Icons.Rounded.DoorFront,null);Spacer(Modifier.width(4.dp));Text("باب")
            }
            OutlinedButton(onClick={openings.add(OpenDraft(type="شباك"))},modifier=Modifier.weight(1f)){
                Icon(Icons.Rounded.Window,null);Spacer(Modifier.width(4.dp));Text("شباك")
            }
        }
    }
}

@Composable
fun RoomEditorScreen(
    repository:ProjectRepository,
    title:String,
    initial:RoomEntry?=null,
    existing:List<RoomEntry> = emptyList(),
    onBack:()->Unit,
    onSave:((RoomEntry)->Unit)?=null,
    onSaveAndContinue:((RoomEntry)->Unit)?=null
){
    var page by remember{mutableIntStateOf(0)}
    val initialType=initial?.type ?: "غرفة نوم"
    var type by remember{mutableStateOf(initialType)}
    var autoName by remember{mutableStateOf(initial==null)}
    var name by remember{mutableStateOf(initial?.name ?: autoRoomName(initialType,existing))}
    var length by remember{mutableStateOf(initial?.length?.let(::fmt) ?: "")}
    var width by remember{mutableStateOf(initial?.width?.let(::fmt) ?: "")}
    var height by remember{mutableStateOf(initial?.height?.let(::fmt) ?: fmt(repository.lastHeight()))}
    var repeats by remember{mutableStateOf(initial?.repeatCount?.toString() ?: "1")}
    var tileHeight by remember{mutableStateOf(initial?.tileHeight?.let(::fmt) ?: "2.40")}
    var waste by remember{mutableStateOf(initial?.waste?.let(::fmt) ?: "7")}
    var note by remember{mutableStateOf(initial?.note ?: "")}
    val openings=remember{
        mutableStateListOf<OpenDraft>().apply{
            initial?.openings?.forEach{o->add(OpenDraft(o.id,o.type,fmt(o.width),fmt(o.height),o.count.toString()))}
        }
    }
    val selected=remember{
        mutableStateMapOf<String,Boolean>().apply{
            QuantityEngine.works.forEach{w->put(w,initial?.works?.contains(w)==true)}
        }
    }

    val l=n(length)
    val w=n(width)
    val h=n(height)
    val count=n(repeats).toInt().coerceAtLeast(1)
    val wasteFactor=1+n(waste)/100.0
    val openingModels=openings.mapNotNull{o->
        val ow=n(o.width);val oh=n(o.height);val oc=n(o.count).toInt().coerceAtLeast(1)
        if(ow>0&&oh>0)Opening(o.id,o.type,ow,oh,oc) else null
    }
    val temp=RoomEntry(
        id=initial?.id ?: UUID.randomUUID().toString(),
        name=name.trim().ifBlank{type},
        type=type,
        length=l,width=w,height=h,
        tileHeight=n(tileHeight),
        waste=n(waste),
        repeatCount=count,
        note=note.trim(),
        openings=openingModels,
        works=QuantityEngine.works.filter{selected[it]==true}
    )
    val warnings=buildList{
        if(l<=0||w<=0) add("أدخل الطول والعرض.")
        if(h<=0) add("أدخل ارتفاعًا صحيحًا.")
        if(h>8) add("ارتفاع الحائط أكبر من 8 م؛ راجع الوحدة.")
        if(l>50||w>50) add("أحد الأبعاد كبير جدًا؛ تأكد أن الوحدة بالمتر.")
        val oneWall=2*(l+w)*h
        if(oneWall>0 && openingModels.sumOf{it.area}>=oneWall) add("مساحة الفتحات أكبر من مساحة الحوائط.")
        if(n(waste)>30) add("نسبة الهالك أكبر من 30%.")
    }

    fun saveRoom(){
        repository.setLastHeight(h)
        onSave?.invoke(temp)
    }

    Scaffold(
        topBar={AppBar(title,"${page+1} من 4",onBack)},
        bottomBar={
            Surface(color=MaterialTheme.colorScheme.surface,tonalElevation=4.dp){
                Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    if(page>0){
                        OutlinedButton(onClick={page--},modifier=Modifier.weight(1f)){Text("السابق")}
                    }
                    when(page){
                        0,1,2 -> Button(onClick={page++},modifier=Modifier.weight(1f)){
                            Text(
                                when(page){
                                    0->"الفتحات"
                                    1->"البنود"
                                    else->"النتيجة"
                                }
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.ChevronLeft,null)
                        }
                        else -> {
                            if(onSave!=null){
                                Button(
                                    onClick=::saveRoom,
                                    enabled=l>0&&w>0&&h>0,
                                    modifier=Modifier.weight(1f)
                                ){
                                    Icon(Icons.Rounded.Save,null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if(initial==null)"حفظ" else "حفظ التعديل")
                                }
                                if(onSaveAndContinue!=null && initial==null){
                                    OutlinedButton(
                                        onClick={
                                            if(l>0&&w>0&&h>0){
                                                repository.setLastHeight(h)
                                                onSaveAndContinue(temp)
                                                page=0
                                                name=autoRoomName(type,existing+temp)
                                                autoName=true
                                                length="";width="";repeats="1";note=""
                                                openings.clear()
                                                QuantityEngine.works.forEach{selected[it]=false}
                                            }
                                        },
                                        modifier=Modifier.weight(1f)
                                    ){Text("حفظ + غرفة")}
                                }
                            }else{
                                Button(onClick={page=0},modifier=Modifier.weight(1f)){Text("حساب جديد")}
                            }
                        }
                    }
                }
            }
        }
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                LinearProgressIndicator(
                    progress={ (page+1)/4f },
                    modifier=Modifier.fillMaxWidth(),
                    color=MaterialTheme.colorScheme.secondary,
                    trackColor=MaterialTheme.colorScheme.surfaceVariant
                )
            }

            item{
                when(page){
                    0 -> CardBox{
                        SectionTitle("بيانات الفراغ","أدخل المقاسات الصافية داخل المكان.")
                        LabeledText(
                            "اسم الغرفة أو الفراغ",
                            name,
                            {name=it;autoName=false},
                            "مثال: غرفة نوم رئيسية",
                            "اسم واضح يظهر في المشروع والملخص."
                        )
                        ChoiceField(
                            "نوع الفراغ",
                            type,
                            roomTypes,
                            {
                                type=it
                                if(autoName)name=autoRoomName(it,existing)
                            },
                            "يساعد البرنامج في اقتراح الاسم."
                        )
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            LabeledNumber("الطول",length,{length=it},"م","الطول الصافي.",Modifier.weight(1f))
                            LabeledNumber("العرض",width,{width=it},"م","العرض الصافي.",Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            LabeledNumber("الارتفاع",height,{height=it},"م","من الأرضية إلى السقف.",Modifier.weight(1f))
                            LabeledNumber("التكرار",repeats,{repeats=it},"عدد","نفس الفراغ مكرر.",Modifier.weight(1f))
                        }
                        LabeledText("ملاحظة",note,{note=it},"مثال: السيراميك حتى 2.20 م","تظهر مع الغرفة وتساعد في التنفيذ.")
                    }
                    1 -> CardBox{
                        SectionTitle("الأبواب والشبابيك","المساحات تُخصم تلقائيًا من الحوائط.")
                        if(openings.isEmpty()){
                            Text("لا توجد فتحات. أضف بابًا أو شباكًا عند الحاجة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OpeningRows(openings)
                        HorizontalDivider()
                        MetricRow("إجمالي الفتحات","${fmt(openingModels.sumOf{it.area})} م²",true)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            LabeledNumber("ارتفاع السيراميك",tileHeight,{tileHeight=it},"م","ارتفاع كسوة الحائط.",Modifier.weight(1f))
                            LabeledNumber("الهالك",waste,{waste=it},"%","يضاف لكمية الشراء.",Modifier.weight(1f))
                        }
                    }
                    2 -> CardBox{
                        SectionTitle("البنود المطلوبة","فعّل الأعمال الموجودة في هذا الفراغ فقط.")
                        QuantityEngine.works.forEach{work->
                            Surface(
                                shape=RoundedCornerShape(12.dp),
                                color=if(selected[work]==true)MaterialTheme.colorScheme.primaryContainer.copy(alpha=.60f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.34f),
                                border=BorderStroke(1.dp,if(selected[work]==true)MaterialTheme.colorScheme.primary.copy(alpha=.30f) else MaterialTheme.colorScheme.outlineVariant)
                            ){
                                Row(Modifier.fillMaxWidth().padding(start=9.dp,end=5.dp,top=3.dp,bottom=3.dp),verticalAlignment=Alignment.CenterVertically){
                                    Text(work,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,fontWeight=if(selected[work]==true)FontWeight.Bold else FontWeight.Medium)
                                    HelpIcon(work,workHelp[work].orEmpty())
                                    Switch(checked=selected[work]==true,onCheckedChange={selected[work]=it})
                                }
                            }
                        }
                    }
                    else -> CardBox{
                        SectionTitle("النتيجة","كل كمية محسوبة من نفس بيانات الفراغ.")
                        if(warnings.isNotEmpty()){
                            Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.errorContainer){
                                Column(Modifier.fillMaxWidth().padding(9.dp)){
                                    warnings.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)}
                                }
                            }
                        }
                        if(l>0&&w>0&&h>0){
                            val active=QuantityEngine.works.filter{selected[it]==true}
                            if(active.isEmpty()){
                                Text("لم يتم اختيار بنود بعد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }else{
                                active.forEach{work->
                                    MetricRow(work,"${fmt(QuantityEngine.quantity(temp,work))} ${QuantityEngine.unit(work)}")
                                }
                            }
                            HorizontalDivider()
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text("تفاصيل حساب الحوائط",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                                HelpIcon(
                                    "تفاصيل حساب الحوائط",
                                    "المحيط = 2 × (${fmt(l)} + ${fmt(w)}) = ${fmt(2*(l+w))} م\n"+
                                        "مسطح الحوائط قبل الخصم = ${fmt(2*(l+w)*h)} م²\n"+
                                        "إجمالي الفتحات = ${fmt(openingModels.sumOf{it.area})} م²\n"+
                                        "الصافي للفراغ الواحد = ${fmt((2*(l+w)*h-openingModels.sumOf{it.area}).coerceAtLeast(0.0))} م²."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val itemTypes=listOf(
    "محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف",
    "الأرضيات","سيراميك الحوائط","الوزرات","المباني","عزل الأرضية","سقف جبس بورد"
)

@Composable
fun ItemCalculatorScreenV6(
    repository:ProjectRepository,
    onBack:()->Unit
){
    var item by remember{mutableStateOf(itemTypes.first())}
    var method by remember{mutableStateOf("غرفة كاملة")}
    var length by remember{mutableStateOf("")}
    var width by remember{mutableStateOf("")}
    var height by remember{mutableStateOf(fmt(repository.lastHeight()))}
    var count by remember{mutableStateOf("1")}
    var direct by remember{mutableStateOf("")}
    var waste by remember{mutableStateOf("7")}
    var thickness by remember{mutableStateOf("0.12")}
    var upstand by remember{mutableStateOf("0.20")}
    val openings=remember{mutableStateListOf<OpenDraft>()}
    val walls=remember{mutableStateListOf(WallDraft(height=fmt(repository.lastHeight())))}

    val wallBased=item in listOf("محارة الحوائط","دهان الحوائط","سيراميك الحوائط","المباني")
    val flatBased=item in listOf("محارة السقف","دهان السقف","الأرضيات","عزل الأرضية","سقف جبس بورد")
    val methods=when{
        wallBased -> listOf("غرفة كاملة","عدة حوائط متتابعة","مساحة جاهزة")
        item=="الوزرات" -> listOf("غرفة كاملة","طول جاهز")
        else -> listOf("غرفة أو مسطح","مساحة جاهزة")
    }
    LaunchedEffect(item){
        if(method !in methods) method=methods.first()
    }

    val l=n(length);val w=n(width);val h=n(height);val c=n(count).toInt().coerceAtLeast(1)
    val openingArea=openings.sumOf{n(it.width)*n(it.height)*n(it.count).toInt().coerceAtLeast(1)}
    val openingDoorWidth=openings.filter{it.type=="باب"}.sumOf{n(it.width)*n(it.count).toInt().coerceAtLeast(1)}
    val roomWalls=(2*(l+w)*h-openingArea).coerceAtLeast(0.0)*c
    val sequentialWalls=walls.sumOf{(n(it.length)*n(it.height)-n(it.openingArea)).coerceAtLeast(0.0)}
    val wallBase=when(method){
        "غرفة كاملة"->roomWalls
        "عدة حوائط متتابعة"->sequentialWalls
        else->n(direct)
    }
    val flatBase=if(method=="مساحة جاهزة")n(direct) else l*w*c
    val factor=1+n(waste)/100.0
    val skirting=if(method=="طول جاهز")n(direct) else (2*(l+w)-openingDoorWidth).coerceAtLeast(0.0)*c
    val result=when(item){
        "محارة الحوائط","دهان الحوائط","المباني"->wallBase
        "سيراميك الحوائط"->wallBase*factor
        "محارة السقف","دهان السقف"->flatBase
        "الأرضيات","سقف جبس بورد"->flatBase*factor
        "عزل الأرضية"->if(method=="مساحة جاهزة")flatBase*factor else (flatBase+2*(l+w)*n(upstand)*c)*factor
        "الوزرات"->skirting*factor
        else->0.0
    }

    Scaffold(
        topBar={AppBar("حصر بند","اختيار الطريقة حسب واقع الموقع",onBack)}
    ){padding->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                BrandHeader(
                    "حساب بند",
                    "غرفة كاملة أو حوائط متتابعة أو كمية جاهزة",
                    listOf("البند" to item,"الطريقة" to method,"الوحدة" to if(item=="الوزرات")"م ط" else "م²")
                )
            }
            item{
                CardBox{
                    ChoiceField("البند",item,itemTypes,{item=it},"اختيار البند يغيّر المدخلات المطلوبة.")
                    ChoiceField("طريقة الحصر",method,methods,{method=it},"للصالات والأشكال غير المنتظمة استخدم عدة حوائط متتابعة.")
                }
            }
            item{
                CardBox{
                    SectionTitle("المقاسات","أدخل المطلوب فقط حسب الطريقة المختارة.")
                    when{
                        wallBased && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                LabeledNumber("الطول",length,{length=it},"م","طول الغرفة.",Modifier.weight(1f))
                                LabeledNumber("العرض",width,{width=it},"م","عرض الغرفة.",Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                LabeledNumber("الارتفاع",height,{height=it;repository.setLastHeight(n(it))},"م","ارتفاع الحائط.",Modifier.weight(1f))
                                LabeledNumber("التكرار",count,{count=it},"عدد","عدد الغرف.",Modifier.weight(1f))
                            }
                            OpeningRows(openings)
                        }
                        wallBased && method=="عدة حوائط متتابعة" -> {
                            Text("أدخل الحوائط وراء بعض؛ البرنامج يجمع الصافي تلقائيًا.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            walls.forEachIndexed{i,wall->
                                Surface(shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.40f),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text("حائط ${i+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                                            Text("${fmt((n(wall.length)*n(wall.height)-n(wall.openingArea)).coerceAtLeast(0.0))} م²",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                            if(walls.size>1)IconButton(onClick={walls.removeAt(i)},modifier=Modifier.size(30.dp)){Icon(Icons.Rounded.Delete,"حذف",Modifier.size(17.dp))}
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                            LabeledNumber("الطول",wall.length,{v->walls[i]=wall.copy(length=v)},"م","طول الحائط.",Modifier.weight(1f))
                                            LabeledNumber("الارتفاع",wall.height,{v->walls[i]=wall.copy(height=v);repository.setLastHeight(n(v))},"م","ارتفاع الحائط.",Modifier.weight(1f))
                                        }
                                        LabeledNumber("مساحة الفتحات",wall.openingArea,{v->walls[i]=wall.copy(openingArea=v)},"م²","إجمالي فتحات الحائط.")
                                    }
                                }
                            }
                            OutlinedButton(onClick={walls.add(WallDraft(height=height.ifBlank{fmt(repository.lastHeight())}))},modifier=Modifier.fillMaxWidth()){
                                Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("إضافة حائط")
                            }
                        }
                        wallBased -> LabeledNumber("المساحة الصافية",direct,{direct=it},"م²","اكتب المساحة النهائية مباشرة.")
                        flatBased && method=="غرفة أو مسطح" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                LabeledNumber("الطول",length,{length=it},"م","طول المسطح.",Modifier.weight(1f))
                                LabeledNumber("العرض",width,{width=it},"م","عرض المسطح.",Modifier.weight(1f))
                            }
                            LabeledNumber("التكرار",count,{count=it},"عدد","عدد المسطحات بنفس المقاس.")
                            if(item=="عزل الأرضية")LabeledNumber("رجوع العزل",upstand,{upstand=it},"م","ارتفاع رجوع العزل على الحوائط.")
                        }
                        flatBased -> LabeledNumber("المساحة",direct,{direct=it},"م²","أدخل المساحة مباشرة.")
                        item=="الوزرات" && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                LabeledNumber("الطول",length,{length=it},"م","طول الغرفة.",Modifier.weight(1f))
                                LabeledNumber("العرض",width,{width=it},"م","عرض الغرفة.",Modifier.weight(1f))
                            }
                            OpeningRows(openings)
                        }
                        else -> LabeledNumber("الطول الصافي",direct,{direct=it},"م ط","طول الوزرات.")
                    }

                    if(item in listOf("الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد")){
                        LabeledNumber("الهالك والقص",waste,{waste=it},"%","زيادة على الكمية.")
                    }
                    if(item=="المباني"){
                        LabeledNumber("سمك الحائط",thickness,{thickness=it},"م","مثال 0.12 م.")
                    }
                }
            }
            item{
                CardBox{
                    SectionTitle("النتيجة","تتحدث فورًا مع أي تعديل.")
                    if(item=="المباني"){
                        MetricRow("مساحة المباني","${fmt(result)} م²",true)
                        MetricRow("حجم المباني","${fmt(result*n(thickness))} م³")
                    }else{
                        MetricRow(item,"${fmt(result)} ${if(item=="الوزرات")"م ط" else "م²"}",true)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("شرح طريقة الحساب",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                        HelpIcon(
                            "طريقة الحساب",
                            when{
                                wallBased && method=="غرفة كاملة" ->
                                    "المحيط = 2 × (${fmt(l)} + ${fmt(w)})\nالمسطح = المحيط × ${fmt(h)} - ${fmt(openingArea)} فتحات"
                                wallBased && method=="عدة حوائط متتابعة" ->
                                    "كل حائط = الطول × الارتفاع - الفتحات، ثم يتم جمع جميع الحوائط."
                                method=="مساحة جاهزة" || method=="طول جاهز" ->
                                    "تم استخدام الكمية المدخلة مباشرة."
                                else -> "المساحة = الطول × العرض × التكرار."
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsLandingScreen(
    onRoom:()->Unit,
    onItem:()->Unit
){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{BrandHeader("الحاسبات السريعة","بدون إنشاء مشروع أو حفظ بيانات.")}
        item{
            DashboardTile(
                "حصر فراغ كامل",
                "أبعاد + فتحات + بنود + نتيجة مفصلة",
                Icons.Rounded.MeetingRoom,
                onRoom,
                true,
                Modifier.fillMaxWidth()
            )
        }
        item{
            DashboardTile(
                "حصر بند مباشر",
                "غرفة كاملة، عدة حوائط متتابعة، أو كمية جاهزة",
                Icons.Rounded.Calculate,
                onItem,
                false,
                Modifier.fillMaxWidth()
            )
        }
    }
}

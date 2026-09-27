package com.fayroz.sitecalculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.ui.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

private val spaceTypes=listOf("غرفة نوم","ريسبشن","حمام","مطبخ","ممر","بلكونة","مخزن","فراغ آخر")
private val workTypes=QuantityEngine.workOrder

private val workHelp=mapOf(
    "محارة الحوائط" to "مساحة الحوائط الصافية بعد خصم الأبواب والشبابيك.",
    "محارة السقف" to "مساحة السقف وتساوي الطول × العرض.",
    "دهان الحوائط" to "مساحة الحوائط الصافية بعد خصم الفتحات.",
    "دهان السقف" to "مساحة السقف الصافية.",
    "الأرضيات" to "مساحة الأرضية مع إضافة نسبة القص والهالك.",
    "سيراميك الحوائط" to "محيط الفراغ × ارتفاع السيراميك، مع خصم الفتحات وإضافة الهالك.",
    "الوزرات" to "محيط الغرفة بعد خصم عروض الأبواب وإضافة الهالك.",
    "عزل الأرضية" to "مساحة الأرضية مع رجوع العزل على الحائط 20 سم.",
    "سقف جبس بورد" to "مساحة السقف مع نسبة القص والهالك."
)

private data class OpeningDraft(
    val id:String=UUID.randomUUID().toString(),
    val type:String,
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

private fun nextRoomName(type:String,existing:List<SavedSpace>):String{
    val n=existing.count{it.type==type}+1
    return if(n==1 && existing.none{it.type==type}) type else "$type $n"
}

private fun openingArea(list:List<OpeningDraft>)=list.sumOf{
    num(it.width)*num(it.height)*num(it.count).toInt().coerceAtLeast(1)
}
private fun doorWidth(list:List<OpeningDraft>)=list.filter{it.type=="باب"}.sumOf{
    num(it.width)*num(it.count).toInt().coerceAtLeast(1)
}
private fun toOpenings(list:List<OpeningDraft>)=list.mapNotNull{
    val w=num(it.width); val h=num(it.height); val c=num(it.count).toInt().coerceAtLeast(1)
    if(w>0 && h>0) Opening(id=it.id,type=it.type,width=w,height=h,count=c) else null
}

@Composable
private fun WarningBox(messages:List<String>){
    if(messages.isEmpty()) return
    Surface(
        shape=RoundedCornerShape(10.dp),
        color=MaterialTheme.colorScheme.errorContainer
    ){
        Column(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text("راجع المدخلات",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onErrorContainer)
            messages.forEach{Text("• $it",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)}
        }
    }
}

@Composable
private fun OpeningEditor(
    openings:MutableList<OpeningDraft>,
    onChanged:()->Unit={}
){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        if(openings.isEmpty()){
            Text("لا توجد أبواب أو شبابيك مسجلة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        openings.forEachIndexed{index,o->
            Surface(
                shape=RoundedCornerShape(11.dp),
                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(Modifier.padding(horizontal=8.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("${o.type} ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                        Text(
                            "${qty(num(o.width)*num(o.height)*num(o.count).toInt().coerceAtLeast(1))} م²",
                            style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.tertiary
                        )
                        IconButton(onClick={openings.removeAt(index);onChanged()},modifier=Modifier.size(32.dp)){
                            Icon(Icons.Rounded.Delete,"حذف")
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Box(Modifier.weight(1f)){
                            NumberField("العرض",o.width,{v->openings[index]=o.copy(width=v);onChanged()},"م","عرض الفتحة الصافي.")
                        }
                        Box(Modifier.weight(1f)){
                            NumberField("الارتفاع",o.height,{v->openings[index]=o.copy(height=v);onChanged()},"م","ارتفاع الفتحة الصافي.")
                        }
                        Box(Modifier.weight(.8f)){
                            NumberField("العدد",o.count,{v->openings[index]=o.copy(count=v);onChanged()},"","عدد الفتحات بنفس المقاس.")
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="باب"));onChanged()},
                modifier=Modifier.weight(1f)
            ){Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("باب")}
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="شباك"));onChanged()},
                modifier=Modifier.weight(1f)
            ){Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("شباك")}
        }
    }
}

@Composable
fun SpaceCalculatorScreen(
    projectContext:String?=null,
    onSave:((SavedSpace)->Unit)?=null,
    onSaveAndContinue:((SavedSpace)->Unit)?=null,
    initialSpace:SavedSpace?=null,
    existingSpaces:List<SavedSpace> = emptyList()
){
    val context=LocalContext.current
    val store=remember{ProjectStore(context.applicationContext)}
    val draftKey=remember(projectContext,initialSpace?.id){
        "room_"+(initialSpace?.id ?: projectContext?.hashCode()?.toString() ?: "quick")
    }

    fun initialOpeningDrafts(space:SavedSpace?):List<OpeningDraft>{
        if(space==null) return emptyList()
        if(space.openings.isNotEmpty()) return space.openings.map{
            OpeningDraft(it.id,it.type,qty(it.width),qty(it.height),it.count.toString())
        }
        val result=mutableListOf<OpeningDraft>()
        if(space.doorArea>0){
            val w=if(space.doorWidth>0) space.doorWidth else .9
            result += OpeningDraft(type="باب",width=qty(w),height=qty(space.doorArea/w),count="1")
        }
        if(space.windowArea>0) result += OpeningDraft(type="شباك",width="1.00",height=qty(space.windowArea),count="1")
        return result
    }

    val savedDraft=remember{if(initialSpace==null) store.loadDraft(draftKey) else null}
    val draftObject=remember(savedDraft){runCatching{savedDraft?.let{JSONObject(it)}}.getOrNull()}

    var step by remember{mutableStateOf(0)}
    var autoName by remember{mutableStateOf(initialSpace==null)}
    var roomName by remember{
        mutableStateOf(
            initialSpace?.name
                ?: draftObject?.optString("name")?.takeIf{it.isNotBlank()}
                ?: nextRoomName(spaceTypes.first(),existingSpaces)
        )
    }
    var spaceType by remember{mutableStateOf(initialSpace?.type ?: draftObject?.optString("type",spaceTypes.first()) ?: spaceTypes.first())}
    var length by remember{mutableStateOf(initialSpace?.length?.let(::qty) ?: draftObject?.optString("length","") ?: "")}
    var width by remember{mutableStateOf(initialSpace?.width?.let(::qty) ?: draftObject?.optString("width","") ?: "")}
    var height by remember{
        mutableStateOf(
            initialSpace?.height?.let(::qty)
                ?: draftObject?.optString("height")?.takeIf{it.isNotBlank()}
                ?: qty(store.lastHeight())
        )
    }
    var repeatCount by remember{mutableStateOf((initialSpace?.repeatCount ?: draftObject?.optInt("repeat",1) ?: 1).toString())}
    var tileHeight by remember{mutableStateOf(initialSpace?.tileHeight?.let(::qty) ?: draftObject?.optString("tileHeight","2.40") ?: "2.40")}
    var waste by remember{mutableStateOf(initialSpace?.waste?.let(::qty) ?: draftObject?.optString("waste","7") ?: "7")}
    var note by remember{mutableStateOf(initialSpace?.note ?: draftObject?.optString("note","") ?: "")}

    val openings=remember{
        mutableStateListOf<OpeningDraft>().apply{
            if(initialSpace!=null) addAll(initialOpeningDrafts(initialSpace))
            else {
                val arr=draftObject?.optJSONArray("openings")
                if(arr!=null) for(i in 0 until arr.length()){
                    val o=arr.getJSONObject(i)
                    add(OpeningDraft(
                        id=o.optString("id",UUID.randomUUID().toString()),
                        type=o.optString("type","باب"),
                        width=o.optString("width",""),
                        height=o.optString("height",""),
                        count=o.optString("count","1")
                    ))
                }
            }
        }
    }

    val selected=remember{
        mutableStateMapOf<String,Boolean>().apply{
            val initialWorks=initialSpace?.works
                ?: draftObject?.optJSONArray("works")?.let{a->List(a.length()){a.optString(it)}}
                ?: listOf("محارة الحوائط","محارة السقف","الأرضيات")
            workTypes.forEach{put(it,it in initialWorks)}
        }
    }

    fun saveDraft(){
        if(initialSpace!=null) return
        val j=JSONObject().apply{
            put("name",roomName);put("type",spaceType);put("length",length);put("width",width);put("height",height)
            put("repeat",num(repeatCount).toInt().coerceAtLeast(1));put("tileHeight",tileHeight);put("waste",waste);put("note",note)
            put("works",JSONArray(workTypes.filter{selected[it]==true}))
            val a=JSONArray()
            openings.forEach{o->a.put(JSONObject().apply{
                put("id",o.id);put("type",o.type);put("width",o.width);put("height",o.height);put("count",o.count)
            })}
            put("openings",a)
        }
        store.saveDraft(draftKey,j.toString())
    }

    LaunchedEffect(roomName,spaceType,length,width,height,repeatCount,tileHeight,waste,note,openings.toList(),selected.toMap()){
        saveDraft()
        num(height).takeIf{it>0}?.let{store.setLastHeight(it)}
    }

    val l=num(length); val w=num(width); val h=num(height)
    val repeats=num(repeatCount).toInt().coerceAtLeast(1)
    val allOpenings=toOpenings(openings)
    val dArea=allOpenings.filter{it.type=="باب"}.sumOf{it.area}
    val winArea=allOpenings.filter{it.type=="شباك"}.sumOf{it.area}
    val dWidth=allOpenings.filter{it.type=="باب"}.sumOf{it.totalWidth}
    val grossOne=2*(l+w)*h
    val floor=l*w*repeats
    val netWalls=(grossOne-dArea-winArea).coerceAtLeast(0.0)*repeats
    val factor=1+num(waste)/100.0
    val wallTile=(2*(l+w)*min(num(tileHeight),h)-dArea-winArea).coerceAtLeast(0.0)*repeats*factor
    val skirting=(2*(l+w)-dWidth).coerceAtLeast(0.0)*repeats*factor
    val waterproof=(l*w+2*(l+w)*0.20)*repeats*factor

    val warnings=buildList{
        if(l>30 || w>30) add("أحد أبعاد الغرفة أكبر من 30 م؛ تأكد أن الرقم بالمتر.")
        if(h>8) add("ارتفاع الحائط أكبر من 8 م؛ راجع الرقم قبل الاعتماد.")
        if(grossOne>0 && dArea+winArea>=grossOne) add("مساحة الفتحات تساوي أو تتجاوز مساحة الحوائط.")
        if(num(waste)>30) add("نسبة الهالك أكبر من 30%؛ تأكد أنها مقصودة.")
    }

    fun buildSpace():SavedSpace=SavedSpace(
        id=initialSpace?.id ?: UUID.randomUUID().toString(),
        name=roomName.trim().ifBlank{spaceType},
        type=spaceType,
        length=l,width=w,height=h,
        doorArea=dArea,windowArea=winArea,doorWidth=dWidth,
        tileHeight=num(tileHeight),waste=num(waste),
        works=workTypes.filter{selected[it]==true},
        openings=allOpenings,
        repeatCount=repeats,
        note=note.trim()
    )

    fun resetForNext(){
        val saved=buildSpace()
        roomName=nextRoomName(spaceType,existingSpaces+saved)
        autoName=true
        length="";width="";repeatCount="1";note=""
        openings.clear()
        step=0
    }

    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=11.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)
    ){
        if(!projectContext.isNullOrBlank()){
            item{
                Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.tertiaryContainer){
                    Text(projectContext,Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        if(initialSpace==null && savedDraft!=null){
            item{
                Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                    Text("تم استعادة آخر بيانات غير محفوظة تلقائيًا.",Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=7.dp),style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item{StepTabs(listOf("الغرفة","الفتحات","البنود","النتيجة"),step){step=it}}
        item{
            when(step){
                0 -> SurfaceCard{
                    SectionHeading("بيانات الغرفة","اكتب المقاسات الأساسية مرة واحدة.")
                    TextFieldSimple(
                        "اسم الغرفة أو الفراغ",roomName,
                        {roomName=it;autoName=false},
                        "اسم واضح للمكان، مثل غرفة نوم رئيسية أو حمام الضيوف.",
                        "مثال: غرفة نوم رئيسية"
                    )
                    SelectField(
                        "نوع الفراغ",spaceType,spaceTypes,
                        {newType->
                            spaceType=newType
                            if(autoName) roomName=nextRoomName(newType,existingSpaces)
                        },
                        "يساعد في ترتيب الغرف واقتراح الاسم تلقائيًا."
                    )
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Box(Modifier.weight(1f)){NumberField("الطول",length,{length=it},"م","المسافة الصافية داخل الغرفة.")}
                        Box(Modifier.weight(1f)){NumberField("العرض",width,{width=it},"م","المسافة الصافية داخل الغرفة.")}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Box(Modifier.weight(1f)){NumberField("ارتفاع الحائط",height,{height=it},"م","من منسوب الأرضية حتى السقف.")}
                        Box(Modifier.weight(1f)){NumberField("عدد الغرف بنفس المقاس",repeatCount,{repeatCount=it},"عدد","استخدمه لو نفس الغرفة مكررة عدة مرات.")}
                    }
                    TextFieldSimple("ملاحظات",note,{note=it},"أي ملاحظة تنفيذية تريد الاحتفاظ بها مع الغرفة.","مثال: السيراميك لحد 2.20 م")
                    WarningBox(warnings)
                    Button(onClick={step=1},modifier=Modifier.fillMaxWidth()){Text("التالي: الأبواب والشبابيك")}
                }
                1 -> SurfaceCard{
                    SectionHeading("الأبواب والشبابيك","أضف كل نوع بمقاسه وعدده، والبرنامج يجمعهم تلقائيًا.")
                    OpeningEditor(openings)
                    HorizontalDivider()
                    QuantityRow("إجمالي مساحة الفتحات",dArea+winArea,"م²")
                    NumberField("ارتفاع سيراميك الحائط",tileHeight,{tileHeight=it},"م","الارتفاع الذي سيصل إليه سيراميك الحائط.")
                    NumberField("زيادة للهالك والقص",waste,{waste=it},"%","تضاف لكمية الشراء فقط.")
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedButton(onClick={step=0},modifier=Modifier.weight(1f)){Text("السابق")}
                        Button(onClick={step=2},modifier=Modifier.weight(1f)){Text("التالي: البنود")}
                    }
                }
                2 -> SurfaceCard{
                    SectionHeading("الأعمال الموجودة","فعّل فقط الأعمال التي ستنفذ في هذه الغرفة.")
                    workTypes.forEach{work->
                        ExplainedToggle(work,workHelp[work].orEmpty(),selected[work]==true){selected[work]=it}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedButton(onClick={step=1},modifier=Modifier.weight(1f)){Text("السابق")}
                        Button(onClick={step=3},modifier=Modifier.weight(1f)){Text("عرض النتيجة")}
                    }
                }
                else -> SurfaceCard{
                    SectionHeading("نتيجة ${roomName.ifBlank{spaceType}}",if(repeats>1)"الكميات تشمل التكرار × $repeats" else "راجع الكميات قبل الحفظ.")
                    WarningBox(warnings)
                    if(l<=0 || w<=0){
                        Text("أدخل الطول والعرض أولًا.",color=MaterialTheme.colorScheme.error)
                    }else{
                        if(selected["محارة الحوائط"]==true) QuantityRow("محارة الحوائط",netWalls,"م²")
                        if(selected["محارة السقف"]==true) QuantityRow("محارة السقف",floor,"م²")
                        if(selected["دهان الحوائط"]==true) QuantityRow("دهان الحوائط",netWalls,"م²")
                        if(selected["دهان السقف"]==true) QuantityRow("دهان السقف",floor,"م²")
                        if(selected["الأرضيات"]==true) QuantityRow("أرضيات بالهالك",floor*factor,"م²")
                        if(selected["سيراميك الحوائط"]==true) QuantityRow("سيراميك حوائط بالهالك",wallTile,"م²")
                        if(selected["الوزرات"]==true) QuantityRow("وزرات بالهالك",skirting,"م ط")
                        if(selected["عزل الأرضية"]==true) QuantityRow("عزل الأرضية",waterproof,"م²")
                        if(selected["سقف جبس بورد"]==true) QuantityRow("سقف جبس بورد بالهالك",floor*factor,"م²")
                        HorizontalDivider()
                        HelpButton(
                            "طريقة حساب الحوائط",
                            "محيط الغرفة = 2 × (${qty(l)} + ${qty(w)}) = ${qty(2*(l+w))} م\n"+
                            "مساحة الحوائط قبل الخصم = ${qty(2*(l+w))} × ${qty(h)} = ${qty(grossOne)} م²\n"+
                            "الفتحات = ${qty(dArea+winArea)} م²\n"+
                            "الصافي للغرفة الواحدة = ${qty((grossOne-dArea-winArea).coerceAtLeast(0.0))} م²"+
                            if(repeats>1)"\nثم × $repeats غرف = ${qty(netWalls)} م²" else ""
                        )
                        if(note.isNotBlank()) Text("ملاحظة: $note",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

                        if(onSave!=null){
                            Button(
                                onClick={
                                    val s=buildSpace()
                                    store.clearDraft(draftKey)
                                    onSave(s)
                                },
                                modifier=Modifier.fillMaxWidth()
                            ){Text(if(initialSpace==null)"حفظ الغرفة" else "حفظ التعديلات")}
                        }
                        if(onSaveAndContinue!=null && initialSpace==null){
                            OutlinedButton(
                                onClick={
                                    val s=buildSpace()
                                    onSaveAndContinue(s)
                                    store.clearDraft(draftKey)
                                    resetForNext()
                                },
                                modifier=Modifier.fillMaxWidth()
                            ){Text("حفظ وإضافة غرفة أخرى بنفس البنود")}
                        }
                    }
                    OutlinedButton(onClick={step=0},modifier=Modifier.fillMaxWidth()){Text("تعديل البيانات")}
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
fun ItemCalculatorScreen(){
    val context=LocalContext.current
    val store=remember{ProjectStore(context.applicationContext)}
    var step by remember{mutableStateOf(0)}
    var item by remember{mutableStateOf(itemTypes.first())}
    var method by remember{mutableStateOf("غرفة كاملة")}
    var length by remember{mutableStateOf("")}
    var width by remember{mutableStateOf("")}
    var height by remember{mutableStateOf(qty(store.lastHeight()))}
    var count by remember{mutableStateOf("1")}
    var directArea by remember{mutableStateOf("")}
    var thickness by remember{mutableStateOf("0.12")}
    var upstand by remember{mutableStateOf("0.20")}
    var waste by remember{mutableStateOf("7")}
    val openings=remember{mutableStateListOf<OpeningDraft>()}
    val walls=remember{mutableStateListOf(WallDraft(height=qty(store.lastHeight())))}

    val wallBased=item in listOf("محارة الحوائط","دهان الحوائط","سيراميك الحوائط","المباني")
    val flatBased=item in listOf("محارة السقف","دهان السقف","الأرضيات","عزل الأرضية","سقف جبس بورد")
    val methodOptions=when{
        wallBased -> listOf("غرفة كاملة","عدة حوائط","مساحة جاهزة")
        item=="الوزرات" -> listOf("غرفة كاملة","طول جاهز")
        else -> listOf("غرفة أو مسطح","مساحة جاهزة")
    }
    LaunchedEffect(item){method=methodOptions.first()}

    val l=num(length); val w=num(width); val h=num(height)
    val n=num(count).toInt().coerceAtLeast(1)
    val factor=1+num(waste)/100.0
    val openingsArea=openingArea(openings)
    val roomWallArea=(2*(l+w)*h-openingsArea).coerceAtLeast(0.0)*n
    val multiWallArea=walls.sumOf{(num(it.length)*num(it.height)-num(it.openingArea)).coerceAtLeast(0.0)}
    val baseWallArea=when(method){
        "غرفة كاملة"->roomWallArea
        "عدة حوائط"->multiWallArea
        else->num(directArea)
    }
    val flatArea=if(method=="مساحة جاهزة") num(directArea) else l*w*n
    val skirting=if(method=="طول جاهز") num(directArea) else (2*(l+w)-doorWidth(openings)).coerceAtLeast(0.0)*n

    val result=when(item){
        "محارة الحوائط","دهان الحوائط" -> baseWallArea
        "سيراميك الحوائط" -> baseWallArea*factor
        "المباني" -> baseWallArea
        "محارة السقف","دهان السقف" -> flatArea
        "الأرضيات","سقف جبس بورد" -> flatArea*factor
        "عزل الأرضية" -> if(method=="مساحة جاهزة") flatArea*factor else (flatArea+2*(l+w)*num(upstand)*n)*factor
        "الوزرات" -> skirting*factor
        else -> 0.0
    }

    val warnings=buildList{
        if(h>8) add("ارتفاع الحائط أكبر من 8 م.")
        if(l>50 || w>50) add("أحد الأبعاد كبير جدًا؛ تأكد أن الوحدة بالمتر.")
        if(method=="غرفة كاملة" && 2*(l+w)*h>0 && openingsArea>=2*(l+w)*h) add("مساحة الفتحات أكبر من مساحة الحوائط.")
        walls.forEachIndexed{i,wall->
            val gross=num(wall.length)*num(wall.height)
            if(gross>0 && num(wall.openingArea)>gross) add("فتحات الحائط ${i+1} أكبر من مساحته.")
        }
    }

    fun formula():String=when{
        wallBased && method=="غرفة كاملة" ->
            "المحيط = 2 × (${qty(l)} + ${qty(w)})\nالمسطح = المحيط × ${qty(h)} - ${qty(openingsArea)} فتحات"+
            if(n>1)"\nثم × $n فراغات" else ""+
            if(item=="سيراميك الحوائط")"\nثم إضافة هالك ${qty(num(waste))}%." else ""
        wallBased && method=="عدة حوائط" ->
            walls.mapIndexed{i,x->"حائط ${i+1}: ${qty(num(x.length))} × ${qty(num(x.height))} - ${qty(num(x.openingArea))} = ${qty((num(x.length)*num(x.height)-num(x.openingArea)).coerceAtLeast(0.0))} م²"}.joinToString("\n")+
            "\nالإجمالي = ${qty(multiWallArea)} م²"
        method=="مساحة جاهزة" || method=="طول جاهز" -> "تم استخدام الكمية المدخلة مباشرة: ${qty(num(directArea))}."
        else -> "المساحة = ${qty(l)} × ${qty(w)} × $n = ${qty(l*w*n)} م²."
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=11.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)
    ){
        item{StepTabs(listOf("البند","طريقة الحصر","النتيجة"),step){step=it}}
        item{
            when(step){
                0 -> SurfaceCard{
                    SectionHeading("اختر البند","ابدأ بالبند الذي تريد حصره.")
                    SelectField("البند المطلوب",item,itemTypes,{item=it},"يغير طريقة القياس والمدخلات المطلوبة.")
                    Button(onClick={step=1},modifier=Modifier.fillMaxWidth()){Text("التالي")}
                }
                1 -> SurfaceCard{
                    SectionHeading("طريقة حصر $item","اختر الطريقة الأقرب لشكل المكان في الموقع.")
                    SelectField("طريقة الإدخال",method,methodOptions,{method=it},"غرفة كاملة للأربع حوائط، وعدة حوائط للصالة أو الشكل غير المنتظم، أو كمية جاهزة لو لديك المسطح.")
                    when{
                        wallBased && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Box(Modifier.weight(1f)){NumberField("طول الغرفة",length,{length=it},"م","الطول الصافي.")}
                                Box(Modifier.weight(1f)){NumberField("عرض الغرفة",width,{width=it},"م","العرض الصافي.")}
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Box(Modifier.weight(1f)){NumberField("ارتفاع الحائط",height,{height=it;store.setLastHeight(num(it))},"م","الارتفاع الصافي.")}
                                Box(Modifier.weight(1f)){NumberField("عدد الفراغات",count,{count=it},"عدد","لو نفس الغرفة مكررة.")}
                            }
                            OpeningEditor(openings)
                        }
                        wallBased && method=="عدة حوائط" -> {
                            walls.forEachIndexed{i,wall->
                                Surface(
                                    shape=RoundedCornerShape(10.dp),
                                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.42f),
                                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                ){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text("حائط ${i+1}",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                                            Text("${qty((num(wall.length)*num(wall.height)-num(wall.openingArea)).coerceAtLeast(0.0))} م²",color=MaterialTheme.colorScheme.tertiary)
                                            if(walls.size>1) IconButton(onClick={walls.removeAt(i)}){Icon(Icons.Rounded.Delete,"حذف")}
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                            Box(Modifier.weight(1f)){NumberField("الطول",wall.length,{v->walls[i]=wall.copy(length=v)},"م","طول هذا الحائط.")}
                                            Box(Modifier.weight(1f)){NumberField("الارتفاع",wall.height,{v->walls[i]=wall.copy(height=v);store.setLastHeight(num(v))},"م","ارتفاع هذا الحائط.")}
                                        }
                                        NumberField("مساحة الفتحات بالحائط",wall.openingArea,{v->walls[i]=wall.copy(openingArea=v)},"م²","إجمالي أبواب وشبابيك هذا الحائط.")
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick={walls.add(WallDraft(height=height.ifBlank{qty(store.lastHeight())}))},
                                modifier=Modifier.fillMaxWidth()
                            ){Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("إضافة حائط")}
                        }
                        wallBased && method=="مساحة جاهزة" -> NumberField("المساحة الصافية",directArea,{directArea=it},"م²","اكتب المساحة النهائية مباشرة.")
                        flatBased && method=="غرفة أو مسطح" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Box(Modifier.weight(1f)){NumberField("الطول",length,{length=it},"م","طول المسطح.")}
                                Box(Modifier.weight(1f)){NumberField("العرض",width,{width=it},"م","عرض المسطح.")}
                            }
                            NumberField("عدد المسطحات",count,{count=it},"عدد","لو نفس المقاس مكرر.")
                            if(item=="عزل الأرضية") NumberField("ارتفاع رجوع العزل",upstand,{upstand=it},"م","ارتفاع العزل على الحائط.")
                        }
                        flatBased && method=="مساحة جاهزة" -> NumberField("المساحة الصافية",directArea,{directArea=it},"م²","أدخل المساحة مباشرة.")
                        item=="الوزرات" && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Box(Modifier.weight(1f)){NumberField("الطول",length,{length=it},"م","طول الغرفة.")}
                                Box(Modifier.weight(1f)){NumberField("العرض",width,{width=it},"م","عرض الغرفة.")}
                            }
                            NumberField("عدد الغرف",count,{count=it},"عدد","عدد الغرف بنفس المقاس.")
                            OpeningEditor(openings)
                        }
                        item=="الوزرات" -> NumberField("الطول الصافي",directArea,{directArea=it},"م ط","أدخل طول الوزرات مباشرة.")
                    }
                    if(item in listOf("الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد")){
                        NumberField("زيادة للهالك والقص",waste,{waste=it},"%","زيادة على كمية الشراء.")
                    }
                    if(item=="المباني"){
                        NumberField("سمك الحائط",thickness,{thickness=it},"م","مثال: 12 سم = 0.12 م.")
                    }
                    WarningBox(warnings)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedButton(onClick={step=0},modifier=Modifier.weight(1f)){Text("السابق")}
                        Button(onClick={step=2},modifier=Modifier.weight(1f)){Text("عرض النتيجة")}
                    }
                }
                else -> SurfaceCard{
                    SectionHeading("نتيجة $item","الكمية محسوبة بالطريقة التي اخترتها.")
                    WarningBox(warnings)
                    if(item=="المباني"){
                        QuantityRow("مساحة المباني",result,"م²")
                        QuantityRow("حجم المباني",result*num(thickness),"م³")
                    }else QuantityRow(item,result,if(item=="الوزرات")"م ط" else "م²")
                    HorizontalDivider()
                    HelpButton("طريقة الحساب",formula())
                    OutlinedButton(onClick={step=1},modifier=Modifier.fillMaxWidth()){Text("تعديل المقاسات")}
                }
            }
        }
    }
}

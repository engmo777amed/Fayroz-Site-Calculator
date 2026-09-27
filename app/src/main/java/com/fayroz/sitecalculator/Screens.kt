package com.fayroz.sitecalculator

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
import com.fayroz.sitecalculator.ui.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

private val spaceTypes=listOf("غرفة نوم","ريسبشن","حمام","مطبخ","ممر","بلكونة","مخزن","فراغ آخر")
private val workTypes=QuantityEngine.workOrder
private val workHelp=mapOf(
    "محارة الحوائط" to "مساحة الحوائط الصافية بعد خصم الأبواب والشبابيك.",
    "محارة السقف" to "مساحة السقف = الطول × العرض.",
    "دهان الحوائط" to "مساحة الحوائط الصافية بعد خصم الفتحات.",
    "دهان السقف" to "مساحة السقف.",
    "الأرضيات" to "مساحة الأرضية مع نسبة القص والهالك.",
    "سيراميك الحوائط" to "محيط الفراغ × ارتفاع السيراميك - الفتحات + الهالك.",
    "الوزرات" to "محيط الغرفة - عروض الأبواب + الهالك.",
    "عزل الأرضية" to "مساحة الأرضية + رجوع 20 سم على الحوائط + الهالك.",
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
    return if(n==1) type else "$type $n"
}

private fun openingArea(list:List<OpeningDraft>)=list.sumOf{
    num(it.width)*num(it.height)*num(it.count).toInt().coerceAtLeast(1)
}
private fun doorWidth(list:List<OpeningDraft>)=list.filter{it.type=="باب"}.sumOf{
    num(it.width)*num(it.count).toInt().coerceAtLeast(1)
}
private fun toOpenings(list:List<OpeningDraft>)=list.mapNotNull{
    val w=num(it.width)
    val h=num(it.height)
    val c=num(it.count).toInt().coerceAtLeast(1)
    if(w>0 && h>0) Opening(id=it.id,type=it.type,width=w,height=h,count=c) else null
}

@Composable
private fun OpeningEditor(
    openings:MutableList<OpeningDraft>,
    onChanged:()->Unit={}
){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        if(openings.isEmpty()){
            Text("لا توجد فتحات مسجلة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        openings.forEachIndexed{index,o->
            Surface(
                shape=RoundedCornerShape(13.dp),
                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.40f),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(Modifier.padding(horizontal=8.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("${o.type} ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                        Text(
                            "${qty(num(o.width)*num(o.height)*num(o.count).toInt().coerceAtLeast(1))} م²",
                            style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick={openings.removeAt(index);onChanged()},
                            modifier=Modifier.size(30.dp)
                        ){Icon(Icons.Rounded.Delete,"حذف",Modifier.size(17.dp))}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        NumberField("العرض",o.width,{v->openings[index]=o.copy(width=v);onChanged()},"م","عرض الفتحة.",Modifier.weight(1f))
                        NumberField("الارتفاع",o.height,{v->openings[index]=o.copy(height=v);onChanged()},"م","ارتفاع الفتحة.",Modifier.weight(1f))
                        NumberField("العدد",o.count,{v->openings[index]=o.copy(count=v);onChanged()},"","عدد الفتحات.",Modifier.weight(.78f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="باب"));onChanged()},
                modifier=Modifier.weight(1f)
            ){Icon(Icons.Rounded.DoorFront,null);Spacer(Modifier.width(4.dp));Text("باب")}
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="شباك"));onChanged()},
                modifier=Modifier.weight(1f)
            ){Icon(Icons.Rounded.Window,null);Spacer(Modifier.width(4.dp));Text("شباك")}
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
    val draft=remember(draftKey){
        if(initialSpace==null) runCatching{JSONObject(store.loadDraft(draftKey)?:"{}")}.getOrNull() else null
    }

    val initialType=initialSpace?.type ?: draft?.optString("type","غرفة نوم") ?: "غرفة نوم"
    var roomName by remember{mutableStateOf(initialSpace?.name ?: draft?.optString("name",nextRoomName(initialType,existingSpaces)) ?: nextRoomName(initialType,existingSpaces))}
    var autoName by remember{mutableStateOf(initialSpace==null && (draft?.optString("name","").isNullOrBlank()))}
    var spaceType by remember{mutableStateOf(initialType)}
    var length by remember{mutableStateOf(initialSpace?.length?.let(::qty) ?: draft?.optString("length","") ?: "")}
    var width by remember{mutableStateOf(initialSpace?.width?.let(::qty) ?: draft?.optString("width","") ?: "")}
    var height by remember{mutableStateOf(initialSpace?.height?.let(::qty) ?: draft?.optString("height",qty(store.lastHeight())) ?: qty(store.lastHeight()))}
    var repeatCount by remember{mutableStateOf(initialSpace?.repeatCount?.toString() ?: draft?.optString("repeat","1") ?: "1")}
    var tileHeight by remember{mutableStateOf(initialSpace?.tileHeight?.let(::qty) ?: draft?.optString("tileHeight","2.40") ?: "2.40")}
    var waste by remember{mutableStateOf(initialSpace?.waste?.let(::qty) ?: draft?.optString("waste","7") ?: "7")}
    var note by remember{mutableStateOf(initialSpace?.note ?: draft?.optString("note","") ?: "")}
    var step by remember{mutableIntStateOf(0)}

    val openings=remember{
        mutableStateListOf<OpeningDraft>().apply{
            when{
                initialSpace!=null && initialSpace.openings.isNotEmpty() -> initialSpace.openings.forEach{
                    add(OpeningDraft(it.id,it.type,qty(it.width),qty(it.height),it.count.toString()))
                }
                draft!=null -> {
                    val arr=draft.optJSONArray("openings") ?: JSONArray()
                    for(i in 0 until arr.length()){
                        val o=arr.optJSONObject(i) ?: continue
                        add(OpeningDraft(
                            o.optString("id",UUID.randomUUID().toString()),
                            o.optString("type","باب"),
                            o.optString("width",""),
                            o.optString("height",""),
                            o.optString("count","1")
                        ))
                    }
                }
            }
        }
    }
    val selected=remember{
        mutableStateMapOf<String,Boolean>().apply{
            workTypes.forEach{put(it,initialSpace?.works?.contains(it)==true)}
            if(initialSpace==null && draft!=null){
                val wa=draft.optJSONArray("works") ?: JSONArray()
                workTypes.forEach{put(it,false)}
                for(i in 0 until wa.length()) put(wa.optString(i),true)
            }
        }
    }

    fun saveDraft(){
        if(initialSpace!=null) return
        val o=JSONObject().apply{
            put("name",roomName);put("type",spaceType)
            put("length",length);put("width",width);put("height",height)
            put("repeat",repeatCount);put("tileHeight",tileHeight);put("waste",waste);put("note",note)
            put("works",JSONArray(workTypes.filter{selected[it]==true}))
            put("openings",JSONArray().apply{
                openings.forEach{x->
                    put(JSONObject().apply{
                        put("id",x.id);put("type",x.type);put("width",x.width);put("height",x.height);put("count",x.count)
                    })
                }
            })
        }
        store.saveDraft(draftKey,o.toString())
    }

    val l=num(length)
    val w=num(width)
    val h=num(height)
    val repeats=num(repeatCount).toInt().coerceAtLeast(1)
    val dArea=openingArea(openings.filter{it.type=="باب"})
    val winArea=openingArea(openings.filter{it.type=="شباك"})
    val dWidth=doorWidth(openings)
    val factor=1+num(waste)/100.0
    val floor=l*w*repeats
    val grossOne=2*(l+w)*h
    val netWalls=(grossOne-dArea-winArea).coerceAtLeast(0.0)*repeats
    val wallTile=((2*(l+w)*min(num(tileHeight),h)-dArea-winArea).coerceAtLeast(0.0))*repeats*factor
    val skirting=(2*(l+w)-dWidth).coerceAtLeast(0.0)*repeats*factor
    val waterproof=(l*w+2*(l+w)*0.20)*repeats*factor

    val warnings=buildList{
        if(h>8) add("ارتفاع الحائط أكبر من 8 م.")
        if(l>50 || w>50) add("أحد الأبعاد كبير جدًا؛ تأكد أن الوحدة بالمتر.")
        if(grossOne>0 && dArea+winArea>=grossOne) add("مساحة الفتحات أكبر من مساحة الحوائط.")
        if(num(waste)>30) add("نسبة الهالك أكبر من 30%.")
    }

    fun buildSpace()=SavedSpace(
        id=initialSpace?.id ?: UUID.randomUUID().toString(),
        name=roomName.trim().ifBlank{spaceType},
        type=spaceType,
        length=l,width=w,height=h,
        doorArea=dArea,windowArea=winArea,doorWidth=dWidth,
        tileHeight=num(tileHeight),waste=num(waste),
        works=workTypes.filter{selected[it]==true},
        openings=toOpenings(openings),
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
        saveDraft()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=11.dp,vertical=9.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        if(!projectContext.isNullOrBlank()){
            item{
                Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.primaryContainer){
                    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Rounded.FolderOpen,null,Modifier.size(17.dp),tint=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(projectContext,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        if(initialSpace==null && draft!=null && draft.length()>0){
            item{
                Surface(shape=RoundedCornerShape(11.dp),color=MaterialTheme.colorScheme.secondaryContainer){
                    Text("تم استعادة آخر بيانات غير محفوظة.",Modifier.fillMaxWidth().padding(8.dp),style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item{StepTabs(listOf("الفراغ","الفتحات","البنود","النتيجة"),step){step=it}}
        item{
            when(step){
                0 -> SurfaceCard{
                    SectionHeader("بيانات الفراغ","المقاسات الصافية داخل المكان.")
                    TextFieldSimple(
                        "اسم الغرفة أو الفراغ",roomName,
                        {roomName=it;autoName=false;saveDraft()},
                        "اسم واضح مثل: غرفة نوم رئيسية أو حمام الضيوف.",
                        "مثال: غرفة نوم رئيسية"
                    )
                    SelectField(
                        "نوع الفراغ",spaceType,spaceTypes,
                        {newType->
                            spaceType=newType
                            if(autoName) roomName=nextRoomName(newType,existingSpaces)
                            saveDraft()
                        },
                        "يساعد في ترتيب الغرف واقتراح الاسم تلقائيًا."
                    )
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberField("الطول",length,{length=it;saveDraft()},"م","الطول الصافي.",Modifier.weight(1f))
                        NumberField("العرض",width,{width=it;saveDraft()},"م","العرض الصافي.",Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberField("الارتفاع",height,{height=it;store.setLastHeight(num(it));saveDraft()},"م","من الأرضية للسقف.",Modifier.weight(1f))
                        NumberField("التكرار",repeatCount,{repeatCount=it;saveDraft()},"عدد","لو نفس الفراغ مكرر.",Modifier.weight(1f))
                    }
                    TextFieldSimple("ملاحظات",note,{note=it;saveDraft()},"ملاحظة تنفيذية تحفظ مع الغرفة.","مثال: السيراميك حتى 2.20 م")
                    WarningBox(warnings)
                    Button(onClick={step=1},modifier=Modifier.fillMaxWidth()){
                        Text("التالي: الفتحات")
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Rounded.ArrowBack,null)
                    }
                }
                1 -> SurfaceCard{
                    SectionHeader("الأبواب والشبابيك","أدخل المقاس والعدد، وسيتم الخصم تلقائيًا.")
                    OpeningEditor(openings){saveDraft()}
                    HorizontalDivider()
                    QuantityRow("إجمالي مساحة الفتحات",dArea+winArea,"م²",true)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberField("ارتفاع السيراميك",tileHeight,{tileHeight=it;saveDraft()},"م","ارتفاع سيراميك الحائط.",Modifier.weight(1f))
                        NumberField("الهالك والقص",waste,{waste=it;saveDraft()},"%","يضاف لكمية الشراء.",Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedButton(onClick={step=0},modifier=Modifier.weight(1f)){Text("السابق")}
                        Button(onClick={step=2},modifier=Modifier.weight(1f)){Text("التالي: البنود")}
                    }
                }
                2 -> SurfaceCard{
                    SectionHeader("البنود المطلوبة","فعّل الأعمال الموجودة فقط في هذا الفراغ.")
                    workTypes.chunked(2).forEach{row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.Top){
                            row.forEach{work->
                                Box(Modifier.weight(1f)){
                                    ExplainedToggle(
                                        work,
                                        workHelp[work].orEmpty(),
                                        selected[work]==true
                                    ){selected[work]=it;saveDraft()}
                                }
                            }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedButton(onClick={step=1},modifier=Modifier.weight(1f)){Text("السابق")}
                        Button(onClick={step=3},modifier=Modifier.weight(1f)){Text("عرض النتيجة")}
                    }
                }
                else -> SurfaceCard{
                    SectionHeader(
                        "نتيجة ${roomName.ifBlank{spaceType}}",
                        if(repeats>1)"الكميات تشمل التكرار × $repeats" else "راجع الكميات قبل الحفظ."
                    )
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
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("طريقة حساب الحوائط",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                            HelpButton(
                                "طريقة حساب الحوائط",
                                "المحيط = 2 × (${qty(l)} + ${qty(w)}) = ${qty(2*(l+w))} م\n"+
                                    "قبل الخصم = ${qty(2*(l+w))} × ${qty(h)} = ${qty(grossOne)} م²\n"+
                                    "الفتحات = ${qty(dArea+winArea)} م²\n"+
                                    "الصافي = ${qty((grossOne-dArea-winArea).coerceAtLeast(0.0))} م² للفراغ الواحد."
                            )
                        }
                        if(note.isNotBlank()) Text("ملاحظة: $note",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

                        if(onSave!=null){
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Button(
                                    onClick={
                                        val s=buildSpace()
                                        store.clearDraft(draftKey)
                                        onSave(s)
                                    },
                                    modifier=Modifier.weight(1f)
                                ){
                                    Icon(Icons.Rounded.Save,null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if(initialSpace==null)"حفظ" else "حفظ التعديل")
                                }
                                if(onSaveAndContinue!=null && initialSpace==null){
                                    OutlinedButton(
                                        onClick={
                                            val s=buildSpace()
                                            onSaveAndContinue(s)
                                            store.clearDraft(draftKey)
                                            resetForNext()
                                        },
                                        modifier=Modifier.weight(1f)
                                    ){
                                        Icon(Icons.Rounded.Add,null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("حفظ + غرفة")
                                    }
                                }
                            }
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
    var step by remember{mutableIntStateOf(0)}
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
    LaunchedEffect(item){
        method=methodOptions.first()
        step=0
    }

    val l=num(length)
    val w=num(width)
    val h=num(height)
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
                (if(n>1)"\nثم × $n فراغات" else "")+
                (if(item=="سيراميك الحوائط")"\nثم إضافة هالك ${qty(num(waste))}%." else "")
        wallBased && method=="عدة حوائط" ->
            walls.mapIndexed{i,x->
                "حائط ${i+1}: ${qty(num(x.length))} × ${qty(num(x.height))} - ${qty(num(x.openingArea))} = ${qty((num(x.length)*num(x.height)-num(x.openingArea)).coerceAtLeast(0.0))} م²"
            }.joinToString("\n")+"\nالإجمالي = ${qty(multiWallArea)} م²"
        method=="مساحة جاهزة" || method=="طول جاهز" ->
            "تم استخدام الكمية المدخلة مباشرة: ${qty(num(directArea))}."
        else -> "المساحة = ${qty(l)} × ${qty(w)} × $n = ${qty(l*w*n)} م²."
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=11.dp,vertical=9.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        item{StepTabs(listOf("البند","القياس","النتيجة"),step){step=it}}
        item{
            when(step){
                0 -> SurfaceCard{
                    SectionHeader("اختيار البند","اختر البند وطريقة الحصر المناسبة للموقع.")
                    SelectField("البند المطلوب",item,itemTypes,{item=it},"يغير المدخلات وطريقة الحساب.")
                    SelectField(
                        "طريقة الحصر",method,methodOptions,{method=it},
                        "غرفة كاملة للأربع حوائط، عدة حوائط للصالة أو الشكل غير المنتظم، أو كمية جاهزة."
                    )
                    Button(onClick={step=1},modifier=Modifier.fillMaxWidth()){Text("إدخال المقاسات")}
                }
                1 -> SurfaceCard{
                    SectionHeader("مقاسات $item","الحقول المطلوبة تتغير حسب طريقة الحصر.")
                    when{
                        wallBased && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                NumberField("الطول",length,{length=it},"م","الطول الصافي.",Modifier.weight(1f))
                                NumberField("العرض",width,{width=it},"م","العرض الصافي.",Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                NumberField("الارتفاع",height,{height=it;store.setLastHeight(num(it))},"م","الارتفاع الصافي.",Modifier.weight(1f))
                                NumberField("التكرار",count,{count=it},"عدد","عدد الفراغات بنفس المقاس.",Modifier.weight(1f))
                            }
                            OpeningEditor(openings)
                        }
                        wallBased && method=="عدة حوائط" -> {
                            Text(
                                "أدخل الحوائط المتتابعة واحدًا وراء الآخر؛ البرنامج يجمع الصافي تلقائيًا.",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            walls.forEachIndexed{i,wall->
                                Surface(
                                    shape=RoundedCornerShape(13.dp),
                                    color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.40f),
                                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                ){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text("حائط ${i+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                                            Text(
                                                "${qty((num(wall.length)*num(wall.height)-num(wall.openingArea)).coerceAtLeast(0.0))} م²",
                                                color=MaterialTheme.colorScheme.primary,
                                                style=MaterialTheme.typography.labelLarge
                                            )
                                            if(walls.size>1){
                                                IconButton(onClick={walls.removeAt(i)},modifier=Modifier.size(30.dp)){
                                                    Icon(Icons.Rounded.Delete,"حذف",Modifier.size(17.dp))
                                                }
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                            NumberField("الطول",wall.length,{v->walls[i]=wall.copy(length=v)},"م","طول الحائط.",Modifier.weight(1f))
                                            NumberField("الارتفاع",wall.height,{v->walls[i]=wall.copy(height=v);store.setLastHeight(num(v))},"م","ارتفاع الحائط.",Modifier.weight(1f))
                                        }
                                        NumberField("مساحة الفتحات",wall.openingArea,{v->walls[i]=wall.copy(openingArea=v)},"م²","إجمالي أبواب وشبابيك هذا الحائط.")
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick={walls.add(WallDraft(height=height.ifBlank{qty(store.lastHeight())}))},
                                modifier=Modifier.fillMaxWidth()
                            ){
                                Icon(Icons.Rounded.Add,null)
                                Spacer(Modifier.width(4.dp))
                                Text("إضافة حائط")
                            }
                            QuantityRow("إجمالي الحوائط",multiWallArea,"م²",true)
                        }
                        wallBased && method=="مساحة جاهزة" ->
                            NumberField("المساحة الصافية",directArea,{directArea=it},"م²","اكتب المساحة النهائية مباشرة.")
                        flatBased && method=="غرفة أو مسطح" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                NumberField("الطول",length,{length=it},"م","طول المسطح.",Modifier.weight(1f))
                                NumberField("العرض",width,{width=it},"م","عرض المسطح.",Modifier.weight(1f))
                            }
                            NumberField("عدد المسطحات",count,{count=it},"عدد","لو نفس المقاس مكرر.")
                            if(item=="عزل الأرضية"){
                                NumberField("ارتفاع رجوع العزل",upstand,{upstand=it},"م","ارتفاع العزل على الحائط.")
                            }
                        }
                        flatBased && method=="مساحة جاهزة" ->
                            NumberField("المساحة الصافية",directArea,{directArea=it},"م²","أدخل المساحة مباشرة.")
                        item=="الوزرات" && method=="غرفة كاملة" -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                NumberField("الطول",length,{length=it},"م","طول الغرفة.",Modifier.weight(1f))
                                NumberField("العرض",width,{width=it},"م","عرض الغرفة.",Modifier.weight(1f))
                            }
                            NumberField("عدد الغرف",count,{count=it},"عدد","عدد الغرف بنفس المقاس.")
                            OpeningEditor(openings)
                        }
                        item=="الوزرات" ->
                            NumberField("الطول الصافي",directArea,{directArea=it},"م ط","أدخل طول الوزرات مباشرة.")
                    }
                    if(item in listOf("الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد")){
                        NumberField("الهالك والقص",waste,{waste=it},"%","زيادة على كمية الشراء.")
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
                    SectionHeader("نتيجة $item","الكمية محسوبة بالطريقة المختارة.")
                    WarningBox(warnings)
                    if(item=="المباني"){
                        QuantityRow("مساحة المباني",result,"م²",true)
                        QuantityRow("حجم المباني",result*num(thickness),"م³")
                    }else{
                        QuantityRow(item,result,if(item=="الوزرات")"م ط" else "م²",true)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("طريقة الحساب",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                        HelpButton("طريقة الحساب",formula())
                    }
                    OutlinedButton(onClick={step=1},modifier=Modifier.fillMaxWidth()){
                        Icon(Icons.Rounded.Edit,null)
                        Spacer(Modifier.width(4.dp))
                        Text("تعديل المقاسات")
                    }
                }
            }
        }
    }
}

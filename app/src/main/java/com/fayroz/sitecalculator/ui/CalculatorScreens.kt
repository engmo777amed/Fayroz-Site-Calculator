@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.fayroz.sitecalculator.data.*
import java.util.UUID

private val roomTypes=listOf("غرفة نوم","ريسبشن","حمام","مطبخ","ممر","بلكونة","مخزن","فراغ آخر")

data class OpeningDraft(
    val id:String=UUID.randomUUID().toString(),
    val type:String="باب",
    val width:String="",
    val height:String="",
    val count:String="1"
)

private fun nextRoomName(type:String,existing:List<Room>):String{
    val n=existing.count{it.type==type}+1
    return if(n==1)type else type+" "+n
}

@Composable
fun RoomEditor(
    title:String,
    initial:Room?,
    existing:List<Room>,
    lastHeight:Double,
    onHeight:(Double)->Unit,
    onBack:()->Unit,
    onSave:(Room)->Unit,
    onSaveMore:((Room)->Unit)?=null
){
    var step by remember{mutableIntStateOf(0)}
    var type by remember{mutableStateOf(initial?.type?:"غرفة نوم")}
    var name by remember{mutableStateOf(initial?.name?:nextRoomName(type,existing))}
    var length by remember{mutableStateOf(initial?.length?.let(::fmt)?:"")}
    var width by remember{mutableStateOf(initial?.width?.let(::fmt)?:"")}
    var height by remember{mutableStateOf(initial?.height?.let(::fmt)?:fmt(lastHeight))}
    var tileHeight by remember{mutableStateOf(initial?.tileHeight?.let(::fmt)?:"2.40")}
    var waste by remember{mutableStateOf(initial?.waste?.let(::fmt)?:"7")}
    var repeat by remember{mutableStateOf(initial?.repeat?.toString()?:"1")}
    var note by remember{mutableStateOf(initial?.note?:"")}

    val selected=remember{
        mutableStateMapOf<String,Boolean>().apply{
            QtyEngine.works.forEach{put(it,initial?.works?.contains(it)==true)}
        }
    }

    val openings=remember{
        mutableStateListOf<OpeningDraft>().apply{
            initial?.openings?.forEach{
                add(OpeningDraft(it.id,it.type,fmt(it.width),fmt(it.height),it.count.toString()))
            }
        }
    }

    fun buildRoom()=Room(
        id=initial?.id?:UUID.randomUUID().toString(),
        name=name.trim().ifBlank{type},
        type=type,
        length=parseNum(length),
        width=parseNum(width),
        height=parseNum(height),
        tileHeight=parseNum(tileHeight),
        waste=parseNum(waste),
        repeat=parseNum(repeat).toInt().coerceAtLeast(1),
        note=note.trim(),
        works=QtyEngine.works.filter{selected[it]==true}.toMutableList(),
        openings=openings.mapNotNull{o->
            val ow=parseNum(o.width)
            val oh=parseNum(o.height)
            if(ow>0&&oh>0){
                Opening(
                    id=o.id,
                    type=o.type,
                    width=ow,
                    height=oh,
                    count=parseNum(o.count).toInt().coerceAtLeast(1)
                )
            }else null
        }.toMutableList()
    )

    val preview=buildRoom()

    Scaffold(
        topBar={AppBar(title,"حساب وحفظ داخل المشروع",onBack)}
    ){pad->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding=PaddingValues(11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{Tabs(listOf("الفراغ","الفتحات","البنود","النتيجة"),step){step=it}}

            item{
                when(step){
                    0->CardBox{
                        SectionTitle("بيانات الفراغ","المقاسات الصافية داخل المكان")
                        Txt("اسم الغرفة",name,{name=it},"مثال: غرفة نوم رئيسية")
                        Choice(
                            "نوع الفراغ",type,roomTypes,
                            {new->
                                type=new
                                if(initial==null)name=nextRoomName(new,existing)
                            }
                        )
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Num("الطول",length,{length=it},"م",modifier=Modifier.weight(1f))
                            Num("العرض",width,{width=it},"م",modifier=Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Num("الارتفاع",height,{height=it;onHeight(parseNum(it))},"م",modifier=Modifier.weight(1f))
                            Num("التكرار",repeat,{repeat=it},"عدد",modifier=Modifier.weight(1f))
                        }
                        Txt("ملاحظات",note,{note=it},"مثال: السيراميك حتى 2.20 م")
                        Button(onClick={step=1},modifier=Modifier.fillMaxWidth()){Text("التالي: الفتحات")}
                    }

                    1->CardBox{
                        SectionTitle("الأبواب والشبابيك","الخصم يتم تلقائيًا من الحوائط")
                        OpeningEditor(openings)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Num("ارتفاع السيراميك",tileHeight,{tileHeight=it},"م",modifier=Modifier.weight(1f))
                            Num("الهالك",waste,{waste=it},"% ",modifier=Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            OutlinedButton(onClick={step=0},modifier=Modifier.weight(1f)){Text("السابق")}
                            Button(onClick={step=2},modifier=Modifier.weight(1f)){Text("التالي")}
                        }
                    }

                    2->CardBox{
                        SectionTitle("البنود المطلوبة","فعّل الأعمال الموجودة في هذا الفراغ")
                        QtyEngine.works.chunked(2).forEach{row->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                row.forEach{work->
                                    Surface(
                                        modifier=Modifier.weight(1f),
                                        onClick={selected[work]=!(selected[work]?:false)},
                                        shape=RoundedCornerShape(12.dp),
                                        color=if(selected[work]==true)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                                    ){
                                        Row(
                                            Modifier.padding(8.dp),
                                            verticalAlignment=Alignment.CenterVertically
                                        ){
                                            Checkbox(
                                                checked=selected[work]==true,
                                                onCheckedChange={selected[work]=it}
                                            )
                                            Text(work,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Bold)
                                        }
                                    }
                                }
                                if(row.size==1)Spacer(Modifier.weight(1f))
                            }
                        }
                        Button(onClick={step=3},modifier=Modifier.fillMaxWidth()){Text("عرض النتيجة")}
                    }

                    else->CardBox{
                        SectionTitle(
                            "نتيجة "+preview.name,
                            if(preview.repeat>1)"الكميات تشمل التكرار × "+preview.repeat else "راجع الكميات قبل الحفظ"
                        )
                        if(preview.length<=0||preview.width<=0){
                            Text("أدخل الطول والعرض أولًا.",color=MaterialTheme.colorScheme.error)
                        }else{
                            preview.works.forEach{work->
                                QtyLine(work,QtyEngine.quantity(preview,work),QtyEngine.unit(work))
                            }
                            HorizontalDivider()
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text("طريقة حساب الحوائط",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                                Help(
                                    "طريقة حساب الحوائط",
                                    "المحيط = 2 × (الطول + العرض). مساحة الحوائط = المحيط × الارتفاع، ثم يتم خصم الأبواب والشبابيك. البنود التي تحتاج هالك تُضاف لها النسبة المدخلة."
                                )
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Button(
                                    onClick={onSave(preview)},
                                    modifier=Modifier.weight(1f)
                                ){
                                    Icon(Icons.Rounded.Save,null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if(initial==null)"حفظ" else "حفظ التعديل")
                                }
                                if(onSaveMore!=null&&initial==null){
                                    OutlinedButton(
                                        onClick={onSaveMore(preview)},
                                        modifier=Modifier.weight(1f)
                                    ){
                                        Icon(Icons.Rounded.Add,null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("حفظ + غرفة")
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
}

@Composable
private fun OpeningEditor(openings:MutableList<OpeningDraft>){
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        openings.forEachIndexed{i,o->
            Surface(
                shape=RoundedCornerShape(13.dp),
                color=MaterialTheme.colorScheme.surfaceVariant,
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
            ){
                Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(o.type+" "+(i+1),Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Black)
                        Text(
                            fmt(parseNum(o.width)*parseNum(o.height)*parseNum(o.count).toInt().coerceAtLeast(1))+" م²",
                            style=MaterialTheme.typography.labelMedium,
                            color=MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick={openings.removeAt(i)},modifier=Modifier.size(30.dp)){
                            Icon(Icons.Rounded.Delete,"حذف",Modifier.size(17.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        Num("العرض",o.width,{v->openings[i]=o.copy(width=v)},"م",modifier=Modifier.weight(1f))
                        Num("الارتفاع",o.height,{v->openings[i]=o.copy(height=v)},"م",modifier=Modifier.weight(1f))
                        Num("العدد",o.count,{v->openings[i]=o.copy(count=v)},modifier=Modifier.weight(.75f))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="باب"))},
                modifier=Modifier.weight(1f)
            ){
                Icon(Icons.Rounded.DoorFront,null)
                Spacer(Modifier.width(4.dp))
                Text("باب")
            }
            OutlinedButton(
                onClick={openings.add(OpeningDraft(type="شباك"))},
                modifier=Modifier.weight(1f)
            ){
                Icon(Icons.Rounded.Window,null)
                Spacer(Modifier.width(4.dp))
                Text("شباك")
            }
        }
    }
}

@Composable
fun QuickRoomScreen(
    lastHeight:Double,
    onHeight:(Double)->Unit,
    onBack:()->Unit
){
    RoomEditor(
        title="حساب فراغ",
        initial=null,
        existing=emptyList(),
        lastHeight=lastHeight,
        onHeight=onHeight,
        onBack=onBack,
        onSave={}
    )
}

@Composable
fun ItemCalculatorScreen(
    lastHeight:Double,
    onHeight:(Double)->Unit,
    onBack:()->Unit
){
    val items=listOf(
        "محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف",
        "الأرضيات","سيراميك الحوائط","الوزرات","المباني","عزل الأرضية","سقف جبس بورد"
    )

    var item by remember{mutableStateOf(items.first())}
    var method by remember{mutableStateOf("غرفة كاملة")}
    var length by remember{mutableStateOf("")}
    var width by remember{mutableStateOf("")}
    var height by remember{mutableStateOf(fmt(lastHeight))}
    var count by remember{mutableStateOf("1")}
    var direct by remember{mutableStateOf("")}
    var waste by remember{mutableStateOf("7")}
    var thickness by remember{mutableStateOf("0.12")}

    val walls=remember{
        mutableStateListOf(Triple("",fmt(lastHeight),""))
    }

    val wallBased=item in listOf("محارة الحوائط","دهان الحوائط","سيراميك الحوائط","المباني")
    val methods=when{
        wallBased->listOf("غرفة كاملة","عدة حوائط","مساحة جاهزة")
        item=="الوزرات"->listOf("غرفة كاملة","طول جاهز")
        else->listOf("غرفة أو مسطح","مساحة جاهزة")
    }

    LaunchedEffect(item){
        method=methods.first()
    }

    val base=when{
        wallBased&&method=="غرفة كاملة"->
            2*(parseNum(length)+parseNum(width))*parseNum(height)*parseNum(count).toInt().coerceAtLeast(1)

        wallBased&&method=="عدة حوائط"->
            walls.sumOf{wall->
                (parseNum(wall.first)*parseNum(wall.second)-parseNum(wall.third)).coerceAtLeast(0.0)
            }

        method.contains("جاهز")->parseNum(direct)

        else->parseNum(length)*parseNum(width)*parseNum(count).toInt().coerceAtLeast(1)
    }

    val factor=1+parseNum(waste)/100.0
    val result=when(item){
        "الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد"->base*factor
        else->base
    }

    Scaffold(
        topBar={AppBar("حساب بند","غرفة كاملة أو عدة حوائط أو كمية جاهزة",onBack)}
    ){pad->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding=PaddingValues(11.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    SectionTitle("اختيار البند","اختر طريقة الحصر المناسبة للموقع")

                    Choice("البند",item,items,{item=it})

                    Choice(
                        "طريقة الحصر",
                        method,
                        methods,
                        {method=it},
                        "استخدم «عدة حوائط» للصالة أو أي شكل غير منتظم."
                    )

                    when{
                        wallBased&&method=="غرفة كاملة"->{
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Num("الطول",length,{length=it},"م",modifier=Modifier.weight(1f))
                                Num("العرض",width,{width=it},"م",modifier=Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Num("الارتفاع",height,{height=it;onHeight(parseNum(it))},"م",modifier=Modifier.weight(1f))
                                Num("التكرار",count,{count=it},modifier=Modifier.weight(1f))
                            }
                        }

                        wallBased&&method=="عدة حوائط"->{
                            Text(
                                "أدخل الحوائط المتتابعة واحدًا وراء الآخر؛ البرنامج يجمع الصافي تلقائيًا.",
                                style=MaterialTheme.typography.bodySmall,
                                color=MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            walls.forEachIndexed{i,wall->
                                Surface(
                                    shape=RoundedCornerShape(12.dp),
                                    color=MaterialTheme.colorScheme.surfaceVariant
                                ){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=Alignment.CenterVertically){
                                            Text("حائط "+(i+1),Modifier.weight(1f),fontWeight=FontWeight.Black)
                                            if(walls.size>1){
                                                IconButton(onClick={walls.removeAt(i)},modifier=Modifier.size(30.dp)){
                                                    Icon(Icons.Rounded.Delete,"حذف")
                                                }
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                            Num(
                                                "الطول",wall.first,
                                                {v->walls[i]=Triple(v,wall.second,wall.third)},
                                                "م",
                                                modifier=Modifier.weight(1f)
                                            )
                                            Num(
                                                "الارتفاع",wall.second,
                                                {v->
                                                    walls[i]=Triple(wall.first,v,wall.third)
                                                    onHeight(parseNum(v))
                                                },
                                                "م",
                                                modifier=Modifier.weight(1f)
                                            )
                                        }
                                        Num(
                                            "مساحة الفتحات",wall.third,
                                            {v->walls[i]=Triple(wall.first,wall.second,v)},
                                            "م²"
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick={walls.add(Triple("",fmt(lastHeight),""))},
                                modifier=Modifier.fillMaxWidth()
                            ){
                                Icon(Icons.Rounded.Add,null)
                                Spacer(Modifier.width(4.dp))
                                Text("إضافة حائط")
                            }
                        }

                        method.contains("جاهز")->{
                            Num(
                                if(method=="طول جاهز")"الطول الصافي" else "المساحة الصافية",
                                direct,
                                {direct=it},
                                if(method=="طول جاهز")"م ط" else "م²"
                            )
                        }

                        else->{
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                Num("الطول",length,{length=it},"م",modifier=Modifier.weight(1f))
                                Num("العرض",width,{width=it},"م",modifier=Modifier.weight(1f))
                            }
                            Num("التكرار",count,{count=it})
                        }
                    }

                    if(item in listOf("الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد")){
                        Num("الهالك",waste,{waste=it},"%")
                    }

                    if(item=="المباني"){
                        Num("سمك الحائط",thickness,{thickness=it},"م")
                    }

                    HorizontalDivider()
                    QtyLine(item,result,if(item=="الوزرات")"م ط" else "م²",true)

                    if(item=="المباني"){
                        QtyLine("حجم المباني",result*parseNum(thickness),"م³")
                    }

                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("طريقة الحساب",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                        Help(
                            "طريقة الحساب",
                            when{
                                method=="عدة حوائط"->
                                    "يتم حساب كل حائط = الطول × الارتفاع - الفتحات، ثم جمع كل الحوائط."
                                method.contains("جاهز")->
                                    "تم استخدام الكمية المدخلة مباشرة."
                                else->
                                    "يتم الحساب من أبعاد الغرفة أو المسطح والتكرار، مع إضافة الهالك للبنود التي تحتاجه."
                            }
                        )
                    }
                }
            }
        }
    }
}

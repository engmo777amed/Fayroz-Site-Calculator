package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine
import java.util.UUID

private data class QuickWall(
    val id:String=UUID.randomUUID().toString(),
    val length:String="",
    val height:String="3.00",
    val openingArea:String=""
)

@Composable
fun ToolsScreen(onRoom:()->Unit,onItem:()->Unit,onStair:()->Unit){
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(12.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp)
    ){
        item{CompactBrandHeader("الحاسبات السريعة","حصر بدون إنشاء مشروع.")}
        item{DashboardCard("حصر فراغ كامل","حوائط ومساحات وفتحات وبنود.",Icons.Rounded.MeetingRoom,onRoom,true,Modifier.fillMaxWidth())}
        item{DashboardCard("حصر بند مباشر","م² / م ط / م³ / عدد مع تعديلات يدوية.",Icons.Rounded.Calculate,onItem,false,Modifier.fillMaxWidth())}
        item{DashboardCard("حصر سلم","نوايم + قوائم + بسطات + وزرات.",Icons.Rounded.Stairs,onStair,false,Modifier.fillMaxWidth())}
    }
}

@Composable
fun QuickItemScreen(
    onBack:()->Unit,
    projectContext:String?=null,
    onSave:((SpaceEntry)->Unit)?=null
){
    var name by remember{mutableStateOf("محارة الحوائط")}
    var unit by remember{mutableStateOf(MeasureUnit.AREA)}
    var method by remember{mutableStateOf(CalcMethod.DIRECT_AREA)}
    var value by remember{mutableStateOf("")}
    var length by remember{mutableStateOf("")}
    var width by remember{mutableStateOf("")}
    var height by remember{mutableStateOf("3.00")}
    var count by remember{mutableStateOf("1")}
    var waste by remember{mutableStateOf("0")}
    var addValue by remember{mutableStateOf("")}
    var deductValue by remember{mutableStateOf("")}
    var layerThickness by remember{mutableStateOf("0.05")}
    val walls=remember{mutableStateListOf(QuickWall())}

    val methods=CalcMethod.entries.filter{m->
        when(unit){
            MeasureUnit.AREA->m in listOf(CalcMethod.ROOM_WALLS,CalcMethod.WALL_SEGMENTS,CalcMethod.FLOOR_SURFACES,CalcMethod.CEILING_SURFACES,CalcMethod.DIRECT_AREA)
            MeasureUnit.LENGTH->m in listOf(CalcMethod.SKIRTING,CalcMethod.DIRECT_LENGTH)
            MeasureUnit.VOLUME->m in listOf(CalcMethod.FLOOR_LAYER_VOLUME,CalcMethod.DIRECT_VOLUME)
            MeasureUnit.COUNT->m==CalcMethod.DIRECT_COUNT
        }
    }
    LaunchedEffect(unit){if(method !in methods)method=methods.first()}

    val wallModels=walls.mapIndexed{index,w->
        WallSegment(
            id=w.id,
            name="حائط ${index+1}",
            length=n(w.length),
            height=n(w.height).takeIf{it>0}?:3.0
        )
    }
    val scratch=SpaceEntry(
        name=name.ifBlank{"حصر بند مباشر"},
        type="حصر بند مباشر",
        length=n(length),width=n(width),height=n(height),
        repeatCount=n(count).toInt().coerceAtLeast(1),
        walls=if(method==CalcMethod.WALL_SEGMENTS)wallModels else emptyList(),
        status=CaptureStatus.DONE
    )
    val adjustments=buildList{
        if(n(addValue)>0)add(Adjustment(type=AdjustmentType.ADD,amount=n(addValue),note="إضافة سريعة"))
        if(n(deductValue)>0)add(Adjustment(type=AdjustmentType.DEDUCT,amount=n(deductValue),note="خصم سريع"))
    }
    val item=TakeoffItem(
        name=name.ifBlank{"بند سريع"},category="سريع",unit=unit,method=method,
        wastePercent=n(waste),directValue=n(value),
        layerThickness=n(layerThickness),adjustments=adjustments
    )
    val result=QuantityEngine.calculate(scratch,item)

    Scaffold(topBar={AppBarX("حصر بند",projectContext ?: "حساب سريع بدون مشروع",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it).imePadding(),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    TextFieldX("اسم البند",name,{name=it},placeholder="مثال: خرسانة قاعدة جهاز")
                    ChoiceFieldX("الوحدة",unit.label,MeasureUnit.entries.map{it.label},{label->unit=MeasureUnit.entries.first{it.label==label}})
                    ChoiceFieldX("طريقة الحصر",method.label,methods.map{it.label},{label->method=methods.first{it.label==label}})
                    when{
                        method in listOf(CalcMethod.DIRECT_AREA,CalcMethod.DIRECT_LENGTH,CalcMethod.DIRECT_VOLUME,CalcMethod.DIRECT_COUNT) ->
                            NumberFieldX("الكمية",value,{value=it},unit.label)

                        method==CalcMethod.WALL_SEGMENTS -> {
                            Text("أدخل الحوائط المتتابعة؛ يتم جمعها كبند واحد.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            walls.forEachIndexed{index,wall->
                                Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.35f)){
                                    Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                                            Text("حائط ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                                            if(walls.size>1)IconButton(onClick={walls.removeAt(index)},modifier=Modifier.size(48.dp)){Icon(Icons.Rounded.Delete,"حذف")}
                                        }
                                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                            NumberFieldX("الطول",wall.length,{v->walls[index]=wall.copy(length=v)},"م",modifier=Modifier.weight(1f))
                                            NumberFieldX("الارتفاع",wall.height,{v->walls[index]=wall.copy(height=v)},"م",modifier=Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            OutlinedButton(onClick={walls.add(QuickWall(height=height.ifBlank{"3.00"}))},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                                Icon(Icons.Rounded.Add,null);Spacer(Modifier.width(4.dp));Text("إضافة حائط")
                            }
                        }

                        else -> {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                NumberFieldX("الطول",length,{length=it},"م",modifier=Modifier.weight(1f))
                                NumberFieldX("العرض",width,{width=it},"م",modifier=Modifier.weight(1f))
                            }
                            if(method==CalcMethod.ROOM_WALLS)NumberFieldX("الارتفاع",height,{height=it},"م")
                            if(method==CalcMethod.FLOOR_LAYER_VOLUME)NumberFieldX("متوسط السمك",layerThickness,{layerThickness=it},"م","مثال: 5 سم = 0.05 م.")
                            NumberFieldX("التكرار",count,{count=it},"عدد")
                        }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("إضافة",addValue,{addValue=it},unit.label,modifier=Modifier.weight(1f))
                        NumberFieldX("خصم",deductValue,{deductValue=it},unit.label,modifier=Modifier.weight(1f))
                    }
                    NumberFieldX("الهالك",waste,{waste=it},"%")
                }
            }
            item{
                CardBox{
                    SectionTitle("النتيجة","الحساب يتحدث فورًا.")
                    MetricRow(name.ifBlank{"البند"},"${fmt(result.final)} ${unit.label}",true)
                    MetricRow("قبل الهالك","${fmt(result.calculated-result.waste)} ${unit.label}")
                    if(result.waste>0)MetricRow("الهالك","${fmt(result.waste)} ${unit.label}")
                    Text(result.explanation,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(onSave!=null){
                        HorizontalDivider()
                        Button(
                            onClick={
                                onSave(
                                    scratch.copy(
                                        name=item.name,
                                        type="حصر بند مباشر",
                                        takeoffs=listOf(item),
                                        status=CaptureStatus.DONE,
                                        updatedAt=System.currentTimeMillis()
                                    )
                                )
                            },
                            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
                        ){
                            Icon(Icons.Rounded.Save,null)
                            Spacer(Modifier.width(5.dp))
                            Text("حفظ داخل المشروع")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StairCalculatorScreen(onBack:()->Unit){
    var steps by remember{mutableStateOf("12")}
    var width by remember{mutableStateOf("1.20")}
    var tread by remember{mutableStateOf("0.30")}
    var riser by remember{mutableStateOf("0.17")}
    var landingLength by remember{mutableStateOf("1.20")}
    var landingWidth by remember{mutableStateOf("1.20")}
    var landings by remember{mutableStateOf("1")}
    var waste by remember{mutableStateOf("7")}

    val nSteps=n(steps).toInt().coerceAtLeast(0)
    val stairWidth=n(width)
    val treadDepth=n(tread)
    val riserHeight=n(riser)
    val landingArea=n(landingLength)*n(landingWidth)*n(landings).toInt().coerceAtLeast(0)
    val treadsArea=nSteps*stairWidth*treadDepth
    val risersArea=nSteps*stairWidth*riserHeight
    val finishArea=(treadsArea+risersArea+landingArea)*(1+n(waste)/100.0)
    val sideLength=nSteps*(treadDepth+riserHeight)*2 + n(landingLength)*2*n(landings).toInt().coerceAtLeast(0)

    Scaffold(topBar={AppBarX("حصر السلم","نوايم وقوائم وبسطات ووزرات",onBack)}){
        LazyColumn(
            Modifier.fillMaxSize().padding(it).imePadding(),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)
        ){
            item{
                CardBox{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("عدد الدرجات",steps,{steps=it},"درجة",modifier=Modifier.weight(1f))
                        NumberFieldX("عرض السلم",width,{width=it},"م",modifier=Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("النائمة",tread,{tread=it},"م",modifier=Modifier.weight(1f))
                        NumberFieldX("القائمة",riser,{riser=it},"م",modifier=Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("طول البسطة",landingLength,{landingLength=it},"م",modifier=Modifier.weight(1f))
                        NumberFieldX("عرض البسطة",landingWidth,{landingWidth=it},"م",modifier=Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        NumberFieldX("عدد البسطات",landings,{landings=it},"عدد",modifier=Modifier.weight(1f))
                        NumberFieldX("الهالك",waste,{waste=it},"%",modifier=Modifier.weight(1f))
                    }
                }
            }
            item{
                CardBox{
                    SectionTitle("النتيجة","الكميات الهندسية الأساسية للسلم.")
                    MetricRow("مسطح النوايم","${fmt(treadsArea)} م²")
                    MetricRow("مسطح القوائم","${fmt(risersArea)} م²")
                    MetricRow("مسطح البسطات","${fmt(landingArea)} م²")
                    MetricRow("إجمالي تشطيب + هالك","${fmt(finishArea)} م²",true)
                    MetricRow("وزرة جانبي السلم تقريبًا","${fmt(sideLength)} م ط")
                }
            }
        }
    }
}

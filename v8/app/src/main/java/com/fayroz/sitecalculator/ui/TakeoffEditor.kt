@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fayroz.sitecalculator.domain.MaterialReview
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.QuantityEngine
import com.fayroz.sitecalculator.domain.CostEngine
import com.fayroz.sitecalculator.domain.CalculatorLibrary

@Composable
fun TakeoffEditorDialog(
    item:Takeoff,
    walls:List<WallPart>,
    space:Space,
    defaultMaterial:MaterialSpec?=null,
    defaultRecipe:Map<String,String> = emptyMap(),
    onDismiss:()->Unit,
    onSave:(Takeoff)->Unit
){
    var recipe by remember{mutableStateOf(defaultRecipe+("waste" to item.waste.toString())+item.calculatorInputs)}
    var stage by remember{mutableIntStateOf(0)}
    var selectedPartId by remember{mutableStateOf<String?>(null)}
    val recipeDef=CalculatorLibrary.forItem(item.name)
    var parts by remember{mutableStateOf(item.parts)}
    var surfaces by remember{mutableStateOf(item.surfaceIds.toSet())}
    var material by remember{mutableStateOf(item.material?:defaultMaterial?:CostEngine.defaultSpec(item.name))}

    var advanced by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var kind by remember{mutableStateOf(item.kind)}
    var direct by remember{mutableStateOf(if(item.directValue>0)exact(item.directValue) else "")}
    var waste by remember{mutableStateOf(exact(item.waste))}
    var tileHeight by remember{mutableStateOf(exact(item.tileHeight))}
    var upstand by remember{mutableStateOf(exact(item.upstand))}
    var layerThickness by remember{mutableStateOf(if(item.layerThickness>0)exact(item.layerThickness) else "")}
    var reveals by remember{mutableStateOf(item.includeOpeningReveals)}
    var wallIds by remember{mutableStateOf(item.wallIds.toSet())}
    var manualEnabled by remember{mutableStateOf(item.manualValue!=null)}
    var manual by remember{mutableStateOf(item.manualValue?.let(::exact)?:"")}
    var overrideReason by remember{mutableStateOf(item.overrideReason)}
    var adjustments by remember{mutableStateOf(item.adjustments)}
    var adjType by remember{mutableStateOf(AdjustKind.ADD)}
    var adjValue by remember{mutableStateOf("")}
    var adjNote by remember{mutableStateOf("")}
    var pieceW by remember{mutableStateOf(if(item.pieceWidth>0)exact(item.pieceWidth) else "")}
    var pieceH by remember{mutableStateOf(if(item.pieceHeight>0)exact(item.pieceHeight) else "")}
    var pack by remember{mutableStateOf(if(item.piecesPerPack>0)item.piecesPerPack.toString() else "")}
    var note by remember{mutableStateOf(item.note)}

    val preview=item.copy(
        kind=kind,directValue=n(direct),waste=n(waste),tileHeight=n(tileHeight),upstand=n(upstand),
        layerThickness=n(layerThickness),includeOpeningReveals=reveals,wallIds=wallIds.toList(),
        manualValue=if(manualEnabled)n(manual)else null,overrideReason=overrideReason,
        adjustments=adjustments,pieceWidth=n(pieceW),pieceHeight=n(pieceH),
        piecesPerPack=n(pack).toInt().coerceAtLeast(0),note=note,parts=parts,surfaceIds=surfaces.toList(),material=material,calculatorInputs=recipe
    )
    val q=QuantityEngine.calculateOne(space,preview)

    fun scopeError():String? {
        return when{
            q.oneSpaceFinal<=0->"أدخل مقاسات أو كمية أكبر من صفر."
            preview.waste<0||preview.tileHeight<0||preview.upstand<0->"القيم لا يمكن أن تكون سالبة."
            parts.any{QuantityEngine.partValue(it,item.unit)<=0}->"راجع مقاسات وخصومات الأجزاء."
            manualEnabled&&overrideReason.isBlank()->"اكتب سبب اعتماد الكمية الفعلية."
            else->null
        }
    }
    fun go(target:Int){
        error=if(target==1)scopeError() else if(target==2)MaterialReview.itemError(space,preview) else null
        if(error==null)stage=target
    }
    val selectedPart=parts.firstOrNull{it.id==selectedPartId}
    val scopeName=if(manualEnabled)"كامل البند — كمية فعلية" else if(selectedPart==null)"كامل البند" else selectedPart.name
    val partTotal=parts.sumOf{QuantityEngine.partValue(it,item.unit)}
    val selectedQuantity=if(selectedPart!=null&&!manualEnabled&&partTotal>0)QuantityEngine.partValue(selectedPart,item.unit)/partTotal*q.repeatedFinal else q.repeatedFinal
    val projected=Project(name=space.name,sections=listOf(Section(name="",spaces=listOf(space.copy(takeoffs=listOf(preview))))))
    val rows=if(MaterialReview.itemError(space,preview)==null)runCatching{CostEngine.rows(projected)}.getOrDefault(emptyList())else emptyList()
    val selectedRows=rows.filter{manualEnabled||selectedPartId==null||it.partId==selectedPartId}
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        BackHandler{if(stage>0){stage--;error=null}else onDismiss()}
        Scaffold(
            topBar={TopAppBar(title={Column{Text(item.name,fontWeight=FontWeight.Bold);Text(space.name,style=MaterialTheme.typography.bodySmall)}},navigationIcon={TextButton(onClick={if(stage>0){stage--;error=null}else onDismiss()}){Text("رجوع")}})},
            bottomBar={Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    OutlinedButton(onClick={if(stage>0){stage--;error=null}else onDismiss()},modifier=Modifier.weight(1f)){Text(if(stage>0)"تعديل المدخلات" else "إلغاء")}
                    Button(onClick={if(stage<2)go(stage+1)else{error=MaterialReview.itemError(space,preview);if(error==null)onSave(preview)}},modifier=Modifier.weight(1f)){Text(when(stage){0->"التالي: المواد";1->"احسب واعرض النتيجة";else->"حفظ البند"})}
                }
            }}}
        ){padding->Column(Modifier.fillMaxSize().padding(padding)){
            StageNavigation(stage,listOf("الكمية","المواد","النتيجة"),::go)
            Text("${space.name} / ${item.name} • صافي ${fmt(q.repeatedFinal)} ${item.unit.label}",Modifier.padding(horizontal=16.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall)
            val listState=rememberLazyListState()
            LaunchedEffect(stage){listState.scrollToItem(0)}
            androidx.compose.foundation.lazy.LazyColumn(state=listState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxSize()){
                if(stage==0){
                item{
                    ChoiceFieldX(
                        "هتحسب البند إزاي؟",
                        kind.label,
                        allowedKinds(item.unit).map{it.label},
                        {label->kind=allowedKinds(item.unit).first{it.label==label}},
                        help="لو عندك الكمية جاهزة اختار «كمية جاهزة»."
                    )
                }
                if(kind==CalcKind.DIRECT)item{
                    NumberFieldX("الكمية الجاهزة",direct,{direct=it},item.unit.label,help="اكتب الكمية اللي معاك من الموقع مباشرة.")
                }
                if(kind==CalcKind.WALL_TILES)item{
                    NumberFieldX("ارتفاع السيراميك",tileHeight,{tileHeight=it},"م",help="الارتفاع الفعلي للكسوة على الحائط.")
                }
                if(kind==CalcKind.WATERPROOF)item{
                    NumberFieldX("رجوع العزل على الحائط",upstand,{upstand=it},"م")
                }
                if(kind==CalcKind.SCREED_VOLUME)item{
                    NumberFieldX("متوسط السمك",layerThickness,{layerThickness=it},"م")
                }
                item{NumberFieldX("هالك شراء البند",waste,{waste=it},"%",help="لا يضاف لصافي الأعمال. خامات المونة لها هالك مستقل.")}
                item{
                    BoxCard{
                        Text("نطاق التنفيذ",fontWeight=FontWeight.Black)
                        Text("اترك الأجزاء فارغة لحساب المسطح المختار كله؛ أو أضف أجزاء مستقلة. الخصومات لكل جزء تُدخل هنا.",style=MaterialTheme.typography.bodySmall)
                        parts.forEach{p->
                            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                                fun change(x:WorkPart){parts=parts.map{if(it.id==p.id)x else it}}
                                TextFieldX("اسم الجزء",p.name,{change(p.copy(name=it))})
                                if(item.unit==UnitType.AREA){
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                        SpecNumber("الطول",p.length,"م",Modifier.weight(1f)){change(p.copy(length=it,quantity=null))}
                                        SpecNumber("العرض / الارتفاع",p.width,"م",Modifier.weight(1f)){change(p.copy(width=it,quantity=null))}
                                    }
                                }else SpecNumber("الكمية",p.quantity?:p.length,item.unit.label){change(p.copy(quantity=it))}
                                SpecNumber("خصم هذا الجزء",p.deduction,item.unit.label){change(p.copy(deduction=it))}
                                TextFieldX("ملاحظة الجزء",p.note,{change(p.copy(note=it))})
                                Row{
                                    if(material!=null)TextButton(onClick={selectedPartId=p.id;stage=1}){Text("خلطة وتكلفة الجزء")}
                                    else if(recipeDef!=null)TextButton(onClick={selectedPartId=p.id;stage=1}){Text("خامات وتكلفة الجزء")}
                                    TextButton(onClick={parts=parts.filterNot{it.id==p.id}}){Text("حذف الجزء")}
                                }
                                HorizontalDivider()
                            }
                        }
                        OutlinedButton(onClick={parts=parts+WorkPart(name="جزء ${parts.size+1}")},modifier=Modifier.fillMaxWidth()){Text("إضافة جزء مستقل")}
                    }
                }
                if(parts.isEmpty()&&kind in listOf(CalcKind.CEILING,CalcKind.FLOOR)){
                    val choices=if(kind==CalcKind.CEILING)QuantityEngine.effectiveCeilingSurfaces(space) else QuantityEngine.effectiveFloorSurfaces(space)
                    if(choices.size>1)item{
                        BoxCard{
                            Text("مسطحات معينة (عدم الاختيار = الكل)")
                            choices.forEach{p->Row(verticalAlignment=Alignment.CenterVertically){
                                Checkbox(p.id in surfaces,{yes->surfaces=if(yes)surfaces+p.id else surfaces-p.id});Text(p.name)
                            }}
                        }
                    }
                }

                item{TextButton(onClick={advanced=!advanced}){Text(if(advanced)"إخفاء التفاصيل الإضافية" else "إضافات وخصومات وكمية فعلية ومقاس القطعة")}}


                if(kind in listOf(CalcKind.WALLS,CalcKind.WALL_TILES)){
                    item{
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("احسب جوانب الأبواب والشبابيك",fontWeight=FontWeight.Black)
                                Text("يتطلب إدخال عمق الجنب في الفتحة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(reveals,{reveals=it})
                        }
                    }
                    if(walls.isNotEmpty()){
                        item{Text("حوائط معينة - اختياري",fontWeight=FontWeight.Black)}
                        items(walls.size){i->
                            val wall=walls[i]
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Checkbox(
                                    checked=wall.id in wallIds,
                                    onCheckedChange={checked->wallIds=if(checked)wallIds+wall.id else wallIds-wall.id}
                                )
                                Text(wall.name)
                            }
                        }
                    }
                }

                if(advanced)item{
                    BoxCard{
                        Text("إضافة أو خصم",fontWeight=FontWeight.Black)
                        adjustments.forEachIndexed{i,a->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text((if(a.kind==AdjustKind.ADD)"+ " else "- ")+fmt(a.amount)+" "+item.unit.label,Modifier.weight(1f))
                                Text(a.note,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                                IconButton(onClick={adjustments=adjustments.filterIndexed{idx,_->idx!=i}}){
                                    Icon(Icons.Rounded.Delete,"حذف")
                                }
                            }
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            ChoiceFieldX(
                                "النوع",
                                if(adjType==AdjustKind.ADD)"إضافة" else "خصم",
                                listOf("إضافة","خصم"),
                                {adjType=if(it=="إضافة")AdjustKind.ADD else AdjustKind.DEDUCT},
                                modifier=Modifier.weight(1f)
                            )
                            NumberFieldX("الكمية",adjValue,{adjValue=it},item.unit.label,Modifier.weight(1f))
                        }
                        TextFieldX("السبب",adjNote,{adjNote=it},placeholder="مثال: استبعاد خلف وحدة")
                        OutlinedButton(
                            onClick={
                                if(n(adjValue)>0){
                                    adjustments=adjustments+Adjustment(kind=adjType,amount=n(adjValue),note=adjNote.trim())
                                    adjValue="";adjNote=""
                                }
                            },
                            modifier=Modifier.fillMaxWidth()
                        ){Text("ضيف التعديل")}
                    }
                }

                if(advanced)item{
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text("عندي كمية فعلية من الموقع",fontWeight=FontWeight.Black)
                            Text("تستخدم بدل المحسوب، والحساب يفضل محفوظ للمراجعة.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(manualEnabled,{manualEnabled=it})
                    }
                }
                if(manualEnabled){
                    item{NumberFieldX("الكمية الفعلية",manual,{manual=it},item.unit.label)}
                    item{TextFieldX("سبب الاعتماد",overrideReason,{overrideReason=it},placeholder="مثال: حصر فعلي بالموقع")}
                }

                if(advanced&&item.unit==UnitType.AREA){
                    item{
                        BoxCard{
                            Text("مقاس البلاطة أو القطعة - اختياري",fontWeight=FontWeight.Black)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                NumberFieldX("العرض",pieceW,{pieceW=it},"م",Modifier.weight(1f))
                                NumberFieldX("الطول",pieceH,{pieceH=it},"م",Modifier.weight(1f))
                            }
                            NumberFieldX("قطع في الكرتونة",pack,{pack=it},"قطعة")
                        }
                    }
                }

                item{TextFieldX("ملاحظة البند",note,{note=it},placeholder="اختياري")}

                }
                if(stage==1||stage==2){
                    item{BoxCard{
                        val labels=listOf("كامل البند")+parts.mapIndexed{i,p->"${p.name} (${i+1})"}
                        val selectedIndex=parts.indexOfFirst{it.id==selectedPartId}
                        if(!manualEnabled)ChoiceFieldX("نطاق المواد والنتيجة",if(selectedIndex<0)labels[0] else labels[selectedIndex+1],labels,{label->selectedPartId=parts.getOrNull(labels.indexOf(label)-1)?.id;error=null})
                        MetricRow("الصافي المحدد", "${fmt(selectedQuantity)} ${item.unit.label}")
                        if(manualEnabled&&selectedPart!=null)Text("الكمية الفعلية تلغي توزيع الأجزاء. اختار كامل البند للنتيجة.",color=MaterialTheme.colorScheme.error)
                    }}
                }
                if(stage==1){
                    if(selectedPart!=null&&(material!=null||recipeDef!=null))item{BoxCard{
                        val custom=if(material!=null)selectedPart.material!=null else selectedPart.calculatorInputs.isNotEmpty()
                        Row(verticalAlignment=Alignment.CenterVertically){Text("إعدادات خاصة لهذا الجزء",Modifier.weight(1f));Switch(custom,{enabled->parts=parts.map{p->if(p.id!=selectedPart.id)p else if(material!=null)p.copy(material=if(enabled)material else null)else p.copy(calculatorInputs=if(enabled)recipe else emptyMap())}})}
                        Text(if(custom)"هذا الجزء له مواصفاته وأسعاره." else "يستخدم مواصفات وأسعار البند. عدّل كامل البند أو فعّل إعدادات خاصة.",style=MaterialTheme.typography.bodySmall)
                    }}
                    material?.let{m->
                        if(selectedPart==null||selectedPart.material!=null)item{BoxCard{key(selectedPartId){MaterialSpecFields(selectedPart?.material?:m,{next->if(selectedPart==null)material=next else parts=parts.map{if(it.id==selectedPart.id)it.copy(material=next)else it};error=null},item.unit.label)}}}
                    }
                    if(material==null&&recipeDef!=null&&(selectedPart==null||selectedPart.calculatorInputs.isNotEmpty()))item{BoxCard{key(selectedPartId){RecipeFields(recipeDef,if(selectedPart==null)q.repeatedFinal else QuantityEngine.partValue(selectedPart,item.unit)*space.repeatCount,selectedPart?.calculatorInputs?.takeIf{it.isNotEmpty()}?:recipe,{next->if(selectedPart==null)recipe=next else parts=parts.map{if(it.id==selectedPart.id)it.copy(calculatorInputs=next)else it};error=null})}}}
                    if(material==null&&recipeDef==null)item{BoxCard{Text("هذا البند له حصر كمية فقط. اضغط احسب لعرض الكمية.")}}
                }
                if(stage==2){
                    item{WorkResultCards(selectedRows,"${space.name} / ${item.name} / $scopeName")}
                    item{OutlinedButton(onClick={stage=1},modifier=Modifier.fillMaxWidth()){Text("تعديل المواد والأسعار")}}
                }
            }
        }}
    }
}

private fun allowedKinds(unit:UnitType):List<CalcKind> = when(unit){
    UnitType.AREA->listOf(CalcKind.WALLS,CalcKind.CEILING,CalcKind.FLOOR,CalcKind.WALL_TILES,CalcKind.WATERPROOF,CalcKind.DIRECT)
    UnitType.LENGTH->listOf(CalcKind.SKIRTING,CalcKind.DIRECT)
    UnitType.VOLUME->listOf(CalcKind.SCREED_VOLUME,CalcKind.DIRECT)
    UnitType.COUNT->listOf(CalcKind.DIRECT)
}

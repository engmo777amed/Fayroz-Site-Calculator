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
    onDismiss:()->Unit,
    onSave:(Takeoff)->Unit
){
    var recipe by remember{mutableStateOf(item.calculatorInputs)}
    var partRecipeId by remember{mutableStateOf<String?>(null)}
    val recipeDef=CalculatorLibrary.forItem(item.name)
    var parts by remember{mutableStateOf(item.parts)}
    var surfaces by remember{mutableStateOf(item.surfaceIds.toSet())}
    var material by remember{mutableStateOf(item.material?:defaultMaterial?:CostEngine.defaultSpec(item.name))}
    var partSpecId by remember{mutableStateOf<String?>(null)}
    var advanced by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var kind by remember{mutableStateOf(item.kind)}
    var direct by remember{mutableStateOf(if(item.directValue>0)fmt(item.directValue) else "")}
    var waste by remember{mutableStateOf(fmt(item.waste))}
    var tileHeight by remember{mutableStateOf(fmt(item.tileHeight))}
    var upstand by remember{mutableStateOf(fmt(item.upstand))}
    var layerThickness by remember{mutableStateOf(if(item.layerThickness>0)fmt(item.layerThickness) else "")}
    var reveals by remember{mutableStateOf(item.includeOpeningReveals)}
    var wallIds by remember{mutableStateOf(item.wallIds.toSet())}
    var manualEnabled by remember{mutableStateOf(item.manualValue!=null)}
    var manual by remember{mutableStateOf(item.manualValue?.let(::fmt)?:"")}
    var overrideReason by remember{mutableStateOf(item.overrideReason)}
    var adjustments by remember{mutableStateOf(item.adjustments)}
    var adjType by remember{mutableStateOf(AdjustKind.ADD)}
    var adjValue by remember{mutableStateOf("")}
    var adjNote by remember{mutableStateOf("")}
    var pieceW by remember{mutableStateOf(if(item.pieceWidth>0)fmt(item.pieceWidth) else "")}
    var pieceH by remember{mutableStateOf(if(item.pieceHeight>0)fmt(item.pieceHeight) else "")}
    var pack by remember{mutableStateOf(if(item.piecesPerPack>0)item.piecesPerPack.toString() else "")}
    var note by remember{mutableStateOf(item.note)}
    var formulaOpen by remember{mutableStateOf(false)}

    val preview=item.copy(
        kind=kind,directValue=n(direct),waste=n(waste),tileHeight=n(tileHeight),upstand=n(upstand),
        layerThickness=n(layerThickness),includeOpeningReveals=reveals,wallIds=wallIds.toList(),
        manualValue=if(manualEnabled)n(manual)else null,overrideReason=overrideReason,
        adjustments=adjustments,pieceWidth=n(pieceW),pieceHeight=n(pieceH),
        piecesPerPack=n(pack).toInt().coerceAtLeast(0),note=note,parts=parts,surfaceIds=surfaces.toList(),material=material,calculatorInputs=recipe
    )
    val q=QuantityEngine.calculateOne(space,preview)

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(item.name,fontWeight=FontWeight.Black)},
        text={
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement=Arrangement.spacedBy(8.dp),
                modifier=Modifier.fillMaxWidth()
            ){
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
                                    if(material!=null)TextButton(onClick={partSpecId=p.id}){Text("خلطة وتكلفة الجزء")}
                                    else if(recipeDef!=null)TextButton(onClick={partRecipeId=p.id}){Text("خامات وتكلفة الجزء")}
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
                material?.let{m->item{
                    var show by remember{mutableStateOf(false)}
                    TextButton(onClick={show=!show}){Text(if(show)"إخفاء مواصفات الخامات" else "خلطة وسمك وأسعار هذا البند")}
                    if(show)BoxCard{MaterialSpecFields(m,{material=it})}
                }}
                if(material==null&&recipeDef!=null)item{
                    var show by remember{mutableStateOf(false)}
                    TextButton(onClick={show=!show}){Text("خامات وأسعار هذا البند")}
                    if(show)BoxCard{RecipeFields(recipeDef,q.repeatedFinal,recipe,{recipe=it})}
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
                error?.let{msg->item{Text(msg,color=MaterialTheme.colorScheme.error)}}
                item{MetricRow("صافي الحصر","${fmt(q.repeatedFinal)} ${item.unit.label}",true)}
                item{
                    OutlinedButton(onClick={formulaOpen=true},modifier=Modifier.fillMaxWidth()){
                        Text("اتحسبت إزاي؟")
                    }
                }
            }
        },
        confirmButton={Button(onClick={
            error=when{
                q.oneSpaceFinal<=0->"أدخل مقاسات أو كمية أكبر من صفر."
                preview.waste<0||preview.tileHeight<0||preview.upstand<0->"القيم لا يمكن أن تكون سالبة."
                recipeDef!=null&&parts.isEmpty()&&runCatching{CalculatorLibrary.evaluate(recipeDef,CalculatorLibrary.recipe(recipeDef,recipe,q.repeatedFinal))}.isFailure->"راجع بيانات الخامات والعبوات."
                parts.any{QuantityEngine.partValue(it,item.unit)<=0}->"راجع مقاسات وخصومات الأجزاء."
                material?.let{it.bagKg<=0||it.cementParts+it.sandParts<=0||it.thicknessMm<=0}==true->"راجع وزن الشيكارة والخلطة والسمك."
                manualEnabled&&overrideReason.isBlank()->"اكتب سبب اعتماد الكمية الفعلية."
                else->null
            }
            if(error==null)onSave(preview)
        }){Text("حفظ")}},
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}}
    )

    partRecipeId?.let{id->
        val part=parts.firstOrNull{it.id==id}
        if(part!=null&&recipeDef!=null){
            var values by remember(id){mutableStateOf(part.calculatorInputs.takeIf{it.isNotEmpty()}?:recipe)}
            AlertDialog(onDismissRequest={partRecipeId=null},title={Text("خامات ${part.name}")},
                text={androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{RecipeFields(recipeDef,QuantityEngine.partValue(part,item.unit)*space.repeatCount,values,{values=it})}}},
                confirmButton={TextButton(onClick={if(runCatching{CalculatorLibrary.evaluate(recipeDef,CalculatorLibrary.recipe(recipeDef,values,QuantityEngine.partValue(part,item.unit)*space.repeatCount))}.isSuccess){parts=parts.map{if(it.id==id)it.copy(calculatorInputs=values)else it};partRecipeId=null}}){Text("اعتماد")}},
                dismissButton={TextButton(onClick={partRecipeId=null}){Text("إلغاء")}})
        }
    }
    partSpecId?.let{id->
        val part=parts.firstOrNull{it.id==id}
        if(part!=null){
            var spec by remember(id){mutableStateOf(part.material?:material?:MaterialSpec())}
            AlertDialog(onDismissRequest={partSpecId=null},title={Text("خامات ${part.name}")},
                text={androidx.compose.foundation.lazy.LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{MaterialSpecFields(spec,{spec=it})}}},
                confirmButton={TextButton(onClick={if(spec.bagKg>0&&spec.thicknessMm>0&&spec.cementParts+spec.sandParts>0){parts=parts.map{if(it.id==id)it.copy(material=spec)else it};partSpecId=null}}){Text("اعتماد")}},
                dismissButton={TextButton(onClick={parts=parts.map{if(it.id==id)it.copy(material=null)else it};partSpecId=null}){Text("استخدم مواصفات البند")}})
        }
    }
    if(formulaOpen){
        AlertDialog(
            onDismissRequest={formulaOpen=false},
            title={Text("اتحسبت إزاي؟",fontWeight=FontWeight.Black)},
            text={Text(q.explanation)},
            confirmButton={TextButton(onClick={formulaOpen=false}){Text("تمام")}}
        )
    }
}

private fun allowedKinds(unit:UnitType):List<CalcKind> = when(unit){
    UnitType.AREA->listOf(CalcKind.WALLS,CalcKind.CEILING,CalcKind.FLOOR,CalcKind.WALL_TILES,CalcKind.WATERPROOF,CalcKind.DIRECT)
    UnitType.LENGTH->listOf(CalcKind.SKIRTING,CalcKind.DIRECT)
    UnitType.VOLUME->listOf(CalcKind.SCREED_VOLUME,CalcKind.DIRECT)
    UnitType.COUNT->listOf(CalcKind.DIRECT)
}

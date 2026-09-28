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

@Composable
fun TakeoffEditorDialog(
    item:Takeoff,
    walls:List<WallPart>,
    space:Space,
    onDismiss:()->Unit,
    onSave:(Takeoff)->Unit
){
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
        piecesPerPack=n(pack).toInt().coerceAtLeast(0),note=note
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
                item{NumberFieldX("الهالك",waste,{waste=it},"%",help="الهالك خاص بالبند ده فقط.")}

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

                item{
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

                item{
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

                if(item.unit==UnitType.AREA){
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
                item{MetricRow("النتيجة","${fmt(q.repeatedFinal)} ${item.unit.label}",true)}
                item{
                    OutlinedButton(onClick={formulaOpen=true},modifier=Modifier.fillMaxWidth()){
                        Text("اتحسبت إزاي؟")
                    }
                }
            }
        },
        confirmButton={Button(onClick={onSave(preview)}){Text("حفظ")}},
        dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}}
    )

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

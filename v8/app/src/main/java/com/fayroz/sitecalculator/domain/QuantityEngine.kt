package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object QuantityEngine {
    fun rectangularWalls(space:Space):List<WallPart> = listOf(
        WallPart(id="rect_1",name="حائط 1",length=space.length,height=space.height),
        WallPart(id="rect_2",name="حائط 2",length=space.width,height=space.height),
        WallPart(id="rect_3",name="حائط 3",length=space.length,height=space.height),
        WallPart(id="rect_4",name="حائط 4",length=space.width,height=space.height)
    )

    fun effectiveWalls(space:Space):List<WallPart> =
        if(space.walls.isNotEmpty())space.walls else rectangularWalls(space)

    fun effectiveFloorSurfaces(space:Space):List<SurfacePart> =
        if(space.floorSurfaces.isNotEmpty())space.floorSurfaces
        else listOf(SurfacePart(id="floor_main",name="المسطح الرئيسي",length=space.length,width=space.width))

    fun effectiveCeilingSurfaces(space:Space):List<SurfacePart> =
        if(space.ceilingSurfaces.isNotEmpty())space.ceilingSurfaces else effectiveFloorSurfaces(space)

    private fun openingsForWall(space:Space,wallId:String,index:Int):List<Opening>{
        val assigned=space.openings.filter{it.wallId==wallId}
        val unassigned=if(index==0)space.openings.filter{it.wallId==null}else emptyList()
        return assigned+unassigned
    }

    private fun openingArea(o:Opening)=o.width*o.height*o.count

    private fun openingOverlapBelow(o:Opening,level:Double):Double{
        val bottom=o.sill.coerceAtLeast(0.0)
        val top=bottom+o.height
        val overlap=max(0.0,min(top,level)-bottom)
        return o.width*overlap*o.count
    }

    private fun revealArea(o:Opening,level:Double?=null):Double{
        val depth=o.revealDepth.coerceAtLeast(0.0)
        if(depth<=0.0)return 0.0
        val visibleHeight=if(level==null)o.height else max(0.0,min(o.sill+o.height,level)-o.sill)
        val vertical=2.0*visibleHeight
        val cutoff=level?:Double.POSITIVE_INFINITY
        val top=o.sill+o.height
        val horizontal=(if(top<=cutoff&&visibleHeight>0)o.width else 0.0)+
            (if(o.kind!=OpeningKind.DOOR&&o.sill<cutoff&&visibleHeight>0)o.width else 0.0)
        return (vertical+horizontal)*depth*o.count
    }

    fun wallAreaOne(space:Space,includeReveals:Boolean=false,wallIds:Set<String> = emptySet(),deductOpenings:Boolean=true):Double{
        val all=effectiveWalls(space)
        val selected=if(wallIds.isEmpty())all else all.filter{it.id in wallIds}
        return selected.mapIndexed{index,wall->
            val gross=wall.length*wall.height
            val ops=openingsForWall(space,wall.id,all.indexOfFirst{it.id==wall.id})
            val deduct=if(deductOpenings)ops.sumOf(::openingArea)else 0.0
            val reveals=if(includeReveals)ops.sumOf{revealArea(it)}else 0.0
            (gross-deduct+reveals).coerceAtLeast(0.0)
        }.sum()
    }

    fun wallTilesOne(space:Space,tileHeight:Double,includeReveals:Boolean=false,wallIds:Set<String> = emptySet(),deductOpenings:Boolean=true):Double{
        val all=effectiveWalls(space)
        val selected=if(wallIds.isEmpty())all else all.filter{it.id in wallIds}
        return selected.mapIndexed{index,wall->
            val level=min(tileHeight.coerceAtLeast(0.0),wall.height)
            val gross=wall.length*level
            val ops=openingsForWall(space,wall.id,all.indexOfFirst{it.id==wall.id})
            val deduct=if(deductOpenings)ops.sumOf{openingOverlapBelow(it,level)}else 0.0
            val reveals=if(includeReveals)ops.sumOf{revealArea(it,level)}else 0.0
            (gross-deduct+reveals).coerceAtLeast(0.0)
        }.sum()
    }

    fun floorAreaOne(space:Space):Double =
        effectiveFloorSurfaces(space).sumOf{(it.length*it.width-it.deductionArea).coerceAtLeast(0.0)}

    fun ceilingAreaOne(space:Space):Double =
        effectiveCeilingSurfaces(space).sumOf{(it.length*it.width-it.deductionArea).coerceAtLeast(0.0)}

    fun perimeterOne(space:Space):Double=effectiveWalls(space).sumOf{it.length}

    fun skirtingOne(space:Space):Double{
        val doors=space.openings.filter{it.kind==OpeningKind.DOOR}.sumOf{it.width*it.count}
        return (perimeterOne(space)-doors).coerceAtLeast(0.0)
    }

    fun waterproofOne(space:Space,upstand:Double):Double =
        floorAreaOne(space)+perimeterOne(space)*upstand.coerceAtLeast(0.0)

    private fun baseValue(space:Space,item:Takeoff):Pair<Double,String> = when(item.kind){
        CalcKind.WALLS -> wallAreaOne(space,item.includeOpeningReveals,item.wallIds.toSet(),item.calculatorInputs["_deductOpenings"]!="false") to
            ((if(item.calculatorInputs["_deductOpenings"]=="false")"مساحة الحوائط دون خصم الفتحات"else "صافي الحوائط بعد خصم الأبواب والشبابيك")+if(item.includeOpeningReveals)" وإضافة جوانب الفتحات" else "")
        CalcKind.CEILING -> ceilingAreaOne(space) to "مساحة السقف بعد خصم أي أجزاء مستبعدة"
        CalcKind.FLOOR -> floorAreaOne(space) to "مساحة الأرضية بعد خصم أي أجزاء مستبعدة"
        CalcKind.WALL_TILES -> wallTilesOne(space,item.tileHeight,item.includeOpeningReveals,item.wallIds.toSet(),item.calculatorInputs["_deductOpenings"]!="false") to
            "سيراميك الحوائط لحد ارتفاع ${f(item.tileHeight)} م"+(if(item.calculatorInputs["_deductOpenings"]=="false")" دون خصم الفتحات"else " مع خصم الفتحات داخل الارتفاع")
        CalcKind.SKIRTING -> skirtingOne(space) to "محيط الحوائط ناقص عروض الأبواب"
        CalcKind.WATERPROOF -> waterproofOne(space,item.upstand) to
            "مساحة الأرضية + رجوع العزل ${f(item.upstand)} م على الحوائط"
        CalcKind.SCREED_VOLUME -> floorAreaOne(space)*item.layerThickness.coerceAtLeast(0.0) to
            "مساحة الأرضية × متوسط السمك ${f(item.layerThickness)} م"
        CalcKind.DIRECT -> item.directValue.coerceAtLeast(0.0) to "الكمية مكتوبة مباشرة"
    }

    fun calculateOne(space:Space,item:Takeoff):QuantityResult{
        val (rawBase,rawFormula)=baseValue(space,item)
        val surfaceBase=if(item.surfaceIds.isNotEmpty()){
            val surfaces=if(item.kind==CalcKind.CEILING)effectiveCeilingSurfaces(space) else effectiveFloorSurfaces(space)
            surfaces.filter{it.id in item.surfaceIds}.sumOf{max(0.0,it.length*it.width-it.deductionArea)}
        }else rawBase
        val base=if(item.parts.isNotEmpty())item.parts.sumOf{partValue(it,item.unit)} else surfaceBase
        val formula=if(item.parts.isNotEmpty())"أجزاء مستقلة: "+item.parts.joinToString("، "){it.name+" = "+f(partValue(it,item.unit))}else rawFormula
        val additions=item.adjustments.filter{it.kind==AdjustKind.ADD}.sumOf{it.amount}
        val deductions=item.adjustments.filter{it.kind==AdjustKind.DEDUCT}.sumOf{it.amount}
        val adjusted=max(0.0,base+additions-deductions)
        val waste=adjusted*(item.waste.coerceAtLeast(0.0)/100.0)
        val calculated=adjusted+waste
        val one=(item.manualValue ?: adjusted).coerceAtLeast(0.0)
        val repeat=space.repeatCount.coerceAtLeast(1)
        val repeated=one*repeat

        val explanation=buildString{
            append(formula)
            append(". الأساس ${f(base)} ${item.unit.label}")
            if(additions>0)append(" + إضافة ${f(additions)}")
            if(deductions>0)append(" - خصم ${f(deductions)}")
            if(waste>0)append(". هالك الشراء ${f(item.waste)}% منفصل عن صافي الحصر")
            if(item.manualValue!=null)append(". تم اعتماد كمية فعلية ${f(item.manualValue)} بدل المحسوب")
            if(repeat>1)append(". المكان الواحد ${f(one)} × $repeat = ${f(repeated)}")
        }

        return QuantityResult(base,additions,deductions,waste,one,repeated,explanation)
    }

    fun partValue(p:WorkPart,unit:UnitType):Double = max(0.0,(p.quantity?:when(unit){
        UnitType.AREA->p.length*p.width
        UnitType.LENGTH->p.length
        UnitType.VOLUME->p.length*p.width
        UnitType.COUNT->p.length
    })-p.deduction)

    fun purchaseInfo(space:Space,item:Takeoff):PurchaseInfo?{
        if(item.unit!=UnitType.AREA||item.pieceWidth<=0.0||item.pieceHeight<=0.0)return null
        val pieceArea=item.pieceWidth*item.pieceHeight
        if(pieceArea<=0.0)return null
        val pieces=ceil(calculateOne(space,item).repeatedFinal*(1+item.waste/100)/pieceArea).toInt().coerceAtLeast(0)
        val packs=if(item.piecesPerPack>0)ceil(pieces.toDouble()/item.piecesPerPack).toInt() else null
        return PurchaseInfo(pieceArea,pieces,packs)
    }

    fun summarize(project:Project):List<SummaryLine>{
        val map=linkedMapOf<Pair<String,UnitType>,Double>()
        project.sections.forEach{section->section.spaces.forEach{space->space.takeoffs.forEach{item->
            val key=item.name to item.unit
            map[key]=(map[key]?:0.0)+calculateOne(space,item).repeatedFinal
        }}}
        CostEngine.rows(project.copy(sections=emptyList())).forEach{r->
            val unit=UnitType.entries.firstOrNull{it.label==r.unit}?:if(r.unit=="م")UnitType.LENGTH else null
            if(unit!=null){val key=r.item to unit;map[key]=(map[key]?:0.0)+r.quantity}
        }
        return map.map{SummaryLine(it.key.first,it.key.second,it.value)}
    }

    fun summarize(section:Section):List<SummaryLine>{
        val map=linkedMapOf<Pair<String,UnitType>,Double>()
        section.spaces.forEach{space->space.takeoffs.forEach{item->
            val key=item.name to item.unit
            map[key]=(map[key]?:0.0)+calculateOne(space,item).repeatedFinal
        }}
        return map.map{SummaryLine(it.key.first,it.key.second,it.value)}
    }

    fun contributors(project:Project,itemName:String,unit:UnitType?=null):List<Pair<String,Double>>{
        val out=mutableListOf<Pair<String,Double>>()
        project.sections.forEach{section->section.spaces.forEach{space->
            space.takeoffs.filter{it.name==itemName&&(unit==null||it.unit==unit)}.forEach{item->
                out += "${section.name} ← ${space.name}" to calculateOne(space,item).repeatedFinal
            }
        }}
        return out
    }

    private fun f(v:Double)=String.format(java.util.Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
}


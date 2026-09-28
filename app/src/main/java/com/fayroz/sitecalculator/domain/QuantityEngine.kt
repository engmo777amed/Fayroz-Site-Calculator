package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object QuantityEngine {
    val standardItems=listOf(
        "مباني",
        "محارة الحوائط","محارة السقف",
        "دهان الحوائط","دهان السقف",
        "الأرضيات","سيراميك الحوائط","الوزرات",
        "عزل الأرضية","سقف جبس بورد",
        "مونة تسوية الأرضيات","خرسانة بسيطة",
        "رخام / جرانيت","واجهات / كسوات",
        "كرانيش / حليات","جبس بورد جوانب ساقطة","بيت نور / كوف","سقف معلق بلاطات","عدد قطع / وحدات"
    )

    fun rectangularWalls(space:SpaceEntry):List<WallSegment> = listOf(
        WallSegment(name="حائط 1",length=space.length,height=space.height),
        WallSegment(name="حائط 2",length=space.width,height=space.height),
        WallSegment(name="حائط 3",length=space.length,height=space.height),
        WallSegment(name="حائط 4",length=space.width,height=space.height)
    )

    fun effectiveWalls(space:SpaceEntry):List<WallSegment> =
        if(space.walls.isNotEmpty()) space.walls else rectangularWalls(space)

    fun effectiveFloorSurfaces(space:SpaceEntry):List<SurfaceSegment> =
        if(space.floorSurfaces.isNotEmpty()) space.floorSurfaces
        else listOf(SurfaceSegment(name="المسطح الرئيسي",length=space.length,width=space.width))

    fun effectiveCeilingSurfaces(space:SpaceEntry):List<SurfaceSegment> =
        if(space.ceilingSurfaces.isNotEmpty()) space.ceilingSurfaces else effectiveFloorSurfaces(space)

    private fun openingsForWall(space:SpaceEntry,wallId:String,index:Int):List<Opening>{
        if(space.walls.isEmpty()){
            val unassigned=if(index==0) space.openings.filter{it.wallId==null} else emptyList()
            return unassigned + space.openings.filter{it.wallId==wallId}
        }
        return space.openings.filter{it.wallId==wallId}
    }

    private fun openingArea(o:Opening)=o.width*o.height*o.count

    private fun openingOverlapBelow(o:Opening,level:Double):Double{
        val bottom=o.sillHeight.coerceAtLeast(0.0)
        val top=bottom+o.height
        val overlap=max(0.0,min(top,level)-bottom)
        return o.width*overlap*o.count
    }

    private fun openingRevealArea(o:Opening,level:Double?=null):Double{
        val depth=o.revealDepth.coerceAtLeast(0.0)
        if(depth<=0.0) return 0.0
        val visibleHeight=if(level==null) o.height else max(0.0,min(o.sillHeight+o.height,level)-o.sillHeight)
        val vertical=2*visibleHeight
        val horizontal=if(o.type==OpeningType.DOOR) o.width else 2*o.width
        return (vertical+horizontal)*depth*o.count
    }

    fun wallArea(space:SpaceEntry,includeReveals:Boolean=false,wallIds:Set<String> = emptySet()):Double{
        val allWalls=effectiveWalls(space)
        val walls=if(wallIds.isEmpty()) allWalls else allWalls.filter{it.id in wallIds}
        return walls.mapIndexed{index,wall->
            val gross=wall.length*wall.height
            val ops=openingsForWall(space,wall.id,index)
            val deduct=ops.sumOf(::openingArea)
            val reveals=if(includeReveals) ops.sumOf{openingRevealArea(it)} else 0.0
            (gross-deduct+reveals).coerceAtLeast(0.0)
        }.sum()*space.repeatCount
    }

    fun wallTiles(space:SpaceEntry,tileHeight:Double,includeReveals:Boolean=false,wallIds:Set<String> = emptySet()):Double{
        val allWalls=effectiveWalls(space)
        val walls=if(wallIds.isEmpty()) allWalls else allWalls.filter{it.id in wallIds}
        return walls.mapIndexed{index,wall->
            val level=min(tileHeight,wall.height).coerceAtLeast(0.0)
            val gross=wall.length*level
            val ops=openingsForWall(space,wall.id,index)
            val deduct=ops.sumOf{openingOverlapBelow(it,level)}
            val reveals=if(includeReveals) ops.sumOf{openingRevealArea(it,level)} else 0.0
            (gross-deduct+reveals).coerceAtLeast(0.0)
        }.sum()*space.repeatCount
    }

    fun floorArea(space:SpaceEntry):Double =
        effectiveFloorSurfaces(space).sumOf{(it.length*it.width-it.deductionArea).coerceAtLeast(0.0)}*space.repeatCount

    fun ceilingArea(space:SpaceEntry):Double =
        effectiveCeilingSurfaces(space).sumOf{(it.length*it.width-it.deductionArea).coerceAtLeast(0.0)}*space.repeatCount

    fun skirtingLength(space:SpaceEntry):Double{
        val perimeter=effectiveWalls(space).sumOf{it.length}
        val doorWidths=space.openings.filter{it.type==OpeningType.DOOR}.sumOf{it.width*it.count}
        return (perimeter-doorWidths).coerceAtLeast(0.0)*space.repeatCount
    }

    fun waterproofArea(space:SpaceEntry,upstand:Double):Double{
        val floor=floorArea(space)
        val perimeter=effectiveWalls(space).sumOf{it.length}*space.repeatCount
        return floor+perimeter*upstand.coerceAtLeast(0.0)
    }

    private fun baseValue(space:SpaceEntry,item:TakeoffItem):Pair<Double,String> = when(item.method){
        CalcMethod.ROOM_WALLS,
        CalcMethod.WALL_SEGMENTS -> wallArea(space,item.includeOpeningReveals,item.wallIds.toSet()) to
            "صافي الحوائط بعد خصم الفتحات${if(item.includeOpeningReveals)" وإضافة جوانب الفتحات" else ""}"

        CalcMethod.WALL_TILES -> wallTiles(space,item.tileHeight,item.includeOpeningReveals,item.wallIds.toSet()) to
            "كسوة الحوائط حتى منسوب ${item.tileHeight} م مع خصم الجزء المتداخل من كل فتحة"

        CalcMethod.FLOOR_SURFACES -> floorArea(space) to "إجمالي مسطحات الأرضية بعد الخصومات"
        CalcMethod.CEILING_SURFACES -> ceilingArea(space) to "إجمالي مسطحات السقف بعد الخصومات"
        CalcMethod.SKIRTING -> skirtingLength(space) to "محيط الحوائط ناقص عروض الأبواب"
        CalcMethod.WATERPROOFING -> waterproofArea(space,item.waterproofUpstand) to
            "الأرضية + رجوع العزل ${item.waterproofUpstand} م على الحوائط"
        CalcMethod.FLOOR_LAYER_VOLUME -> floorArea(space)*item.layerThickness.coerceAtLeast(0.0) to
            "مساحة الأرضية × متوسط السمك ${item.layerThickness} م"
        CalcMethod.DIRECT_AREA,
        CalcMethod.DIRECT_LENGTH,
        CalcMethod.DIRECT_VOLUME,
        CalcMethod.DIRECT_COUNT -> item.directValue to "كمية فعلية مدخلة مباشرة"
    }

    fun calculate(space:SpaceEntry,item:TakeoffItem):QuantityBreakdown{
        val (base,explanation)=baseValue(space,item)
        val additions=item.adjustments.filter{it.type==AdjustmentType.ADD}.sumOf{it.amount}
        val deductions=item.adjustments.filter{it.type==AdjustmentType.DEDUCT}.sumOf{it.amount}
        val afterAdjust=(base+additions-deductions).coerceAtLeast(0.0)
        val waste=afterAdjust*(item.wastePercent.coerceAtLeast(0.0)/100.0)
        val calculated=afterAdjust+waste
        val final=item.manualOverride ?: calculated
        return QuantityBreakdown(base,additions,deductions,waste,calculated,final,item.manualOverride!=null,explanation)
    }

    fun purchaseInfo(space:SpaceEntry,item:TakeoffItem):PurchaseInfo?{
        if(item.unit!=MeasureUnit.AREA || item.pieceWidth<=0.0 || item.pieceHeight<=0.0)return null
        val pieceArea=item.pieceWidth*item.pieceHeight
        if(pieceArea<=0.0)return null
        val pieces=ceil(calculate(space,item).final/pieceArea).toInt().coerceAtLeast(0)
        val packs=if(item.piecesPerPack>0)ceil(pieces.toDouble()/item.piecesPerPack.toDouble()).toInt() else null
        return PurchaseInfo(pieceArea,pieces,packs)
    }

    fun projectSummary(project:SiteProject):Map<Pair<String,MeasureUnit>,Double>{
        val result=linkedMapOf<Pair<String,MeasureUnit>,Double>()
        project.sections.forEach{section->
            section.spaces.forEach{space->
                space.takeoffs.forEach{item->
                    val key=item.name to item.unit
                    result[key]=(result[key]?:0.0)+calculate(space,item).final
                }
            }
        }
        return result
    }

    fun sectionSummary(section:SectionEntry):Map<Pair<String,MeasureUnit>,Double>{
        val result=linkedMapOf<Pair<String,MeasureUnit>,Double>()
        section.spaces.forEach{space->
            space.takeoffs.forEach{item->
                val key=item.name to item.unit
                result[key]=(result[key]?:0.0)+calculate(space,item).final
            }
        }
        return result
    }
}

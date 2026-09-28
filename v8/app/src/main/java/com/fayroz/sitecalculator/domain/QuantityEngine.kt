package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.max

object QuantityEngine {
    fun openingArea(space:Space):Double =
        space.openings.sumOf{it.width*it.height*it.count}

    fun doorWidth(space:Space):Double =
        space.openings.filter{it.kind==OpeningKind.DOOR}.sumOf{it.width*it.count}

    fun wallGross(space:Space):Double =
        if(space.walls.isNotEmpty())space.walls.sumOf{it.length*it.height}
        else 2.0*(space.length+space.width)*space.height

    fun perimeter(space:Space):Double =
        if(space.walls.isNotEmpty())space.walls.sumOf{it.length}
        else 2.0*(space.length+space.width)

    fun floorArea(space:Space):Double = space.length*space.width

    private fun tiledOpeningDeduction(space:Space,tileHeight:Double):Double =
        space.openings.sumOf{o->
            val bottom=if(o.kind==OpeningKind.WINDOW)o.sill else 0.0
            val overlap=max(0.0,minOf(bottom+o.height,tileHeight)-bottom)
            o.width*overlap*o.count
        }

    fun calculateOne(space:Space,item:Takeoff):QuantityResult{
        val base=when(item.kind){
            CalcKind.WALLS -> max(0.0,wallGross(space)-openingArea(space))
            CalcKind.CEILING,CalcKind.FLOOR -> floorArea(space)
            CalcKind.WALL_TILES -> max(
                0.0,
                perimeter(space)*minOf(space.height,item.tileHeight)-tiledOpeningDeduction(space,item.tileHeight)
            )
            CalcKind.SKIRTING -> max(0.0,perimeter(space)-doorWidth(space))
            CalcKind.WATERPROOF -> floorArea(space)+perimeter(space)*item.upstand
            CalcKind.SCREED_VOLUME -> floorArea(space)*item.layerThickness
            CalcKind.DIRECT -> item.directValue
        }

        val additions=item.adjustments.filter{it.kind==AdjustKind.ADD}.sumOf{it.amount}
        val deductions=item.adjustments.filter{it.kind==AdjustKind.DEDUCT}.sumOf{it.amount}
        val adjusted=max(0.0,base+additions-deductions)
        val waste=adjusted*(item.waste/100.0)
        val oneSpace=item.manualValue ?: adjusted+waste

        // أهم قاعدة في V8:
        // التكرار لا يدخل أبدًا داخل حساب البند نفسه. يطبق مرة واحدة فقط هنا.
        val repeated=oneSpace*space.repeatCount.coerceAtLeast(1)

        return QuantityResult(
            base=base,
            additions=additions,
            deductions=deductions,
            waste=waste,
            oneSpaceFinal=oneSpace,
            repeatedFinal=repeated,
            explanation="كمية المكان الواحد ${f(base)}، النهائية ${f(oneSpace)}. التكرار ×${space.repeatCount.coerceAtLeast(1)} بيتطبق مرة واحدة بس وقت التجميع."
        )
    }

    fun summarize(project:Project):List<SummaryLine>{
        val map=linkedMapOf<Pair<String,UnitType>,Double>()
        project.sections.forEach{section->
            section.spaces.forEach{space->
                space.takeoffs.forEach{item->
                    val key=item.name to item.unit
                    map[key]=(map[key]?:0.0)+calculateOne(space,item).repeatedFinal
                }
            }
        }
        return map.map{SummaryLine(it.key.first,it.key.second,it.value)}
    }

    fun summarize(section:Section):List<SummaryLine>{
        val map=linkedMapOf<Pair<String,UnitType>,Double>()
        section.spaces.forEach{space->
            space.takeoffs.forEach{item->
                val key=item.name to item.unit
                map[key]=(map[key]?:0.0)+calculateOne(space,item).repeatedFinal
            }
        }
        return map.map{SummaryLine(it.key.first,it.key.second,it.value)}
    }

    fun contributors(project:Project,itemName:String):List<Pair<String,Double>>{
        val out=mutableListOf<Pair<String,Double>>()
        project.sections.forEach{section->
            section.spaces.forEach{space->
                space.takeoffs.filter{it.name==itemName}.forEach{item->
                    out += "${section.name} ← ${space.name}" to calculateOne(space,item).repeatedFinal
                }
            }
        }
        return out
    }

    private fun f(v:Double)=String.format(java.util.Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
}

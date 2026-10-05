package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.ceil

object CostEngine {
    data class Row(val sectionId:String,val spaceId:String,val itemId:String,val partId:String?,
        val location:String,val item:String,val part:String,val quantity:Double,val unit:String,
        val cementKg:Double=0.0,val sandM3:Double=0.0,val extra:Double=0.0,
        val materialCost:Double=0.0,val labor:Double=0.0,val transport:Double=0.0,val equipment:Double=0.0,
        val spec:MaterialSpec?=null,val formula:String="",
        val calculatorId:String?=null,val calculatorInputs:Map<String,String> = emptyMap(),val materialLines:List<MaterialLine> = emptyList()) {
        val total get()=materialCost+labor+transport+equipment
    }
    fun defaultSpec(name:String,defaults:Map<String,String> = emptyMap()):MaterialSpec? {
        if(!isMortar(name))return null
        fun d(k:String,f:Double)=defaults[k]?.toDoubleOrNull()?:f
        val splash=name.contains("طرطشة")
        val screed=name.contains("تسوية")
        return MaterialSpec(thicknessMm=d(if(splash)"splashThickness" else "plasterThickness",if(splash)5.0 else if(screed)50.0 else 15.0),
            sandParts=d(if(splash)"splashSand" else "plasterSand",if(splash)2.0 else 4.0),
            waste=d("mortarWaste",5.0),cementPrice=d("cementPrice",0.0),sandPrice=d("sandPrice",0.0))
    }
    fun isMortar(name:String)=name.contains("محارة")||name.contains("طرطشة")||name.contains("مونة")
    fun measure(area:Double,s:MaterialSpec):Triple<Double,Double,Double> {
        val dry=area*s.thicknessMm/1000*s.dryFactor*(1+s.waste/100)
        val sum=s.cementParts+s.sandParts
        require(sum>0&&s.bagKg>0&&s.thicknessMm>=0&&s.waste>=0)
        return Triple(dry*s.cementParts/sum*s.cementDensity,dry*s.sandParts/sum,area*s.extraRate*(1+s.waste/100))
    }
    fun rows(project:Project):List<Row> = buildList {
        project.sections.forEach{section->section.spaces.forEach{space->space.takeoffs.forEach{item->
            val q=QuantityEngine.calculateOne(space,item)
            val values=if(item.parts.isEmpty()||item.manualValue!=null)listOf(null to q.oneSpaceFinal)
                else item.parts.map{it to QuantityEngine.partValue(it,item.unit)}
            // Item-level adjustments distributed proportionally to preserve item total.
            val partsTotal=values.sumOf{it.second}
            val factor=if(partsTotal>0)q.oneSpaceFinal/partsTotal else 1.0
            values.forEach{(part,base)->
                val qty=base*factor*space.repeatCount
                val spec=part?.material?:item.material?:defaultSpec(item.name,project.defaults)
                val mat=if(spec!=null)measure(qty,spec)else Triple(0.0,0.0,0.0)
                val def=if(spec==null)CalculatorLibrary.forItem(item.name)else null
                val inputs=(def?.let{CalculatorLibrary.defaults(it,project.defaults)}.orEmpty()+("waste" to item.waste.toString()))+(part?.calculatorInputs?.takeIf{it.isNotEmpty()}?:item.calculatorInputs)
                val raw=def?.let{CalculatorLibrary.recipe(it,inputs,qty)}.orEmpty()
                val answer=def?.let{runCatching{CalculatorLibrary.evaluate(it,raw)}.getOrNull()}
                val cost=if(spec!=null)mat.first/spec.bagKg*spec.cementPrice+mat.second*spec.sandPrice+mat.third*spec.extraPrice else answer?.consumedCost?:0.0
                add(Row(section.id,space.id,item.id,part?.id,"${section.name} / ${space.name}",item.name,
                    part?.name?:"كامل البند",qty,item.unit.label,mat.first,mat.second,mat.third,cost,
                    qty*(spec?.laborRate?:inputs["_laborRate"]?.toDoubleOrNull()?:0.0),qty*(spec?.transportRate?:inputs["_transportRate"]?.toDoubleOrNull()?:0.0),qty*(spec?.equipmentRate?:inputs["_equipmentRate"]?.toDoubleOrNull()?:0.0),spec,q.explanation,def?.id,raw,
                    if(def!=null&&answer!=null)CalculatorLibrary.result(def,raw,answer).lines else emptyList()))
            }
        }}}
    }
    data class Purchase(val material:String,val unit:String,val amount:Double,val packages:Int?,val price:Double,val cost:Double)
    fun purchase(rows:List<Row>):List<Purchase> = buildList {
        // Different package sizes/prices remain separate, quantities rounded only after aggregation.
        rows.filter{it.calculatorId!=null}.groupBy{r->r.calculatorId to r.calculatorInputs.filterKeys{it !in setOf("area","length","count")}}.forEach{(key,group)->
            val def=CalculatorLibrary.all.first{it.id==key.first}
            val raw=CalculatorLibrary.recipe(def,key.second,group.sumOf{it.quantity})
            val answer=runCatching{CalculatorLibrary.evaluate(def,raw)}.getOrNull()
            if(answer!=null)answer.outputs.filter{it.unit !in setOf("جنيه","جنيه/م²")}.forEach{line->
                add(Purchase("${def.title} — ${line.label}",line.unit,line.value,null,0.0,0.0))
            }
            if(answer!=null)add(Purchase("${def.title} — إجمالي شراء","جنيه",answer.cost,null,0.0,answer.cost))
        }
        rows.filter{it.spec!=null}.groupBy{Triple(it.spec!!.bagKg,it.spec.cementPrice,"أسمنت")}.forEach{(key,group)->
            val kg=group.sumOf{it.cementKg};if(kg>0){val bags=ceil(kg/key.first).toInt();add(Purchase("أسمنت (${key.first} كجم)","كجم",kg,bags,key.second,bags*key.second))}
        }
        rows.filter{it.spec!=null}.groupBy{it.spec!!.sandPrice}.forEach{(price,group)->
            val amount=group.sumOf{it.sandM3};if(amount>0)add(Purchase("رمل","م³",amount,null,price,amount*price))
        }
        rows.filter{it.spec!=null&&it.extra>0}.groupBy{it.spec!!.extraName to it.spec.extraPrice}.forEach{(key,group)->
            val amount=group.sumOf{it.extra};add(Purchase(key.first,"وحدة",amount,null,key.second,amount*key.second))
        }
    }
}

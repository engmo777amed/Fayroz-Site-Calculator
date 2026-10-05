package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.ceil

object CostEngine {
    private fun numeric(raw:String?):Double?=raw.orEmpty().map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()?.takeIf{it.isFinite()}
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
        fun d(k:String,f:Double)=numeric(defaults[k])?:f
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
                val mat=if(spec!=null)measure(if(item.unit==UnitType.VOLUME)qty/(spec.thicknessMm/1000)else qty,spec)else Triple(0.0,0.0,0.0)
                val def=if(spec==null)CalculatorLibrary.forItem(item.name)else null
                val inputs=(def?.let{CalculatorLibrary.defaults(it,project.defaults)}.orEmpty()+("waste" to item.waste.toString()))+(part?.calculatorInputs?.takeIf{it.isNotEmpty()}?:item.calculatorInputs)
                val raw=def?.let{CalculatorLibrary.recipe(it,inputs,qty)}.orEmpty()
                val answer=def?.let{runCatching{CalculatorLibrary.evaluate(it,raw)}.getOrNull()}
                val cost=if(spec!=null)mat.first/spec.bagKg*spec.cementPrice+mat.second*spec.sandPrice+mat.third*spec.extraPrice else answer?.consumedCost?:0.0
                add(Row(section.id,space.id,item.id,part?.id,"${section.name} / ${space.name}",item.name,
                    part?.name?:"كامل البند",qty,item.unit.label,mat.first,mat.second,mat.third,cost,
                    qty*(spec?.laborRate?:numeric(inputs["_laborRate"])?:0.0),qty*(spec?.transportRate?:numeric(inputs["_transportRate"])?:0.0),qty*(spec?.equipmentRate?:numeric(inputs["_equipmentRate"])?:0.0),spec,q.explanation,def?.id,raw,
                    if(def!=null&&answer!=null)CalculatorLibrary.result(def,raw,answer).lines else emptyList()))
            }
        }}}
    }
    data class Purchase(val material:String,val unit:String,val amount:Double,val packages:Int?,val price:Double,val cost:Double,val packageUnit:String="عبوة")
    fun purchase(rows:List<Row>):List<Purchase> = buildList {
        // Different package sizes/prices remain separate, quantities rounded only after aggregation.
        rows.filter{it.calculatorId!=null}.groupBy{r->r.calculatorId to r.calculatorInputs.filterKeys{k->k !in setOf("area","length","count")&&CalculatorLibrary.all.first{it.id==r.calculatorId}.fields.any{it.key==k}}}.forEach{(key,group)->
            val def=CalculatorLibrary.all.first{it.id==key.first}
            val raw=CalculatorLibrary.recipe(def,key.second,group.sumOf{it.quantity})
            val answer=runCatching{CalculatorLibrary.evaluate(def,raw)}.getOrNull()
            if(answer!=null)addAll(calculatorPurchases(def,raw,answer))
        }
        rows.filter{it.spec!=null}.groupBy{Triple(it.spec!!.bagKg,it.spec.cementPrice,"أسمنت")}.forEach{(key,group)->
            val kg=group.sumOf{it.cementKg};if(kg>0){val bags=ceil(kg/key.first).toInt();add(Purchase("أسمنت (${key.first} كجم)","كجم",kg,bags,key.second,bags*key.second,"شيكارة"))}
        }
        rows.filter{it.spec!=null}.groupBy{it.spec!!.sandPrice}.forEach{(price,group)->
            val amount=group.sumOf{it.sandM3};if(amount>0)add(Purchase("رمل","م³",amount,null,price,amount*price))
        }
        rows.filter{it.spec!=null&&it.extra>0}.groupBy{it.spec!!.extraName to it.spec.extraPrice}.forEach{(key,group)->
            val amount=group.sumOf{it.extra};add(Purchase(key.first,"وحدة",amount,null,key.second,amount*key.second))
        }
    }
    fun calculatorPurchases(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer):List<Purchase> {
        fun v(k:String)=numeric(raw[k])?:0.0
        fun out(label:String)=answer.outputs.firstOrNull{it.label==label}?.value?:0.0
        fun packageRow(name:String,amount:Double,unit:String,packs:Double,price:Double,packUnit:String="عبوة")=
            Purchase(name,unit,amount,packs.toInt(),price,packs*price,packUnit)
        if(def.id in CalculatorLibrary.mortarIds){
            val kg=out("أسمنت فعلي");val bags=out("شراء أسمنت");val sand=out("رمل")
            return buildList{
                add(packageRow("أسمنت (${v("bag")} كجم)",kg,"كجم",bags,v("cementPrice"),"شيكارة"))
                add(Purchase("رمل","م³",sand,null,v("sandPrice"),sand*v("sandPrice")))
                if(out("مادة إضافية")>0)add(Purchase("مادة إضافية","وحدة",out("مادة إضافية"),null,v("extraPrice"),out("مادة إضافية")*v("extraPrice")))
            }
        }
        return when(def.id){
            "tile"->listOf(packageRow("بلاط / رخام / جرانيت",out("بعد الهالك"),"م²",out("عبوات الشراء"),v("price"),"كرتونة"))
            "skirting"->listOf(packageRow("وزرات",v("length")*(1+v("waste")/100),"م ط",out("عبوات"),v("price")))
            "paint"->listOf(packageRow("دهان",out("استهلاك دهان"),"لتر",out("عبوات"),v("price")))
            "adhesive","putty","primer","woodpaint","waterproof","block_adhesive","tack","marking"->listOf(packageRow(def.title,out("كمية فعلية"),answer.outputs.firstOrNull{it.label=="كمية فعلية"}?.unit?:"وحدة",out("عبوات كاملة"),v("price")))
            "gypsum"->listOf(packageRow("ألواح جبس",out("مساحة تغطية بالهالك"),"م²",out("ألواح"),v("price"),"لوح"))
            "masonry","blocks"->listOf(
                Purchase(if(def.id=="masonry")"طوب" else "بلوك","وحدة",out("وحدات شراء"),null,v("brickPrice")/1000,out("وحدات شراء")/1000*v("brickPrice")),
                packageRow("أسمنت (50 كجم)",out("أسمنت فعلي"),"كجم",out("أسمنت"),v("cementPrice"),"شيكارة"),
                Purchase("رمل","م³",out("رمل"),null,v("sandPrice"),out("رمل")*v("sandPrice")))
            else->emptyList()
        }.filter{it.amount>0||it.packages?.let{n->n>0}==true}
    }

}


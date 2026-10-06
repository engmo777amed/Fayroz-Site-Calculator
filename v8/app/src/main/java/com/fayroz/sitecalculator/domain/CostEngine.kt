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
        val base=MaterialSpec(thicknessMm=d(if(splash)"splashThickness" else if(screed)"screedThickness" else "plasterThickness",if(splash)5.0 else if(screed)50.0 else 15.0),
            sandParts=d(if(splash)"splashSand" else if(screed)"screedSand" else "plasterSand",if(splash)2.0 else 4.0),
            waste=d("mortarWaste",5.0),cementPrice=d("cementPrice",0.0),sandPrice=d("sandPrice",0.0),cementName=defaults["cementName"]?:"أسمنت",sandName=defaults["sandName"]?:"رمل")
        val key=if(splash)"splashBagsPerSand"else if(screed)"screedBagsPerSand"else "plasterBagsPerSand"
        return numeric(defaults[key])?.let{MortarMix.withBags(base,it)}?:base
    }
    fun reprice(project:Project):Project {
        fun spec(s:MaterialSpec)=s.copy(cementPrice=numeric(project.defaults["cementPrice"])?.let{it*s.bagKg/50}?:s.cementPrice,sandPrice=numeric(project.defaults["sandPrice"])?:s.sandPrice)
        fun inputs(id:String?,raw:Map<String,String>):Map<String,String> {
            val def=CalculatorLibrary.all.firstOrNull{it.id==id}?:return raw
            val prices=def.fields.filter{it.key.contains("price",true)}.mapNotNull{f->
                val value=project.defaults["recipe.${def.id}.${f.key}"]?:project.defaults[f.key]?.takeIf{f.key in setOf("cementPrice","sandPrice")}
                value?.let{v->f.key to if(f.key=="cementPrice"&&def.id in CalculatorLibrary.mortarIds&&project.defaults["recipe.${def.id}.${f.key}"]==null) ((numeric(v)?:0.0)*(numeric(raw["bag"])?:50.0)/50).toString()else v}
            }.toMap()
            return raw+prices
        }
        return project.copy(sections=project.sections.map{s->s.copy(spaces=s.spaces.map{sp->sp.copy(takeoffs=sp.takeoffs.map{t->
            val id=CalculatorLibrary.forItem(t.name)?.id
            t.copy(material=t.material?.let(::spec),calculatorInputs=inputs(id,t.calculatorInputs),parts=t.parts.map{part->part.copy(material=part.material?.let(::spec),calculatorInputs=if(part.calculatorInputs.isEmpty())emptyMap()else inputs(id,part.calculatorInputs))})
        })})},calculations=project.calculations.map{c->
            if(c.inputs["_includeInProject"]!="true")c else {
                val def=CalculatorLibrary.all.firstOrNull{it.id==c.toolId}
                val raw=inputs(c.toolId,c.inputs)
                val answer=def?.let{runCatching{CalculatorLibrary.evaluate(it,raw)}.getOrNull()}
                c.copy(inputs=raw,cost=answer?.cost?:c.cost)
            }
        },updatedAt=System.currentTimeMillis())
    }
    data class SellingPrice(val direct:Double,val overhead:Double,val profit:Double,val tax:Double){val total get()=direct+overhead+profit+tax}
    fun selling(project:Project,rows:List<Row>):SellingPrice {
        val direct=rows.sumOf{it.total}
        val overhead=direct*(numeric(project.defaults["overheadPercent"])?:0.0)/100
        val profit=(direct+overhead)*(numeric(project.defaults["profitPercent"])?:0.0)/100
        val tax=(direct+overhead+profit)*(numeric(project.defaults["taxPercent"])?:0.0)/100
        return SellingPrice(direct,overhead,profit,tax)
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
        project.calculations.filter{it.inputs["_includeInProject"]=="true"}.forEach{c->
            val def=CalculatorLibrary.all.firstOrNull{it.id==c.toolId}?:return@forEach
            val answer=runCatching{CalculatorLibrary.evaluate(def,c.inputs)}.getOrNull()?:return@forEach
            val result=CalculatorLibrary.result(def,c.inputs,answer)
            val measured=answer.outputs.firstOrNull{it.label.contains("صافي")&&!it.label.contains("فائض")}?:answer.outputs.firstOrNull{it.label in setOf("المساحة","وزن","نقاط","حجم مفيد","طول صافي","مساحة شدة")}
            val useInput=def.fields.any{it.key=="area"}||def.id in setOf("pipes","cables","wires","skirting","skirting_cut","sealant","kerb","conduits","trays","points","fittings")
            val qty=if(useInput)result.sourceQuantity.takeIf{it>0}?:measured?.value?:1.0 else measured?.value?:result.sourceQuantity.takeIf{it>0}?:1.0
            val unit=if(useInput)result.sourceUnit.ifBlank{measured?.unit?:"حساب"}else measured?.unit?:result.sourceUnit.ifBlank{"حساب"}
            add(Row(c.sectionId.orEmpty(),c.spaceId.orEmpty(),c.id,null,"حساب مضاف / ${c.inputs["_source"].orEmpty()}",c.title,"كامل الحساب",qty,unit,
                materialCost=answer.consumedCost,labor=qty*(numeric(c.inputs["_laborRate"])?:0.0),transport=qty*(numeric(c.inputs["_transportRate"])?:0.0),equipment=qty*(numeric(c.inputs["_equipmentRate"])?:0.0),
                formula=result.explanation,calculatorId=def.id,calculatorInputs=c.inputs+("_savedCalculation" to "true"),materialLines=result.lines))
        }
    }
    data class Purchase(val material:String,val unit:String,val amount:Double,val packages:Int?,val price:Double,val cost:Double,val packageUnit:String="عبوة",val packageSize:Double?=null)
    fun purchase(rows:List<Row>):List<Purchase> = consolidate(buildList {
        // Different package sizes/prices remain separate, quantities rounded only after aggregation.
        rows.filter{it.calculatorId!=null&&it.calculatorInputs["_savedCalculation"]!="true"}.groupBy{r->r.calculatorId to r.calculatorInputs.filterKeys{k->k !in setOf("area","length","count")&&(k=="_materialName"||k=="_cementName"||k=="_sandName"||CalculatorLibrary.all.first{it.id==r.calculatorId}.fields.any{it.key==k})}}.forEach{(key,group)->
            val def=CalculatorLibrary.all.first{it.id==key.first}
            val raw=CalculatorLibrary.recipe(def,key.second,group.sumOf{it.quantity})
            val answer=runCatching{CalculatorLibrary.evaluate(def,raw)}.getOrNull()
            if(answer!=null)addAll(calculatorPurchases(def,raw,answer))
        }
        rows.filter{it.calculatorInputs["_savedCalculation"]=="true"}.forEach{r->
            val def=CalculatorLibrary.all.first{it.id==r.calculatorId}
            runCatching{CalculatorLibrary.evaluate(def,r.calculatorInputs)}.getOrNull()?.let{addAll(calculatorPurchases(def,r.calculatorInputs,it))}
        }
        rows.filter{it.spec!=null}.groupBy{Triple(it.spec!!.bagKg,it.spec.cementPrice,it.spec.cementName)}.forEach{(key,group)->
            val kg=group.sumOf{it.cementKg};if(kg>0){val bags=ceil(kg/key.first).toInt();add(Purchase("${key.third} (${java.math.BigDecimal.valueOf(key.first).stripTrailingZeros().toPlainString()} كجم)","كجم",kg,bags,key.second,bags*key.second,"شيكارة",key.first))}
        }
        rows.filter{it.spec!=null}.groupBy{it.spec!!.sandName to it.spec.sandPrice}.forEach{(key,group)->
            val amount=group.sumOf{it.sandM3};if(amount>0)add(Purchase(key.first,"م³",amount,null,key.second,amount*key.second))
        }
        rows.filter{it.spec!=null&&it.extra>0}.groupBy{it.spec!!.extraName to it.spec.extraPrice}.forEach{(key,group)->
            val amount=group.sumOf{it.extra};add(Purchase(key.first,"وحدة",amount,null,key.second,amount*key.second))
        }
    })
    fun consolidate(purchases:List<Purchase>):List<Purchase> = purchases.groupBy{listOf(it.material,it.unit,it.price,it.packageUnit,it.packageSize)}.values.map{group->
        val first=group.first();val amount=group.sumOf{it.amount};val size=first.packageSize
        if(size!=null&&size>0){val packs=ceil(amount/size-1e-10).toInt();first.copy(amount=amount,packages=packs,cost=packs*first.price)}
        else first.copy(amount=amount,packages=if(first.packages==null)null else group.sumOf{it.packages?:0},cost=group.sumOf{it.cost})
    }
    fun calculatorPurchases(def:CalcDef,raw:Map<String,String>,answer:CalcAnswer):List<Purchase> {
        fun v(k:String)=numeric(raw[k])?:0.0
        fun out(label:String)=answer.outputs.firstOrNull{it.label==label}?.value?:0.0
        fun packageRow(name:String,amount:Double,unit:String,packs:Double,price:Double,packUnit:String="عبوة")=
            Purchase(if(name.startsWith("أسمنت"))name.replace("أسمنت",raw["_cementName"]?:"أسمنت")else raw["_materialName"]?.takeIf{it.isNotBlank()}?:name,unit,amount,packs.toInt(),price,packs*price,packUnit,when {unit=="كجم"&&name.contains("أسمنت")->v("bag").takeIf{it>0}?:50.0;def.id=="tile"->v("pack")*v("tileW")*v("tileH")/10000;def.id=="skirting"->v("piece")*v("pack");def.id=="gypsum"->v("boardL")*v("boardW");else->v("pack").takeIf{it>0}})
        if(def.id in CalculatorLibrary.mortarIds){
            val kg=out("أسمنت فعلي");val bags=out("شراء أسمنت");val sand=out("رمل")
            return buildList{
                add(packageRow("أسمنت (${java.math.BigDecimal.valueOf(v("bag")).stripTrailingZeros().toPlainString()} كجم)",kg,"كجم",bags,v("cementPrice"),"شيكارة"))
                add(Purchase(raw["_sandName"]?:"رمل","م³",sand,null,v("sandPrice"),sand*v("sandPrice")))
                if(out("مادة إضافية")>0)add(Purchase("مادة إضافية","وحدة",out("مادة إضافية"),null,v("extraPrice"),out("مادة إضافية")*v("extraPrice")))
            }
        }
        fun stockName(fallback:String)=raw["_materialName"]?.takeIf{it.isNotBlank()}?:fallback
        return when(def.id){
            "tile"->listOf(packageRow("بلاط / رخام / جرانيت",out("بعد الهالك"),"م²",out("عبوات الشراء"),v("price"),"كرتونة"))
            "skirting"->listOf(packageRow("وزرات",v("length")*(1+v("waste")/100),"م ط",out("عبوات"),v("price")))
            "paint"->listOf(packageRow("دهان",out("استهلاك دهان"),"لتر",out("عبوات"),v("price")))
            "adhesive","putty","primer","woodpaint","waterproof","block_adhesive","tack","marking"->listOf(packageRow(def.title,out("كمية فعلية"),answer.outputs.firstOrNull{it.label=="كمية فعلية"}?.unit?:"وحدة",out("عبوات كاملة"),v("price")))
            "grout"->listOf(packageRow("روبة فواصل",out("روبة"),"كجم",out("عبوات"),v("price")))
            "gypsum"->listOf(packageRow("ألواح جبس",out("مساحة تغطية بالهالك"),"م²",out("ألواح"),v("price"),"لوح"))
            "masonry","blocks"->listOf(
                Purchase(if(def.id=="masonry")"طوب" else "بلوك","وحدة",out("وحدات شراء"),null,v("brickPrice")/1000,out("وحدات شراء")/1000*v("brickPrice")),
                packageRow("أسمنت (50 كجم)",out("أسمنت فعلي"),"كجم",out("أسمنت"),v("cementPrice"),"شيكارة"),
                Purchase(raw["_sandName"]?:"رمل","م³",out("رمل"),null,v("sandPrice"),out("رمل")*v("sandPrice")))
            "gypsum_system"->{val a=v("area")*(1+v("waste")/100);listOf(
                Purchase(stockName("نظام جبس")+" / قطاعات","م ط",a*v("profiles"),out("قطاعات").toInt(),v("profilePrice"),out("قطاعات")*v("profilePrice"),"قطاع",v("profileL")),
                Purchase(stockName("نظام جبس")+" / علاقات","عدد",out("علاقات"),null,v("hangerPrice"),out("علاقات")*v("hangerPrice")),
                Purchase(stockName("نظام جبس")+" / مسامير","عدد",out("مسامير"),null,v("screwPrice"),out("مسامير")*v("screwPrice")))}
            "conduits"->listOf(Purchase(stockName("مواسير كهرباء"),"م",v("length")*(1+v("waste")/100),out("مواسير").toInt(),v("price"),out("مواسير")*v("price"),"ماسورة",v("piece")),Purchase(stockName("كهرباء")+" / علب","علبة",out("علب"),null,v("boxPrice"),out("علب")*v("boxPrice")))
            "trays"->listOf(Purchase(stockName("حوامل كابلات"),"م",v("length")*(1+v("waste")/100),out("قطع حوامل").toInt(),v("price"),out("قطع حوامل")*v("price"),"قطعة",v("piece")),Purchase(stockName("حوامل كابلات")+" / دعامات","عدد",out("دعامات لمسار مستقيم"),null,v("supportPrice"),out("دعامات لمسار مستقيم")*v("supportPrice")))
            "interlock"->listOf(Purchase(stockName("إنترلوك"),"م²",out("مساحة شراء"),null,v("price"),answer.cost),Purchase("فرشة إنترلوك — السعر غير مدخل","م³",out("حجم فرشة"),null,0.0,0.0))
            "rolls"->listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:"لفائف عزل","م²",out("مساحة بالرجوع والهالك"),out("لفات").toInt(),v("price"),answer.cost,"لفة",out("تغطية فعالة للفة")))
            "insulation"->listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:"ألواح عزل حراري","م²",v("area")*(1+v("waste")/100),out("ألواح").toInt(),v("price"),answer.cost,"لوح",v("boardL")*v("boardW")))
            "sealant"->listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:"مادة ملء الفواصل","مل",out("استهلاك"),out("عبوات").toInt(),v("price"),answer.cost,"عبوة",v("pack")))
            "pipes"->listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:"مواسير تغذية وصرف","م",out("طول مطلوب"),out("مواسير شراء").toInt(),v("price"),answer.cost,"ماسورة",v("piece")))
            "wires"->listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:"أسلاك","م",out("طول مطلوب"),out("لفات").toInt(),v("price"),answer.cost,"لفة",v("roll")))
            else->{
                val qty=answer.outputs.firstOrNull{it.label.contains("توريد")||it.label.contains("شراء")}?:answer.outputs.firstOrNull()
                if(qty!=null&&answer.cost>=0&&def.fields.any{it.key.contains("price",true)})listOf(Purchase(raw["_materialName"]?.takeIf{it.isNotBlank()}?:def.title,qty.unit,qty.value,null,if(qty.value>0)answer.cost/qty.value else 0.0,answer.cost))else emptyList()
            }
        }.filter{it.amount>0||it.packages?.let{n->n>0}==true}
    }

}


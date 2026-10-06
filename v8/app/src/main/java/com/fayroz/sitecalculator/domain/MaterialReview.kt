package com.fayroz.sitecalculator.domain
import com.fayroz.sitecalculator.core.*
object MaterialReview {
    private fun numberValue(raw:String?):Double?=raw.orEmpty().map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').toDoubleOrNull()
    private fun number(raw:String?):Double=numberValue(raw)?:0.0
    fun specError(s:MaterialSpec):String? {
        val values=listOf(s.thicknessMm,s.cementParts,s.sandParts,s.dryFactor,s.cementDensity,s.bagKg,s.waste,s.cementPrice,s.sandPrice,s.extraRate,s.extraPrice,s.laborRate,s.transportRate,s.equipmentRate)
        if(values.any{!it.isFinite()||it<0})return "اكتب قيمًا صحيحة صفر أو أكبر في مواصفات المواد والأسعار."
        return when{
            s.thicknessMm<=0->"متوسط السمك يجب أن يكون أكبر من صفر."
            s.cementParts+s.sandParts<=0->"أدخل عدد شكاير الأسمنت على متر الرمل."
            s.dryFactor<=0->"معامل الحجم الجاف يجب أن يكون أكبر من صفر."
            s.cementDensity<=0->"كثافة الأسمنت يجب أن تكون أكبر من صفر."
            s.bagKg<=0->"وزن شيكارة الأسمنت يجب أن يكون أكبر من صفر."
            else->null
        }
    }
    fun missing(def:CalcDef,raw:Map<String,String>):List<String> = def.fields.filter{f->
        f.key.contains("price",true)&&!(f.key=="extraPrice"&&number(raw["extraRate"])==0.0)&&!(f.key=="boxPrice"&&number(raw["boxes"])==0.0)&&number(raw[f.key])<=0
    }.map{it.label}
    fun missing(row:CostEngine.Row):List<String> {
        val s=row.spec
        if(s!=null)return buildList{if(row.cementKg>0&&s.cementPrice<=0)add("سعر شيكارة الأسمنت");if(row.sandM3>0&&s.sandPrice<=0)add("سعر متر الرمل");if(row.extra>0&&s.extraPrice<=0)add("سعر ${s.extraName}")}
        val def=CalculatorLibrary.all.firstOrNull{it.id==row.calculatorId}?:return emptyList()
        return missing(def,row.calculatorInputs)
    }
    fun itemError(space:Space,item:Takeoff):String? {
        if(item.parts.any{p->p.deduction<0||!p.deduction.isFinite()||(item.unit==UnitType.AREA&&(p.length<=0||p.width<=0||!p.length.isFinite()||!p.width.isFinite()))})return "راجع أطوال وعروض وخصومات الأجزاء."
        Validation.space(space.copy(name=space.name.ifBlank{"المكان"},takeoffs=listOf(item)))?.let{return it}
        (listOfNotNull(item.material)+item.parts.mapNotNull{it.material}).forEach{specError(it)?.let{error->return error}}
        val def=CalculatorLibrary.forItem(item.name)
        if(def!=null&&item.material==null){
            val scopes=if(item.parts.isEmpty()||item.manualValue!=null)listOf(null to QuantityEngine.calculateOne(space,item).repeatedFinal)else item.parts.map{it to QuantityEngine.partValue(it,item.unit)*space.repeatCount}
            scopes.forEach{(part,quantity)->
                val raw=item.calculatorInputs+(part?.calculatorInputs.orEmpty())
                val message=runCatching{CalculatorLibrary.evaluate(def,CalculatorLibrary.recipe(def,raw,quantity))}.exceptionOrNull()?.message
                if(message!=null)return "${part?.name?:item.name}: $message"
                if(raw.filterKeys{it.startsWith("_")&&it.endsWith("Rate")}.values.any{it.isNotBlank()&&numberValue(it)?.let{v->v.isFinite()&&v>=0}!=true})return "راجع المصنعية والنقل والمعدات."
            }
        }
        return null
    }
}


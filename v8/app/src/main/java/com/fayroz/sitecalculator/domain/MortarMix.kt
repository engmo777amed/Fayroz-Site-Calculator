package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.MaterialSpec

/** Site input: cement bags for one cubic metre of sand. Legacy recipes retain their quantities. */
object MortarMix {
    const val key="_bagsPerSand"
    fun number(s:String?):Double?=s.orEmpty().map{if(it.isDigit())it.digitToInt().digitToChar()else it}.joinToString("").replace('٫','.').replace(',','.').replace("٬","").toDoubleOrNull()?.takeIf{it.isFinite()}
    fun bags(raw:Map<String,String>):Double=number(raw[key])?:((number(raw["cement"])?:1.0)/(number(raw["sand"])?:4.0)*(number(raw["density"])?:1440.0)/(number(raw["bag"])?:50.0))
    fun bags(s:MaterialSpec):Double=if(s.sandParts>0&&s.bagKg>0)s.cementParts/s.sandParts*s.cementDensity/s.bagKg else 0.0
    fun withBags(s:MaterialSpec,bags:Double)=s.copy(cementParts=if(s.cementDensity>0&&bags.isFinite()&&s.bagKg.isFinite())bags*s.bagKg/s.cementDensity else -1.0,sandParts=1.0)
    fun normalize(raw:Map<String,String>):Map<String,String>{
        if(key !in raw)return raw
        val bags=number(raw[key]);val bag=number(raw["bag"])?:50.0;val density=number(raw["density"])?:1440.0
        require(bags!=null&&bags>0){"أدخل عدد الشكاير على متر الرمل أكبر من صفر"}
        require(bag>0&&density>0){"راجع وزن الشيكارة وكثافة الأسمنت"}
        return raw+mapOf("cement" to (bags*bag/density).toString(),"sand" to "1")
    }
    fun summary(raw:Map<String,String>):String="${format(bags(raw))} شيكارة × ${format(number(raw["bag"])?:50.0)} كجم على ١ م³ رمل"
    private fun format(v:Double)=java.math.BigDecimal.valueOf(v).setScale(3,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    fun isMix(def:CalcDef)=def.fields.any{it.key=="cement"}&&def.fields.any{it.key=="sand"}
    fun basic(def:CalcDef)=def.fields.filter{f->!f.key.contains("price",true)&&f.key !in setOf("cement","sand","bag","density","dry","waste","extraRate")&&
        ((f.required&&f.default.isBlank())||f.key in setOf("area","length","count","thickness","coats","rate","coverage","wall","bags","tileW","tileH","tileL","pack","piece","roll","layers","sides","slope","start","end","brickL","brickW","brickH","boardL","boardW","strip","tread","riser","jointW","jointD","rollL","rollW","conductors"))}
}

package com.fayroz.sitecalculator.domain

/** Price identity includes product specification and purchase packaging, never work quantity. */
object PriceBook {
    val stockKeys=setOf("pack","piece","roll","tileW","tileH","tileL","boardL","boardW","brickL","brickW","brickH","rollL","rollW","profileL","stock","pieceArea","diameter")
    fun specification(def:CalcDef,raw:Map<String,String>):String = buildString{
        append(raw["_materialName"].orEmpty().trim())
        def.fields.filter{it.key in stockKeys}.sortedBy{it.key}.forEach{f->
            val text=raw[f.key]?:f.default
            val number=MortarMix.number(text)
            append('|');append(f.key);append('=');append(if(number!=null)java.math.BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()else text)
        }
    }
    fun key(def:CalcDef,raw:Map<String,String>,field:String):String {
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(specification(def,raw).toByteArray())
        return "pricebook.${def.id}."+digest.joinToString(""){"%02x".format(it)}+".$field"
    }
    fun record(def:CalcDef,raw:Map<String,String>):Map<String,String> = buildMap{
        def.fields.filter{it.key in stockKeys}.forEach{put("recipe.${def.id}.${it.key}",raw[it.key]?:it.default)}
        put("recipe.${def.id}._materialName",raw["_materialName"].orEmpty())
        def.fields.filter{it.key.contains("price",true)&&it.key !in setOf("cementPrice","sandPrice")}.forEach{put(key(def,raw,it.key),raw[it.key]?:"0")}
    }
    fun apply(def:CalcDef,raw:Map<String,String>,book:Map<String,String>):Map<String,String> = raw+buildMap{
        def.fields.filter{it.key.contains("price",true)}.forEach{f->
            if(raw["_priceOverride.${f.key}"]=="true")return@forEach
            val indexed=book[key(def,raw,f.key)]
            val legacy=book["recipe.${def.id}.${f.key}"]?.takeIf{
                specification(def,raw)==specification(def,CalculatorLibrary.baseDefaults(def,book))
            }
            val value=when(f.key){
                "cementPrice"->legacy?:indexed?:book["cementPrice"]?.let{if((MortarMix.number(raw["bag"])?:50.0)==50.0)it else ((MortarMix.number(it)?:0.0)*(MortarMix.number(raw["bag"])?:50.0)/50).toString()}
                "sandPrice"->legacy?:indexed?:book["sandPrice"]
                else->legacy?:indexed
            }
            // A configured book with no matching specification must not reuse an old price.
            if(value!=null)put(f.key,value)
            else if(book.keys.any{it.startsWith("pricebook.${def.id}.")||it=="recipe.${def.id}.${f.key}"})put(f.key,"0")
        }
    }
}

package com.fayroz.sitecalculator.core

import java.util.UUID

data class Opening(
    val id:String=UUID.randomUUID().toString(),
    val type:String,
    val width:Double,
    val height:Double,
    val count:Int=1
){
    val area:Double get()=width*height*count
    val totalWidth:Double get()=width*count
}

data class RoomEntry(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val type:String,
    val length:Double,
    val width:Double,
    val height:Double,
    val tileHeight:Double=2.4,
    val waste:Double=7.0,
    val repeatCount:Int=1,
    val note:String="",
    val openings:List<Opening> = emptyList(),
    val works:List<String> = emptyList()
)

data class SectionEntry(
    val id:String=UUID.randomUUID().toString(),
    var name:String,
    val rooms:MutableList<RoomEntry> = mutableListOf()
)

data class SiteProject(
    val id:String=UUID.randomUUID().toString(),
    var name:String,
    var type:String,
    val sections:MutableList<SectionEntry> = mutableListOf()
)

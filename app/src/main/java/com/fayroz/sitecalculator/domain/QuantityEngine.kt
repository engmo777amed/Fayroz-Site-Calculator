package com.fayroz.sitecalculator.domain

import com.fayroz.sitecalculator.core.*
import kotlin.math.min

object QuantityEngine {
    val works=listOf(
        "محارة الحوائط","محارة السقف","دهان الحوائط","دهان السقف",
        "الأرضيات","سيراميك الحوائط","الوزرات","عزل الأرضية","سقف جبس بورد"
    )

    fun floor(r:RoomEntry)=r.length*r.width*r.repeatCount
    fun doorsArea(r:RoomEntry)=r.openings.filter{it.type=="باب"}.sumOf{it.area}
    fun windowsArea(r:RoomEntry)=r.openings.filter{it.type=="شباك"}.sumOf{it.area}
    fun openingsArea(r:RoomEntry)=r.openings.sumOf{it.area}
    fun doorsWidth(r:RoomEntry)=r.openings.filter{it.type=="باب"}.sumOf{it.totalWidth}

    fun grossWalls(r:RoomEntry)=2*(r.length+r.width)*r.height*r.repeatCount
    fun netWalls(r:RoomEntry)=(
        2*(r.length+r.width)*r.height-openingsArea(r)
    ).coerceAtLeast(0.0)*r.repeatCount

    fun wallTiles(r:RoomEntry):Double {
        val factor=1+r.waste/100.0
        val one=(
            2*(r.length+r.width)*min(r.tileHeight,r.height)-openingsArea(r)
        ).coerceAtLeast(0.0)
        return one*r.repeatCount*factor
    }

    fun skirting(r:RoomEntry):Double {
        val factor=1+r.waste/100.0
        val one=(2*(r.length+r.width)-doorsWidth(r)).coerceAtLeast(0.0)
        return one*r.repeatCount*factor
    }

    fun waterproof(r:RoomEntry):Double {
        val factor=1+r.waste/100.0
        val one=r.length*r.width+2*(r.length+r.width)*0.20
        return one*r.repeatCount*factor
    }

    fun quantity(r:RoomEntry,work:String)=when(work){
        "محارة الحوائط","دهان الحوائط" -> netWalls(r)
        "محارة السقف","دهان السقف" -> floor(r)
        "الأرضيات","سقف جبس بورد" -> floor(r)*(1+r.waste/100.0)
        "سيراميك الحوائط" -> wallTiles(r)
        "الوزرات" -> skirting(r)
        "عزل الأرضية" -> waterproof(r)
        else -> 0.0
    }

    fun unit(work:String)=if(work=="الوزرات")"م ط" else "م²"

    fun sectionSummary(section:SectionEntry)=works.associateWith{w->
        section.rooms.filter{w in it.works}.sumOf{quantity(it,w)}
    }.filterValues{it>0.0}

    fun projectSummary(project:SiteProject)=works.associateWith{w->
        project.sections.sumOf{s->s.rooms.filter{w in it.works}.sumOf{quantity(it,w)}}
    }.filterValues{it>0.0}
}

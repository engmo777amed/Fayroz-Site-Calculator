package com.fayroz.sitecalculator.core

import java.util.UUID

enum class UnitType(val label:String){ AREA("م²"), LENGTH("م ط"), VOLUME("م³"), COUNT("عدد") }
enum class WorkStatus(val label:String){ NOT_STARTED("لسه ما بدأتش"), IN_PROGRESS("شغال عليها"), DONE("خلصت الحصر"), REVIEWED("اتراجعت") }
enum class OpeningKind(val label:String){ DOOR("باب"), WINDOW("شباك"), VOID("فتحة") }
enum class CalcKind(val label:String){
    WALLS("حوائط المكان"), CEILING("السقف"), FLOOR("الأرضية"), WALL_TILES("سيراميك الحوائط"),
    SKIRTING("الوزرات"), WATERPROOF("عزل الأرضية"), SCREED_VOLUME("مونة تسوية بالحجم"), DIRECT("كمية جاهزة")
}
enum class AdjustKind{ ADD, DEDUCT }

data class Opening(
    val id:String=UUID.randomUUID().toString(),
    val kind:OpeningKind=OpeningKind.DOOR,
    val width:Double=0.0,
    val height:Double=0.0,
    val sill:Double=0.0,
    val count:Int=1
)

data class WallPart(
    val id:String=UUID.randomUUID().toString(),
    val name:String="حائط",
    val length:Double=0.0,
    val height:Double=3.0
)

data class Adjustment(
    val id:String=UUID.randomUUID().toString(),
    val kind:AdjustKind=AdjustKind.ADD,
    val amount:Double=0.0,
    val note:String=""
)

data class Takeoff(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val unit:UnitType,
    val kind:CalcKind,
    val waste:Double=0.0,
    val tileHeight:Double=2.4,
    val upstand:Double=0.20,
    val layerThickness:Double=0.0,
    val directValue:Double=0.0,
    val manualValue:Double?=null,
    val adjustments:List<Adjustment> = emptyList()
)

data class Space(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val type:String,
    val length:Double=0.0,
    val width:Double=0.0,
    val height:Double=3.0,
    val repeatCount:Int=1,
    val walls:List<WallPart> = emptyList(),
    val openings:List<Opening> = emptyList(),
    val takeoffs:List<Takeoff> = emptyList(),
    val note:String="",
    val status:WorkStatus=WorkStatus.IN_PROGRESS,
    val updatedAt:Long=System.currentTimeMillis()
)

data class Section(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val spaces:List<Space> = emptyList()
)

data class SavedCalculation(
    val id:String=UUID.randomUUID().toString(),
    val toolId:String,
    val title:String,
    val summary:String,
    val sourceQuantity:Double=0.0,
    val unit:String="",
    val sectionId:String?=null,
    val spaceId:String?=null,
    val createdAt:Long=System.currentTimeMillis()
)

data class Project(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val type:String="شقة",
    val sections:List<Section> = emptyList(),
    val calculations:List<SavedCalculation> = emptyList(),
    val createdAt:Long=System.currentTimeMillis(),
    val updatedAt:Long=System.currentTimeMillis()
)

data class ActiveLocation(
    val projectId:String,
    val sectionId:String?=null,
    val spaceId:String?=null
)

data class QuantityResult(
    val base:Double,
    val additions:Double,
    val deductions:Double,
    val waste:Double,
    val oneSpaceFinal:Double,
    val repeatedFinal:Double,
    val explanation:String
)

data class SummaryLine(val name:String,val unit:UnitType,val quantity:Double)
data class MaterialLine(val label:String,val value:String)
data class MaterialResult(
    val toolId:String,
    val title:String,
    val sourceQuantity:Double,
    val sourceUnit:String,
    val lines:List<MaterialLine>,
    val explanation:String
)

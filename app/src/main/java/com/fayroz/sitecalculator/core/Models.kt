package com.fayroz.sitecalculator.core

import java.util.UUID

enum class MeasureUnit(val label:String){ AREA("م²"), LENGTH("م ط"), VOLUME("م³"), COUNT("عدد") }
enum class CaptureStatus(val label:String){ NOT_STARTED("لسه ما بدأتش"), IN_PROGRESS("شغال عليها"), DONE("خلصت الحصر"), REVIEWED("اتراجعت") }
enum class AdjustmentType { ADD, DEDUCT }
enum class OpeningType(val label:String){ DOOR("باب"), WINDOW("شباك"), VOID("فتحة") }

enum class CalcMethod(val label:String){
    ROOM_WALLS("حوائط المكان"),
    WALL_SEGMENTS("أكتر من حائط"),
    FLOOR_SURFACES("قسم الأرضية لأجزاء"),
    CEILING_SURFACES("قسم السقف لأجزاء"),
    WALL_TILES("سيراميك الحوائط"),
    SKIRTING("وزرات"),
    WATERPROOFING("عزل أرضية"),
    FLOOR_LAYER_VOLUME("مونة تسوية بالحجم"),
    DIRECT_AREA("عندي المساحة جاهزة"),
    DIRECT_LENGTH("عندي الطول جاهز"),
    DIRECT_VOLUME("عندي الحجم جاهز"),
    DIRECT_COUNT("عندي العدد جاهز")
}

data class Opening(
    val id:String=UUID.randomUUID().toString(),
    val wallId:String?=null,
    val type:OpeningType=OpeningType.DOOR,
    val width:Double=0.0,
    val height:Double=0.0,
    val sillHeight:Double=0.0,
    val revealDepth:Double=0.0,
    val count:Int=1,
    val note:String=""
)

data class WallSegment(
    val id:String=UUID.randomUUID().toString(),
    val name:String="حائط",
    val length:Double=0.0,
    val height:Double=3.0,
    val note:String=""
)

data class SurfaceSegment(
    val id:String=UUID.randomUUID().toString(),
    val name:String="مسطح",
    val length:Double=0.0,
    val width:Double=0.0,
    val deductionArea:Double=0.0,
    val note:String=""
)

data class Adjustment(
    val id:String=UUID.randomUUID().toString(),
    val type:AdjustmentType=AdjustmentType.ADD,
    val amount:Double=0.0,
    val note:String=""
)

data class TakeoffItem(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val category:String,
    val unit:MeasureUnit,
    val method:CalcMethod,
    val wastePercent:Double=0.0,
    val tileHeight:Double=2.4,
    val waterproofUpstand:Double=0.20,
    val layerThickness:Double=0.0,
    val includeOpeningReveals:Boolean=false,
    val wallIds:List<String> = emptyList(),
    val directValue:Double=0.0,
    val pieceWidth:Double=0.0,
    val pieceHeight:Double=0.0,
    val piecesPerPack:Int=0,
    val manualOverride:Double?=null,
    val overrideReason:String="",
    val adjustments:List<Adjustment> = emptyList(),
    val note:String=""
)

data class SpaceEntry(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val type:String,
    val length:Double=0.0,
    val width:Double=0.0,
    val height:Double=3.0,
    val repeatCount:Int=1,
    val walls:List<WallSegment> = emptyList(),
    val floorSurfaces:List<SurfaceSegment> = emptyList(),
    val ceilingSurfaces:List<SurfaceSegment> = emptyList(),
    val openings:List<Opening> = emptyList(),
    val takeoffs:List<TakeoffItem> = emptyList(),
    val note:String="",
    val photoUris:List<String> = emptyList(),
    val status:CaptureStatus=CaptureStatus.IN_PROGRESS,
    val updatedAt:Long=System.currentTimeMillis()
)

data class SectionEntry(
    val id:String=UUID.randomUUID().toString(),
    var name:String,
    val spaces:MutableList<SpaceEntry> = mutableListOf()
)

data class SiteProject(
    val id:String=UUID.randomUUID().toString(),
    var name:String,
    var type:String,
    val sections:MutableList<SectionEntry> = mutableListOf(),
    val createdAt:Long=System.currentTimeMillis(),
    var updatedAt:Long=System.currentTimeMillis()
)

data class AppDefaults(
    val defaultHeight:Double=3.0,
    val defaultTileHeight:Double=2.4,
    val waterproofUpstand:Double=0.20,
    val floorWaste:Double=7.0,
    val wallTileWaste:Double=5.0,
    val gypsumWaste:Double=10.0,
    val skirtingWaste:Double=5.0
)

data class PurchaseInfo(
    val pieceArea:Double,
    val pieces:Int,
    val packs:Int?
)

data class QuantityBreakdown(
    val base:Double,
    val additions:Double,
    val deductions:Double,
    val waste:Double,
    val calculated:Double,
    val final:Double,
    val overridden:Boolean,
    val explanation:String
)

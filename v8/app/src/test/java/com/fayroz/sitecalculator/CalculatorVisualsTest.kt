package com.fayroz.sitecalculator
import org.junit.Assert.*
import org.junit.Test
import com.fayroz.sitecalculator.domain.*
class CalculatorVisualsTest {
 companion object {
  fun tableExample(id:String):Map<String,String> {
   val rows:List<List<String>> = when(id){
    "level_survey"->listOf(listOf("1"))
    "level_closure"->listOf(listOf("1.5","1.4","100"))
    "section_volumes"->listOf(listOf("0","2","0"),listOf("10","4","2"))
    "mixed_cutting"->listOf(listOf("5","2"),listOf("2","1"))
    "coordinate_area"->listOf(listOf("0","0"),listOf("4","0"),listOf("4","3"),listOf("0","3"))
    "sieve"->listOf(listOf("4.75","100"),listOf("2.36","300"),listOf("0","600"))
    "proctor"->listOf(listOf("8","1944"),listOf("10","2200"),listOf("12","2128"))
    else->return emptyMap()
   }
   val schema=ExtraCalculators.tables.getValue(id)
   return buildMap{put("_rows",rows.size.toString());rows.forEachIndexed{i,r->schema.columns.forEachIndexed{j,c->put("_row.$i.${c.key}",r[j])}}}
  }
 }
 private fun calc(id:String,vararg changes:Pair<String,String>):CalcAnswer {
  val d=CalculatorLibrary.all.first{it.id==id};return CalculatorLibrary.evaluate(d,d.fields.associate{it.key to it.default}+tableExample(id)+changes.toMap())
 }
 private fun value(a:CalcAnswer,label:String)=a.outputs.first{it.label==label}.value
 private fun reject(id:String,vararg changes:Pair<String,String>){assertTrue(id,runCatching{calc(id,*changes)}.isFailure)}
 @Test fun rampSlopeAndLength(){val a=calc("garage_ramp");assertEquals(12.0,value(a,"ميل الجزء الرئيسي"),1e-10);assertEquals(kotlin.math.hypot(10.0,1.2),value(a,"الطول المائل الكلي"),1e-10)}
 @Test fun rampInverseModes(){assertEquals(10.0,value(calc("garage_ramp","_rampMode" to "length"),"الطول الأفقي الكلي"),1e-10);assertEquals(1.2,value(calc("garage_ramp","_rampMode" to "drop"),"فرق المنسوب نزولًا"),1e-10)}
 @Test fun segmentedRamp(){val a=calc("garage_ramp","_rampMode" to "parts","topLength" to "2","topSlope" to "5","length" to "6","slope" to "15","bottomLength" to "2","bottomSlope" to "5");assertEquals(1.1,value(a,"فرق المنسوب نزولًا"),1e-10);assertEquals(10.0,value(a,"الطول الأفقي الكلي"),1e-10)}
 @Test fun rampRejectsZeroKnownLength(){reject("garage_ramp","length" to "0")}
 @Test fun rampArabicDigits(){assertEquals(12.0,value(calc("garage_ramp","rise" to "١٫٢","length" to "١٠"),"ميل الجزء الرئيسي"),1e-10)}
 @Test fun stirrupPurchaseIsWeight(){val d=CalculatorLibrary.all.first{it.id=="stirrups"};val r=d.fields.associate{it.key to it.default}+mapOf("a" to "20","b" to "30","hooks" to "20","diameter" to "8","count" to "100","price" to "50000");val a=CalculatorLibrary.evaluate(d,r);val p=CostEngine.calculatorPurchases(d,r,a).single();assertEquals("كجم",p.unit);assertEquals(47.4074074074,p.amount,1e-8);assertEquals(a.cost,p.cost,1e-10)}
 @Test fun masonryRejectsImpossibleThickness(){reject("masonry","area" to "10","wall" to "1")}
 @Test fun stairsRejectConflictingDimensions(){reject("stairs_concrete","width" to "1","run" to "3","rise" to "1.7","steps" to "100")}
 @Test fun horizontalTankHalfFull(){val a=calc("cylindrical_tank","_tankOrientation" to "horizontal");assertEquals(value(a,"سعة كلية")/2,value(a,"حجم السائل"),1e-10)}
 @Test fun horizontalTankEmptyAndFull(){assertEquals(0.0,value(calc("cylindrical_tank","_tankOrientation" to "horizontal","fill" to "0"),"حجم السائل"),1e-10);val a=calc("cylindrical_tank","_tankOrientation" to "horizontal","fill" to "2");assertEquals(value(a,"سعة كلية"),value(a,"حجم السائل"),1e-10)}
 @Test fun tankRejectsOverfill(){reject("cylindrical_tank","_tankOrientation" to "horizontal","fill" to "2.1")}
 @Test fun moistureKnownWeights(){assertEquals(20.0,value(calc("soil_moisture"),"المحتوى المائي"),1e-10);reject("soil_moisture","dry" to "20")}
 @Test fun sandConeKnownVolume(){val a=calc("sand_cone");assertEquals(2000.0,value(a,"حجم الحفرة"),1e-10);assertEquals(2/1.1,value(a,"كثافة جافة"),1e-10)}
 @Test fun compactionRatio(){assertEquals(95.0,value(calc("compaction"),"نسبة الدمك"),1e-10)}
 @Test fun compressionUsesCorrectFace(){assertEquals(500000.0/22500,value(calc("concrete_compression"),"مقاومة الضغط"),1e-10);assertEquals(500000.0/(kotlin.math.PI*150*150/4),value(calc("concrete_compression","_specimen" to "cylinder"),"مقاومة الضغط"),1e-10)}
 @Test fun slumpKnownDrop(){assertEquals(80.0,value(calc("slump"),"الهبوط"),1e-10);reject("slump","sampleHeight" to "301")}
 @Test fun asphaltCoreDisplacement(){assertEquals(1200.0/((1205-700)/.998),value(calc("asphalt_core"),"كثافة الكور"),1e-10)}
 @Test fun asphaltVoids(){assertEquals(2.5,value(calc("asphalt_voids"),"Gmm"),1e-10);assertEquals(6.0,value(calc("asphalt_voids"),"فراغات هوائية"),1e-10)}
 @Test fun aggregateAbsorption(){assertEquals(2.0,value(calc("aggregate"),"امتصاص"),1e-10)}
 @Test fun surveyUsesReference(){assertEquals(100.5,value(calc("level_survey"),"منسوب نقطة 1"),1e-10)}
 @Test fun closureEndsAtKnownLevel(){assertEquals(100.0,value(calc("level_closure"),"منسوب مصحح محطة 1"),1e-10)}
 @Test fun sectionsKeepCutAndFillSeparate(){val a=calc("section_volumes");assertEquals(30.0,value(a,"حجم حفر"),1e-10);assertEquals(10.0,value(a,"حجم ردم"),1e-10)}
 @Test fun coordinatesAndIntersection(){assertEquals(12.0,value(calc("coordinate_area"),"المساحة"),1e-10);reject("coordinate_area","_row.1.x" to "4","_row.1.y" to "3","_row.2.x" to "4","_row.2.y" to "0")}
 @Test fun missingTableCellIsNotZero(){reject("coordinate_area","_row.1.x" to "")}
 @Test fun proctorKeepsMeasuredPeak(){assertEquals(2.0,value(calc("proctor"),"أعلى كثافة جافة مقاسة"),1e-10);assertEquals(10.0,value(calc("proctor"),"الرطوبة عند أعلى نقطة مقاسة"),1e-10)}
 @Test fun sieveBalancesMass(){assertEquals(90.0,value(calc("sieve"),"مار منخل 4.75 مم"),1e-10);reject("sieve","_row.2.mass" to "100")}
 @Test fun tileAxisPreservesSpan(){for(center in listOf(false,true))for(span in listOf(.3,.6,1.2,4.0)){val x=ExtraCalculators.tileAxis(span,.6,.003,center);assertTrue(x.all{it>0&&it<=.6+1e-9});assertEquals(span,x.sum()+(x.size-1)*.003,1e-9)}}
 @Test fun mixedCuttingConservesLength(){val a=calc("mixed_cutting","kerf" to "3");assertEquals(2.0,value(a,"أسياخ خام"),1e-10);assertEquals(24.0,value(a,"أطوال قطع صافية")+value(a,"فاقد قص")+value(a,"فضلات متبقية"),1e-10)}
 @Test fun slopedExcavationReducesToBox(){assertEquals(40.0,value(calc("sloped_excavation","sideX" to "0","sideY" to "0"),"حجم الحفر"),1e-10)}
 @Test fun variableVolumeBoxLimit(){assertEquals(24.0,value(calc("variable_volume","topL" to "4","topW" to "3"),"الحجم"),1e-10)}
 @Test fun consumptionCannotBeNegative(){reject("consumption_check","returned" to "300")}
 @Test fun listQuantitiesRejectFractions(){reject("mixed_cutting","_row.0.count" to "1.5")}
 @Test fun readingsSurviveCodec(){val raw=tableExample("coordinate_area")+mapOf("_tankOrientation" to "horizontal");val p=com.fayroz.sitecalculator.core.Project(name="test",calculations=listOf(com.fayroz.sitecalculator.core.SavedCalculation(toolId="coordinate_area",title="plot",summary="",inputs=raw)));val decoded=com.fayroz.sitecalculator.data.V8Codec.decodeProjects(com.fayroz.sitecalculator.data.V8Codec.encodeProjects(listOf(p))).single();assertEquals(raw,decoded.calculations.single().inputs)}
 @Test fun derivedRampInputDoesNotBlockAnotherMode(){assertEquals(10.0,value(calc("garage_ramp","_rampMode" to "length","length" to "bad"),"الطول الأفقي الكلي"),1e-10)}
 @Test fun cylinderDoesNotRequireUnusedFaceLength(){assertTrue(calc("concrete_compression","_specimen" to "cylinder","b" to "").outputs.all{it.value.isFinite()})}
 @Test fun meshIndependentDiameters(){val a=calc("mesh","length" to "2","width" to "2","diameter" to "10","diameterY" to "20","spacing" to "20","spacingY" to "25","cover" to "0","waste" to "0");assertEquals(11.0,value(a,"أسياخ اتجاه الطول للشبكة"),0.0);assertEquals(9.0,value(a,"أسياخ اتجاه العرض للشبكة"),0.0);assertEquals((11*2*100+9*2*400)/162.0,value(a,"وزن شراء"),1e-10)}
 @Test fun optionalMeshDiameterPreservesOldPriceIdentity(){val d=CalculatorLibrary.all.first{it.id=="mesh"};val old=d.copy(fields=d.fields.filter{it.key !in setOf("spacingY","diameterY")});val raw=d.fields.associate{it.key to it.default};assertEquals(PriceBook.key(old,raw,"price"),PriceBook.key(d,raw,"price"))}
}

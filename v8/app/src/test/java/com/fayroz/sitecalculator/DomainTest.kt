package com.fayroz.sitecalculator
import org.junit.Assert.*
import org.junit.Test
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.domain.*
import com.fayroz.sitecalculator.data.V8Codec

class DomainTest {
 private fun calc(id:String,vararg args:Pair<String,String>):CalcAnswer {
    val d=CalculatorLibrary.all.first{it.id==id}
    return CalculatorLibrary.evaluate(d,d.fields.associate{it.key to it.default}+args.toMap())
 }
 @Test fun wasteIsNotWorkQuantity(){
    val s=Space(name="غرفة",type="",length=10.0,width=10.0,repeatCount=2)
    val t=Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.FLOOR,waste=7.0,pieceWidth=1.0,pieceHeight=1.0)
    assertEquals(200.0,QuantityEngine.calculateOne(s,t).repeatedFinal,1e-9)
    assertEquals(214,QuantityEngine.purchaseInfo(s,t)!!.pieces)
 }
 @Test fun wallSubsetDoesNotStealUnassignedOpenings(){
    val s=Space(name="",type="",walls=listOf(WallPart(id="a",length=4.0,height=3.0),WallPart(id="b",length=4.0,height=3.0)),openings=listOf(Opening(width=1.0,height=2.0)))
    assertEquals(12.0,QuantityEngine.wallAreaOne(s,wallIds=setOf("b")),1e-9)
    assertEquals(10.0,QuantityEngine.wallAreaOne(s,wallIds=setOf("a")),1e-9)
 }
 @Test fun revealAboveTilesAddsNothing(){
    val s=Space(name="",type="",length=4.0,width=3.0,height=3.0,openings=listOf(Opening(kind=OpeningKind.WINDOW,width=1.0,height=1.0,sill=2.0,revealDepth=.2)))
    assertEquals(QuantityEngine.wallTilesOne(s,1.5,false),QuantityEngine.wallTilesOne(s,1.5,true),1e-9)
 }
 @Test fun ceilingIsIndependent(){
    val s=Space(name="",type="",length=4.0,width=3.0,ceilingSurfaces=listOf(SurfacePart(length=2.0,width=3.0)))
    assertEquals(6.0,QuantityEngine.ceilingAreaOne(s),1e-9)
    assertEquals(12.0,QuantityEngine.floorAreaOne(s),1e-9)
    assertEquals(CalcKind.CEILING,Catalog.items.first{it.name=="سقف جبس بورد"}.kind)
 }
 @Test fun partsAreIndependent(){
    val t=Takeoff(name="محارة الأسقف",unit=UnitType.AREA,kind=CalcKind.CEILING,parts=listOf(WorkPart(name="جزء",length=2.0,width=3.0,deduction=1.0)))
    val s=Space(name="",type="",length=10.0,width=10.0,repeatCount=3,takeoffs=listOf(t))
    assertEquals(15.0,QuantityEngine.calculateOne(s,t).repeatedFinal,1e-9)
 }
 @Test fun purchaseRoundedAfterAggregation(){
    val spec=MaterialSpec(cementPrice=200.0,sandPrice=300.0)
    val t=Takeoff(name="محارة الحوائط",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=1.0,material=spec)
    val p=Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="1",type="",takeoffs=listOf(t)),Space(name="2",type="",takeoffs=listOf(t.copy(id="t2")))))))
    val rows=CostEngine.rows(p);val purchase=CostEngine.purchase(rows).first{it.material.startsWith("أسمنت")}
    assertEquals(1,purchase.packages)
    assertTrue(rows.sumOf{it.materialCost}<purchase.cost+rows.sumOf{it.sandM3}*300)
 }
 @Test fun perPartSpecsAndProjectDefaults(){
    val t=Takeoff(name="محارة الحوائط",unit=UnitType.AREA,kind=CalcKind.WALLS,parts=listOf(WorkPart(name="أ",length=1.0,width=1.0,material=MaterialSpec(thicknessMm=10.0)),WorkPart(name="ب",length=1.0,width=1.0,material=MaterialSpec(thicknessMm=20.0))))
    val rows=CostEngine.rows(Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(t)))))))
    assertEquals(rows[0].cementKg*2,rows[1].cementKg,1e-9)
 }
 @Test fun mortarVolumeDoesNotMultiplyThicknessTwice(){
    val t=Takeoff(name="مونة تسوية الأرضيات",unit=UnitType.VOLUME,kind=CalcKind.DIRECT,directValue=1.0,material=MaterialSpec(thicknessMm=50.0,waste=0.0))
    val space=Space(name="",type="",takeoffs=listOf(t))
    val project=Project(name="",sections=listOf(Section(name="",spaces=listOf(space))))
    val row=CostEngine.rows(project).single()
    assertEquals(1.33/5*1440,row.cementKg,1e-9)
    assertEquals(1.33*4/5,row.sandM3,1e-9)
 }
 @Test fun tileConsumptionDoesNotRoundPerPart(){
    val recipe=mapOf("tileW" to "50","tileH" to "50","pack" to "4","price" to "100","waste" to "0")
    val t=Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.FLOOR,calculatorInputs=recipe,parts=listOf(WorkPart(length=.5,width=1.0),WorkPart(length=.5,width=1.0)))
    val p=Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(t))))))
    val rows=CostEngine.rows(p)
    assertEquals(100.0,rows.sumOf{it.materialCost},1e-9)
    assertEquals(100.0,CostEngine.purchase(rows).sumOf{it.cost},1e-9)
 }
 @Test fun codecPreservesAllNewData(){
    val t=Takeoff(name="طرطشة الأسقف",unit=UnitType.AREA,kind=CalcKind.CEILING,material=MaterialSpec(cementPrice=100.0),parts=listOf(WorkPart(length=2.0,width=3.0,material=MaterialSpec(thicknessMm=5.0),calculatorInputs=mapOf("rate" to "5"))),surfaceIds=listOf("s1"))
    val p=Project(name="اختبار",archived=true,defaults=mapOf("cementPrice" to "100"),sections=listOf(Section(name="دور",spaces=listOf(Space(name="غرفة",type="",takeoffs=listOf(t))))),calculations=listOf(SavedCalculation(toolId="tile",title="حساب",summary="",inputs=mapOf("area" to "12"),cost=300.0)))
    assertEquals(p,V8Codec.decodeProjects(V8Codec.encodeProjects(listOf(p))).single())
 }
 @Test fun tilePurchase(){val a=calc("tile","area" to "100","waste" to "7","tileW" to "50","tileH" to "50","pack" to "4","price" to "200");assertEquals(21400.0,a.cost,1e-9)}
 @Test fun slopeSigned(){val a=calc("slope","length" to "10","start" to "-1","slope" to "-1");assertEquals(-1.1,a.outputs.first{it.label=="منسوب النهاية"}.value,1e-9)}
 @Test fun steelWeight(){assertEquals(16.0*16/162*12*10,calc("steel","diameter" to "16","length" to "12","count" to "10","waste" to "0").outputs[0+1].value,1e-9)}
 @Test fun asphaltTons(){assertEquals(24.31,calc("asphalt","area" to "200","thickness" to "5","density" to "2.431","waste" to "0").outputs[0].value,1e-9)}
 @Test fun groutDimensions(){val a=calc("grout","area" to "10");assertTrue(a.outputs[0].value>0&&a.outputs[0].value<1)}
 @Test fun reverseMasonryThickness(){val a=calc("masonry","area" to "10","wall" to "12");val b=calc("masonry","area" to "10","wall" to "25");assertTrue(b.outputs[1].value>a.outputs[1].value*1.95)}
 @Test fun rejectsNegativeDimensions(){try{calc("rectangle","length" to "-1","width" to "3");fail()}catch(_:IllegalArgumentException){}}
 @Test fun rejectsNonFinite(){try{calc("rectangle","length" to "Infinity","width" to "3");fail()}catch(_:IllegalArgumentException){}}
 @Test fun rejectsInvalidRollOverlap(){try{calc("rolls","area" to "10","overlapW" to "110");fail()}catch(_:IllegalArgumentException){}}
 @Test fun arabicDigits(){assertEquals(12.0,calc("rectangle","length" to "٤","width" to "٣").outputs[0].value,1e-9)}
 @Test fun allCalculatorDefinitionsHaveUniqueIds(){assertEquals(CalculatorLibrary.all.size,CalculatorLibrary.all.map{it.id}.distinct().size);assertTrue(CalculatorLibrary.all.size>=50)}
 @Test fun allCalculatorsEvaluateRepresentativeInputs(){CalculatorLibrary.all.forEach{d->
    val v=d.fields.associate{it.key to (it.default.ifBlank{when(it.key){"diameter"->"16";"pieceArea"->"0.02";"wall"->"12";else->"2"}})}
    val a=CalculatorLibrary.evaluate(d,v);assertTrue(d.id,a.outputs.all{it.value.isFinite()})
 }}
 @Test fun materialReviewValidatesEachPartAndAllowsMissingPrices(){
    val t=Takeoff(name="محارة الأسقف",unit=UnitType.AREA,kind=CalcKind.CEILING,material=MaterialSpec(),parts=listOf(WorkPart(name="جزء",length=2.0,width=3.0)))
    val space=Space(name="غرفة",type="",length=4.0,width=3.0,takeoffs=listOf(t))
    assertNull(com.fayroz.sitecalculator.domain.MaterialReview.itemError(space,t))
    val row=CostEngine.rows(Project(name="",sections=listOf(Section(name="",spaces=listOf(space))))).single()
    assertEquals(2,com.fayroz.sitecalculator.domain.MaterialReview.missing(row).size)
    val invalid=t.copy(parts=t.parts.map{it.copy(material=MaterialSpec(thicknessMm=0.0))})
    assertNotNull(com.fayroz.sitecalculator.domain.MaterialReview.itemError(space,invalid))
    assertNotNull(com.fayroz.sitecalculator.domain.MaterialReview.specError(MaterialSpec(cementPrice=-1.0)))
 }
 @Test fun optionalUnusedAdditionPriceIsNotRequired(){
    val def=CalculatorLibrary.all.first{it.id=="plaster"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("area" to "12.5","cementPrice" to "٢٠٠","sandPrice" to "٣٠٠")
    assertTrue(com.fayroz.sitecalculator.domain.MaterialReview.missing(def,raw).isEmpty())
 }
 @Test fun blankOptionalPricesAllowQuantityResults(){
    val def=CalculatorLibrary.all.first{it.id=="paint"}
    val inputs=def.fields.associate{it.key to it.default}+mapOf("area" to "10","price" to "")
    val answer=CalculatorLibrary.evaluate(def,inputs)
    assertEquals(2.1,answer.outputs.first{it.label=="استهلاك دهان"}.value,1e-9)
    assertEquals(0.0,answer.cost,0.0)
    assertTrue(com.fayroz.sitecalculator.domain.MaterialReview.missing(def,inputs).isNotEmpty())
    assertTrue(runCatching{CalculatorLibrary.evaluate(def,inputs+("area" to ""))}.isFailure)
 }
 @Test fun procurementGroupsIgnoreDisplayUnitsAndLaborRates(){
    val recipe=mapOf("tileW" to "50","tileH" to "50","pack" to "4","price" to "100","waste" to "0")
    val item=Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.FLOOR,calculatorInputs=recipe,parts=listOf(
      WorkPart(name="أ",length=.5,width=.5,calculatorInputs=recipe+("_laborRate" to "10")+("_unit.tileW" to "سم")),
      WorkPart(name="ب",length=.5,width=.5,calculatorInputs=recipe+("_laborRate" to "20")+("_unit.tileW" to "مم"))))
    val project=Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(item))))))
    val rows=CostEngine.rows(project)
    assertEquals(7.5,rows.sumOf{it.labor},1e-9)
    assertEquals(100.0,CostEngine.purchase(rows).sumOf{it.cost},1e-9)
 }
 @Test fun projectPricesAndLaborAcceptArabicNumbers(){
    val defaults=mapOf("cementPrice" to "٢٠٠","sandPrice" to "٣٠٠")
    assertEquals(200.0,CostEngine.defaultSpec("محارة الأسقف",defaults)!!.cementPrice,0.0)
    val plaster=CalculatorLibrary.all.first{it.id=="plaster"}
    assertEquals("٢٠٠",CalculatorLibrary.defaults(plaster,defaults)["cementPrice"])
    val recipe=mapOf("tileW" to "50","tileH" to "50","pack" to "4","price" to "100","waste" to "0","_laborRate" to "١٠")
    val item=Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.FLOOR,calculatorInputs=recipe)
    val space=Space(name="غرفة",type="",length=1.0,width=1.0,takeoffs=listOf(item))
    assertNull(com.fayroz.sitecalculator.domain.MaterialReview.itemError(space,item))
    val rows=CostEngine.rows(Project(name="",sections=listOf(Section(name="",spaces=listOf(space)))))
    assertEquals(10.0,rows.single().labor,0.0)
 }
 @Test fun mortarCubicMeterAndCoverageModes(){
    val def=CalculatorLibrary.all.first{it.id=="plaster"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("_mortarMode" to "unit","thickness" to "20","waste" to "0","cementPrice" to "200","sandPrice" to "300")
    val answer=CalculatorLibrary.evaluate(def,raw)
    assertEquals(1.0,answer.outputs.first{it.label=="مونة منفذة"}.value,1e-9)
    assertEquals(50.0,answer.outputs.first{it.label=="مساحة التغطية عند السمك المدخل"}.value,1e-9)
    assertEquals(answer.outputs.first{it.label=="تكلفة الاستهلاك"}.value,answer.consumedCost,1e-9)
    assertTrue(answer.cost>answer.consumedCost)
    val coverage=raw+mapOf("_mortarMode" to "coverage","_volume" to "2","waste" to "10")
    assertEquals(100.0/1.1,CalculatorLibrary.mortarArea(coverage),1e-9)
    assertTrue(runCatching{CalculatorLibrary.evaluate(def,coverage+("_volume" to "0"))}.isFailure)
 }
 @Test fun shoppingContainsOnlyMaterialsAndUsesPackageUnits(){
    val def=CalculatorLibrary.all.first{it.id=="tile"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("area" to "10","tileW" to "50","tileH" to "50","pack" to "4","price" to "100","waste" to "0")
    val answer=CalculatorLibrary.evaluate(def,raw)
    val buy=CostEngine.calculatorPurchases(def,raw,answer)
    assertEquals(1,buy.size);assertEquals("كرتونة",buy.single().packageUnit)
    assertEquals(10,buy.single().packages);assertEquals(1000.0,buy.single().cost,1e-9)
    assertFalse(buy.any{it.unit=="جنيه"||it.material.contains("فائض")||it.material.contains("صافي")})
 }
 @Test fun netExecutionAndWasteStaySeparate(){
    val t=Takeoff(name="الأرضيات",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=100.0,waste=5.0)
    val q=QuantityEngine.calculateOne(Space(name="",type="",takeoffs=listOf(t)),t)
    assertEquals(100.0,q.repeatedFinal,0.0);assertEquals(105.0,q.repeatedFinal+q.waste,0.0)
 }
}


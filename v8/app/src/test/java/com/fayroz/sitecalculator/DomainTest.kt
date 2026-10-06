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

 @Test fun screedDoesNotInheritPlasterThickness(){
    val spec=CostEngine.defaultSpec("مونة تسوية الأرضيات",mapOf("plasterThickness" to "15"))!!
    assertEquals(50.0,spec.thicknessMm,0.0)
    assertEquals(50.0,CostEngine.defaultSpec("مونة تسوية الأرضيات",mapOf("plasterThickness" to "15","screedThickness" to "50"))!!.thicknessMm,0.0)
 }
 @Test fun groutSeparatesUsedMaterialFromWholePackage(){
    val answer=calc("grout","area" to "10","price" to "100")
    assertEquals(26.88,answer.consumedCost,1e-9);assertEquals(100.0,answer.cost,1e-9)
 }
 @Test fun mortarAndMasonryCementAreRoundedOnce(){
    val item=Takeoff(name="مباني",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=1.0,calculatorInputs=mapOf("cementPrice" to "100","sandPrice" to "200"))
    val rows=CostEngine.rows(Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(item)))))))
    val cement=CostEngine.purchase(rows).first{it.material.contains("أسمنت")}
    val mortar=CostEngine.Purchase("أسمنت (50 كجم)","كجم",20.0,1,100.0,100.0,"شيكارة",50.0)
    val total=CostEngine.consolidate(listOf(cement,mortar)).filter{it.material.contains("أسمنت")}
    assertEquals(1,total.size);assertEquals(kotlin.math.ceil((cement.amount+20)/50).toInt(),total.single().packages)
 }
 @Test fun differentStockNamesAreNeverMerged(){
    val a=CostEngine.Purchase("دهان أبيض","لتر",1.0,1,100.0,100.0,"عبوة",10.0)
    assertEquals(2,CostEngine.consolidate(listOf(a,a.copy(material="دهان بيج"))).size)
    assertEquals(1,CostEngine.consolidate(listOf(a,a)).single().packages)
 }
 @Test fun repricingPreservesGeometryAndReferenceSnapshots(){
    val spec=MaterialSpec(thicknessMm=22.0,cementPrice=100.0,sandPrice=200.0)
    val item=Takeoff(name="محارة الحوائط",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=10.0,material=spec,parts=listOf(WorkPart(length=2.0,width=5.0,material=spec)))
    val ref=SavedCalculation(toolId="plaster",title="مرجع",summary="",cost=77.0)
    val project=Project(name="",defaults=mapOf("cementPrice" to "300","sandPrice" to "400"),calculations=listOf(ref),sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(item))))))
    val updated=CostEngine.reprice(project);val result=updated.sections.single().spaces.single().takeoffs.single()
    assertEquals(300.0,result.material!!.cementPrice,0.0);assertEquals(22.0,result.material!!.thicknessMm,0.0)
    assertEquals(300.0,result.parts.single().material!!.cementPrice,0.0);assertEquals(item.directValue,result.directValue,0.0)
    assertEquals(ref,updated.calculations.single())
 }
 @Test fun onlyExplicitlyIncludedCalculationsAffectTotals(){
    val def=CalculatorLibrary.all.first{it.id=="paint"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("area" to "10","price" to "100")
    val ref=SavedCalculation(toolId="paint",title="دهان",summary="",inputs=raw)
    val p=Project(name="",calculations=listOf(ref))
    assertTrue(CostEngine.rows(p).isEmpty());assertTrue(QuantityEngine.summarize(p).isEmpty())
    val included=p.copy(calculations=listOf(ref.copy(inputs=raw+("_includeInProject" to "true"))))
    assertEquals(10.0,CostEngine.rows(included).single().quantity,0.0)
    assertEquals(10.0,QuantityEngine.summarize(included).single().quantity,0.0)
    assertEquals(1,CostEngine.purchase(CostEngine.rows(included)).single().packages)
 }
 @Test fun savedConcreteDoesNotReinterpretVolumeAsElementCount(){
    val def=CalculatorLibrary.all.first{it.id=="footing"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("length" to "2","width" to "3","height" to "1","count" to "2","waste" to "0","price" to "100","_includeInProject" to "true")
    val p=Project(name="",calculations=listOf(SavedCalculation(toolId=def.id,title="قواعد",summary="",inputs=raw)))
    assertEquals(12.0,CostEngine.rows(p).single().quantity,0.0)
    assertEquals("م³",CostEngine.rows(p).single().unit)
    assertEquals(12.0,CostEngine.purchase(CostEngine.rows(p)).single().amount,0.0)
    assertEquals(1200.0,CostEngine.rows(p).single().materialCost,0.0)
 }
 @Test fun sellingPriceShowsExplicitCompounding(){
    val row=CostEngine.Row("","","",null,"","","",10.0,"م²",materialCost=1000.0)
    val result=CostEngine.selling(Project(name="",defaults=mapOf("overheadPercent" to "10","profitPercent" to "20","taxPercent" to "14")),listOf(row))
    assertEquals(100.0,result.overhead,0.0);assertEquals(220.0,result.profit,0.0);assertEquals(1504.8,result.total,1e-9)
 }
 @Test fun stockIdentitySurvivesBackupCodec(){
    val spec=MaterialSpec(cementName="أسمنت مقاوم",sandName="رمل مغسول")
    assertEquals(spec,com.fayroz.sitecalculator.data.V8Codec.materialFrom(com.fayroz.sitecalculator.data.V8Codec.materialObj(spec)))
 }
 @Test fun openingDeductionCanFollowProjectMeasurementRule(){
    val item=Takeoff(name="محارة",unit=UnitType.AREA,kind=CalcKind.WALLS)
    val room=Space(name="",type="",length=4.0,width=3.0,height=3.0,openings=listOf(Opening(width=1.0,height=2.0,wallId="rect_1")))
    assertEquals(40.0,QuantityEngine.calculateOne(room,item).repeatedFinal,0.0)
    assertEquals(42.0,QuantityEngine.calculateOne(room,item.copy(calculatorInputs=mapOf("_deductOpenings" to "false"))).repeatedFinal,0.0)
 }

 @Test fun globalCementPriceUsesFiftyKilogramBasisWhenRepricing(){
    val item=Takeoff(name="محارة",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=10.0,material=MaterialSpec(bagKg=25.0,cementPrice=80.0))
    val def=CalculatorLibrary.all.first{it.id=="plaster"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("area" to "10","bag" to "25","_includeInProject" to "true")
    val project=Project(name="",defaults=mapOf("cementPrice" to "300"),sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(item))))),calculations=listOf(SavedCalculation(toolId="plaster",title="",summary="",inputs=raw)))
    val updated=CostEngine.reprice(project)
    assertEquals(150.0,updated.sections.single().spaces.single().takeoffs.single().material!!.cementPrice,0.0)
    assertEquals(150.0,updated.calculations.single().inputs["cementPrice"]!!.toDouble(),0.0)
 }

 @Test fun siteMixBagsArePerSandNotPerWetMortar(){
    val a=calc("plaster","_bagsPerSand" to "5","_mortarMode" to "sand","_sandAvailable" to "1","thickness" to "20","waste" to "0")
    assertEquals(1.0,a.outputs.first{it.label=="رمل"}.value,1e-9)
    assertEquals(250.0,a.outputs.first{it.label=="أسمنت فعلي"}.value,1e-9)
    assertEquals(5.0,a.outputs.first{it.label=="شراء أسمنت"}.value,0.0)
    assertEquals((1+250.0/1440)/1.33,a.outputs.first{it.label=="مونة منفذة"}.value,1e-9)
 }
 @Test fun migratedLegacyMixPreservesAllQuantities(){
    val def=CalculatorLibrary.all.first{it.id=="plaster"};val raw=def.fields.associate{it.key to it.default}+mapOf("area" to "100")
    val old=CalculatorLibrary.evaluate(def,raw);val next=CalculatorLibrary.evaluate(def,raw+(MortarMix.key to MortarMix.bags(raw).toString()))
    old.outputs.zip(next.outputs).forEach{(a,b)->assertEquals(a.value,b.value,1e-9)}
 }
 @Test fun stockCalculationRespectsBothMaterials(){
    val a=calc("plaster","_bagsPerSand" to "5","_mortarMode" to "stock","_sandAvailable" to "2","_bagsAvailable" to "5","thickness" to "20","waste" to "5")
    assertEquals(1.0,a.outputs.first{it.label=="رمل"}.value,1e-9)
    assertEquals(250.0,a.outputs.first{it.label=="أسمنت فعلي"}.value,1e-9)
    val b=calc("plaster","_bagsPerSand" to "5","_mortarMode" to "stock","_sandAvailable" to "0.5","_bagsAvailable" to "5","thickness" to "20","waste" to "5")
    assertEquals(.5,b.outputs.first{it.label=="رمل"}.value,1e-9)
    assertEquals(125.0,b.outputs.first{it.label=="أسمنت فعلي"}.value,1e-9)
 }
 @Test fun bagsMixWorksForMasonryAndReverseCalculator(){
    val a=calc("masonry","area" to "100","_bagsPerSand" to "5")
    assertEquals(5.0,a.outputs.first{it.label=="أسمنت فعلي"}.value/50/a.outputs.first{it.label=="رمل"}.value,1e-9)
    val b=calc("reverse_mortar","bags" to "5","_bagsPerSand" to "5")
    assertEquals(1.0,b.outputs.first{it.label=="رمل مطلوب"}.value,1e-9)
 }
 @Test fun projectSpecUsesBagsAndMatchesCalculator(){
    val spec=CostEngine.defaultSpec("محارة الحوائط",mapOf("plasterBagsPerSand" to "5","plasterThickness" to "20"))!!
    val quantities=CostEngine.measure(100.0,spec)
    val a=calc("plaster","area" to "100","thickness" to "20","_bagsPerSand" to "5")
    assertEquals(quantities.first,a.outputs.first{it.label=="أسمنت فعلي"}.value,1e-9)
    assertEquals(quantities.second,a.outputs.first{it.label=="رمل"}.value,1e-9)
    val small=MortarMix.withBags(spec.copy(bagKg=25.0),5.0)
    assertEquals(5.0,MortarMix.bags(small),1e-9)
 }
 @Test fun invalidSiteMixAndNegativeDimensionsCannotProduceAResult(){
    listOf("0","-1","abc").forEach{bags->assertTrue(runCatching{calc("plaster","area" to "100","_bagsPerSand" to bags)}.isFailure)}
    assertTrue(runCatching{calc("plaster","area" to "4","_areaMethod" to "dimensions","_areaLength" to "-2","_areaWidth" to "-2")}.isFailure)
 }

 @Test fun savedWiresUseTotalNetLength(){
    val def=CalculatorLibrary.all.first{it.id=="wires"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("length" to "10","conductors" to "3","price" to "100","_includeInProject" to "true")
    val c=SavedCalculation(toolId="wires",title="أسلاك",summary="",sourceQuantity=10.0,unit="م",inputs=raw)
    val row=CostEngine.rows(Project(name="",calculations=listOf(c))).single()
    assertEquals(30.0,row.quantity,1e-9);assertEquals("م",row.unit)
 }
 @Test fun savedMeshUsesWeightAndReverseMortarUsesArea(){
    val mesh=CalculatorLibrary.all.first{it.id=="mesh"}
    val raw=mesh.fields.associate{it.key to it.default}+mapOf("length" to "4","width" to "3","spacing" to "20","diameter" to "12")
    val answer=CalculatorLibrary.evaluate(mesh,raw)
    val measured=CalculatorLibrary.workQuantity(mesh,raw,answer)
    assertEquals("كجم",measured.unit);assertEquals(answer.outputs.last().value/1.05,measured.value,1e-9)
    val reverse=CalculatorLibrary.all.first{it.id=="reverse_mortar"}
    val rr=reverse.fields.associate{it.key to it.default}+("bags" to "10")
    assertEquals("م²",CalculatorLibrary.workQuantity(reverse,rr,CalculatorLibrary.evaluate(reverse,rr)).unit)
 }
 @Test fun invalidIncludedCalculationRemainsVisible(){
    val c=SavedCalculation(toolId="wires",title="حساب يحتاج إصلاح",summary="",inputs=mapOf("length" to "invalid","_includeInProject" to "true"))
    val row=CostEngine.rows(Project(name="",calculations=listOf(c))).single()
    assertNotNull(row.issue);assertTrue(MaterialReview.missing(row).isNotEmpty());assertTrue(CostEngine.purchase(listOf(row)).isEmpty())
 }
 @Test fun zeroCementAndInvalidMaterialAreRejected(){
    assertNotNull(MaterialReview.specError(MortarMix.withBags(MaterialSpec(),0.0)))
    val t=Takeoff(name="محارة الحوائط",unit=UnitType.AREA,kind=CalcKind.DIRECT,directValue=10.0,material=MaterialSpec(cementPrice=-1.0))
    val row=CostEngine.rows(Project(name="",sections=listOf(Section(name="",spaces=listOf(Space(name="",type="",takeoffs=listOf(t))))))).single()
    assertNotNull(row.issue);assertEquals(0.0,row.total,1e-9)
 }
 @Test fun fittingsHaveNoHiddenExtraPiece(){assertEquals(3.0,calc("fittings","count" to "3").outputs.first().value,1e-9)}
 @Test fun interlockCostIsExplicitlyPartial(){
    val def=CalculatorLibrary.all.first{it.id=="interlock"}
    assertTrue(MaterialReview.missing(def,def.fields.associate{it.key to it.default}+("price" to "100")).any{it.contains("فرشة")})
 }
 @Test fun basicFieldsIncludePurchaseSizesAndBlockDimensions(){
    val tile=CalculatorLibrary.all.first{it.id=="tile"}
    assertTrue(MortarMix.basic(tile).map{it.key}.containsAll(listOf("tileW","tileH","pack")))
    val block=CalculatorLibrary.all.first{it.id=="blocks"}
    assertTrue(block.fields.first{it.key=="brickH"}.default.isBlank())
 }
 @Test fun unspecifiedStocksStaySeparate(){
    val def=CalculatorLibrary.all.first{it.id=="pipes"}
    val raw=def.fields.associate{it.key to it.default}+mapOf("length" to "2","price" to "100","_includeInProject" to "true")
    val cs=listOf("a","b").map{SavedCalculation(id=it,toolId="pipes",title=it,summary="",inputs=raw)}
    val purchases=CostEngine.purchase(CostEngine.rows(Project(name="",calculations=cs)))
    assertEquals(2,purchases.size);assertEquals(200.0,purchases.sumOf{it.cost},1e-9)
 }

 @Test fun centralPriceMatchesPackagingAndName(){
    val d=CalculatorLibrary.all.first{it.id=="tile"}
    val r=CalculatorLibrary.baseDefaults(d,emptyMap())+mapOf("_materialName" to "سيراميك بيج","price" to "200")
    val book=PriceBook.record(d,r)
    assertEquals("200",PriceBook.apply(d,r+("price" to "0"),book)["price"])
    assertEquals("0",PriceBook.apply(d,r+mapOf("tileW" to "80"),book)["price"])
    assertEquals("0",PriceBook.apply(d,r+mapOf("pack" to "8"),book)["price"])
    assertEquals("0",PriceBook.apply(d,r+mapOf("_materialName" to "سيراميك آخر"),book)["price"])
 }
 @Test fun centralPriceDoesNotDependOnJobAreaOrWaste(){
    val d=CalculatorLibrary.all.first{it.id=="tile"};val r=CalculatorLibrary.baseDefaults(d,emptyMap())+("price" to "200")
    val book=PriceBook.record(d,r)
    assertEquals("200",PriceBook.apply(d,r+mapOf("area" to "500","waste" to "12"),book)["price"])
 }
 @Test fun centralCementPriceScalesWithBagWeightAndHonorsOverride(){
    val d=CalculatorLibrary.all.first{it.id=="plaster"};val raw=CalculatorLibrary.baseDefaults(d,emptyMap())+mapOf("bag" to "25")
    assertEquals(100.0,PriceBook.apply(d,raw,mapOf("cementPrice" to "200"))["cementPrice"]!!.toDouble(),0.0)
    assertEquals("80",PriceBook.apply(d,raw+mapOf("cementPrice" to "80","_priceOverride.cementPrice" to "true"),mapOf("cementPrice" to "200"))["cementPrice"])
 }
 @Test fun explicitRepricingUsesMatchingSpecificationOnly(){
    val d=CalculatorLibrary.all.first{it.id=="tile"};val raw=CalculatorLibrary.baseDefaults(d,emptyMap())+mapOf("area" to "10","price" to "200","_includeInProject" to "true")
    val book=PriceBook.record(d,raw+("price" to "300"))
    val p=Project(name="",defaults=book,calculations=listOf(SavedCalculation(toolId="tile",title="",summary="",inputs=raw)))
    val next=CostEngine.reprice(p).calculations.single();assertEquals("300",next.inputs["price"]);assertEquals("10",next.inputs["area"])
 }
}


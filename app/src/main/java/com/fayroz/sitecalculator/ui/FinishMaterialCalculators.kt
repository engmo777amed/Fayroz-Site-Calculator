package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.ProjectRepository
import kotlin.math.ceil

@Composable
fun TilePurchaseScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit,onOpenTool:(String,Double?)->Unit){
    val toolId="tile_purchase"
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var tileW by remember{mutableStateOf(repository.getToolValue("$toolId.tileW","60"))}
    var tileH by remember{mutableStateOf(repository.getToolValue("$toolId.tileH","60"))}
    var tileUnit by remember{mutableStateOf(repository.getToolValue("$toolId.tileUnit","سم"))}
    var piecesBox by remember{mutableStateOf(repository.getToolValue("$toolId.piecesBox",""))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","7"))}
    var sizePreset by remember{mutableStateOf(repository.getToolValue("$toolId.sizePreset","60 × 60 سم"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    fun applySize(label:String){
        sizePreset=label
        when(label){
            "60 × 60 سم"->{tileW="60";tileH="60";tileUnit="سم"}
            "60 × 120 سم"->{tileW="60";tileH="120";tileUnit="سم"}
            "30 × 60 سم"->{tileW="30";tileH="60";tileUnit="سم"}
        }
        set("sizePreset",sizePreset);set("tileW",tileW);set("tileH",tileH);set("tileUnit",tileUnit)
    }

    val a=n(area)
    val purchaseArea=a*(1+n(waste)/100.0)
    val pieceArea=lengthToMeters(tileW,tileUnit)*lengthToMeters(tileH,tileUnit)
    val pieces=if(pieceArea>0)ceil(purchaseArea/pieceArea).toInt() else 0
    val boxCount=n(piecesBox).toInt().coerceAtLeast(0)
    val boxes=if(boxCount>0)ceil(pieces.toDouble()/boxCount).toInt() else 0

    ToolPage("البلاط والكراتين","من المساحة لعدد البلاطات والكراتين",repository,toolId,onBack){
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    area=area;waste="7";piecesBox="";applySize("60 × 60 سم")
                    set("waste",waste);set("piecesBox",piecesBox)
                }
            }
            ChoiceFieldX("مقاس شائع",sizePreset,listOf("60 × 60 سم","60 × 120 سم","30 × 60 سم","مقاس تاني"),{label->
                if(label=="مقاس تاني"){sizePreset=label;set("sizePreset",label)}else applySize(label)
            },"اختصار للمقاس فقط. عدد القطع في الكرتونة لازم يتاخد من العبوة.")
            NumberFieldX("صافي المساحة",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberWithUnitX("عرض البلاطة",tileW,{tileW=it;sizePreset="مقاس تاني";set("tileW",it);set("sizePreset",sizePreset)},tileUnit,{tileUnit=it;set("tileUnit",it)},modifier=Modifier.weight(1f))
                NumberWithUnitX("طول البلاطة",tileH,{tileH=it;sizePreset="مقاس تاني";set("tileH",it);set("sizePreset",sizePreset)},tileUnit,{tileUnit=it;set("tileUnit",it)},modifier=Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("قطع في الكرتونة",piecesBox,{piecesBox=it;set("piecesBox",it)},"قطعة","سيبها فاضية لو مش عارفها، وخد الرقم من الكرتونة.",Modifier.weight(1f))
                NumberFieldX("هالك وقص",waste,{waste=it;set("waste",it)},"%",
                    "قيمة بداية 7%، وعدلها حسب المقاس وطريقة التركيب.",Modifier.weight(1f))
            }
            if(n(waste)>20)LogicWarning("الهالك كبير للبلاط. راجع المقاس وطريقة القص.")
        }
        if(a>0 && pieceArea>0){
            ToolResultCard(
                rows=listOf(
                    "عدد البلاطات" to "$pieces بلاطة",
                    "مساحة الشراء" to "${fmt(purchaseArea)} م²",
                    "عدد الكراتين" to if(boxCount>0)"$boxes كرتونة" else "اكتب قطع/كرتونة",
                    "مساحة البلاطة" to "${fmt(pieceArea)} م²"
                ),
                explanation="مساحة الشراء = الصافي + الهالك. عدد البلاطات = مساحة الشراء ÷ مساحة البلاطة، والعدد بيتقرب لأعلى.",
                copyText="بلاط ${fmt(a)} م² — شراء ${fmt(purchaseArea)} م² — $pieces بلاطة"+if(boxCount>0)" — $boxes كرتونة" else "",
                links=listOf("احسب لاصق لنفس المساحة" to {onOpenTool("tile_adhesive",a)})
            )
        }
    }
}

@Composable
fun TileAdhesiveScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit,onOpenTool:(String,Double?)->Unit){
    val toolId="tile_adhesive"
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var rate by remember{mutableStateOf(repository.getToolValue("$toolId.rate","5"))}
    var bagWeight by remember{mutableStateOf(repository.getToolValue("$toolId.bagWeight","20"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","5"))}
    var preset by remember{mutableStateOf(repository.getToolValue("$toolId.preset","بداية 5 كجم/م²"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    fun applyPreset(label:String){
        preset=label
        rate=when(label){"خفيف 4 كجم/م²"->"4";"ثقيل 6 كجم/م²"->"6";else->"5"}
        set("preset",preset);set("rate",rate)
    }
    val a=n(area)
    val kg=a*n(rate)*(1+n(waste)/100.0)
    val bags=if(n(bagWeight)>0)ceil(kg/n(bagWeight)).toInt() else 0

    ToolPage("لاصق السيراميك","كمية اللاصق حسب معدل المنتج",repository,toolId,onBack){
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{applyPreset("بداية 5 كجم/م²");bagWeight="20";waste="5";set("bagWeight",bagWeight);set("waste",waste)}
            }
            ChoiceFieldX("معدل بداية",preset,listOf("خفيف 4 كجم/م²","بداية 5 كجم/م²","ثقيل 6 كجم/م²"),{applyPreset(it)},
                "دي قيم بداية فقط. الاستهلاك الحقيقي بيتغير حسب المنتج والمشط ومقاس البلاطة وحالة السطح.")
            PresetNote("راجع معدل الاستهلاك المكتوب على عبوة اللاصق لو متاح، وهو المرجع الأفضل.")
            NumberFieldX("المساحة",area,{area=it;set("area",it)},"م²")
            NumberFieldX("استهلاك اللاصق",rate,{rate=it;preset="معدل معدل يدوي";set("rate",it);set("preset",preset)},"كجم/م²",
                "لو معاك نشرة المنتج اكتب الرقم منها.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%",modifier=Modifier.weight(1f))
            }
            if(n(waste)>20)LogicWarning("الهالك مرتفع. راجع النسبة.")
        }
        if(a>0 && n(rate)>0){
            ToolResultCard(
                rows=listOf("عدد الشكاير" to "$bags شيكارة","اللاصق" to "${fmt(kg)} كجم"),
                explanation="اللاصق = المساحة × معدل الاستهلاك × (1 + الهالك). عدد الشكاير اتقرب لأعلى.",
                copyText="لاصق سيراميك — مساحة ${fmt(a)} م² — ${fmt(kg)} كجم ≈ $bags شيكارة",
                links=listOf("احسب البلاط والكراتين لنفس المساحة" to {onOpenTool("tile_purchase",a)})
            )
        }
    }
}

@Composable
fun PaintMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="paint_materials"
    var detailed by remember{mutableStateOf(repository.getToolValue("$toolId.view","quick")=="detailed")}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var paintCoverage by remember{mutableStateOf(repository.getToolValue("$toolId.paintCoverage","10"))}
    var paintCoats by remember{mutableStateOf(repository.getToolValue("$toolId.paintCoats","2"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","5"))}
    var primerCoverage by remember{mutableStateOf(repository.getToolValue("$toolId.primerCoverage","10"))}
    var primerCoats by remember{mutableStateOf(repository.getToolValue("$toolId.primerCoats","1"))}
    var puttyRate by remember{mutableStateOf(repository.getToolValue("$toolId.puttyRate","1.2"))}
    var puttyCoats by remember{mutableStateOf(repository.getToolValue("$toolId.puttyCoats","2"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val factor=1+n(waste)/100.0
    val paintLiters=if(n(paintCoverage)>0)a*n(paintCoats)*factor/n(paintCoverage) else 0.0
    val primerLiters=if(n(primerCoverage)>0)a*n(primerCoats)*factor/n(primerCoverage) else 0.0
    val puttyKg=a*n(puttyRate)*n(puttyCoats)*factor

    ToolPage("دهان ومعجون","كمية تقريبية حسب معدل تغطية المنتج",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it;set("view",if(it)"detailed" else "quick")}
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    paintCoverage="10";paintCoats="2";waste="5";primerCoverage="10";primerCoats="1";puttyRate="1.2";puttyCoats="2"
                    set("paintCoverage",paintCoverage);set("paintCoats",paintCoats);set("waste",waste);set("primerCoverage",primerCoverage);set("primerCoats",primerCoats);set("puttyRate",puttyRate);set("puttyCoats",puttyCoats)
                }
            }
            PresetNote("القيم دي بداية للحساب فقط. معدل التغطية واستهلاك المعجون بيتاخدوا من المنتج وحالة السطح.")
            NumberFieldX("مساحة الدهان",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberFieldX("تغطية الدهان",paintCoverage,{paintCoverage=it;set("paintCoverage",it)},"م²/لتر",
                    "المساحة اللي يغطيها لتر واحد في وجه واحد.",Modifier.weight(1f))
                NumberFieldX("عدد الأوجه",paintCoats,{paintCoats=it;set("paintCoats",it)},"وجه",modifier=Modifier.weight(1f))
            }
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
            if(n(waste)>20)LogicWarning("الهالك مرتفع للدهانات. راجع النسبة.")
            if(detailed){
                HorizontalDivider()
                Text("البرايمر والمعجون",style=MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("تغطية البرايمر",primerCoverage,{primerCoverage=it;set("primerCoverage",it)},"م²/لتر",
                        "راجع معدل المنتج.",Modifier.weight(1f))
                    NumberFieldX("أوجه البرايمر",primerCoats,{primerCoats=it;set("primerCoats",it)},"وجه",modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("معدل المعجون",puttyRate,{puttyRate=it;set("puttyRate",it)},"كجم/م²/وجه","قيمة بداية، راجع المنتج والتنفيذ.",Modifier.weight(1f))
                    NumberFieldX("أوجه المعجون",puttyCoats,{puttyCoats=it;set("puttyCoats",it)},"وجه",modifier=Modifier.weight(1f))
                }
            }
        }
        if(a>0){
            val rows=buildList{
                add("الدهان" to "${fmt(paintLiters)} لتر")
                if(detailed){
                    add("البرايمر" to "${fmt(primerLiters)} لتر")
                    add("المعجون" to "${fmt(puttyKg)} كجم")
                }
            }
            ToolResultCard(
                rows=rows,
                explanation="الدهان = المساحة × عدد الأوجه ÷ معدل التغطية، وبعدها الهالك. نفس الفكرة للبرايمر، والمعجون حسب معدل كجم/م²/وجه.",
                copyText="دهانات ${fmt(a)} م² — دهان ${fmt(paintLiters)} لتر"+if(detailed)" — برايمر ${fmt(primerLiters)} لتر — معجون ${fmt(puttyKg)} كجم" else ""
            )
        }
    }
}

@Composable
fun WaterproofMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="waterproof_materials"
    var type by remember{mutableStateOf(repository.getToolValue("$toolId.type","عزل دهان / أسمنتي"))}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var rate by remember{mutableStateOf(repository.getToolValue("$toolId.rate","1.5"))}
    var coats by remember{mutableStateOf(repository.getToolValue("$toolId.coats","2"))}
    var packWeight by remember{mutableStateOf(repository.getToolValue("$toolId.packWeight","20"))}
    var rollLength by remember{mutableStateOf(repository.getToolValue("$toolId.rollLength","10"))}
    var rollWidth by remember{mutableStateOf(repository.getToolValue("$toolId.rollWidth","1"))}
    var rollUnit by remember{mutableStateOf(repository.getToolValue("$toolId.rollUnit","م"))}
    var overlap by remember{mutableStateOf(repository.getToolValue("$toolId.overlap","10"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","5"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val coatingKg=a*n(rate)*n(coats)*(1+n(waste)/100.0)
    val packs=if(n(packWeight)>0)ceil(coatingKg/n(packWeight)).toInt() else 0
    val nominalRollArea=lengthToMeters(rollLength,rollUnit)*lengthToMeters(rollWidth,rollUnit)
    val effectiveRollArea=nominalRollArea*(1-n(overlap)/100.0)
    val rollAreaNeed=a*(1+n(waste)/100.0)
    val rolls=if(effectiveRollArea>0)ceil(rollAreaNeed/effectiveRollArea).toInt() else 0

    ToolPage("خامات العزل","دهان/أسمنتي أو لفائف",repository,toolId,onBack){
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    rate="1.5";coats="2";packWeight="20";rollLength="10";rollWidth="1";rollUnit="م";overlap="10";waste="5"
                    set("rate",rate);set("coats",coats);set("packWeight",packWeight);set("rollLength",rollLength);set("rollWidth",rollWidth);set("rollUnit",rollUnit);set("overlap",overlap);set("waste",waste)
                }
            }
            ChoiceFieldX("نوع العزل",type,listOf("عزل دهان / أسمنتي","لفائف عزل"),{type=it;set("type",it)})
            NumberFieldX("المساحة",area,{area=it;set("area",it)},"م²")
            if(type=="عزل دهان / أسمنتي"){
                PresetNote("1.5 كجم/م²/وجه و2 وجه قيمة بداية فقط. نشرة المنتج هي المرجع.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("استهلاك الوجه",rate,{rate=it;set("rate",it)},"كجم/م²","راجع نشرة المنتج.",Modifier.weight(1f))
                    NumberFieldX("عدد الأوجه",coats,{coats=it;set("coats",it)},"وجه",modifier=Modifier.weight(1f))
                }
                NumberFieldX("وزن العبوة",packWeight,{packWeight=it;set("packWeight",it)},"كجم")
            }else{
                PresetNote("مقاس الرول والتراكب بيتغيروا حسب المنتج. عدلهم من بيانات الرول اللي معاك.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberWithUnitX("طول الرول",rollLength,{rollLength=it;set("rollLength",it)},rollUnit,{rollUnit=it;set("rollUnit",it)},modifier=Modifier.weight(1f))
                    NumberWithUnitX("عرض الرول",rollWidth,{rollWidth=it;set("rollWidth",it)},rollUnit,{rollUnit=it;set("rollUnit",it)},modifier=Modifier.weight(1f))
                }
                NumberFieldX("تراكب اللفائف",overlap,{overlap=it;set("overlap",it)},"%","الجزء اللي بيركب فوق الرول اللي قبله.")
            }
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
            if(n(waste)>20)LogicWarning("الهالك مرتفع. راجع النسبة.")
        }
        if(a>0){
            if(type=="عزل دهان / أسمنتي"){
                ToolResultCard(
                    rows=listOf("عدد العبوات" to "$packs عبوة","إجمالي الخامة" to "${fmt(coatingKg)} كجم"),
                    explanation="الخامة = المساحة × استهلاك الوجه × عدد الأوجه × الهالك.",
                    copyText="عزل ${fmt(a)} م² — ${fmt(coatingKg)} كجم ≈ $packs عبوة"
                )
            }else{
                ToolResultCard(
                    rows=listOf("عدد الرولات" to "$rolls رول","مساحة الرول الفعلية" to "${fmt(effectiveRollArea)} م²"),
                    explanation="مساحة الرول = الطول × العرض، وبعدها اتخصم التراكب واتحسب الهالك.",
                    copyText="لفائف عزل ${fmt(a)} م² — $rolls رول"
                )
            }
        }
    }
}

@Composable
fun GypsumMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="gypsum_materials"
    var detailed by remember{mutableStateOf(repository.getToolValue("$toolId.view","quick")=="detailed")}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var boardW by remember{mutableStateOf(repository.getToolValue("$toolId.boardW","1.20"))}
    var boardH by remember{mutableStateOf(repository.getToolValue("$toolId.boardH","2.40"))}
    var boardUnit by remember{mutableStateOf(repository.getToolValue("$toolId.boardUnit","م"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","10"))}
    var mainRate by remember{mutableStateOf(repository.getToolValue("$toolId.mainRate","0.9"))}
    var furringRate by remember{mutableStateOf(repository.getToolValue("$toolId.furringRate","2.8"))}
    var screwsRate by remember{mutableStateOf(repository.getToolValue("$toolId.screwsRate","20"))}
    var compoundRate by remember{mutableStateOf(repository.getToolValue("$toolId.compoundRate","0.35"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val purchaseArea=a*(1+n(waste)/100.0)
    val boardArea=lengthToMeters(boardW,boardUnit)*lengthToMeters(boardH,boardUnit)
    val boards=if(boardArea>0)ceil(purchaseArea/boardArea).toInt() else 0
    val main=purchaseArea*n(mainRate)
    val furring=purchaseArea*n(furringRate)
    val screws=ceil(purchaseArea*n(screwsRate)).toInt()
    val compound=purchaseArea*n(compoundRate)

    ToolPage("خامات الجبس بورد","ألواح وباقي الخامات بمعدلات واضحة",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it;set("view",if(it)"detailed" else "quick")}
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    boardW="1.20";boardH="2.40";boardUnit="م";waste="10";mainRate="0.9";furringRate="2.8";screwsRate="20";compoundRate="0.35"
                    set("boardW",boardW);set("boardH",boardH);set("boardUnit",boardUnit);set("waste",waste);set("mainRate",mainRate);set("furringRate",furringRate);set("screwsRate",screwsRate);set("compoundRate",compoundRate)
                }
            }
            PresetNote("مقاس اللوح 1.20 × 2.40 م بداية شائعة. معدلات القطاعات والمسامير بتختلف حسب نظام التركيب والشركة.")
            NumberFieldX("مساحة السقف / الحائط",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberWithUnitX("عرض اللوح",boardW,{boardW=it;set("boardW",it)},boardUnit,{boardUnit=it;set("boardUnit",it)},modifier=Modifier.weight(1f))
                NumberWithUnitX("طول اللوح",boardH,{boardH=it;set("boardH",it)},boardUnit,{boardUnit=it;set("boardUnit",it)},modifier=Modifier.weight(1f))
            }
            NumberFieldX("هالك وقص",waste,{waste=it;set("waste",it)},"%")
            if(n(waste)>25)LogicWarning("الهالك مرتفع للجبس بورد. راجع النسبة.")
            if(detailed){
                HorizontalDivider()
                Text("معدلات التركيب",style=MaterialTheme.typography.labelLarge)
                NumberFieldX("القطاع الرئيسي",mainRate,{mainRate=it;set("mainRate",it)},"م ط/م²","اكتب معدل النظام المستخدم.")
                NumberFieldX("القطاع الثانوي",furringRate,{furringRate=it;set("furringRate",it)},"م ط/م²","اكتب معدل النظام المستخدم.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("مسامير",screwsRate,{screwsRate=it;set("screwsRate",it)},"عدد/م²",modifier=Modifier.weight(1f))
                    NumberFieldX("معجون فواصل",compoundRate,{compoundRate=it;set("compoundRate",it)},"كجم/م²",modifier=Modifier.weight(1f))
                }
            }
        }
        if(a>0 && boardArea>0){
            val rows=buildList{
                add("عدد الألواح" to "$boards لوح")
                add("مساحة الشراء" to "${fmt(purchaseArea)} م²")
                if(detailed){
                    add("قطاع رئيسي" to "${fmt(main)} م ط")
                    add("قطاع ثانوي" to "${fmt(furring)} م ط")
                    add("مسامير" to "$screws مسمار تقريبًا")
                    add("معجون فواصل" to "${fmt(compound)} كجم")
                }
            }
            ToolResultCard(
                rows=rows,
                explanation="عدد الألواح = مساحة التنفيذ بعد الهالك ÷ مساحة اللوح. باقي الخامات في الوضع التفصيلي = المساحة × معدل النظام اللي أنت مدخله.",
                copyText="جبس بورد ${fmt(a)} م² — $boards لوح"+if(detailed)" — رئيسي ${fmt(main)} م ط — ثانوي ${fmt(furring)} م ط" else ""
            )
        }
    }
}

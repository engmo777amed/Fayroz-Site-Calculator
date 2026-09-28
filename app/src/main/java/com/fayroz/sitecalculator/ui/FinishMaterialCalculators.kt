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
    var piecesBox by remember{mutableStateOf(repository.getToolValue("$toolId.piecesBox","4"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","7"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}

    val a=n(area)
    val purchaseArea=a*(1+n(waste)/100.0)
    val pieceArea=(n(tileW)/100.0)*(n(tileH)/100.0)
    val pieces=if(pieceArea>0)ceil(purchaseArea/pieceArea).toInt() else 0
    val boxCount=n(piecesBox).toInt().coerceAtLeast(0)
    val boxes=if(boxCount>0)ceil(pieces.toDouble()/boxCount).toInt() else 0

    ToolPage("البلاط والكراتين","من المساحة لعدد البلاطات والكراتين",repository,toolId,onBack){
        CardBox{
            NumberFieldX("صافي المساحة",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("عرض البلاطة",tileW,{tileW=it;set("tileW",it)},"سم",modifier=Modifier.weight(1f))
                NumberFieldX("طول البلاطة",tileH,{tileH=it;set("tileH",it)},"سم",modifier=Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("قطع في الكرتونة",piecesBox,{piecesBox=it;set("piecesBox",it)},"قطعة","اكتب عدد القطع المكتوب على الكرتونة.",Modifier.weight(1f))
                NumberFieldX("هالك وقص",waste,{waste=it;set("waste",it)},"%",modifier=Modifier.weight(1f))
            }
        }
        if(a>0 && pieceArea>0){
            ToolResultCard(
                rows=listOf(
                    "عدد الكراتين" to if(boxCount>0)"$boxes كرتونة" else "اكتب عدد القطع/كرتونة",
                    "عدد البلاطات" to "$pieces بلاطة",
                    "مساحة الشراء" to "${fmt(purchaseArea)} م²",
                    "مساحة البلاطة" to "${fmt(pieceArea)} م²"
                ),
                explanation="مساحة الشراء = ${fmt(a)} × (1 + ${fmt(n(waste))}%) = ${fmt(purchaseArea)} م².\nمساحة البلاطة = ${fmt(pieceArea)} م²، وعدد البلاطات = مساحة الشراء ÷ مساحة البلاطة ثم تقريب العدد لأعلى.",
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
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val kg=a*n(rate)*(1+n(waste)/100.0)
    val bags=if(n(bagWeight)>0)ceil(kg/n(bagWeight)).toInt() else 0

    ToolPage("لاصق السيراميك","كمية اللاصق حسب معدل المنتج",repository,toolId,onBack){
        CardBox{
            NumberFieldX("المساحة",area,{area=it;set("area",it)},"م²")
            NumberFieldX(
                "استهلاك اللاصق",rate,{rate=it;set("rate",it)},"كجم/م²",
                "خد الرقم من نشرة المنتج أو حسب مقاس المشط والبلاطة. مفيش رقم ثابت مناسب لكل الحالات."
            )
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%",modifier=Modifier.weight(1f))
            }
        }
        if(a>0 && n(rate)>0){
            ToolResultCard(
                rows=listOf(
                    "عدد الشكاير" to "$bags شيكارة",
                    "اللاصق" to "${fmt(kg)} كجم"
                ),
                explanation="اللاصق = المساحة × معدل الاستهلاك × الهالك = ${fmt(a)} × ${fmt(n(rate))} × (1 + ${fmt(n(waste))}%).\nعدد الشكاير اتقرب لأعلى حسب وزن الشيكارة ${fmt(n(bagWeight))} كجم.",
                copyText="لاصق سيراميك — مساحة ${fmt(a)} م² — ${fmt(kg)} كجم ≈ $bags شيكارة",
                links=listOf("احسب البلاط والكراتين لنفس المساحة" to {onOpenTool("tile_purchase",a)})
            )
        }
    }
}

@Composable
fun PaintMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="paint_materials"
    var detailed by remember{mutableStateOf(false)}
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
        QuickDetailedSwitch(detailed){detailed=it}
        CardBox{
            NumberFieldX("مساحة الدهان",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX(
                    "تغطية الدهان",paintCoverage,{paintCoverage=it;set("paintCoverage",it)},"م²/لتر",
                    "اكتب معدل التغطية المكتوب على المنتج.",
                    Modifier.weight(1f)
                )
                NumberFieldX("عدد الأوجه",paintCoats,{paintCoats=it;set("paintCoats",it)},"وجه",modifier=Modifier.weight(1f))
            }
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
            if(detailed){
                HorizontalDivider()
                Text("البرايمر والمعجون",style=MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("تغطية البرايمر",primerCoverage,{primerCoverage=it;set("primerCoverage",it)},"م²/لتر",modifier=Modifier.weight(1f))
                    NumberFieldX("أوجه البرايمر",primerCoats,{primerCoats=it;set("primerCoats",it)},"وجه",modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("معدل المعجون",puttyRate,{puttyRate=it;set("puttyRate",it)},"كجم/م²/وجه","اكتب معدل المنتج والتنفيذ.",Modifier.weight(1f))
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
                explanation="الدهان = المساحة × عدد الأوجه ÷ معدل التغطية، وبعدها الهالك.\nالأرقام بتعتمد على معدل التغطية الفعلي للمنتج وحالة السطح، علشان كده كل المعدلات قابلة للتعديل.",
                copyText="دهانات ${fmt(a)} م² — دهان ${fmt(paintLiters)} لتر"+if(detailed)" — برايمر ${fmt(primerLiters)} لتر — معجون ${fmt(puttyKg)} كجم" else ""
            )
        }
    }
}

@Composable
fun WaterproofMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="waterproof_materials"
    var type by remember{mutableStateOf("عزل دهان / أسمنتي")}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var rate by remember{mutableStateOf(repository.getToolValue("$toolId.rate","1.5"))}
    var coats by remember{mutableStateOf(repository.getToolValue("$toolId.coats","2"))}
    var packWeight by remember{mutableStateOf(repository.getToolValue("$toolId.packWeight","20"))}
    var rollLength by remember{mutableStateOf(repository.getToolValue("$toolId.rollLength","10"))}
    var rollWidth by remember{mutableStateOf(repository.getToolValue("$toolId.rollWidth","1"))}
    var overlap by remember{mutableStateOf(repository.getToolValue("$toolId.overlap","10"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","5"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val coatingKg=a*n(rate)*n(coats)*(1+n(waste)/100.0)
    val packs=if(n(packWeight)>0)ceil(coatingKg/n(packWeight)).toInt() else 0
    val nominalRollArea=n(rollLength)*n(rollWidth)
    val effectiveRollArea=nominalRollArea*(1-n(overlap)/100.0)
    val rollAreaNeed=a*(1+n(waste)/100.0)
    val rolls=if(effectiveRollArea>0)ceil(rollAreaNeed/effectiveRollArea).toInt() else 0

    ToolPage("خامات العزل","دهان/أسمنتي أو لفائف",repository,toolId,onBack){
        CardBox{
            ChoiceFieldX("نوع العزل",type,listOf("عزل دهان / أسمنتي","لفائف عزل"),{type=it})
            NumberFieldX("المساحة",area,{area=it;set("area",it)},"م²")
            if(type=="عزل دهان / أسمنتي"){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("استهلاك الوجه",rate,{rate=it;set("rate",it)},"كجم/م²","خد المعدل من نشرة المنتج.",Modifier.weight(1f))
                    NumberFieldX("عدد الأوجه",coats,{coats=it;set("coats",it)},"وجه",modifier=Modifier.weight(1f))
                }
                NumberFieldX("وزن العبوة",packWeight,{packWeight=it;set("packWeight",it)},"كجم")
            }else{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("طول الرول",rollLength,{rollLength=it;set("rollLength",it)},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("عرض الرول",rollWidth,{rollWidth=it;set("rollWidth",it)},"م",modifier=Modifier.weight(1f))
                }
                NumberFieldX("تراكب اللفائف",overlap,{overlap=it;set("overlap",it)},"%","النسبة اللي بتضيع في التراكب بين الرولات.")
            }
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
        }
        if(a>0){
            if(type=="عزل دهان / أسمنتي"){
                ToolResultCard(
                    rows=listOf("عدد العبوات" to "$packs عبوة","إجمالي الخامة" to "${fmt(coatingKg)} كجم"),
                    explanation="الخامة = المساحة × استهلاك الوجه × عدد الأوجه × الهالك. معدل الاستهلاك لازم يتراجع من المنتج المستخدم.",
                    copyText="عزل ${fmt(a)} م² — ${fmt(coatingKg)} كجم ≈ $packs عبوة"
                )
            }else{
                ToolResultCard(
                    rows=listOf("عدد الرولات" to "$rolls رول","مساحة الرول الفعلية بعد التراكب" to "${fmt(effectiveRollArea)} م²"),
                    explanation="مساحة الرول الاسمية = الطول × العرض. البرنامج خصم تراكب ${fmt(n(overlap))}% ثم حسب عدد الرولات وقرّبه لأعلى.",
                    copyText="لفائف عزل ${fmt(a)} م² — $rolls رول"
                )
            }
        }
    }
}

@Composable
fun GypsumMaterialsScreen(repository:ProjectRepository,seedArea:Double?,onBack:()->Unit){
    val toolId="gypsum_materials"
    var detailed by remember{mutableStateOf(false)}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var boardW by remember{mutableStateOf(repository.getToolValue("$toolId.boardW","1.20"))}
    var boardH by remember{mutableStateOf(repository.getToolValue("$toolId.boardH","2.40"))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","10"))}
    var mainRate by remember{mutableStateOf(repository.getToolValue("$toolId.mainRate","0.9"))}
    var furringRate by remember{mutableStateOf(repository.getToolValue("$toolId.furringRate","2.8"))}
    var screwsRate by remember{mutableStateOf(repository.getToolValue("$toolId.screwsRate","20"))}
    var compoundRate by remember{mutableStateOf(repository.getToolValue("$toolId.compoundRate","0.35"))}
    fun set(k:String,v:String){repository.setToolValue("$toolId.$k",v)}
    val a=n(area)
    val purchaseArea=a*(1+n(waste)/100.0)
    val boardArea=n(boardW)*n(boardH)
    val boards=if(boardArea>0)ceil(purchaseArea/boardArea).toInt() else 0
    val main=purchaseArea*n(mainRate)
    val furring=purchaseArea*n(furringRate)
    val screws=ceil(purchaseArea*n(screwsRate)).toInt()
    val compound=purchaseArea*n(compoundRate)

    ToolPage("خامات الجبس بورد","ألواح وباقي الخامات بمعدلات واضحة",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it}
        CardBox{
            NumberFieldX("مساحة السقف / الحائط",area,{area=it;set("area",it)},"م²")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("عرض اللوح",boardW,{boardW=it;set("boardW",it)},"م",modifier=Modifier.weight(1f))
                NumberFieldX("طول اللوح",boardH,{boardH=it;set("boardH",it)},"م",modifier=Modifier.weight(1f))
            }
            NumberFieldX("هالك وقص",waste,{waste=it;set("waste",it)},"%")
            if(detailed){
                HorizontalDivider()
                Text("معدلات التركيب",style=MaterialTheme.typography.labelLarge)
                Text("المعدلات دي ظاهرة وقابلة للتعديل حسب النظام اللي هتنفذه.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                NumberFieldX("القطاع الرئيسي",mainRate,{mainRate=it;set("mainRate",it)},"م ط/م²","اكتب معدل النظام المستخدم.")
                NumberFieldX("القطاع الثانوي",furringRate,{furringRate=it;set("furringRate",it)},"م ط/م²")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("مسامير",screwsRate,{screwsRate=it;set("screwsRate",it)},"عدد/م²",modifier=Modifier.weight(1f))
                    NumberFieldX("معجون فواصل",compoundRate,{compoundRate=it;set("compoundRate",it)},"كجم/م²",modifier=Modifier.weight(1f))
                }
            }
        }
        if(a>0 && boardArea>0){
            val rows=buildList{
                add("عدد الألواح" to "$boards لوح")
                add("مساحة شراء بالأهلاك" to "${fmt(purchaseArea)} م²")
                if(detailed){
                    add("قطاع رئيسي" to "${fmt(main)} م ط")
                    add("قطاع ثانوي" to "${fmt(furring)} م ط")
                    add("مسامير" to "$screws مسمار تقريبًا")
                    add("معجون فواصل" to "${fmt(compound)} كجم")
                }
            }
            ToolResultCard(
                rows=rows,
                explanation="عدد الألواح = مساحة التنفيذ بعد الهالك ÷ مساحة اللوح، مع التقريب لأعلى.\nفي الوضع التفصيلي باقي الخامات = المساحة × المعدل اللي أنت مدخله؛ لأن نظام التعليق والمسافات بين القطاعات بتختلف من نظام للتاني.",
                copyText="جبس بورد ${fmt(a)} م² — $boards لوح"+if(detailed)" — رئيسي ${fmt(main)} م ط — ثانوي ${fmt(furring)} م ط" else ""
            )
        }
    }
}

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.data.ProjectRepository
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun MortarMaterialsScreen(
    toolId:String,
    title:String,
    subtitle:String,
    repository:ProjectRepository,
    seedArea:Double?,
    onBack:()->Unit,
    defaultCement:Double,
    defaultSand:Double,
    defaultThicknessMm:Double,
    defaultWaste:Double,
    onOpenTool:(String,Double?)->Unit
){
    var detailed by remember{mutableStateOf(false)}
    val areaDefault=seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area","")
    var area by remember(toolId,seedArea){mutableStateOf(areaDefault)}
    var thickness by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.thickness",fmt(defaultThicknessMm)))}
    var cementPart by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.cementPart",fmt(defaultCement)))}
    var sandPart by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.sandPart",fmt(defaultSand)))}
    var waste by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.waste",fmt(defaultWaste)))}
    var dryFactor by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.dryFactor","1.33"))}
    var cementDensity by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}
    var bagWeight by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}

    fun save(key:String,value:String){repository.setToolValue("$toolId.$key",value)}

    val a=n(area)
    val t=n(thickness)/1000.0
    val c=n(cementPart)
    val s=n(sandPart)
    val parts=(c+s).takeIf{it>0}?:1.0
    val wet=a*t
    val dry=wet*n(dryFactor)
    val dryWithWaste=dry*(1+n(waste)/100.0)
    val cementVol=dryWithWaste*(c/parts)
    val cementKg=cementVol*n(cementDensity)
    val bags=if(n(bagWeight)>0)ceil(cementKg/n(bagWeight)).toInt() else 0
    val sandM3=dryWithWaste*(s/parts)

    ToolPage(title,subtitle,repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it}

        CardBox{
            SectionTitle("دخل البيانات","الأرقام اللي بتتكرر البرنامج بيفتكرها.")
            NumberFieldX("المساحة",area,{area=it;save("area",it)},"م²","اكتب صافي المساحة اللي هيتنفذ عليها البند.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX(
                    "متوسط السمك",thickness,{thickness=it;save("thickness",it)},"مم",
                    "اكتب متوسط السمك الفعلي، مش أكبر سمك موجود في نقطة واحدة.",
                    Modifier.weight(1f)
                )
                NumberFieldX(
                    "الهالك",waste,{waste=it;save("waste",it)},"%",
                    "زيادة بسيطة للخسارة والاختلافات في التنفيذ.",
                    Modifier.weight(1f)
                )
            }
            Text("الخلطة",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("أسمنت",cementPart,{cementPart=it;save("cementPart",it)},"جزء","نسبة الأسمنت في الخلطة.",Modifier.weight(1f))
                NumberFieldX("رمل",sandPart,{sandPart=it;save("sandPart",it)},"جزء","مثال 1 أسمنت : 4 رمل.",Modifier.weight(1f))
            }

            if(detailed){
                HorizontalDivider()
                Text("إعدادات متقدمة",style=MaterialTheme.typography.labelLarge)
                NumberFieldX(
                    "معامل الحجم الجاف",dryFactor,{dryFactor=it;save("dryFactor",it)},"",
                    "المونة قبل الخلط حجم مكوناتها أكبر من حجمها بعد التنفيذ. الرقم قابل للتعديل."
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("كثافة الأسمنت",cementDensity,{cementDensity=it;save("cementDensity",it)},"كجم/م³","قيمة تقديرية للحساب.",Modifier.weight(1f))
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;save("bagWeight",it)},"كجم","غيرها لو الشيكارة عندك وزن مختلف.",Modifier.weight(1f))
                }
            }
        }

        if(a>0 && t>0){
            val copy="$title — مساحة ${fmt(a)} م² — أسمنت ${fmt(cementKg)} كجم ≈ $bags شيكارة — رمل ${fmt(sandM3)} م³"
            val explain="حجم المونة بعد التنفيذ = المساحة × السمك = ${fmt(a)} × ${fmt(t)} = ${fmt(wet)} م³.\n" +
                "الحجم الجاف = ${fmt(wet)} × ${fmt(n(dryFactor))}، وبعد الهالك = ${fmt(dryWithWaste)} م³.\n" +
                "الخلطة ${fmt(c)} : ${fmt(s)}، لذلك نصيب الأسمنت والرمل اتقسم على مجموع أجزاء الخلطة.\n" +
                "الأسمنت اتحول من م³ إلى كجم بكثافة ${fmt(n(cementDensity))} كجم/م³ ثم إلى شكاير وزن ${fmt(n(bagWeight))} كجم."
            val links=when(toolId){
                "plaster_materials"->listOf("احسب الطرطشة لنفس المساحة" to {onOpenTool("splash_materials",a)})
                "splash_materials"->listOf("احسب المحارة لنفس المساحة" to {onOpenTool("plaster_materials",a)})
                else->emptyList()
            }
            ToolResultCard(
                rows=listOf(
                    "عدد شكاير الأسمنت" to "$bags شيكارة",
                    "الأسمنت" to "${fmt(cementKg)} كجم",
                    "الرمل" to "${fmt(sandM3)} م³",
                    "حجم المونة بعد التنفيذ" to "${fmt(wet)} م³"
                ),
                explanation=explain,
                copyText=copy,
                links=links
            )
        }
    }
}

@Composable
fun MasonryMaterialsScreen(
    repository:ProjectRepository,
    seedArea:Double?,
    onBack:()->Unit
){
    val toolId="masonry_materials"
    var detailed by remember{mutableStateOf(false)}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var wallThickness by remember{mutableStateOf(repository.getToolValue("$toolId.wallThickness","0.12"))}
    var brickLength by remember{mutableStateOf(repository.getToolValue("$toolId.brickLength","25"))}
    var brickWidth by remember{mutableStateOf(repository.getToolValue("$toolId.brickWidth","12"))}
    var brickHeight by remember{mutableStateOf(repository.getToolValue("$toolId.brickHeight","6"))}
    var jointMm by remember{mutableStateOf(repository.getToolValue("$toolId.joint","10"))}
    var brickWaste by remember{mutableStateOf(repository.getToolValue("$toolId.brickWaste","5"))}
    var cementPart by remember{mutableStateOf(repository.getToolValue("$toolId.cementPart","1"))}
    var sandPart by remember{mutableStateOf(repository.getToolValue("$toolId.sandPart","5"))}
    var dryFactor by remember{mutableStateOf(repository.getToolValue("$toolId.dryFactor","1.33"))}
    var bagWeight by remember{mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}
    var cementDensity by remember{mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}

    fun set(key:String,value:String){repository.setToolValue("$toolId.$key",value)}
    val a=n(area)
    val wt=n(wallThickness)
    val bl=n(brickLength)/100.0
    val bw=n(brickWidth)/100.0
    val bh=n(brickHeight)/100.0
    val joint=n(jointMm)/1000.0
    val moduleArea=(bl+joint)*(bh+joint)
    val netBricks=if(moduleArea>0)a/moduleArea else 0.0
    val purchaseBricks=ceil(netBricks*(1+n(brickWaste)/100.0)).toInt()
    val wallVolume=a*wt
    val actualBrickVolume=bl*bw*bh*netBricks
    val wetMortar=max(0.0,wallVolume-actualBrickVolume)
    val dryMortar=wetMortar*n(dryFactor)
    val parts=(n(cementPart)+n(sandPart)).takeIf{it>0}?:1.0
    val cementKg=dryMortar*(n(cementPart)/parts)*n(cementDensity)
    val bags=if(n(bagWeight)>0)ceil(cementKg/n(bagWeight)).toInt() else 0
    val sand=dryMortar*(n(sandPart)/parts)

    ToolPage("مباني وطوب","عدد الطوب ومونة المباني بشكل تقريبي",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it}
        CardBox{
            NumberFieldX("صافي مساحة المباني",area,{area=it;set("area",it)},"م²","بعد خصم الفتحات.")
            NumberFieldX("سمك الحائط",wallThickness,{wallThickness=it;set("wallThickness",it)},"م","مثال 12 سم = 0.12 م.")
            Text("مقاس الطوبة",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("الطول",brickLength,{brickLength=it;set("brickLength",it)},"سم",modifier=Modifier.weight(1f))
                NumberFieldX("العرض",brickWidth,{brickWidth=it;set("brickWidth",it)},"سم",modifier=Modifier.weight(1f))
                NumberFieldX("الارتفاع",brickHeight,{brickHeight=it;set("brickHeight",it)},"سم",modifier=Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("سمك اللحام",jointMm,{jointMm=it;set("joint",it)},"مم","متوسط سمك المونة بين الطوب.",Modifier.weight(1f))
                NumberFieldX("هالك الطوب",brickWaste,{brickWaste=it;set("brickWaste",it)},"%",modifier=Modifier.weight(1f))
            }
            Text("خلطة مونة المباني",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("أسمنت",cementPart,{cementPart=it;set("cementPart",it)},"جزء",modifier=Modifier.weight(1f))
                NumberFieldX("رمل",sandPart,{sandPart=it;set("sandPart",it)},"جزء",modifier=Modifier.weight(1f))
            }
            if(detailed){
                NumberFieldX("معامل الحجم الجاف",dryFactor,{dryFactor=it;set("dryFactor",it)},"","قابل للتعديل حسب طريقة الحساب.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                    NumberFieldX("كثافة الأسمنت",cementDensity,{cementDensity=it;set("cementDensity",it)},"كجم/م³",modifier=Modifier.weight(1f))
                }
            }
        }
        if(a>0 && moduleArea>0){
            ToolResultCard(
                rows=listOf(
                    "الطوب المطلوب" to "$purchaseBricks طوبة",
                    "مونة المباني" to "${fmt(wetMortar)} م³",
                    "أسمنت المونة" to "$bags شيكارة ≈ ${fmt(cementKg)} كجم",
                    "رمل المونة" to "${fmt(sand)} م³"
                ),
                explanation="عدد الطوب اتحسب من صافي مساحة الحائط ÷ مساحة الطوبة مع اللحام، وبعدها اتضاف هالك ${fmt(n(brickWaste))}%.\nمونة المباني تقريبًا = حجم الحائط - الحجم الفعلي للطوب، وبعدها اتوزعت على خلطة ${fmt(n(cementPart))}:${fmt(n(sandPart))}.\nالنتيجة تقريبية لأن الرباط والكسور وطريقة البناء بتفرق.",
                copyText="مباني ${fmt(a)} م² — طوب $purchaseBricks — مونة ${fmt(wetMortar)} م³ — أسمنت $bags شيكارة — رمل ${fmt(sand)} م³"
            )
        }
    }
}

@Composable
fun ConcreteMaterialsScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="concrete_materials"
    var detailed by remember{mutableStateOf(false)}
    var mode by remember{mutableStateOf("أبعاد")}
    var length by remember{mutableStateOf(repository.getToolValue("$toolId.length",""))}
    var width by remember{mutableStateOf(repository.getToolValue("$toolId.width",""))}
    var height by remember{mutableStateOf(repository.getToolValue("$toolId.height",""))}
    var count by remember{mutableStateOf(repository.getToolValue("$toolId.count","1"))}
    var directVolume by remember{mutableStateOf(repository.getToolValue("$toolId.volume",""))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","3"))}
    var cementPart by remember{mutableStateOf(repository.getToolValue("$toolId.cementPart","1"))}
    var sandPart by remember{mutableStateOf(repository.getToolValue("$toolId.sandPart","2"))}
    var gravelPart by remember{mutableStateOf(repository.getToolValue("$toolId.gravelPart","4"))}
    var dryFactor by remember{mutableStateOf(repository.getToolValue("$toolId.dryFactor","1.54"))}
    var cementDensity by remember{mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}
    var bagWeight by remember{mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}
    var waterCement by remember{mutableStateOf(repository.getToolValue("$toolId.waterCement","0.50"))}
    fun set(key:String,value:String){repository.setToolValue("$toolId.$key",value)}

    val baseVol=if(mode=="حجم جاهز")n(directVolume) else n(length)*n(width)*n(height)*n(count).toInt().coerceAtLeast(1)
    val targetVol=baseVol*(1+n(waste)/100.0)
    val dry=targetVol*n(dryFactor)
    val totalParts=(n(cementPart)+n(sandPart)+n(gravelPart)).takeIf{it>0}?:1.0
    val cementKg=dry*(n(cementPart)/totalParts)*n(cementDensity)
    val bags=if(n(bagWeight)>0)ceil(cementKg/n(bagWeight)).toInt() else 0
    val sand=dry*(n(sandPart)/totalParts)
    val gravel=dry*(n(gravelPart)/totalParts)
    val waterLiters=cementKg*n(waterCement)

    ToolPage("خرسانة بسيطة","تقدير خامات موقع — مش تصميم خلطة خرسانية",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it}
        CardBox{
            ChoiceFieldX("هتدخل الكمية إزاي؟",mode,listOf("أبعاد","حجم جاهز"),{mode=it})
            if(mode=="أبعاد"){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("الطول",length,{length=it;set("length",it)},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("العرض",width,{width=it;set("width",it)},"م",modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("السمك/الارتفاع",height,{height=it;set("height",it)},"م",modifier=Modifier.weight(1f))
                    NumberFieldX("العدد",count,{count=it;set("count",it)},"عدد",modifier=Modifier.weight(1f))
                }
            }else NumberFieldX("حجم الخرسانة",directVolume,{directVolume=it;set("volume",it)},"م³")
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
            Text("نسبة الخلطة",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                NumberFieldX("أسمنت",cementPart,{cementPart=it;set("cementPart",it)},"جزء",modifier=Modifier.weight(1f))
                NumberFieldX("رمل",sandPart,{sandPart=it;set("sandPart",it)},"جزء",modifier=Modifier.weight(1f))
                NumberFieldX("سن",gravelPart,{gravelPart=it;set("gravelPart",it)},"جزء",modifier=Modifier.weight(1f))
            }
            if(detailed){
                NumberFieldX("معامل الحجم الجاف",dryFactor,{dryFactor=it;set("dryFactor",it)},"","قيمة تقديرية وقابلة للتعديل.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                    NumberFieldX("كثافة الأسمنت",cementDensity,{cementDensity=it;set("cementDensity",it)},"كجم/م³",modifier=Modifier.weight(1f))
                }
                NumberFieldX("مياه/أسمنت",waterCement,{waterCement=it;set("waterCement",it)},"","نسبة تقريبية لتقدير المياه، وليست تصميم خلطة.")
            }
            Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.secondaryContainer){
                Text(
                    "دي حاسبة تقديرية لخامات الخلط في الموقع، مش بديل لتصميم خلطة معتمد.",
                    Modifier.padding(9.dp),
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        if(baseVol>0){
            ToolResultCard(
                rows=listOf(
                    "الخرسانة المطلوبة" to "${fmt(targetVol)} م³",
                    "الأسمنت" to "$bags شيكارة ≈ ${fmt(cementKg)} كجم",
                    "الرمل" to "${fmt(sand)} م³",
                    "السن" to "${fmt(gravel)} م³",
                    "المياه التقريبية" to "${fmt(waterLiters)} لتر"
                ),
                explanation="الحجم المطلوب بعد الهالك = ${fmt(targetVol)} م³. الحجم الجاف = الحجم × ${fmt(n(dryFactor))}.\nالخامات اتوزعت على نسبة ${fmt(n(cementPart))}:${fmt(n(sandPart))}:${fmt(n(gravelPart))}.\nالمياه = وزن الأسمنت × نسبة مياه/أسمنت ${fmt(n(waterCement))}.",
                copyText="خرسانة ${fmt(targetVol)} م³ — أسمنت $bags شيكارة — رمل ${fmt(sand)} م³ — سن ${fmt(gravel)} م³ — مياه ${fmt(waterLiters)} لتر"
            )
        }
    }
}

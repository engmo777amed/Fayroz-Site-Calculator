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
fun LogicWarning(text:String){
    Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.errorContainer.copy(alpha=.55f)){
        Text(text,Modifier.fillMaxWidth().padding(8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onErrorContainer)
    }
}

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
    val presets=remember(toolId){mortarPresets(toolId)}
    val basePreset=presets.first()
    var detailed by remember{mutableStateOf(repository.getToolValue("$toolId.view","quick")=="detailed")}
    val areaDefault=seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area","")
    var area by remember(toolId,seedArea){mutableStateOf(areaDefault)}
    var presetLabel by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.preset",basePreset.label))}
    var thickness by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.thickness",fmt(basePreset.thicknessMm)))}
    var thicknessUnit by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.thicknessUnit","مم"))}
    var cementPart by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.cementPart",fmt(basePreset.cement)))}
    var sandPart by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.sandPart",fmt(basePreset.sand)))}
    var waste by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.waste",fmt(basePreset.waste)))}
    var dryFactor by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.dryFactor",fmt(basePreset.dryFactor)))}
    var cementDensity by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}
    var bagWeight by remember(toolId){mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}

    fun save(key:String,value:String){repository.setToolValue("$toolId.$key",value)}
    fun applyPreset(p:MortarPreset){
        presetLabel=p.label
        cementPart=fmt(p.cement);sandPart=fmt(p.sand)
        thickness=fmt(p.thicknessMm);thicknessUnit="مم"
        waste=fmt(p.waste);dryFactor=fmt(p.dryFactor)
        save("preset",p.label);save("cementPart",cementPart);save("sandPart",sandPart)
        save("thickness",thickness);save("thicknessUnit",thicknessUnit)
        save("waste",waste);save("dryFactor",dryFactor)
    }

    val a=n(area)
    val t=lengthToMeters(thickness,thicknessUnit)
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
    val selectedPreset=presets.firstOrNull{it.label==presetLabel}

    ToolPage(title,subtitle,repository,toolId,onBack){
        QuickDetailedSwitch(detailed){
            detailed=it
            save("view",if(it)"detailed" else "quick")
        }

        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{applyPreset(basePreset)}
            }
            ChoiceFieldX(
                "الخلطة الجاهزة",
                presetLabel,
                presets.map{it.label}+listOf("خلطة تانية"),
                {label->
                    if(label=="خلطة تانية"){
                        presetLabel=label;detailed=true
                        save("preset",label);save("view","detailed")
                    }else presets.firstOrNull{it.label==label}?.let(::applyPreset)
                },
                "اختار خلطة جاهزة كبداية، وتقدر تعدل كل رقم في الوضع التفصيلي."
            )
            selectedPreset?.let{PresetNote(it.note)}
            NumberFieldX("المساحة",area,{area=it;save("area",it)},"م²","اكتب صافي المساحة اللي هيتنفذ عليها البند.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberWithUnitX(
                    "متوسط السمك",thickness,{thickness=it;save("thickness",it)},
                    thicknessUnit,{thicknessUnit=it;save("thicknessUnit",it)},
                    help="اكتب متوسط السمك الفعلي. تقدر تستخدم مم أو سم أو م.",
                    modifier=Modifier.weight(1f)
                )
                NumberFieldX("الهالك",waste,{waste=it;save("waste",it)},"%",
                    "زيادة بسيطة للخسارة والاختلافات في التنفيذ.",Modifier.weight(1f))
            }

            if((toolId=="plaster_materials" && t>0.05)||(toolId=="splash_materials" && t>0.01)){
                LogicWarning(if(toolId=="plaster_materials")"السمك كبير للمحارة العادية. راجع القيمة أو حالة الحائط." else "سمك الطرطشة كبير. راجع القيمة.")
            }
            if(n(waste)>25)LogicWarning("الهالك أعلى من المعتاد. راجع النسبة.")

            if(detailed){
                HorizontalDivider()
                Text("تفاصيل الخلطة",style=MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("أسمنت",cementPart,{cementPart=it;presetLabel="خلطة تانية";save("cementPart",it);save("preset",presetLabel)},"جزء","نسبة الأسمنت.",Modifier.weight(1f))
                    NumberFieldX("رمل",sandPart,{sandPart=it;presetLabel="خلطة تانية";save("sandPart",it);save("preset",presetLabel)},"جزء","مثال 1 أسمنت : 4 رمل.",Modifier.weight(1f))
                }
                NumberFieldX(
                    "معامل الحجم الجاف",dryFactor,{dryFactor=it;save("dryFactor",it)},"",
                    "معامل تحويل تقديري من حجم المونة المنفذة إلى حجم المكونات الجافة. تقدر تعدله حسب مرجعك."
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("كثافة الأسمنت",cementDensity,{cementDensity=it;save("cementDensity",it)},"كجم/م³","قيمة حسابية قابلة للتعديل.",Modifier.weight(1f))
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;save("bagWeight",it)},"كجم","الافتراضي 50 كجم.",Modifier.weight(1f))
                }
            }
        }

        if(a>0 && t>0){
            val copy="$title — مساحة ${fmt(a)} م² — أسمنت ${fmt(cementKg)} كجم ≈ $bags شيكارة — رمل ${fmt(sandM3)} م³"
            val explain="حجم المونة بعد التنفيذ = المساحة × السمك = ${fmt(a)} × ${fmt(t)} = ${fmt(wet)} م³.\n" +
                "الحجم الجاف بعد الهالك = ${fmt(dryWithWaste)} م³.\n" +
                "الخلطة ${fmt(c)} : ${fmt(s)}، وبعدها الأسمنت اتحول لكجم وشكاير."
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
                    "حجم المونة" to "${fmt(wet)} م³"
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
    val basePreset=masonryMixPresets.first()
    var detailed by remember{mutableStateOf(repository.getToolValue("$toolId.view","quick")=="detailed")}
    var presetLabel by remember{mutableStateOf(repository.getToolValue("$toolId.preset",basePreset.label))}
    var area by remember(seedArea){mutableStateOf(seedArea?.let(::fmt) ?: repository.getToolValue("$toolId.area",""))}
    var wallThickness by remember{mutableStateOf(repository.getToolValue("$toolId.wallThickness","12"))}
    var wallUnit by remember{mutableStateOf(repository.getToolValue("$toolId.wallUnit","سم"))}
    var brickLength by remember{mutableStateOf(repository.getToolValue("$toolId.brickLength","25"))}
    var brickWidth by remember{mutableStateOf(repository.getToolValue("$toolId.brickWidth","12"))}
    var brickHeight by remember{mutableStateOf(repository.getToolValue("$toolId.brickHeight","6"))}
    var brickUnit by remember{mutableStateOf(repository.getToolValue("$toolId.brickUnit","سم"))}
    var joint by remember{mutableStateOf(repository.getToolValue("$toolId.joint","10"))}
    var jointUnit by remember{mutableStateOf(repository.getToolValue("$toolId.jointUnit","مم"))}
    var brickWaste by remember{mutableStateOf(repository.getToolValue("$toolId.brickWaste","5"))}
    var cementPart by remember{mutableStateOf(repository.getToolValue("$toolId.cementPart",fmt(basePreset.cement)))}
    var sandPart by remember{mutableStateOf(repository.getToolValue("$toolId.sandPart",fmt(basePreset.sand)))}
    var dryFactor by remember{mutableStateOf(repository.getToolValue("$toolId.dryFactor","1.33"))}
    var bagWeight by remember{mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}
    var cementDensity by remember{mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}

    fun set(key:String,value:String){repository.setToolValue("$toolId.$key",value)}
    fun applyPreset(p:SimpleMixPreset){
        presetLabel=p.label;cementPart=fmt(p.cement);sandPart=fmt(p.sand)
        set("preset",presetLabel);set("cementPart",cementPart);set("sandPart",sandPart)
    }

    val a=n(area)
    val wt=lengthToMeters(wallThickness,wallUnit)
    val bl=lengthToMeters(brickLength,brickUnit)
    val bw=lengthToMeters(brickWidth,brickUnit)
    val bh=lengthToMeters(brickHeight,brickUnit)
    val jointM=lengthToMeters(joint,jointUnit)
    val moduleArea=(bl+jointM)*(bh+jointM)
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
        QuickDetailedSwitch(detailed){detailed=it;set("view",if(it)"detailed" else "quick")}
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    applyPreset(basePreset)
                    wallThickness="12";wallUnit="سم";brickLength="25";brickWidth="12";brickHeight="6";brickUnit="سم";joint="10";jointUnit="مم";brickWaste="5"
                    set("wallThickness",wallThickness);set("wallUnit",wallUnit);set("brickLength",brickLength);set("brickWidth",brickWidth);set("brickHeight",brickHeight);set("brickUnit",brickUnit);set("joint",joint);set("jointUnit",jointUnit);set("brickWaste",brickWaste)
                }
            }
            ChoiceFieldX("خلطة مونة المباني",presetLabel,masonryMixPresets.map{it.label}+listOf("خلطة تانية"),{label->
                if(label=="خلطة تانية"){presetLabel=label;detailed=true;set("preset",label);set("view","detailed")}
                else masonryMixPresets.firstOrNull{it.label==label}?.let(::applyPreset)
            },"النسبة الافتراضية بداية للحساب، والمواصفة المعتمدة للمشروع هي المرجع.")
            masonryMixPresets.firstOrNull{it.label==presetLabel}?.let{PresetNote(it.note)}
            NumberFieldX("صافي مساحة المباني",area,{area=it;set("area",it)},"م²","بعد خصم الفتحات.")
            NumberWithUnitX("سمك الحائط",wallThickness,{wallThickness=it;set("wallThickness",it)},wallUnit,{wallUnit=it;set("wallUnit",it)},help="مثال: 12 سم أو 0.12 م.")
            Text("مقاس الطوبة",style=MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberWithUnitX("الطول",brickLength,{brickLength=it;set("brickLength",it)},brickUnit,{brickUnit=it;set("brickUnit",it)},modifier=Modifier.weight(1f))
                NumberWithUnitX("العرض",brickWidth,{brickWidth=it;set("brickWidth",it)},brickUnit,{brickUnit=it;set("brickUnit",it)},modifier=Modifier.weight(1f))
            }
            NumberWithUnitX("الارتفاع",brickHeight,{brickHeight=it;set("brickHeight",it)},brickUnit,{brickUnit=it;set("brickUnit",it)})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                NumberWithUnitX("سمك اللحام",joint,{joint=it;set("joint",it)},jointUnit,{jointUnit=it;set("jointUnit",it)},help="متوسط سمك المونة بين الطوب.",modifier=Modifier.weight(1f))
                NumberFieldX("هالك الطوب",brickWaste,{brickWaste=it;set("brickWaste",it)},"%",modifier=Modifier.weight(1f))
            }
            if(n(brickWaste)>20)LogicWarning("هالك الطوب مرتفع. راجع النسبة.")
            if(detailed){
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("أسمنت",cementPart,{cementPart=it;presetLabel="خلطة تانية";set("cementPart",it);set("preset",presetLabel)},"جزء",modifier=Modifier.weight(1f))
                    NumberFieldX("رمل",sandPart,{sandPart=it;presetLabel="خلطة تانية";set("sandPart",it);set("preset",presetLabel)},"جزء",modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                    NumberFieldX("معامل الحجم الجاف",dryFactor,{dryFactor=it;set("dryFactor",it)},"",modifier=Modifier.weight(1f))
                }
                NumberFieldX("كثافة الأسمنت",cementDensity,{cementDensity=it;set("cementDensity",it)},"كجم/م³")
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
                explanation="عدد الطوب = صافي مساحة الحائط ÷ مساحة الطوبة مع اللحام، وبعدها الهالك. مونة المباني تقريبًا = حجم الحائط - حجم الطوب.",
                copyText="مباني ${fmt(a)} م² — طوب $purchaseBricks — أسمنت $bags شيكارة — رمل ${fmt(sand)} م³"
            )
        }
    }
}

@Composable
fun ConcreteMaterialsScreen(repository:ProjectRepository,onBack:()->Unit){
    val toolId="concrete_materials"
    val basePreset=concreteMixPresets.first()
    var detailed by remember{mutableStateOf(repository.getToolValue("$toolId.view","quick")=="detailed")}
    var presetLabel by remember{mutableStateOf(repository.getToolValue("$toolId.preset",basePreset.label))}
    var mode by remember{mutableStateOf("أبعاد")}
    var length by remember{mutableStateOf(repository.getToolValue("$toolId.length",""))}
    var width by remember{mutableStateOf(repository.getToolValue("$toolId.width",""))}
    var height by remember{mutableStateOf(repository.getToolValue("$toolId.height",""))}
    var dimUnit by remember{mutableStateOf(repository.getToolValue("$toolId.dimUnit","م"))}
    var count by remember{mutableStateOf(repository.getToolValue("$toolId.count","1"))}
    var directVolume by remember{mutableStateOf(repository.getToolValue("$toolId.volume",""))}
    var waste by remember{mutableStateOf(repository.getToolValue("$toolId.waste","3"))}
    var cementPart by remember{mutableStateOf(repository.getToolValue("$toolId.cementPart",fmt(basePreset.cement)))}
    var sandPart by remember{mutableStateOf(repository.getToolValue("$toolId.sandPart",fmt(basePreset.sand)))}
    var gravelPart by remember{mutableStateOf(repository.getToolValue("$toolId.gravelPart",fmt(basePreset.aggregate)))}
    var dryFactor by remember{mutableStateOf(repository.getToolValue("$toolId.dryFactor","1.54"))}
    var cementDensity by remember{mutableStateOf(repository.getToolValue("$toolId.cementDensity","1440"))}
    var bagWeight by remember{mutableStateOf(repository.getToolValue("$toolId.bagWeight","50"))}
    var waterCement by remember{mutableStateOf(repository.getToolValue("$toolId.waterCement","0.50"))}
    fun set(key:String,value:String){repository.setToolValue("$toolId.$key",value)}
    fun applyPreset(p:SimpleMixPreset){
        presetLabel=p.label;cementPart=fmt(p.cement);sandPart=fmt(p.sand);gravelPart=fmt(p.aggregate)
        set("preset",presetLabel);set("cementPart",cementPart);set("sandPart",sandPart);set("gravelPart",gravelPart)
    }

    val l=lengthToMeters(length,dimUnit)
    val w=lengthToMeters(width,dimUnit)
    val h=lengthToMeters(height,dimUnit)
    val baseVol=if(mode=="حجم جاهز")n(directVolume) else l*w*h*n(count).toInt().coerceAtLeast(1)
    val targetVol=baseVol*(1+n(waste)/100.0)
    val dry=targetVol*n(dryFactor)
    val totalParts=(n(cementPart)+n(sandPart)+n(gravelPart)).takeIf{it>0}?:1.0
    val cementKg=dry*(n(cementPart)/totalParts)*n(cementDensity)
    val bags=if(n(bagWeight)>0)ceil(cementKg/n(bagWeight)).toInt() else 0
    val sand=dry*(n(sandPart)/totalParts)
    val gravel=dry*(n(gravelPart)/totalParts)
    val waterLiters=cementKg*n(waterCement)

    ToolPage("خرسانة بسيطة","تقدير خامات موقع — مش تصميم خلطة خرسانية",repository,toolId,onBack){
        QuickDetailedSwitch(detailed){detailed=it;set("view",if(it)"detailed" else "quick")}
        CardBox{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("دخل البيانات",style=MaterialTheme.typography.titleSmall)
                ResetDefaultsButton{
                    applyPreset(basePreset);dimUnit="م";waste="3";dryFactor="1.54";waterCement="0.50";bagWeight="50"
                    set("dimUnit",dimUnit);set("waste",waste);set("dryFactor",dryFactor);set("waterCement",waterCement);set("bagWeight",bagWeight)
                }
            }
            ChoiceFieldX("الخلطة الجاهزة",presetLabel,concreteMixPresets.map{it.label}+listOf("خلطة تانية"),{label->
                if(label=="خلطة تانية"){presetLabel=label;detailed=true;set("preset",label);set("view","detailed")}
                else concreteMixPresets.firstOrNull{it.label==label}?.let(::applyPreset)
            },"الخلطات هنا تقديرية. الخرسانة الإنشائية تتبع Mix Design معتمد.")
            concreteMixPresets.firstOrNull{it.label==presetLabel}?.let{PresetNote(it.note)}
            ChoiceFieldX("هتدخل الكمية إزاي؟",mode,listOf("أبعاد","حجم جاهز"),{mode=it})
            if(mode=="أبعاد"){
                ChoiceFieldX("وحدة الأبعاد",dimUnit,listOf("م","سم","مم"),{dimUnit=it;set("dimUnit",it)})
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("الطول",length,{length=it;set("length",it)},dimUnit,modifier=Modifier.weight(1f))
                    NumberFieldX("العرض",width,{width=it;set("width",it)},dimUnit,modifier=Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("السمك/الارتفاع",height,{height=it;set("height",it)},dimUnit,modifier=Modifier.weight(1f))
                    NumberFieldX("العدد",count,{count=it;set("count",it)},"عدد",modifier=Modifier.weight(1f))
                }
            }else NumberFieldX("حجم الخرسانة",directVolume,{directVolume=it;set("volume",it)},"م³")
            NumberFieldX("هالك",waste,{waste=it;set("waste",it)},"%")
            if(n(waste)>15)LogicWarning("الهالك مرتفع للخرسانة. راجع النسبة.")
            if(detailed){
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("أسمنت",cementPart,{cementPart=it;presetLabel="خلطة تانية";set("cementPart",it);set("preset",presetLabel)},"جزء",modifier=Modifier.weight(1f))
                    NumberFieldX("رمل",sandPart,{sandPart=it;presetLabel="خلطة تانية";set("sandPart",it);set("preset",presetLabel)},"جزء",modifier=Modifier.weight(1f))
                }
                NumberFieldX("سن",gravelPart,{gravelPart=it;presetLabel="خلطة تانية";set("gravelPart",it);set("preset",presetLabel)},"جزء")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    NumberFieldX("وزن الشيكارة",bagWeight,{bagWeight=it;set("bagWeight",it)},"كجم",modifier=Modifier.weight(1f))
                    NumberFieldX("معامل الحجم الجاف",dryFactor,{dryFactor=it;set("dryFactor",it)},"",modifier=Modifier.weight(1f))
                }
                NumberFieldX("مياه/أسمنت",waterCement,{waterCement=it;set("waterCement",it)},"","نسبة تقريبية لتقدير المياه، وليست تصميم خلطة.")
            }
            PresetNote("دي حاسبة تقديرية لخامات الخلط في الموقع، مش بديل لتصميم خلطة خرسانية معتمد.")
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
                explanation="الحجم بعد الهالك = ${fmt(targetVol)} م³. المكونات اتوزعت على نسبة الخلطة الحالية.",
                copyText="خرسانة ${fmt(targetVol)} م³ — أسمنت $bags شيكارة — رمل ${fmt(sand)} م³ — سن ${fmt(gravel)} م³"
            )
        }
    }
}

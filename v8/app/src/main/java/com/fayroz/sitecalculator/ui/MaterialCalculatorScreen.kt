@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.MaterialEngine

private data class SaveTarget(val label:String,val sectionId:String?,val spaceId:String?)

@Composable
fun MaterialCalculatorScreen(
    toolId:String,
    seed:MaterialResult?,
    repository:V8Repository,
    activeProject:Project?,
    onBack:()->Unit,
    onSave:(MaterialResult,String?,String?)->Unit
){
    val tool=materialTools.firstOrNull{it.id==toolId}
    val title=seed?.title ?: tool?.title ?: "حاسبة خامات"
    var area by remember(toolId,seed?.sourceQuantity){
        mutableStateOf(seed?.sourceQuantity?.takeIf{it>0}?.let(::fmt) ?: repository.pref("$toolId.area",""))
    }
    var detailed by remember(toolId){mutableStateOf(repository.pref("$toolId.view","quick")=="detailed")}
    var helpOpen by remember{mutableStateOf(false)}

    val mortarOptions=when(toolId){
        "splash"->MaterialEngine.splashPresets
        "screed"->MaterialEngine.screedPresets
        else->MaterialEngine.plasterPresets
    }
    var presetName by remember(toolId){mutableStateOf(repository.pref("$toolId.preset",mortarOptions.firstOrNull()?.name?:""))}
    val firstPreset=mortarOptions.firstOrNull()
    var cement by remember(toolId){mutableStateOf(repository.pref("$toolId.cement",firstPreset?.cement?.let(::fmt)?:"1"))}
    var sand by remember(toolId){mutableStateOf(repository.pref("$toolId.sand",firstPreset?.sand?.let(::fmt)?:"4"))}
    var thickness by remember(toolId){mutableStateOf(repository.pref("$toolId.thickness",firstPreset?.thicknessMm?.let(::fmt)?:"15"))}
    var thicknessUnit by remember(toolId){mutableStateOf(repository.pref("$toolId.thicknessUnit","مم"))}
    var waste by remember(toolId){mutableStateOf(repository.pref("$toolId.waste",firstPreset?.waste?.let(::fmt)?:"5"))}

    var wallThickness by remember{mutableStateOf(repository.pref("masonry.wall","12"))}
    var wallUnit by remember{mutableStateOf(repository.pref("masonry.wallUnit","سم"))}
    var brickL by remember{mutableStateOf(repository.pref("masonry.brickL","25"))}
    var brickW by remember{mutableStateOf(repository.pref("masonry.brickW","12"))}
    var brickH by remember{mutableStateOf(repository.pref("masonry.brickH","6"))}
    var brickUnit by remember{mutableStateOf(repository.pref("masonry.brickUnit","سم"))}
    var joint by remember{mutableStateOf(repository.pref("masonry.joint","10"))}
    var jointUnit by remember{mutableStateOf(repository.pref("masonry.jointUnit","مم"))}

    var tileW by remember{mutableStateOf(repository.pref("tile.w","60"))}
    var tileH by remember{mutableStateOf(repository.pref("tile.h","60"))}
    var tileUnit by remember{mutableStateOf(repository.pref("tile.unit","سم"))}
    var rate by remember(toolId){mutableStateOf(repository.pref("$toolId.rate",if(toolId=="waterproof")"1.5" else "5"))}
    var bag by remember{mutableStateOf(repository.pref("adhesive.bag","20"))}
    var coverage by remember{mutableStateOf(repository.pref("paint.coverage","10"))}
    var coats by remember(toolId){mutableStateOf(repository.pref("$toolId.coats","2"))}

    fun pref(k:String,v:String){repository.setPref(k,v)}

    fun applyPreset(name:String){
        val p=mortarOptions.firstOrNull{it.name==name}?:return
        presetName=p.name;cement=fmt(p.cement);sand=fmt(p.sand);thickness=fmt(p.thicknessMm);thicknessUnit="مم";waste=fmt(p.waste)
        pref("$toolId.preset",presetName);pref("$toolId.cement",cement);pref("$toolId.sand",sand)
        pref("$toolId.thickness",thickness);pref("$toolId.thicknessUnit",thicknessUnit);pref("$toolId.waste",waste)
    }
    fun resetDefaults(){
        when(toolId){
            "plaster","splash","screed"->mortarOptions.firstOrNull()?.let(::applyPreset)
            "masonry"->{
                wallThickness="12";wallUnit="سم";brickL="25";brickW="12";brickH="6";brickUnit="سم";joint="10";jointUnit="مم";waste="5"
                pref("masonry.wall",wallThickness);pref("masonry.wallUnit",wallUnit)
                pref("masonry.brickL",brickL);pref("masonry.brickW",brickW);pref("masonry.brickH",brickH);pref("masonry.brickUnit",brickUnit)
                pref("masonry.joint",joint);pref("masonry.jointUnit",jointUnit);pref("$toolId.waste",waste)
            }
            "tile"->{
                tileW="60";tileH="60";tileUnit="سم";waste="7"
                pref("tile.w",tileW);pref("tile.h",tileH);pref("tile.unit",tileUnit);pref("$toolId.waste",waste)
            }
            "adhesive"->{
                rate="5";bag="20";waste="5"
                pref("$toolId.rate",rate);pref("adhesive.bag",bag);pref("$toolId.waste",waste)
            }
            "paint"->{
                coverage="10";coats="2";waste="5"
                pref("paint.coverage",coverage);pref("$toolId.coats",coats);pref("$toolId.waste",waste)
            }
            "waterproof"->{
                rate="1.5";coats="2";waste="5"
                pref("$toolId.rate",rate);pref("$toolId.coats",coats);pref("$toolId.waste",waste)
            }
            "gypsum"->{
                waste="10";pref("$toolId.waste",waste)
            }
        }
    }


    val source=n(area)
    val result:MaterialResult?=if(source<=0)null else when(toolId){
        "plaster","splash","screed"->{
            val thickMm=lengthMeters(thickness,thicknessUnit)*1000.0
            MaterialEngine.mortar(
                toolId,title,source,
                MaterialEngine.MortarPreset(
                    name=if(presetName.isBlank())"خلطة مخصصة" else presetName,
                    cement=n(cement).takeIf{it>0}?:1.0,
                    sand=n(sand).takeIf{it>0}?:4.0,
                    thicknessMm=thickMm,
                    waste=n(waste)
                )
            )
        }
        "masonry"->MaterialEngine.masonry(
            source,
            lengthMeters(wallThickness,wallUnit),
            lengthMeters(brickL,brickUnit),
            lengthMeters(brickW,brickUnit),
            lengthMeters(brickH,brickUnit),
            lengthMeters(joint,jointUnit),
            n(waste)
        )
        "tile"->MaterialEngine.tile(source,n(waste),lengthMeters(tileW,tileUnit),lengthMeters(tileH,tileUnit))
        "adhesive"->MaterialEngine.adhesive(source,n(rate),n(bag).takeIf{it>0}?:20.0,n(waste))
        "paint"->MaterialEngine.paint(source,n(coverage).takeIf{it>0}?:10.0,n(coats).toInt().coerceAtLeast(1),n(waste))
        "waterproof"->MaterialEngine.waterproof(source,n(rate),n(coats).toInt().coerceAtLeast(1),n(waste))
        "gypsum"->MaterialEngine.gypsum(source,n(waste))
        else->null
    }

    val targets=buildList{
        if(activeProject!=null){
            add(SaveTarget("المشروع كله",null,null))
            activeProject.sections.forEach{s->
                add(SaveTarget(s.name,s.id,null))
                s.spaces.forEach{sp->add(SaveTarget("${s.name} ← ${sp.name}",s.id,sp.id))}
            }
        }
    }
    var targetLabel by remember(activeProject?.id){mutableStateOf(targets.firstOrNull()?.label?:"")}
    val target=targets.firstOrNull{it.label==targetLabel}

    Scaffold(
        topBar={
            TopAppBar(
                title={Column{Text(title,fontWeight=FontWeight.Black);Text("القيم الافتراضية واضحة وقابلة للتعديل",style=MaterialTheme.typography.labelSmall)}},
                navigationIcon={IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"رجوع")}},
                actions={IconButton(onClick={helpOpen=true}){Icon(Icons.Rounded.HelpOutline,"شرح")}}
            )
        }
    ){padding->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding=PaddingValues(12.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            item{
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    FilterChip(selected=!detailed,onClick={detailed=false;pref("$toolId.view","quick")},label={Text("سريع")},modifier=Modifier.weight(1f))
                    FilterChip(selected=detailed,onClick={detailed=true;pref("$toolId.view","detailed")},label={Text("تفصيلي")},modifier=Modifier.weight(1f))
                }
            }

            item{
                BoxCard{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text("دخل البيانات",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Black)
                        TextButton(onClick=::resetDefaults){
                            Icon(Icons.Rounded.RestartAlt,null,Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("رجّع القيم الأصلية")
                        }
                    }
                    NumberFieldX("المساحة",area,{area=it;pref("$toolId.area",it)},"م²",help="اكتب صافي مساحة البند بعد الخصومات، أو افتح الحاسبة من ملخص المشروع عشان تتعبى تلقائيًا.")

                    if(toolId in listOf("plaster","splash","screed")){
                        ChoiceFieldX("الخلطة الجاهزة",presetName,mortarOptions.map{it.name},{applyPreset(it)})
                        NumberUnitField(
                            "متوسط السمك",thickness,{thickness=it;pref("$toolId.thickness",it)},
                            thicknessUnit,{thicknessUnit=it;pref("$toolId.thicknessUnit",it)},
                            help="السمك المتوسط الفعلي، مش أكبر سمك في نقطة واحدة."
                        )
                        if(detailed){
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                NumberFieldX("أسمنت",cement,{cement=it;presetName="خلطة مخصصة";pref("$toolId.cement",it)},"جزء",Modifier.weight(1f))
                                NumberFieldX("رمل",sand,{sand=it;presetName="خلطة مخصصة";pref("$toolId.sand",it)},"جزء",Modifier.weight(1f))
                            }
                        }
                        NumberFieldX("الهالك",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="masonry"){
                        NumberUnitField("سمك الحائط",wallThickness,{wallThickness=it;pref("masonry.wall",it)},wallUnit,{wallUnit=it;pref("masonry.wallUnit",it)})
                        if(detailed){
                            Text("مقاس الطوبة",fontWeight=FontWeight.Black)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                NumberUnitField("الطول",brickL,{brickL=it;pref("masonry.brickL",it)},brickUnit,{brickUnit=it;pref("masonry.brickUnit",it)},Modifier.weight(1f))
                                NumberUnitField("العرض",brickW,{brickW=it;pref("masonry.brickW",it)},brickUnit,{brickUnit=it;pref("masonry.brickUnit",it)},Modifier.weight(1f))
                            }
                            NumberUnitField("الارتفاع",brickH,{brickH=it;pref("masonry.brickH",it)},brickUnit,{brickUnit=it;pref("masonry.brickUnit",it)})
                            NumberUnitField("سمك اللحام",joint,{joint=it;pref("masonry.joint",it)},jointUnit,{jointUnit=it;pref("masonry.jointUnit",it)})
                        }
                        NumberFieldX("هالك الطوب",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="tile"){
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            NumberUnitField("عرض البلاطة",tileW,{tileW=it;pref("tile.w",it)},tileUnit,{tileUnit=it;pref("tile.unit",it)},Modifier.weight(1f))
                            NumberUnitField("طول البلاطة",tileH,{tileH=it;pref("tile.h",it)},tileUnit,{tileUnit=it;pref("tile.unit",it)},Modifier.weight(1f))
                        }
                        NumberFieldX("هالك وقص",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="adhesive"){
                        NumberFieldX("معدل الاستهلاك",rate,{rate=it;pref("$toolId.rate",it)},"كجم/م²",help="قيمة بداية؛ نشرة المنتج هي المرجع.")
                        if(detailed)NumberFieldX("وزن الشيكارة",bag,{bag=it;pref("adhesive.bag",it)},"كجم")
                        NumberFieldX("هالك",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="paint"){
                        NumberFieldX("تغطية الدهان",coverage,{coverage=it;pref("paint.coverage",it)},"م²/لتر",help="خدها من عبوة المنتج لو متاحة.")
                        NumberFieldX("عدد الأوجه",coats,{coats=it;pref("$toolId.coats",it)},"وجه")
                        NumberFieldX("هالك",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="waterproof"){
                        NumberFieldX("استهلاك الوجه",rate,{rate=it;pref("$toolId.rate",it)},"كجم/م²",help="خد المعدل من نشرة المنتج.")
                        NumberFieldX("عدد الأوجه",coats,{coats=it;pref("$toolId.coats",it)},"وجه")
                        NumberFieldX("هالك",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }

                    if(toolId=="gypsum"){
                        Text("لوح البداية: 1.20 × 2.40 م",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        NumberFieldX("هالك وقص",waste,{waste=it;pref("$toolId.waste",it)},"%")
                    }
                }
            }

            if(result!=null){
                item{PageHeader("هتحتاج تقريبًا")}
                item{
                    BoxCard{
                        result.lines.forEachIndexed{i,line->MetricRow(line.label,line.value,i==0)}
                        HorizontalDivider()
                        Text(result.explanation,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if(activeProject!=null){
                    item{
                        BoxCard{
                            Text("احفظ النتيجة في المشروع",fontWeight=FontWeight.Black)
                            ChoiceFieldX("مكان الحفظ",targetLabel,targets.map{it.label},{targetLabel=it})
                            Button(
                                onClick={target?.let{onSave(result,it.sectionId,it.spaceId)}},
                                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
                            ){
                                Icon(Icons.Rounded.Save,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("ضيف للمشروع")
                            }
                        }
                    }
                }
            }
        }
    }

    if(helpOpen){
        AlertDialog(
            onDismissRequest={helpOpen=false},
            title={Text("عن الحاسبة",fontWeight=FontWeight.Black)},
            text={Text("القيم اللي بتفتح عليها الحاسبة هي قيم بداية عملية وليست مواصفة إجبارية. أي مواصفة مشروع أو نشرة منتج معتمدة هي المرجع، وكل القيم المهمة ظاهرة وقابلة للتعديل.")},
            confirmButton={TextButton(onClick={helpOpen=false}){Text("تمام")}}
        )
    }
}

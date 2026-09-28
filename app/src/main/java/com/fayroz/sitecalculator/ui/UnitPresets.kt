package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class LengthUnitOption(val label:String,val meters:Double)

val lengthUnits=listOf(
    LengthUnitOption("م",1.0),
    LengthUnitOption("سم",0.01),
    LengthUnitOption("مم",0.001)
)

fun lengthToMeters(value:String,unit:String):Double =
    n(value)*(lengthUnits.firstOrNull{it.label==unit}?.meters ?: 1.0)

fun metersToUnit(value:Double,unit:String):String {
    val factor=lengthUnits.firstOrNull{it.label==unit}?.meters ?: 1.0
    return fmt(value/factor)
}

@Composable
fun NumberWithUnitX(
    label:String,
    value:String,
    onValue:(String)->Unit,
    unit:String,
    onUnit:(String)->Unit,
    units:List<String> = listOf("م","سم","مم"),
    help:String?=null,
    modifier:Modifier=Modifier
){
    var unitOpen by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(3.dp)){
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Box{
            OutlinedTextField(
                value=value,
                onValueChange={raw->onValue(raw.filter{it.isDigit()||it=='.'||it==','||it=='٫'})},
                modifier=Modifier.fillMaxWidth(),
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                shape=RoundedCornerShape(11.dp),
                suffix={
                    TextButton(
                        onClick={unitOpen=true},
                        contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)
                    ){
                        Text(unit,style=MaterialTheme.typography.labelMedium)
                        Icon(Icons.Rounded.ExpandMore,null,Modifier.size(16.dp))
                    }
                },
                trailingIcon={if(help!=null){{CompactHelpIcon(label,help)}}else null}
            )
            DropdownMenu(expanded=unitOpen,onDismissRequest={unitOpen=false}){
                units.forEach{u->
                    DropdownMenuItem(text={Text(u)},onClick={unitOpen=false;onUnit(u)})
                }
            }
        }
    }
}

data class MortarPreset(
    val label:String,
    val cement:Double,
    val sand:Double,
    val thicknessMm:Double,
    val waste:Double,
    val dryFactor:Double,
    val note:String
)

fun mortarPresets(toolId:String):List<MortarPreset> = when(toolId){
    "plaster_materials"->listOf(
        MortarPreset("محارة داخلية عادية 1:4",1.0,4.0,15.0,5.0,1.33,"بداية عملية شائعة. راجع مواصفات المشروع لو محددة نسبة أو سمك مختلف."),
        MortarPreset("محارة أغنى 1:3",1.0,3.0,15.0,5.0,1.33,"خلطة أغنى بالأسمنت."),
        MortarPreset("محارة اقتصادية 1:5",1.0,5.0,15.0,5.0,1.33,"استخدمها فقط لو مواصفات المشروع تسمح.")
    )
    "splash_materials"->listOf(
        MortarPreset("طرطشة 1:2",1.0,2.0,5.0,10.0,1.33,"طرطشة ربط قبل المحارة. السمك قيمة بداية قابلة للتعديل."),
        MortarPreset("طرطشة غنية 1:1.5",1.0,1.5,5.0,10.0,1.33,"خلطة أغنى للأسطح اللي محتاجة تماسك أعلى.")
    )
    else->listOf(
        MortarPreset("تسوية أرضيات 1:4",1.0,4.0,50.0,5.0,1.33,"مونة تسوية تقليدية. السمك حسب المنسوب الفعلي."),
        MortarPreset("تسوية أقوى 1:3",1.0,3.0,50.0,5.0,1.33,"خلطة أغنى بالأسمنت.")
    )
}

data class SimpleMixPreset(
    val label:String,
    val cement:Double,
    val sand:Double,
    val aggregate:Double=0.0,
    val note:String=""
)

val masonryMixPresets=listOf(
    SimpleMixPreset("مونة مباني 1:5",1.0,5.0,note="بداية عملية شائعة للمباني. المواصفة المعتمدة للمشروع هي المرجع."),
    SimpleMixPreset("مونة أغنى 1:4",1.0,4.0,note="أسمنت أعلى."),
    SimpleMixPreset("مونة 1:6",1.0,6.0,note="استخدمها لو المواصفات تسمح.")
)

val concreteMixPresets=listOf(
    SimpleMixPreset("خرسانة بسيطة 1:2:4",1.0,2.0,4.0,"خلطة حجمية اسمية للحساب المبدئي فقط، وليست تصميم خلطة إنشائية."),
    SimpleMixPreset("خلطة أغنى 1:1.5:3",1.0,1.5,3.0,"خلطة اسمية أغنى. الخرسانة الإنشائية تتبع Mix Design معتمد.")
)

@Composable
fun ResetDefaultsButton(onClick:()->Unit){
    TextButton(onClick=onClick,modifier=Modifier.heightIn(min=40.dp)){
        Icon(Icons.Rounded.RestartAlt,null,Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text("رجّع القيم الأصلية")
    }
}

@Composable
fun PresetNote(text:String){
    Surface(
        shape=RoundedCornerShape(10.dp),
        color=MaterialTheme.colorScheme.secondaryContainer.copy(alpha=.55f)
    ){
        Text(
            text,
            Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=7.dp),
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

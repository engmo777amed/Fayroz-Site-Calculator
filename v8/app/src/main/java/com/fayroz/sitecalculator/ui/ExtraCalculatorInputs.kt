package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.domain.*

@Composable
fun ExtraCalculatorInputs(def:CalcDef,raw:Map<String,String>,set:(String,String)->Unit){
    @Composable
    fun choice(label:String,key:String,default:String,options:List<Pair<String,String>>){
        val current=raw[key]?:default
        ChoiceFieldX(label,options.firstOrNull{it.first==current}?.second?:options.first().second,options.map{it.second},{labelValue->set(key,options.first{it.second==labelValue}.first)})
    }
    when(def.id){
        "garage_ramp"->choice("طريقة حساب الرامب","_rampMode","slope",listOf("slope" to "الميل من فرق المنسوب والطول","length" to "الطول من فرق المنسوب والميل","drop" to "فرق المنسوب من الطول والميل","parts" to "رامب بانتقال علوي ورئيسي وسفلي"))
        "bar_schedule"->choice("شكل التفريدة","_shape","straight",listOf("straight" to "مستقيم","L" to "حرف L","U" to "حرف U","rectangle" to "مستطيل مع إضافة الثني المدخلة"))
        "cylindrical_tank"->choice("اتجاه الخزان","_tankOrientation","vertical",listOf("vertical" to "رأسي","horizontal" to "أفقي"))
        "concrete_compression","mortar_compression"->choice("شكل العينة","_specimen","cube",listOf("cube" to "مكعب أو وجه مستطيل","cylinder" to "أسطوانة"))
        "tile_layout"->choice("بداية شبكة البلاط","_tileStart","edge",listOf("edge" to "بداية من طرف","center" to "قطع طرفية متماثلة"))
        "slump"->choice("شكل الهبوط بالملاحظة","_slumpShape","true",listOf("true" to "هبوط حقيقي","shear" to "هبوط قص — راجع صلاحية الاختبار","collapse" to "انهيار — راجع صلاحية الاختبار"))
    }
    if(def.id in ExtraCalculators.ids)Text("قيم البداية للتجربة؛ عدّلها لقياساتك الفعلية قبل اعتماد النتيجة.",style=MaterialTheme.typography.labelSmall)
}

@Composable
fun ExtraCalculatorTable(def:CalcDef,raw:Map<String,String>,set:(String,String)->Unit){
    val schema=ExtraCalculators.tables[def.id]?:return
    val count=(raw["_rows"]?.toIntOrNull()?:schema.minimum).coerceIn(schema.minimum,schema.maximum)
    BoxCard{
        Text("قراءات الحساب",style=MaterialTheme.typography.titleMedium)
        Text("أدخل قراءاتك؛ الخانات الفارغة لا تُعتبر صفرًا. القيم تحفظ مع النتيجة.",style=MaterialTheme.typography.bodySmall)
        repeat(count){i->
            HorizontalDivider()
            Text("${if(def.id=="coordinate_area")"نقطة"else "صف"} ${i+1}")
            schema.columns.forEach{col->
                val key="_row.$i.${col.key}"
                NumberFieldX(col.label,raw[key].orEmpty(),{set(key,it)},col.unit)
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(enabled=count<schema.maximum,onClick={set("_rows",(count+1).toString())}){Text("إضافة صف")}
            TextButton(enabled=count>schema.minimum,onClick={set("_rows",(count-1).toString())}){Text("حذف آخر صف")}
        }
    }
}

fun diagramInputVisible(id:String,key:String,raw:Map<String,String>)=ExtraCalculators.activeField(id,key,raw)

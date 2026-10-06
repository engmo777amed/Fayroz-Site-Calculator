package com.fayroz.sitecalculator.domain
import com.fayroz.sitecalculator.core.*
object Validation {
 fun space(s:Space):String? {
    if(s.name.isBlank())return "اكتب اسم المكان."
    if(s.repeatCount<1)return "عدد التكرارات يبدأ من 1."
    if(listOf(s.length,s.width,s.height).any{!it.isFinite()||it<0})return "راجع أبعاد المكان."
    if(s.takeoffs.isEmpty())return "اختار بندًا واحدًا على الأقل."
    if(s.walls.any{it.length<=0||it.height<=0})return "راجع أطوال وارتفاعات الحوائط."
    if((s.floorSurfaces+s.ceilingSurfaces).any{it.length<=0||it.width<=0||it.deductionArea<0||it.deductionArea>it.length*it.width})return "راجع مقاسات وخصومات المسطحات."
    if(s.openings.any{it.width<=0||it.height<=0||it.sill<0||it.revealDepth<0||it.count<1})return "راجع مقاسات الفتحات وعددها."
    val walls=QuantityEngine.effectiveWalls(s)
    s.openings.forEach{o->
       val w=walls.firstOrNull{it.id==o.wallId}?:if(o.wallId==null)walls.firstOrNull()else null
       if(w==null)return "اربط الفتحات بالحوائط الحالية."
       if(o.width>w.length||o.sill+o.height>w.height)return "فتحة أكبر من الحائط المرتبطة به."
    }
    walls.forEachIndexed{i,w->
       val ops=s.openings.filter{it.wallId==w.id||(i==0&&it.wallId==null)}
       if(ops.sumOf{it.width*it.height*it.count}>w.length*w.height)return "إجمالي الفتحات أكبر من مساحة ${w.name}."
    }
    s.takeoffs.forEach{t->
       if(t.waste<0||t.adjustments.any{it.amount<0})return "الهالك والتعديلات لا يمكن أن تكون سالبة."
       if(t.parts.any{!it.length.isFinite()||!it.width.isFinite()||!it.deduction.isFinite()||it.deduction<0||(t.unit==UnitType.AREA&&(it.length<=0||it.width<=0))||QuantityEngine.partValue(it,t.unit)<=0})return "راجع الأجزاء المستقلة في ${t.name}."
       if(QuantityEngine.calculateOne(s,t).oneSpaceFinal<=0)return "لا توجد كمية صالحة لبند ${t.name}؛ أكمل المقاسات أو احذف البند."
       (listOfNotNull(t.material)+t.parts.mapNotNull{it.material}).forEach{m->
          MaterialReview.specError(m)?.let{return "${t.name}: $it"}
       }
    }
    return null
 }
}


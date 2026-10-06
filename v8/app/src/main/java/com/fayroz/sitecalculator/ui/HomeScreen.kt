package com.fayroz.sitecalculator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fayroz.sitecalculator.core.*

@Composable
fun HomeScreen(projects:List<Project>,active:ActiveLocation?,recentCalcs:List<SavedCalculation>,
    onContinue:()->Unit,onProjects:()->Unit,onToday:()->Unit,onTools:()->Unit,onNewRoom:()->Unit,
    onDirectItem:()->Unit,onSettings:()->Unit,onOpenSaved:(SavedCalculation)->Unit={},
    onSaved:()->Unit=onToday,onPrices:()->Unit=onSettings,onCreate:(String,String)->Unit={_,_->},
    onOpenPlace:(String,String,String)->Unit={_,_,_->}){
    val project=projects.firstOrNull{it.id==active?.projectId}
    var create by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Row(verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text("فيروز",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("حصر الموقع",style=MaterialTheme.typography.bodySmall)}
            TextButton(onClick=onPrices){Text("الأسعار")}
            IconButton(onClick=onSettings){Icon(Icons.Rounded.Settings,"الإعدادات")}
        }}
        if(project!=null)item{Surface(onClick=onContinue,shape=RoundedCornerShape(12.dp),color=FayrozNavy){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){Text(project.name,color=androidx.compose.ui.graphics.Color.White,fontWeight=FontWeight.Bold);Text("${project.sections.sumOf{it.spaces.size}} مكان • المشروع الحالي",color=FayrozTurquoise,style=MaterialTheme.typography.bodySmall)}
            Text("كمّل الحصر ←",color=FayrozTurquoise)
        }}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick=onTools,modifier=Modifier.weight(1f)){Text("حساب سريع")}
            OutlinedButton(onClick={create=true},modifier=Modifier.weight(1f)){Text("مشروع جديد")}
        }}
        if(project!=null){
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                TextButton(onClick=onNewRoom,modifier=Modifier.weight(1f)){Text("إضافة مكان")}
                TextButton(onClick=onDirectItem,modifier=Modifier.weight(1f)){Text("كمية جاهزة")}
            }}
            val places=project.sections.flatMap{s->s.spaces.map{s to it}}.sortedByDescending{it.second.updatedAt}.take(3)
            if(places.isNotEmpty())item{Text("آخر الأماكن",fontWeight=FontWeight.Bold)}
            items(places.size){i->val (section,space)=places[i];Surface(onClick={onOpenPlace(project.id,section.id,space.id)},shape=RoundedCornerShape(10.dp),tonalElevation=1.dp){Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(space.name,fontWeight=FontWeight.Bold);Text(section.name,style=MaterialTheme.typography.labelSmall)};Text("${space.takeoffs.size} بند",style=MaterialTheme.typography.bodySmall)}}}
        }else if(projects.isNotEmpty())item{TextButton(onClick=onProjects,modifier=Modifier.fillMaxWidth()){Text("اختيار مشروع لاستكمال الحصر")}}
        if(recentCalcs.isNotEmpty()){
            item{Text("آخر الحسابات",fontWeight=FontWeight.Bold)}
            items(recentCalcs.take(3).size){i->val c=recentCalcs[i];Surface(onClick={onOpenSaved(c)},shape=RoundedCornerShape(10.dp),tonalElevation=1.dp){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text(c.title,Modifier.weight(1f),maxLines=1,fontWeight=FontWeight.Bold);Text("فتح ←",style=MaterialTheme.typography.labelMedium)}}}
        }
    }
    if(create)NewProjectDialog(onDismiss={create=false},onCreate={name,type->create=false;onCreate(name,type)})
}

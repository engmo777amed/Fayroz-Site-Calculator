package com.fayroz.sitecalculator.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun StageNavigation(stage:Int,labels:List<String>,onSelect:(Int)->Unit){
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        labels.forEachIndexed{i,label->FilterChip(selected=stage==i,onClick={onSelect(i)},label={Text(label,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},modifier=Modifier.weight(1f))}
    }
}


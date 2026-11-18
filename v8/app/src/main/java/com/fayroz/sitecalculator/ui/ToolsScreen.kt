package com.fayroz.sitecalculator.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.fayroz.sitecalculator.core.*
import com.fayroz.sitecalculator.data.V8Repository
import com.fayroz.sitecalculator.domain.CalculatorLibrary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolDef(val id:String,val title:String,val subtitle:String,val icon:ImageVector)
val utilityTools=listOf(ToolDef("convert","تحويل الوحدات","طول ومساحة وحجم ووزن",Icons.Rounded.Calculate),ToolDef("area","تجميع مساحات","إضافة وخصم أجزاء",Icons.Rounded.Calculate))
val materialTools=CalculatorLibrary.all.map{ToolDef(it.id,it.title,it.group,Icons.Rounded.Calculate)}
private val toolsById = (utilityTools + materialTools).associateBy { it.id }
private val definitionsById = CalculatorLibrary.all.associateBy { it.id }
internal val familyTitles = mapOf(
    "rectangle" to "المساحات", "slope" to "الميول والمناسيب", "cutting" to "تقطيع الحديد",
    "plaster" to "المحارة وخلطات المونة", "raft" to "حجم الخرسانة", "form_column" to "الشدات",
    "wires" to "الأسلاك وتسعير الكهرباء", "reverse_mortar" to "معايا أسمنت",
    "flood_test" to "تسجيل قراءات الغمر",
)
private val descriptions = mapOf(
    "rectangle" to "مساحة مستطيل أو مثلث أو دائرة أو شبه منحرف بعد الخصم",
    "layer" to "حجم طبقة من مساحتها وسمكها",
    "coordinate_area" to "مساحة ومحيط مضلع من نقاط محلية مرتبة",
    "area" to "تجميع أجزاء المسطح وإدخال الخصومات",
    "diagonal" to "قطر المستطيل لمراجعة الأبعاد وتربيع الزوايا",
    "spacing" to "تقسيم طول إلى مسافات متساوية",
    "convert" to "تحويل وحدات الطول والمساحة والحجم والوزن",
    "slope" to "حل الميل أو فرق المنسوب من الأبعاد والقراءات",
    "floor_slope" to "فرق مناسيب الأرضية وتصريفها على طول المسار",
    "garage_ramp" to "حل طول الرامب أو الميل أو فرق المنسوب وعرض انتقالاته",
    "level_survey" to "مناسيب نقاط من مرجع وقراءة خلفية واحدة",
    "level_closure" to "خطأ غلق الميزانية وتوزيع التصحيح بالمسافة",
    "raft" to "حجم القاعدة أو الكمرة أو العمود أو البلاطة أو الحائط",
    "round_column" to "حجم أعمدة دائرية من قطرها وارتفاعها وعددها",
    "stairs_concrete" to "حجم بطنية القلبة والدرجات والبسطات المدخلة",
    "stair_layout" to "تقسيم القلبة وتحديد القوائم والنوايم ومنسوب النهاية",
    "variable_volume" to "حجم بين مقطعين مستطيلين تتغير أبعادهما تدريجيًا",
    "concrete_supply" to "تقدير عدد الحمولات وفاصلها وعدد الشاحنات اللازمة",
    "form_column" to "مساحة أوجه شدة عمود أو حائط أو كمرة",
    "steel" to "وزن السيخ وإجمالي الوزن المطلوب للشراء",
    "mesh" to "عدد وأطوال وأوزان حديد الشبكة في اتجاهين",
    "stirrups" to "أطوال وأوزان الكانات من أبعاد مركز السيخ",
    "bar_schedule" to "التفريد والوزن حسب الشكل وأبعاد الأضلاع",
    "cutting" to "توزيع أطوال متماثلة أو مختلفة على الأسياخ الخام",
    "plaster" to "خامات المحارة أو تغطية الأسمنت المتاح أو خلطة مخصصة وأوجه متكررة",
    "splash" to "خامات طبقة الطرطشة على المسطح",
    "screed" to "مونة طبقة التسوية حسب المساحة والسمك",
    "bedding" to "مونة فرشة التركيب وتغطيتها",
    "masonry" to "عدد الطوب الأحمر وتقدير المونة",
    "blocks" to "عدد البلوك وتقدير مونة المباني",
    "block_adhesive" to "كمية لاصق البلوك وعدد العبوات",
    "tile" to "عدد قطع البلاط وعبوات الشراء",
    "skirting" to "طول الوزرة والقطع اللازمة",
    "skirting_cut" to "إنتاج الوزرات من قص البلاطات",
    "stairs_finish" to "مسطحات كسوة الدرجات والقوائم",
    "adhesive" to "كمية لاصق البلاط من معدل الفرد",
    "grout" to "كمية حشو الفواصل حسب الشبكة والأبعاد",
    "tile_layout" to "توزيع قطع البلاط وفواصله في غرفة مستطيلة",
    "putty" to "استهلاك معجون الحوائط وعدد العبوات",
    "primer" to "استهلاك طبقة الأساس وفق التغطية",
    "paint" to "استهلاك الدهان وعدد العبوات حسب الأوجه",
    "woodpaint" to "كمية دهان الخشب من المساحة ومعدل التغطية",
    "waterproof" to "استهلاك مادة العزل وفق المساحة والأوجه",
    "rolls" to "كمية لفائف العزل مع حساب التراكب",
    "insulation" to "عدد ألواح العزل من المساحة والأبعاد",
    "sealant" to "حجم حشو الفاصل وطول التعبئة",
    "gypsum" to "كمية ألواح الجبس من المساحة والطبقات",
    "gypsum_system" to "تقدير مكونات نظام سقف الجبس من معدلاته",
    "light_cove" to "حصر طول بيت النور وخاماته",
    "excavation" to "حجم حفر قواعد أو خنادق مستطيلة",
    "fill" to "حجم الردم المنفذ والكمية الموردة",
    "earth_levels" to "أعمال ترابية من فروق المناسيب والمساحة",
    "sloped_excavation" to "حجم حفر بسعة عمل وجوانب مائلة",
    "section_volumes" to "حجوم حفر وردم بين محطات قطاعات متتالية",
    "haulage" to "رحلات النقل من الحجم والكثافة وحد الحمولة",
    "asphalt" to "حجم ووزن طبقة الأسفلت",
    "base_course" to "كمية طبقة الأساس حسب المساحة والسمك",
    "spray" to "كمية رش الطرق حسب المساحة ومعدل الرش",
    "kerb" to "طول البردورات وعدد القطع",
    "marking" to "كمية دهان تخطيط الطرق من المساحة والاستهلاك",
    "interlock" to "حصر بلاط الإنترلوك للشراء",
    "pipes" to "أطوال شراء المواسير والقطع القياسية",
    "fittings" to "حصر تكلفة قطع توصيل المواسير",
    "pipe_insulation" to "مسطح عزل المواسير من القطر والطول",
    "tank" to "سعة خزان مستطيل من أبعاده الداخلية",
    "cylindrical_tank" to "حجم السائل والفراغ في خزان دائري أفقي أو رأسي",
    "wires" to "أطوال الموصلات أو تسعير نقاط الإنارة والبرايز",
    "cables" to "طول الكابلات الصافي وطول الشراء",
    "conduits" to "أطوال مواسير الكهرباء وعدد العلب",
    "trays" to "حصر الحوامل والدعامات لمسار مستقيم",
    "soil_moisture" to "محتوى رطوبة التربة من الأوزان الرطبة والجافة",
    "sand_cone" to "حجم الحفرة والكثافة الحقلية بطريقة المخروط الرملي",
    "compaction" to "مقارنة الكثافة الجافة الحقلية بالمرجع المعملي",
    "proctor" to "الكثافات الجافة المقاسة حسب نقاط الرطوبة",
    "sieve" to "نسب المار التراكمي واتزان كتلة المناخل",
    "aggregate" to "امتصاص وكثافة نسبية للركام الخشن",
    "concrete_compression" to "مقاومة الضغط من حمل كسر العينة ومساحة التحميل",
    "slump" to "قياس الهبوط وتسجيل شكل العينة بعد رفع المخروط",
    "mortar_compression" to "مقاومة ضغط عينة المونة",
    "asphalt_core" to "كثافة كور الأسفلت بطريقة الإزاحة",
    "asphalt_voids" to "الكثافة النسبية القصوى ونسبة الفراغات",
    "flood_test" to "تسجيل عمق ماء الغمر والمدة وتغير القراءة",
    "reverse_coverage" to "مساحة التغطية الممكنة من خامة متاحة",
    "consumption_check" to "مقارنة الكمية المصروفة والمرتجع بالاستهلاك المتوقع",
)
private val categoryIcons = listOf(
    Icons.Rounded.Straighten, Icons.Rounded.ViewInAr, Icons.Rounded.Wallpaper,
    Icons.Rounded.FormatPaint, Icons.Rounded.Terrain, Icons.Rounded.ElectricalServices,
    Icons.Rounded.Science, Icons.Rounded.Inventory2,
)
private val iconById = toolCategories.flatMapIndexed { index, category ->
    category.sections.flatMap { it.ids }.map { it to categoryIcons[index] }
}.toMap()

internal fun familyTitle(id: String): String = familyTitles[id] ?: toolsById.getValue(id).title
internal fun familyModeTitle(id: String): String = when (id) {
    "rectangle" -> "مستطيل"
    "slope" -> "الميل"
    "cutting" -> "أطوال متماثلة"
    "plaster" -> "الخامات المطلوبة للمحارة"
    "raft" -> "بلاطة"
    "form_column" -> "عمود أو حائط"
    "form_beam" -> "كمرة"
    "wires" -> "أطوال الأسلاك"
    "points" -> "تسعير النقاط"
    "footing" -> "قاعدة"
    "beam" -> "كمرة"
    "column" -> "عمود"
    "wall_concrete" -> "حائط"
    else -> familyTitle(id)
}

@Composable
fun ToolsScreen(onOpen: (String) -> Unit, repository: V8Repository) {
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var globalSearch by rememberSaveable { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(repository.pref("favorites", "").split('|').filter(toolsById::containsKey).distinct()) }
    val favoriteName = "المفضلة"
    val currentSections = toolCategories.firstOrNull { it.title == selected }?.sections.orEmpty()
    val inFavorites = selected == favoriteName
    val isSearching = search.isNotBlank()
    val allSections = toolCategories.flatMap { it.sections }
    val sections = when {
        inFavorites && !(isSearching && globalSearch) -> listOf(ToolSection(favoriteName, favorites))
        selected != null && !(isSearching && globalSearch) -> currentSections
        else -> allSections
    }
    fun saveFavorites(ids: List<String>) {
        favorites = ids
        repository.setPref("favorites", ids.joinToString("|"))
    }
    fun goBack() {
        selected = null
        search = ""
        globalSearch = false
    }
    fun searchable(id: String): String = (toolFamilies[id] ?: listOf(id)).joinToString(" ") { mode ->
        val entry = toolsById.getValue(mode)
        listOf(familyTitle(mode), entry.title, descriptions[mode], definitionsById[mode]?.formula, toolAliases[mode])
            .filterNotNull().joinToString(" ")
    }
    BackHandler(enabled = selected != null || isSearching) { goBack() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageHeader(selected ?: "أقسام الحاسبات", "اختر قسمًا أو ابحث عن حاسبة") }
        if (selected != null) item { TextButton(onClick = ::goBack) { Text("رجوع للأقسام") } }
        item {
            TextFieldX("بحث في الحاسبات", search, { search = it }, placeholder = "الاسم أو الوظيفة")
            if (selected != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(globalSearch, { globalSearch = it })
                    Text("البحث في كل الحاسبات")
                }
            }
        }
        if (selected == null && !isSearching) {
            item {
                OutlinedButton(onClick = { selected = favoriteName }, modifier = Modifier.fillMaxWidth()) {
                    Text("المفضلة ★ (${favorites.size})")
                }
            }
            val recent = repository.pref("recent_tool_ids", "").split('|').filter(toolsById::containsKey).distinct().take(5)
            if (recent.isNotEmpty()) item {
                Text("آخر الحاسبات • حساب جديد", fontWeight = FontWeight.Bold)
                recent.forEach { id -> TextButton(onClick = { onOpen(id) }) { Text(familyTitle(id)) } }
            }
            toolCategories.chunked(2).forEachIndexed { row, pair ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEachIndexed { column, category ->
                            Surface(onClick = { selected = category.title }, modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                                shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(category.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Icon(categoryIcons[row * 2 + column], null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val matches = sections.map { section ->
                section.copy(ids = section.ids.filter { searchTool(search, searchable(it)) })
            }.filter { it.ids.isNotEmpty() }
            for (section in matches) {
                if (!inFavorites || globalSearch) item { Text(section.title, fontWeight = FontWeight.Bold) }
                section.ids.forEach { id ->
                    item {
                        val index = favorites.indexOf(id)
                        Surface(onClick = { onOpen(id) }, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(iconById[id] ?: Icons.Rounded.Calculate, null, tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f)) {
                                        Text(familyTitle(id), fontWeight = FontWeight.Bold)
                                        Text(descriptions[id] ?: definitionsById[id]?.formula ?: toolsById.getValue(id).subtitle,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = {
                                        saveFavorites(if (index >= 0) favorites - id else favorites + id)
                                    }) { Text(if (index >= 0) "★" else "☆") }
                                }
                                if (inFavorites && index >= 0) Row {
                                    TextButton(enabled = index > 0, onClick = {
                                        saveFavorites(listOf(id) + (favorites - id))
                                    }) { Text("تثبيت بالأعلى") }
                                    TextButton(enabled = index < favorites.lastIndex, onClick = {
                                        saveFavorites(favorites.toMutableList().apply { add(index + 1, removeAt(index)) })
                                    }) { Text("لأسفل") }
                                }
                            }
                        }
                    }
                }
            }
            if (matches.isEmpty()) item { EmptyState("مفيش نتائج", "اختار قسمًا آخر أو غير البحث.") }
        }
    }
}

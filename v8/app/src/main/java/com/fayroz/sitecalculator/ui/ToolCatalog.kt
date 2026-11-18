package com.fayroz.sitecalculator.ui

import java.text.Normalizer
import java.util.Locale

internal data class ToolCategory(val title: String, val sections: List<ToolSection>)
internal data class ToolSection(val title: String, val ids: List<String>)

private fun section(title: String, ids: String) = ToolSection(title, ids.split(' '))
private fun category(title: String, vararg sections: ToolSection) = ToolCategory(title, sections.toList())

internal val toolCategories = listOf(
    category("القياسات والمناسيب",
        section("المساحات والأحجام", "rectangle layer coordinate_area area"),
        section("القياس والتحويل", "diagonal spacing convert"),
        section("الميول والمناسيب", "slope floor_slope garage_ramp level_survey level_closure")),
    category("الخرسانة والحديد",
        section("الخرسانة والصب", "raft round_column stairs_concrete stair_layout variable_volume concrete_supply"),
        section("الشدات", "form_column form_beam"),
        section("الحديد والتفريدة", "steel mesh stirrups bar_schedule cutting")),
    category("المونة والمباني",
        section("المونة والبياض", "plaster splash screed bedding"),
        section("المباني", "masonry blocks block_adhesive")),
    category("التشطيبات والعزل",
        section("الأرضيات والكسوات", "tile skirting skirting_cut stairs_finish adhesive grout tile_layout"),
        section("الدهانات", "putty primer paint woodpaint"),
        section("العزل والفواصل", "waterproof rolls insulation sealant"),
        section("الجبس والأسقف", "gypsum gypsum_system light_cove")),
    category("الحفر والطرق",
        section("الحفر والردم والنقل", "excavation fill earth_levels sloped_excavation section_volumes haulage"),
        section("الطرق والرصف", "asphalt base_course spray kerb marking interlock")),
    category("السباكة والكهرباء",
        section("السباكة والخزانات", "pipes fittings pipe_insulation tank cylindrical_tank"),
        section("الكهرباء", "wires cables conduits trays")),
    category("اختبارات الموقع",
        section("التربة والركام", "soil_moisture sand_cone compaction proctor sieve aggregate"),
        section("الخرسانة والمونة", "concrete_compression slump mortar_compression"),
        section("الأسفلت", "asphalt_core asphalt_voids"),
        section("العزل", "flood_test")),
    category("الخامات المتاحة والاستهلاك",
        section("التغطية والاستهلاك", "reverse_coverage consumption_check")),
)

internal val toolFamilies = mapOf(
    "rectangle" to listOf("rectangle", "triangle", "circle", "trapezoid"),
    "slope" to listOf("slope", "levels"),
    "cutting" to listOf("cutting", "mixed_cutting"),
    "plaster" to listOf("plaster", "reverse_mortar", "custom_mortar", "faces"),
    "raft" to listOf("raft", "footing", "beam", "column", "wall_concrete"),
    "form_column" to listOf("form_column", "form_beam"),
    "wires" to listOf("wires", "points"),
)

internal val toolAliases = mapOf(
    "garage_ramp" to "رامب جراج منحدر",
    "level_survey" to "ميزان ليزر روبير",
    "level_closure" to "ميزان ليزر روبير",
    "blocks" to "طوب بلوك مباني",
    "masonry" to "طوب بلوك",
    "stirrups" to "كانة كانات",
    "steel" to "أسياخ حديد",
    "cutting" to "أسياخ حديد",
    "slump" to "هبوط Slump",
    "sand_cone" to "مخروط رملي Sand cone",
    "proctor" to "بروكتور Proctor",
    "asphalt_voids" to "Gmm Gmb",
)

private val marks = Regex("[\\p{M}ـ]")
private val spaces = Regex("[^\\p{L}\\p{N}]+")
internal fun normalizeToolSearch(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(marks, "")
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace(spaces, " ")
        .trim()

internal fun searchTool(query: String, text: String): Boolean {
    val haystack = normalizeToolSearch(text)
    return normalizeToolSearch(query).split(' ').all { it in haystack }
}

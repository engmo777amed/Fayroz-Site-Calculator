package com.fayroz.sitecalculator

import com.fayroz.sitecalculator.core.Project
import com.fayroz.sitecalculator.core.SavedCalculation
import com.fayroz.sitecalculator.data.V8Codec
import com.fayroz.sitecalculator.domain.*
import com.fayroz.sitecalculator.ui.normalizeToolSearch
import com.fayroz.sitecalculator.ui.searchTool
import com.fayroz.sitecalculator.ui.toolCategories
import com.fayroz.sitecalculator.ui.toolFamilies
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class CalculatorContinuationTest {
    @Test fun deletingAndMovingIndexedRowsKeepsOnlyActiveValues() {
        val schema = ExtraCalculators.tables.getValue("mixed_cutting")
        val input = mutableMapOf(
            "_rows" to "3", "_row.0.piece" to "5", "_row.0.count" to "2",
            "_row.1.piece" to "4", "_row.1.count" to "3",
            "_row.2.piece" to "2", "_row.2.count" to "1", "_row.3.piece" to "9",
            "stock" to "12",
        )
        val rows = CalculatorRows(schema, input)
        rows.delete(1)
        rows.move(1, 0)
        rows.add()
        assertEquals("3", input["_rows"])
        assertEquals("2", input["_row.0.piece"])
        assertEquals("1", input["_row.0.count"])
        assertEquals("5", input["_row.1.piece"])
        assertEquals("2", input["_row.1.count"])
        assertFalse(input.containsKey("_row.2.piece"))
        assertFalse(input.containsKey("_row.3.piece"))
        assertEquals("12", input["stock"])
        val project = Project(name = "Rows", calculations = listOf(
            SavedCalculation(toolId = "mixed_cutting", title = "Cut", summary = "", inputs = input)
        ))
        val restored = V8Codec.decodeProjects(V8Codec.encodeProjects(listOf(project))).single()
            .calculations.single().inputs.toMutableMap()
        assertEquals(input, restored)
        CalculatorRows(schema, restored).delete(2)
        assertEquals(listOf(2.0, 5.0), ExtraCalculators.tableValues("mixed_cutting", restored).map { it.getValue("piece") })
    }

    @Test fun catalogIncludesAllDefinitionsOrTheirHistoricalEntries() {
        val ids = toolCategories.flatMap { it.sections }.flatMap { it.ids }
        assertEquals(8, toolCategories.size)
        assertEquals(ids.size, ids.toSet().size)
        val represented = ids.flatMap { toolFamilies[it] ?: listOf(it) }.toSet()
        assertEquals(CalculatorLibrary.all.map { it.id }.toSet() - "loads", represented - setOf("convert", "area"))
        assertEquals("اضاءه", normalizeToolSearch("إِضَـاءَة"))
        assertTrue(searchTool("sand CONE", "المخروط الرملي Sand cone"))
        assertTrue(searchTool("الرّامب", "ميل الرامب مدخل الجراج"))
    }

    private fun scene(id: String, vararg values: Pair<String, String>): Pair<CalculatorSolid, CalcAnswer> {
        val def = CalculatorLibrary.all.first { it.id == id }
        val raw = def.fields.associate { it.key to it.default } + values
        val answer = CalculatorLibrary.evaluate(def, raw)
        return requireNotNull(CalculatorGeometry.create(id, raw, answer)) to answer
    }

    @Test fun slabUsesInputDimensionsAndActualCalculatedVolume() {
        val (scene, answer) = scene("raft", "length" to "2", "width" to "2", "height" to ".5",
            "count" to "1", "waste" to "0", "price" to "0")
        val points = scene.faces.flatMap { it.points }
        assertEquals(6, scene.faces.size)
        val measured = points.maxOf { it.x } * points.maxOf { it.y } * points.maxOf { it.z }
        assertEquals(2.0, measured, 1e-12)
        assertEquals(measured, answer.outputs.first().value, 1e-12)
    }

    @Test fun rampSolidHasParallelSurfacesAndCorrectDrop() {
        val (scene, answer) = scene("garage_ramp", "rampWidth" to "3", "rampThickness" to "20")
        val points = scene.faces.flatMap { it.points }.distinct()
        assertEquals(8, points.size)
        val start = points.filter { it.x == 0.0 }.map { it.z }.distinct().sorted()
        val end = points.filter { it.x == 10.0 }.map { it.z }.distinct().sorted()
        assertEquals(.2, start.last() - start.first(), 1e-12)
        assertEquals(.2, end.last() - end.first(), 1e-12)
        assertEquals(answer.outputs.first().value, start.last() - end.last(), 1e-12)
        assertEquals(3.0, points.maxOf { it.y }, 1e-12)
    }

    @Test fun horizontalHalfFilledTankHasLiquidAtCenter() {
        val (scene, answer) = scene("cylindrical_tank", "_tankOrientation" to "horizontal")
        val water = scene.faces.filter { it.surface == Surface3.Water }.flatMap { it.points }
        assertEquals(0.0, water.maxOf { it.z }, 1e-12)
        assertEquals(-1.0, water.minOf { it.z }, 1e-12)
        assertEquals(4.0, water.maxOf { it.x }, 1e-12)
        assertEquals(2 * PI, answer.outputs.first { it.label == "حجم السائل" }.value, 1e-12)
    }
    @Test fun stairGeometryUsesMeasuredTreadAndRiser() {
        val (solid, answer)=scene("stairs_concrete","width" to "1.1","run" to "1.2","rise" to ".68",
            "steps" to "4","tread" to "30","riser" to "17","waist" to "15")
        val points=solid.faces.filter{it.part.startsWith("درجة")}.flatMap{it.points}
        assertEquals(1.2,points.maxOf{it.x},1e-12)
        assertEquals(1.1,points.maxOf{it.y},1e-12)
        assertEquals(.68,points.maxOf{it.z},1e-12)
        assertEquals(.5*.30*.17*1.1*4,answer.outputs.first{it.label=="درجات"}.value,1e-12)
    }

    @Test fun insulationBoardDimensionsAreInMeters() {
        val (solid,_)=scene("insulation","area" to "12","boardL" to "1.2","boardW" to ".6","thickness" to "5")
        val points=solid.faces.flatMap{it.points}
        assertEquals(1.2,points.maxOf{it.x},1e-12)
        assertEquals(.6,points.maxOf{it.y},1e-12)
        assertEquals(.05,points.maxOf{it.z},1e-12)
    }

    @Test fun meshContainsOneSolidRodForEveryBarInSmallGrid() {
        val (solid,answer)=scene("mesh","length" to "2","width" to "1","cover" to "0","spacing" to "50",
            "diameter" to "12","spacingY" to "50","diameterY" to "10","layers" to "1")
        val lengths=answer.outputs.first{it.label=="أسياخ اتجاه الطول للشبكة"}.value
        val widths=answer.outputs.first{it.label=="أسياخ اتجاه العرض للشبكة"}.value
        assertEquals(((lengths+widths)*14).toInt(),solid.faces.size)
        assertEquals(2.0,solid.faces.filter{it.part=="اتجاه الطول"}.flatMap{it.points}.maxOf{it.x},1e-12)
    }
}

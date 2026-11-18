package com.fayroz.sitecalculator.domain

/** Edits indexed table entries using the same keys as existing drafts and saved results. */
internal class CalculatorRows(
    private val schema: ExtraCalculators.Table,
    private val inputs: MutableMap<String, String>,
) {
    val count: Int
        get() = (inputs["_rows"]?.toIntOrNull() ?: schema.minimum)
            .coerceIn(schema.minimum, schema.maximum)

    fun add() {
        if (count < schema.maximum) replace(readRows() + listOf(emptyMap()))
    }

    fun delete(index: Int) {
        if (count > schema.minimum && index in 0 until count) {
            replace(readRows().toMutableList().apply { removeAt(index) })
        }
    }

    fun move(from: Int, to: Int) {
        if (from in 0 until count && to in 0 until count && from != to) {
            replace(readRows().toMutableList().apply { add(to, removeAt(from)) })
        }
    }

    private fun readRows(): List<Map<String, String>> = List(count) { index ->
        buildMap {
            for (column in schema.columns) {
                inputs["_row.$index.${column.key}"]?.let { put(column.key, it) }
            }
        }
    }

    private fun replace(rows: List<Map<String, String>>) {
        // Also discard stale entries left by older releases that only decremented _rows.
        inputs.keys.filter { it.startsWith("_row.") }.forEach { inputs.remove(it) }
        rows.forEachIndexed { index, row ->
            row.forEach { (key, value) -> inputs["_row.$index.$key"] = value }
        }
        inputs["_rows"] = rows.size.toString()
    }
}

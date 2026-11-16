package com.rolando.komori

import com.rolando.komori.data.Converters
import com.rolando.komori.data.Subtask
import com.rolando.komori.data.TaskPriority
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testPriorityConversion() {
        assertEquals("HIGH", converters.fromPriority(TaskPriority.HIGH))
        assertEquals("MEDIUM", converters.fromPriority(TaskPriority.MEDIUM))
        assertEquals("LOW", converters.fromPriority(TaskPriority.LOW))

        assertEquals(TaskPriority.HIGH, converters.toPriority("HIGH"))
        assertEquals(TaskPriority.LOW, converters.toPriority("LOW"))
        // Fallback for unknown value
        assertEquals(TaskPriority.MEDIUM, converters.toPriority("UNKNOWN"))
    }

    @Test
    fun testSubtaskListConversion() {
        val subtasks = listOf(
            Subtask(title = "Item 1", isCompleted = false),
            Subtask(title = "Item 2", isCompleted = true)
        )
        val json = converters.fromSubtaskList(subtasks)
        val decoded = converters.toSubtaskList(json)

        assertEquals(2, decoded.size)
        assertEquals("Item 1", decoded[0].title)
        assertFalse(decoded[0].isCompleted)
        assertEquals("Item 2", decoded[1].title)
        assertTrue(decoded[1].isCompleted)
    }

    @Test
    fun testIntSetConversion() {
        val set = setOf(0, 2, 4, 6)
        val serialized = converters.fromIntSet(set)
        val deserialized = converters.toIntSet(serialized)
        assertEquals(set, deserialized)

        assertEquals(emptySet<Int>(), converters.toIntSet(null))
        assertEquals(emptySet<Int>(), converters.toIntSet(""))
    }

    @Test
    fun testStringSetConversion() {
        val dates = setOf("2026-03-28", "2026-03-29", "2026-03-30")
        val serialized = converters.fromStringSet(dates)
        val deserialized = converters.toStringSet(serialized)
        assertEquals(dates, deserialized)

        assertEquals(emptySet<String>(), converters.toStringSet(null))
        assertEquals(emptySet<String>(), converters.toStringSet(""))
    }

    @Test
    fun testLocalDateConversion() {
        val date = LocalDate.of(2026, 3, 30)
        val epochDay = converters.fromLocalDate(date)
        val reconstructed = converters.toLocalDate(epochDay)
        assertEquals(date, reconstructed)
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
    }
}

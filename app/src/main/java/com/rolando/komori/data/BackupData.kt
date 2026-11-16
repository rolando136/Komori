package com.rolando.komori.data

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class KomoriBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val tasks: List<Task> = emptyList(),
    val weeklyEvents: List<WeeklyEvent> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val notes: List<Note> = emptyList()
)

data class ImportSummary(
    val tasksCount: Int,
    val eventsCount: Int,
    val habitsCount: Int,
    val notesCount: Int
)

object BackupManager {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, object : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
            override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.toString())
            }

            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? {
                val str = json?.asString ?: return null
                return try {
                    LocalDate.parse(str)
                } catch (e: Exception) {
                    null
                }
            }
        })
        .registerTypeAdapter(LocalTime::class.java, object : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
            override fun serialize(src: LocalTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_TIME))
            }

            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalTime? {
                val str = json?.asString ?: return null
                return try {
                    LocalTime.parse(str)
                } catch (e: Exception) {
                    null
                }
            }
        })
        .setPrettyPrinting()
        .create()

    fun toJson(backup: KomoriBackup): String {
        return gson.toJson(backup)
    }

    fun fromJson(json: String): KomoriBackup? {
        return try {
            gson.fromJson(json, KomoriBackup::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("isDone", isDone)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChecklistItem {
            return ChecklistItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                text = json.optString("text", ""),
                isDone = json.optBoolean("isDone", false)
            )
        }

        fun listToJson(items: List<ChecklistItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String?): List<ChecklistItem> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<ChecklistItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

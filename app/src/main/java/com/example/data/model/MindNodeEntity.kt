package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "mind_nodes",
    indices = [
        Index("mapId"),
        Index("parentId"),
        Index("syncMasterId")
    ]
)
data class MindNodeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val mapId: Long,
    val parentId: String? = null, // null indicates root node
    val syncMasterId: String? = null, // If set, points to the shared sync group/master node for live synchronized twins
    val title: String,
    val notes: String = "",
    val checklistJson: String = "",
    val linkUrl: String = "",
    val colorHex: String = "#3B82F6",
    val iconName: String = "lightbulb",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isSyncTwin: Boolean
        get() = !syncMasterId.isNullOrBlank()

    val checklist: List<ChecklistItem>
        get() = ChecklistItem.listFromJson(checklistJson)
}

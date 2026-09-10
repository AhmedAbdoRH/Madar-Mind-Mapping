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
        Index("syncMasterId"),
        Index(value = ["mapId", "orderIndex", "createdAt"]),
        Index(value = ["mapId", "parentId", "orderIndex", "createdAt"])
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
    val imageUri: String? = null,
    val progress: Int? = null, // Progress level from 0 to 10 (null = progress disabled/hidden)
    val impact: Int? = null, // Impact power level from 1 to 5 (null = impact disabled/hidden)
    val dueDate: Long? = null, // Epoch timestamp in ms for due date / deadline reminder (null = no due date)
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isSyncTwin: Boolean
        get() = !syncMasterId.isNullOrBlank()

    val checklist: List<ChecklistItem>
        get() = ChecklistItem.listFromJson(checklistJson)

    val hasProgress: Boolean
        get() = progress != null && progress in 0..10

    val hasImpact: Boolean
        get() = impact != null && impact in 1..5

    val hasDueDate: Boolean
        get() = dueDate != null && dueDate > 0L
}

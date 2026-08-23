package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MindNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindNodeDao {
    @Query("SELECT * FROM mind_nodes WHERE mapId = :mapId ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllNodesForMap(mapId: Long): Flow<List<MindNodeEntity>>

    @Query("SELECT * FROM mind_nodes WHERE mapId = :mapId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getAllNodesForMapSync(mapId: Long): List<MindNodeEntity>

    @Query("SELECT * FROM mind_nodes WHERE id = :id LIMIT 1")
    fun getNodeById(id: String): Flow<MindNodeEntity?>

    @Query("SELECT * FROM mind_nodes WHERE id = :id LIMIT 1")
    suspend fun getNodeByIdSync(id: String): MindNodeEntity?

    @Query("SELECT * FROM mind_nodes WHERE mapId = :mapId AND parentId = :parentId ORDER BY orderIndex ASC, createdAt ASC")
    fun getChildrenOf(mapId: Long, parentId: String): Flow<List<MindNodeEntity>>

    @Query("SELECT * FROM mind_nodes WHERE mapId = :mapId AND parentId IS NULL LIMIT 1")
    suspend fun getRootNodeSync(mapId: Long): MindNodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: MindNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<MindNodeEntity>)

    @Update
    suspend fun updateNode(node: MindNodeEntity)

    @Query("DELETE FROM mind_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: String)

    @Query("DELETE FROM mind_nodes WHERE id IN (:ids)")
    suspend fun deleteNodesByIds(ids: List<String>)

    @Query("DELETE FROM mind_nodes WHERE mapId = :mapId")
    suspend fun deleteAllNodesForMap(mapId: Long)
}

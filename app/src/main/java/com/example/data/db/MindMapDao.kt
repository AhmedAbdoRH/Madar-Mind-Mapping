package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MindMapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MindMapDao {
    @Query("SELECT * FROM mind_maps ORDER BY updatedAt DESC")
    fun getAllMaps(): Flow<List<MindMapEntity>>

    @Query("SELECT * FROM mind_maps WHERE id = :id LIMIT 1")
    fun getMapById(id: Long): Flow<MindMapEntity?>

    @Query("SELECT * FROM mind_maps WHERE id = :id LIMIT 1")
    suspend fun getMapByIdSync(id: Long): MindMapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: MindMapEntity): Long

    @Update
    suspend fun updateMap(map: MindMapEntity)

    @Query("DELETE FROM mind_maps WHERE id = :id")
    suspend fun deleteMap(id: Long)
}

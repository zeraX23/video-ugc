package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UgcDraftDao {
    @Query("SELECT * FROM ugc_drafts ORDER BY createdAt DESC")
    fun getAllDrafts(): Flow<List<UgcDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: UgcDraft)

    @Delete
    suspend fun deleteDraft(draft: UgcDraft)

    @Query("DELETE FROM ugc_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}

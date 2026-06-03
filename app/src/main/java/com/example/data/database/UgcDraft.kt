package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ugc_drafts")
data class UgcDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val style: String,
    val prompt: String,
    val generationJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

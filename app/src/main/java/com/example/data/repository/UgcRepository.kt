package com.example.data.repository

import com.example.data.database.UgcDraft
import com.example.data.database.UgcDraftDao
import kotlinx.coroutines.flow.Flow

class UgcRepository(private val ugcDraftDao: UgcDraftDao) {

    val allDrafts: Flow<List<UgcDraft>> = ugcDraftDao.getAllDrafts()

    suspend fun insertDraft(draft: UgcDraft) {
        ugcDraftDao.insertDraft(draft)
    }

    suspend fun deleteDraft(draft: UgcDraft) {
        ugcDraftDao.deleteDraft(draft)
    }

    suspend fun deleteDraftById(id: Int) {
        ugcDraftDao.deleteDraftById(id)
    }
}

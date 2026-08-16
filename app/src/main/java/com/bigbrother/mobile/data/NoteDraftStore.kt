package com.bigbrother.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.noteDraftDataStore by preferencesDataStore("note_drafts")

class NoteDraftStore(private val context: Context) {
    private val ds = context.noteDraftDataStore

    suspend fun hasDraft(recordId: String): Boolean =
        ds.data.first()[booleanPreferencesKey(existsKey(recordId))] ?: false

    suspend fun loadText(recordId: String): String =
        ds.data.first()[stringPreferencesKey(textKey(recordId))] ?: ""

    suspend fun loadImages(recordId: String): List<String> {
        val raw = ds.data.first()[stringPreferencesKey(imagesKey(recordId))] ?: ""
        return raw.split(',').filter { it.isNotBlank() }
    }

    suspend fun save(recordId: String, text: String, imageNames: List<String>) {
        ds.edit { pref ->
            pref[booleanPreferencesKey(existsKey(recordId))] = true
            pref[stringPreferencesKey(textKey(recordId))] = text
            pref[stringPreferencesKey(imagesKey(recordId))] = imageNames.joinToString(",")
        }
    }

    suspend fun clear(recordId: String) {
        ds.edit { pref ->
            pref.remove(booleanPreferencesKey(existsKey(recordId)))
            pref.remove(stringPreferencesKey(textKey(recordId)))
            pref.remove(stringPreferencesKey(imagesKey(recordId)))
        }
    }

    suspend fun clearAll() {
        ds.edit { pref ->
            pref.asMap().keys.forEach { pref.remove(it) }
        }
    }

    suspend fun allDraftRecordIds(): List<String> {
        val prefix = "note_draft_exists_"
        return ds.data.first().asMap().keys
            .map { it.name }
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
    }

    private fun existsKey(recordId: String) = "note_draft_exists_$recordId"
    private fun textKey(recordId: String) = "note_draft_text_$recordId"
    private fun imagesKey(recordId: String) = "note_draft_images_$recordId"
}

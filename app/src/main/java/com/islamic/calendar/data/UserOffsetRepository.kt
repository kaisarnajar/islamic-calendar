package com.islamic.calendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userOffsetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_offset",
)

class UserOffsetRepository(private val context: Context) {

    val offsetDays: Flow<Int> = context.userOffsetDataStore.data.map { prefs ->
        prefs[KEY_OFFSET] ?: 0
    }

    suspend fun setOffsetDays(value: Int) {
        context.userOffsetDataStore.edit { prefs ->
            prefs[KEY_OFFSET] = value
        }
    }

    companion object {
        private val KEY_OFFSET = intPreferencesKey("hijri_offset_days")
    }
}

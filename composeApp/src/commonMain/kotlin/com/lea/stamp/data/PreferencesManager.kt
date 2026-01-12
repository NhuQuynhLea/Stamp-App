package com.lea.stamp.data

import kotlinx.coroutines.flow.StateFlow

interface PreferencesManager {
    fun getGeminiApiKey(): StateFlow<String?>
    suspend fun setGeminiApiKey(apiKey: String)
    suspend fun loadApiKey()
}

expect class PreferencesManagerImpl() : PreferencesManager

object PreferencesProvider {
    val instance: PreferencesManager by lazy { PreferencesManagerImpl() }
}

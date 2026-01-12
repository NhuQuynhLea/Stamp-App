package com.lea.stamp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

actual class PreferencesManagerImpl : PreferencesManager {
    private val _geminiApiKey = MutableStateFlow<String?>(null)
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    init {
        _geminiApiKey.value = userDefaults.stringForKey(KEY_GEMINI_API)
    }
    
    override fun getGeminiApiKey(): StateFlow<String?> = _geminiApiKey.asStateFlow()
    
    override suspend fun setGeminiApiKey(apiKey: String) {
        userDefaults.setObject(apiKey, KEY_GEMINI_API)
        userDefaults.synchronize()
        _geminiApiKey.value = apiKey
    }
    
    override suspend fun loadApiKey() {
        _geminiApiKey.value = userDefaults.stringForKey(KEY_GEMINI_API)
    }
    
    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}

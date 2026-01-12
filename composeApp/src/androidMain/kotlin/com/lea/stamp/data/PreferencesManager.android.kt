package com.lea.stamp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private var appContext: Context? = null

fun initPreferences(context: Context) {
    appContext = context.applicationContext
}

actual class PreferencesManagerImpl : PreferencesManager {
    private val _geminiApiKey = MutableStateFlow<String?>(null)
    
    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("stamp_prefs", Context.MODE_PRIVATE)
    }
    
    init {
        _geminiApiKey.value = prefs?.getString(KEY_GEMINI_API, null)
    }
    
    override fun getGeminiApiKey(): StateFlow<String?> = _geminiApiKey.asStateFlow()
    
    override suspend fun setGeminiApiKey(apiKey: String) {
        prefs?.edit()?.putString(KEY_GEMINI_API, apiKey)?.apply()
        _geminiApiKey.value = apiKey
    }
    
    override suspend fun loadApiKey() {
        _geminiApiKey.value = prefs?.getString(KEY_GEMINI_API, null)
    }
    
    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }
}

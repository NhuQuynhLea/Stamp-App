package com.lea.stamp.data.gemini

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import io.ktor.util.*

sealed class GeminiResult<out T> {
    data class Success<T>(val data: T) : GeminiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : GeminiResult<Nothing>()
}

interface GeminiClient {
    suspend fun generateContent(
        prompt: String,
        imageData: ByteArray? = null,
        apiKey: String,
        model: String = DEFAULT_MODEL
    ): GeminiResult<String>
    
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-pro"
    }
}

class GeminiClientImpl(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 120000
        }
    }
) : GeminiClient {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun generateContent(
        prompt: String,
        imageData: ByteArray?,
        apiKey: String,
        model: String
    ): GeminiResult<String> {
        return try {
            if (apiKey.isBlank()) {
                println("GeminiClient: API key is not configured")
                return GeminiResult.Error("API key is not configured")
            }
            
            println("GeminiClient: Generating content with model: $model")
            
            val requestBody = buildRequest(prompt, imageData)
            
            val response: HttpResponse = httpClient.post {
                url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            println("GeminiClient: Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("GeminiClient: Response received, length: ${responseText.length}")
                
                val extractedText = extractTextFromResponse(responseText)
                GeminiResult.Success(extractedText)
            } else {
                val errorBody = response.bodyAsText()
                println("GeminiClient: Error response: $errorBody")
                GeminiResult.Error("API request failed: ${response.status}")
            }
        } catch (e: Exception) {
            println("GeminiClient: Exception: ${e.message}")
            e.printStackTrace()
            GeminiResult.Error("Request failed: ${e.message}", e)
        }
    }
    
    private fun buildRequest(prompt: String, imageData: ByteArray?): String {
        val parts = mutableListOf<String>()
        
        if (imageData != null) {
            val base64Image = imageData.encodeBase64()
            parts.add("""
                {
                    "inline_data": {
                        "mime_type": "image/jpeg",
                        "data": "$base64Image"
                    }
                }
            """.trimIndent())
        }
        
        parts.add("""
            {
                "text": "${prompt.replace("\n", "\\n").replace("\"", "\\\"")}"
            }
        """.trimIndent())
        
        return """
        {
            "contents": [{
                "parts": [${parts.joinToString(",")}]
            }]
        }
        """.trimIndent()
    }
    
    private fun extractTextFromResponse(responseText: String): String {
        try {
            val textPattern = Regex(""""text":\s*"((?:[^"\\]|\\.)*)"""")
            val textMatch = textPattern.find(responseText)
                ?: throw Exception("Could not find text field in response")
            
            val textContent = textMatch.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            println("GeminiClient: Extracted text content (first 500 chars): ${textContent.take(500)}")
            return textContent
        } catch (e: Exception) {
            println("GeminiClient: Text extraction error: ${e.message}")
            throw e
        }
    }
}

object GeminiClientProvider {
    val instance: GeminiClient = GeminiClientImpl()
}

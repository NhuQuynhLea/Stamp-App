package com.lea.stamp.data

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

private var appContext: Context? = null

fun initImageUtils(context: Context) {
    appContext = context.applicationContext
}

actual suspend fun readImageBytes(imagePath: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val context = appContext ?: return@withContext null
        val uri = Uri.parse(imagePath)
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

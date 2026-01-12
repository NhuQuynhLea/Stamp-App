package com.lea.stamp.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readImageBytes(imagePath: String): ByteArray? {
    return try {
        val url = NSURL.URLWithString(imagePath) ?: return null
        val data = NSData.dataWithContentsOfURL(url) ?: return null
        
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        bytes
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

package com.lea.stamp.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.UIKit.UIImage
import org.jetbrains.skia.Image

@OptIn(ExperimentalForeignApi::class)
actual fun decodeBase64Mask(base64Data: String): ImageBitmap? {
    return try {
        // Decode base64 string to NSData
        val nsData = platform.Foundation.NSData.create(
            base64EncodedString = base64Data,
            options = NSDataBase64DecodingIgnoreUnknownCharacters
        ) ?: return null
        
        // Convert NSData to UIImage
        val uiImage = UIImage.imageWithData(nsData) ?: return null
        
        // Convert UIImage to Skia Image then to ImageBitmap
        val bytes = nsData.bytes ?: return null
        val byteArray = ByteArray(nsData.length.toInt())
        byteArray.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, nsData.length)
        }
        
        val skiaImage = Image.makeFromEncoded(byteArray)
        skiaImage?.toComposeImageBitmap()
    } catch (e: Exception) {
        println("MaskDecoder: Failed to decode mask on iOS: ${e.message}")
        null
    }
}

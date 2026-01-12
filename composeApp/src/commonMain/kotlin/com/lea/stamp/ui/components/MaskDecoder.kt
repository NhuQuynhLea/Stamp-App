package com.lea.stamp.ui.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a base64-encoded PNG mask image into an ImageBitmap
 * The mask is expected to be black/white where white pixels represent the object
 */
expect fun decodeBase64Mask(base64Data: String): ImageBitmap?

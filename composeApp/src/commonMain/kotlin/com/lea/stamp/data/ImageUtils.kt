package com.lea.stamp.data

expect suspend fun readImageBytes(imagePath: String): ByteArray?

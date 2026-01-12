package com.lea.stamp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

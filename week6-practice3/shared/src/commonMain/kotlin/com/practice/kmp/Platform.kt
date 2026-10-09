package com.practice.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

package com.practice.kmp

class Greeting {
    fun greet(): String = "Hi from shared code!"

    fun greetPlatform(): String = "Running on " + getPlatform().name
}

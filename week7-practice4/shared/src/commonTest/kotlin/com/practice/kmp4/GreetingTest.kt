package com.practice.kmp4

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun testGreet() {
        assertEquals("Hello from commonMain", Greeting().greet())
    }
}

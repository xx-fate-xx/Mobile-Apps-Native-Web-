package com.practice.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreetingTest {

    @Test
    fun greetReturnsText() {
        assertEquals("Hi from shared code!", Greeting().greet())
    }

    @Test
    fun timestampIsPositive() {
        assertTrue(currentTimestamp() > 0)
    }
}

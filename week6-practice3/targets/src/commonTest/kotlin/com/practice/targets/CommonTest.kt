package com.practice.targets

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonTest {
    @Test
    fun messageIsCorrect() {
        assertEquals("common code", commonMessage())
    }
}

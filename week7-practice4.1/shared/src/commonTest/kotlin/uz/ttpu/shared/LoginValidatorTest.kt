package uz.ttpu.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginValidatorTest {
    private val validator = LoginValidator()

    @Test
    fun correctLogin() {
        assertNull(validator.validate("student@ttpu.uz", "Password1"))
    }

    @Test
    fun shortPassword() {
        assertEquals("Password must be at least 8 characters", validator.validate("student@ttpu.uz", "Pa1"))
    }
}

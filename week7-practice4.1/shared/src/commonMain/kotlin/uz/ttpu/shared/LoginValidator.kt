package uz.ttpu.shared

// Task 14
class LoginValidator {

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email is empty"
        if (!isValidEmail(email)) return "Email is not valid"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (password.none { it.isDigit() }) return "Password must contain a digit"
        if (password.none { it.isUpperCase() }) return "Password must contain an uppercase letter"
        return null
    }

    // returns null if everything is ok, otherwise the first error
    fun validate(email: String, password: String): String? =
        validateEmail(email) ?: validatePassword(password)
}

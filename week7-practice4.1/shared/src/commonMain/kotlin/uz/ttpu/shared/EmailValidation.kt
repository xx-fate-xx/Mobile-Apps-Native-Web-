package uz.ttpu.shared

// Task 12
fun isValidEmail(text: String): Boolean {
    val at = text.indexOf('@')
    if (at <= 0 || at != text.lastIndexOf('@')) return false
    val domain = text.substring(at + 1)
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}

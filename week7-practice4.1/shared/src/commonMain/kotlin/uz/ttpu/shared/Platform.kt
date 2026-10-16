package uz.ttpu.shared

// Task 4
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

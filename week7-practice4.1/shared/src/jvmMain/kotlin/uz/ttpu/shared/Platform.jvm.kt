package uz.ttpu.shared

class JvmPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")} on ${System.getProperty("os.name")}"
}

actual fun getPlatform(): Platform = JvmPlatform()

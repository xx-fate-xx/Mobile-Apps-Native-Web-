package com.practice.kmp

import platform.UIKit.UIDevice

class IosPlatform : Platform {
    override val name: String = "iOS " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IosPlatform()

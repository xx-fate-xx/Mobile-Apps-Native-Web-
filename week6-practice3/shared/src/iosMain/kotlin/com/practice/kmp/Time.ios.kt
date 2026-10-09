package com.practice.kmp

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

// timeIntervalSince1970 is in seconds, so * 1000
actual fun currentTimestamp(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

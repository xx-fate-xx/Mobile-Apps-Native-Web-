package com.practice.kmp.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.practice.kmp.Greeting
import com.practice.kmp.currentTimestamp

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val greeting = Greeting()
        val tv = TextView(this)
        tv.textSize = 18f
        tv.setPadding(40, 100, 40, 40)
        tv.text = "${greeting.greet()}\n${greeting.greetPlatform()}\ntime: ${currentTimestamp()}"
        setContentView(tv)
    }
}

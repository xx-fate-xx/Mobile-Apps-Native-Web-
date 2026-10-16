package uz.ttpu.desktop

import uz.ttpu.shared.LoginValidator
import uz.ttpu.shared.getPlatform

fun main() {
    println("Running on: " + getPlatform().name)
    println("Login check: " + (LoginValidator().validate("student@ttpu.uz", "Password1") ?: "ok"))
}

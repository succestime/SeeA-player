package com.jaidev.seeaplayer

class MyClass {
    var myProperty: Int = 0

    fun modifyProperty(newValue: Int) {
        myProperty = newValue // This is allowed because it's within the same class
    }
}

fun main() {
    val obj = MyClass()
    obj.myProperty = 42 // Error: Setter is inaccessible
}

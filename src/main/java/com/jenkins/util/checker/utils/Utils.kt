package com.jenkins.util.checker.utils

import java.io.File

inline fun <reified T> isEqual(first: List<T>, second: List<T>): Boolean {
    if (first.size != second.size) {
        return false
    }
    return first.toTypedArray() contentDeepEquals second.toTypedArray()
}

fun getFile(filename: String): File? {
    val file = File(filename)
    return if (file.isFile) {
        file
    } else {
        println("No Files Found for $filename!! Please Check the Path Again..")
        null
    }
}
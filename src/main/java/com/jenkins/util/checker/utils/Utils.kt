package com.jenkins.util.checker.utils

import java.io.File
import java.util.stream.Collectors

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

fun stringContainsItems(inputStr: String, list: MutableList<String>, grouping: Map<String, List<String>>?): Boolean {
    /*return list.stream().anyMatch {
        it.contains(inputStr)
    }*/
    /*println("InputStr-> $inputStr || $list")
    return list.contains(inputStr)*/

    var checkData = false
    list.stream().collect(Collectors.groupingBy {
        inputStr.contains(it)
    })
    for (data in list) {
        if (inputStr.contains(data)) {
            println("Path:: $inputStr <--> $data")
            checkData = true
        }
    }
    return checkData
    /*val set = HashSet<String>(list)
    return set.contains(inputStr)*/
}

/*
fun groupings(inputStr: String, list: MutableList<String>, grouping: Map<String, List<String>>?): MutableMap<String, MutableList<String>>? {
    val mutableMap: MutableMap<String, String> = mutableMapOf()
    for (data in list) {
        if (inputStr.contains(data)) {
            println("Path:: $inputStr <--> $data")
            mutableMap[data] = inputStr
        }
    }
}*/

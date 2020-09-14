package com.jenkins.util.checker.utils

import org.apache.commons.collections4.CollectionUtils
import java.io.File

inline fun <reified T> isEqual(first: List<T>, second: List<T>): Boolean {
    if (first.size != second.size) {
        return false
    }
    return first.toTypedArray() contentDeepEquals second.toTypedArray()
}

inline fun <reified T> isContentEquals(first: List<T>, second: List<T>): Boolean {
    if (first.size != second.size) {
        return false
    }

    return CollectionUtils.isEqualCollection(first, second)
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

fun checkConfigDirectory(value: String): Boolean {
    return value.contains("/") || value.contains("\\")
}

fun checkerValue(value: String): String? {
    var setValue: String? = null
    setValue = when (value) {
        "EmailListener" -> "notif"
        "IBConsumerRespApproval" -> "apprv"
        "IBConsumerRespRegistration" -> "regis"
        "IBAdministrationService" -> "adm"
        "IBUserService" -> "usr"
        "IBTransferService" -> "trf"
        else -> null
    }
    return setValue
}

package com.jenkins.util.checker.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jenkins.util.checker.ConfigValidator
import com.jenkins.util.checker.models.ConfigMapper
import org.apache.commons.collections4.CollectionUtils
import java.io.*
import java.nio.charset.StandardCharsets

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

//Get File from Resources --> Not Work if Accessing in Jar File
fun getFileFromRes(filename: String): File? {
    val resource = ConfigValidator::class.java.classLoader.getResource(filename)
    return if (resource == null) null else File(resource.file)
}

//Get File as ResourceStream --> Work if Accessing in Jar File
fun getFileFromResources(filename: String): BufferedReader? {
    var bufferedReader: BufferedReader? = null
    val resourceStream: InputStream? = ConfigValidator::class.java.classLoader.getResourceAsStream(filename)
    resourceStream?.let {
        try {
            val streamReader = InputStreamReader(it, StandardCharsets.UTF_8)
            bufferedReader = BufferedReader(streamReader)
        } catch (e: IOException) {
            println(e.message.toString())
        }
    }

    return bufferedReader
}

fun checkConfigDirectory(value: String): Boolean {
    return value.contains("/") || value.contains("\\")
}

fun valueChecker(projectName: String, value: String): List<String>? {
    var listDir: List<String>? = null

    //val filename = getFileFromRes("ConfigMapper.json")?.readText(Charsets.UTF_8)
    val filename = getFileFromResources("ConfigMapper.json")
    val listMapping = object : TypeToken<ConfigMapper>() {}.type
    val dataGSON = Gson().fromJson<ConfigMapper>(filename, listMapping)
    for (data in dataGSON.mapper) {
        if (data.project_name == projectName) {
            for (dirName in data.dir_name) {
                if (dirName.last_dir_name == value) {
                    listDir = dirName.dir_path_name
                }
            }
        } else {
            println("Project Name Not Founds!")
        }
    }

    return listDir
}

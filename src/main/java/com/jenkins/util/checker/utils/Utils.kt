package com.jenkins.util.checker.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jenkins.util.checker.ConfigValidator
import com.jenkins.util.checker.models.ConfigMapper
import com.jenkins.util.checker.models.DeploymentMappers
import org.apache.commons.collections4.CollectionUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

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

fun subStringDir(lastConfigPath: String): String {
    var mLastConfigPath = lastConfigPath
    if (mLastConfigPath.contains("\\")) {
        mLastConfigPath = mLastConfigPath.replace("\\", "/")
    }
    val indexing = mLastConfigPath.lastIndexOf("/")
    return mLastConfigPath.substring(indexing + 1) //Getting the Last Dir Name -> ex: from ~> C\TestPath\Test\Path || to ~> Path
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

fun deploymentChecker(projectName: String, propName: String, dirValue: String, lastConfigPath: String) {
    //projectName -> KBI
    //propName -> IBank
    //dirValue -> ibank_pilot1 or mklik_pilot
    val filename = getFileFromResources("DeployMapper.json")
    val listMapping = object : TypeToken<DeploymentMappers>() {}.type
    val dataGSON = Gson().fromJson<DeploymentMappers>(filename, listMapping)
    for (data in dataGSON.deploy_mapper) {
        if (data.project_name == projectName) {
            for (deploymentList in data.deployment_dir_name) {
                if (deploymentList.prop_name == propName) {
                    if (dirValue.contains(deploymentList.dir_path_name)) {
                        for (modelsData in deploymentList.models) {
                            if (lastConfigPath.contains(modelsData.application)) {

                            }
                        }
                    } else {

                    }
                }
            }
        }
    }
}

fun deploymentConfig(projectName: String, propName: String, deploymentProperties: Properties) {
    //projectName -> KBI
    //propName -> IBank
    val filename = getFileFromRes("DeployMapper.json")
    filename?.let {
        val listMapping = object : TypeToken<DeploymentMappers>() {}.type
        val bufferedReader = BufferedReader(FileReader(it))

        val dataGSON = Gson().fromJson<DeploymentMappers>(bufferedReader, listMapping)
        for (data in dataGSON.deploy_mapper) {
            if (data.project_name == projectName) {
                for (deploymentList in data.deployment_dir_name) {
                    if (deploymentList.prop_name == propName) {
                        val dirPathName = deploymentList.dir_path_name
                        for (models in deploymentList.models) {
                            val applicationModel = models.application
                            for (listArtifact in models.artifact) {
                                deploymentProperties.setProperty("$dirPathName/$listArtifact",applicationModel)
                            }
                        }
                    }
                }

                //deploymentProp.store(FileOutputStream(File.createTempFile( "DeployProps", ".properties")), null)
            }
        }
    }
}

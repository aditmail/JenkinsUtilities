package com.jenkins.util.function.helper

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class ReadFileProperties {

    private val properties = Properties()
    private val writeProperties = Properties()

    //Get File from Resources
    fun getFileFromRes(filename: String): File? {
        val resource = ReadFileProperties::class.java.classLoader.getResource(filename)
        return if (resource == null) null else File(resource.file)
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

    fun readFromFiles(file: File?, fileTemp: File?, sourceTo: String?) {
        file?.let {
            val envStream = FileInputStream(file)
            properties.load(envStream)

            val tempLine = fileTemp?.readLines()
            tempLine?.forEach { data ->
                if (properties.getProperty(data) == "true") {
                    println("Selected Data: $data")
                    writeToFile(data, sourceTo)
                }
            }
        }
    }

    private fun writeToFile(value: String, sourceTo: String?) {
        sourceTo?.let {
            val files = File(sourceTo)
            val outputStream = FileOutputStream(files)
            writeProperties.setProperty(value, "true")
            writeProperties.store(outputStream, null)

            outputStream.fd.sync()
            outputStream.close()
        }

        /*val files = File("files.txt")
        if (files.createNewFile()) {
            val writer = FileWriter("files.txt", true)
            writer.write("$value=true\n")
            writer.close()
        } else {
            val writer = FileWriter("files.txt", true)
            writer.write("$value=true\n")
            writer.close()
        }*/
    }
}
package com.jenkins.util.checker.helper

import com.jenkins.util.checker.utils.getFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class CheckSumHelper(val filePath: String, val filePath2: String) {

    private val msgDigest = MessageDigest.getInstance("SHA-256")
    private val selectedFile: File? = getFile(filePath)

    fun fileCheckSum() {
        selectedFile?.let {
            try {

            } catch (e: IOException) {

            }
        }
    }

    fun compareFiles() {
        val file1 = File(filePath)
        val file2 = File(filePath2)

        val isEquals = FileUtils.contentEquals(file1, file2)
        if (isEquals) {
            println("Two Files are Equals")
        } else {
            println("Two Files are NOT Equals")
        }
    }
}
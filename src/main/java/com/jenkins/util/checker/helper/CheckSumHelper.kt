package com.jenkins.util.checker.helper

import com.jenkins.util.checker.utils.getFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class CheckSumHelper(filePath: String) {

    private val msgDigest = MessageDigest.getInstance("SHA-256")
    private val selectedFile: File? = getFile(filePath)

    fun fileCheckSum() {
        selectedFile?.let {
            try {

            } catch (e: IOException) {

            }
        }
    }
}
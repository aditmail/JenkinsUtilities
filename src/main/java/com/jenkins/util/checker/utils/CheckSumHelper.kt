package com.jenkins.util.checker.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

class CheckSumHelper() {

    private val msgDigestSHA = MessageDigest.getInstance("SHA-256")
    private val msgDigestMD5 = MessageDigest.getInstance("MD5")

    fun generateMD5Code(fileName: String?): String? {
        var checksumMD5: String? = null
        fileName?.let {
            val getFile = getFile(it)
            if (getFile != null) {
                //val generateMD5File = DigestUtils.md5Hex()
                checksumMD5 = getFileChecksum(msgDigestMD5, fileName)
            } else {
                println("No Files Found for:: $fileName, Please Check the Path/FileName Again")
            }
        }

        return checksumMD5
    }

    private fun getFileChecksum(messageDigest: MessageDigest, file: String): String? {
        messageDigest.update(Files.readAllBytes(Paths.get(file)))
        val digest = messageDigest.digest()
        return DatatypeConverter.printHexBinary(digest)
    }

    //Compare Two Files
    /*fun compareFiles() {
        val file1 = File(filePath)
        val file2 = File(filePath2)

        val isEquals = FileUtils.contentEquals(file1, file2)
        if (isEquals) {
            println("Two Files are Equals")
        } else {
            println("Two Files are NOT Equals")
        }
    }*/
}
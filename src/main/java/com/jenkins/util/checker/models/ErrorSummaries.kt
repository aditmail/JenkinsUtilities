package com.jenkins.util.checker.models

import java.io.File

data class ErrorSummaries(
        val errorPath: File?,
        var listExpected: MutableList<String>? ,
        var listActualItems: MutableList<String>?
)
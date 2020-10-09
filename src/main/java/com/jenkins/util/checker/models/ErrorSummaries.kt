package com.jenkins.util.checker.models

import java.io.File
import java.nio.file.Path

data class ErrorSummaries(
        val errorPath: File?,
        var listExpected: MutableList<String>?,
        var listActualItems: MutableList<String>?
)

data class ErrorDeployment(
        val deploymentModel: String?,
        var errorParentPath: File?,
        var errorPathName: Path?,
        var artifactName: String?
)

data class ListChildGrouping(
        var listMap: MutableList<MutableMap<String,String>>?
)

data class ChildGrouping(
        val mapChildGrouping: MutableMap<String, String>
)

data class MapGrouping(
        val mapGrouping: MutableMap<Int, String>?
)
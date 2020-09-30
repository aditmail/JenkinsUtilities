package com.jenkins.util.checker.models

import java.io.File

data class ErrorSummaries(
        val errorPath: File?,
        var listExpected: MutableList<String>?,
        var listActualItems: MutableList<String>?
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
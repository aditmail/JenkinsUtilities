package com.jenkins.util.checker.models

data class ConfigMapper(
        val mapper: List<Mapper>
)

data class DirName(
        val dir_path_name: List<String>,
        val last_dir_name: String
)

data class Mapper(
        val dir_name: List<DirName>,
        val project_name: String
)
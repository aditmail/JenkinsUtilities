package com.jenkins.util.checker.models

data class DeploymentMappers(
    val deploy_mapper: List<DeployMapper>
)

data class Model(
        val application: String,
        val artifact: List<String>
)

data class DeploymentDirName(
        val dir_path_name: String,
        val models: List<Model>,
        val prop_name: String
)

data class DeployMapper(
        val deployment_dir_name: List<DeploymentDirName>,
        val project_name: String
)
package com.jenkins.util.checker.helper

import com.jenkins.util.checker.helper.config.ConfigHelperNew
import com.jenkins.util.checker.helper.deployment.DeploymentHelperNew
import com.jenkins.util.checker.models.DividerModels
import com.jenkins.util.checker.utils.*
import java.io.File
import java.nio.file.Paths

class DividerHelper(private val args: Array<String>?) {

    private val listConfigPaths: MutableList<DividerModels>? = ArrayList()
    private val listDeploymentPaths: MutableList<DividerModels>? = ArrayList()

    fun initFiles() {
        if (args?.size == 0 || args?.size != 4) {
            println(strInputParameters)
            println(strInputFirstParams)
            println(strInputSecondParams)
            println(strInputThirdParams)
            println(strInputFourthParams)
        } else {
            val projectName = args[0].trim()
            val configType = args[1].trim()
            val nodeDir = args[2].trim()
            val configPath = args[3].trim()

            println("""
                |-----------------------
                |Detail of Params
                |-----------------------
                |projectName:: $projectName
                |flavor:: $configType
                |Directory:: $nodeDir
                |configPath:: $configPath
                |------------------------
            """.trimMargin())

            startValidating(projectName, configType, nodeDir, configPath)
        }
    }

    private fun startValidating(projectName: String, flavorType: String, nodeDir: String, configPath: String) {
        println("Project Name: $projectName w/ $flavorType Flavor")
        File(nodeDir).listFiles()?.let { getListDir ->
            if (!getListDir.isNullOrEmpty()) {
                for (listDir in getListDir) {
                    val startParentPathing = Paths.get(listDir.path)
                    val deploymentModels = startParentPathing.fileName.toString()
                    println("DeployModels -> $deploymentModels")

                    getDirectoryNode(startParentPathing.normalize())?.let { listNodes ->
                        listNodes.removeAt(0)

                        for (node in listNodes) {
                            //Getting the Last Node Name
                            var getNodeName = node
                            if(getNodeName.contains("\\")){
                                getNodeName = getNodeName.replace("\\", "/")
                            }
                            val lastIndexOf = getNodeName.lastIndexOf("/")
                            val setNodeName = getNodeName.substring(lastIndexOf + 1)

                            println("Nodes--> $setNodeName")
                            getParentStreamListTest(Paths.get(node))?.let { parentDirList ->
                                for (parentDir in parentDirList) {
                                    if (parentDir.contains("config")) {
                                        val dividerModels = DividerModels(setNodeName, deploymentModels, parentDir)
                                        listConfigPaths?.add(dividerModels)
                                    } else if (parentDir.contains("deployment")) {
                                        val dividerModels = DividerModels(setNodeName, deploymentModels, parentDir)
                                        listDeploymentPaths?.add(dividerModels)
                                    }
                                }
                            }
                        }
                    }

                    listConfigPaths?.let {
                        val getConfigList = it.toMutableList()
                        ConfigHelperNew(getConfigList, configPath)
                                .validateConfig(projectName, flavorType)
                    }

                    listDeploymentPaths?.let {
                        val getDeploymentList = it.toMutableList()
                        DeploymentHelperNew(getDeploymentList, configPath)
                                .validateDeployment(projectName, flavorType)
                    }

                    listConfigPaths?.clear()
                    listDeploymentPaths?.clear()
                }
            }
        }
    }
}
package com.jenkins.util.checker.helper.deployment

import com.jenkins.util.checker.models.DividerModels
import com.jenkins.util.checker.models.ErrorDeployment
import com.jenkins.util.checker.utils.*
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.RoundingMode
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DeploymentHelperNew(private val listDeployment: MutableList<DividerModels>, private val configPath: String) : IConfig.StringBuilders {

    //Data Files
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Init Helper
    private lateinit var projectName: String
    private lateinit var flavorType: String

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private val stringBuilder: StringBuilder = StringBuilder()
    private val properties = Properties()

    private val deploymentProperties = Properties()
    private lateinit var deployPropFile: File

    private val listDataProps: MutableList<String>? = ArrayList()
    private val listNodesName: MutableList<File>? = ArrayList()
    private val listErrorPath: MutableList<ErrorDeployment>? = ArrayList()

    //Init Counter
    private var totalConfigData = 0
    private var passedConfigData = 0
    private var failedConfigData = 0

    fun validateDeployment(projectName: String, flavorType: String) {
        this.projectName = projectName
        this.flavorType = flavorType

        if (!listDeployment.isNullOrEmpty()) {
            println("ListOfDeployment -> $listDeployment || size: ${listDeployment.size}")

            listDeployment.forEachIndexed { index, dividerModels ->
                println("indexed -> $index")
                listNodesName?.add(File(dividerModels.parentDirectory))
                if (index == 0) {
                    println("TEST")
                    generatedFile(projectName, flavorType, dividerModels.deployModels)
                    getConfigFile(configPath)

                    populateProperties(null) //Read the txt contains Properties Data..
                }

                startMappingDeployment(dividerModels.deployModels, dividerModels.parentDirectory, dividerModels.nodeName, index)
            }

            summaryPercentage(totalConfigData, passedConfigData, failedConfigData)
            errorSummaries(listErrorPath)

            closeAndCreateFile()
        } else {
            println("No Data Found in this Directory")
        }
    }

    private fun closeAndCreateFile() {
        try {
            val deleteResult = deployPropFile.delete()
            if (deleteResult) {
                println("Temp Data is Deleted! ${deployPropFile.name}")
            } else {
                println("Unable to Deleted ${deployPropFile.name}")
            }
        } catch (ex: Exception) {
            println("Ex: ${ex.message}")
        }

        stbAppendStyle("h4", strReportSummaries)
        stbAppendStyle("div-close", null)

        println("Successfully Running the Deployment Validator!")

        stbAppendStyle("body-close", null)
        stbAppendStyle("html-close", null)

        writeToFile(stringBuilder.toString(), fileOutput)
        println("Creating Report File Successful!")
    }

    private fun startMappingDeployment(configType: String, nodesDirPath: String, nodeName: String?, index: Int) {
        stbAppendStyle("div-open", "content")
        stbAppendStyle("h4", String.format(strDeploymentNameHTML, configType))

        stbAppendStyle("div-open", "pj")
        stbAppendStyle("table-open", null)
        val headerName = mutableListOf<Any>(strNodeNo, strNodeName)
        stbAppendTableHeader(null, headerName)

        val tableData = mutableListOf<Any>((index + 1), "<mark><b>$nodeName</b></mark>")
        stbAppendTableData("center", tableData)
        stbAppendStyle("table-close", null)

        deployPropFile.let { deploy ->
            val envStream = FileInputStream(deploy)
            deploymentProperties.load(envStream)

            stbAppend(null)
            stbAppendStyle("table-open", null)
            val headerFileName = mutableListOf<Any>(strNo, strPathName, strName, strSize, strMD5Code, strStatus, strNotes)
            stbAppendTableHeader(null, headerFileName)

            getConfigStreamList(nodesDirPath, 2)?.let { listPath ->
                println("listPath -> ${listPath.size} ++ ${listPath.toList()}")
                listPath.forEachIndexed { fileIndex, path ->
                    val getParentFile = File(path).parentFile
                    println("ParentPath-> $getParentFile")
                    val getParentPath = Paths.get(getParentFile.toString())

                    val getSubPath: Path? = checkDataHandling(projectName, flavorType, configType, getParentPath)

                    val getFileName = File(path).name
                    val sizeOfFile = FileUtils.byteCountToDisplaySize(File(path).length())
                    val generateMD5 = CheckSumHelper().generateMD5Code(File(path).absolutePath)

                    for (keys in deploymentProperties.stringPropertyNames()) {
                        if (deploymentProperties.getProperty(keys) == configType) {
                            val splitKeys = keys.split("/")
                            val getLocationArtifact = splitKeys[0]
                            val getNameArtifact = splitKeys[1]

                            print("GetFilePath: $getSubPath --> isContains $getLocationArtifact? ")
                            getSubPath?.let {
                                println("subpath -> ${path}")
                                if (it.toString().contains(getLocationArtifact)) {
                                    totalConfigData += 1 //Count How Many Files Are Compared
                                    println("[TRUE]")

                                    val pathLink = "<a href=\"$getParentFile\" target=\"_blank\">${it}</a>"
                                    if (getNameArtifact == getFileName) {
                                        passedConfigData += 1 //If Passed, Add Counter

                                        val tableDataFile = mutableListOf<Any>((fileIndex + 1), pathLink, "<p class=\"listData\">$getFileName</p>", sizeOfFile, generateMD5.toString(), "<p class=\"passed\">$strPassed</p>", strCorrectFile)
                                        stbAppendTableData("center", tableDataFile)
                                    } else {
                                        failedConfigData += 1 //If Failed Add Counter
                                        val errorDeployment = ErrorDeployment(configType, getParentFile, it, "<mark>$getFileName</mark> [Incorrect File] in Directory")
                                        listErrorPath?.add(errorDeployment)

                                        val tableDataFile = mutableListOf<Any>((fileIndex + 1), pathLink, "<p class=\"listData\">$getFileName</p>", sizeOfFile, generateMD5.toString(), "<p class=\"failed\">$strFailed</p>", strIncorrectFile)
                                        stbAppendTableData("center", tableDataFile)
                                    }

                                    //filtering files matched with artifactName
                                    val isFound = File(path).parentFile.listFiles()?.let { data ->
                                        data.filter { filter ->
                                            println("filter -> $filter")
                                            filter.name == getNameArtifact
                                        }
                                    }

                                    //To check if Data File Exist in Dir or Not
                                    if(isFound.isNullOrEmpty()){
                                        val errorDeployment = ErrorDeployment(configType, getParentFile, it, "<mark>$getNameArtifact</mark> [Not Found] in Directory")
                                        listErrorPath?.add(errorDeployment)

                                        val tableDataFile = mutableListOf<Any>((fileIndex + 1), pathLink, "<p class=\"failed\">XXXXX</p>", "<p class=\"failed\">XXXXX</p>", "<p class=\"failed\">XXXXX</p>", "<p class=\"failed\">$strFailed</p>", String.format(strNotFoundFile, getNameArtifact))
                                        stbAppendTableData("center", tableDataFile)
                                    }
                                } else {
                                    println("[FALSE]")
                                }
                            }
                        }
                    }
                }
            }
            stbAppendStyle("table-close", null)


            stbAppendStyle("div-close", null)
            stbAppendStyle("div-close", null)

            stbAppend(null)
        }
    }

    //Checking model for APP/WEB; KBI OR OTHER; INTRA/INTER OR OTHER
    private fun checkDataHandling(projectName: String, flavorType: String, configType: String, getParentPath: Path): Path? {
        var setPath: Path? = null
        if (projectName == "klikBCAIndividu") {
            when (flavorType) {
                "PILOT" -> {
                    setPath = getParentPath.subpath(getParentPath.nameCount - 2, getParentPath.nameCount - 1)
                }
                "INTRA" -> {
                    setPath = getParentPath.subpath(getParentPath.nameCount - 3, getParentPath.nameCount - 2)
                }
                "INTER" -> {
                    setPath = if (configType == "APP") {
                        getParentPath.subpath(getParentPath.nameCount - 3, getParentPath.nameCount - 2)
                    } else {
                        getParentPath.subpath(getParentPath.nameCount - 2, getParentPath.nameCount - 1)
                    }
                }
            }
        }

        return setPath
    }

    private fun summaryPercentage(totalValue: Int, passedValue: Int, failedValue: Int) {
        if (totalValue != 0) {
            val decFormat = DecimalFormat("#.##")
            decFormat.roundingMode = RoundingMode.CEILING

            val successPercentage = (passedValue / totalValue.toDouble()) * 100f
            val failedPercentage = (failedValue / totalValue.toDouble()) * 100f

            stbAppend("<hr>")
            stbAppendStyle("div-open", "pj")
            stbAppendStyle("h4", strReportSummaries)
            stbAppendStyle("p", "&#8258;<b>$strTotalData</b> &#8658; $totalValue")
            stbAppendStyle("p", "&#8258;<b style=\"color:green\">$strTotalPassedData</b> &#8658; $passedValue")
            stbAppendStyle("p", "&#8258;<b style=\"color:red\">$strTotalFailedData</b> &#8658; $failedValue")

            stbAppendStyle("table-open", null)
            val nameTableHeader = mutableListOf<Any>("$strPassed &#9989;", "$strFailed &#10060;")
            stbAppendTableHeader(null, nameTableHeader)

            val successful = decFormat.format(successPercentage)
            val failed = decFormat.format(failedPercentage)
            val summaryTableData = mutableListOf<Any>("<p class=\"percentage\">$successful%</p>", "<p class=\"percentage\">$failed%</p>")
            stbAppendTableData("center", summaryTableData)
            stbAppendStyle("table-close", null)

            stbAppend(null)
        }
    }

    private fun errorSummaries(listErrorPath: MutableList<ErrorDeployment>?) {
        println("ListError: $listErrorPath")
        listErrorPath?.let {
            println("Name:: $it")
            if (!it.isNullOrEmpty()) {
                stbAppendStyle("h4", strErrorList)

                stbAppendStyle("table-open", null)
                val nameTableHeader = mutableListOf<Any>(strNo, strModel, strErrorPathName, strArtifactName)
                stbAppendTableHeader(null, nameTableHeader)

                for ((index, dirPaths) in it.withIndex()) {
                    val linkPath = "<a href =\"${dirPaths.errorParentPath}\" target=\"_blank\"> ${dirPaths.errorPathName}</a>"

                    val errorListTableData = mutableListOf<Any>((index + 1), "<mark>${dirPaths.deploymentModel}</mark>", linkPath, "<b>${dirPaths.artifactName} &#10060;</b>")
                    stbAppendTableData("center", errorListTableData)
                }

                stbAppendStyle("table-close", null)
                stbAppendStyle("p", strConfigErrorActionHTML)
            }
        }
    }

    private fun getConfigFile(configPath: String?) {
        configPath?.let {
            File(it).listFiles()?.let { list ->
                list.forEach { file ->
                    if (file.name == "changes-deployment.txt") {
                        configFile = getFile(file.toString())
                    }
                }
            }
        }
    }

    private fun populateProperties(destinationPath: String?) {
        println("configFile -> $configFile")
        configFile?.let { config ->
            deployPropFile = if (destinationPath.isNullOrEmpty()) {
                //File("var/", "DeployProp.properties") //For Real Using
                File("DeployProp.properties") //For Testing
            } else {
                File(destinationPath, "DeployProp.properties")
            }

            if (!deployPropFile.exists()) {
                deployPropFile.createNewFile()
            }

            println("Path -> ${deployPropFile.absolutePath}")
            val envStream = FileInputStream(config) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties
            for (keys in properties.stringPropertyNames()) {
                if (properties.getProperty(keys) == "true") {
                    deploymentConfig(projectName, keys, deploymentProperties)
                    listDataProps?.add(keys)
                }
            }

            deploymentProperties.store(FileOutputStream(deployPropFile), null)
        }
    }

    private fun generatedFile(projectName: String, flavorType: String, deployModels: String) {
        fileOutput = File("[$projectName]outputDeployment_${deployModels}.html") //Save not in VAR Folder..
        if (!fileOutput.exists()) {
            fileOutput.createNewFile()
        } else {
            println("Existing Data will be Rewrite")
        }

        initHtmlStyle(projectName, flavorType, deployModels)
    }

    private fun initHtmlStyle(projectName: String, configType: String, deployModels: String) {
        stbAppend("""<html>
                        <head>
                            <title>Validator $projectName | $configType - $deployModels </title>
                            <meta name ="viewport" content="width=device-width, initial-scale=1">
                            <meta http-equiv ="Content-Security-Policy" content="default-src 'self'; style-src 'self' 'unsafe-inline';">
                            $strStyleHTML
                        </head>
                        <body>
                    """.trimIndent())

        initHeader(projectName, configType, deployModels)
    }

    private fun initHeader(projectName: String, configType: String, deployModels: String) {
        stbAppendStyle("div-open", "pj")

        stbAppendStyle("h4", strDeployValidator)
        stbAppendStyle("table-open", null)

        val headerName = mutableListOf<Any>(strProjectName, strFlavorType, strDeployModel)
        stbAppendTableHeader(null, headerName)
        val tableData = mutableListOf<Any>(
                "<p class=\"listData\">$projectName</p>",
                "<mark><b>$configType</b></mark>",
                "<b>$deployModels</b>"
        )

        stbAppendTableData("center", tableData)
        stbAppendStyle("table-close", null)

        val dateNow = LocalDateTime.now()
        stbAppendStyle("p", String.format(strGeneratedAt, dateFormatter.format(dateNow)))
        stbAppendStyle("h4", strDeployValidator)

        stbAppendStyle("div-close", null)
        stbAppend("<hr>")
    }

    override fun stbAppend(value: String?) {
        if (value == null) {
            stringBuilder.append("<p></br></p>")
        } else {
            stringBuilder.append(value)
        }
    }

    override fun stbAppendStyle(model: String?, value: String?) {
        model?.let {
            when (it) {
                "p" -> stringBuilder.append("<p>$value</p>")
                "p-open" -> stringBuilder.append("<p>$value")
                "p-close" -> stringBuilder.append("$value</p>")
                "h4" -> stringBuilder.append("<h4>$value</h4>")
                "div-open" -> stringBuilder.append("<div class=\"$value\">")
                "div-close" -> stringBuilder.append("</div>")
                "table-open" -> {
                    if (value == "no-border") {
                        stringBuilder.append("<table>")
                    } else {
                        stringBuilder.append("<table border=\"1px;\" bordercolor=\"#000000;\">")
                    }
                }
                "table-close" -> stringBuilder.append("</table>")
                "body-close" -> stringBuilder.append("</body>")
                "html-close" -> stringBuilder.append("</html>")
                else -> stringBuilder.append(value)
            }
        }
    }

    override fun stbAppendTableHeader(value: String?, arrValue: MutableList<Any>?) {
        arrValue?.let {
            stringBuilder.append("<tr>")
            for (data in it) {
                stringBuilder.append("<th class=\"center\"><b>$data</b></th>")
            }
            stringBuilder.append("</tr>")
        }
    }

    override fun stbAppendTableData(value: String?, arrValue: MutableList<Any>?) {
        arrValue?.let {
            stringBuilder.append("<tr>")
            for (data in it) {
                if (value.isNullOrBlank()) {
                    stringBuilder.append("<td>$data</td>")
                } else {
                    stringBuilder.append("<td class=\"center\">$data</td>")
                }
            }
            stringBuilder.append("</tr>")
        }
    }
}
package com.jenkins.util.checker.helper.deployment

import com.jenkins.util.checker.models.ErrorDeployment
import com.jenkins.util.checker.utils.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.RoundingMode
import java.nio.file.Paths
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class DeploymentHelperHTML(private val args: Array<String>?) : IConfig.StringBuilders {

    //Data Files
    private var nodeDirFiles: File? = null //Path of Dir Config
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Init Helper
    private lateinit var projectName: String
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private lateinit var stringBuilder: StringBuilder
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

    fun initFiles() {
        if (args?.size == 0 || args?.size != 4) {
            println(strInputParameters)
            println(strInputFirstParams)
            println(strInputSecondParams)
            println(strInputThirdParams)
            println(strInputFourthParams)
        } else {
            this.projectName = args[0].trim()
            val configType = args[1].trim()
            val nodeDir = args[2].trim()
            val configPath = args[3].trim()

            //Init Config File
            configFile = getFile(configPath)
            populateProperties(null) //Read the txt contains Properties Data..

            println("ListDataProps:: $listDataProps (${listDataProps?.size} Data)")
            if (!listDataProps.isNullOrEmpty()) {
                startValidating(configType, nodeDir, null)
            } else {
                println("""All ${configFile?.name} Properties Contains False Value
                The Deployment Validator is Cancelled.
            """.trimMargin())
            }
        }
    }

    fun initFiles(projectName: String, configType: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        this.projectName = projectName

        //Init Config File
        configFile = getFile(configPath)
        populateProperties(destinationPath) //Read the txt contains Properties Data..

        println("ListDataProps:: $listDataProps (${listDataProps?.size} Data)")
        if (!listDataProps.isNullOrEmpty()) {
            startValidating(configType, nodesDirPath, destinationPath)
        } else {
            println("""All ${configFile?.name} Properties Contains False Value
                The Deployment Validator is Cancelled.
            """.trimMargin())
        }
    }

    private fun populateProperties(destinationPath: String?) {
        configFile?.let { config ->
            deployPropFile = if (destinationPath.isNullOrEmpty()) {
                File("var/", "DeployProp.properties")
            } else {
                File(destinationPath, "DeployProp.properties")
                //createTempFile("DeployProp_", ".properties", File(destinationPath))
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

    private fun startValidating(configType: String, nodesDirPath: String, destinationPath: String?) {

        //Init FileOutput
        fileOutput = if (destinationPath.isNullOrEmpty()) {
            File("outputDeployments_${configType}.html")
        } else {
            File("$destinationPath/outputDeployments_${configType}.html")
        }

        if (!fileOutput.exists()) {
            fileOutput.createNewFile()
            println("Creating File:: $fileOutput")
        } else {
            println("File Already Exist:: $fileOutput")
            println("Existing Data Will be Rewrite")
        }
        stringBuilder = StringBuilder()

        //Init Config Dir
        nodeDirFiles = File(nodesDirPath)

        //Checking Data..
        checkMappings(nodeDirFiles, projectName, configType)
    }

    private fun checkMappings(nodeDirFiles: File?, projectName: String, configType: String) {
        nodeDirFiles?.let { data ->
            val lists = data.listFiles() //Listing Files in Parameter Path
            if (lists != null && lists.isNotEmpty()) {
                initHtmlStyle(projectName, configType)
                initHeader(projectName, configType, lists)

                for (dirPaths in lists) {
                    println("#DirPath -> $dirPaths")

                    stbAppendStyle("div-open", "content")
                    listNodesName?.add(dirPaths)

                    val startParentPathing = Paths.get(dirPaths.path) //Start Listing using Relative PATH
                    val deploymentModels = startParentPathing.fileName.toString()
                    stbAppendStyle("h4", String.format(strDeploymentNameHTML, deploymentModels))

                    val nodeCollect = getDirectoryNode(startParentPathing.normalize())
                    nodeCollect?.let {
                        it.removeAt(0)

                        for ((no, nodeList) in it.withIndex()) {
                            stbAppendStyle("div-open", "pj")
                            stbAppendStyle("table-open", null)

                            val headerName = mutableListOf<Any>(strNodeNo, strNodeName)
                            stbAppendTableHeader(null, headerName)

                            val tableData = mutableListOf<Any>((no + 1), "<mark><b>${subStringDir(nodeList)}</b></mark>")
                            stbAppendTableData("center", tableData)
                            stbAppendStyle("table-close", null)

                            val collect = getParentStreamList(Paths.get(nodeList), 2)
                            collect?.let { parentList ->
                                for (configList in parentList) {

                                    /** NEW MODEL - Concept */
                                    deployPropFile.let { deploy ->
                                        val envStream = FileInputStream(deploy)
                                        deploymentProperties.load(envStream)

                                        getConfigStreamList(configList, 2)?.let { listPath ->
                                            listPath.forEach { path ->
                                                val getParentFile = File(path).parentFile
                                                val getParentPath = Paths.get(getParentFile.toString())

                                                val getSubPath = getParentPath.subpath(getParentPath.nameCount - 2, getParentPath.nameCount - 1)
                                                val getFileName = File(path).name

                                                for (keys in deploymentProperties.stringPropertyNames()) {
                                                    if (deploymentProperties.getProperty(keys) == deploymentModels) {
                                                        val splitKeys = keys.split("/")
                                                        val getLocationArtifact = splitKeys[0]
                                                        val getNameArtifact = splitKeys[1]

                                                        print("GetFilePath: $getSubPath --> isContains $getLocationArtifact? ")
                                                        if (getSubPath.toString().contains(getLocationArtifact)) {

                                                            totalConfigData += 1 //Count How Many Files Are Compared
                                                            println("[TRUE]")

                                                            stbAppend(null)
                                                            stbAppendStyle("table-open", null)

                                                            val pathLink = "<a href=\"$getParentFile\" target=\"_blank\">${getSubPath}</a>"
                                                            val pathTableData = mutableListOf<Any>("<p class=\"tableData\">$strPathName</p>", pathLink)

                                                            stbAppendTableData(null, pathTableData)
                                                            if (getNameArtifact == getFileName) {
                                                                passedConfigData += 1 //If Passed, Add Counter

                                                                val statusTableData = mutableListOf<Any>("<p class=\"tableData\">$strStatus</p>", "<p class=\"passed\">$strPassed</p>")
                                                                stbAppendTableData(null, statusTableData)

                                                                val notesTableData = mutableListOf<Any>("<p class=\"tableData\">$strNotes</p>", String.format(strDeploymentPassedHTML, getFileName))
                                                                stbAppendTableData(null, notesTableData)
                                                                stbAppendStyle("table-close", null)
                                                            } else {
                                                                failedConfigData += 1 //If Failed Add Counter
                                                                val errorDeployment = ErrorDeployment(deploymentModels, getParentFile, getSubPath, getFileName)
                                                                listErrorPath?.add(errorDeployment)

                                                                val statusTableData = mutableListOf<Any>("<p class=\"tableData\">$strStatus</p>", "<p class=\"failed\">$strFailed</p>")
                                                                stbAppendTableData(null, statusTableData)

                                                                val notesTableData = mutableListOf<Any>("<p class=\"tableData\">$strNotes</p>", String.format(strDeploymentFailedHTML, getFileName))
                                                                stbAppendTableData(null, notesTableData)
                                                                stbAppendStyle("table-close", null)
                                                            }
                                                        } else {
                                                            println("[FALSE]")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            stbAppendStyle("div-close", null)
                            stbAppend(null)
                        }
                    }

                    stbAppendStyle("div-close", null)
                    stbAppend("<hr>")
                }
            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }

            summaryPercentage(totalConfigData, passedConfigData, failedConfigData)
            errorSummaries(listErrorPath)

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
    }

    private fun initHtmlStyle(projectName: String, configType: String) {
        stbAppend("""<html>
                        <head>
                            <title>Validator $projectName | $configType </title>
                            <meta name ="viewport" content="width=device-width, initial-scale=1">
                            <meta http-equiv ="Content-Security-Policy" content="default-src 'self'; style-src 'self' 'unsafe-inline';">
                            $strStyleHTML
                        </head>
                        <body>
                    """.trimIndent())
    }

    private fun initHeader(projectName: String, configType: String, lists: Array<File>) {
        stbAppendStyle("div-open", "pj")

        stbAppendStyle("h4", strDeployValidator)
        stbAppendStyle("table-open", null)

        val headerName = mutableListOf<Any>(strProjectName, strFlavorType, strDeployModel)
        stbAppendTableHeader(null, headerName)

        val data: MutableList<String>? = ArrayList()
        lists.asList().forEach { data?.add(it.name) }
        println("Models Data:: $data")

        val tableData = mutableListOf<Any>(
                "<p class=\"listData\">$projectName</p>",
                "<mark><b>$configType</b></mark>",
                "<b>$data (${data?.size})</b>"
        )

        stbAppendTableData("center", tableData)
        stbAppendStyle("table-close", null)

        val dateNow = LocalDateTime.now()
        stbAppendStyle("p", String.format(strGeneratedAt, dateFormatter.format(dateNow)))
        stbAppendStyle("h4", strDeployValidator)

        stbAppendStyle("div-close", null)
        stbAppend("<hr>")
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
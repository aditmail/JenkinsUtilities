package com.jenkins.util.checker.helper.config

import com.jenkins.util.checker.models.DividerModels
import com.jenkins.util.checker.models.ErrorSummaries
import com.jenkins.util.checker.utils.*
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConfigHelperNew(private val listConfig: MutableList<DividerModels>?, private val configPath: String?) : IConfig.StringBuilders {

    private lateinit var fileOutput: File //Path + Filename of Output Config Validator
    private lateinit var projectName: String //Path + Filename of Output Config Validator
    private var configFile: File? = null

    private val stringBuilder: StringBuilder = StringBuilder()
    private val properties = Properties()
    private val checkSumHelper = CheckSumHelper()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    //Init List
    private val listActualItems: MutableList<String> = ArrayList()
    private val listExpectedItems: MutableList<String> = ArrayList()
    private val listActualFiles: MutableList<File> = ArrayList()

    //List of mapChildDataGrouping
    private val listChildGrouping: MutableList<MutableMap<String, String>> = ArrayList()

    private val listDataProps: MutableList<String>? = ArrayList()
    private val listNodesName: MutableList<String>? = ArrayList()
    private val listPaths: MutableList<File>? = ArrayList()
    private val listErrorPath: MutableList<ErrorSummaries>? = ArrayList()

    //Init Parent Map
    private val mapChildDataGroupings: MutableMap<Int, MutableList<MutableMap<String, String>>> = mutableMapOf()
    private val mapDataGrouping: MutableMap<Int, MutableMap<Int, String>> = mutableMapOf()

    //Init Child Map
    private lateinit var mapChildGrouping: MutableMap<String, String>
    private lateinit var mapGrouping: MutableMap<Int, String>

    //Init Counter
    private var totalConfigData = 0
    private var passedConfigData = 0
    private var failedConfigData = 0

    fun validateConfig(projectName: String, flavorType: String) {
        this.projectName = projectName
        populateProperties()

        if (!listConfig.isNullOrEmpty()) {
            println("ListOfConfig -> $listConfig || size: ${listConfig.size}")
            listConfig.forEachIndexed { index, dividerModels ->
                listNodesName?.add(dividerModels.nodeName)
                listPaths?.add(File(dividerModels.parentDirectory))
                if (index == 0) {
                    generatedFile(projectName, flavorType, dividerModels.deployModels)
                    getConfigFile(configPath, dividerModels.deployModels)
                    populateProperties()
                }

                startMappingConfig(index, dividerModels.parentDirectory)
            }
            if (!listDataProps.isNullOrEmpty()) {
                populateChildData(mapChildDataGroupings, listPaths)
            } else {
                populateData(mapDataGrouping, listPaths)
            }

            summaryPercentage(totalConfigData, passedConfigData, failedConfigData)
            errorSummaries(listErrorPath)

            stbAppendStyle("h4", strReportSummaries)
            stbAppendStyle("div-close", null)

            writeFile()
        } else {
            println("No Data Found in this Directory")
        }
    }

    private fun getConfigFile(configPath: String?, deployModels: String) {
        configPath?.let {
            File(it).listFiles()?.let { list ->
                list.forEach { file ->
                    if (deployModels.contains("APP") && file.name == "changes-config-app.txt") {
                        configFile = getFile(file.toString())
                    } else if (deployModels.contains("WEB") && file.name == "changes-config-web.txt") {
                        configFile = getFile(file.toString())
                    }
                }
            }
        }
    }

    private fun startMappingConfig(index: Int, parentDirectory: String) {
        val configCollect = getConfigStreamList(parentDirectory, 1)

        if (listDataProps.isNullOrEmpty()) {
            mapGrouping = mutableMapOf() //Init Map to Hold Values
        }

        configCollect?.let { childList ->
            childList.removeAt(0) //Remove Parent Dir
            childList.forEachIndexed { index, lastConfigPath ->
                if (checkConfigDirectory(lastConfigPath)) {
                    val getLastDirName = subStringDir(lastConfigPath)
                    if (!listDataProps.isNullOrEmpty()) {
                        mappingChildConfig(listDataProps, getLastDirName, lastConfigPath)
                    } else {
                        mappingConfig(index, lastConfigPath, mapGrouping)
                    }
                }
            }
        }

        if (!listDataProps.isNullOrEmpty()) {
            val listChildPath = listChildGrouping.toMutableList()

            //Inserting The Child Data Looping to Parent Mapping
            mapChildDataGroupings[index] = listChildPath

            //Reset the List Value to use for another loop
            listChildGrouping.clear()
        } else {
            //Inserting The Data Looping to Parent Mapping
            mapDataGrouping[index] = mapGrouping
        }
    }

    private fun mappingChildConfig(listDataProps: MutableList<String>, lastDirName: String, lastConfigPath: String) {
        for (value in listDataProps) {
            if (lastDirName.contains(value)) {
                println("'$lastDirName' Contains $value ? [TRUE][MAPPED to '$value'] (V)")
                mapChildGrouping = mutableMapOf(value to lastConfigPath)

                listChildGrouping.add(mapChildGrouping)
            } else {
                val checkerTest = valueChecker(projectName, value)
                if (checkerTest != null) {
                    print("is $lastDirName Contains $checkerTest? ")
                    for (data in checkerTest) {
                        if (lastDirName.contains(data)) {
                            println("[TRUE][MAPPED to $value] (V)")

                            mapChildGrouping = mutableMapOf(value to lastConfigPath)
                            listChildGrouping.add(mapChildGrouping)
                        }
                    }
                }
            }
        }
    }

    private fun populateChildData(mapChildDataGroupings: MutableMap<Int, MutableList<MutableMap<String, String>>>, listNodesName: MutableList<File>?) {
        if (!listNodesName.isNullOrEmpty()) {
            println("Data Nodes Found! Populating Data Now...")

            for ((parentIndex, dirPaths) in listNodesName.withIndex()) {
                printListNode(parentIndex, dirPaths)

                if (!mapChildDataGroupings.isNullOrEmpty()) {
                    mapChildDataGroupings.forEach { (index, data) ->
                        if (parentIndex == index) {
                            var numbers = 1
                            var keyNames: String? = null

                            if (data.size < 2) {
                                stbAppendStyle("h4", String.format(strDirFoundsHTML, data.size, "Directory"))
                            } else {
                                stbAppendStyle("h4", String.format(strDirFoundsHTML, data.size, "Directories"))
                            }

                            for (listData in data) {
                                listData.forEach { (key, value) ->
                                    when {
                                        keyNames == null -> {
                                            keyNames = key
                                        }
                                        keyNames != key -> {
                                            numbers = 1
                                            keyNames = null
                                        }
                                        else -> {
                                            numbers++
                                        }
                                    }

                                    //Add DIV
                                    stbAppendStyle("div-open", "pj")
                                    stbAppendStyle("p", String.format(strConfigNameHTML, numbers, key))

                                    stbAppendStyle("table-open", null)

                                    var setPath = value
                                    if (setPath.contains("\\")) {
                                        setPath = setPath.replace("\\", "/")
                                    }

                                    val lastIndexOf = setPath.lastIndexOf("/")
                                    val pathLink = "<a href=\"$setPath\" target=\"_blank\">${setPath.substring(lastIndexOf + 1)}</a>"
                                    val pathTableData = mutableListOf<Any>("<p class=\"tableData\">$strPathName</p>", pathLink)
                                    stbAppendTableData(null, pathTableData)

                                    val filePath: File? = File(value)
                                    printListData(filePath, key)
                                }
                            }

                            stbAppendStyle("div-close", null)
                        }
                    }
                }
            }
        } else {
            println("No Data Node Founds")
        }
    }

    private fun populateData(mapData: MutableMap<Int, MutableMap<Int, String>>?, listNodesName: MutableList<File>?) {
        listNodesName?.let { nodes ->
            for ((parentIndex, dirPaths) in nodes.withIndex()) {
                printListNode(parentIndex, dirPaths)

                if (!mapData.isNullOrEmpty()) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            if (data.size < 2) {
                                stbAppendStyle("h4", String.format(strDirFoundsHTML, data.size, "Instance"))
                            } else {
                                stbAppendStyle("h4", String.format(strDirFoundsHTML, data.size, "Instance(s)"))
                            }

                            data.forEach { (key, value) ->
                                //Add DIV
                                stbAppendStyle("div-open", "pj")

                                stbAppendStyle("p", String.format(strInstanceNameHTML, key + 1))
                                stbAppendStyle("table-open", null)

                                var setPath = value
                                if (setPath.contains("\\")) {
                                    setPath = setPath.replace("\\", "/")
                                }

                                val lastIndexOf = setPath.lastIndexOf("/")
                                val pathLink = "<a href=\"$setPath\" target=\"_blank\">${setPath.substring(lastIndexOf + 1)}</a>"

                                val pathTableData = mutableListOf<Any>("<p class=\"tableData\">$strPathName</p>", pathLink)
                                stbAppendTableData(null, pathTableData)

                                val filePath: File? = File(value)
                                printListData(filePath, null)
                            }
                            stbAppendStyle("div-close", null)
                        }
                    }
                } else {
                    println("No Data Node Founds")
                }
            }
        }
    }

    private fun mappingConfig(index: Int, lastConfigPath: String, mapGrouping: MutableMap<Int, String>) {
        mapGrouping[index] = lastConfigPath
    }

    private fun printListNode(parentIndex: Int, dirPaths: File) {
        listNodesName?.let {
            it.forEachIndexed { index, nodeName ->
                if(index == parentIndex){
                    stbAppend("<hr>")
                    stbAppendStyle("div-open", "content")

                    stbAppendStyle("table-open", null)
                    val nameTableHeader = mutableListOf<Any>(strNodeNo, strNodeName)
                    stbAppendTableHeader(null, nameTableHeader)

                    println("dirPath -> $dirPaths")
                    val nodeNameTableData = mutableListOf<Any>((parentIndex + 1), "<mark><b>${nodeName}</b></mark>")
                    stbAppendTableData("center", nodeNameTableData)
                    stbAppendStyle("table-close", null)
                }
            }
        }
        /*stbAppend("<hr>")
        stbAppendStyle("div-open", "content")

        stbAppendStyle("table-open", null)
        val nameTableHeader = mutableListOf<Any>(strNodeNo, strNodeName)
        stbAppendTableHeader(null, nameTableHeader)

        println("dirPath -> $dirPaths")
        val nodeNameTableData = mutableListOf<Any>((parentIndex + 1), "<mark><b>${dirPaths.name}</b></mark>")
        stbAppendTableData("center", nodeNameTableData)
        stbAppendStyle("table-close", null)*/
    }

    private fun printListData(filePath: File?, key: String?) {
        filePath?.let {
            val getItemList = filePath.listFiles()
            if (getItemList != null && getItemList.isNotEmpty()) {
                saveActualItems(getItemList)

                saveExpectedItems(configFile, key)

                //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                compareData(filePath, listExpectedItems, listActualItems)
            }
        }

        //**Reset the List of Expected and Actual
        listExpectedItems.clear()
        listActualItems.clear()
        listActualFiles.clear()
    }

    private fun saveActualItems(itemList: Array<File>) {
        for (item in itemList) {
            listActualItems.add(item.name)

            //Checking MD5 Files
            listActualFiles.add(item.absoluteFile)
            /*val data = checkSumHelper.generateMD5Code(item.path)
            println("Data Name -> ${item.name}:: MD5 ->$data")*/
        }
    }

    private fun saveExpectedItems(configFile: File?, groupingKey: String?) {
        configFile?.let {
            val envStream = FileInputStream(it) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties

            val getPropertiesKey = properties.propertyNames() //Getting Key values from Properties
            while (getPropertiesKey.hasMoreElements()) {
                val keyValue = getPropertiesKey.nextElement().toString()
                if (properties.getProperty(keyValue) == "true") {
                    if (keyValue.contains("/") && groupingKey != null) {
                        val indexed = keyValue.lastIndexOf("/")
                        val firstKeyValue = keyValue.substring(0, indexed)
                        val lastKeyValue = keyValue.substring(indexed + 1)

                        if (firstKeyValue == groupingKey) {
                            listExpectedItems.add(lastKeyValue)
                        }
                    } else {
                        listExpectedItems.add(keyValue)
                    }
                }
            }
        }
    }

    private fun compareData(listFile: File?, listExpectedItems: MutableList<String>?, listActualItems: MutableList<String>?) {
        if (listExpectedItems != null && listActualItems != null) {
            isContentEquals(listExpectedItems, listActualItems).also {
                val outputMessage: String
                var notesOutputMessage: String? = null
                var expectedOutputMessage: String? = null

                totalConfigData += 1 //Count How Many Files Are Compared

                val expectedSize: Int = listExpectedItems.size
                val actualSize: Int = listActualItems.size

                if (it) {
                    passedConfigData += 1 //If Passed, Add Counter
                    val statusTableData = mutableListOf<Any>("<p class=\"tableData\">$strStatus</p>", "<p class=\"passed\">$strPassed</p>")
                    stbAppendTableData(null, statusTableData)
                    stbAppendStyle("table-close", null)

                    outputMessage = if (actualSize < 2) {
                        String.format(strConfigPassedHTML, expectedSize, "Data", "is")
                    } else {
                        String.format(strConfigPassedHTML, expectedSize, "Data(s)", "are")
                    }

                } else {
                    failedConfigData += 1 //If Failed Add Counter
                    val statusTableData = mutableListOf<Any>("<p class=\"tableData\">$strStatus</p>", "<p class=\"failed\">$strFailed</p>")
                    stbAppendTableData(null, statusTableData)
                    stbAppendStyle("table-close", null)

                    outputMessage = if (expectedSize > actualSize) {
                        val differenceSize = expectedSize - actualSize
                        if (differenceSize < 2) {
                            String.format(strConfigErrorNotMappingHTML, 1, "is")
                        } else {
                            String.format(strConfigErrorNotMappingHTML, differenceSize, "are")
                        }
                    } else {
                        val differenceSize = actualSize - expectedSize
                        if (differenceSize < 2) {
                            String.format(strConfigErrorNotBasedHTML, 1, "is")
                        } else {
                            String.format(strConfigErrorNotBasedHTML, differenceSize, "are")
                        }
                    }

                    notesOutputMessage = if (expectedSize < 2) {
                        String.format(strConfigErrorNotesHTML, "$expectedSize Item")
                    } else {
                        String.format(strConfigErrorNotesHTML, "$expectedSize Item(s)")
                    }

                    notesOutputMessage += if (actualSize < 2) {
                        String.format(strConfigErrorNotesActualHTML, "$actualSize Item")
                    } else {
                        String.format(strConfigErrorNotesActualHTML, "$actualSize Item(s)")
                    }

                    expectedOutputMessage = String.format(strConfigErrorExpectingHTML, listExpectedItems, listActualItems)

                    val listException = listExpectedItems.toMutableList()
                    val listActual = listActualItems.toMutableList()
                    val errorSummaries = ErrorSummaries(listFile, listException, listActual)

                    listErrorPath?.add(errorSummaries)
                }

                stbAppend(null)

                //Listing Items
                if (actualSize < 2) {
                    stbAppendStyle("p", String.format(strListItemHTML, "$actualSize Data"))
                } else {
                    stbAppendStyle("p", String.format(strListItemHTML, "$actualSize Data(s)"))
                }

                createListFileTable(listActualFiles)

                stbAppendStyle("p", outputMessage)
                if (!notesOutputMessage.isNullOrEmpty()) stbAppendStyle("p", notesOutputMessage)
                if (!expectedOutputMessage.isNullOrEmpty()) stbAppendStyle("p", expectedOutputMessage)
            }

            stbAppendStyle("div-close", null)
            stbAppend(null)
        }
    }

    private fun createListFileTable(listActualFiles: MutableList<File>?) {
        listActualFiles?.let {
            //Creating Table
            stbAppendStyle("table-open", null)
            val nameTableHeader = mutableListOf<Any>(strNo, strName, strSize, strMD5Code)
            stbAppendTableHeader(null, nameTableHeader)

            for ((index, data) in it.withIndex()) {
                val indexData = index + 1
                val fileName = data.name
                val sizeOfFile = FileUtils.byteCountToDisplaySize(data.length())
                val generateMD5 = checkSumHelper.generateMD5Code(data.absolutePath)

                val listItemTableData = mutableListOf<Any>(indexData, "<p class=\"listData\">$fileName</p>", sizeOfFile, generateMD5.toString())
                stbAppendTableData("center", listItemTableData)
            }
            stbAppendStyle("table-close", null)
        }
    }

    private fun errorSummaries(listErrorPath: MutableList<ErrorSummaries>?) {
        listErrorPath?.let {
            if (!it.isNullOrEmpty()) {
                stbAppendStyle("h4", strErrorList)

                stbAppendStyle("table-open", null)
                val nameTableHeader = mutableListOf<Any>(strNo, strErrorPathName, strExpectedHTML, strFoundHTML)
                stbAppendTableHeader(null, nameTableHeader)

                for ((index, dirPaths) in it.withIndex()) {
                    var errorPaths = dirPaths.errorPath.toString()
                    if (errorPaths.contains("\\")) {
                        errorPaths = errorPaths.replace("\\", "/")
                    }

                    val lastIndexOf = errorPaths.lastIndexOf("/")
                    val linkPath = "<a href =\"${errorPaths}\" target=\"_blank\"> ${errorPaths.substring(lastIndexOf + 1)}</a>"

                    val errorListTableData = mutableListOf<Any>((index + 1), linkPath, "<b>${dirPaths.listExpected}</b>", "${dirPaths.listActualItems}")
                    stbAppendTableData("center", errorListTableData)
                }

                stbAppendStyle("table-close", null)
                stbAppendStyle("p", strConfigErrorActionHTML)
            }
        }
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

    private fun populateProperties() {
        configFile?.let { config ->
            val envStream = FileInputStream(config) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties

            for (keys in properties.stringPropertyNames()) {
                if (properties.getProperty(keys) == "true") {
                    if (keys.contains("/")) {
                        val index = keys.lastIndexOf("/")
                        val firstValue = keys.substring(0, index)
                        //val lastValue = keys.substring(index + 1)

                        if (listDataProps!!.isEmpty()) {
                            listDataProps.add(firstValue)
                        } else {
                            if (!listDataProps.contains(firstValue)) {
                                listDataProps.add(firstValue)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generatedFile(projectName: String, flavorType: String, deployModels: String) {
        fileOutput = File("outputConfigTEST_${deployModels}.html") //Save not in VAR Folder..
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

        initHeader(projectName, configType, deployModels, listConfig!!.size)
    }

    private fun initHeader(projectName: String, configType: String, deployModels: String, size: Int) {
        stbAppendStyle("div-open", "pj")

        stbAppendStyle("h4", strConfigValidator)
        stbAppendStyle("table-open", null)

        val headerName = mutableListOf<Any>(strProjectName, strFlavorType, strNodeQuantity)
        stbAppendTableHeader(null, headerName)

        val tableData = mutableListOf<Any>("<p class=\"listData\">$projectName</p>", "<mark><b>$configType-$deployModels</b></mark>", size)
        stbAppendTableData("center", tableData)

        stbAppendStyle("table-close", null)

        val dateNow = LocalDateTime.now()
        stbAppendStyle("p", String.format(strGeneratedAt, dateFormatter.format(dateNow)))
        stbAppendStyle("h4", strConfigValidator)

        stbAppendStyle("div-close", null)
    }

    private fun writeFile() {
        println("Successfully Running the Config Validator!")
        stbAppendStyle("body-close", null)
        stbAppendStyle("html-close", null)

        writeToFile(stringBuilder.toString(), fileOutput)
        println("Creating Report File Successful!")
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
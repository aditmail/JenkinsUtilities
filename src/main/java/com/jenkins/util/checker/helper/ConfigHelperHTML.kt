package com.jenkins.util.checker.helper

import com.jenkins.util.checker.models.ErrorSummaries
import com.jenkins.util.checker.utils.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.ArrayList

class ConfigHelperHTML(private val args: Array<String>?) : IConfig.StringBuilders {

    //Data Files
    private var nodeDirFiles: File? = null //Path of Dir Config
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Init Helper
    private lateinit var projectName: String

    private lateinit var stringBuilder: StringBuilder
    private val properties = Properties()

    //Init List
    private val listActualItems: MutableList<String> = ArrayList()
    private val listExpectedItems: MutableList<String> = ArrayList()
    private val listChildGrouping: MutableList<MutableMap<String, String>> = ArrayList() //List of mapChildDataGrouping

    private val listDataProps: MutableList<String>? = ArrayList()
    private val listNodesName: MutableList<File>? = ArrayList()
    private val listErrorPath: MutableList<ErrorSummaries>? = ArrayList()

    //Init Parent Map
    private val mapChildDataGroupings: MutableMap<Int, MutableList<MutableMap<String, String>>> = mutableMapOf()
    private val mapDataGrouping: MutableMap<Int, MutableMap<Int, String>> = mutableMapOf()

    //Init Child Map
    private lateinit var mapChildGrouping: MutableMap<String, String>
    private lateinit var mapGrouping: MutableMap<Int, String>

    fun initFiles(projectName: String, configType: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        this.projectName = projectName

        //Init FileOutput
        fileOutput = File("$destinationPath/outputConfigs_${configType}.html")
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

        //Init Config File
        configFile = getFile(configPath)

        populateProperties()

        //Checking Data..
        checkMappings(nodeDirFiles, projectName, configType)
    }

    /*fun initFiles() {
        if (args?.size == 0 || args?.size != 4) {
            println("Please Input The Parameters That's are Needed")
            println("1st Params --> Project-Name (ex: klikBCAIndividu)")
            println("2nd Params --> Config-Type (ex: Pilot-APP)")
            println("3rd Params --> Nodes-Dir-Path (ex: ../KBI/PILOT/CONFIG/APP")
            println("4th Params --> Config_Path (ex: ..KBI/var/changes-config-app.txt")
        } else {
            projectName = args[0].trim()
            val configType = args[1].trim()
            val nodeDir = args[2].trim()
            val configPath = args[3].trim()

            //Init FileOutput
            fileOutput = File("var/outputConfig_${configType}.html")
            if (!fileOutput.exists()) {
                fileOutput.createNewFile()
            }
            stringBuilder = StringBuilder()

            //Init Config Dir
            nodeDirFiles = File(nodeDir)

            //Init Config File
            configFile = getFile(configPath)

            populateProperties()

            //Checking Data..
            checkMappings(nodeDirFiles, projectName, configType)
        }
    }*/

    private fun checkMappings(nodeDirFiles: File?, projectName: String, configType: String) {
        nodeDirFiles?.let { data ->
            val lists = data.listFiles() //Listing Files in Parameter Path
            if (lists != null && lists.isNotEmpty()) {

                initHtmlStyle(projectName, configType)
                initHeader(projectName, configType, lists)

                for ((index, dirPaths) in lists.withIndex()) {
                    listNodesName?.add(dirPaths)
                    val startParentPathing = Paths.get(dirPaths.absolutePath) //Start Listing

                    try {
                        val collect = getParentStreamList(startParentPathing)
                        collect?.let { parentList ->
                            for (configList in parentList) {
                                val configCollect = getConfigStreamList(configList)
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
                            }

                            if (!listDataProps.isNullOrEmpty()) {
                                val listChildPath = listChildGrouping.toMutableList()

                                //Inserting The Child Data Looping to Parent Mapping
                                mapChildDataGroupings[index] = listChildPath

                                //Reset the List Value to use for another loop
                                listChildGrouping.clear()
                            } else {
                                //Inserting The Data Looping to Parent Mapping
                                mapDataGrouping.put(index, mapGrouping)
                            }
                        }
                    } catch (e: IOException) {
                        println("Err:: ${e.message.toString()}")
                    }
                }

                if (!listDataProps.isNullOrEmpty()) {
                    populateChildData(mapChildDataGroupings, listNodesName)
                } else {
                    populateData(mapDataGrouping, listNodesName)
                }

            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }

            errorSummaries(listErrorPath)
            println("Successfully Running the Config Validator!")

            stbAppendStyle("body-close", null)
            stbAppendStyle("html-close", null)

            writeToFile(stringBuilder.toString(), fileOutput)
            println("Creating Report File!")
        }
    }

    private fun writeToFile(fileContent: String, fileOutput: File) {
        val outputStream = FileOutputStream(fileOutput.absoluteFile)
        val writer = OutputStreamWriter(outputStream)
        writer.write(fileContent)
        writer.close()
    }

    private fun populateChildData(mapChildDataGroupings: MutableMap<Int, MutableList<MutableMap<String, String>>>, listNodesName: MutableList<File>?) {
        if (!listNodesName.isNullOrEmpty()) {
            println("Data Nodes Found! Populating Data Now...")

            for ((parentIndex, dirPaths) in listNodesName.withIndex()) {
                printListNode(listNodesName, parentIndex, dirPaths)

                if (!mapChildDataGroupings.isNullOrEmpty()) {
                    mapChildDataGroupings.forEach { (index, data) ->
                        if (parentIndex == index) {
                            var numbers = 1
                            var keyNames: String? = null

                            if (data.size < 2) {
                                stbAppendStyle("h4",
                                        """${data.size} Directory Found 
|                                           <br>
|                                           **********************************
|                                           """.trimMargin())
                            } else {
                                stbAppendStyle("h4",
                                        """${data.size} Directories Found 
|                                           <br>
|                                           **********************************
|                                           """.trimMargin())
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

                                    stbAppendStyle("p", "&#8251; $numbers <u>Config</u> &#8658; <b>$key</b>")

                                    stbAppendStyle("table-open", null)

                                    val lastIndexOf = value.lastIndexOf("\\")
                                    val pathLink = "<a href=\"$value\" target=\"_blank\">${value.substring(lastIndexOf + 1)}</a>"
                                    val pathTableData = mutableListOf<Any>("&#10102;", "<p class=\"tableData\">Path Name</p>", pathLink)
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
                printListNode(listNodesName, parentIndex, dirPaths)
                stbAppend(null)

                if (!mapData.isNullOrEmpty()) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            data.forEach { (key, value) ->
                                stbAppendStyle("p", "<b>&#8656; &#8251;${key + 1} Instance &#8658;</b>")
                                stbAppendStyle("table-open", null)

                                val lastIndexOf = value.lastIndexOf("\\")
                                val pathLink = "<a href=\"$value\" target=\"_blank\">${value.substring(lastIndexOf + 1)}</a>"

                                val pathTableData = mutableListOf<Any>("&#10102;", "Path Name", pathLink)
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

    private fun getParentStreamList(startParentPathing: Path): List<String>? {
        val parentStream: Stream<Path> = Files.walk(startParentPathing, Int.MAX_VALUE) //Discovering the parentPath with Max value to its Last Subfolder
        return parentStream.map(java.lang.String::valueOf)
                .filter { it.endsWith("config") } //Filtering to get 'config' directory Only
                .sorted()
                .collect(Collectors.toList())
    }

    private fun getConfigStreamList(configPath: String): MutableList<String>? {
        val configStream: Stream<Path> = Files.walk(Paths.get(configPath), 1) //Discovering the configPath with Min value, jumping to Instance dir
        return configStream.map(java.lang.String::valueOf)
                .sorted()
                .collect(Collectors.toList())
    }

    private fun subStringDir(lastConfigPath: String): String {
        val fixPathDir = lastConfigPath.replace("/", "\\")
        val indexing = fixPathDir.lastIndexOf("\\")
        return fixPathDir.substring(indexing + 1) //Getting the Last Dir Name -> ex: from ~> C\TestPath\Test\Path || to ~> Path
    }

    /** Sorting Process..
     * if ListDataProps value are contains in the path looping
     * Then insert it into mapChildGrouping
     * ex: ListDataProps => [mklik, ibank]
     * if path -> '../mklik_bca_inter1' contains words in 'mklik' => insert to machildGrouping as 'key: mklik' && 'value: ../mklik_bca_inter1'
     * */
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

    private fun mappingConfig(index: Int, lastConfigPath: String, mapGrouping: MutableMap<Int, String>) {
        mapGrouping[index] = lastConfigPath
    }

    private fun printListNode(listNodesName: MutableList<File>, parentIndex: Int, dirPaths: File) {
        stbAppend("<hr>")
        stbAppendStyle("div-open", "content")

        stbAppendStyle("table-open", null)
        val nameTableHeader = mutableListOf<Any>("Node No.", "Node Name")
        stbAppendTableHeader(null, nameTableHeader)

        val nodeNameTableData = mutableListOf<Any>((parentIndex + 1), dirPaths.name)
        stbAppendTableData("center", nodeNameTableData)
        stbAppendStyle("table-close", null)
    }

    private fun printListData(filePath: File?, key: String?) {
        filePath?.let {
            val getItemList = filePath.listFiles()
            if (getItemList != null && getItemList.isNotEmpty()) {
                saveActualItems(getItemList)

                val itemFoundTableData = mutableListOf<Any>("&#10103;", "<p class=\"tableData\">Item Found</p>", "<p class=\"tableData\">${listActualItems.size}</p>")
                stbAppendTableData(null, itemFoundTableData)

                val listItemTableData = mutableListOf<Any>("&#10104;", "<p class=\"tableData\">List Items</p>", "<p class=\"listData\">$listActualItems</p>")
                stbAppendTableData(null, listItemTableData)

                saveExpectedItems(configFile, key)

                //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                compareData(filePath, listExpectedItems, listActualItems)
            }
        }

        //**Reset the List of Expected and Actual
        listExpectedItems.clear()
        listActualItems.clear()
    }

    private fun saveActualItems(itemList: Array<File>) {
        for (item in itemList) {
            listActualItems.add(item.name)
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
                val expectedSize: Int = listExpectedItems.size
                val actualSize: Int = listActualItems.size

                if (it) {
                    val statusTableData = mutableListOf<Any>("&#10105;", "<p class=\"tableData\">Status</p>", "<p class=\"passed\">PASSED</p>")
                    stbAppendTableData(null, statusTableData)
                    stbAppendStyle("table-close", null)

                    if (actualSize < 2) {
                        stbAppendStyle("p", "&#8258;<u>NOTES</u>:: <b>$expectedSize Data</b> from Config (.txt) is <b>Successfully Mapped</b> to Selected Directories &#9989;")
                    } else {
                        stbAppendStyle("p", "&#8258;<u>NOTES</u>:: <b>$expectedSize Data(s)</b> from Config (.txt) are <b>Successfully Mapped</b> to Selected Directories &#9989;")
                    }
                } else {
                    val statusTableData = mutableListOf<Any>("&#10105;", "<p class=\"tableData\">Status</p>", "<p class=\"failed\">FAILED</p>")
                    stbAppendTableData(null, statusTableData)
                    stbAppendStyle("table-close", null)

                    if (expectedSize > actualSize) {
                        val differenceSize = expectedSize - actualSize
                        if (differenceSize < 2) {
                            stbAppendStyle("p", "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>1 Data from Config</b> (.txt) That is <b>NOT Mapping</b> to Selected Directories &#10060;")
                        } else {
                            stbAppendStyle("p", "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>$differenceSize Data from Config</b> (.txt) That are <b>NOT Mapping</b> to Selected Directories &#10060;")
                        }
                    } else {
                        val differenceSize = actualSize - expectedSize
                        if (differenceSize < 2) {
                            stbAppendStyle("p", "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>1 Data</b> That is <b>NOT Based on the Config</b> (.txt) Mapped to Selected Directories &#10060;")
                        } else {
                            stbAppendStyle("p", "*<b style=\"color:red\">ERROR</b> &#8658; There's <b>$differenceSize Data</b> That are <b>NOT Based on the Config</b> (.txt) Mapped to Selected Directories &#10060;")
                        }
                    }
                    if (expectedSize < 2) {
                        stbAppendStyle("p-open", "*<b>NOTES</b> &#8658; <mark> <b>Expected &#8614; $expectedSize Item Mapped</b></mark> &#9474; ")
                    } else {
                        stbAppendStyle("p-open", "*<b>NOTES</b> &#8658; <mark> <b>Expected &#8614; $expectedSize Item(s) Mapped</b></mark> &#9474; ")
                    }

                    if (actualSize < 2) {
                        stbAppendStyle("p-close", "<b>Actual &#8614; $actualSize Item Mapped </b>")
                    } else {
                        stbAppendStyle("p-close", "<b>Actual &#8614; $actualSize Item(s) Mapped </b>")
                    }

                    stbAppendStyle("p", "&#8258;<b>EXPECTING</b> &#8658; <mark><b>$listExpectedItems</b></mark> but Found <b>$listActualItems</b> &#10071;")

                    val listException = listExpectedItems.toMutableList()
                    val listActual = listActualItems.toMutableList()
                    val errorSummaries = ErrorSummaries(listFile, listException, listActual)

                    listErrorPath?.add(errorSummaries)
                }
            }
            stbAppend(null)
        }
    }

    private fun errorSummaries(listErrorPath: MutableList<ErrorSummaries>?) {
        listErrorPath?.let {
            if (!it.isNullOrEmpty()) {
                stbAppend("<hr>")
                stbAppendStyle("div-open", "pj")
                stbAppendStyle("h4", "*************** ERROR SUMMARIES ***************")

                stbAppendStyle("table-open", null)
                val nameTableHeader = mutableListOf<Any>("No.", "Error Path Name", "Expected &#9989;", "Found &#10060;")
                stbAppendTableHeader(null, nameTableHeader)

                for ((index, dirPaths) in it.withIndex()) {
                    val lastIndexOf = dirPaths.errorPath.toString().lastIndexOf("\\")
                    val linkPath = "<a href =\"${dirPaths.errorPath}\" target=\"_blank\"> ${dirPaths.errorPath.toString().substring(lastIndexOf + 1)}</a>"

                    val errorListTableData = mutableListOf<Any>((index + 1), linkPath, "<b>${dirPaths.listExpected}</b>", "${dirPaths.listActualItems}")
                    stbAppendTableData("center", errorListTableData)
                }

                stbAppendStyle("table-close", null)
                stbAppendStyle("p", "&#8258;<b>ACTION</b> &#8658; Please <b>Check the Path/Jenkins Configuration</b> Again for Correction/Validation")
                stbAppendStyle("h4", "*************** ERROR SUMMARIES ***************")
                stbAppendStyle("div-close", null)
            }
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

    private fun initHeader(projectName: String, configType: String, lists: Array<File>) {
        stbAppendStyle("div-open", "pj")

        stbAppendStyle("h4", "*************** CONFIG VALIDATOR ***************")
        stbAppendStyle("table-open", null)

        val headerName = mutableListOf<Any>("Project Name", "Flavor-Type", "Node Quantity")
        stbAppendTableHeader(null, headerName)

        val tableData = mutableListOf<Any>(projectName, configType, lists.size)
        stbAppendTableData("center", tableData)

        stbAppendStyle("table-close", null)
        stbAppendStyle("h4", "*************** CONFIG VALIDATOR ***************")

        stbAppendStyle("div-close", null)
    }

    private fun initHtmlStyle(projectName: String, configType: String) {
        stbAppend("""<html>
                        <head>
                            <title>Config Validator $projectName | $configType </title>
                            <meta name ="viewport" content="width=device-width, initial-scale=1">
                            <style>
                                table, th, td {
                                    border-collapse: initial;
                                    text-align: left;
                                    vertical-align: middle;
                                    padding: 8px;
                                    border-radius: 4px;
                                }
                                
                                div{
                                    padding: 16px;
                                    display:inline-block;
                                    border-radius: 8px;
                                }
                                
                                div.pj{
                                    background-color: #ffe6cc;
                                }
                                
                                div.content{
                                    background-color: #f2f2f2;
                                }
                                
                                th.center{
                                    vertical-align: middle;
                                    text-align: center;
                                }
                                
                                td.center{
                                    text-align: center;
                                }

                                h4{
                                    margin-top: 16px;
                                    margin-bottom: 16px;
                                }
                                
                                h3 {
                                    margin-top: 4px;
                                    margin-bottom: 4px;
                                }
                                
                                p{
                                    margin-top: 4px;    
                                    margin-bottom: 4px;
                                }
                                
                                p.passed{
                                    font-weight: bold;
                                    color: green;
                                }
                                
                                p.failed{
                                    font-weight: bold;
                                    color: red;
                                }
                                
                                p.listData{
                                    font-weight: bold;
                                    color: blue;
                                }
                                
                                p.tableData{
                                    font-weight: bold;
                                }
                            </style>
                        </head>
                        <body>
                    """.trimIndent())
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
                "table-open" -> stringBuilder.append("<table border=\"1px;\" bordercolor=\"#000000;\">")
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
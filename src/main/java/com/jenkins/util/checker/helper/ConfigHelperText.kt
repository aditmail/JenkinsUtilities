package com.jenkins.util.checker.helper

import com.jenkins.util.checker.models.ErrorSummaries
import com.jenkins.util.checker.utils.*
import java.io.*
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream


class ConfigHelperText(private val args: Array<String>?) : IConfig.PrintWriter {

    //Data Files
    private var nodeDirFiles: File? = null //Path of Dir Config
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Init Helper
    private lateinit var projectName: String

    private lateinit var printWriter: PrintWriter
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

    //Init Counter
    private var totalConfigData = 0
    private var passedConfigData = 0
    private var failedConfigData = 0

    /*fun initFiles(projectName: String, configType: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        this.projectName = projectName

        //Init FileOutput
        fileOutput = File("$destinationPath/outputConfigs_${configType}.txt")
        if (!fileOutput.exists()) {
            fileOutput.createNewFile()
            println("Creating File:: $fileOutput")
        } else {
            println("File Already Exist:: $fileOutput")
            println("Existing Data Will be Rewrite")
        }
        printWriter = PrintWriter(FileOutputStream(fileOutput), true)

        //Init Config Dir
        nodeDirFiles = File(nodesDirPath)

        //Init Config File
        configFile = getFile(configPath)

        populateProperties()

        //Checking Data..
        checkMappings(nodeDirFiles, projectName, configType)
    }*/

    fun initFiles() {
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
            fileOutput = File("outputConfig_${configType}.txt")
            if (!fileOutput.exists()) {
                fileOutput.createNewFile()
            }
            printWriter = PrintWriter(FileOutputStream(fileOutput), true)

            //Init Config Dir
            nodeDirFiles = File(nodeDir)

            //Init Config File
            configFile = getFile(configPath)

            populateProperties()

            //Checking Data..
            checkMappings(nodeDirFiles, projectName, configType)
        }
    }

    private fun checkMappings(nodeDirFiles: File?, projectName: String, configType: String) {
        nodeDirFiles?.let { data ->
            val lists = data.listFiles() //Listing Files in Parameter Path
            if (lists != null && lists.isNotEmpty()) {
                pwLine("**********************************")
                pwLine("Project Name\t:: $projectName")
                pwLine("Flavor-Type\t:: $configType")

                if (lists.size < 2) {
                    pwLine("Node Quantity:: ${lists.size}")
                } else {
                    pwLine("Node(s) Quantity:: ${lists.size}")
                }
                pwLine("**********************************")
                pwLine(null)

                for ((index, dirPaths) in lists.withIndex()) {
                    listNodesName?.add(dirPaths)

                    val startParentPathing = Paths.get(dirPaths.path) //Start Listing
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

            summaryPercentage(totalConfigData, passedConfigData, failedConfigData)

            errorSummaries(listErrorPath)
            println("Successfully Running the Config Validator!")

            printWriter.close()
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
                        } else {
                            println("[FALSE][NOT MAPPED to '$value'] (X)")
                        }
                    }
                }
            }
        }
    }

    private fun mappingConfig(index: Int, lastConfigPath: String, mapGrouping: MutableMap<Int, String>) {
        mapGrouping[index] = lastConfigPath
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
                                pwLine("${data.size} Directory Found")
                            } else {
                                pwLine("${data.size} Directories Found(s)")
                            }
                            pwLine("----------------------------------")

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

                                    pwLine("[$numbers] Config <=== $key ===>")
                                    pwLine("A) Path :: $value")

                                    val filePath: File? = File(value)
                                    printListData(filePath, key)
                                }
                            }
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
                if (!mapData.isNullOrEmpty()) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            data.forEach { (key, value) ->
                                pwLine("<-- #${key + 1} Instance -->")
                                pwLine("A) Path :: $value")

                                val filePath: File? = File(value)
                                printListData(filePath, null)
                            }
                        }
                    }
                } else {
                    println("No Data Node Founds")
                }
            }
        }
    }

    private fun subStringDir(lastConfigPath: String): String {
        val fixPathDir = lastConfigPath.replace("/", "\\")
        val indexing = fixPathDir.lastIndexOf("\\")
        return fixPathDir.substring(indexing + 1) //Getting the Last Dir Name -> ex: from ~> C\TestPath\Test\Path || to ~> Path
    }

    private fun printListNode(listNodesName: MutableList<File>, parentIndex: Int, dirPaths: File) {
        printWriter.println("----------------------------------")
        if (listNodesName.size < 2) {
            pwLine("Node #${parentIndex + 1} :: ${dirPaths.name}")
        } else {
            pwLine("Node(s) #${parentIndex + 1} :: ${dirPaths.name}")
        }
        pwLine("----------------------------------")
    }

    private fun printListData(filePath: File?, key: String?) {
        filePath?.let {
            val getItemList = filePath.listFiles()
            if (getItemList != null && getItemList.isNotEmpty()) {

                saveActualItems(getItemList)

                pw("B) File\t:: ")
                if (listActualItems.size < 2) {
                    pwLine("(${listActualItems.size}) Item Found in Directory!") //How many files found
                } else {
                    pwLine("(${listActualItems.size}) Item(s) Found in Directory!")
                }
                pwLine("C) List of File\t:: $listActualItems") //Printing the list of file name

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
                totalConfigData += 1 //Count How Many Files Are Compared

                val expectedSize: Int = listExpectedItems.size
                val actualSize: Int = listActualItems.size

                if (it) {
                    passedConfigData += 1 //If Passed, Add Counter

                    if (actualSize < 2) {
                        pwLine("**PASSED --> $expectedSize Data from Config (.txt) is Successfully Mapped to Selected Directories")
                    } else {
                        pwLine("**PASSED --> $expectedSize Data(s) from Config (.txt) are Successfully Mapped to Selected Directories")
                    }
                } else {
                    failedConfigData += 1 //If Failed Add Counter

                    if (expectedSize > actualSize) {
                        val differenceSize = expectedSize - actualSize
                        if (differenceSize < 2) {
                            pwLine("**ERROR --> There's 1 Data from Config (.txt) That is NOT Mapping to Selected Directories")
                        } else {
                            pwLine("**ERROR --> There's $differenceSize Data from Config (.txt) That are NOT Mapping to Selected Directories")
                        }
                    } else {
                        val differenceSize = actualSize - expectedSize
                        if (differenceSize < 2) {
                            pwLine("**ERROR --> There's 1 Data That is NOT Based on the Config (.txt) Mapped to Selected Directories")
                        } else {
                            pwLine("**ERROR --> There's $differenceSize Data That are NOT Based on the Config (.txt) Mapped to Selected Directories")
                        }
                    }
                    if (expectedSize < 2) {
                        pw("**ERROR --> Expected :: $expectedSize Item Mapped || ")
                    } else {
                        pw("**ERROR --> Expected :: $expectedSize Item(s) Mapped || ")
                    }

                    if (actualSize < 2) {
                        pwLine("Actual -> $actualSize Item Mapped")
                    } else {
                        pwLine("Actual -> $actualSize Item(s) Mapped")
                    }
                    pwLine("==============================================================")
                    pwLine("**EXPECTING --> $listExpectedItems but Found $listActualItems")
                    pwLine("==============================================================")

                    val listException = listExpectedItems.toMutableList()
                    val listActual = listActualItems.toMutableList()
                    val errorSummaries = ErrorSummaries(listFile, listException, listActual)

                    listErrorPath?.add(errorSummaries)
                }
            }
            printWriter.println()
        }
    }

    private fun summaryPercentage(totalValue: Int, passedValue: Int, failedValue: Int) {
        if (totalValue != 0) {
            pwLine("*************** REPORT SUMMARIES ***************")

            val decFormat = DecimalFormat("#.##")
            decFormat.roundingMode = RoundingMode.CEILING

            val successPercentage = (passedValue / totalValue.toDouble()) * 100f
            val failedPercentage = (failedValue / totalValue.toDouble()) * 100f
            val successful = decFormat.format(successPercentage)
            val failed = decFormat.format(failedPercentage)

            pwLine("***Total Data:: $totalValue")
            pwLine("***Total Passed Data:: $passedValue")
            pwLine("***Total Failed Data:: $failedValue")

            pwLine("-----------------------------------------")
            pwLine("TOTAL PERCENTAGE")
            pwLine("SUCCESS ---> $successful%")
            pwLine("FAILED ---> $failed%")
            pwLine("-----------------------------------------")
        }
    }

    private fun errorSummaries(listErrorPath: MutableList<ErrorSummaries>?) {
        listErrorPath?.let {
            if (!it.isNullOrEmpty()) {
                pwLine(null)
                pwLine("ERROR LIST")
                for ((index, dirPaths) in it.withIndex()) {
                    if (it.size < 2) {
                        pwLine("<-- #${index + 1} Error -->")
                    } else {
                        pwLine("<-- #${index + 1} Error(s) -->")
                    }
                    pwLine("-> Path :: ${dirPaths.errorPath}")
                    pwLine("-> EXPECTED :: ${dirPaths.listExpected} || FOUND :: ${dirPaths.listActualItems}")
                    pwLine(null)
                }
                pwLine("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                pwLine("*************** REPORT SUMMARIES ***************")
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

    override fun pwLine(value: String?) {
        if (value == null) {
            printWriter.println()
        } else {
            printWriter.println(value)
        }
    }

    override fun pw(value: String) {
        printWriter.print(value)
    }
}
package com.jenkins.util.checker.helper

import com.jenkins.util.checker.models.ErrorSummaries
import com.jenkins.util.checker.utils.IConfig
import com.jenkins.util.checker.utils.checkConfigDirectory
import com.jenkins.util.checker.utils.getFile
import com.jenkins.util.checker.utils.isContentEquals
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.ArrayList

class ConfigHelper(private val args: Array<String>?) : IConfig {

    //Data Files
    private var nodeDirFiles: File? = null //Path of Dir Config
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Init Helper
    private lateinit var printWriter: PrintWriter
    private val properties = Properties()

    //Init List
    private val listActualItems: MutableList<String> = ArrayList() //List Actual Item Files in Dir
    private val listExpectedItems: MutableList<String> = ArrayList() //List Expected Item Files in Dir

    private val listDataProps: MutableList<String>? = ArrayList()
    private val listNodesName: MutableList<File>? = ArrayList()
    private val listErrorPath: MutableList<ErrorSummaries>? = ArrayList()

    //Init Parent Map
    private val listSummaries: MutableMap<File, MutableMap<MutableList<String>, MutableList<String>>> = mutableMapOf()
    private val mapChildDataGrouping: MutableMap<Int, MutableMap<String, String>> = mutableMapOf()
    private val mapDataGrouping: MutableMap<Int, MutableMap<Int, String>> = mutableMapOf()

    //Init Child Map
    private lateinit var mapSummaries: MutableMap<MutableList<String>, MutableList<String>>
    private lateinit var mapChildGrouping: MutableMap<String, String>
    private lateinit var mapGrouping: MutableMap<Int, String>

    //Init ErrorSummary
    private lateinit var errorSummaries: ErrorSummaries

    fun initFiles(flavor: String, configType: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        //Init FileOutput
        fileOutput = File("$destinationPath/outputConfig_${configType}.txt")
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
        checkMappings(nodeDirFiles, flavor, configType)
    }

    fun initFiles() {
        if (args?.size == 0 || args?.size != 3) {
            println("Please Input The Parameters That's are Needed")
            println("1st Params --> Build_Flavor")
            println("2nd Params --> Config_Type")
            println("3rd Params --> Nodes_Dir_Path")
            println("4th Params --> Config_Path")
        } else {
            val buildFlavor = args[0].trim()
            val configType = args[1].trim()
            val nodeDir = args[2].trim()
            val configPath = args[3].trim()

            //Init FileOutput
            fileOutput = File("var/outputConfig_${configType}.txt")
            if (!fileOutput.exists()) {
                fileOutput.createNewFile()
            }
            printWriter = PrintWriter(FileOutputStream(fileOutput), true)

            //Init Config Dir
            nodeDirFiles = File(nodeDir)

            //Init Config File
            configFile = getFile(configPath)

            //Checking Data..
            checkMappings(nodeDirFiles, buildFlavor, configType)
        }
    }

    private fun checkMappings(nodeDirFiles: File?, flavor: String, configType: String) {
        nodeDirFiles?.let { data ->
            val lists = data.listFiles() //Listing Files in Parameter Path
            if (lists != null && lists.isNotEmpty()) {
                pwLine("**********************************")
                pwLine("Build Flavor\t:: $flavor")
                pwLine("Config Type\t:: $configType")

                if (lists.size < 2) {
                    pwLine("Node Quantity:: ${lists.size}")
                } else {
                    pwLine("Node(s) Quantity:: ${lists.size}")
                }
                pwLine("**********************************")
                pwLine(null)

                for ((index, dirPaths) in lists.withIndex()) {
                    listNodesName?.add(dirPaths)

                    val startParentPathing = Paths.get(dirPaths.absolutePath) //Start Listing
                    try {
                        val collect = getParentStreamList(startParentPathing)
                        collect?.let { parentList ->
                            val configPath = parentList[0] //Since it 'listing' and 'filtering' occurs, the path will be in '0' Index
                            val configCollect = getConfigStreamList(configPath)

                            if (!listDataProps.isNullOrEmpty()) {
                                mapChildGrouping = mutableMapOf() //Init Map to Hold Child Values
                            } else {
                                mapGrouping = mutableMapOf() //Init Map to Hold Values
                            }

                            configCollect?.let { childList ->
                                childList.removeAt(0) //Remove Parent Dir
                                childList.forEachIndexed { index, lastConfigPath ->
                                    if (checkConfigDirectory(lastConfigPath)) {
                                        val getLastDirName = subStringDir(lastConfigPath)
                                        if (!listDataProps.isNullOrEmpty()) {
                                            mappingChildConfig(listDataProps, getLastDirName, mapChildGrouping, lastConfigPath)
                                        } else {
                                            mappingConfig(index, lastConfigPath, mapGrouping)
                                        }
                                    }
                                }

                                if (!listDataProps.isNullOrEmpty()) {
                                    mapChildDataGrouping.put(index, mapChildGrouping) //Inserting The Child Data Looping to Parent Mapping
                                } else {
                                    mapDataGrouping.put(index, mapGrouping) //Inserting The Data Looping to Parent Mapping
                                }
                            }
                        }
                    } catch (e: IOException) {
                        println("Err:: ${e.message.toString()}")
                    }
                }

                if (!listDataProps.isNullOrEmpty()) {
                    populateChildData(mapChildDataGrouping, listNodesName)
                } else {
                    populateData(mapDataGrouping, listNodesName)

                }
            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }
        }

        //printWriter.close()
        errorSummaries(listErrorPath)
        println("ERR:: $listErrorPath")
        println("Successfully Running the Config Validator!")
    }

    private fun populateData(mapData: MutableMap<Int, MutableMap<Int, String>>?, listNodesName: MutableList<File>?) {
        listNodesName?.let { nodes ->
            for ((parentIndex, dirPaths) in nodes.withIndex()) {

                printListNode(listNodesName, parentIndex, dirPaths)

                if (!mapData.isNullOrEmpty()) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            data.forEach { (key, value) ->
                                printWriter.println("<-- #${key + 1} Instance -->")
                                printWriter.println("A) Path :: $value")

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

    private fun populateChildData(mapData: MutableMap<Int, MutableMap<String, String>>?, listNodesName: MutableList<File>?) {
        if (!listNodesName.isNullOrEmpty()) {
            println("Data Nodes Found! Populating Data Now...")
            for ((parentIndex, dirPaths) in listNodesName.withIndex()) {
                printListNode(listNodesName, parentIndex, dirPaths)

                if (!mapData.isNullOrEmpty()) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            data.forEach { (key, value) ->
                                pwLine("Config <=== $key ===>")
                                pwLine("A) Path :: $value")

                                val filePath: File? = File(value)
                                printListData(filePath, key)
                            }
                        }
                    }
                }
            }
        } else {
            println("No Data Node Founds")
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
        val fixPathDir = lastConfigPath.replace("/", "\\") //Replacing Path ('\') -> ex: from ~> C:/TestPath || to ~> C:\TestPath
        val indexing = fixPathDir.lastIndexOf("\\") //Indexing Path based On '\' -> ex: C\TestPath\Test\Path
        return fixPathDir.substring(indexing + 1) //Getting the Last Dir Name -> ex: from ~> C\TestPath\Test\Path || to ~> Path
    }

    private fun mappingChildConfig(listDataProps: MutableList<String>, lastDirName: String, mapChildGrouping: MutableMap<String, String>, lastConfigPath: String) {
        for (value in listDataProps) {
            if (lastDirName.contains(value)) {
                println("Found:: $value in Config Properties --> $lastDirName")
                mapChildGrouping[value] = lastConfigPath
            }
        }
    }

    private fun mappingConfig(index: Int, lastConfigPath: String, mapGrouping: MutableMap<Int, String>) {
        mapGrouping[index] = lastConfigPath
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
                    if (actualSize < 2) {
                        pwLine("**PASSED --> $expectedSize Data from Config (.txt) is Successfully Mapped to Selected Directories")
                    } else {
                        pwLine("**PASSED --> $expectedSize Data(s) from Config (.txt) are Successfully Mapped to Selected Directories")
                    }
                } else {
                    mapSummaries = mutableMapOf()
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

                    errorSummaries = ErrorSummaries(listFile, listExpectedItems, listActualItems)
                    listErrorPath?.add(errorSummaries)
                    //println(errorSummaries)
                    println("List: $listErrorPath")
                    //printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                }
            }
            printWriter.println()
        }
    }

    private fun errorSummaries(listErrorPath: MutableList<ErrorSummaries>?) {
        listErrorPath?.let {
            if (!it.isNullOrEmpty()) {
                pwLine(" ===================== ERROR SUMMARIES ===================== ")
                for ((index, dirPaths) in it.withIndex()) {
                    pwLine("Path #$index:: ${dirPaths.errorPath}")
                    pwLine("Expecting ${dirPaths.listExpected} but Found ${dirPaths.listActualItems}")
                }
            }
        }
    }

    private fun populateProperties() {
        configFile?.let { config ->
            val envStream = FileInputStream(config) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties

            val keyProps = properties.propertyNames() //Getting Key values from Properties
            while (keyProps.hasMoreElements()) { //Iteration
                val keys = keyProps.nextElement().toString()
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
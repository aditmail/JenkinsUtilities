package com.jenkins.util.checker.helper

import com.jenkins.util.checker.utils.checkConfigDirectory
import com.jenkins.util.checker.utils.getFile
import com.jenkins.util.checker.utils.isEqual
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.ArrayList

class ConfigHelper(private val args: Array<String>?) {

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

    //Init Map
    private val mapDataGrouping: MutableMap<Int, MutableMap<String, String>> = mutableMapOf()

    fun initFiles(flavor: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        //Init FileOutput
        fileOutput = File("$destinationPath/outputConfig_WEB.txt")
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
        checkMappings(nodeDirFiles, flavor)
    }

    fun initFiles() {
        if (args?.size == 0 || args?.size != 3) {
            println("Please Input The Parameters That's are Needed")
            println("1st Params --> Build_Flavor")
            println("2nd Params --> Nodes_Dir_Path")
            println("3rd Params --> Config_Path")
        } else {
            //Init FileOutput
            fileOutput = File("var/outputConfig_WEB.txt")
            if (!fileOutput.exists()) {
                fileOutput.createNewFile()
            }
            printWriter = PrintWriter(FileOutputStream(fileOutput), true)

            //Init Config Dir
            nodeDirFiles = File(args[1].trim())

            //Init Config File
            configFile = getFile(args[2].trim())

            //Checking Data..
            checkMappings(nodeDirFiles, args[0].trim())
        }
    }

    private fun checkMappings(nodeDirFiles: File?, flavor: String) {
        nodeDirFiles?.let { data ->
            val lists = data.listFiles() //Listing Files in Parameter Path
            if (lists != null && lists.isNotEmpty()) {
                printWriter.println("----------------------------------")
                printWriter.println("Build Flavor:: $flavor")

                if (lists.size < 2) {
                    printWriter.println("Node Quantity:: (${lists.size})")
                } else {
                    printWriter.println("Node(s) Quantity:: (${lists.size})")
                }

                printWriter.println("----------------------------------")
                for ((index, dirPaths) in lists.withIndex()) {
                    listNodesName?.add(dirPaths)

                    val startParentPathing = Paths.get(dirPaths.absolutePath) //Start Listing
                    try {
                        val collect = getParentStreamList(startParentPathing)
                        collect?.let { parentList ->
                            val configPath = parentList[0] //Since it 'listing' and 'filtering' occurs, the path will be in '0' Index
                            val configCollect = getConfigStreamList(configPath)
                            val mapChildGrouping: MutableMap<String, String> = mutableMapOf() //Init Map to Hold Values

                            configCollect?.let { childList ->
                                childList.removeAt(0) //Remove Parent Dir
                                childList.forEachIndexed { _, lastConfigPath ->
                                    if (checkConfigDirectory(lastConfigPath)) {
                                        val getLastDirName = fixPathDirectory(lastConfigPath)
                                        mappingConfig(getLastDirName, mapChildGrouping, lastConfigPath)
                                    }
                                }

                                mapDataGrouping.put(index, mapChildGrouping) //Inserting The Child Data Looping to Parent Mapping
                            }
                        }
                    } catch (e: IOException) {
                        println("Err:: ${e.message.toString()}")
                    }
                }

                populateData(mapDataGrouping, listNodesName)
            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }
        }

        //printWriter.close()
        println("Successfully Running the Config Validator!")
    }

    private fun populateData(mapData: MutableMap<Int, MutableMap<String, String>>?, listNodesName: MutableList<File>?) {
        if (listNodesName != null) {
            println("Data Nodes Found! Populating Data Now...")
            for ((parentIndex, dirPaths) in listNodesName.withIndex()) {
                printWriter.println()
                if (listNodesName.size < 2) {
                    printWriter.println("Node #${parentIndex + 1} :: ${dirPaths.name}")
                } else {
                    printWriter.println("Node(s) #${parentIndex + 1} :: ${dirPaths.name}")
                }

                if (mapData != null) {
                    mapData.forEach { (index, data) ->
                        if (parentIndex == index) {
                            data.forEach { (key, value) ->
                                printWriter.println("Config <=== $key ===>")
                                printWriter.println("A) Path :: $value")

                                val filePath: File? = File(value)
                                printListData(filePath, key)
                            }
                        }
                    }
                } else {
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

    private fun fixPathDirectory(lastConfigPath: String): String {
        val fixPathDir = lastConfigPath.replace("/", "\\") //Replacing Path ('\') -> ex: from ~> C:/TestPath || to ~> C:\TestPath
        val indexing = fixPathDir.lastIndexOf("\\") //Indexing Path based On '\' -> ex: C\TestPath\Test\Path
        return fixPathDir.substring(indexing + 1) //Getting the Last Dir Name -> ex: from ~> C\TestPath\Test\Path || to ~> Path
    }

    private fun mappingConfig(lastDirName: String, mapChildGrouping: MutableMap<String, String>, lastConfigPath: String) {
        if (listDataProps != null) {
            for (value in listDataProps) {
                if (lastDirName.contains(value)) {
                    println("Found:: $value in Config Properties --> $lastDirName")
                    mapChildGrouping[value] = lastConfigPath
                }
            }
        } else {
            println("No Child/Multiple Value in Properties (Config [.txt])")
        }
    }

    private fun printListData(filePath: File?, key: String?) {
        filePath?.let {
            val getItemList = filePath.listFiles()
            if (getItemList != null && getItemList.isNotEmpty()) {

                saveActualItems(getItemList)

                printWriter.print("B) File\t:: ")
                if (listActualItems.size < 2) {
                    printWriter.println("(${listActualItems.size}) Item Found in Directory!") //How many files found
                } else {
                    printWriter.println("(${listActualItems.size}) Item(s) Found in Directory!")
                }
                printWriter.println("C) List of File\t:: $listActualItems") //Printing the list of file name

                saveExpectedItems(configFile, key)

                //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                compareData(listExpectedItems, listActualItems)
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

                    }
                }
            }
        }
    }

    private fun compareData(listExpectedItems: MutableList<String>?, listActualItems: MutableList<String>?) {
        if (listExpectedItems != null && listActualItems != null) {
            isEqual(listExpectedItems, listActualItems).also {
                val expectedSize: Int = listExpectedItems.size
                val actualSize: Int = listActualItems.size

                if (it) {
                    if (listActualItems.size < 2) {
                        printWriter.println("**PASSED --> $expectedSize Data from Config (.txt) is Successfully Mapped to Selected Directories")
                    } else {
                        printWriter.println("**PASSED --> $expectedSize Data(s) from Config (.txt) are Successfully Mapped to Selected Directories")
                    }
                    printWriter.println()
                } else {
                    if (expectedSize > actualSize) {
                        val differenceSize = expectedSize - actualSize
                        if (differenceSize < 2) {
                            printWriter.println("**ERROR --> There's 1 Data from Config (.txt) That is NOT Mapping to Selected Directories")
                        } else {
                            printWriter.println("**ERROR --> There's $differenceSize Data from Config (.txt) That are NOT Mapping to Selected Directories")
                        }
                    } else {
                        val differenceSize = actualSize - expectedSize
                        if (differenceSize < 2) {
                            printWriter.println("**ERROR --> There's 1 Data That is NOT Based on the Config (.txt) Mapped to Selected Directories")
                        } else {
                            printWriter.println("**ERROR --> There's $differenceSize Data That are NOT Based on the Config (.txt) Mapped to Selected Directories")
                        }
                    }
                    if (expectedSize < 2) {
                        printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedItems")
                    } else {
                        printWriter.println("**EXPECTED --> (${listExpectedItems.size}) File(s) in Directory :: $listExpectedItems")
                    }
                    printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                }
            }
        }
    }

    private fun findPropertiesFiles(lastConfigPath: String) {
        File(lastConfigPath).also {
            val lists = it.listFiles() //Listing Files in end of Path (to get .properties files)
            if (lists != null && lists.isNotEmpty()) {
                for (file in lists) {
                    if (file.path.contains("mklik")) {
                        println("true --> ${file.path}")
                    }
                }
                printWriter.print("B) File\t:: ")
                if (listActualItems.size < 2) {
                    printWriter.println("(${listActualItems.size}) Item Found in Directory!") //How many files found
                } else {
                    printWriter.println("(${listActualItems.size}) Item(s) Found in Directory!")
                }
                printWriter.println("C) List of File\t:: $listActualItems") //Printing the list of file name

                checkConfigStatus(listActualItems) //Go to 'checkConfigStatus' function
                printWriter.println("----------------------------------------------------")
            }
        }
    }

    private fun checkConfigStatus(listActualProps: MutableList<String>) {
        configFile?.let { config ->
            val envStream = FileInputStream(config) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties

            val keyProps = properties.propertyNames() //Getting Key values from Properties
            while (keyProps.hasMoreElements()) { //Iteration
                val keys = keyProps.nextElement().toString()
                if (properties.getProperty(keys) == "true")
                    listExpectedItems.add(keys) //Adding to List
            }

            //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
            isEqual(listExpectedItems, listActualProps).also {
                val expectedSize: Int = listExpectedItems.size
                val actualSize: Int = listActualProps.size

                if (it) {
                    if (listActualProps.size < 2) {
                        printWriter.println("**PASSED --> $expectedSize Data from Config (.txt) is Successfully Mapped to Selected Directories")
                    } else {
                        printWriter.println("**PASSED --> $expectedSize Data(s) from Config (.txt) are Successfully Mapped to Selected Directories")
                    }
                    printWriter.println()
                } else {
                    if (expectedSize > actualSize) {
                        val differenceSize = expectedSize - actualSize
                        if (differenceSize < 2) {
                            printWriter.println("**ERROR --> There's 1 Data from Config (.txt) That is NOT Mapping to Selected Directories")
                        } else {
                            printWriter.println("**ERROR --> There's $differenceSize Data from Config (.txt) That are NOT Mapping to Selected Directories")
                        }
                    } else {
                        val differenceSize = actualSize - expectedSize
                        if (differenceSize < 2) {
                            printWriter.println("**ERROR --> There's 1 Data That is NOT Based on the Config (.txt) Mapped to Selected Directories")
                        } else {
                            printWriter.println("**ERROR --> There's $differenceSize Data That are NOT Based on the Config (.txt) Mapped to Selected Directories")
                        }
                    }
                    if (expectedSize < 2) {
                        printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedItems")
                    } else {
                        printWriter.println("**EXPECTED --> (${listExpectedItems.size}) File(s) in Directory :: $listExpectedItems")
                    }
                    printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                }
            }

            listExpectedItems.clear()
            listActualProps.clear()
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
                        val lastValue = keys.substring(index + 1)

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
}
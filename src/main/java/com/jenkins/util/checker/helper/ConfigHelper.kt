package com.jenkins.util.checker.helper

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

    //Properties Helper
    private val properties = Properties()
    private val listActualProps: MutableList<String> = ArrayList()
    private val listExpectedProps: MutableList<String> = ArrayList()

    //Data Files
    private var nodeDirFiles: File? = null //Path of Dir Config
    private var configFile: File? = null //File of Config-App.txt from Var Dir
    private lateinit var fileOutput: File //Path + Filename of Output Config Validator

    //Files Helper
    private lateinit var printWriter: PrintWriter

    //List Array for Files...
    private val listFiles: MutableList<String> = ArrayList()
    private val listNodes: MutableList<File> = ArrayList()

    private val grouping: MutableMap<Int, MutableMap<String, String>> = mutableMapOf()

    fun initFiles(flavor: String, nodesDirPath: String, configPath: String, destinationPath: String) {
        //Init FileOutput
        fileOutput = File("$destinationPath/outputConfig_WEB.txt")
        if (!fileOutput.exists()) {
            fileOutput.createNewFile()
            println("Creating File:: $fileOutput")
        } else {
            println("File Already Exist:: $fileOutput")
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
                    /*printWriter.println()
                    if (lists.size < 2) {
                        printWriter.println("Node #${index + 1} :: ${dirPaths.name}")
                    } else {
                        printWriter.println("Node(s) #${index + 1} :: ${dirPaths.name}")
                    }*/
                    listNodes.add(dirPaths)

                    val startParentPathing = Paths.get(dirPaths.absolutePath) //Start Listing
                    try {
                        val parentStream: Stream<Path> = Files.walk(startParentPathing, Int.MAX_VALUE) //Discovering the parentPath with Max value to its Last Subfolder
                        val collect: List<String> = parentStream.map(java.lang.String::valueOf)
                                .filter { it.endsWith("config") } //Filtering to get 'config' directory Only
                                .sorted()
                                .collect(Collectors.toList())

                        val configPath = collect[0] //Since it 'listing' and 'filtering' occurs, the path will be in '0' Index
                        val configStream: Stream<Path> = Files.walk(Paths.get(configPath), 1) //Discovering the configPath with Min value, jumping to Instance dir
                        val configCollect: MutableList<String>? = configStream.map(java.lang.String::valueOf)
                                .sorted()
                                .collect(Collectors.toList())

                        val groupes: MutableMap<String, String> = mutableMapOf()
                        configCollect?.let {
                            it.removeAt(0)
                            //Count Instance Qty, (-1 because the root are counted in it)
                            it.forEachIndexed { index, lastConfigPath ->
                                if (lastConfigPath.contains("/") || lastConfigPath.contains("\\")) {
                                    val cleanLastConfigPath = lastConfigPath.replace("/", "\\")
                                    val indexing = cleanLastConfigPath.lastIndexOf("\\")
                                    val lastDir = cleanLastConfigPath.substring(indexing + 1)

                                    for (datas in listFiles) {
                                        if (lastDir.contains(datas)) {
                                            groupes[datas] = lastConfigPath
                                        }
                                    }
                                }
                                /*if (it.size < 2) {
                                    printWriter.println("<-- Instance #${index + 1} -->")
                                } else {
                                    printWriter.println("<-- Instance(s) #${index + 1} -->")
                                }*/
                                //findPropertiesFiles(lastConfigPath) //Go to 'findPropertiesFiles' function
                            }
                            //println("Grouping Child :: $groupes")
                            //testingGroups(groupes, index)

                            //println("Grouping Child :: $groupes")
                            grouping.put(index, groupes)
                        }
                    } catch (e: IOException) {
                        println("Err:: ${e.message.toString()}")
                    }
                }

                testingGroupingParent(grouping)
                println("Grouping Parent:: $grouping")
            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }
        }

        //printWriter.close()
        println("Successfully Running the Config Validator!")
    }

    private fun testingGroupingParent(grouping: MutableMap<Int, MutableMap<String, String>>) {
        for ((parentIndex, dirPaths) in listNodes.withIndex()) {
            printWriter.println()
            if (listNodes.size < 2) {
                printWriter.println("Node #${parentIndex + 1} :: ${dirPaths.name}")
            } else {
                printWriter.println("Node(s) #${parentIndex + 1} :: ${dirPaths.name}")
            }

            grouping.forEach { (index, data) ->
                if (parentIndex == index) {
                    data.forEach { (key, value) ->
                        printWriter.println("Config <=== $key ===>")
                        printWriter.println("A) Path :: $value")

                        val filePath: File? = File(value)
                        filePath?.let {
                            val getItemList = filePath.listFiles()
                            if (getItemList != null && getItemList.isNotEmpty()) {
                                for (item in getItemList) {
                                    listActualProps.add(item.name)
                                }

                                printWriter.print("B) File\t:: ")
                                if (listActualProps.size < 2) {
                                    printWriter.println("(${listActualProps.size}) Item Found in Directory!") //How many files found
                                } else {
                                    printWriter.println("(${listActualProps.size}) Item(s) Found in Directory!")
                                }
                                printWriter.println("C) List of File\t:: $listActualProps") //Printing the list of file name

                                configFile?.let { config ->
                                    val envStream = FileInputStream(config) //Load Config Properties from Params
                                    properties.load(envStream) //Load as Properties

                                    val keyProps = properties.propertyNames() //Getting Key values from Properties
                                    while (keyProps.hasMoreElements()) {
                                        val propKeys = keyProps.nextElement().toString()
                                        if (properties.getProperty(propKeys) == "true") {
                                            if (propKeys.contains("/")) {
                                                val indexed = propKeys.lastIndexOf("/")
                                                val firstValue = propKeys.substring(0, indexed)
                                                val lastValue = propKeys.substring(indexed + 1)

                                                if (firstValue == key) {
                                                    listExpectedProps.add(lastValue)
                                                }
                                            }
                                        }
                                    }

                                    //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                                    isEqual(listExpectedProps, listActualProps).also {
                                        val expectedSize: Int = listExpectedProps.size
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
                                                printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedProps")
                                            } else {
                                                printWriter.println("**EXPECTED --> (${listExpectedProps.size}) File(s) in Directory :: $listExpectedProps")
                                            }
                                            printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                                        }
                                    }
                                }
                            }

                            listExpectedProps.clear()
                            listActualProps.clear()
                        }
                    }
                }
            }
        }
    }

    private fun testingGroups(groupes: MutableMap<String, String>, index: Int) {
        if (groupes.size < 2) {
            printWriter.println("<-- Instance #${index + 1} -->")
        } else {
            printWriter.println("<-- Instance(s) #${index + 1} -->")
        }

        groupes.forEach { (key, value) ->
            printWriter.println("Config <=== $key ===>")
            printWriter.println("A) Path :: $value")
            val filePath: File? = File(value)
            filePath?.let {
                val getItemList = filePath.listFiles()
                if (getItemList != null && getItemList.isNotEmpty()) {
                    for (item in getItemList) {
                        listActualProps.add(item.name)
                    }

                    printWriter.print("B) File\t:: ")
                    if (listActualProps.size < 2) {
                        printWriter.println("(${listActualProps.size}) Item Found in Directory!") //How many files found
                    } else {
                        printWriter.println("(${listActualProps.size}) Item(s) Found in Directory!")
                    }
                    printWriter.println("C) List of File\t:: $listActualProps") //Printing the list of file name

                    configFile?.let { config ->
                        val envStream = FileInputStream(config) //Load Config Properties from Params
                        properties.load(envStream) //Load as Properties

                        val keyProps = properties.propertyNames() //Getting Key values from Properties
                        while (keyProps.hasMoreElements()) {
                            val propKeys = keyProps.nextElement().toString()
                            if (properties.getProperty(propKeys) == "true") {
                                if (propKeys.contains("/")) {
                                    val indexed = propKeys.lastIndexOf("/")
                                    val firstValue = propKeys.substring(0, indexed)
                                    val lastValue = propKeys.substring(indexed + 1)

                                    if (firstValue == key) {
                                        listExpectedProps.add(lastValue)
                                    }
                                }
                            }
                        }

                        //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                        isEqual(listExpectedProps, listActualProps).also {
                            val expectedSize: Int = listExpectedProps.size
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
                                    printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedProps")
                                } else {
                                    printWriter.println("**EXPECTED --> (${listExpectedProps.size}) File(s) in Directory :: $listExpectedProps")
                                }
                                printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                            }
                        }
                    }
                }

                listExpectedProps.clear()
                listActualProps.clear()
            }
        }

        /*for (data in listFiles) {
            if (groupes.containsKey(data)) {
                val lists: String? = groupes[data]
                printWriter.println("A) Path\t:: $lists")
                printWriter.println("child:: $data")
                lists?.let {
                    val files = File(lists).listFiles()
                    if (files != null && files.isNotEmpty()) {
                        for (item in files) {
                            listActualProps.add(item.name)
                        }

                        printWriter.print("B) File\t:: ")
                        if (listActualProps.size < 2) {
                            printWriter.println("(${listActualProps.size}) Item Found in Directory!") //How many files found
                        } else {
                            printWriter.println("(${listActualProps.size}) Item(s) Found in Directory!")
                        }
                        printWriter.println("C) List of File\t:: $listActualProps") //Printing the list of file name

                        configFile?.let { config ->
                            val envStream = FileInputStream(config) //Load Config Properties from Params
                            properties.load(envStream) //Load as Properties

                            val keyProps = properties.propertyNames() //Getting Key values from Properties
                            while (keyProps.hasMoreElements()) { //Iteration
                                val keys = keyProps.nextElement().toString()
                                if (properties.getProperty(keys) == "true") {
                                    if (keys.contains("/")) {
                                        val indexed = keys.lastIndexOf("/")
                                        val firstValue = keys.substring(0, indexed)

                                        if (firstValue == data) {
                                            listExpectedProps.add(keys)
                                        }
                                    }
                                }
                            }

                            //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
                            isEqual(listExpectedProps, listActualProps).also {
                                val expectedSize: Int = listExpectedProps.size
                                val actualSize: Int = listActualProps.size

                                if (it) {
                                    if (listActualProps.size < 2) {
                                        printWriter.println("**PASSED --> $expectedSize Data from Config (.txt) is Successfully Mapped to Selected Directories")
                                    } else {
                                        printWriter.println("**PASSED --> $expectedSize Data(s) from Config (.txt) are Successfully Mapped to Selected Directories")
                                    }
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
                                        printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedProps")
                                    } else {
                                        printWriter.println("**EXPECTED --> (${listExpectedProps.size}) File(s) in Directory :: $listExpectedProps")
                                    }
                                    printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                                }
                            }

                            listExpectedProps.clear()
                            listActualProps.clear()
                        }
                    }
                }
            }
        }*/
    }

    private fun findPropertiesFiles(lastConfigPath: String) {
        File(lastConfigPath).also {
            val lists = it.listFiles() //Listing Files in end of Path (to get .properties files)
            if (lists != null && lists.isNotEmpty()) {
                for (file in lists) {
                    if (file.path.contains("mklik")) {
                        println("true --> ${file.path}")
                    }
                    /*if (file.path.contains("mklik")){
                        println("Path:: ${file.path}")
                    }*/
                    /*for (testData in listFiles) {
                        if (file.path.contains("mklik")){
                            println("Path:: ${file.path} || Data $testData")
                        }
                            //listActualProps.add(file.name) //Adding properties name to List
                    }*/
                }
                printWriter.print("B) File\t:: ")
                if (listActualProps.size < 2) {
                    printWriter.println("(${listActualProps.size}) Item Found in Directory!") //How many files found
                } else {
                    printWriter.println("(${listActualProps.size}) Item(s) Found in Directory!")
                }
                printWriter.println("C) List of File\t:: $listActualProps") //Printing the list of file name

                //populateProperties(listActualProps) //TESTINGGGGG

                checkConfigStatus(listActualProps) //Go to 'checkConfigStatus' function
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
                    listExpectedProps.add(keys) //Adding to List
            }

            //Comparing the Expected and Actual Properties File that has been Mapping via Jenkins..
            isEqual(listExpectedProps, listActualProps).also {
                val expectedSize: Int = listExpectedProps.size
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
                        printWriter.println("**EXPECTED --> (1) File in Directory :: $listExpectedProps")
                    } else {
                        printWriter.println("**EXPECTED --> (${listExpectedProps.size}) File(s) in Directory :: $listExpectedProps")
                    }
                    printWriter.println("**ACTION --> Please Check the Path/Jenkins Configuration Again for Correction/Validation")
                }
            }

            listExpectedProps.clear()
            listActualProps.clear()

            //This is Correct.. But try to find another method
            /*for (i in properties) {
                if (prop.containsKey(i)) {
                    println("Data $i --> OK")
                } else {
                    println("Data Not Found :: $i --> FAILED")
                }
            }*/
        }
    }

    private fun populateProperties() {
        configFile?.let { config ->
            val envStream = FileInputStream(config) //Load Config Properties from Params
            properties.load(envStream) //Load as Properties

            println("ISENG-ISENG TEST")
            val keyProps = properties.propertyNames() //Getting Key values from Properties
            while (keyProps.hasMoreElements()) { //Iteration
                val keys = keyProps.nextElement().toString()
                if (properties.getProperty(keys) == "true") {
                    if (keys.contains("/")) {
                        val index = keys.lastIndexOf("/")
                        val firstValue = keys.substring(0, index)
                        val lastValue = keys.substring(index + 1)

                        if (listFiles.isEmpty()) {
                            listFiles.add(firstValue)
                        } else {
                            if (!listFiles.contains(firstValue)) {
                                listFiles.add(firstValue)
                            }
                        }
                    }
                }
                //listExpectedProps.add(keys) //Adding to List
            }
        }
    }
}
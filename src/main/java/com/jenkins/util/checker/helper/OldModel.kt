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

class OldModel(private val args: Array<String>?) {

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
                    printWriter.println()
                    if (lists.size < 2) {
                        printWriter.println("Node #${index + 1} :: ${dirPaths.name}")
                    } else {
                        printWriter.println("Node(s) #${index + 1} :: ${dirPaths.name}")
                    }

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

                        configCollect?.let {
                            it.removeAt(0)
                            //Count Instance Qty, (-1 because the root are counted in it)
                            it.forEachIndexed { index, lastConfigPath ->
                                //if (index != 0) {
                                if (it.size < 2) {
                                    printWriter.println("<-- Instance #${index + 1} -->")
                                } else {
                                    printWriter.println("<-- Instance(s) #${index + 1} -->")
                                }
                                printWriter.println("A) Path\t:: $lastConfigPath")
                                findPropertiesFiles(lastConfigPath) //Go to 'findPropertiesFiles' function
                                //}
                            }
                        }
                    } catch (e: IOException) {
                        println("Err:: ${e.message.toString()}")
                    }
                }
            } else {
                println("No Directory Founds in ${this.nodeDirFiles}")
            }
        }

        printWriter.close()
        println("Successfully Running the Config Validator!")
    }

    private fun findPropertiesFiles(lastConfigPath: String) {
        File(lastConfigPath).also {
            val lists = it.listFiles() //Listing Files in end of Path (to get .properties files)
            if (lists != null && lists.isNotEmpty()) {
                for (file in lists) {
                    listActualProps.add(file.name) //Adding properties name to List
                }
                printWriter.print("B) File\t:: ")
                if (listActualProps.size < 2) {
                    printWriter.println("(${listActualProps.size}) Item Found in Directory!") //How many files found
                } else {
                    printWriter.println("(${listActualProps.size}) Item(s) Found in Directory!")
                }
                printWriter.println("C) List of File\t:: $listActualProps") //Printing the list of file name

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
                var expectedSize: Int = listExpectedProps.size
                var actualSize: Int = listActualProps.size

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
}
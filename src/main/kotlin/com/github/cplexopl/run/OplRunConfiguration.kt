package com.github.cplexopl.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import com.github.cplexopl.settings.OplSettingsState

// RunConfiguration = run configuration (what you see in the dropdown next to the Run button)
// Implements logic what will happen after clicking the green button ▶

class OplRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<OplRunConfigurationOptions>(project, factory, name) {

    override fun getOptions() = super.getOptions() as OplRunConfigurationOptions

    var modelFile: String
        get() = options.modelFile ?: ""
        set(value) { options.modelFile = value }

    var dataFile: String
        get() = options.dataFile ?: ""
        set(value) { options.dataFile = value }

    var settingsFile: String
        get() = options.settingsFile ?: ""
        set(value) { options.settingsFile = value }

    var cplexPath: String
        get() {
            // 1. Priority: Path overridden manually in "Edit Configurations" window for this specific run
            val localPath = options.cplexPath
            if (!localPath.isNullOrEmpty()) return localPath

            // 2. Main path: From global IDE settings (Settings -> Tools -> CPLEX OPL)
            val globalPath = OplSettingsState.instance.savedCplexPath
            if (globalPath.isNotEmpty()) return globalPath

            // 3. Fallback: If user hasn't set anything, try to guess
            return com.github.cplexopl.utils.CplexPathFinder.find() ?: ""
        }
        set(value) { options.cplexPath = value }

    override fun getConfigurationEditor() = OplRunConfigurationEditor(project)

    override fun checkConfiguration() {
        if (modelFile.isEmpty()) throw RuntimeConfigurationError("Model file (.mod) not specified")
        if (!File(modelFile).exists()) throw RuntimeConfigurationError("Model file does not exist: $modelFile")
        
        if (dataFile.isNotEmpty() && !File(dataFile).exists()) {
            throw RuntimeConfigurationError("Data file does not exist: $dataFile")
        }
        
        if (settingsFile.isNotEmpty() && !File(settingsFile).exists()) {
            throw RuntimeConfigurationError("Settings file (.ops) does not exist: $settingsFile")
        }
        
        if (cplexPath.isEmpty()) throw RuntimeConfigurationError(
            "CPLEX installation not found. Set path globally in: File -> Settings -> Tools -> CPLEX OPL"
        )
    }

    fun createCommandLine(tempModelFile: File): GeneralCommandLine {
        return GeneralCommandLine().apply {
            exePath = cplexPath
            addParameter(tempModelFile.absolutePath)
            if (dataFile.isNotEmpty()) {
                addParameter(dataFile)
            }
            val modelParentDir = File(modelFile).parentFile ?: File(".")
            workDirectory = modelParentDir
            withEnvironment(System.getenv())
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return OplRunState(environment, this)
    }

    companion object {
        fun generateExecuteBlock(settingsFilePath: String): String {
            if (settingsFilePath.isEmpty()) return ""
            val settingsFile = File(settingsFilePath)
            if (!settingsFile.exists()) return ""

            return try {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(settingsFile)
                
                val result = StringBuilder()
                result.appendLine("// TEMP FILE GENERATED AUTOMATICALLY BY CPLEX OPL JETBRAINS PLUGIN. SAFE TO REMOVE.")
                result.appendLine("execute {")
                
                val settings = doc.getElementsByTagName("setting")
                for (i in 0 until settings.length) {
                    val element = settings.item(i)
                    val name = element.attributes.getNamedItem("name")?.nodeValue ?: continue
                    val value = element.attributes.getNamedItem("value")?.nodeValue ?: continue
                    
                    val decodedValue = decodeXmlEntities(value)
                    
                    val isNumericOrBoolean = decodedValue.toDoubleOrNull() != null || 
                                             decodedValue.toLongOrNull() != null || 
                                             decodedValue == "true" || 
                                             decodedValue == "false"
                    
                    val escapedValue = decodedValue
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                    val formattedValue = if (isNumericOrBoolean) escapedValue else "\"$escapedValue\""
                    result.appendLine("  cplex.$name = $formattedValue;")
                }
                result.appendLine("}")
                result.append("\n")
                result.toString()
            } catch (e: Exception) {
                ""
            }
        }
        
        private fun decodeXmlEntities(value: String): String {
            return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
        }

    }
}

// RunProfileState = class executing actual process (runs oplrun as subprocess)
class OplRunState(
    environment: ExecutionEnvironment,
    private val config: OplRunConfiguration
) : CommandLineState(environment) {

    override fun startProcess(): OSProcessHandler {
        try {
            val originalModel = File(config.modelFile)
            val tempFileSuffix = "_temp_${UUID.randomUUID()}_${originalModel.name}"
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            val tempModelFile = File(tempDir, tempFileSuffix)
            tempModelFile.createNewFile()
            tempModelFile.setReadable(false, false)
            tempModelFile.setReadable(true, true)
            tempModelFile.setWritable(false, false)
            tempModelFile.setWritable(true, true)
            tempModelFile.deleteOnExit()

            try {
                val executeBlock = OplRunConfiguration.generateExecuteBlock(config.settingsFile)
                val originalContent = originalModel.readText(Charsets.UTF_8)
                tempModelFile.writeText(executeBlock + originalContent, Charsets.UTF_8)
            } catch (e: Exception) {
                if (tempModelFile.exists()) {
                    tempModelFile.delete()
                }
                throw RuntimeConfigurationException("Failed to prepare model file: ${e.message}")
            }

            val commandLine = config.createCommandLine(tempModelFile)
            val handler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)

            handler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    if (tempModelFile.exists()) {
                        try {
                            tempModelFile.delete()
                        } catch (e: Exception) {
                            // Ignore errors when removing temporary file
                        }
                    }
                }
            })
            
            ProcessTerminatedListener.attach(handler)
            return handler
        } catch (e: Exception) {
            throw RuntimeConfigurationException("Failed to start OPL process: ${e.message}")
        }
    }
}

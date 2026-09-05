package com.github.cplexopl.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.execution.ParametersListUtil
import java.io.File
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
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

    var timeoutSeconds: Int
        get() = options.timeoutSeconds
        set(value) { options.timeoutSeconds = value }

    var additionalArgs: String?
        get() = options.additionalArgs
        set(value) { options.additionalArgs = value }

    var runConflictRefiner: Boolean
        get() = options.runConflictRefiner
        set(value) { options.runConflictRefiner = value }

    var cplexPath: String
        get() {
            val localPath = options.cplexPath
            if (!localPath.isNullOrEmpty()) return localPath

            val globalPath = OplSettingsState.instance.savedCplexPath
            if (globalPath.isNotEmpty()) return globalPath

            return com.github.cplexopl.utils.CplexPathFinder.find() ?: ""
        }
        set(value) { options.cplexPath = value }

    var executionMode: ExecutionMode
        get() = try { ExecutionMode.valueOf(options.executionModeStr ?: "LOCAL") } catch (e: Exception) { ExecutionMode.LOCAL }
        set(value) { options.executionModeStr = value.name }

    var wslDistribution: String
        get() = options.wslDistribution ?: ""
        set(value) { options.wslDistribution = value }

    var dockerImage: String
        get() = options.dockerImage ?: ""
        set(value) { options.dockerImage = value }

    override fun getConfigurationEditor() = OplRunConfigurationEditor(project)

    override fun checkConfiguration() {
        if (modelFile.isEmpty()) throw RuntimeConfigurationError(com.github.cplexopl.OplBundle.message("error.run.modelNotSpecified"))
        if (!File(modelFile).exists()) throw RuntimeConfigurationError(com.github.cplexopl.OplBundle.message("error.run.modelNotFound", modelFile))
        
        if (dataFile.isNotEmpty() && !File(dataFile).exists()) {
            throw RuntimeConfigurationError(com.github.cplexopl.OplBundle.message("error.run.dataNotFound", dataFile))
        }
        
        if (settingsFile.isNotEmpty() && !File(settingsFile).exists()) {
            throw RuntimeConfigurationError(com.github.cplexopl.OplBundle.message("error.run.settingsNotFound", settingsFile))
        }
        
        if (cplexPath.isEmpty()) throw RuntimeConfigurationError(com.github.cplexopl.OplBundle.message("error.run.cplexNotFound"))
    }

    fun createCommandLine(tempModelFile: File): GeneralCommandLine {
        return GeneralCommandLine().apply {
            when (executionMode) {
                ExecutionMode.LOCAL -> {
                    exePath = cplexPath
                    val modelParentDir = File(modelFile).parentFile ?: File(".")
                    workDirectory = modelParentDir
                    
                    val args = options.additionalArgs
                    if (!args.isNullOrBlank()) {
                        addParameters(ParametersListUtil.parse(args))
                    }
                    if (options.runConflictRefiner) {
                        addParameter("-conflict")
                    }
                    addParameter(tempModelFile.absolutePath)
                    if (dataFile.isNotEmpty()) {
                        addParameter(dataFile)
                    }
                }
                ExecutionMode.WSL -> {
                    exePath = "wsl.exe"
                    if (wslDistribution.isNotEmpty()) {
                        addParameter("-d")
                        addParameter(wslDistribution)
                    }
                    addParameter("--")
                    addParameter(cplexPath)
                    
                    val args = options.additionalArgs
                    if (!args.isNullOrBlank()) {
                        addParameters(ParametersListUtil.parse(args))
                    }
                    if (options.runConflictRefiner) {
                        addParameter("-conflict")
                    }
                    
                    val translatedModel = OplPathTranslator.translateToWslPath(tempModelFile.absolutePath)
                    val translatedData = if (dataFile.isNotEmpty()) OplPathTranslator.translateToWslPath(dataFile) else ""
                    
                    addParameter(translatedModel)
                    if (translatedData.isNotEmpty()) {
                        addParameter(translatedData)
                    }
                }
                ExecutionMode.DOCKER -> {
                    exePath = "docker"
                    addParameter("run")
                    addParameter("--rm")
                    
                    val projectDir = project.basePath ?: ""
                    val wDir = "/workspace"
                    val tempDir = System.getProperty("java.io.tmpdir").trimEnd('\\', '/')
                    
                    if (projectDir.isNotEmpty()) {
                        addParameter("-v")
                        addParameter("$projectDir:$wDir")
                        addParameter("-w")
                        addParameter(wDir)
                    }
                    if (tempDir.isNotEmpty()) {
                        addParameter("-v")
                        addParameter("$tempDir:/tmp")
                    }
                    
                    addParameter(if (dockerImage.isEmpty()) "cplex" else dockerImage)
                    addParameter(cplexPath)
                    
                    val args = options.additionalArgs
                    if (!args.isNullOrBlank()) {
                        addParameters(ParametersListUtil.parse(args))
                    }
                    if (options.runConflictRefiner) {
                        addParameter("-conflict")
                    }
                    
                    val translatedModel = OplPathTranslator.translateToDockerPath(tempModelFile.absolutePath, projectDir, wDir, tempDir)
                    val translatedData = if (dataFile.isNotEmpty()) OplPathTranslator.translateToDockerPath(dataFile, projectDir, wDir, tempDir) else ""
                    
                    addParameter(translatedModel)
                    if (translatedData.isNotEmpty()) {
                        addParameter(translatedData)
                    }
                }
            }
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
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                factory.isXIncludeAware = false
                factory.isExpandEntityReferences = false
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(settingsFile)
                
                val result = StringBuilder()
                result.appendLine(com.github.cplexopl.OplBundle.message("error.run.tempFileComment"))
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
                    val formattedValue = if (isNumericOrBoolean) escapedValue else "\"${escapedValue}\""
                    result.appendLine("  cplex.${name} = ${formattedValue};")
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
                throw RuntimeConfigurationException(com.github.cplexopl.OplBundle.message("error.run.prepareFailed", e.message ?: ""))
            }

            val commandLine = config.createCommandLine(tempModelFile)
            val handler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)

            val timeout = config.timeoutSeconds
            var watchdogTask: ScheduledFuture<*>? = null

            if (timeout > 0) {
                watchdogTask = AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    if (!handler.isProcessTerminated) {
                        handler.notifyTextAvailable(com.github.cplexopl.OplBundle.message("error.run.timeout", timeout.toString()), ProcessOutputTypes.STDERR)
                        handler.destroyProcess()
                    }
                }, timeout.toLong(), TimeUnit.SECONDS)
            }

            handler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (text.contains("<<< no solution") && !config.runConflictRefiner) {
                        handler.notifyTextAvailable(
                            com.github.cplexopl.OplBundle.message("error.run.conflictHint"),
                            ProcessOutputTypes.STDERR
                        )
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    watchdogTask?.cancel(false)
                    if (tempModelFile.exists()) {
                        try {
                            tempModelFile.delete()
                        } catch (e: Exception) {
                            // Ignore errors when removing temporary file
                        }
                    }

                    if (event.exitCode == 0) {
                        val settings = OplSettingsState.instance
                        settings.successfulRunCount++
                        if (settings.successfulRunCount == 5 && !settings.neverShowRatePrompt) {
                            com.github.cplexopl.actions.OplRatePrompt.showNotification(config.project)
                        }
                    }
                }
            })
            
            ProcessTerminatedListener.attach(handler)
            return handler
        } catch (e: Exception) {
            throw RuntimeConfigurationException(com.github.cplexopl.OplBundle.message("error.run.startFailed", e.message ?: ""))
        }
    }
}

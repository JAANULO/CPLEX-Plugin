package com.github.cplexopl.run

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons

enum class ExecutionMode { LOCAL, WSL, DOCKER }

// ConfigurationType = entry in "Add New Configuration" list (+ in Run dropdown)
class OplRunConfigurationType : ConfigurationTypeBase(
    "OPL_RUN",                          // Unique identifier
    com.github.cplexopl.OplBundle.message("runConfig.type.displayName"),                        // Displayed name
    com.github.cplexopl.OplBundle.message("runConfig.type.description"),         // Description
    com.intellij.openapi.util.NotNullLazyValue.createValue { com.intellij.icons.AllIcons.RunConfigurations.Application }
) {
    init {
        // Each ConfigurationType must have at least one factory
        addFactory(OplConfigurationFactory(this))
    }

    companion object {
        // Singleton - easy access to instance from other places
        fun getInstance(): OplRunConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(OplRunConfigurationType::class.java)
    }
}

// ConfigurationFactory = creates new configuration instances
class OplConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "OPL_CONFIGURATION_FACTORY"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return OplRunConfiguration(project, this, com.github.cplexopl.OplBundle.message("runConfig.type.displayName"))
    }

    override fun getOptionsClass() = OplRunConfigurationOptions::class.java
}

// Options = class storing configuration settings (serialized to XML)
// Serialization = conversion of Kotlin object to XML to save in .idea/ file
class OplRunConfigurationOptions : RunConfigurationOptions() {
    private val _modelFile = string("").provideDelegate(this, ::modelFile)
    private val _dataFile = string("").provideDelegate(this, ::dataFile)
    private val _settingsFile = string("").provideDelegate(this, ::settingsFile)
    private val _cplexPath = string("").provideDelegate(this, ::cplexPath)
    
    // Nowe opcje dla watchdoga i tuningu
    private val _timeoutSeconds = property(0).provideDelegate(this, ::timeoutSeconds)
    private val _additionalArgs = string("").provideDelegate(this, ::additionalArgs)
    private val _runConflictRefiner = property(false).provideDelegate(this, ::runConflictRefiner)

    // Opcje dla WSL/Docker
    private val _executionModeStr = string(ExecutionMode.LOCAL.name).provideDelegate(this, ::executionModeStr)
    private val _wslDistribution = string("").provideDelegate(this, ::wslDistribution)
    private val _dockerImage = string("").provideDelegate(this, ::dockerImage)

    var modelFile: String? by _modelFile
    var dataFile: String? by _dataFile
    var settingsFile: String? by _settingsFile
    var cplexPath: String? by _cplexPath
    var timeoutSeconds: Int by _timeoutSeconds
    var additionalArgs: String? by _additionalArgs
    var runConflictRefiner: Boolean by _runConflictRefiner
    
    var executionModeStr: String? by _executionModeStr
    var wslDistribution: String? by _wslDistribution
    var dockerImage: String? by _dockerImage
}

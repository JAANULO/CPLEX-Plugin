package com.github.cplexopl.run

import com.github.cplexopl.OplFileType
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

// RunConfigurationProducer = automatically creates Run configuration
// when user right-clicks on .mod file → "Run 'model.mod'"
class OplRunConfigurationProducer : LazyRunConfigurationProducer<OplRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        OplRunConfigurationType.getInstance().configurationFactories[0]

    // Does this producer fit the given PSI element (cursor in code)?
    override fun isConfigurationFromContext(
        configuration: OplRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        return file.fileType == OplFileType && configuration.modelFile == file.path
    }

    // Create configuration based on context (clicked file)
    override fun setupConfigurationFromContext(
        configuration: OplRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        if (file.fileType != OplFileType) return false

        configuration.modelFile = file.path
        configuration.name = file.nameWithoutExtension

        // Try to automatically find .dat file with the same name
        val dataFile = file.parent?.findChild("${file.nameWithoutExtension}.dat")
        if (dataFile != null) {
            configuration.dataFile = dataFile.path
        }

        // Try to automatically find .ops file with the same name
        val settingsFile = file.parent?.findChild("${file.nameWithoutExtension}.ops")
        if (settingsFile != null) {
            configuration.settingsFile = settingsFile.path
        }

        // Detect CPLEX path
        val detectedPath = com.github.cplexopl.utils.CplexPathFinder.find() ?: ""
        if (detectedPath.isNotEmpty()) {
            configuration.cplexPath = detectedPath
        }

        return true
    }
}

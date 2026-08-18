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

        // Try to automatically find .dat file
        // 1. Same name as model (.dat)
        // 2. data.dat
        // 3. If there is only one .dat file in the directory
        val parent = file.parent
        val dataFile = parent?.findChild("${file.nameWithoutExtension}.dat")
            ?: parent?.findChild("data.dat")
            ?: parent?.children?.filter { it.extension == "dat" }?.singleOrNull()

        if (dataFile != null) {
            configuration.dataFile = dataFile.path
        }

        // Try to automatically find .ops file
        // 1. Same name as model (.ops)
        // 2. settings.ops
        // 3. If there is only one .ops file in the directory
        val settingsFile = parent?.findChild("${file.nameWithoutExtension}.ops")
            ?: parent?.findChild("settings.ops")
            ?: parent?.children?.filter { it.extension == "ops" }?.singleOrNull()

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

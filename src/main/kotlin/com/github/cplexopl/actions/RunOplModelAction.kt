package com.github.cplexopl.actions

import com.github.cplexopl.OplFileType
import com.github.cplexopl.run.OplRunConfiguration
import com.github.cplexopl.run.OplRunConfigurationType
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutionTargetManager // Added import
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

import com.intellij.openapi.actionSystem.ActionUpdateThread

class RunOplModelAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val runManager = RunManager.getInstance(project)
        val configType = OplRunConfigurationType.getInstance()

        val existing = runManager.allSettings.firstOrNull { setting ->
            setting.type == configType &&
                    (setting.configuration as? OplRunConfiguration)?.modelFile == file.path
        }

        val configSetting = existing ?: run {
            val factory = configType.configurationFactories[0]
            val newConfig = runManager.createConfiguration(
                file.nameWithoutExtension,
                factory
            )
            val oplConfig = newConfig.configuration as OplRunConfiguration
            oplConfig.modelFile = file.path

            val datFile = file.parent?.findChild("${file.nameWithoutExtension}.dat")
            if (datFile != null) oplConfig.dataFile = datFile.path

            val cplexPath = com.github.cplexopl.utils.CplexPathFinder.find() ?: ""
            if (cplexPath.isNotEmpty()) oplConfig.cplexPath = cplexPath

            runManager.addConfiguration(newConfig)
            newConfig
        }

        runManager.selectedConfiguration = configSetting

        // Fixed call to restartRunProfile
        ExecutionManager.getInstance(project).restartRunProfile(
            project,
            DefaultRunExecutor.getRunExecutorInstance(),
            ExecutionTargetManager.getActiveTarget(project), // Fixed parameter 3
            configSetting, // Fixed parameter 4 (pass RunnerAndConfigurationSettings)
            null
        )
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.fileType == OplFileType
    }
}
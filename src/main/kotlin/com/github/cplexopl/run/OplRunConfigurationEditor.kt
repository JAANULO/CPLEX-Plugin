package com.github.cplexopl.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.FormBuilder
import com.intellij.openapi.ui.ComboBox
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JCheckBox
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import com.github.cplexopl.utils.CplexPathFinder
import com.github.cplexopl.settings.OplSettingsState
import java.io.File
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// SettingsEditor = UI panel displayed in "Edit Run Configuration" window
// FormBuilder = IntelliJ helper for building forms (label + field)
class OplRunConfigurationEditor(private val project: Project) : SettingsEditor<OplRunConfiguration>() {

    // Form fields - TextFieldWithBrowseButton = text field with "..." button for file selection
    private val modelFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("mod")
                .withTitle(com.github.cplexopl.OplBundle.message("runConfig.editor.modelFile.title"))
                .withDescription(com.github.cplexopl.OplBundle.message("runConfig.editor.modelFile.description"))
        )
    }

    private val dataFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("dat")
                .withTitle(com.github.cplexopl.OplBundle.message("runConfig.editor.dataFile.title"))
                .withDescription(com.github.cplexopl.OplBundle.message("runConfig.editor.dataFile.description"))
        )
    }

    private val settingsFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("ops")
                .withTitle(com.github.cplexopl.OplBundle.message("runConfig.editor.settingsFile.title"))
                .withDescription(com.github.cplexopl.OplBundle.message("runConfig.editor.settingsFile.description"))
        )
    }

    private val cplexPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("exe")
                .withTitle(com.github.cplexopl.OplBundle.message("runConfig.editor.oplrun.title"))
                .withDescription(com.github.cplexopl.OplBundle.message("runConfig.editor.oplrun.description"))
        )
    }

    private val autoDetectButton = JButton(com.github.cplexopl.OplBundle.message("runConfig.editor.autodetect.button")).apply {
        addActionListener {
            val foundPath = CplexPathFinder.find()
            if (foundPath != null) {
                cplexPathField.text = foundPath
                OplSettingsState.instance.savedCplexPath = foundPath // Global save!
            } else {
                Messages.showWarningDialog(
                    com.github.cplexopl.OplBundle.message("runConfig.editor.autodetect.failure.message"),
                    com.github.cplexopl.OplBundle.message("runConfig.editor.autodetect.failure.title")
                )
            }
        }
    }

    private val pathPanel = JPanel(BorderLayout()).apply {
        add(cplexPathField, BorderLayout.CENTER)
        add(autoDetectButton, BorderLayout.EAST)
    }

    // New fields
    private val timeoutField = JTextField().apply {
        toolTipText = com.github.cplexopl.OplBundle.message("runConfig.editor.timeout.tooltip")
    }
    private val additionalArgsField = JTextField().apply {
        toolTipText = com.github.cplexopl.OplBundle.message("runConfig.editor.args.tooltip")
    }
    private val runConflictRefinerCheckbox = JCheckBox(com.github.cplexopl.OplBundle.message("runConfig.editor.conflict.checkbox"))

    private val executionModeCombo = ComboBox(ExecutionMode.values()).apply {
        addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                updateVisibility()
            }
        }
    }
    
    private val wslDistributionField = JTextField().apply {
        toolTipText = com.github.cplexopl.OplBundle.message("runConfig.editor.wsl.tooltip")
    }
    
    private val dockerImageField = JTextField().apply {
        toolTipText = com.github.cplexopl.OplBundle.message("runConfig.editor.docker.tooltip")
    }
    
    private fun updateVisibility() {
        val mode = executionModeCombo.selectedItem as? ExecutionMode ?: ExecutionMode.LOCAL
        wslDistributionField.isVisible = mode == ExecutionMode.WSL
        dockerImageField.isVisible = mode == ExecutionMode.DOCKER
    }

    // Build form panel
    private val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.executionMode.label"), executionModeCombo)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.wsl.label"), wslDistributionField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.docker.label"), dockerImageField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.modelFile.label"), modelFileField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.dataFile.label"), dataFileField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.settingsFile.label"), settingsFileField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.path.label"), pathPanel)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.args.label"), additionalArgsField)
        .addLabeledComponent(com.github.cplexopl.OplBundle.message("runConfig.editor.timeout.label"), timeoutField)
        .addComponent(runConflictRefinerCheckbox)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    init {
        // Clean Java Swing listener, resilient to changes in JetBrains API
        modelFileField.textField.document.addDocumentListener(object : DocumentListener {
            private fun autoFillFiles() {
                val modPath = modelFileField.text
                if (modPath.endsWith(".mod")) {
                    val base = modPath.removeSuffix(".mod")
                    
                    if (dataFileField.text.isEmpty()) {
                        val potentialDatPath = base + ".dat"
                        if (File(potentialDatPath).exists()) {
                            dataFileField.text = potentialDatPath
                        }
                    }
                    
                    if (settingsFileField.text.isEmpty()) {
                        val potentialOpsPath = base + ".ops"
                        if (File(potentialOpsPath).exists()) {
                            settingsFileField.text = potentialOpsPath
                        }
                    }
                }
            }

            override fun insertUpdate(e: DocumentEvent?) = autoFillFiles()
            override fun removeUpdate(e: DocumentEvent?) = autoFillFiles()
            override fun changedUpdate(e: DocumentEvent?) = autoFillFiles()
        })
    }

    override fun resetEditorFrom(config: OplRunConfiguration) {
        // Load values from configuration to UI fields
        modelFileField.text = config.modelFile
        dataFileField.text = config.dataFile
        settingsFileField.text = config.settingsFile

        // If local configuration has no path (new profile), load saved globally
        if (config.cplexPath.isEmpty() && OplSettingsState.instance.savedCplexPath.isNotEmpty()) {
            cplexPathField.text = OplSettingsState.instance.savedCplexPath
        } else {
            cplexPathField.text = config.cplexPath
        }

        timeoutField.text = config.timeoutSeconds.toString()
        additionalArgsField.text = config.additionalArgs ?: ""
        runConflictRefinerCheckbox.isSelected = config.runConflictRefiner
        
        executionModeCombo.selectedItem = config.executionMode
        wslDistributionField.text = config.wslDistribution
        dockerImageField.text = config.dockerImage
        updateVisibility()
    }

    override fun applyEditorTo(config: OplRunConfiguration) {
        // Save values from UI fields to configuration
        config.modelFile = modelFileField.text
        config.dataFile = dataFileField.text
        config.settingsFile = settingsFileField.text
        config.cplexPath = cplexPathField.text
        
        config.timeoutSeconds = timeoutField.text.toIntOrNull() ?: 0
        config.additionalArgs = additionalArgsField.text
        config.runConflictRefiner = runConflictRefinerCheckbox.isSelected
        
        config.executionMode = executionModeCombo.selectedItem as? ExecutionMode ?: ExecutionMode.LOCAL
        config.wslDistribution = wslDistributionField.text
        config.dockerImage = dockerImageField.text

        // On confirmation of "Apply" form, also update global path
        if (cplexPathField.text.isNotEmpty()) {
            OplSettingsState.instance.savedCplexPath = cplexPathField.text
        }
    }

    override fun createEditor(): JComponent = panel
}

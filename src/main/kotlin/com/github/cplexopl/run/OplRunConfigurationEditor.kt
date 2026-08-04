package com.github.cplexopl.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
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
                .withTitle("Select OPL Model File")
                .withDescription("Select the .mod file to run")
        )
    }

    private val dataFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("dat")
                .withTitle("Select OPL Data File")
                .withDescription("Select the .dat data file (optional)")
        )
    }

    private val settingsFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("ops")
                .withTitle("Select OPL Settings File")
                .withDescription("Select the .ops settings file (optional)")
        )
    }

    private val cplexPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                .withTitle("Select Oplrun Executable")
                .withDescription("Select the oplrun binary from your CPLEX installation")
        )
    }

    private val autoDetectButton = JButton("Auto-Detect").apply {
        addActionListener {
            val foundPath = CplexPathFinder.find()
            if (foundPath != null) {
                cplexPathField.text = foundPath
                OplSettingsState.instance.savedCplexPath = foundPath // Global save!
            } else {
                Messages.showWarningDialog(
                    "Could not automatically find the oplrun executable in default locations.\nPlease select the path manually.",
                    "Auto-Detect Failed"
                )
            }
        }
    }

    private val pathPanel = JPanel(BorderLayout()).apply {
        add(cplexPathField, BorderLayout.CENTER)
        add(autoDetectButton, BorderLayout.EAST)
    }

    // Build form panel
    private val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("Model file (.mod):", modelFileField)
        .addLabeledComponent("Data file (.dat):", dataFileField)
        .addLabeledComponent("Settings file (.ops):", settingsFileField)
        .addLabeledComponent("Oplrun path:", pathPanel)
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
    }

    override fun applyEditorTo(config: OplRunConfiguration) {
        // Save values from UI fields to configuration
        config.modelFile = modelFileField.text
        config.dataFile = dataFileField.text
        config.settingsFile = settingsFileField.text
        config.cplexPath = cplexPathField.text

        // On confirmation of "Apply" form, also update global path
        if (cplexPathField.text.isNotEmpty()) {
            OplSettingsState.instance.savedCplexPath = cplexPathField.text
        }
    }

    override fun createEditor(): JComponent = panel
}

package com.github.cplexopl.settings

import com.github.cplexopl.utils.CplexPathFinder
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel

class OplSettingsComponent {
    val panel: JPanel
    val cplexPathField = TextFieldWithBrowseButton()
    val autoDetectButton = JButton("Auto-detect")

    init {
        // Dialog window for selecting executable file
        cplexPathField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor().apply {
                title = "Select CPLEX Executable (Oplrun)"
                description = "Specify the path to the oplrun executable."
            }
        )

        autoDetectButton.addActionListener {
            val detected = CplexPathFinder.find()
            if (detected != null) {
                cplexPathField.text = detected
                Messages.showInfoMessage(
                    panel,
                    "Detected CPLEX at: $detected",
                    "CPLEX Auto-Detection"
                )
            } else {
                Messages.showWarningDialog(
                    panel,
                    "Could not auto-detect CPLEX installation path. Please specify it manually.",
                    "CPLEX Auto-Detection"
                )
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Path to oplrun:", cplexPathField, 1, false)
            .addComponent(autoDetectButton)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    var cplexPath: String
        get() = cplexPathField.text
        set(value) { cplexPathField.text = value }
}
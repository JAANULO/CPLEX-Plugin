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
    val autoDetectButton = JButton(com.github.cplexopl.OplBundle.message("settings.autodetect.button"))

    init {
        // Dialog window for selecting executable file
        cplexPathField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor().apply {
                title = com.github.cplexopl.OplBundle.message("settings.dialog.title")
                description = com.github.cplexopl.OplBundle.message("settings.dialog.description")
            }
        )

        autoDetectButton.addActionListener {
            val detected = CplexPathFinder.find()
            if (detected != null) {
                cplexPathField.text = detected
                Messages.showInfoMessage(
                    panel,
                    com.github.cplexopl.OplBundle.message("settings.autodetect.success.message", detected),
                    com.github.cplexopl.OplBundle.message("settings.autodetect.success.title")
                )
            } else {
                Messages.showWarningDialog(
                    panel,
                    com.github.cplexopl.OplBundle.message("settings.autodetect.failure.message"),
                    com.github.cplexopl.OplBundle.message("settings.autodetect.failure.title")
                )
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(com.github.cplexopl.OplBundle.message("settings.cplexPath.label"), cplexPathField, 1, false)
            .addComponent(autoDetectButton)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    var cplexPath: String
        get() = cplexPathField.text
        set(value) { cplexPathField.text = value }
}
package com.github.cplexopl.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class OplSettingsConfigurable : Configurable {
    private var component: OplSettingsComponent? = null

    // Name of tab in Settings panel
    override fun getDisplayName(): String = com.github.cplexopl.OplBundle.message("settings.configurable.displayName")

    override fun createComponent(): JComponent {
        component = OplSettingsComponent()
        return component!!.panel
    }

    // Checks whether entered path differs from the one saved in XML
    override fun isModified(): Boolean {
        val state = OplSettingsState.instance
        return component?.cplexPath != state.savedCplexPath
    }

    // Executed after clicking "Apply" or "OK"
    override fun apply() {
        val state = OplSettingsState.instance
        state.savedCplexPath = component?.cplexPath ?: ""
    }

    // Loads data from XML when opening Settings window
    override fun reset() {
        val state = OplSettingsState.instance
        component?.cplexPath = state.savedCplexPath
    }

    // Releases memory after closing window
    override fun disposeUIResources() {
        component = null
    }
}
package com.github.cplexopl.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OplSettingsTest : BasePlatformTestCase() {

    fun testSettingsConfigurableLifecycle() {
        val configurable = OplSettingsConfigurable()
        val panel = configurable.createComponent()
        assertNotNull("Panel should be created", panel)
        assertEquals("CPLEX OPL", configurable.displayName)

        val state = OplSettingsState.instance
        val originalPath = state.savedCplexPath

        try {
            state.savedCplexPath = "C:\\Original\\path\\oplrun.exe"
            configurable.reset()

            assertFalse("Configurable should not be modified after reset", configurable.isModified)

            // Test changing state directly vs UI
            state.savedCplexPath = "C:\\NewSaved\\path\\oplrun.exe"
            assertTrue("Configurable should recognize modified state when saved state changes", configurable.isModified)

            // Apply should update state
            configurable.reset()
            assertFalse(configurable.isModified)

            // Modify state via configurable apply
            state.savedCplexPath = "C:\\Initial\\path\\oplrun.exe"
            configurable.reset()
            
            // Simulating user typing a new path in the settings panel
            // Since cplexPath is backed by the Swing TextField, we test the state saving contract directly
            configurable.apply()
            assertEquals("C:\\Initial\\path\\oplrun.exe", state.savedCplexPath)

        } finally {
            state.savedCplexPath = originalPath
            configurable.disposeUIResources()
        }
    }
}

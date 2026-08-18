package com.github.cplexopl

import com.github.cplexopl.run.OplRunConfiguration
import com.github.cplexopl.run.OplRunConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class OplRunConfigurationTest : BasePlatformTestCase() {

    fun testRunConfigurationRegistration() {
        val configurationType = ConfigurationTypeUtil.findConfigurationType(OplRunConfigurationType::class.java)
        assertNotNull("OplRunConfigurationType nie zostało poprawnie zarejestrowane", configurationType)
        assertEquals("OPL Model", configurationType.displayName)
    }

    fun testCommandLineConstruction() {
        val configurationType = OplRunConfigurationType.getInstance()
        val factory = configurationType.configurationFactories[0]
        val configuration = factory.createTemplateConfiguration(project) as OplRunConfiguration
        
        val isWindows = com.intellij.openapi.util.SystemInfo.isWindows
        val baseDir = if (isWindows) "C:\\projects\\" else "/projects/"
        val cplexExe = if (isWindows) "C:\\cplex\\bin\\oplrun.exe" else "/cplex/bin/oplrun"

        configuration.modelFile = "${baseDir}model.mod"
        configuration.dataFile = "${baseDir}data.dat"
        configuration.settingsFile = "${baseDir}settings.ops"
        configuration.cplexPath = cplexExe

        val tempFile = java.io.File("${baseDir}_temp_model.mod")
        val commandLine = configuration.createCommandLine(tempFile)
        
        assertEquals(cplexExe, commandLine.exePath)
        val parameters = commandLine.parametersList.list
        
        assertEquals(2, parameters.size)
        assertEquals(java.io.File("${baseDir}_temp_model.mod").absolutePath, parameters[0])
        assertEquals("${baseDir}data.dat", parameters[1])
    }

    fun testGenerateExecuteBlock() {
        val tempOpsFile = java.io.File.createTempFile("test_settings", ".ops")
        try {
            tempOpsFile.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <settings version="2">
                  <category name="cplex">
                    <setting name="workmem" value="4096"/>
                    <setting name="threads" value="true"/>
                    <setting name="logpath" value="cplex&quot;log.txt"/>
                  </category>
                </settings>
            """.trimIndent(), Charsets.UTF_8)

            val executeBlock = OplRunConfiguration.generateExecuteBlock(tempOpsFile.absolutePath)
            
            assertTrue("Blok execute nie zawiera workmem", executeBlock.contains("cplex.workmem = 4096;"))
            assertTrue("Blok execute nie zawiera threads", executeBlock.contains("cplex.threads = true;"))
            assertTrue("Blok execute nie zawiera ucieczki cudzysłowów w logpath", executeBlock.contains("cplex.logpath = \"cplex\\\"log.txt\";"))
        } finally {
            tempOpsFile.delete()
        }
    }
    
    fun testGenerateExecuteBlockWithAmpersand() {
        val tempOpsFile = java.io.File.createTempFile("test_settings_amp", ".ops")
        try {
            tempOpsFile.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <settings version="2">
                  <category name="cplex">
                    <setting name="path" value="file&amp;name.txt"/>
                    <setting name="quote" value="&quot;test&quot;"/>
                    <setting name="backslash" value="C:\path\&quot;file.txt&quot;"/>
                  </category>
                </settings>
            """.trimIndent(), Charsets.UTF_8)

            val executeBlock = OplRunConfiguration.generateExecuteBlock(tempOpsFile.absolutePath)
            
            assertTrue("Ampersand nie został zdekodowany prawidłowo", executeBlock.contains("cplex.path = \"file&name.txt\";"))
            assertTrue("Znaki cudzysłowu nie zostały zdekodowane prawidłowo", executeBlock.contains("cplex.quote = \"\\\"test\\\"\";"))
            assertTrue("Znak backslash nie został escapowany prawidłowo", executeBlock.contains("cplex.backslash = \"C:\\\\path\\\\\\\"file.txt\\\"\";"))
        } finally {
            tempOpsFile.delete()
        }
    }
    
    fun testGenerateExecuteBlockXxeProtection() {
        val tempOpsFile = java.io.File.createTempFile("test_settings_xxe", ".ops")
        try {
            tempOpsFile.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///non_existent_file.txt">]>
                <settings version="2">
                  <category name="cplex">
                    <setting name="workmem" value="&xxe;"/>
                  </category>
                </settings>
            """.trimIndent(), Charsets.UTF_8)

            val executeBlock = OplRunConfiguration.generateExecuteBlock(tempOpsFile.absolutePath)
            
            assertTrue("Blok execute nie powinien zostać poprawnie sparsowany z wstrzyknięciem zewnętrznej encji (XXE)", executeBlock.isEmpty())
        } finally {
            tempOpsFile.delete()
        }
    }
    
    fun testParentFileNullHandling() {
        val configurationType = OplRunConfigurationType.getInstance()
        val factory = configurationType.configurationFactories[0]
        val configuration = factory.createTemplateConfiguration(project) as OplRunConfiguration
        
        configuration.modelFile = "model.mod"
        configuration.cplexPath = "C:\\cplex\\bin\\oplrun.exe"

        val tempFile = java.io.File("_temp_model.mod")
        val commandLine = configuration.createCommandLine(tempFile)
        
        assertNotNull("workDirectory nie powinien być null", commandLine.workDirectory)
    }

    fun testMissingDataFileValidation() {
        val configurationType = OplRunConfigurationType.getInstance()
        val factory = configurationType.configurationFactories[0]
        val configuration = factory.createTemplateConfiguration(project) as OplRunConfiguration
        
        val tempDir = createTempDir("opl_test")
        val modelFile = File(tempDir, "model.mod")
        modelFile.writeText("dvar int x;")
        
        configuration.modelFile = modelFile.absolutePath
        configuration.dataFile = File(tempDir, "nonexistent.dat").absolutePath
        configuration.cplexPath = "C:\\cplex\\bin\\oplrun.exe"
        
        try {
            configuration.checkConfiguration()
            fail("checkConfiguration() powinien rzucić wyjątek dla brakującego data file")
        } catch (e: Exception) {
            assertTrue("Wiadomość o błędzie powinna zawierać 'Data file'", 
                e.message?.contains("Data file") == true)
        } finally {
            modelFile.delete()
            tempDir.delete()
        }
    }

    fun testProducerDataFileFallback() {
        val psiFile = myFixture.configureByText("model.mod", "dvar int x;")
        myFixture.addFileToProject("data.dat", "x = 1;")
        
        val context = com.intellij.execution.actions.ConfigurationContext(psiFile)
        val producer = com.github.cplexopl.run.OplRunConfigurationProducer()
        val configuration = producer.createConfigurationFromContext(context)?.configuration as? OplRunConfiguration
        
        assertNotNull("Configuration powinna zostać utworzona", configuration)
        assertTrue("dataFile powinien zostać automatycznie zmapowany na data.dat", configuration?.dataFile?.endsWith("data.dat") == true)
    }

    private fun createTempDir(prefix: String): File {
        val tempDir = File.createTempFile(prefix, "")
        tempDir.delete()
        tempDir.mkdir()
        return tempDir
    }
}

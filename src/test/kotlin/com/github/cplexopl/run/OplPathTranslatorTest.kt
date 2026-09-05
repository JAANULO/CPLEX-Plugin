package com.github.cplexopl.run

import org.junit.Assert.assertEquals
import org.junit.Test

class OplPathTranslatorTest {

    @Test
    fun testTranslateToWslPath() {
        assertEquals("/mnt/c/Users/Janusz/test.mod", OplPathTranslator.translateToWslPath("C:\\Users\\Janusz\\test.mod"))
        assertEquals("/mnt/d/Project/data.dat", OplPathTranslator.translateToWslPath("d:/Project/data.dat"))
        assertEquals("/opt/ibm/ILOG/CPLEX_Studio/opl/bin/x86-64_linux/oplrun", OplPathTranslator.translateToWslPath("/opt/ibm/ILOG/CPLEX_Studio/opl/bin/x86-64_linux/oplrun"))
    }

    @Test
    fun testTranslateToDockerPath_Normal() {
        val winPath = "C:\\Project\\model.mod"
        val base = "C:\\Project"
        val wDir = "/workspace"
        val tempDir = "C:\\Temp"
        
        assertEquals("/workspace/model.mod", OplPathTranslator.translateToDockerPath(winPath, base, wDir, tempDir))
    }

    @Test
    fun testTranslateToDockerPath_IgnoreCase() {
        val winPath = "c:\\project\\model.mod"
        val base = "C:\\Project"
        val wDir = "/workspace"
        val tempDir = "C:\\Temp"
        
        assertEquals("/workspace/model.mod", OplPathTranslator.translateToDockerPath(winPath, base, wDir, tempDir))
    }

    @Test
    fun testTranslateToDockerPath_PrefixBoundary() {
        val winPath = "C:\\ProjectB\\model.mod" // Should NOT map to /workspace if base is C:\Project
        val base = "C:\\Project"
        val wDir = "/workspace"
        val tempDir = "C:\\Temp"
        
        // Because of the prefix boundary fix (it adds /), it should not match and should return the original path (normalized)
        assertEquals("C:/ProjectB/model.mod", OplPathTranslator.translateToDockerPath(winPath, base, wDir, tempDir))
    }

    @Test
    fun testTranslateToDockerPath_TempDir() {
        val winPath = "C:\\Temp\\_temp_123_model.mod"
        val base = "C:\\Project"
        val wDir = "/workspace"
        val tempDir = "C:\\Temp"
        
        assertEquals("/tmp/_temp_123_model.mod", OplPathTranslator.translateToDockerPath(winPath, base, wDir, tempDir))
    }
}

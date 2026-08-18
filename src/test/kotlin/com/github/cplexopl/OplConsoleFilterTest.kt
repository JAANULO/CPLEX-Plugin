package com.github.cplexopl

import com.github.cplexopl.console.OplInfeasibilityFilter
import com.github.cplexopl.console.OplLinkFilter
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert

class OplConsoleFilterTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val basePath = project.basePath ?: return
        val baseDir = java.io.File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        java.io.File(baseDir, "infeasible-test.mod").createNewFile()
        java.io.File(baseDir, "data.dat").createNewFile()
        java.io.File(baseDir, "error-test.mod").createNewFile()
    }

    fun testInfeasibilityFilterMatchesConflict() {
        val filter = OplInfeasibilityFilter(project)
        val line = "ctInfeasible at 4:17-25 infeasible-test.mod"
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNotNull("Filter should match the conflict line", result)
        Assert.assertEquals("Hyperlink should cover the matched text", line.length, result!!.resultItems[0].highlightEndOffset)
    }

    fun testInfeasibilityFilterMatchesDatFile() {
        val filter = OplInfeasibilityFilter(project)
        val line = "myAssertion at 10:1-5 data.dat"
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNotNull("Filter should match .dat files", result)
    }

    fun testInfeasibilityFilterMatchesTempFilePath() {
        val filter = OplInfeasibilityFilter(project)
        val line = "ct1 at 4:8-16 C:\\Users\\atona\\AppData\\Local\\Temp\\_temp_242972bd-5b07-4fbf-b663-1084e80f9198_infeasible-test.mod"
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNotNull("Filter should match conflict line even with temp file path", result)
    }

    fun testInfeasibilityFilterIgnoresIrrelevantOutput() {
        val filter = OplInfeasibilityFilter(project)
        val line = "Version identifier: 22.1.2.0 | 2024-11-25 | 0edbb82fd"
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNull("Filter should ignore random lines", result)
    }

    fun testLinkFilterMatchesStandardError() {
        val filter = OplLinkFilter(project)
        val line = "Error at line 15: error-test.mod:15"
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNotNull("Link filter should match line error-test.mod:15", result)
    }

    fun testLinkFilterIgnoresNonMatchingLines() {
        val filter = OplLinkFilter(project)
        val line = "CPLEX 22.1.2.0: Optimal solution found."
        
        val result = filter.applyFilter(line, line.length)
        Assert.assertNull("Link filter should ignore lines without file:line pattern", result)
    }
}


package com.github.cplexopl

import com.github.cplexopl.console.OplInfeasibilityFilter
import com.github.cplexopl.console.OplLinkFilter
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertTrue
import kotlin.system.measureTimeMillis

class OplConsoleFilterPerformanceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val basePath = project.basePath ?: return
        val baseDir = java.io.File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        java.io.File(baseDir, "perf-test.mod").createNewFile()
    }

    fun testConsoleFilterPerformance100kLines() {
        val linkFilter = OplLinkFilter(project)
        val infeasibilityFilter = OplInfeasibilityFilter(project)

        val numLines = 100_000
        val sampleLines = listOf(
            "CPLEX 22.1.2.0: Optimal solution found.",
            "Iteration 1200: Objective = 452.1234",
            "ctInfeasible at 4:17-25 perf-test.mod",
            "Error: perf-test.mod:42",
            "Presolve time = 0.02 sec. (12.45 ticks)",
            "Gomory fractional cuts applied: 4"
        )

        val linesToProcess = ArrayList<String>(numLines)
        for (i in 0 until numLines) {
            linesToProcess.add(sampleLines[i % sampleLines.size])
        }

        val elapsed = measureTimeMillis {
            for (line in linesToProcess) {
                val len = line.length
                linkFilter.applyFilter(line, len)
                infeasibilityFilter.applyFilter(line, len)
            }
        }

        println("Processed $numLines console log lines through OplLinkFilter and OplInfeasibilityFilter in $elapsed ms")
        assertTrue("Filtrowanie 100k linii powinno trwać poniżej 5000 ms, trwało: ${elapsed}ms", elapsed < 5000)
    }
}

package com.github.cplexopl

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.system.measureTimeMillis

class OplAnnotatorPerformanceTest : BasePlatformTestCase() {

    fun testAnnotatorPerformance() {
        val numVariables = 1000
        val sb = java.lang.StringBuilder()
        sb.append("int n = 10;\n")
        
        // Generate declarations
        for (i in 1..numVariables) {
            sb.append("dvar int x_$i;\n")
        }
        
        // Generate uses inside loops
        sb.append("subject to {\n")
        for (i in 1..numVariables) {
            sb.append("  forall(i in 1..n) {\n")
            sb.append("    x_$i >= i;\n")
            sb.append("  }\n")
        }
        sb.append("}\n")
        
        val code = sb.toString()
        myFixture.configureByText("performance_test.mod", code)
        
        // Warmup
        myFixture.doHighlighting()
        
        val time = measureTimeMillis {
            // we have to modify the file slightly to clear caches and trigger re-highlighting
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                myFixture.editor.document.insertString(0, " ")
            }
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitAllDocuments()
            myFixture.doHighlighting()
        }
        
        println("Annotator execution time for $numVariables variables in loops: $time ms")
        // Just print, no strict assertion because time depends on the machine.
        // It's just for benchmark purposes.
    }
}

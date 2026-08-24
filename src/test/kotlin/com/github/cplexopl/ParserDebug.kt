package com.github.cplexopl

import com.github.cplexopl.parser.OplParserDefinition
import com.intellij.testFramework.ParsingTestCase
import org.junit.Test
import java.io.File

class ParserDebug : ParsingTestCase("debug", "mod", OplParserDefinition()) {

    override fun getTestDataPath(): String {
        return "C:/Users/Janusz/Documents/GitHub/cplex-opl-examples/models"
    }

    override fun skipSpaces(): Boolean = false
    override fun includeRanges(): Boolean = true

    @Test
    fun testNurseScheduling() {
        val file = File(testDataPath, "20-nurse-scheduling/model.mod")
        val text = file.readText()
        val psiFile = createPsiFile("model", text)
        ensureParsed(psiFile)
        val tree = toParseTreeText(psiFile, skipSpaces(), includeRanges())
        
        val errors = mutableListOf<String>()
        psiFile.accept(object : com.intellij.psi.PsiRecursiveElementVisitor() {
            override fun visitErrorElement(element: com.intellij.psi.PsiErrorElement) {
                super.visitErrorElement(element)
                errors.add("Error at offset ${element.textOffset}: ${element.errorDescription}")
            }
        })
        
        if (errors.isNotEmpty()) {
            println("=== SYNTAX ERRORS ===")
            errors.forEach { println(it) }
        } else {
            println("=== NO ERRORS ===")
        }
        
        File("C:/Users/Janusz/Documents/GitHub/cplex-opl-jetbrains/psi_dump.txt").writeText("ERRORS:\n" + errors.joinToString("\n") + "\n\nTREE:\n" + tree)
    }
}

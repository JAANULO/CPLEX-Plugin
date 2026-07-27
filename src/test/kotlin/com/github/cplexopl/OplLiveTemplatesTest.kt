package com.github.cplexopl

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class OplLiveTemplatesTest : BasePlatformTestCase() {

    fun testModelTemplate() {
        myFixture.configureByText(OplFileType, "model<caret>")
        myFixture.type("\t") // Trigger expansion
        // Przejdź tabulatorami przez wszystkie 5 zmiennych szablonu (NAME, DESCRIPTION, N, CTNAME, CONSTRAINT), aby zakończyć sesję szablonu
        repeat(5) {
            myFixture.type("\t")
        }
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            com.intellij.psi.codeStyle.CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        myFixture.checkResult(
            """
            // OPL Model: MyModel
            // Description: Model description
            
            // === DATA ===
            int n = 10;
            range R = 1..n;
            
            // === DECISION VARIABLES ===
            dvar float+ x[R];
            
            // === OBJECTIVE ===
            minimize
            sum(i in R) x[i];
            
            // === CONSTRAINTS ===
            subject to {
                forall(i in R) {
                    ct_constraint:
                    x[i] >= 0;
                }
            }
            
            """.trimIndent()
        )
    }
}

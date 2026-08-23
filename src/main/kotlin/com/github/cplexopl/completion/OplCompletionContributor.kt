package com.github.cplexopl.completion

import com.github.cplexopl.OplLanguage
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.github.cplexopl.OplBundle

// CompletionContributor = class adding autocomplete hints (Ctrl+Space)
// LookupElementBuilder = builder for hint list element

class OplCompletionContributor : CompletionContributor() {

    init {
        // Register hint provider for OPL language
        // PlatformPatterns.psiElement() = pattern for matching cursor location
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            OplKeywordCompletionProvider()
        )
    }
}

class OplKeywordCompletionProvider : CompletionProvider<CompletionParameters>() {

    // Keywords with descriptions - will appear on hint list
    private val keywords = listOf(
        // Data types
        "int" to OplBundle.message("completion.keyword.int"),
        "float" to OplBundle.message("completion.keyword.float"),
        "boolean" to OplBundle.message("completion.keyword.boolean"),
        "string" to OplBundle.message("completion.keyword.string"),
        "range" to OplBundle.message("completion.keyword.range"),

        // Decision variables
        "dvar" to OplBundle.message("completion.keyword.dvar"),
        "dexpr" to OplBundle.message("completion.keyword.dexpr"),

        // Optimization objective
        "minimize" to OplBundle.message("completion.keyword.minimize"),
        "maximize" to OplBundle.message("completion.keyword.maximize"),

        // Constraints
        "subject to" to OplBundle.message("completion.keyword.subject.to"),
        "forall" to OplBundle.message("completion.keyword.forall"),
        "exists" to OplBundle.message("completion.keyword.exists"),

        // Operators
        "sum" to OplBundle.message("completion.keyword.sum"),
        "all" to OplBundle.message("completion.keyword.all"),

        // Structures
        "tuple" to OplBundle.message("completion.keyword.tuple"),
        "execute" to OplBundle.message("completion.keyword.execute"),
        "include" to OplBundle.message("completion.keyword.include"),
        "assert" to OplBundle.message("completion.keyword.assert"),

        // Other
        "in" to OplBundle.message("completion.keyword.in"),
        "using" to OplBundle.message("completion.keyword.using"),
        "with" to OplBundle.message("completion.keyword.with")
    )

    // CPLEX OPL built-in functions
    private val builtinFunctions = listOf(
        "abs" to OplBundle.message("completion.func.abs"),
        "ceil" to OplBundle.message("completion.func.ceil"),
        "floor" to OplBundle.message("completion.func.floor"),
        "round" to OplBundle.message("completion.func.round"),
        "sqrt" to OplBundle.message("completion.func.sqrt"),
        "log" to OplBundle.message("completion.func.log"),
        "exp" to OplBundle.message("completion.func.exp"),
        "max" to OplBundle.message("completion.func.max"),
        "min" to OplBundle.message("completion.func.min"),
        "card" to OplBundle.message("completion.func.card")
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Add keywords (bold = bold because these are keywords)
        keywords.forEach { (keyword, description) ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText(description)
                    .bold()
            )
        }

        // Add built-in functions
        builtinFunctions.forEach { (func, description) ->
            result.addElement(
                LookupElementBuilder.create(func)
                    .withTypeText(description)
                    .withTailText("()", true)  // Add () as hint
            )
        }

// --- TASK 2.2 PRO: Semantic scanning of declarations from PSI tree ---
        val file = parameters.originalFile
        val declaredVariables = mutableSetOf<String>()

        // Helper function: extracts ID only from checked declaration nodes
        fun extractId(psiElement: com.intellij.psi.PsiElement) {
            psiElement.node.findChildByType(com.github.cplexopl.psi.OplTypes.ID)?.text?.let { declaredVariables.add(it) }
        }

        // Get only nodes that are formal declarations
        com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, com.github.cplexopl.psi.OplVarDeclaration::class.java).forEach { extractId(it) }
        com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, com.github.cplexopl.psi.OplDvarDeclaration::class.java).forEach { extractId(it) }
        com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, com.github.cplexopl.psi.OplTupleDeclaration::class.java).forEach { extractId(it) }

        // Add confirmed variables to autocomplete results
        declaredVariables.forEach { variable ->
            if (!variable.contains("IntellijIdeaRulezzz")) {
                result.addElement(
                    LookupElementBuilder.create(variable)
                        .withIcon(com.intellij.icons.AllIcons.Nodes.Variable)
                        .withTypeText(OplBundle.message("completion.local.variable"))
                )
            }
        }
    }
}

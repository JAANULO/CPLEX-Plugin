package com.github.cplexopl.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.lang.annotation.HighlightSeverity
import com.github.cplexopl.psi.*
import com.intellij.openapi.diagnostic.Logger

class OplAnnotator : Annotator {
    companion object {
        private val LOG = Logger.getInstance(OplAnnotator::class.java)
    }

    private fun resolveIncludedFile(currentFile: com.intellij.psi.PsiFile, relPath: String): com.intellij.psi.PsiFile? {
        val psiInDir = currentFile.containingDirectory?.findFile(relPath)
        if (psiInDir != null) return psiInDir

        val vFile = currentFile.originalFile.virtualFile ?: currentFile.virtualFile
        val parentVFile = vFile?.parent
        var targetVFile = parentVFile?.findFileByRelativePath(relPath) ?: parentVFile?.findChild(relPath)
        if (targetVFile != null) {
            val psi = currentFile.manager.findFile(targetVFile)
            if (psi != null) return psi
        }

        val candidates = mutableListOf<java.io.File>()
        if (vFile != null && vFile.path.isNotBlank()) {
            val parentIo = java.io.File(vFile.path).parentFile
            if (parentIo != null) candidates.add(java.io.File(parentIo, relPath))
        }
        val testDataDir = System.getProperty("testData.dir")
        if (!testDataDir.isNullOrBlank()) {
            val tdFile = java.io.File(testDataDir)
            candidates.add(java.io.File(tdFile, relPath))
            if (tdFile.exists()) {
                tdFile.walkTopDown().maxDepth(3).filter { it.name == relPath }.forEach { candidates.add(it) }
            }
        }

        for (cand in candidates) {
            if (cand.exists()) {
                val vf = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByIoFile(cand)
                    ?: com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(cand)
                if (vf != null) {
                    val psi = currentFile.manager.findFile(vf)
                    if (psi != null) return psi
                }
            }
        }

        return null
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // PROTECTION SHIELD: If any error occurs, catch it, thanks to which IDE never again hangs on "Analyzing"
        try {
            // --- 1. Type validation (boolean in range) ---
            if (element is OplIncludeDeclaration) {
                val strLiteral = element.node.findChildByType(OplTypes.STRING_LITERAL)?.text?.trim('"')
                if (strLiteral != null) {
                    val incPsi = resolveIncludedFile(element.containingFile, strLiteral)
                    if (incPsi == null) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Included file '$strLiteral' not found")
                            .range(element.textRange)
                            .create()
                    }
                }
            }

            if (element is OplDvarDeclaration) {
                val isBoolean = element.node.findChildByType(OplTypes.BOOLEAN) != null
                val hasRange = element.node.findChildByType(OplTypes.IN) != null

                if (isBoolean && hasRange) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Type 'boolean' cannot have a range ('in' clause)")
                        .range(element.textRange)
                        .create()
                }
            }

            // --- Inspections: Non-linearity (MIP) ---
            if (element is OplFactor) {
                val idNode = element.node.findChildByType(OplTypes.ID)
                if (idNode != null) {
                    val name = idNode.text
                    if (name == "min" || name == "max" || name == "abs") {
                        holder.newAnnotation(HighlightSeverity.WARNING, "Using non-linear function '$name' may increase computation time (MIP).")
                            .range(idNode.textRange)
                            .create()
                    }
                }
            }

            // --- Inspections: Validation of objective function in scheduling (CP) ---
            if (element is OplObjectiveDeclaration) {
                val file = element.containingFile
                if (file != null) {
                    val hasInterval = CachedValuesManager.getCachedValue(file) {
                        val found = PsiTreeUtil.findChildrenOfType(file, OplDvarDeclaration::class.java).any {
                            it.node.findChildByType(OplTypes.INTERVAL) != null || it.node.findChildByType(OplTypes.SEQUENCE) != null
                        }
                        CachedValueProvider.Result.create(found, PsiModificationTracker.MODIFICATION_COUNT)
                    }
                    if (hasInterval) {
                        val objText = element.text
                        if (!objText.contains("endOf") && !objText.contains("lengthOf") && !objText.contains("startOf") && !objText.contains("startAtEnd")) {
                            val targetRange = element.node.findChildByType(OplTypes.MINIMIZE)?.textRange ?: element.node.findChildByType(OplTypes.MAXIMIZE)?.textRange ?: element.textRange
                            holder.newAnnotation(HighlightSeverity.WARNING, "Missing timing function (e.g. endOf) in objective function in scheduling.")
                                .range(targetRange)
                                .create()
                        }
                    }
                }
            }

            // We analyze only variable tokens
            if (element.node.elementType == OplTypes.ID) {
                // Ignore properties after DOT (e.g. i.weight)
                var prev = element.node.treePrev
                while(prev != null && prev.elementType == com.intellij.psi.TokenType.WHITE_SPACE) {
                    prev = prev.treePrev
                }
                if (prev?.elementType == OplTypes.DOT) return

                // 0. Skip execute blocks (JS script, not OPL declarations)
                var p = element.parent
                while (p != null && p !is com.intellij.psi.PsiFile) {
                    if (p is OplExecuteBlock) return
                    p = p.parent
                }

                val name = element.text
                val parent = element.parent ?: return

                // Ignore CPLEX built-in words and global CP (Constraint Programming) functions
                val builtins = setOf(
                    "abs", "ceil", "floor", "max", "min", "sum", "forall",
                    "pulse", "step", "allDifferent", "pack", "all",
                    "endOf", "startOf", "lengthOf", "endBeforeStart", "startBeforeEnd",
                    "startAtEnd", "endAtStart", "startAtStart", "endAtEnd",
                    "noOverlap", "size", "card", "ord", "first", "last",
                    "item", "in", "length", "typeOf", "presenceOf", "val", "powerset",
                    "alwaysEqual", "alwaysIn", "alwaysConstant", "stateFunction", "cumulFunction", "piecewise"
                )

                if (builtins.contains(name)) return

                // Is this ID a variable declaration location?
                val isDeclaration = parent is OplDvarDeclaration ||
                        parent is OplVarDeclaration ||
                        parent is OplDexprDeclaration ||
                        parent is OplTupleDeclaration ||
                        parent is OplTupleField ||
                        parent is OplConstraintItem ||
                        parent is OplPiecewiseDeclaration ||
                        (parent is OplFactor && parent.node.findChildByType(OplTypes.SUM) != null)

                val file = element.containingFile ?: return
                val declaredVariables = CachedValuesManager.getCachedValue(file) {
                    val map = mutableMapOf<String, MutableList<PsiElement>>()
                    fun collectDeclarations(currentFile: com.intellij.psi.PsiFile, visited: MutableSet<com.intellij.psi.PsiFile>) {
                        if (!visited.add(currentFile)) return

                        fun registerDeclaration(declarationNode: PsiElement) {
                            val idNodes = declarationNode.node.getChildren(null).filter { it.elementType == OplTypes.ID }
                            val idNode = if (declarationNode is OplDvarDeclaration || declarationNode is OplTupleDeclaration || declarationNode is OplConstraintItem || declarationNode is OplPiecewiseDeclaration) {
                                idNodes.firstOrNull()
                            } else {
                                idNodes.lastOrNull()
                            }
                            if (idNode != null) {
                                map.computeIfAbsent(idNode.text) { mutableListOf() }.add(declarationNode)
                            }
                        }

                        PsiTreeUtil.findChildrenOfType(currentFile, OplVarDeclaration::class.java).forEach { registerDeclaration(it) }
                        PsiTreeUtil.findChildrenOfType(currentFile, OplDvarDeclaration::class.java).forEach { registerDeclaration(it) }
                        PsiTreeUtil.findChildrenOfType(currentFile, OplDexprDeclaration::class.java).forEach { registerDeclaration(it) }
                        PsiTreeUtil.findChildrenOfType(currentFile, OplTupleDeclaration::class.java).forEach { registerDeclaration(it) }
                        PsiTreeUtil.findChildrenOfType(currentFile, OplPiecewiseDeclaration::class.java).forEach { registerDeclaration(it) }

                        PsiTreeUtil.findChildrenOfType(currentFile, OplConstraintItem::class.java).forEach {
                            if (it.node.findChildByType(OplTypes.COLON) != null) {
                                registerDeclaration(it)
                            }
                        }

                        PsiTreeUtil.findChildrenOfType(currentFile, OplIncludeDeclaration::class.java).forEach { inc ->
                            val relPath = inc.node.findChildByType(OplTypes.STRING_LITERAL)?.text?.trim('"')
                            if (relPath != null) {
                                val incPsi = resolveIncludedFile(currentFile, relPath)
                                if (incPsi != null) {
                                    collectDeclarations(incPsi, visited)
                                }
                            }
                        }
                    }

                    collectDeclarations(file, mutableSetOf())
                    CachedValueProvider.Result.create(map, PsiModificationTracker.MODIFICATION_COUNT)
                }

                if (isDeclaration) {
                    // Checking for duplicates
                    val declarationsList = declaredVariables[name]
                    if (declarationsList != null && declarationsList.size > 1 && declarationsList.indexOf(parent) > 0) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Variable '$name' is already defined")
                            .range(element.textRange)
                            .create()
                    }

                    // Checking for missing semicolon (only for actual variable/type declarations)
                    val needsSemicolon = parent is OplDvarDeclaration ||
                            parent is OplVarDeclaration ||
                            parent is OplDexprDeclaration

                    if (needsSemicolon) {
                        var lastChild = parent.node.lastChildNode
                        while (lastChild != null && lastChild.elementType == com.intellij.psi.TokenType.WHITE_SPACE) {
                            lastChild = lastChild.treePrev
                        }

                        if (lastChild != null && lastChild.elementType != OplTypes.SEMICOLON) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Missing semicolon ';' at the end of declaration")
                                .range(element.textRange)
                                .create()
                        }
                    }

                } else {

                    // --- 2. Scope-aware analysis (Scope in loops and multiple iterators) ---
                    val localScopeVariables = CachedValuesManager.getCachedValue(file) {
                        val map = mutableMapOf<PsiElement, Set<String>>()
                        
                        val scopeNodes = PsiTreeUtil.findChildrenOfAnyType(file, 
                            OplFactor::class.java, 
                            OplConstraintItem::class.java, 
                            OplDvarDeclaration::class.java, 
                            OplDexprDeclaration::class.java, 
                            OplVarDeclaration::class.java,
                            OplAssertDeclaration::class.java,
                            OplAssertItem::class.java
                        )
                        
                        for (node in scopeNodes) {
                            val iterators = PsiTreeUtil.findChildrenOfType(node, OplOplIterator::class.java)
                            val vars = mutableSetOf<String>()
                            for (iter in iterators) {
                                val inNode = iter.node.findChildByType(OplTypes.IN)
                                val iterIds = iter.node.getChildren(null)
                                    .filter { it.elementType == OplTypes.ID && (inNode == null || it.startOffset < inNode.startOffset) }
                                    .map { it.text }
                                vars.addAll(iterIds)
                            }
                            if (vars.isNotEmpty()) map[node] = vars
                        }
                        
                        val allIterators = PsiTreeUtil.findChildrenOfType(file, OplOplIterator::class.java)
                        for (iter in allIterators) {
                            val inNode = iter.node.findChildByType(OplTypes.IN)
                            val iterIds = iter.node.getChildren(null)
                                .filter { it.elementType == OplTypes.ID && (inNode == null || it.startOffset < inNode.startOffset) }
                                .map { it.text }
                            if (iterIds.isNotEmpty()) map[iter] = iterIds.toSet()
                        }
                        
                        CachedValueProvider.Result.create(map, PsiModificationTracker.MODIFICATION_COUNT)
                    }

                    var isLocalVariable = false
                    var currentNode: PsiElement? = element.parent

                    while (currentNode != null && currentNode !is com.intellij.psi.PsiFile) {
                        val vars = localScopeVariables[currentNode]
                        if (vars != null && vars.contains(name)) {
                            isLocalVariable = true
                            break
                        }
                        currentNode = currentNode.parent
                    }

                    if (!isLocalVariable && !declaredVariables.containsKey(name)) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Undefined variable: '$name'")
                            .range(element.textRange)
                            .create()
                    }
                }
            }
        } catch (e: Exception) {
            // Suppress critical PSI exceptions so IDE continues to work
            LOG.warn("OplAnnotator failed on element: ${element.text.take(50)}", e)
        }
    }
}
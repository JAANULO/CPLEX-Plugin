package com.github.cplexopl.reference

import com.github.cplexopl.psi.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class OplReference(
    element: PsiElement,
    textRange: TextRange,
    private val variableName: String
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile ?: return null
        val targetName = variableName.trim()

        fun extractTargetToken(node: PsiElement): PsiElement? {
            val ids = node.node.getChildren(null).filter { it.elementType == OplTypes.ID }
            return ids.firstOrNull { it.text == targetName }?.psi
        }

        // 1. Hierarchical scope search: check enclosing iterators (forall, sum, etc.) in parent PSI tree
        var currentNode: PsiElement? = element.parent
        while (currentNode != null && currentNode !is PsiFile) {
            val iterators = PsiTreeUtil.findChildrenOfType(currentNode, OplOplIterator::class.java)
            for (iter in iterators) {
                val inNode = iter.node.findChildByType(OplTypes.IN)
                val iterIds = iter.node.getChildren(null)
                    .filter { it.elementType == OplTypes.ID && (inNode == null || it.startOffset < inNode.startOffset) }
                val match = iterIds.firstOrNull { it.text == targetName }
                if (match != null) {
                    return match.psi
                }
            }
            currentNode = currentNode.parent
        }

        // 2. Global declarations search
        val dvars = PsiTreeUtil.findChildrenOfType(file, OplDvarDeclaration::class.java)
        for (dvar in dvars) { extractTargetToken(dvar)?.let { return it } }

        val vars = PsiTreeUtil.findChildrenOfType(file, OplVarDeclaration::class.java)
        for (v in vars) { extractTargetToken(v)?.let { return it } }

        val dexprs = PsiTreeUtil.findChildrenOfType(file, OplDexprDeclaration::class.java)
        for (dexpr in dexprs) { extractTargetToken(dexpr)?.let { return it } }

        val tuples = PsiTreeUtil.findChildrenOfType(file, OplTupleDeclaration::class.java)
        for (t in tuples) { extractTargetToken(t)?.let { return it } }

        val piecewises = PsiTreeUtil.findChildrenOfType(file, OplPiecewiseDeclaration::class.java)
        for (p in piecewises) { extractTargetToken(p)?.let { return it } }

        val constraints = PsiTreeUtil.findChildrenOfType(file, OplConstraintItem::class.java)
        for (c in constraints) { extractTargetToken(c)?.let { return it } }

        return null
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = element as? OplFactor
        if (element != null) {
            val idNode = element.node.findChildByType(OplTypes.ID)
            if (idNode != null) {
                val newIdentifier = OplElementFactory.createIdentifier(element.project, newElementName)
                element.node.replaceChild(idNode, newIdentifier.node)
                return element
            }
        }
        return super.handleElementRename(newElementName)
    }
}
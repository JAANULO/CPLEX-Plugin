package com.github.cplexopl.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.util.regex.Pattern

class OplLinkFilter(private val project: Project) : Filter {
    private val targetExtensions = listOf(".mod:", ".dat:", ".ops:", ".prj:", ".oplproject:")

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        var matchIndex = -1
        var matchedExt = ""
        
        for (ext in targetExtensions) {
            matchIndex = line.indexOf(ext)
            if (matchIndex != -1) {
                matchedExt = ext
                break
            }
        }
        
        if (matchIndex == -1) return null

        val colonIndex = matchIndex + matchedExt.length - 1
        var endIndex = colonIndex + 1
        while (endIndex < line.length && line[endIndex].isDigit()) {
            endIndex++
        }
        
        if (endIndex == colonIndex + 1) return null
        val lineNumber = line.substring(colonIndex + 1, endIndex).toIntOrNull()?.minus(1) ?: return null

        val startOfPath = line.lastIndexOf(' ', matchIndex).takeIf { it != -1 }?.plus(1) ?: 0
        val filePath = line.substring(startOfPath, colonIndex).trim()
        
        if (filePath.isEmpty()) return null

            val virtualFile = OplFileResolver.resolve(project, filePath)

            if (virtualFile != null) {
                val info = OpenFileHyperlinkInfo(project, virtualFile, lineNumber)
                val start = entireLength - line.length + startOfPath
                val end = entireLength - line.length + endIndex
                return Filter.Result(start, end, info)
            }
        
        return null
    }
}
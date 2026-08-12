package com.github.cplexopl.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.util.regex.Pattern

class OplInfeasibilityFilter(private val project: Project) : Filter {
    // Matches: "ctName at 4:17-25 model.mod" -> line=4, colStart=17, path=model.mod
    private val conflictPattern = Pattern.compile("(?:at\\s+)?(?<line>\\d+):(?<colStart>\\d+)-(?<colEnd>\\d+)\\s+(?<path>[^\\s]+\\.(?:mod|dat))")

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val matcher = conflictPattern.matcher(line)
        if (matcher.find()) {
            val filePath = matcher.group("path")
            val lineNumber = matcher.group("line").toInt() - 1 // IntelliJ counts from 0
            val columnNumber = matcher.group("colStart").toInt() - 1

            val normalizedPath = filePath.replace('\\', '/')
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(normalizedPath)
                ?: project.basePath?.let { basePath ->
                    val fullPath = "$basePath/$normalizedPath".replace('\\', '/')
                    LocalFileSystem.getInstance().findFileByPath(fullPath)
                }

            if (virtualFile != null) {
                val info = OpenFileHyperlinkInfo(project, virtualFile, lineNumber, columnNumber)
                val start = entireLength - line.length + matcher.start()
                val end = entireLength - line.length + matcher.end()
                return Filter.Result(start, end, info)
            }
        }
        return null
    }
}

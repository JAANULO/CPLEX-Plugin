package com.github.cplexopl.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.util.regex.Pattern

class OplLinkFilter(private val project: Project) : Filter {
    // Pattern capturing file path and line, e.g. "C:\test.mod:15" or "test.mod:15"
    private val pattern = Pattern.compile("(?<path>[a-zA-Z]:[/\\\\][^:]+\\.mod|[^:]+\\.mod):(?<line>\\d+)")

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val filePath = matcher.group("path").trim()
            val lineNumber = matcher.group("line").toInt() - 1 // IntelliJ counts from 0

            val virtualFile = OplFileResolver.resolve(project, filePath)

            if (virtualFile != null) {
                val info = OpenFileHyperlinkInfo(project, virtualFile, lineNumber)
                val start = entireLength - line.length + matcher.start()
                val end = entireLength - line.length + matcher.end()
                return Filter.Result(start, end, info)
            }
        }
        return null
    }
}
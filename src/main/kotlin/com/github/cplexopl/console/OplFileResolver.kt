package com.github.cplexopl.console

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object OplFileResolver {
    private val cache = ConcurrentHashMap<String, VirtualFile>()

    fun resolve(project: Project, rawPath: String): VirtualFile? {
        val cacheKey = "${project.name}:${project.basePath}:$rawPath"
        val cached = cache[cacheKey]
        if (cached != null && cached.isValid) {
            return cached
        }

        val resolved = doResolve(project, rawPath)
        if (resolved != null) {
            if (cache.size > 1000) {
                cache.clear()
            }
            cache[cacheKey] = resolved
        }
        return resolved
    }

    private fun doResolve(project: Project, rawPath: String): VirtualFile? {
        val normalizedPath = rawPath.replace('\\', '/')
        val fileName = File(normalizedPath).name
        
        // Extract real name if it's a temp file created by plugin (_temp_<uuid>_<realName>)
        val realFileName = if (fileName.startsWith("_temp_")) {
            val parts = fileName.split("_", limit = 4)
            if (parts.size >= 4) parts[3] else fileName
        } else {
            fileName
        }

        // 1. Direct path lookup if not temp
        if (!fileName.startsWith("_temp_")) {
            val directFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(normalizedPath)
            if (directFile != null && directFile.exists()) return directFile

            val basePath = project.basePath
            if (basePath != null) {
                val fullPath = "$basePath/$normalizedPath".replace('\\', '/')
                val relativeFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(fullPath)
                if (relativeFile != null && relativeFile.exists()) return relativeFile
            }
        }

        // 2. Search project workspace via FilenameIndex
        try {
            val files = FilenameIndex.getVirtualFilesByName(realFileName, GlobalSearchScope.projectScope(project))
            val firstFound = files.firstOrNull()
            if (firstFound != null && firstFound.isValid) return firstFound
        } catch (e: Exception) {
            // Fallback if index is not ready during test
        }

        // 3. Fallback: check project basePath / realFileName directly via LocalFileSystem
        val basePath = project.basePath
        if (basePath != null) {
            val projectFile = LocalFileSystem.getInstance().refreshAndFindFileByPath("$basePath/$realFileName".replace('\\', '/'))
            if (projectFile != null && projectFile.exists()) return projectFile
        }

        return null
    }

    fun clearCache() {
        cache.clear()
    }
}

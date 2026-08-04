package com.github.cplexopl.utils // Change to your actual package

import com.intellij.openapi.util.SystemInfo
import java.io.File

object CplexPathFinder {

    fun find(): String? {
        val envPath = findFromEnvVariable()
        if (envPath != null && File(envPath).exists()) {
            return envPath
        }
        return findDefaultOplrunPath()
    }

    private fun findFromEnvVariable(): String? {
        val envDir = System.getenv("CPLEX_STUDIO_DIR") ?: return null
        return when {
            SystemInfo.isWindows -> "$envDir\\opl\\bin\\x64_win64\\oplrun.exe"
            SystemInfo.isMac -> "$envDir/opl/bin/x86-64_osx/oplrun"
            else -> "$envDir/opl/bin/x86-64_linux/oplrun"
        }
    }

    /**
     * Scans default installation paths looking for CPLEX engine.
     * Returns absolute path to oplrun.exe or null if not found.
     */
    fun findDefaultOplrunPath(): String? {
        val baseDirs = mutableListOf<String>()
        val relativeBinPath: String
        val executableName: String

        // 1. Determine search strategy based on operating system
        when {
            SystemInfo.isWindows -> {
                baseDirs.add("C:\\Program Files\\IBM\\ILOG")
                relativeBinPath = "opl\\bin\\x64_win64"
                executableName = "oplrun.exe"
            }
            SystemInfo.isMac -> {
                baseDirs.add("/Applications/IBM/ILOG")
                baseDirs.add("/Applications") // Some users install directly here
                relativeBinPath = "opl/bin/x86-64_osx"
                executableName = "oplrun"
            }
            else -> { // Linux family systems
                baseDirs.add("/opt/ibm/ILOG")
                baseDirs.add("/opt")
                relativeBinPath = "opl/bin/x86-64_linux"
                executableName = "oplrun"
            }
        }

        // 2. Search through defined base directories
        for (baseDirPath in baseDirs) {
            val baseDir = File(baseDirPath)
            if (!baseDir.exists() || !baseDir.isDirectory) continue

            // 3. Get all folders whose name starts with "CPLEX_Studio"
            val studioDirs = baseDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("CPLEX_Studio")
            } ?: continue

            // 4. Sort descending - if user has multiple versions installed,
            //    use the newest one (e.g., 2212 will be checked before 1210)
            val sortedDirs = studioDirs.sortedByDescending { it.name }

            // 5. Check if executable file physically exists inside version folder
            for (studioDir in sortedDirs) {
                val oplrunFile = File(studioDir, "$relativeBinPath${File.separator}$executableName")
                if (oplrunFile.exists() && oplrunFile.canExecute()) {
                    return oplrunFile.absolutePath
                }
            }
        }

       return null // Returns null if heuristic failed
    }
}
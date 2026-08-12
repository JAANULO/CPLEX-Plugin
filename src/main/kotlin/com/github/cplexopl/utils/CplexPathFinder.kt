package com.github.cplexopl.utils

import java.io.File

object CplexPathFinder {

    fun find(
        envProvider: (String) -> String? = System::getenv,
        osProvider: () -> String = { System.getProperty("os.name") }
    ): String? {
        val envPath = findFromEnvVariable(envProvider, osProvider)
        if (envPath != null && File(envPath).exists()) {
            return envPath
        }
        return findDefaultOplrunPath(osProvider = osProvider)
    }

    private fun findFromEnvVariable(
        envProvider: (String) -> String?,
        osProvider: () -> String
    ): String? {
        val envDir = envProvider("CPLEX_STUDIO_DIR") ?: return null
        val osName = osProvider().lowercase()
        val isWindows = osName.contains("win")
        val isMac = osName.contains("mac")

        val relativeParts = when {
            isWindows -> listOf("opl", "bin", "x64_win64", "oplrun.exe")
            isMac -> listOf("opl", "bin", "x86-64_osx", "oplrun")
            else -> listOf("opl", "bin", "x86-64_linux", "oplrun")
        }

        val file = relativeParts.fold(File(envDir)) { parent, child -> File(parent, child) }
        return file.absolutePath
    }

    /**
     * Scans default installation paths looking for CPLEX engine.
     * Returns absolute path to oplrun.exe or null if not found.
     */
    fun findDefaultOplrunPath(
        customBaseDirs: List<String>? = null,
        osProvider: () -> String = { System.getProperty("os.name") }
    ): String? {
        val osName = osProvider().lowercase()
        val isWindows = osName.contains("win")
        val isMac = osName.contains("mac")

        val baseDirs = customBaseDirs?.toMutableList() ?: mutableListOf()

        if (customBaseDirs == null) {
            when {
                isWindows -> {
                    baseDirs.add("C:\\Program Files\\IBM\\ILOG")
                }
                isMac -> {
                    baseDirs.add("/Applications/IBM/ILOG")
                    baseDirs.add("/Applications")
                }
                else -> {
                    baseDirs.add("/opt/ibm/ILOG")
                    baseDirs.add("/opt")
                }
            }
        }

        val relativeBinParts = when {
            isWindows -> listOf("opl", "bin", "x64_win64")
            isMac -> listOf("opl", "bin", "x86-64_osx")
            else -> listOf("opl", "bin", "x86-64_linux")
        }
        val executableName = if (isWindows) "oplrun.exe" else "oplrun"

        for (baseDirPath in baseDirs) {
            val baseDir = File(baseDirPath)
            if (!baseDir.exists() || !baseDir.isDirectory) continue

            val studioDirs = baseDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("CPLEX_Studio")
            } ?: continue

            val sortedDirs = studioDirs.sortedByDescending { it.name }

            for (studioDir in sortedDirs) {
                val oplrunFile = (relativeBinParts + executableName).fold(studioDir) { parent, child -> File(parent, child) }
                if (oplrunFile.exists() && oplrunFile.canExecute()) {
                    return oplrunFile.absolutePath
                }
            }
        }

        return null
    }
}
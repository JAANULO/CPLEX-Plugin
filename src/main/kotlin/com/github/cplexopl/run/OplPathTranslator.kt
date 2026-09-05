package com.github.cplexopl.run

object OplPathTranslator {

    fun translateToWslPath(winPath: String): String {
        if (winPath.length >= 2 && winPath[1] == ':') {
            val drive = winPath[0].lowercaseChar()
            return "/mnt/$drive" + winPath.substring(2).replace('\\', '/')
        }
        return winPath.replace('\\', '/')
    }

    fun translateToDockerPath(winPath: String, projectBase: String, wDir: String, tempDir: String): String {
        val normalizedWin = winPath.replace('\\', '/')
        val normalizedBase = projectBase.replace('\\', '/').let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }
        val normalizedTemp = tempDir.replace('\\', '/').let { if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it }
        
        if (normalizedBase.isNotEmpty() && normalizedWin.startsWith(normalizedBase, ignoreCase = true)) {
            val wDirSlash = if (wDir.endsWith("/")) wDir else "$wDir/"
            return wDirSlash + normalizedWin.substring(normalizedBase.length)
        }
        if (normalizedTemp.isNotEmpty() && normalizedWin.startsWith(normalizedTemp, ignoreCase = true)) {
            return "/tmp/" + normalizedWin.substring(normalizedTemp.length)
        }
        return normalizedWin
    }
}

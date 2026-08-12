package com.github.cplexopl.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CplexPathFinderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createFile(parent: File, vararg names: String): File {
        val dir = names.dropLast(1).fold(parent) { p, name -> File(p, name) }.apply { mkdirs() }
        return File(dir, names.last()).apply { createNewFile(); setExecutable(true) }
    }

    @Test
    fun testFindFromEnvVariableWindows() {
        val fakeEnvDir = tempFolder.newFolder("fake_cplex_env")
        val exeFile = createFile(fakeEnvDir, "opl", "bin", "x64_win64", "oplrun.exe")

        val found = CplexPathFinder.find(
            envProvider = { if (it == "CPLEX_STUDIO_DIR") fakeEnvDir.absolutePath else null },
            osProvider = { "Windows 11" }
        )

        assertNotNull(found)
        assertEquals(exeFile.absolutePath, found)
    }

    @Test
    fun testFindDefaultOplrunPathWindowsHighestVersion() {
        val baseDir = tempFolder.newFolder("IBM_ILOG")

        // Older version 1210
        createFile(baseDir, "CPLEX_Studio1210", "opl", "bin", "x64_win64", "oplrun.exe")

        // Newer version 2212
        val v2212Exe = createFile(baseDir, "CPLEX_Studio2212", "opl", "bin", "x64_win64", "oplrun.exe")

        val found = CplexPathFinder.findDefaultOplrunPath(
            customBaseDirs = listOf(baseDir.absolutePath),
            osProvider = { "Windows 11" }
        )

        assertNotNull(found)
        assertEquals(v2212Exe.absolutePath, found)
    }

    @Test
    fun testFindDefaultOplrunPathLinux() {
        val baseDir = tempFolder.newFolder("opt_ibm")
        val v2212Exe = createFile(baseDir, "CPLEX_Studio2212", "opl", "bin", "x86-64_linux", "oplrun")

        val found = CplexPathFinder.findDefaultOplrunPath(
            customBaseDirs = listOf(baseDir.absolutePath),
            osProvider = { "Linux" }
        )

        assertNotNull(found)
        assertEquals(v2212Exe.absolutePath, found)
    }

    @Test
    fun testFindReturnsNullWhenNotFound() {
        val emptyDir = tempFolder.newFolder("empty")
        val found = CplexPathFinder.findDefaultOplrunPath(
            customBaseDirs = listOf(emptyDir.absolutePath),
            osProvider = { "Windows 11" }
        )
        assertNull(found)
    }
}

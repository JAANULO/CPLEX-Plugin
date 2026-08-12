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

    @Test
    fun testFindFromEnvVariableWindows() {
        val fakeEnvDir = tempFolder.newFolder("fake_cplex_env")
        val binDir = File(fakeEnvDir, "opl\\bin\\x64_win64").apply { mkdirs() }
        val exeFile = File(binDir, "oplrun.exe").apply { createNewFile(); setExecutable(true) }

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
        val v1210Bin = File(baseDir, "CPLEX_Studio1210\\opl\\bin\\x64_win64").apply { mkdirs() }
        File(v1210Bin, "oplrun.exe").apply { createNewFile(); setExecutable(true) }

        // Newer version 2212
        val v2212Bin = File(baseDir, "CPLEX_Studio2212\\opl\\bin\\x64_win64").apply { mkdirs() }
        val v2212Exe = File(v2212Bin, "oplrun.exe").apply { createNewFile(); setExecutable(true) }

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
        val v2212Bin = File(baseDir, "CPLEX_Studio2212/opl/bin/x86-64_linux").apply { mkdirs() }
        val v2212Exe = File(v2212Bin, "oplrun").apply { createNewFile(); setExecutable(true) }

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

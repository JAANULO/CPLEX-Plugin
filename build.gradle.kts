import java.io.File
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import org.gradle.api.tasks.testing.TestListener
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
    // Update of this line:
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        // Added PythonCore plugin for compilation and tests
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })
        testFramework(TestFrameworkType.Platform)
    }
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/gen")
        java.srcDirs("src/main/gen")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        vendor {
            name = providers.gradleProperty("pluginVendor")
        }

        description = providers.fileContents(layout.projectDirectory.file("docs/DESCRIPTION.md")).asText.map { markdownToHTML(it) }

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    pluginVerification {
        ides {
            // Using a manual list of Community Edition (IC) builds instead of recommended(),
            // because recommended() downloads ~6 IDEs (including Ultimate ~3 GB each),
            // which exhausts the GitHub Actions runner disk (~14 GB) and causes a tar extraction error.
            // sinceBuild = 243, so we verify from 2024.3 onwards.
            // IC = IntelliJ IDEA Community Edition product code
            create("IC", "2024.3.7.1")
            create("IC", "2025.1.7.1")
            create("IC", "2025.2.6.1")
        }
    }
}

changelog {
    path = file("docs/CHANGELOG.md").path
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
    versionPrefix = ""
    headerParserRegex = """(\d+\.\d+\.\d+(?:\.\d+)?)""".toRegex()
}

kover {
    reports {
        total {
            xml { onCheck = true }
        }
    }
}

tasks {
    named<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateLexer") {
        sourceFile.set(layout.projectDirectory.file("src/main/grammars/OplLexer.flex"))
        // Revert to the newer name and Directory type
        targetOutputDir.set(layout.projectDirectory.dir("src/main/gen/com/github/cplexopl/lexer"))
        // targetClass removed - the plugin will read it from the .flex file
        purgeOldFiles.set(true)
    }

    named<org.jetbrains.grammarkit.tasks.GenerateParserTask>("generateParser") {
        sourceFile.set(layout.projectDirectory.file("src/main/grammars/OplGrammar.bnf"))
        // Revert to the newer name and Directory type
        targetRootOutputDir.set(layout.projectDirectory.dir("src/main/gen"))
        pathToParser.set("com/github/cplexopl/parser/OplParser.java")
        pathToPsiRoot.set("com/github/cplexopl/psi")
        purgeOldFiles.set(true)
    }

    compileKotlin {
        dependsOn("generateLexer", "generateParser")
    }

    compileJava {
        dependsOn("generateLexer", "generateParser")
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}
// Disable instrumentation tasks (for both code and tests) due to an MS JDK bug
tasks.matching { it.name == "instrumentCode" || it.name == "instrumentTestCode" }.configureEach {
    enabled = false
}
tasks.withType<Test> {
    val isCi = providers.environmentVariable("CI").isPresent
    val availableCores = Runtime.getRuntime().availableProcessors()
    val osBean = ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean
    @Suppress("DEPRECATION")
    val totalRamBytes = osBean?.totalMemorySize ?: osBean?.totalPhysicalMemorySize ?: 0L
    val totalRamGb = totalRamBytes / (1024 * 1024 * 1024)

    if (isCi) {
        maxParallelForks = 1
        minHeapSize = "512m"
        maxHeapSize = "2g"
    } else {
        maxParallelForks = 1
        maxHeapSize = if (totalRamGb >= 16) "2g" else "1g"
    }

    filter {
        includeTestsMatching("com.github.cplexopl.OplTestSuite")
    }

    val testDetails = Collections.synchronizedList(mutableListOf<Map<String, Any>>())
    val pluginVer = providers.gradleProperty("pluginVersion").get()
    val reportsDir = layout.projectDirectory.dir("src/test/reports").asFile

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            testDetails.add(
                mapOf(
                    "name" to testDescriptor.name,
                    "className" to (testDescriptor.className ?: "Unknown"),
                    "resultType" to result.resultType.name,
                    "durationMs" to (result.endTime - result.startTime)
                )
            )
        }
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                val nowWarsaw = LocalDateTime.now(ZoneId.of("Europe/Warsaw"))
                val fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val filename = "test-summary-${nowWarsaw.format(fileFormatter)}.json"
                val summaryFile = File(reportsDir, filename)

                val totalDuration = result.endTime - result.startTime
                val currentOsBean = ManagementFactory.getOperatingSystemMXBean()
                val testsJson = testDetails.joinToString(",\n                  ") { test ->
                    """{"name": "${test["name"]}", "className": "${test["className"]}", "result": "${test["resultType"]}", "durationMs": ${test["durationMs"]}}"""
                }
                val summaryJson = """
                {
                  "timestamp": "${nowWarsaw.format(displayFormatter)} (Europe/Warsaw)",
                  "pluginVersion": "$pluginVer",
                  "result": "${result.resultType}",
                  "totalTests": ${result.testCount},
                  "successfulTests": ${result.successfulTestCount},
                  "failedTests": ${result.failedTestCount},
                  "skippedTests": ${result.skippedTestCount},
                  "durationMs": $totalDuration,
                  "environment": {
                    "os": "${currentOsBean.name}",
                    "arch": "${currentOsBean.arch}",
                    "availableProcessors": ${currentOsBean.availableProcessors}
                  },
                  "tests": [
                    $testsJson
                  ]
                }
                """.trimIndent()

                reportsDir.mkdirs()
                summaryFile.writeText(summaryJson)
            }
        }
    })

    testLogging { showStandardStreams = true }
    systemProperty("idea.tests.overwrite.data", "true")
}

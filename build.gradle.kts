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

        description.set("Native support for IBM ILOG CPLEX OPL in JetBrains IDEs: syntax highlighting, code completion, and a built-in oplrun runner.")

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
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
    versionPrefix = ""
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
    testLogging { showStandardStreams = true } 
    systemProperty("idea.tests.overwrite.data", "true")
}

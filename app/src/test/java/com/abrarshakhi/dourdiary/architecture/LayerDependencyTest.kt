package com.abrarshakhi.dourdiary.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LayerDependencyTest {

    private val sourceRoot = File("src/main/java/com/abrarshakhi/dourdiary")

    private val forbiddenInDomain = listOf(
        "android.",
        "androidx.",
        "org.koin.",
        "com.google.android.",
    )

    @Test
    fun `the guard is actually looking at source files`() {
        assertTrue(
            "Expected sources at ${sourceRoot.absolutePath}; this test's path is stale.",
            sourceRoot.isDirectory,
        )
        assertTrue("Found no domain sources to check.", domainFiles().isNotEmpty())
        assertTrue("Found no data sources to check.", layerFiles("data").isNotEmpty())
    }

    @Test
    fun `domain does not depend on frameworks`() {
        val violations = domainFiles().flatMap { file ->
            importsOf(file)
                .filter { import -> forbiddenInDomain.any(import::startsWith) }
                .map { import -> "${relativePath(file)} imports $import" }
        }

        assertTrue(
            "Domain must stay pure Kotlin:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `domain does not depend on data or presentation`() {
        val violations = domainFiles().flatMap { file ->
            importsOf(file)
                .filter { import ->
                    import.startsWith("com.abrarshakhi.dourdiary") &&
                        (import.contains(".data.") || import.contains(".presentation."))
                }
                .map { import -> "${relativePath(file)} imports $import" }
        }

        assertTrue(
            "Domain must not know about outer layers:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `data does not depend on presentation`() {
        val violations = layerFiles("data").flatMap { file ->
            importsOf(file)
                .filter { it.startsWith("com.abrarshakhi.dourdiary") && it.contains(".presentation.") }
                .map { import -> "${relativePath(file)} imports $import" }
        }

        assertTrue(
            "Data must not know about presentation:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    private fun domainFiles(): List<File> = layerFiles("domain")

    private fun layerFiles(layer: String): List<File> =
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.invariantPath().contains("/$layer/") }
            .toList()

    private fun importsOf(file: File): List<String> =
        file.readLines()
            .mapNotNull { line -> line.trim().removePrefixOrNull("import ") }
            .map { it.substringBefore(" as ").trim() }

    private fun relativePath(file: File): String =
        file.invariantPath().substringAfter(sourceRoot.invariantPath() + "/")

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private fun String.removePrefixOrNull(prefix: String): String? =
        if (startsWith(prefix)) removePrefix(prefix) else null
}

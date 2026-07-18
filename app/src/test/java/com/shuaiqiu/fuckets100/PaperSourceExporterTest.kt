package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

class PaperSourceExporterTest {
    @Test
    fun exportFileNameRemovesUnsafeCharacters() {
        val name = PaperSourceExporter.buildExportFileName("试卷/:*?\"<>|. ", 0L)

        assertTrue(name.startsWith("Fe_Source_试卷_________"))
        assertTrue(name.endsWith("19700101_000000_000.zip"))
        assertFalse(name.contains('/'))
    }

    @Test
    fun localDirectoryExportFileNameUsesDedicatedPrefix() {
        val name = PaperSourceExporter.buildLocalDirectoryExportFileName(0L)

        assertEquals("Fe_Local_Files_19700101_000000_000.zip", name)
    }

    @Test
    fun localDirectoryExportBlocksEmptyOversizedAndMultipleParsedPapers() {
        assertEquals(
            PaperSourceExporter.LocalDirectoryExportBlockReason.EMPTY,
            PaperSourceExporter.localDirectoryExportBlockReason(0, 0L, 0)
        )
        assertEquals(
            PaperSourceExporter.LocalDirectoryExportBlockReason.TOO_MANY_PARSED_PAPERS,
            PaperSourceExporter.localDirectoryExportBlockReason(1, 1L, 3)
        )
        assertEquals(
            PaperSourceExporter.LocalDirectoryExportBlockReason.TOO_LARGE,
            PaperSourceExporter.localDirectoryExportBlockReason(1, 150L * 1024L * 1024L + 1L, 0)
        )
        assertEquals(null, PaperSourceExporter.localDirectoryExportBlockReason(1, 150L * 1024L * 1024L, 2))
    }

    @Test
    fun localDirectoryReadmeListsFilesAndEmptyParseResult() {
        val inspection = PaperSourceExporter.LocalDirectoryInspection(
            mode = ActivationMode.DIRECT_READ,
            files = listOf(
                PaperSourceExporter.LocalDirectoryFile(
                    sourcePath = "/source/data/index.json",
                    archivePath = "data/index.json",
                    size = 42L
                ),
                PaperSourceExporter.LocalDirectoryFile(
                    sourcePath = "/source/resource/item/content.json",
                    archivePath = "resource/item/content.json",
                    size = 84L
                )
            ),
            skipped = listOf("resource/link（符号链接，已跳过）"),
            totalBytes = 126L,
            parsedPapers = emptyList()
        )

        val readme = PaperSourceExporter.buildLocalDirectoryReadme(inspection)

        assertTrue(readme.contains("data/index.json"))
        assertTrue(readme.contains("resource/item/content.json"))
        assertTrue(readme.contains("未识别到可解析试卷"))
        assertTrue(readme.contains("resource/link（符号链接，已跳过）"))
    }

    @Test
    fun cloudUrlUsesHttpsAndPreservesAbsoluteUrls() {
        assertEquals(
            "https://cdn.example.com/base/paper.zip",
            PaperSourceExporter.resolveCloudUrl("http://cdn.example.com/base/", "/paper.zip")
        )
        assertEquals(
            "https://files.example.com/paper.zip",
            PaperSourceExporter.resolveCloudUrl("https://ignored.example.com", "http://files.example.com/paper.zip")
        )
    }

    @Test
    fun resourceReferencesSupportOldAndNewIndexes() {
        val raw = """
            {
              "sectionData": [{"sectionItemData": [{"fileName": "old_resource"}]}],
              "data": [{"struct": {"contents": [
                {"url": "https://cdn.example.com/new_resource.zip?token=1"},
                {"url": "../unsafe.zip"}
              ]}}]
            }
        """.trimIndent()

        assertEquals(
            setOf("old_resource", "new_resource", "unsafe"),
            PaperSourceExporter.extractResourceReferences(raw)
        )
    }

    @Test
    fun invalidIndexReturnsNoReferences() {
        assertTrue(PaperSourceExporter.extractResourceReferences("not-json").isEmpty())
    }

    @Test
    fun cloudCacheFilesUseFullUrlIdentity() {
        val cacheDir = File("cache")

        val first = PaperSourceExporter.cloudCachedFile(cacheDir, "https://a.example/source.zip")
        val second = PaperSourceExporter.cloudCachedFile(cacheDir, "https://b.example/source.zip")

        assertFalse(first.name == second.name)
        assertTrue(first.name.endsWith("_source.zip"))
    }

    @Test
    fun zipValidationRejectsTruncatedFiles() {
        val directory = createTempDirectory("source-export-test").toFile()
        try {
            val valid = File(directory, "valid.zip")
            ZipOutputStream(valid.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
            val corrupt = File(directory, "corrupt.zip").apply { writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) }

            assertTrue(PaperSourceExporter.isUsableZip(valid))
            assertFalse(PaperSourceExporter.isUsableZip(corrupt))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun zipValidationAcceptsEtsPackageWithCustomFooter() {
        val directory = createTempDirectory("ets-source-export-test").toFile()
        try {
            val file = File(directory, "ets-source.zip")
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("content.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
            val footer = ByteArray(336)
            "MSTCHINA".toByteArray().copyInto(footer)
            file.appendBytes(footer)

            assertTrue(ZipPasswordGenerator.hasValidEtsFooter(file))
            assertTrue(PaperSourceExporter.isUsableZip(file))
        } finally {
            directory.deleteRecursively()
        }
    }
}

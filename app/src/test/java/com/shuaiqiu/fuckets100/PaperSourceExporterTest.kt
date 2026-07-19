package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

class PaperSourceExporterTest {
    @Test
    fun directorySizeKibParserReturnsBytesAndRejectsInvalidOutput() {
        assertEquals(1536L * 1024L, ETS100FileReader.parseDirectorySizeKib("1536\t/resource\n"))
        assertEquals(0L, ETS100FileReader.parseDirectorySizeKib("not-a-size"))
        assertEquals(0L, ETS100FileReader.parseDirectorySizeKib(null))
    }

    @Test
    fun tarStagingExtractsRegularFilesIntoLocalDirectory() {
        val directory = createTempDirectory("local-export-tar-test").toFile()
        try {
            val tar = tarWithFile("resource/paper/content.json", "{\"id\":1}".toByteArray())

            val count = PaperSourceExporter.extractTarToDirectory(ByteArrayInputStream(tar), directory)

            assertEquals(1, count)
            assertEquals("{\"id\":1}", File(directory, "resource/paper/content.json").readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun exportFileNameRemovesUnsafeCharacters() {
        val name = PaperSourceExporter.buildExportFileName("试卷/:*?\"<>|. ", 0L)

        assertTrue(name.startsWith("Fe_Source_试卷_________"))
        assertTrue(name.endsWith("${exportTimestamp(0L)}.zip"))
        assertFalse(name.contains('/'))
    }

    @Test
    fun localDirectoryExportFileNameUsesDedicatedPrefix() {
        val name = PaperSourceExporter.buildLocalDirectoryExportFileName(0L)

        assertEquals("Fe_Local_Files_${exportTimestamp(0L)}.zip", name)
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
    fun localDirectoryReadmeListsTopLevelFilesAndUngroupedResources() {
        val inspection = PaperSourceExporter.LocalDirectoryInspection(
            mode = ActivationMode.DIRECT_READ,
            dataFiles = listOf(
                PaperSourceExporter.LocalDirectoryFile(
                    sourcePath = "/source/data/index.json",
                    archivePath = "data/index.json",
                    size = 42L
                )
            ),
            resourceDirectories = listOf(
                PaperSourceExporter.LocalResourceDirectory("/source/resource/item", "item")
            ),
            skipped = listOf("resource/link（符号链接，已跳过）"),
            totalBytes = 126L,
            parsedPapers = emptyList()
        )

        val readme = PaperSourceExporter.buildLocalDirectoryReadme(inspection)

        assertTrue(readme.contains("data/index.json"))
        assertTrue(readme.contains("resource/item/"))
        assertTrue(readme.contains("未解析或未分组资源"))
        assertFalse(readme.contains("resource/item/content.json"))
        assertTrue(readme.contains("resource/link（符号链接，已跳过）"))
    }

    @Test
    fun localDirectoryReadmeMarksIncompleteBackgroundParsing() {
        val inspection = PaperSourceExporter.LocalDirectoryInspection(
            mode = ActivationMode.DIRECT_READ,
            dataFiles = emptyList(),
            resourceDirectories = emptyList(),
            skipped = emptyList(),
            totalBytes = 0L,
            parsedPapers = emptyList(),
            isParsingInProgress = true
        )

        assertTrue(
            PaperSourceExporter.buildLocalDirectoryReadme(inspection)
                .contains("后台解析尚未完成")
        )
    }

    @Test
    fun localDirectoryReadmeGroupsResourcesByParsedPaper() {
        val question = ETS100AnswerReader.Question(
            order = 1,
            sectionOrder = 1,
            sectionCaption = "分区",
            typeName = "题型",
            questionText = "题目",
            answers = emptyList(),
            originalText = null
        )
        val paper = ETS100AnswerReader.Paper(
            paperId = 1L,
            title = "已解析试卷",
            dataFileName = "data.json",
            fileSize = 1L,
            sections = listOf(ETS100AnswerReader.Section("分区", "type", "题型", listOf(question), null)),
            source = PaperSource.Local(ActivationMode.DIRECT_READ, listOf("grouped", "shared"))
        )
        val otherPaper = paper.copy(
            paperId = 2L,
            title = "另一套试卷",
            source = PaperSource.Local(ActivationMode.DIRECT_READ, listOf("shared"))
        )
        val inspection = PaperSourceExporter.LocalDirectoryInspection(
            mode = ActivationMode.DIRECT_READ,
            dataFiles = emptyList(),
            resourceDirectories = listOf(
                PaperSourceExporter.LocalResourceDirectory("/resource/grouped", "grouped"),
                PaperSourceExporter.LocalResourceDirectory("/resource/shared", "shared"),
                PaperSourceExporter.LocalResourceDirectory("/resource/ungrouped", "ungrouped")
            ),
            skipped = emptyList(),
            totalBytes = 1L,
            parsedPapers = listOf(paper, otherPaper)
        )

        val readme = PaperSourceExporter.buildLocalDirectoryReadme(inspection)

        assertTrue(readme.contains("已解析试卷 1：已解析试卷"))
        assertTrue(readme.contains("- resource/grouped/"))
        assertTrue(readme.contains("共享/多组引用资源："))
        assertTrue(readme.contains("resource/shared/ -> 已解析试卷, 另一套试卷"))
        assertTrue(readme.contains("未解析或未分组资源："))
        assertTrue(readme.contains("- resource/ungrouped/"))
    }

    @Test
    fun completedParsedPapersExcludesLoadingAndEmptyPapers() {
        val question = ETS100AnswerReader.Question(
            order = 1,
            sectionOrder = 1,
            sectionCaption = "分区",
            typeName = "题型",
            questionText = "题目",
            answers = emptyList(),
            originalText = null
        )
        val parsedPaper = ETS100AnswerReader.Paper(
            paperId = 1L,
            title = "已解析",
            dataFileName = "data.json",
            fileSize = 1L,
            sections = listOf(
                ETS100AnswerReader.Section("分区", "type", "题型", listOf(question), null)
            )
        )
        val loadingPaper = parsedPaper.copy(
            paperId = 2L,
            title = "解析中",
            sections = listOf(
                ETS100AnswerReader.Section("解析中", "local_loading", "解析中", emptyList(), null)
            )
        )
        val emptyPaper = parsedPaper.copy(
            paperId = 3L,
            title = "空试卷",
            sections = emptyList()
        )

        assertEquals(
            listOf(parsedPaper),
            PaperSourceExporter.completedParsedPapers(listOf(parsedPaper, loadingPaper, emptyPaper))
        )
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

    private fun exportTimestamp(timestamp: Long): String =
        SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))

    private fun tarWithFile(name: String, content: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val header = ByteArray(512)
        name.toByteArray().copyInto(header)
        "0000644\u0000".toByteArray().copyInto(header, destinationOffset = 100)
        content.size.toString(8).padStart(11, '0').plus('\u0000').toByteArray()
            .copyInto(header, destinationOffset = 124)
        header[156] = '0'.code.toByte()
        output.write(header)
        output.write(content)
        val padding = (512 - content.size % 512) % 512
        output.write(ByteArray(padding))
        output.write(ByteArray(1024))
        return output.toByteArray()
    }
}

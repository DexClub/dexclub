package io.github.dexclub.core.impl.dex

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun assertNoJavaExportTempsLeaked(
    workspaceDir: File,
    workspace: io.github.dexclub.core.api.workspace.WorkspaceContext,
    outputFile: File,
) {
    assertTrue(!File(outputFile.parentFile, "${outputFile.name}.tmp.dex").exists())
    assertTrue(
        outputFile.parentFile.listFiles()
            ?.none { it.name.startsWith(".jadx-tmp-") } != false,
    )
    val exportTempDir = File(
        workspaceDir,
        ".dexclub/targets/${workspace.activeTargetId}/cache/exports/tmp",
    )
    assertTrue(exportTempDir.isDirectory)
    assertTrue(exportTempDir.listFiles()?.isEmpty() != false)
}

internal fun assertApkDexCacheContainsOnly(
    workspaceDir: File,
    workspace: io.github.dexclub.core.api.workspace.WorkspaceContext,
    vararg fileNames: String,
) {
    val apkDexDir = File(
        workspaceDir,
        ".dexclub/targets/${workspace.activeTargetId}/cache/decoded/apk-dex",
    )
    assertTrue(apkDexDir.isDirectory)
    assertTrue(File(apkDexDir, ".content-fingerprint").isFile)
    val dexFiles = apkDexDir.listFiles()
        ?.filter { it.isFile && it.extension == "dex" }
        ?.map { it.name }
        ?.sorted()
        ?: emptyList()
    assertEquals(fileNames.sorted(), dexFiles)
}

internal fun isDexFile(file: File): Boolean {
    val header = ByteArray(8)
    file.inputStream().use { input ->
        val read = input.read(header)
        if (read < header.size) return false
    }
    return header.decodeToString() == "dex\n035\u0000"
}

internal fun assertClassSourceMapContainsSingleDexSource(
    indexFile: File,
    expectedFingerprint: String,
    expectedSourcePath: String,
) {
    assertTrue(indexFile.isFile)
    val root = Json.parseToJsonElement(indexFile.readText()).jsonObject
    assertEquals(expectedFingerprint, root.getValue("contentFingerprint").jsonPrimitive.content)
    val sources = root.getValue("sources").jsonArray
    assertEquals(1, sources.size)
    assertEquals(expectedSourcePath, sources.single().jsonObject.getValue("sourcePath").jsonPrimitive.content)
    assertEquals(0, sources.single().jsonObject.getValue("id").jsonPrimitive.int)
    val mappings = root.getValue("mappings").jsonObject
    assertEquals(0, mappings.getValue("Lfixture/samples/SampleSearchTarget;").jsonPrimitive.int)
}

internal class DexAnalysisFixture(
    val dexWorkspaceDir: File,
    val dexFile: File,
    val ambiguousWorkspaceDir: File,
    val ambiguousApkFile: File,
    val apkWorkspaceDir: File,
    val apkFile: File,
) {
    companion object {
        fun generated(): DexAnalysisFixture {
            val root = Files.createTempDirectory("dexclub-core-dex-fixture").toFile()
            val sampleClasses = compileJava(
                root = root,
                fileName = "SampleSearchTarget.java",
                source = """
                    package fixture.samples;
                    public class SampleSearchTarget {
                        public static final String NEEDLE = "dexclub-needle-string";
                        public void acceptObjects(Object[] values) {
                        }
                        public String exposeNeedle() {
                            return NEEDLE;
                        }
                    }
                """.trimIndent(),
            )
            val anotherClasses = compileJava(
                root = root,
                fileName = "AnotherSearchTarget.java",
                source = """
                    package fixture.samples;
                    public class AnotherSearchTarget {
                        public static final String NEEDLE = "dexclub-needle-string";
                        public String exposeNeedle() {
                            return NEEDLE;
                        }
                    }
                """.trimIndent(),
            )

            val sampleDex = compileDex(root, "sample", sampleClasses)
            val duplicateSampleDex = compileDex(root, "duplicate", sampleClasses)

            val dexWorkspaceDir = File(root, "dex-input").also(File::mkdirs)
            val dexFile = compileDex(root, "fixture", sampleClasses, anotherClasses)
                .copyTo(File(dexWorkspaceDir, "fixture.dex"), overwrite = true)

            val ambiguousWorkspaceDir = File(root, "ambiguous-dex-input").also(File::mkdirs)
            val ambiguousApkFile = File(ambiguousWorkspaceDir, "fixture.apk")
            createPseudoApk(
                outputApk = ambiguousApkFile,
                "classes.dex" to sampleDex,
                "classes2.dex" to duplicateSampleDex,
            )

            val apkWorkspaceDir = File(root, "apk-input").also(File::mkdirs)
            val apkFile = File(apkWorkspaceDir, "fixture.apk")
            createPseudoApk(apkFile, "classes.dex" to sampleDex)

            return DexAnalysisFixture(
                dexWorkspaceDir = dexWorkspaceDir,
                dexFile = dexFile,
                ambiguousWorkspaceDir = ambiguousWorkspaceDir,
                ambiguousApkFile = ambiguousApkFile,
                apkWorkspaceDir = apkWorkspaceDir,
                apkFile = apkFile,
            )
        }

        private fun compileJava(root: File, fileName: String, source: String): File {
            val sourceDir = File(root, "src-$fileName/fixture/samples").also(File::mkdirs)
            val sourceFile = File(sourceDir, fileName).apply {
                writeText(source, Charsets.UTF_8)
            }
            val classesDir = File(root, "classes-$fileName").also(File::mkdirs)
            runCommand(
                command = listOf(
                    "javac",
                    "--release",
                    "8",
                    "-d",
                    classesDir.absolutePath,
                    sourceFile.absolutePath,
                ),
                workingDirectory = root,
            )
            return classesDir
        }

        private fun compileDex(root: File, name: String, vararg classesDirs: File): File {
            val dexDir = File(root, "dex-$name").also(File::mkdirs)
            val classFiles = classesDirs.asSequence()
                .flatMap { classesDir ->
                    classesDir.walkTopDown()
                        .asSequence()
                        .filter { it.isFile && it.extension == "class" }
                        .map { it.absolutePath }
                }
                .toList()
            runCommand(
                command = buildList {
                    add(resolveD8Command())
                    add("--min-api")
                    add("21")
                    add("--output")
                    add(dexDir.absolutePath)
                    addAll(classFiles)
                },
                workingDirectory = root,
            )
            return File(dexDir, "classes.dex")
        }

        private fun resolveD8Command(): String {
            val envRoots = listOfNotNull(
                System.getenv("ANDROID_SDK_ROOT"),
                System.getenv("ANDROID_HOME"),
            ).map(::File)
            val candidates = buildList {
                envRoots.forEach { root ->
                    root.resolve("build-tools").listFiles()
                        ?.sortedByDescending(File::getName)
                        ?.forEach { buildToolsDir ->
                            add(buildToolsDir.resolve("d8").path)
                            add(buildToolsDir.resolve("d8.bat").path)
                        }
                }
                add("d8")
                add("d8.bat")
            }
            return candidates.firstOrNull { candidate ->
                runCatching {
                    val process = ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true)
                        .start()
                    process.inputStream.bufferedReader().use { it.readText() }
                    process.waitFor() == 0
                }.getOrDefault(false)
            } ?: error("未找到可用的 d8 命令")
        }

        private fun runCommand(command: List<String>, workingDirectory: File) {
            val process = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            check(process.waitFor() == 0) {
                buildString {
                    appendLine("命令执行失败: ${command.joinToString(" ")}")
                    append(output)
                }
            }
        }

        private fun createPseudoApk(outputApk: File, vararg entries: Pair<String, File>) {
            ZipOutputStream(outputApk.outputStream().buffered()).use { zip ->
                entries.forEach { (name, sourceDex) ->
                    zip.putNextEntry(ZipEntry(name))
                    sourceDex.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }
}

internal const val QUERY_SEARCH_TARGETS =
    """{"matcher":{"className":{"value":"SearchTarget","matchType":"Contains","ignoreCase":true}}}"""

internal const val QUERY_SAMPLE_CLASS =
    """{"matcher":{"className":{"value":"fixture.samples.SampleSearchTarget","matchType":"Equals"}}}"""

internal const val QUERY_EXPOSE_METHOD =
    """{"matcher":{"name":{"value":"exposeNeedle","matchType":"Equals"}}}"""

internal const val QUERY_NEEDLE_FIELD =
    """{"matcher":{"name":{"value":"NEEDLE","matchType":"Equals"}}}"""

internal const val QUERY_CLASS_USING_STRINGS_SINGLE_GROUP =
    """{"groups":{"needle":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}"""

internal const val QUERY_CLASS_USING_STRINGS_DUP_GROUPS =
    """{"groups":{"needle-a":[{"value":"dexclub-needle-string","matchType":"Equals"}],"needle-b":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}"""

internal const val QUERY_METHOD_USING_STRINGS_SINGLE_GROUP =
    """{"groups":{"needle":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}"""

internal const val QUERY_METHOD_USING_STRINGS_DUP_GROUPS =
    """{"groups":{"needle-a":[{"value":"dexclub-needle-string","matchType":"Equals"}],"needle-b":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}"""

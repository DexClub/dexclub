package io.github.dexclub.core.impl.dex

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import io.github.dexclub.core.api.dex.DexExportError
import io.github.dexclub.core.api.dex.ExportClassDexRequest
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodDexRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.shared.createDefaultServices
import io.github.dexclub.core.api.workspace.WorkspaceRef
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultDexAnalysisExportTest {
    @Test
    fun exportClassSmaliWritesSmaliForUniqueClass() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "SampleSearchTarget.smali")

        val result = services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        assertTrue(output.isFile)
        val text = output.readText()
        assertTrue(text.contains("Lfixture/samples/SampleSearchTarget;"))
        assertTrue(text.contains(".method public exposeNeedle()Ljava/lang/String;"))
    }

    @Test
    fun exportClassSmaliRequiresSourceWhenClassIsAmbiguous() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val error = assertFailsWith<DexExportError> {
            services.dex.exportClassSmali(
                workspace = workspace,
                request = ExportClassSmaliRequest(
                    className = "fixture.samples.SampleSearchTarget",
                    outputPath = File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.smali").absolutePath,
                ),
            )
        }

        assertEquals(io.github.dexclub.core.api.dex.DexExportErrorReason.AmbiguousClass, error.reason)
        val message = error.message.orEmpty()
        assertTrue(
            message.contains("fixture.samples.SampleSearchTarget"),
            "ambiguous-class message should retain the class name, was: $message",
        )
        assertTrue(
            message.contains("--source-path"),
            "ambiguous-class message should mention --source-path, was: $message",
        )
    }

    @Test
    fun exportClassSmaliCanBeNarrowedBySourcePath() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))
        val output = File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget-from-second.smali")

        val result = services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                source = SourceLocator(sourcePath = "fixture.apk", sourceEntry = "classes2.dex"),
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        assertTrue(output.readText().contains("Lfixture/samples/SampleSearchTarget;"))
    }

    @Test
    fun exportMethodSmaliWritesSnippetByDefault() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "exposeNeedle.snippet.smali")

        val result = services.dex.exportMethodSmali(
            workspace = workspace,
            request = io.github.dexclub.core.api.dex.ExportMethodSmaliRequest(
                methodSignature = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        val text = output.readText()
        assertTrue(text.startsWith(".method public exposeNeedle()Ljava/lang/String;"))
        assertTrue(!text.contains(".class "))
        assertTrue(!text.contains(".field "))
        assertTrue(!text.contains("callExposeNeedle"))
        assertTrue(text.contains("return-object"))
    }

    @Test
    fun exportMethodSmaliClassModeBuildsMinimalShell() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "exposeNeedle.class.smali")

        val result = services.dex.exportMethodSmali(
            workspace = workspace,
            request = io.github.dexclub.core.api.dex.ExportMethodSmaliRequest(
                methodSignature = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                outputPath = output.absolutePath,
                mode = MethodSmaliMode.Class,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        val text = output.readText()
        assertTrue(text.contains(".class public Lfixture/samples/SampleSearchTarget;"))
        assertTrue(text.contains(".method public exposeNeedle()Ljava/lang/String;"))
        assertTrue(!text.contains(".field public static final NEEDLE:"))
        assertTrue(!text.contains(".method public constructor <init>()V"))
        assertTrue(!text.contains(".method public callExposeNeedle()Ljava/lang/String;"))
    }

    @Test
    fun exportMethodSmaliRequiresSourceWhenMethodClassIsAmbiguous() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val error = assertFailsWith<DexExportError> {
            services.dex.exportMethodSmali(
                workspace = workspace,
                request = io.github.dexclub.core.api.dex.ExportMethodSmaliRequest(
                    methodSignature = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                    outputPath = File(fixture.ambiguousWorkspaceDir, "exposeNeedle.smali").absolutePath,
                ),
            )
        }

        assertEquals(io.github.dexclub.core.api.dex.DexExportErrorReason.AmbiguousClass, error.reason)
    }

    @Test
    fun exportMethodDexWritesMethodOnlyDex() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "exposeNeedle.method.dex")

        val result = services.dex.exportMethodDex(
            workspace = workspace,
            request = ExportMethodDexRequest(
                methodSignature = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        val dexFile = DexBackedDexFile(Opcodes.getDefault(), Files.readAllBytes(output.toPath()))
        val classDef = dexFile.classes.single()
        assertEquals("Lfixture/samples/SampleSearchTarget;", classDef.type)
        assertTrue(classDef.staticFields.none())
        assertTrue(classDef.instanceFields.none())
        val methods = classDef.methods.toList()
        assertEquals(1, methods.size)
        assertEquals("exposeNeedle", methods.single().name)
        assertEquals("()Ljava/lang/String;", buildString {
            append('(')
            methods.single().parameterTypes.forEach { append(it) }
            append(')')
            append(methods.single().returnType)
        })
    }

    @Test
    fun exportMethodJavaWritesMethodOnlyJavaForUniqueMethod() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "exposeNeedle.method.java")

        val result = services.dex.exportMethodJava(
            workspace = workspace,
            request = ExportMethodJavaRequest(
                methodSignature = "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        assertTrue(output.isFile)
        val text = output.readText()
        assertTrue(text.contains("class SampleSearchTarget"))
        assertTrue(text.contains("exposeNeedle()"))
        assertTrue(text.contains("dexclub-needle-string"))
        assertTrue(!text.contains("callExposeNeedle("))
        assertNoJavaExportTempsLeaked(fixture.dexWorkspaceDir, workspace, output)
    }

    @Test
    fun exportClassDexWritesSingleClassDexForUniqueClass() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "SampleSearchTarget.dex")

        val result = services.dex.exportClassDex(
            workspace = workspace,
            request = ExportClassDexRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        assertTrue(output.isFile)
        assertTrue(isDexFile(output))
    }

    @Test
    fun exportClassDexRequiresSourceWhenClassIsAmbiguous() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val error = assertFailsWith<DexExportError> {
            services.dex.exportClassDex(
                workspace = workspace,
                request = ExportClassDexRequest(
                    className = "fixture.samples.SampleSearchTarget",
                    outputPath = File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.dex").absolutePath,
                ),
            )
        }

        assertEquals(io.github.dexclub.core.api.dex.DexExportErrorReason.AmbiguousClass, error.reason)
    }

    @Test
    fun exportClassJavaWritesSingleClassJavaForUniqueClass() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val output = File(fixture.dexWorkspaceDir, "SampleSearchTarget.java")

        val result = services.dex.exportClassJava(
            workspace = workspace,
            request = ExportClassJavaRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = output.absolutePath,
            ),
        )

        assertEquals(output.absolutePath, result.outputPath)
        assertTrue(output.isFile)
        val text = output.readText()
        assertTrue(text.contains("class SampleSearchTarget"))
        assertTrue(text.contains("dexclub-needle-string"))
        assertNoJavaExportTempsLeaked(fixture.dexWorkspaceDir, workspace, output)
    }

    @Test
    fun exportClassJavaRequiresSourceWhenClassIsAmbiguous() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.ambiguousApkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.ambiguousWorkspaceDir.absolutePath))

        val error = assertFailsWith<DexExportError> {
            services.dex.exportClassJava(
                workspace = workspace,
                request = ExportClassJavaRequest(
                    className = "fixture.samples.SampleSearchTarget",
                    outputPath = File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.java").absolutePath,
                ),
            )
        }

        assertEquals(io.github.dexclub.core.api.dex.DexExportErrorReason.AmbiguousClass, error.reason)
    }

    @Test
    fun exportCreatesClassSourceMapWhenMissing() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val indexFile = File(fixture.dexWorkspaceDir, ".dexclub/targets/${workspace.activeTargetId}/cache/indexes/class-source-map.json")
        assertTrue(!indexFile.exists())

        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = File(fixture.dexWorkspaceDir, "SampleSearchTarget.smali").absolutePath,
            ),
        )

        assertClassSourceMapContainsSingleDexSource(
            indexFile = indexFile,
            expectedFingerprint = workspace.snapshot.contentFingerprint,
            expectedSourcePath = "fixture.dex",
        )
    }

    @Test
    fun exportCreatesClassSourceMapWithApkEntryPrecision() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.apkFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.apkWorkspaceDir.absolutePath))
        val indexFile = File(fixture.apkWorkspaceDir, ".dexclub/targets/${workspace.activeTargetId}/cache/indexes/class-source-map.json")
        assertTrue(!indexFile.exists())

        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = File(fixture.apkWorkspaceDir, "SampleSearchTarget.smali").absolutePath,
            ),
        )

        val root = kotlinx.serialization.json.Json.parseToJsonElement(indexFile.readText()).jsonObject
        val sources = root.getValue("sources").jsonArray
        assertEquals(1, sources.size)
        val source = sources.single().jsonObject
        assertEquals("fixture.apk", source.getValue("sourcePath").jsonPrimitive.content)
        assertEquals("classes.dex", source.getValue("sourceEntry").jsonPrimitive.content)
        assertEquals(0, source.getValue("id").jsonPrimitive.int)
        val mappings = root.getValue("mappings").jsonObject
        assertEquals(0, mappings.getValue("Lfixture/samples/SampleSearchTarget;").jsonPrimitive.int)
    }

    @Test
    fun exportUsesValidClassSourceMapWithoutRescanningOtherSources() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val indexFile = File(fixture.dexWorkspaceDir, ".dexclub/targets/${workspace.activeTargetId}/cache/indexes/class-source-map.json")

        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = File(fixture.dexWorkspaceDir, "first-export.smali").absolutePath,
            ),
        )
        assertTrue(indexFile.isFile)

        File(fixture.dexWorkspaceDir, "ignored.bin").writeText("broken", Charsets.UTF_8)
        val indexedExport = File(fixture.dexWorkspaceDir, "second-export.smali")
        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = indexedExport.absolutePath,
            ),
        )
        assertTrue(indexedExport.readText().contains("Lfixture/samples/SampleSearchTarget;"))
    }

    @Test
    fun exportRebuildsStaleClassSourceMap() {
        val fixture = DexAnalysisFixture.generated()
        val services = createDefaultServices()
        services.workspace.initialize(fixture.dexFile.absolutePath)
        val workspace = services.workspace.open(WorkspaceRef(fixture.dexWorkspaceDir.absolutePath))
        val indexFile = File(fixture.dexWorkspaceDir, ".dexclub/targets/${workspace.activeTargetId}/cache/indexes/class-source-map.json")

        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = File(fixture.dexWorkspaceDir, "first-export.smali").absolutePath,
            ),
        )

        val staleIndex = """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-04-25T12:26:00Z",
              "targetId": "${workspace.activeTargetId}",
              "toolVersion": "test",
              "contentFingerprint": "stale-fingerprint",
              "format": "class-source-map-v2",
              "sources": [
                {
                  "id": 0,
                  "sourcePath": "fixture.dex",
                  "sourceEntry": null
                }
              ],
              "mappings": {
                "Lfixture/samples/SampleSearchTarget;": 0
              }
            }
        """.trimIndent()
        indexFile.writeText(staleIndex, Charsets.UTF_8)

        val rebuiltExport = File(fixture.dexWorkspaceDir, "third-export.smali")
        services.dex.exportClassSmali(
            workspace = workspace,
            request = ExportClassSmaliRequest(
                className = "fixture.samples.SampleSearchTarget",
                outputPath = rebuiltExport.absolutePath,
            ),
        )

        val rebuilt = kotlinx.serialization.json.Json.parseToJsonElement(indexFile.readText()).jsonObject
        assertEquals(workspace.snapshot.contentFingerprint, rebuilt.getValue("contentFingerprint").jsonPrimitive.content)
        val sources = rebuilt.getValue("sources").jsonArray
        assertEquals(1, sources.size)
        assertEquals("fixture.dex", sources.single().jsonObject.getValue("sourcePath").jsonPrimitive.content)
        val mappings = rebuilt.getValue("mappings").jsonObject
        assertEquals(0, mappings["Lfixture/samples/SampleSearchTarget;"]?.jsonPrimitive?.int)
        kotlin.test.assertNotNull(mappings["Lfixture/samples/AnotherSearchTarget;"])
    }
}

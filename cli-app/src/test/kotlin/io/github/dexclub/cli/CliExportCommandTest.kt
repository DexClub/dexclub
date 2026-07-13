package io.github.dexclub.cli

import io.github.dexclub.core.app.createDefaultAppServices
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliExportCommandTest {
    @Test
    fun exportClassSmaliRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "SampleSearchTarget.smali")
        val output = runCli(
            app,
            listOf(
                "export-class-smali",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        assertTrue(outputFile.isFile)
        assertTrue(outputFile.readText().contains("Lfixture/samples/SampleSearchTarget;"))
    }

    @Test
    fun exportClassSmaliReturnsWorkspaceErrorWhenClassIsAmbiguous() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.ambiguousWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.ambiguousApkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "export-class-smali",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.smali").absolutePath,
            ),
        )

        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("specify --source-path"), output.stderr)
    }

    @Test
    fun exportMethodSmaliRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "exposeNeedle.snippet.smali")
        val output = runCli(
            app,
            listOf(
                "export-method-smali",
                "--method",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        val text = outputFile.readText()
        assertTrue(text.startsWith(".method public exposeNeedle()Ljava/lang/String;"))
        assertTrue(!text.contains(".field "))
        assertTrue(!text.contains("callExposeNeedle"))
    }

    @Test
    fun exportMethodSmaliClassModeRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "exposeNeedle.class.smali")
        val output = runCli(
            app,
            listOf(
                "export-method-smali",
                "--method",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--output",
                outputFile.absolutePath,
                "--mode",
                "class",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val text = outputFile.readText()
        assertTrue(text.contains(".class public Lfixture/samples/SampleSearchTarget;"))
        assertTrue(!text.contains(".field public static final NEEDLE:"))
        assertTrue(!text.contains(".method public callExposeNeedle()Ljava/lang/String;"))
    }

    @Test
    fun exportMethodDexRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "exposeNeedle.method.dex")
        val output = runCli(
            app,
            listOf(
                "export-method-dex",
                "--method",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun exportMethodJavaRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "exposeNeedle.method.java")
        val output = runCli(
            app,
            listOf(
                "export-method-java",
                "--method",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        val text = outputFile.readText()
        assertTrue(text.contains("class SampleSearchTarget"))
        assertTrue(text.contains("exposeNeedle()"))
        assertTrue(text.contains("dexclub-needle-string"))
        assertTrue(!text.contains("callExposeNeedle("))
    }

    @Test
    fun exportClassDexRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "SampleSearchTarget.dex")
        val output = runCli(
            app,
            listOf(
                "export-class-dex",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        assertTrue(outputFile.isFile)
        assertTrue(isDexFile(outputFile))
    }

    @Test
    fun exportClassDexReturnsWorkspaceErrorWhenClassIsAmbiguous() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.ambiguousWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.ambiguousApkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "export-class-dex",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.dex").absolutePath,
            ),
        )

        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("specify --source-path"), output.stderr)
    }

    @Test
    fun exportClassJavaRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val outputFile = File(fixture.dexWorkspaceDir, "SampleSearchTarget.java")
        val output = runCli(
            app,
            listOf(
                "export-class-java",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                outputFile.absolutePath,
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        assertEquals("output=${outputFile.absolutePath}", output.stdout.trimEnd())
        val text = outputFile.readText()
        assertTrue(text.contains("class SampleSearchTarget"))
        assertTrue(text.contains("dexclub-needle-string"))
    }

    @Test
    fun exportClassJavaReturnsWorkspaceErrorWhenClassIsAmbiguous() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultAppServices(),
            cwdProvider = { fixture.ambiguousWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.ambiguousApkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "export-class-java",
                "--class",
                "fixture.samples.SampleSearchTarget",
                "--output",
                File(fixture.ambiguousWorkspaceDir, "SampleSearchTarget.java").absolutePath,
            ),
        )

        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("specify --source-path"), output.stderr)
    }
}

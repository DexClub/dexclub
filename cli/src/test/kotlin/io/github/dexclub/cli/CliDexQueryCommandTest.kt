package io.github.dexclub.cli

import io.github.dexclub.core.api.shared.createDefaultServices
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliDexQueryCommandTest {
    @Test
    fun findClassRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val queryFile = File(fixture.dexWorkspaceDir, "find-class.json").apply {
            writeText(
                """{"matcher":{"className":{"value":"SearchTarget","matchType":"Contains","ignoreCase":true}}}""",
                Charsets.UTF_8,
            )
        }
        val output = runCli(
            app,
            listOf(
                "find-class",
                "--query-file",
                queryFile.absolutePath,
                "--offset",
                "1",
                "--limit",
                "1",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(1, parsed.size)
        val hit = parsed.single().jsonObject
        assertEquals("Lfixture/samples/SampleSearchTarget;", hit.getValue("className").jsonPrimitive.content)
        assertEquals("fixture.dex", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun findMethodRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-method",
                "--query-json",
                """{"matcher":{"name":{"value":"exposeNeedle","matchType":"Equals"}}}""",
                "--offset",
                "1",
                "--limit",
                "1",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(1, parsed.size)
        val hit = parsed.single().jsonObject
        assertEquals("fixture.samples.SampleSearchTarget", hit.getValue("className").jsonPrimitive.content)
        assertEquals("exposeNeedle", hit.getValue("methodName").jsonPrimitive.content)
        assertEquals("fixture.dex", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun findMethodAcceptsShellQuotedQueryJson() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-method",
                "--query-json",
                """'{"matcher":{"name":{"value":"exposeNeedle","matchType":"Equals"}}}'""",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(2, parsed.size)
    }

    @Test
    fun findFieldRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-field",
                "--query-json",
                """{"matcher":{"name":{"value":"NEEDLE","matchType":"Equals"}}}""",
                "--offset",
                "1",
                "--limit",
                "1",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(1, parsed.size)
        val hit = parsed.single().jsonObject
        assertEquals("fixture.samples.SampleSearchTarget", hit.getValue("className").jsonPrimitive.content)
        assertEquals("NEEDLE", hit.getValue("fieldName").jsonPrimitive.content)
        assertEquals("fixture.dex", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun findClassUsingStringsRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-class-using-strings",
                "--query-json",
                """{"groups":{"needle-a":[{"value":"dexclub-needle-string","matchType":"Equals"}],"needle-b":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}""",
                "--offset",
                "1",
                "--limit",
                "1",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        assertEquals(1, parsed.size)
        val hit = parsed.single().jsonObject
        assertEquals("Lfixture/samples/SampleSearchTarget;", hit.getValue("className").jsonPrimitive.content)
        assertEquals("fixture.dex", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun findMethodUsingStringsRunsThroughCliPipeline() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "find-method-using-strings",
                "--query-json",
                """{"groups":{"needle-a":[{"value":"dexclub-needle-string","matchType":"Equals"}],"needle-b":[{"value":"dexclub-needle-string","matchType":"Equals"}]}}""",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonArray
        val hit = parsed.firstOrNull { element ->
            val method = element.jsonObject
            method["className"]?.jsonPrimitive?.content == "fixture.samples.SampleSearchTarget" &&
                method["methodName"]?.jsonPrimitive?.content == "exposeNeedle"
        }?.jsonObject
        assertTrue(hit != null, output.stdout)
        assertEquals("fixture.dex", hit.getValue("sourcePath").jsonPrimitive.content)
    }

    @Test
    fun inspectMethodReturnsRequestedMethodDetailsAsJson() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SampleSearchTarget;->readMutableNeedle()Ljava/lang/String;",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        val method = parsed.getValue("method").jsonObject
        assertEquals("fixture.samples.SampleSearchTarget", method.getValue("className").jsonPrimitive.content)
        assertEquals("readMutableNeedle", method.getValue("methodName").jsonPrimitive.content)
        assertEquals("fixture.dex", method.getValue("sourcePath").jsonPrimitive.content)

        val usingField = parsed.getValue("usingFields").jsonArray.single().jsonObject
        assertEquals("Read", usingField.getValue("usingType").jsonPrimitive.content)
        val field = usingField.getValue("field").jsonObject
        assertEquals("fixture.samples.SampleSearchTarget", field.getValue("className").jsonPrimitive.content)
        assertEquals("mutableNeedle", field.getValue("fieldName").jsonPrimitive.content)

        val callers = parsed.getValue("callers").jsonArray
        assertEquals(1, callers.size)
        assertEquals(
            "callReadMutableNeedle",
            callers.single().jsonObject.getValue("methodName").jsonPrimitive.content,
        )

        assertEquals(0, parsed.getValue("invokes").jsonArray.size)
    }

    @Test
    fun inspectMethodIncludeOmitsUnrequestedSections() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SampleSearchTarget;->readMutableNeedle()Ljava/lang/String;",
                "--include",
                "using-fields,callers",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        assertTrue("usingFields" in parsed)
        assertTrue("callers" in parsed)
        assertTrue("invokes" !in parsed)
    }

    @Test
    fun inspectMethodCanReturnUsingStrings() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--include",
                "strings",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        val strings = parsed.getValue("strings").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("dexclub-needle-string"), strings)
        assertTrue("usingFields" !in parsed)
        assertTrue("callers" !in parsed)
        assertTrue("invokes" !in parsed)
    }

    @Test
    fun inspectMethodCanReturnAnnotations() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.dexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.dexFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SampleSearchTarget;->exposeNeedle()Ljava/lang/String;",
                "--include",
                "annotations",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        val annotations = parsed.getValue("annotations").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("""@fixture.samples.Marker(value = "expose-needle")"""), annotations)
        assertTrue("usingFields" !in parsed)
        assertTrue("callers" !in parsed)
        assertTrue("invokes" !in parsed)
        assertTrue("strings" !in parsed)
    }

    @Test
    fun inspectMethodReturnsWorkspaceErrorWhenDescriptorIsAmbiguous() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.ambiguousWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.ambiguousApkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SampleSearchTarget;->readMutableNeedle()Ljava/lang/String;",
            ),
        )

        assertEquals(2, output.exitCode)
        assertTrue(output.stderr.contains("requires a unique descriptor within the workspace"), output.stderr)
    }

    @Test
    fun inspectMethodFindsCrossDexCallersAndResolvesRealSourceEntries() {
        val fixture = CliDexFixture.generated()
        val app = CliApp(
            services = createDefaultServices(),
            cwdProvider = { fixture.crossDexWorkspaceDir.absolutePath },
        )

        val initOut = runCli(app, listOf("init", fixture.crossDexApkFile.absolutePath))
        assertEquals(0, initOut.exitCode)

        val output = runCli(
            app,
            listOf(
                "inspect-method",
                "--descriptor",
                "Lfixture/samples/SplitTarget;->readFromHelper()Ljava/lang/String;",
                "--json",
            ),
        )

        assertEquals(0, output.exitCode, output.stderr)
        val parsed = Json.parseToJsonElement(output.stdout).jsonObject
        val method = parsed.getValue("method").jsonObject
        assertEquals("classes2.dex", method.getValue("sourceEntry").jsonPrimitive.content)

        val callers = parsed.getValue("callers").jsonArray
        assertEquals(1, callers.size)
        val caller = callers.single().jsonObject
        assertEquals("invokeTarget", caller.getValue("methodName").jsonPrimitive.content)
        assertEquals("classes.dex", caller.getValue("sourceEntry").jsonPrimitive.content)

        val usingFields = parsed.getValue("usingFields").jsonArray
        val sharedField = usingFields.first { element ->
            element.jsonObject.getValue("field").jsonObject.getValue("fieldName").jsonPrimitive.content == "sharedField"
        }.jsonObject.getValue("field").jsonObject
        assertEquals("classes.dex", sharedField.getValue("sourceEntry").jsonPrimitive.content)

        val helperInvoke = parsed.getValue("invokes").jsonArray.first { element ->
            val methodObject = element.jsonObject
            methodObject.getValue("className").jsonPrimitive.content == "fixture.samples.SplitHelper" &&
                methodObject.getValue("methodName").jsonPrimitive.content == "helper"
        }.jsonObject
        assertEquals("classes.dex", helperInvoke.getValue("sourceEntry").jsonPrimitive.content)
    }
}

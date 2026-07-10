import java.util.concurrent.TimeUnit
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.github.dexclub.mcp.MainKt"
    applicationDefaultJvmArgs = listOf(
        "-XX:ErrorFile=hs_err_pid%p.log",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=.",
    )
}

fun resolveMcpVersion(): String {
    val configured = project.version.toString()
    if (configured.isNotBlank() && configured != "unspecified") {
        return configured
    }

    return try {
        val process = ProcessBuilder("git", "describe", "--tags", "--always", "--dirty")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        process.waitFor(10, TimeUnit.SECONDS)
        process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            .trim()
            .ifBlank { "dev" }
    } catch (_: Exception) {
        "dev"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.ktor.bom))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mcp.kotlin.sdk.server)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.serialization.kotlinx.json)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(kotlin("test"))
    testImplementation(project(":dexkit"))
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}

val mcpTestSourceSet = the<SourceSetContainer>()["test"]

fun Test.configureMcpTestRuntime() {
    useJUnitPlatform()
}

fun registerMcpTestTask(
    name: String,
    description: String,
    vararg classNames: String,
) = tasks.register<Test>(name) {
    group = "verification"
    this.description = description
    testClassesDirs = mcpTestSourceSet.output.classesDirs
    classpath = mcpTestSourceSet.runtimeClasspath
    configureMcpTestRuntime()
    filter {
        classNames.forEach(::includeTestsMatching)
    }
}

tasks.test {
    configureMcpTestRuntime()
}

val testSession by registerMcpTestTask(
    name = "testSession",
    description = "运行 MCP session 生命周期与会话存储测试",
    "io.github.dexclub.mcp.McpSessionToolsTest",
)

val testDex by registerMcpTestTask(
    name = "testDex",
    description = "运行 MCP dex 查询、inspect 与导出测试",
    "io.github.dexclub.mcp.McpDexToolsTest",
)

val testResource by registerMcpTestTask(
    name = "testResource",
    description = "运行 MCP manifest、resource 与 XML 测试",
    "io.github.dexclub.mcp.McpResourceToolsTest",
)

val testModels by registerMcpTestTask(
    name = "testModels",
    description = "运行 MCP 结果映射、字段投影与错误渲染测试",
    "io.github.dexclub.mcp.McpModelsTest",
)

val testSmoke by registerMcpTestTask(
    name = "testSmoke",
    description = "运行 MCP HTTP smoke 测试",
    "io.github.dexclub.mcp.McpHttpSmokeTest",
)

tasks.register("testFast") {
    group = "verification"
    description = "运行 MCP 单元测试分组，不含 HTTP smoke"
    dependsOn(testSession, testDex, testResource, testModels)
}

tasks.register("testStructured") {
    group = "verification"
    description = "按职责串行运行拆分后的全部 MCP 测试任务"
    dependsOn(testSession, testDex, testResource, testModels, testSmoke)
}

val generateMcpVersionResource = tasks.register("generateMcpVersionResource") {
    val outputDir = layout.buildDirectory.dir("generated/resources/mcpBuildInfo")
    outputs.dir(outputDir)
    doLast {
        val outputFile = outputDir.get().file("dexclub-mcp-version.txt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(resolveMcpVersion(), Charsets.UTF_8)
    }
}

tasks.processResources {
    dependsOn(generateMcpVersionResource)
    from(generateMcpVersionResource)
}

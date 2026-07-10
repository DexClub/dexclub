plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import java.util.concurrent.TimeUnit

fun resolveCoreVersion(): String {
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

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":dexkit"))
            implementation(libs.kotlinx.serialization.json)
        }

        jvmMain.dependencies {
            implementation(libs.smali.dexlib2)
            implementation(libs.smali.baksmali)
            implementation(libs.arsclib)
            implementation(libs.jadx.core)
            implementation(libs.jadx.dex.input)
            implementation(libs.jadx.kotlin.metadata)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

val jvmTestSourceSet = the<SourceSetContainer>()["jvmTest"]

fun Test.configureCoreJvmTestRuntime() {
    dependsOn(gradle.includedBuild("DexKit").task(":dexkit:copyLibrary"))
}

fun registerCoreJvmTestTask(
    name: String,
    description: String,
    vararg classNames: String,
) = tasks.register<Test>(name) {
    group = "verification"
    this.description = description
    testClassesDirs = jvmTestSourceSet.output.classesDirs
    classpath = jvmTestSourceSet.runtimeClasspath
    configureCoreJvmTestRuntime()
    filter {
        classNames.forEach(::includeTestsMatching)
    }
}

val generateCoreVersionResource = tasks.register("generateCoreVersionResource") {
    val outputDir = layout.buildDirectory.dir("generated/resources/coreBuildInfo")
    outputs.dir(outputDir)
    doLast {
        val outputFile = outputDir.get().file("dexclub-core-version.txt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(resolveCoreVersion(), Charsets.UTF_8)
    }
}

tasks.named("processJvmMainResources") {
    dependsOn(generateCoreVersionResource)
    (this as org.gradle.language.jvm.tasks.ProcessResources).from(generateCoreVersionResource)
}

tasks.named<Test>("jvmTest") {
    configureCoreJvmTestRuntime()
}

val testDex by registerCoreJvmTestTask(
    name = "testDex",
    description = "运行 core dex 分析、查询与导出测试",
    "io.github.dexclub.core.impl.dex.DefaultDexAnalysisSearchTest",
    "io.github.dexclub.core.impl.dex.DefaultDexAnalysisInspectTest",
    "io.github.dexclub.core.impl.dex.DefaultDexAnalysisExportTest",
    "io.github.dexclub.core.impl.dex.ScopedContextCacheTest",
)

val testResourceManifest by registerCoreJvmTestTask(
    name = "testResourceManifest",
    description = "运行 core manifest 解码与结构化解析测试",
    "io.github.dexclub.core.impl.resource.ResourceManifestServiceTest",
)

val testResourceTable by registerCoreJvmTestTask(
    name = "testResourceTable",
    description = "运行 core resource table 解码测试",
    "io.github.dexclub.core.impl.resource.ResourceTableServiceTest",
)

val testResourceXml by registerCoreJvmTestTask(
    name = "testResourceXml",
    description = "运行 core XML 解码测试",
    "io.github.dexclub.core.impl.resource.ResourceXmlServiceTest",
)

val testResourceEntry by registerCoreJvmTestTask(
    name = "testResourceEntry",
    description = "运行 core resource entry 索引与列举测试",
    "io.github.dexclub.core.impl.resource.ResourceEntryServiceTest",
)

val testResourceValue by registerCoreJvmTestTask(
    name = "testResourceValue",
    description = "运行 core resource value 解析与搜索测试",
    "io.github.dexclub.core.impl.resource.ResourceValueServiceTest",
)

val testWorkspace by registerCoreJvmTestTask(
    name = "testWorkspace",
    description = "运行 core workspace 初始化、默认服务装配与运行时解析测试",
    "io.github.dexclub.core.api.shared.CreateDefaultServicesTest",
    "io.github.dexclub.core.impl.workspace.DefaultWorkspaceServiceTest",
    "io.github.dexclub.core.impl.workspace.runtime.WorkspaceRuntimeResolverTest",
)

tasks.register("testResource") {
    group = "verification"
    description = "按职责运行全部 core resource 测试"
    dependsOn(
        testResourceManifest,
        testResourceTable,
        testResourceXml,
        testResourceEntry,
        testResourceValue,
    )
}

tasks.register("testFast") {
    group = "verification"
    description = "运行 core dex、resource 与 workspace 分组测试入口"
    dependsOn(testDex, tasks.named("testResource"), testWorkspace)
}

tasks.register("testStructured") {
    group = "verification"
    description = "按职责串行运行拆分后的全部 core JVM 测试入口"
    dependsOn(testDex, tasks.named("testResource"), testWorkspace)
}

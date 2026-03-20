import java.util.Properties
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.ktreesitter.plugin)
}

val grammarDir = project.layout.projectDirectory.dir("../tree-sitter-java").asFile
val generatedDir = layout.buildDirectory.dir("generated")
val generatedSrcDir = generatedDir.map { it.dir("src") }
val jvmCmakeBuildDir = layout.buildDirectory.dir(".cmake/jvm")
val jvmInstallPrefix = generatedSrcDir.map { it.dir("jvmMain/resources").asFile.path }
val androidCmakeBuildRootDir = layout.buildDirectory.dir(".cmake/android")

val osName = System.getProperty("os.name").lowercase()
val isWindows = osName.contains("windows")
val isLinux = osName.contains("linux")
val isMacOs = osName.contains("mac")
val libPlatform = when {
    isWindows -> "windows"
    isLinux -> "linux"
    isMacOs -> "macos"
    else -> throw GradleException("Unsupported operating system: $osName")
}
val arch = System.getProperty("os.arch").lowercase()
val libArch = when (arch) {
    "amd64", "x86_64" -> "x64"
    "aarch64", "arm64" -> "aarch64"
    else -> throw GradleException("Unsupported architecture: $arch")
}
val jvmLibSubDir = "lib/$libPlatform/$libArch"
val jvmLibExtension = when {
    isWindows -> "dll"
    isLinux -> "so"
    isMacOs -> "dylib"
    else -> throw GradleException("Unsupported operating system: $osName")
}
val jniLibBaseName = "ktreesitter-java"
val cmakeProducedLibFileName = "lib$jniLibBaseName.$jvmLibExtension"
val jvmLibFileName = when {
    isWindows -> "$jniLibBaseName.$jvmLibExtension"
    else -> cmakeProducedLibFileName
}
val jvmInstalledLib = generatedSrcDir.map {
    it.file("jvmMain/resources/$jvmLibSubDir/$jvmLibFileName")
}
val cmakeInstalledLib = generatedSrcDir.map {
    it.file("jvmMain/resources/$jvmLibSubDir/$cmakeProducedLibFileName")
}
val androidAbis = listOf(
    "armeabi-v7a",
    "arm64-v8a",
    "x86",
    "x86_64",
)
val androidJniLibsRootDir = layout.buildDirectory.dir(
    "intermediates/merged_native_libs/androidMain/mergeAndroidMainNativeLibs/out",
)
val androidSoFileName = "lib$jniLibBaseName.so"
val androidMinSdk = 24

fun File.asCmakePath(): String = absolutePath.replace('\\', '/')

fun String.toTaskSuffix(): String {
    return split('-', '_').joinToString(separator = "") { part ->
        part.replaceFirstChar { char -> char.uppercaseChar() }
    }
}

fun resolveAndroidNdkDir(): File {
    val ndkFromEnv = listOf("ANDROID_NDK_HOME", "ANDROID_NDK_ROOT")
        .asSequence()
        .mapNotNull { System.getenv(it) }
        .map(::File)
        .firstOrNull { it.isDirectory }
    if (ndkFromEnv != null) {
        return ndkFromEnv
    }

    val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties").asFile
    val localProperties = Properties()
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(localProperties::load)
    }

    val sdkPath = localProperties.getProperty("sdk.dir")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: throw GradleException(
            "Android SDK 未找到。请配置 local.properties 的 sdk.dir 或 ANDROID_SDK_ROOT/ANDROID_HOME。",
        )

    val ndkRoot = File(sdkPath, "ndk")
    val ndkDir = ndkRoot
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.filter { it.isDirectory }
        ?.maxByOrNull { it.name }
        ?: throw GradleException(
            "Android NDK 未找到。请安装 NDK 或配置 ANDROID_NDK_HOME/ANDROID_NDK_ROOT。",
        )
    return ndkDir
}

val androidNdkDir = resolveAndroidNdkDir()
val androidToolchainFile = androidNdkDir.resolve("build/cmake/android.toolchain.cmake")
if (!androidToolchainFile.isFile) {
    throw GradleException("未找到 Android toolchain 文件: ${androidToolchainFile.absolutePath}")
}

grammar {
    baseDir = grammarDir
    grammarName = "java"
    className = "TreeSitterJava"
    packageName = "io.github.dexclub.treesitter.java"
    files = arrayOf(
        grammarDir.resolve("src/parser.c"),
    )
}

kotlin {
    jvm()
    android {
        namespace = "io.github.dexclub.treesitter.java"
        compileSdk = 36
        minSdk = 24
    }

    sourceSets {
        configureEach {
            kotlin.srcDir(generatedSrcDir.map { it.dir(name).dir("kotlin") })
        }

        jvmMain {
            resources.srcDir(generatedSrcDir.map { it.dir(name).dir("resources") })
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(tasks.named("generateGrammarFiles"))
}

val normalizeCmakePaths by tasks.registering {
    dependsOn(tasks.named("generateGrammarFiles"))
    val cmakeFile = generatedDir.map { it.file("CMakeLists.txt").asFile }
    inputs.file(cmakeFile)
    outputs.file(cmakeFile)

    doLast {
        val file = cmakeFile.get()
        val content = file.readText()
        val normalized = content.replace('\\', '/')
        if (content != normalized) {
            file.writeText(normalized)
        }
    }
}

tasks.matching { it.name == "prepareAndroidMainArtProfile" }.configureEach {
    dependsOn(tasks.named("generateGrammarFiles"))
}

val configureJvmJni by tasks.registering(Exec::class) {
    dependsOn(normalizeCmakePaths)
    inputs.file(generatedDir.map { it.file("CMakeLists.txt") })
    outputs.file(jvmCmakeBuildDir.map { it.file("CMakeCache.txt") })
    val args = mutableListOf(
        "cmake",
        "-S",
        generatedDir.get().asFile.path,
        "-B",
        jvmCmakeBuildDir.get().asFile.path,
        "-DCMAKE_INSTALL_PREFIX=${jvmInstallPrefix.get()}",
        "-DCMAKE_INSTALL_BINDIR=$jvmLibSubDir",
        "-DCMAKE_INSTALL_LIBDIR=$jvmLibSubDir",
    )
    commandLine(args)
}

val buildJvmJni by tasks.registering(Exec::class) {
    dependsOn(configureJvmJni)
    inputs.files(grammar.files)
    outputs.file(jvmCmakeBuildDir.map { it.file(cmakeProducedLibFileName) })
    commandLine(
        "cmake",
        "--build",
        jvmCmakeBuildDir.get().asFile.path,
        "--config",
        "Release",
    )
}

val installJvmJni by tasks.registering(Exec::class) {
    dependsOn(buildJvmJni)
    inputs.file(jvmCmakeBuildDir.map { it.file(cmakeProducedLibFileName) })
    outputs.file(cmakeInstalledLib)
    commandLine(
        "cmake",
        "--install",
        jvmCmakeBuildDir.get().asFile.path,
        "--config",
        "Release",
    )
}

if (isWindows) {
    val normalizeWindowsLibName by tasks.registering(Copy::class) {
        dependsOn(installJvmJni)
        from(cmakeInstalledLib)
        into(generatedSrcDir.map { it.dir("jvmMain/resources/$jvmLibSubDir") })
        rename(cmakeProducedLibFileName, jvmLibFileName)
        outputs.file(jvmInstalledLib)
    }

    tasks.named("jvmProcessResources") {
        dependsOn(normalizeWindowsLibName)
    }
} else {
    tasks.named("jvmProcessResources") {
        dependsOn(installJvmJni)
    }
}

// Kotlin AMPL target does not expose externalNativeBuild/ndk DSL yet,
// so Android JNI is built via explicit CMake configure/build tasks.
val configureAndroidJniTasks = androidAbis.associateWith { abi ->
    val suffix = abi.toTaskSuffix()
    tasks.register<Exec>("configureAndroidJni$suffix") {
        dependsOn(normalizeCmakePaths)
        inputs.file(generatedDir.map { it.file("CMakeLists.txt") })
        outputs.file(androidCmakeBuildRootDir.map { it.dir(abi).file("CMakeCache.txt") })
        val buildDir = androidCmakeBuildRootDir.get().dir(abi).asFile
        val jniOutDir = androidJniLibsRootDir.get().dir("lib/$abi").asFile
        commandLine(
            "cmake",
            "-S",
            generatedDir.get().asFile.asCmakePath(),
            "-B",
            buildDir.asCmakePath(),
            "-DCMAKE_TOOLCHAIN_FILE=${androidToolchainFile.asCmakePath()}",
            "-DANDROID_ABI=$abi",
            "-DANDROID_PLATFORM=android-$androidMinSdk",
            "-DANDROID_STL=c++_shared",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${jniOutDir.asCmakePath()}",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${jniOutDir.asCmakePath()}",
        )
    }
}

val buildAndroidJniTasks = androidAbis.associateWith { abi ->
    val suffix = abi.toTaskSuffix()
    tasks.register<Exec>("buildAndroidJni$suffix") {
        dependsOn(configureAndroidJniTasks.getValue(abi))
        inputs.files(grammar.files)
        outputs.file(androidJniLibsRootDir.map { it.dir("lib/$abi").file(androidSoFileName) })
        commandLine(
            "cmake",
            "--build",
            androidCmakeBuildRootDir.get().dir(abi).asFile.asCmakePath(),
            "--config",
            "Release",
        )
    }
}

val buildAndroidJni by tasks.registering {
    dependsOn(buildAndroidJniTasks.values)
}

tasks.matching { it.name == "mergeAndroidMainJniLibFolders" }.configureEach {
    dependsOn(buildAndroidJni)
}

tasks.matching { it.name == "copyAndroidMainJniLibsProjectOnly" }.configureEach {
    dependsOn(buildAndroidJni)
}

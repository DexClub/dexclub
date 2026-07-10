package io.github.dexclub.cli

import java.io.File

internal fun compileBinaryManifestApk(outputApk: File, manifestText: String) {
    val root = outputApk.parentFile
    val manifestFile = File(root, "AndroidManifest.xml").apply {
        writeText(manifestText, Charsets.UTF_8)
    }
    runExternalCommand(
        command = listOf(
            resolveAapt2Command(),
            "link",
            "-o",
            outputApk.absolutePath,
            "--manifest",
            manifestFile.absolutePath,
            "-I",
            resolveAndroidJar().absolutePath,
        ),
        workingDirectory = root,
    )
}

internal fun compileResourceApk(
    outputApk: File,
    manifestText: String,
    resourceXml: String,
    layoutXml: String? = null,
) {
    val root = outputApk.parentFile
    val manifestFile = File(root, "AndroidManifest.xml").apply {
        writeText(manifestText, Charsets.UTF_8)
    }
    val valuesDir = File(root, "res/values").apply { mkdirs() }
    File(valuesDir, "strings.xml").writeText(resourceXml, Charsets.UTF_8)
    if (layoutXml != null) {
        val layoutDir = File(root, "res/layout").apply { mkdirs() }
        File(layoutDir, "activity_main.xml").writeText(layoutXml, Charsets.UTF_8)
    }
    val compiledDir = File(root, "compiled-res").apply { mkdirs() }
    runExternalCommand(
        command = listOf(
            resolveAapt2Command(),
            "compile",
            "--dir",
            valuesDir.parentFile.absolutePath,
            "-o",
            compiledDir.absolutePath,
        ),
        workingDirectory = root,
    )
    val compiledRes = compiledDir.listFiles()
        ?.filter(File::isFile)
        ?.sortedBy(File::getName)
        ?.takeIf { it.isNotEmpty() }
        ?: error("未生成编译资源产物")
    runExternalCommand(
        command = buildList {
            add(resolveAapt2Command())
            add("link")
            add("-o")
            add(outputApk.absolutePath)
            add("--manifest")
            add(manifestFile.absolutePath)
            add("-I")
            add(resolveAndroidJar().absolutePath)
            add("--auto-add-overlay")
            compiledRes.forEach { compiled ->
                add("-R")
                add(compiled.absolutePath)
            }
        },
        workingDirectory = root,
    )
}

private fun resolveAapt2Command(): String {
    val sdkRoot = System.getenv("ANDROID_HOME")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?: error("未找到 ANDROID_HOME，无法定位 aapt2")
    return sdkRoot.resolve("build-tools").listFiles()
        ?.sortedByDescending(File::getName)
        ?.asSequence()
        ?.map { it.resolve("aapt2.exe") }
        ?.firstOrNull { it.isFile }
        ?.absolutePath
        ?: error("未找到可用的 aapt2.exe")
}

private fun resolveAndroidJar(): File {
    val sdkRoot = System.getenv("ANDROID_HOME")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?: error("未找到 ANDROID_HOME，无法定位 android.jar")
    return sdkRoot.resolve("platforms").walkTopDown()
        .filter { it.isFile && it.name == "android.jar" }
        .sortedByDescending(File::getAbsolutePath)
        .firstOrNull()
        ?: error("未找到可用的 android.jar")
}

private fun runExternalCommand(command: List<String>, workingDirectory: File) {
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

package io.github.dexclub.core.impl.resource

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.writeBytes

internal fun createZip(vararg entries: Pair<String, ByteArray>): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}

internal fun compileBinaryManifestApk(outputApk: File, manifestText: String) {
    val root = outputApk.parentFile
    val manifestFile = File(root, "AndroidManifest.xml").apply {
        writeText(manifestText, Charsets.UTF_8)
    }
    runCommand(
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
    runCommand(
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
        ?: error("No compiled resource artifacts were produced")
    runCommand(
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

internal fun resolveAapt2Command(): String {
    val sdkRoot = resolveAndroidSdkRoot()
    return sdkRoot.resolve("build-tools").listFiles()
        ?.sortedByDescending(File::getName)
        ?.asSequence()
        ?.flatMap { buildToolsDir ->
            sequenceOf(
                buildToolsDir.resolve("aapt2"),
                buildToolsDir.resolve("aapt2.exe"),
            )
        }
        ?.firstOrNull { it.isFile }
        ?.absolutePath
        ?: error("No usable aapt2 executable was found under ${sdkRoot.resolve("build-tools").absolutePath}")
}

internal fun resolveAndroidJar(): File {
    val sdkRoot = resolveAndroidSdkRoot()
    return sdkRoot.resolve("platforms").walkTopDown()
        .filter { it.isFile && it.name == "android.jar" }
        .sortedByDescending(File::getAbsolutePath)
        .firstOrNull()
        ?: error("No usable android.jar was found under ${sdkRoot.resolve("platforms").absolutePath}")
}

internal fun resolveAndroidSdkRoot(): File = sequenceOf("ANDROID_SDK_ROOT", "ANDROID_HOME")
    .mapNotNull { name ->
        System.getenv(name)
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
    }
    .firstOrNull()
    ?: error("Neither ANDROID_SDK_ROOT nor ANDROID_HOME was found; cannot locate Android SDK")

internal fun runCommand(command: List<String>, workingDirectory: File) {
    val process = ProcessBuilder(command)
        .directory(workingDirectory)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    check(process.waitFor() == 0) {
        buildString {
            appendLine("Command failed: ${command.joinToString(" ")}")
            append(output)
        }
    }
}

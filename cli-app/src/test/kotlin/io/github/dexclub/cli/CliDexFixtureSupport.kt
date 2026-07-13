package io.github.dexclub.cli

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

internal fun isDexFile(file: File): Boolean {
    val header = ByteArray(8)
    file.inputStream().use { input ->
        val read = input.read(header)
        if (read < header.size) return false
    }
    return header.decodeToString() == "dex\n035\u0000"
}

internal class CliDexFixture(
    val dexWorkspaceDir: File,
    val dexFile: File,
    val ambiguousWorkspaceDir: File,
    val ambiguousApkFile: File,
    val crossDexWorkspaceDir: File,
    val crossDexApkFile: File,
) {
    companion object {
        fun generated(): CliDexFixture {
            val root = createTempDirectory("dexclub-dex-fixture").toFile()
            val sampleClasses = compileJava(
                root = root,
                fileName = "SampleSearchTarget.java",
                source = """
                    package fixture.samples;
                    import java.lang.annotation.Retention;
                    import java.lang.annotation.RetentionPolicy;

                    @Retention(RetentionPolicy.RUNTIME)
                    @interface Marker {
                        String value();
                    }

                    public class SampleSearchTarget {
                        public static final String NEEDLE = "dexclub-needle-string";
                        public String mutableNeedle = NEEDLE;
                        @Marker("expose-needle")
                        public String exposeNeedle() {
                            return NEEDLE;
                        }
                        public String callExposeNeedle() {
                            return exposeNeedle();
                        }
                        public String readMutableNeedle() {
                            return mutableNeedle;
                        }
                        public String callReadMutableNeedle() {
                            return readMutableNeedle();
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

            val splitHelperClasses = compileJava(
                root = root,
                fileName = "SplitHelper.java",
                source = """
                    package fixture.samples;
                    public class SplitHelper {
                        public String sharedField = "split-field";
                        public String helper() {
                            return "split-helper";
                        }
                    }
                """.trimIndent(),
            )
            val splitTargetClasses = compileJava(
                root = root,
                fileName = "SplitTarget.java",
                classpath = listOf(splitHelperClasses),
                source = """
                    package fixture.samples;
                    public class SplitTarget {
                        private final SplitHelper helper = new SplitHelper();
                        public String readFromHelper() {
                            return helper.sharedField + helper.helper();
                        }
                    }
                """.trimIndent(),
            )
            val splitCallerClasses = compileJava(
                root = root,
                fileName = "SplitCaller.java",
                classpath = listOf(splitHelperClasses, splitTargetClasses),
                source = """
                    package fixture.samples;
                    public class SplitCaller {
                        public String invokeTarget() {
                            return new SplitTarget().readFromHelper();
                        }
                    }
                """.trimIndent(),
            )
            val splitClassesDex = compileDex(root, "split-classes", splitHelperClasses, splitCallerClasses)
            val splitClasses2Dex = compileDex(root, "split-classes2", splitTargetClasses)
            val crossDexWorkspaceDir = File(root, "cross-dex-input").also(File::mkdirs)
            val crossDexApkFile = File(crossDexWorkspaceDir, "fixture.apk")
            createPseudoApk(
                outputApk = crossDexApkFile,
                "classes.dex" to splitClassesDex,
                "classes2.dex" to splitClasses2Dex,
            )

            return CliDexFixture(
                dexWorkspaceDir = dexWorkspaceDir,
                dexFile = dexFile,
                ambiguousWorkspaceDir = ambiguousWorkspaceDir,
                ambiguousApkFile = ambiguousApkFile,
                crossDexWorkspaceDir = crossDexWorkspaceDir,
                crossDexApkFile = crossDexApkFile,
            )
        }

        private fun compileJava(
            root: File,
            fileName: String,
            source: String,
            classpath: List<File> = emptyList(),
        ): File {
            val sourceDir = File(root, "src-$fileName/fixture/samples").also(File::mkdirs)
            val sourceFile = File(sourceDir, fileName).apply {
                writeText(source, Charsets.UTF_8)
            }
            val classesDir = File(root, "classes-$fileName").also(File::mkdirs)
            runCommand(
                command = buildList {
                    add("javac")
                    add("--release")
                    add("8")
                    if (classpath.isNotEmpty()) {
                        add("-classpath")
                        add(classpath.joinToString(File.pathSeparator) { it.absolutePath })
                    }
                    add("-d")
                    add(classesDir.absolutePath)
                    add(sourceFile.absolutePath)
                },
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

        private fun createPseudoApk(outputApk: File, vararg entries: Pair<String, File>) {
            ZipOutputStream(outputApk.outputStream().buffered()).use { zip ->
                entries.forEach { (name, sourceDex) ->
                    zip.putNextEntry(ZipEntry(name))
                    sourceDex.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
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
            } ?: error("No usable d8 command was found")
        }

        private fun runCommand(command: List<String>, workingDirectory: File) {
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
    }
}

package io.github.dexclub.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun cliAppTestDir(vararg segments: String): Path {
    val path = segments.fold(
        Paths.get("build", "tmp", "test-fixtures").toAbsolutePath().normalize(),
    ) { current, segment ->
        current.resolve(segment)
    }
    Files.createDirectories(path)
    return path
}

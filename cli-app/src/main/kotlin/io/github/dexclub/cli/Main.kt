package io.github.dexclub.cli

import io.github.dexclub.core.app.createAppRuntime
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    DexKitCliBootstrap.configureNativeLibraryDir()
    val runtime = createAppRuntime()
    val exitCode = try {
        CliApp(runtime = runtime).run(
            argv = args.toList(),
            stdout = System.out,
            stderr = System.err,
        )
    } finally {
        runtime.close()
    }
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

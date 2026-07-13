package io.github.dexclub.cli

internal fun runCli(app: CliApp, argv: List<String>): CapturedOutput {
    val stdout = StringBuilder()
    val stderr = StringBuilder()
    val exitCode = app.run(argv, stdout, stderr)
    return CapturedOutput(
        stdout = stdout.toString(),
        stderr = stderr.toString(),
        exitCode = exitCode,
    )
}

internal data class CapturedOutput(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)

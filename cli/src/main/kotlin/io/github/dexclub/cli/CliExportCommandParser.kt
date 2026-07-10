package io.github.dexclub.cli

internal class CliExportCommandParser {
    fun parseExportClassSmali(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-class-smali")
        }
        val parsed = parseExportClassSmaliCommand(tokens, CliUsages.exportClassSmali)
        return CliRequest.ExportClassSmali(
            workdir = parsed.workdir,
            className = parsed.className,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
            autoUnicodeDecode = parsed.autoUnicodeDecode,
        )
    }

    fun parseExportClassDex(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-class-dex")
        }
        val parsed = parseExportClassDexCommand(tokens, CliUsages.exportClassDex)
        return CliRequest.ExportClassDex(
            workdir = parsed.workdir,
            className = parsed.className,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
        )
    }

    fun parseExportClassJava(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-class-java")
        }
        val parsed = parseExportClassJavaCommand(tokens, CliUsages.exportClassJava)
        return CliRequest.ExportClassJava(
            workdir = parsed.workdir,
            className = parsed.className,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
        )
    }

    fun parseExportMethodSmali(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-method-smali")
        }
        val parsed = parseExportMethodSmaliCommand(tokens, CliUsages.exportMethodSmali)
        return CliRequest.ExportMethodSmali(
            workdir = parsed.workdir,
            methodSignature = parsed.methodSignature,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
            autoUnicodeDecode = parsed.autoUnicodeDecode,
            mode = parsed.mode,
        )
    }

    fun parseExportMethodDex(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-method-dex")
        }
        val parsed = parseExportMethodDexCommand(tokens, CliUsages.exportMethodDex)
        return CliRequest.ExportMethodDex(
            workdir = parsed.workdir,
            methodSignature = parsed.methodSignature,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
        )
    }

    fun parseExportMethodJava(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("export-method-java")
        }
        val parsed = parseExportMethodJavaCommand(tokens, CliUsages.exportMethodJava)
        return CliRequest.ExportMethodJava(
            workdir = parsed.workdir,
            methodSignature = parsed.methodSignature,
            sourcePath = parsed.sourcePath,
            sourceEntry = parsed.sourceEntry,
            output = parsed.output,
        )
    }

    private fun parseExportClassSmaliCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportClassSmaliCommand {
        val positionals = mutableListOf<String>()
        var className: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var autoUnicodeDecode = true
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--class" -> {
                    className = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--auto-unicode-decode" -> {
                    autoUnicodeDecode = CliParserSupport.parseBooleanOption(tokens, token, index, usage)
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportClassSmaliCommand(
            workdir = positionals.singleOrNull(),
            className = className ?: throw CliUsageError(
                message = "missing required option: --class",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
            autoUnicodeDecode = autoUnicodeDecode,
        )
    }

    private fun parseExportClassDexCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportClassDexCommand {
        val positionals = mutableListOf<String>()
        var className: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--class" -> {
                    className = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportClassDexCommand(
            workdir = positionals.singleOrNull(),
            className = className ?: throw CliUsageError(
                message = "missing required option: --class",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
        )
    }

    private fun parseExportMethodSmaliCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportMethodSmaliCommand {
        val positionals = mutableListOf<String>()
        var methodSignature: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var autoUnicodeDecode = true
        var mode = "snippet"
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--method" -> {
                    methodSignature = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--auto-unicode-decode" -> {
                    autoUnicodeDecode = CliParserSupport.parseBooleanOption(tokens, token, index, usage)
                    index += 2
                }

                "--mode" -> {
                    mode = CliParserSupport.requireOptionValue(tokens, token, index, usage).also { value ->
                        if (value != "snippet" && value != "class") {
                            throw CliUsageError(
                                message = "invalid value for --mode: expected snippet or class",
                                usage = usage,
                            )
                        }
                    }
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportMethodSmaliCommand(
            workdir = positionals.singleOrNull(),
            methodSignature = methodSignature ?: throw CliUsageError(
                message = "missing required option: --method",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
            autoUnicodeDecode = autoUnicodeDecode,
            mode = mode,
        )
    }

    private fun parseExportMethodDexCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportMethodDexCommand {
        val positionals = mutableListOf<String>()
        var methodSignature: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--method" -> {
                    methodSignature = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportMethodDexCommand(
            workdir = positionals.singleOrNull(),
            methodSignature = methodSignature ?: throw CliUsageError(
                message = "missing required option: --method",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
        )
    }

    private fun parseExportMethodJavaCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportMethodJavaCommand {
        val positionals = mutableListOf<String>()
        var methodSignature: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--method" -> {
                    methodSignature = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportMethodJavaCommand(
            workdir = positionals.singleOrNull(),
            methodSignature = methodSignature ?: throw CliUsageError(
                message = "missing required option: --method",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
        )
    }

    private fun parseExportClassJavaCommand(
        tokens: List<String>,
        usage: String,
    ): ParsedExportClassJavaCommand {
        val positionals = mutableListOf<String>()
        var className: String? = null
        var sourcePath: String? = null
        var sourceEntry: String? = null
        var output: String? = null
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (!token.startsWith("--")) {
                if (seenOption) {
                    throw CliUsageError(
                        message = "positional arguments must appear before options",
                        usage = usage,
                    )
                }
                positionals += token
                index += 1
                continue
            }

            seenOption = true
            if (!seenFlags.add(token)) {
                throw CliUsageError(
                    message = "option may only be specified once: $token",
                    usage = usage,
                )
            }

            when (token) {
                "--class" -> {
                    className = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-path" -> {
                    sourcePath = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--source-entry" -> {
                    sourceEntry = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--output" -> {
                    output = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                else -> throw CliUsageError(
                    message = "unknown option: $token",
                    usage = usage,
                )
            }
        }

        if (positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        if (sourceEntry != null && sourcePath == null) {
            throw CliUsageError(
                message = "--source-entry requires --source-path",
                usage = usage,
            )
        }

        return ParsedExportClassJavaCommand(
            workdir = positionals.singleOrNull(),
            className = className ?: throw CliUsageError(
                message = "missing required option: --class",
                usage = usage,
            ),
            sourcePath = sourcePath,
            sourceEntry = sourceEntry,
            output = output ?: throw CliUsageError(
                message = "missing required option: --output",
                usage = usage,
            ),
        )
    }
}

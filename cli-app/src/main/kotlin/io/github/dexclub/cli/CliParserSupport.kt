package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.MethodDetailSection
import io.github.dexclub.core.app.contract.PageWindow

internal object CliParserSupport {
    fun parseQueryCommand(tokens: List<String>, usage: String): ParsedQueryCommand {
        val positionals = mutableListOf<String>()
        var outputFormat = OutputFormat.Text
        var queryJson: String? = null
        var queryFile: String? = null
        var offset = 0
        var limit: Int? = null
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
                "--json" -> {
                    outputFormat = OutputFormat.Json
                    index += 1
                }

                "--query-json" -> {
                    queryJson = requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--query-file" -> {
                    queryFile = requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--offset" -> {
                    val value = requireOptionValue(tokens, token, index, usage)
                    offset = value.toIntOrNull()
                        ?.takeIf { it >= 0 }
                        ?: throw CliUsageError(
                            message = "invalid value for --offset: expected a non-negative integer",
                            usage = usage,
                        )
                    index += 2
                }

                "--limit" -> {
                    val value = requireOptionValue(tokens, token, index, usage)
                    limit = value.toIntOrNull()
                        ?.takeIf { it > 0 }
                        ?: throw CliUsageError(
                            message = "invalid value for --limit: expected a positive integer",
                            usage = usage,
                        )
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
        if (queryJson != null && queryFile != null) {
            throw CliUsageError(
                message = "--query-json and --query-file are mutually exclusive",
                usage = usage,
            )
        }
        val query = when {
            queryJson != null -> QueryInput.Json(queryJson)
            queryFile != null -> QueryInput.File(queryFile)
            else -> throw CliUsageError(
                message = "missing required option: --query-json or --query-file",
                usage = usage,
            )
        }
        return ParsedQueryCommand(
            workdir = positionals.singleOrNull(),
            query = query,
            window = PageWindow(offset = offset, limit = limit),
            outputFormat = outputFormat,
        )
    }

    fun parsePositionalAndFlags(
        tokens: List<String>,
        allowJson: Boolean,
        usage: String,
    ): ParsedCommand {
        val positionals = mutableListOf<String>()
        var outputFormat = OutputFormat.Text
        var seenOption = false
        val seenFlags = mutableSetOf<String>()
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (token.startsWith("--")) {
                seenOption = true
                if (!seenFlags.add(token)) {
                    throw CliUsageError(
                        message = "option may only be specified once: $token",
                        usage = usage,
                    )
                }
                when (token) {
                    "--json" -> {
                        if (!allowJson) {
                            throw CliUsageError(
                                message = "unknown option: $token",
                                usage = usage,
                            )
                        }
                        outputFormat = OutputFormat.Json
                    }

                    else -> throw CliUsageError(
                        message = "unknown option: $token",
                        usage = usage,
                    )
                }
                index += 1
                continue
            }

            if (seenOption) {
                throw CliUsageError(
                    message = "positional arguments must appear before options",
                    usage = usage,
                )
            }
            positionals += token
            index += 1
        }
        return ParsedCommand(
            positionals = positionals,
            outputFormat = outputFormat,
        )
    }

    fun requireOptionValue(
        tokens: List<String>,
        option: String,
        index: Int,
        usage: String,
    ): String {
        if (index + 1 >= tokens.size) {
            throw CliUsageError(
                message = "missing value for option: $option",
                usage = usage,
            )
        }
        val value = tokens[index + 1].trim()
        if (value.isEmpty() || value.startsWith("--")) {
            throw CliUsageError(
                message = "missing value for option: $option",
                usage = usage,
            )
        }
        return value
    }

    fun parseBooleanOption(
        tokens: List<String>,
        option: String,
        index: Int,
        usage: String,
    ): Boolean {
        val value = requireOptionValue(tokens, option, index, usage)
        return when (value) {
            "true" -> true
            "false" -> false
            else -> throw CliUsageError(
                message = "invalid value for $option: expected true or false",
                usage = usage,
            )
        }
    }

    fun parseMethodDetailIncludes(value: String, usage: String): Set<MethodDetailSection> {
        if (value.isBlank()) {
            throw CliUsageError(
                message = "invalid value for --include: expected a comma-separated list",
                usage = usage,
            )
        }
        val sections = value.split(',')
            .map { token ->
                when (val normalized = token.trim()) {
                    "using-fields" -> MethodDetailSection.UsingFields
                    "callers" -> MethodDetailSection.Callers
                    "invokes" -> MethodDetailSection.Invokes
                    "strings" -> MethodDetailSection.Strings
                    "annotations" -> MethodDetailSection.Annotations
                    "" -> throw CliUsageError(
                        message = "invalid value for --include: empty section is not allowed",
                        usage = usage,
                    )
                    else -> throw CliUsageError(
                        message = "invalid value for --include: unsupported section '$normalized'",
                        usage = usage,
                    )
                }
            }
        if (sections.distinct().size != sections.size) {
            throw CliUsageError(
                message = "invalid value for --include: duplicate sections are not allowed",
                usage = usage,
            )
        }
        return sections.toSet()
    }
}

internal data class ParsedCommand(
    val positionals: List<String>,
    val outputFormat: OutputFormat,
)

internal data class ParsedQueryCommand(
    val workdir: String?,
    val query: QueryInput,
    val window: PageWindow,
    val outputFormat: OutputFormat,
)

internal data class ParsedExportClassSmaliCommand(
    val workdir: String?,
    val className: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
    val autoUnicodeDecode: Boolean,
)

internal data class ParsedExportClassDexCommand(
    val workdir: String?,
    val className: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
)

internal data class ParsedExportMethodSmaliCommand(
    val workdir: String?,
    val methodSignature: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
    val autoUnicodeDecode: Boolean,
    val mode: String,
)

internal data class ParsedExportMethodDexCommand(
    val workdir: String?,
    val methodSignature: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
)

internal data class ParsedExportMethodJavaCommand(
    val workdir: String?,
    val methodSignature: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
)

internal data class ParsedExportClassJavaCommand(
    val workdir: String?,
    val className: String,
    val sourcePath: String?,
    val sourceEntry: String?,
    val output: String,
)


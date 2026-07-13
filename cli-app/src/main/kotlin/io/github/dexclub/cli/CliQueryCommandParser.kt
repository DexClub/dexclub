package io.github.dexclub.cli

import io.github.dexclub.core.app.contract.MethodDetailSection

internal class CliQueryCommandParser {
    fun parseFindClass(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-class")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findClass)
        return CliRequest.FindClass(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }

    fun parseFindMethod(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-method")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findMethod)
        return CliRequest.FindMethod(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }

    fun parseInspectMethod(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("inspect-method")
        }
        val usage = CliUsages.inspectMethod
        val positionals = mutableListOf<String>()
        var outputFormat = OutputFormat.Text
        var descriptor: String? = null
        var includes: Set<MethodDetailSection>? = null
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

                "--descriptor" -> {
                    descriptor = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--include" -> {
                    includes = CliParserSupport.parseMethodDetailIncludes(
                        CliParserSupport.requireOptionValue(tokens, token, index, usage),
                        usage,
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

        return CliRequest.InspectMethod(
            workdir = positionals.singleOrNull(),
            descriptor = descriptor ?: throw CliUsageError(
                message = "missing required option: --descriptor",
                usage = usage,
            ),
            includes = includes ?: MethodDetailSection.entries.toSet(),
            outputFormat = outputFormat,
        )
    }

    fun parseFindField(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-field")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findField)
        return CliRequest.FindField(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }

    fun parseFindClassUsingStrings(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-class-using-strings")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findClassUsingStrings)
        return CliRequest.FindClassUsingStrings(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }

    fun parseFindMethodUsingStrings(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-method-using-strings")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findMethodUsingStrings)
        return CliRequest.FindMethodUsingStrings(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }
}


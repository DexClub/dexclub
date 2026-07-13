package io.github.dexclub.cli

internal class CliResourceCommandParser {
    fun parseDecodeXml(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("decode-xml")
        }
        val usage = CliUsages.decodeXml
        val positionals = mutableListOf<String>()
        var outputFormat = OutputFormat.Text
        var path: String? = null
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

                "--path" -> {
                    path = CliParserSupport.requireOptionValue(tokens, token, index, usage)
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

        return CliRequest.DecodeXml(
            workdir = positionals.singleOrNull(),
            path = path ?: throw CliUsageError(
                message = "missing required option: --path",
                usage = usage,
            ),
            outputFormat = outputFormat,
        )
    }

    fun parseGetResValue(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("get-res-value")
        }
        val usage = CliUsages.getResValue
        val positionals = mutableListOf<String>()
        var outputFormat = OutputFormat.Text
        var resourceId: String? = null
        var type: String? = null
        var name: String? = null
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

                "--id" -> {
                    resourceId = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--type" -> {
                    type = CliParserSupport.requireOptionValue(tokens, token, index, usage)
                    index += 2
                }

                "--name" -> {
                    name = CliParserSupport.requireOptionValue(tokens, token, index, usage)
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
        if (resourceId != null && (type != null || name != null)) {
            throw CliUsageError(
                message = "--id and --type/--name are mutually exclusive",
                usage = usage,
            )
        }
        if ((type == null) != (name == null)) {
            throw CliUsageError(
                message = "--type and --name must be specified together",
                usage = usage,
            )
        }
        if (resourceId == null && type == null && name == null) {
            throw CliUsageError(
                message = "missing required selector: --id or --type with --name",
                usage = usage,
            )
        }

        return CliRequest.GetResValue(
            workdir = positionals.singleOrNull(),
            resourceId = resourceId,
            type = type,
            name = name,
            outputFormat = outputFormat,
        )
    }

    fun parseFindResValues(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("find-res-values")
        }
        val parsed = CliParserSupport.parseQueryCommand(tokens, CliUsages.findResValues)
        return CliRequest.FindResValues(
            workdir = parsed.workdir,
            query = parsed.query,
            window = parsed.window,
            outputFormat = parsed.outputFormat,
        )
    }
}

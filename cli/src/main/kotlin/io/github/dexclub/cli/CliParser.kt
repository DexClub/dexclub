package io.github.dexclub.cli

internal class CliParser {
    private val queryParser = CliQueryCommandParser()
    private val resourceParser = CliResourceCommandParser()
    private val exportParser = CliExportCommandParser()
    private val commandParsers: Map<String, (List<String>) -> CliRequest> = mapOf(
        "help" to ::parseHelp,
        "init" to ::parseInit,
        "switch" to ::parseSwitch,
        "targets" to { tokens -> parseWorkdirCommand("targets", tokens) },
        "status" to { tokens -> parseWorkdirCommand("status", tokens) },
        "gc" to { tokens -> parseWorkdirCommand("gc", tokens) },
        "inspect" to { tokens -> parseWorkdirCommand("inspect", tokens) },
        "manifest" to { tokens -> parseWorkdirCommand("manifest", tokens) },
        "res-table" to { tokens -> parseWorkdirCommand("res-table", tokens) },
        "decode-xml" to resourceParser::parseDecodeXml,
        "list-res" to { tokens -> parseWorkdirCommand("list-res", tokens) },
        "get-res-value" to resourceParser::parseGetResValue,
        "find-res-values" to resourceParser::parseFindResValues,
        "find-class" to queryParser::parseFindClass,
        "find-method" to queryParser::parseFindMethod,
        "inspect-method" to queryParser::parseInspectMethod,
        "find-field" to queryParser::parseFindField,
        "find-class-using-strings" to queryParser::parseFindClassUsingStrings,
        "find-method-using-strings" to queryParser::parseFindMethodUsingStrings,
        "export-class-dex" to exportParser::parseExportClassDex,
        "export-class-java" to exportParser::parseExportClassJava,
        "export-class-smali" to exportParser::parseExportClassSmali,
        "export-method-smali" to exportParser::parseExportMethodSmali,
        "export-method-dex" to exportParser::parseExportMethodDex,
        "export-method-java" to exportParser::parseExportMethodJava,
    )

    fun parse(argv: List<String>): CliRequest {
        if (argv.isEmpty()) {
            return CliRequest.Help()
        }
        if (argv.size == 1 && argv.first() == "--version") {
            return CliRequest.Version()
        }
        if (argv.size == 1 && CliHelp.isHelpFlag(argv.first())) {
            return CliRequest.Help()
        }
        val command = argv.first()
        return commandParsers[command]?.invoke(argv.drop(1)) ?: unknownCommand(command)
    }

    private fun parseHelp(tokens: List<String>): CliRequest.Help {
        if (tokens.isEmpty()) {
            return CliRequest.Help()
        }
        if (tokens.size != 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = CliUsages.help,
            )
        }
        val command = tokens.single()
        if (command.startsWith("--")) {
            throw CliUsageError(
                message = "unknown option: $command",
                usage = CliUsages.help,
            )
        }
        if (!CliHelp.isKnownCommand(command)) {
            throw CliUsageError(
                message = "unknown command: $command",
                usage = CliUsages.help,
                hint = "Run 'cli help' to see available commands.",
            )
        }
        return CliRequest.Help(command)
    }

    private fun parseInit(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("init")
        }
        val parsed = CliParserSupport.parsePositionalAndFlags(
            tokens = tokens,
            allowJson = false,
            usage = CliUsages.init,
        )
        if (parsed.positionals.size != 1) {
            throw CliUsageError(
                message = "missing required argument: <input>",
                usage = CliUsages.init,
            )
        }
        return CliRequest.Init(
            input = parsed.positionals.single(),
            outputFormat = parsed.outputFormat,
        )
    }

    private fun parseSwitch(tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help("switch")
        }
        val parsed = CliParserSupport.parsePositionalAndFlags(
            tokens = tokens,
            allowJson = false,
            usage = CliUsages.switch,
        )
        if (parsed.positionals.size != 1) {
            throw CliUsageError(
                message = "missing required argument: <input>",
                usage = CliUsages.switch,
            )
        }
        return CliRequest.Switch(
            input = parsed.positionals.single(),
            outputFormat = parsed.outputFormat,
        )
    }

    private fun parseWorkdirCommand(command: String, tokens: List<String>): CliRequest {
        if (tokens.size == 1 && CliHelp.isHelpFlag(tokens.single())) {
            return CliRequest.Help(command)
        }
        val usage = CliUsages.forCommand(command)
        val parsed = CliParserSupport.parsePositionalAndFlags(
            tokens = tokens,
            allowJson = true,
            usage = usage,
        )
        if (parsed.positionals.size > 1) {
            throw CliUsageError(
                message = "too many positional arguments",
                usage = usage,
            )
        }
        return when (command) {
            "status" -> CliRequest.Status(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "targets" -> CliRequest.Targets(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "gc" -> CliRequest.Gc(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "inspect" -> CliRequest.Inspect(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "manifest" -> CliRequest.Manifest(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "res-table" -> CliRequest.ResTable(parsed.positionals.singleOrNull(), parsed.outputFormat)
            "list-res" -> CliRequest.ListRes(parsed.positionals.singleOrNull(), parsed.outputFormat)
            else -> error("unsupported command: $command")
        }
    }

    private fun unknownCommand(command: String): Nothing =
        throw CliUsageError(
            message = "unknown command: $command",
            usage = CliUsages.general,
            hint = "Run 'cli help' to see available commands.",
        )
}

internal object CliUsages {
    const val general: String = "cli <command> ..."
    const val help: String = "cli help [command]"
    const val init: String = "cli init <input>"
    const val switch: String = "cli switch <input>"
    const val targets: String = "cli targets [workdir] [--json]"
    const val status: String = "cli status [workdir] [--json]"
    const val gc: String = "cli gc [workdir] [--json]"
    const val inspect: String = "cli inspect [workdir] [--json]"
    const val manifest: String = "cli manifest [workdir] [--json]"
    const val resTable: String = "cli res-table [workdir] [--json]"
    const val decodeXml: String = "cli decode-xml [workdir] --path <xml-path> [--json]"
    const val listRes: String = "cli list-res [workdir] [--json]"
    const val getResValue: String =
        "cli get-res-value [workdir] (--id <res-id> | --type <type> --name <name>) [--json]"
    const val findResValues: String =
        "cli find-res-values [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val findClass: String =
        "cli find-class [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val findMethod: String =
        "cli find-method [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val inspectMethod: String =
        "cli inspect-method [workdir] --descriptor <method-descriptor> [--include <sections>] [--json]"
    const val findField: String =
        "cli find-field [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val findClassUsingStrings: String =
        "cli find-class-using-strings [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val findMethodUsingStrings: String =
        "cli find-method-using-strings [workdir] (--query-json <json> | --query-file <file>) [--offset <n>] [--limit <n>] [--json]"
    const val exportClassDex: String =
        "cli export-class-dex [workdir] --class <class-name> [--source-path <path>] [--source-entry <entry>] --output <file>"
    const val exportClassJava: String =
        "cli export-class-java [workdir] --class <class-name> [--source-path <path>] [--source-entry <entry>] --output <file>"
    const val exportClassSmali: String =
        "cli export-class-smali [workdir] --class <class-name> [--source-path <path>] [--source-entry <entry>] --output <file> [--auto-unicode-decode true|false]"
    const val exportMethodSmali: String =
        "cli export-method-smali [workdir] --method <signature> [--source-path <path>] [--source-entry <entry>] --output <file> [--auto-unicode-decode true|false] [--mode snippet|class]"
    const val exportMethodDex: String =
        "cli export-method-dex [workdir] --method <signature> [--source-path <path>] [--source-entry <entry>] --output <file>"
    const val exportMethodJava: String =
        "cli export-method-java [workdir] --method <signature> [--source-path <path>] [--source-entry <entry>] --output <file>"

    fun forCommand(command: String): String =
        when (command) {
            "init" -> init
            "switch" -> switch
            "targets" -> targets
            "status" -> status
            "gc" -> gc
            "inspect" -> inspect
            "manifest" -> manifest
            "res-table" -> resTable
            "decode-xml" -> decodeXml
            "list-res" -> listRes
            "get-res-value" -> getResValue
            "find-res-values" -> findResValues
            "find-class" -> findClass
            "find-method" -> findMethod
            "inspect-method" -> inspectMethod
            "find-field" -> findField
            "find-class-using-strings" -> findClassUsingStrings
            "find-method-using-strings" -> findMethodUsingStrings
            "export-class-dex" -> exportClassDex
            "export-class-java" -> exportClassJava
            "export-class-smali" -> exportClassSmali
            "export-method-smali" -> exportMethodSmali
            "export-method-dex" -> exportMethodDex
            "export-method-java" -> exportMethodJava
            else -> general
        }
}

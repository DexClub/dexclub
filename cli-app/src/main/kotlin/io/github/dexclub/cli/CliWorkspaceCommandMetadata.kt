package io.github.dexclub.cli

internal enum class CliWorkspaceCommandParseMode {
    InitInput,
    SwitchInput,
    Workdir,
}

internal data class CliWorkspaceCommandMetadata(
    val command: String,
    val usage: String,
    val description: String,
    val arguments: List<String>,
    val options: List<String> = emptyList(),
    val output: String,
    val notes: List<String> = emptyList(),
    val generalHelpLine: String,
    val parseMode: CliWorkspaceCommandParseMode,
    val allowJson: Boolean,
)

internal object CliWorkspaceCommandCatalog {
    val commands: List<CliWorkspaceCommandMetadata> = listOf(
        CliWorkspaceCommandMetadata(
            command = "init",
            usage = "cli init <input>",
            description = "Initialize a workspace and create the managed .dexclub directory for the given input.",
            arguments = listOf("<input>  Single input file to bind as the active target."),
            output = "Text only. Prints the initialized workspace status summary.",
            notes = listOf(
                "This is the only command that may create .dexclub.",
                "The input argument is required and may not be omitted.",
                "Directory input is not supported in the first version.",
            ),
            generalHelpLine = "  init                     Initialize a workspace from an input path.",
            parseMode = CliWorkspaceCommandParseMode.InitInput,
            allowJson = false,
        ),
        CliWorkspaceCommandMetadata(
            command = "switch",
            usage = "cli switch <input>",
            description = "Switch the active target to an already initialized input in the same workspace.",
            arguments = listOf("<input>  Existing input path already bound in the workspace."),
            output = "Text only. Prints the switched workspace status summary.",
            notes = listOf(
                "switch does not create a new target. Use init first if the input has never been initialized.",
                "The input must belong to the same workspace as the current workdir.",
            ),
            generalHelpLine = "  switch                   Switch to an existing target in the current workspace.",
            parseMode = CliWorkspaceCommandParseMode.SwitchInput,
            allowJson = false,
        ),
        CliWorkspaceCommandMetadata(
            command = "targets",
            usage = "cli targets [workdir] [--json]",
            description = "List initialized targets in the current workspace and mark the active one.",
            arguments = listOf("[workdir]  Optional workspace directory. Defaults to the current directory."),
            options = listOf("--json  Render the target list as JSON."),
            output = "Text prints a stable tab-separated target table. JSON prints the target array directly.",
            notes = listOf(
                "targets is read-only and does not rebuild cache or refresh snapshots.",
            ),
            generalHelpLine = "  targets                  List initialized targets in the current workspace.",
            parseMode = CliWorkspaceCommandParseMode.Workdir,
            allowJson = true,
        ),
        CliWorkspaceCommandMetadata(
            command = "status",
            usage = "cli status [workdir] [--json]",
            description = "Read the current workspace status without modifying managed state.",
            arguments = listOf("[workdir]  Optional workspace directory. Defaults to the current directory."),
            options = listOf("--json  Render the structured status as JSON."),
            output = "Text prints workspace identity, binding summary, capability summary, cache state, and issues.",
            notes = listOf(
                "Status is read-only and does not refresh snapshot or rebuild cache.",
                "If workdir is omitted, cli uses cwd directly and does not search parent directories.",
            ),
            generalHelpLine = "  status                   Show workspace status and issues.",
            parseMode = CliWorkspaceCommandParseMode.Workdir,
            allowJson = true,
        ),
        CliWorkspaceCommandMetadata(
            command = "gc",
            usage = "cli gc [workdir] [--json]",
            description = "Delete safe-to-rebuild derived state for the active target.",
            arguments = listOf("[workdir]  Optional workspace directory. Defaults to the current directory."),
            options = listOf("--json  Render the deletion summary as JSON."),
            output = "Text prints workdir, targetId, deletedFiles, and deletedBytes.",
            notes = listOf(
                "Only active-target cache directories are cleaned.",
                "Binding metadata, target metadata, and snapshot metadata are preserved.",
            ),
            generalHelpLine = "  gc                       Delete safe-to-rebuild derived state.",
            parseMode = CliWorkspaceCommandParseMode.Workdir,
            allowJson = true,
        ),
        CliWorkspaceCommandMetadata(
            command = "refresh",
            usage = "cli refresh [workdir] [--json]",
            description = "Rebuild and persist the active target snapshot explicitly.",
            arguments = listOf("[workdir]  Optional workspace directory. Defaults to the current directory."),
            options = listOf("--json  Render the refreshed active-target summary as JSON."),
            output = "Text prints the refreshed active target summary. JSON prints the structured summary directly.",
            notes = listOf(
                "refresh is the explicit high-cost path for re-reading workspace materials.",
                "Use status when you only need read-only health and issue reporting.",
            ),
            generalHelpLine = "  refresh                  Rebuild the active target snapshot explicitly.",
            parseMode = CliWorkspaceCommandParseMode.Workdir,
            allowJson = true,
        ),
        CliWorkspaceCommandMetadata(
            command = "inspect",
            usage = "cli inspect [workdir] [--json]",
            description = "Print the current active target summary for the workspace.",
            arguments = listOf("[workdir]  Optional workspace directory. Defaults to the current directory."),
            options = listOf("--json  Render the structured summary as JSON."),
            output = "Text prints kind, input summary, inventory counts, optional classCount, and capabilities.",
            notes = listOf(
                "Inspect focuses on the active target summary rather than workspace status.",
            ),
            generalHelpLine = "  inspect                  Show the active target summary.",
            parseMode = CliWorkspaceCommandParseMode.Workdir,
            allowJson = true,
        ),
    )

    private val commandsByName = commands.associateBy(CliWorkspaceCommandMetadata::command)

    fun find(command: String): CliWorkspaceCommandMetadata? = commandsByName[command]

    fun require(command: String): CliWorkspaceCommandMetadata =
        commandsByName[command] ?: error("unknown workspace command: $command")
}

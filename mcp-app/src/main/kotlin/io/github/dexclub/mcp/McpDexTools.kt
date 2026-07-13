package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.ClassHit
import io.github.dexclub.core.app.contract.MethodHit
import io.modelcontextprotocol.kotlin.sdk.server.Server

internal fun McpApp.registerDexTools(server: Server) {
    registerCatalogTool(server, McpDexToolCatalog.require("inspect_method")) { request ->
        runToolCatching {
            val includes = request.methodIncludeSections()
            val target = request.dexToolTarget()
            val brief = request.briefFlag()
            val execution = inspectMethodExecution(
                sessionId = target.sessionId,
                workdir = target.workdir,
                methodHandle = request.optionalStringArgument("method_handle"),
                descriptor = request.optionalStringArgument("descriptor"),
                sourcePath = request.optionalStringArgument("source_path"),
                sourceEntry = request.optionalStringArgument("source_entry"),
                includes = includes,
            )
            inspectMethodResult(execution, brief)
        }
    }

    registerCatalogTool(server, McpDexToolCatalog.require("export_method_java")) { request ->
        exportMethodTextTool(
            request = request,
            view = "java",
        )
    }

    registerCatalogTool(server, McpDexToolCatalog.require("export_method_smali")) { request ->
        exportMethodTextTool(
            request = request,
            view = "smali",
        )
    }

    registerCatalogTool(server, McpDexToolCatalog.require("export_class_java")) { request ->
        exportClassTextTool(
            request = request,
            view = "java",
        )
    }

    registerCatalogTool(server, McpDexToolCatalog.require("export_class_smali")) { request ->
        exportClassTextTool(
            request = request,
            view = "smali",
        )
    }

    registerCatalogTool(server, McpDexToolCatalog.require("find_methods")) { request ->
        runToolCatching {
            val target = request.dexToolTarget()
            val brief = request.briefFlag()
            val fields = request.methodProjectionFields(target.sessionId)
            val execution = findMethodsExecution(
                sessionId = target.sessionId,
                workdir = target.workdir,
                classNameContains = request.optionalStringArgument("class_name_contains"),
                methodNameContains = request.optionalStringArgument("method_name_contains"),
                descriptorContains = request.optionalStringArgument("descriptor_contains"),
                offset = request.intArgument("offset"),
                limit = request.intArgument("limit"),
            )
            findMethodsResult(
                execution = execution,
                handleProvider = execution.session?.let { activeSession ->
                    { hit: MethodHit ->
                        sessionStore.putMethodHandle(activeSession.sessionId, hit.descriptor, hit.sourcePath, hit.sourceEntry)
                    }
                },
                fields = fields,
                brief = brief,
            )
        }
    }

    registerCatalogTool(server, McpDexToolCatalog.require("find_classes_using_strings")) { request ->
        runToolCatching {
            val target = request.dexToolTarget()
            val brief = request.briefFlag()
            val fields = request.classProjectionFields(target.sessionId)
            val execution = findClassesUsingStringsExecution(
                sessionId = target.sessionId,
                workdir = target.workdir,
                containsAnyStrings = request.stringArrayArgument("contains_any_strings"),
                containsAllStrings = request.stringArrayArgument("contains_all_strings"),
                offset = request.intArgument("offset"),
                limit = request.intArgument("limit"),
            )
            findClassesUsingStringsResult(
                execution = execution,
                handleProvider = execution.session?.let { activeSession ->
                    { hit: ClassHit ->
                        sessionStore.putClassHandle(activeSession.sessionId, hit.className, hit.sourcePath, hit.sourceEntry)
                    }
                },
                fields = fields,
                brief = brief,
            )
        }
    }

    registerCatalogTool(server, McpDexToolCatalog.require("find_methods_using_strings")) { request ->
        runToolCatching {
            val target = request.dexToolTarget()
            val brief = request.briefFlag()
            val fields = request.methodProjectionFields(target.sessionId)
            val execution = findMethodsUsingStringsExecution(
                sessionId = target.sessionId,
                workdir = target.workdir,
                containsAnyStrings = request.stringArrayArgument("contains_any_strings"),
                containsAllStrings = request.stringArrayArgument("contains_all_strings"),
                offset = request.intArgument("offset"),
                limit = request.intArgument("limit"),
            )
            findMethodsUsingStringsResult(
                execution = execution,
                handleProvider = execution.session?.let { activeSession ->
                    { hit: MethodHit ->
                        sessionStore.putMethodHandle(activeSession.sessionId, hit.descriptor, hit.sourcePath, hit.sourceEntry)
                    }
                },
                fields = fields,
                brief = brief,
            )
        }
    }
}


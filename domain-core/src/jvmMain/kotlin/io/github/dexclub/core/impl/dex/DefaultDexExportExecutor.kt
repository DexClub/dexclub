package io.github.dexclub.core.impl.dex

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import io.github.dexclub.core.api.dex.DexExportError
import io.github.dexclub.core.api.dex.DexExportErrorReason
import io.github.dexclub.core.api.dex.ExportClassDexRequest
import io.github.dexclub.core.api.dex.ExportClassJavaRequest
import io.github.dexclub.core.api.dex.ExportClassSmaliRequest
import io.github.dexclub.core.api.dex.ExportMethodDexRequest
import io.github.dexclub.core.api.dex.ExportMethodJavaRequest
import io.github.dexclub.core.api.dex.ExportMethodSmaliRequest
import io.github.dexclub.core.api.dex.ExportResult
import io.github.dexclub.core.api.shared.MethodSmaliMode
import io.github.dexclub.core.api.shared.SourceLocator
import io.github.dexclub.core.api.workspace.WorkspaceContext
import io.github.dexclub.core.impl.workspace.model.MaterialInventory
import io.github.dexclub.core.impl.workspace.store.WorkspaceStore
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths

internal class DefaultDexExportExecutor(
    private val store: WorkspaceStore,
    private val toolVersion: String,
) : DexExportExecutor {
    private val jadxDecompilerService = JadxDecompilerService()
    private val sourceResolver = DexExportSourceResolver(store, toolVersion)
    private val fileWriter = DexExportFileWriter(store)

    override fun exportClassDex(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportClassDexRequest,
    ): ExportResult {
        val classDef = resolveClassDef(
            workspace = workspace,
            inventory = inventory,
            className = request.className,
            source = request.source,
        )
        val outputPath = fileWriter.writeSingleClassDex(
            classDef = classDef,
            outputPath = request.outputPath,
        )
        return ExportResult(outputPath = outputPath)
    }

    override fun exportClassSmali(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportClassSmaliRequest,
    ): ExportResult {
        val classDef = resolveClassDef(
            workspace = workspace,
            inventory = inventory,
            className = request.className,
            source = request.source,
        )
        val outputPath = fileWriter.renderClassSmali(
            classDef = classDef,
            outputPath = request.outputPath,
            renderText = { renderClassSmaliText(it, request.autoUnicodeDecode) },
        )
        return ExportResult(outputPath = outputPath)
    }

    override fun exportClassJava(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportClassJavaRequest,
    ): ExportResult {
        val classDef = resolveClassDef(
            workspace = workspace,
            inventory = inventory,
            className = request.className,
            source = request.source,
        )
        return ExportResult(
            outputPath = decompileSingleClassDefToJava(
                workspace = workspace,
                classDef = classDef,
                outputPath = request.outputPath,
            ),
        )
    }

    override fun exportMethodSmali(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportMethodSmaliRequest,
    ): ExportResult {
        val signature = parseMethodSignature(request.methodSignature)
        val methodOnlyClassDef = resolveMethodOnlyClassDef(
            workspace = workspace,
            inventory = inventory,
            signature = signature,
            methodSignature = request.methodSignature,
            source = request.source,
        )
        val classSmali = renderClassSmaliText(
            classDef = methodOnlyClassDef,
            autoUnicodeDecode = request.autoUnicodeDecode,
        )
        val methodBlock = extractMethodBlock(
            classSmali = classSmali,
            methodName = signature.methodName,
            descriptor = signature.descriptor,
        )
        val outputText = when (request.mode) {
            MethodSmaliMode.Snippet -> ensureTrailingNewline(methodBlock)
            MethodSmaliMode.Class -> buildMethodClassShell(classSmali, methodBlock)
        }
        return ExportResult(
            outputPath = fileWriter.writeTextOutput(
                outputPath = request.outputPath,
                text = outputText,
            ),
        )
    }

    override fun exportMethodDex(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportMethodDexRequest,
    ): ExportResult {
        val methodOnlyClassDef = resolveMethodOnlyClassDef(
            workspace = workspace,
            inventory = inventory,
            methodSignature = request.methodSignature,
            source = request.source,
        )
        return ExportResult(
            outputPath = fileWriter.writeSingleClassDex(
                classDef = methodOnlyClassDef,
                outputPath = request.outputPath,
            ),
        )
    }

    override fun exportMethodJava(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        request: ExportMethodJavaRequest,
    ): ExportResult {
        val methodOnlyClassDef = resolveMethodOnlyClassDef(
            workspace = workspace,
            inventory = inventory,
            methodSignature = request.methodSignature,
            source = request.source,
        )
        return ExportResult(
            outputPath = decompileSingleClassDefToJava(
                workspace = workspace,
                classDef = methodOnlyClassDef,
                outputPath = request.outputPath,
            ),
        )
    }

    private fun resolveClassDef(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        className: String,
        source: SourceLocator,
    ): ClassDef = sourceResolver.resolveUniqueClassSource(
        workdirPath = Paths.get(workspace.workdir),
        inventory = inventory,
        className = className,
        source = source,
        workspace = workspace,
    ).classDef

    private fun resolveMethodOnlyClassDef(
        workspace: WorkspaceContext,
        inventory: MaterialInventory,
        methodSignature: String,
        source: SourceLocator,
        signature: ParsedMethodSignature = parseMethodSignature(methodSignature),
    ): ClassDef {
        val classDef = resolveClassDef(
            workspace = workspace,
            inventory = inventory,
            className = signature.classSignature,
            source = source,
        )
        val method = classDef.methods.firstOrNull {
            it.name == signature.methodName && methodDescriptorOf(it) == signature.descriptor
        } ?: throw DexExportError(
            reason = DexExportErrorReason.MethodNotFound,
            message = buildString {
                append("method not found: ")
                append(methodSignature)
                append(" in class ")
                append(signature.classSignature)
                val sourceDescription = source.describe()
                if (sourceDescription != null) {
                    append(" (")
                    append(sourceDescription)
                    append(')')
                }
            },
        )
        return buildMethodOnlyClassDef(
            classDef = classDef,
            method = method,
        )
    }

    private fun buildMethodOnlyClassDef(classDef: ClassDef, method: Method): ClassDef {
        val immutableMethod = ImmutableMethod.of(method)
        return ImmutableClassDef(
            classDef.type,
            classDef.accessFlags,
            classDef.superclass,
            classDef.interfaces,
            classDef.sourceFile,
            classDef.annotations,
            emptyList(),
            emptyList(),
            if (method in classDef.directMethods) listOf(immutableMethod) else emptyList(),
            if (method in classDef.virtualMethods) listOf(immutableMethod) else emptyList(),
        )
    }

    private fun decompileSingleClassDefToJava(
        workspace: WorkspaceContext,
        classDef: ClassDef,
        outputPath: String,
    ): String = fileWriter.decompileSingleClassDefToJava(
        workspace = workspace,
        classDef = classDef,
        outputPath = outputPath,
        sanitize = ::sanitizeClassDefForJavaDecompile,
        decompile = jadxDecompilerService::decompileDexToJavaSource,
    )

    private fun sanitizeClassDefForJavaDecompile(classDef: ClassDef): ClassDef =
        ImmutableClassDef(
            classDef.type,
            classDef.accessFlags,
            classDef.superclass,
            classDef.interfaces,
            null,
            emptySet(),
            classDef.staticFields.map(::sanitizeFieldForJavaDecompile),
            classDef.instanceFields.map(::sanitizeFieldForJavaDecompile),
            classDef.directMethods.map(::sanitizeMethodForJavaDecompile),
            classDef.virtualMethods.map(::sanitizeMethodForJavaDecompile),
        )

    private fun sanitizeFieldForJavaDecompile(field: com.android.tools.smali.dexlib2.iface.Field) =
        ImmutableField(
            field.definingClass,
            field.name,
            field.type,
            field.accessFlags,
            field.initialValue,
            emptySet(),
            emptySet(),
        )

    private fun sanitizeMethodForJavaDecompile(method: Method): Method {
        val implementation = method.implementation
        val sanitizedImplementation = if (implementation == null) {
            null
        } else {
            ImmutableMethodImplementation(
                implementation.registerCount,
                implementation.instructions,
                implementation.tryBlocks,
                emptyList(),
            )
        }
        return ImmutableMethod(
            method.definingClass,
            method.name,
            method.parameters,
            method.returnType,
            method.accessFlags,
            emptySet(),
            emptySet(),
            sanitizedImplementation,
        )
    }

    private fun renderClassSmaliText(
        classDef: ClassDef,
        autoUnicodeDecode: Boolean,
    ): String {
        val options = BaksmaliOptions().apply {
            apiLevel = Opcodes.getDefault().api
            parameterRegisters = true
            localsDirective = true
            debugInfo = true
            accessorComments = true
        }
        val text = StringWriter()
        val writer: BaksmaliWriter = if (autoUnicodeDecode) {
            UnescapedUnicodeBaksmaliWriter(text)
        } else {
            BaksmaliWriter(text)
        }
        ClassDefinition(options, classDef).writeTo(writer)
        writer.flush()
        return text.toString()
    }

}

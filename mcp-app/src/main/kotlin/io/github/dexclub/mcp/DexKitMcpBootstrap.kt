package io.github.dexclub.mcp

import io.github.dexclub.core.app.contract.DexKitNative
import java.io.File

internal object DexKitMcpBootstrap {
    fun configureNativeLibraryDir() {
        if (System.getProperty(DexKitNative.LIBRARY_DIR_PROPERTY) != null) return
        if (System.getenv(DexKitNative.LIBRARY_DIR_ENV) != null) return

        val codeSource = McpApp::class.java.protectionDomain.codeSource?.location ?: return
        val codeSourceFile = runCatching { File(codeSource.toURI()) }.getOrNull() ?: return
        if (!codeSourceFile.isFile || !codeSourceFile.name.endsWith(".jar", ignoreCase = true)) return

        val libraryDir = codeSourceFile.parentFile ?: return
        System.setProperty(DexKitNative.LIBRARY_DIR_PROPERTY, libraryDir.absolutePath)
    }
}


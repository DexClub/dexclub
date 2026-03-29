package io.github.dexclub.codeview.runtime

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
public fun CodeRuntime(): CodeRuntime = DefaultCodeRuntime()

package io.github.dexclub.codeview.core.language

import io.github.dexclub.codeview.core.api.CodeViewApi

@CodeViewApi
@JvmInline
public value class CodeLanguageFamilyId(public val value: String) {
    init {
        require(value.isNotBlank()) { "familyId 不能为空" }
    }


    override fun toString(): String = value
}

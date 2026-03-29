package io.github.dexclub.codeview.language.addon

import io.github.dexclub.codeview.core.api.CodeViewApi
import io.github.dexclub.codeview.language.install.CodeLanguageInstallable

@CodeViewApi
public class CodeLanguageConflictException(
    message: String,
) : IllegalStateException(message)

@CodeViewApi
public class CodeAddons private constructor(
    public val languages: List<CodeLanguageInstallable>,
) {
    public class Builder {
        private val languages = mutableListOf<CodeLanguageInstallable>()

        public fun install(language: CodeLanguageInstallable): Unit {
            validateConflict(language)
            languages += language
        }


        public fun build(): CodeAddons = CodeAddons(
            languages = languages.toList(),
        )


        private fun validateConflict(candidate: CodeLanguageInstallable) {
            val candidateDescriptor = candidate.descriptor
            for (existing in languages) {
                val existingDescriptor = existing.descriptor
                if (
                    existingDescriptor.familyId == candidateDescriptor.familyId &&
                    existingDescriptor.languageId == candidateDescriptor.languageId
                ) {
                    throw CodeLanguageConflictException(
                        message = "重复安装语言包: familyId=${candidateDescriptor.familyId}, languageId=${candidateDescriptor.languageId}",
                    )
                }

                if (existingDescriptor.languageId != candidateDescriptor.languageId) {
                    continue
                }

                if (existingDescriptor.familyId == candidateDescriptor.familyId) {
                    continue
                }

                val overlappedCapabilities = existingDescriptor.capabilities intersect candidateDescriptor.capabilities
                if (overlappedCapabilities.isNotEmpty()) {
                    throw CodeLanguageConflictException(
                        message = buildString {
                            append("语言能力冲突: languageId=")
                            append(candidateDescriptor.languageId)
                            append(", capabilities=")
                            append(overlappedCapabilities.joinToString(separator = ",") { capability ->
                                capability.name
                            })
                            append(", existingFamilyId=")
                            append(existingDescriptor.familyId)
                            append(", candidateFamilyId=")
                            append(candidateDescriptor.familyId)
                        },
                    )
                }
            }
        }
    }

    public companion object {
        public fun build(block: Builder.() -> Unit): CodeAddons {
            val builder = Builder()
            builder.block()
            return builder.build()
        }
    }
}

package io.github.dexclub.core.api.resource

enum class ResourceDecodeErrorReason {
    ManifestSourceMissing,
    AmbiguousManifestSource,
    ManifestEntryMissing,
    ManifestTextInvalid,
    ManifestDecodeFailed,
    ManifestInspectFailed,
    ResourceTableSourceMissing,
    AmbiguousResourceTableSource,
    ResourceTableEntryMissing,
    ResourceTableDecodeFailed,
    XmlPathNotFound,
    AmbiguousXmlPath,
    XmlDecodeFailed,
    ResourceValueInvalidSelector,
    ResourceValueNotFound,
    ResourceValueAmbiguous,
    ResourceQueryInvalid,
}

class ResourceDecodeError(
    val reason: ResourceDecodeErrorReason,
    val sourcePath: String? = null,
    val candidates: List<ResourceValueCandidate> = emptyList(),
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

data class ResourceValueCandidate(
    val resourceId: String? = null,
    val packageName: String? = null,
    val type: String? = null,
    val name: String? = null,
    val sourcePath: String? = null,
    val sourceEntry: String? = null,
)

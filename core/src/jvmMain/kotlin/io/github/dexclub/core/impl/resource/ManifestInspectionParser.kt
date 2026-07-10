package io.github.dexclub.core.impl.resource

import io.github.dexclub.core.api.resource.InspectManifestRequest
import io.github.dexclub.core.api.resource.ManifestApplicationInfo
import io.github.dexclub.core.api.resource.ManifestComponentInfo
import io.github.dexclub.core.api.resource.ManifestInspectionResult
import io.github.dexclub.core.api.resource.ManifestInspectionSection
import io.github.dexclub.core.api.resource.ManifestIntentData
import io.github.dexclub.core.api.resource.ManifestIntentFilter
import io.github.dexclub.core.api.resource.ManifestMetaData
import io.github.dexclub.core.api.resource.ManifestResult
import io.github.dexclub.core.api.resource.ManifestUsesFeature
import io.github.dexclub.core.api.resource.ManifestUsesSdk
import io.github.dexclub.core.api.resource.ResourceDecodeError
import io.github.dexclub.core.api.resource.ResourceDecodeErrorReason
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

internal class ManifestInspectionParser {
    fun parse(
        manifest: ManifestResult,
        request: InspectManifestRequest,
    ): ManifestInspectionResult {
        val document = try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(ByteArrayInputStream(manifest.text.toByteArray(Charsets.UTF_8)))
        } catch (error: Exception) {
            throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ManifestInspectFailed,
                sourcePath = manifest.sourcePath,
                message = "Failed to inspect manifest: ${manifest.sourcePath ?: manifest.sourceEntry ?: "AndroidManifest.xml"}",
                cause = error,
            )
        }
        val root = document.documentElement?.takeIf { it.tagName == "manifest" }
            ?: throw ResourceDecodeError(
                reason = ResourceDecodeErrorReason.ManifestTextInvalid,
                sourcePath = manifest.sourcePath,
                message = "Decoded manifest is not a <manifest> document",
            )
        val includes = request.includes
        val application = root.firstChildElements("application").firstOrNull()
        val queryElements = root.firstChildElements("queries")
        val usesPermissions = root.firstChildElements("uses-permission")
            .mapNotNull { it.androidAttr("name") }
            .distinct()
            .sorted()
            .takeIf { includes.contains(ManifestInspectionSection.UsesPermissions) }
        val definedPermissions = root.firstChildElements("permission")
            .mapNotNull { it.androidAttr("name") }
            .distinct()
            .sorted()
            .takeIf { includes.contains(ManifestInspectionSection.DefinedPermissions) }
        val usesFeatures = root.firstChildElements("uses-feature")
            .map { it.toUsesFeature() }
            .sortedWith(compareBy({ it.name.orEmpty() }, { it.glEsVersion.orEmpty() }))
            .takeIf { includes.contains(ManifestInspectionSection.UsesFeatures) }
        val queriesPackages = queryElements
            .flatMap { queries ->
                queries.firstChildElements("package").mapNotNull { it.androidAttr("name") }
            }
            .distinct()
            .sorted()
            .takeIf { includes.contains(ManifestInspectionSection.Queries) }
        val queriesProviders = queryElements
            .flatMap { queries ->
                queries.firstChildElements("provider").mapNotNull { it.androidAttr("authorities") }
            }
            .distinct()
            .sorted()
            .takeIf { includes.contains(ManifestInspectionSection.Queries) }
        val queriesIntents = queryElements
            .flatMap { queries ->
                queries.firstChildElements("intent").map { it.toIntentFilter() }
            }
            .takeIf { includes.contains(ManifestInspectionSection.Queries) }
        return ManifestInspectionResult(
            sourcePath = manifest.sourcePath,
            sourceEntry = manifest.sourceEntry,
            packageName = root.getAttribute("package").trim(),
            versionCode = root.androidAttr("versionCode"),
            versionName = root.androidAttr("versionName"),
            sharedUserId = root.androidAttr("sharedUserId"),
            usesSdk = root.firstChildElements("uses-sdk").firstOrNull()?.toUsesSdk()
                .takeIf { includes.contains(ManifestInspectionSection.UsesSdk) },
            application = application?.toApplicationInfo(root)
                .takeIf { includes.contains(ManifestInspectionSection.Application) },
            usesPermissions = usesPermissions,
            definedPermissions = definedPermissions,
            usesFeatures = usesFeatures,
            queriesPackages = queriesPackages,
            queriesProviders = queriesProviders,
            queriesIntents = queriesIntents,
            activities = application.childComponents(root, "activity")
                .takeIf { includes.contains(ManifestInspectionSection.Activities) },
            activityAliases = application.childComponents(root, "activity-alias")
                .takeIf { includes.contains(ManifestInspectionSection.ActivityAliases) },
            services = application.childComponents(root, "service")
                .takeIf { includes.contains(ManifestInspectionSection.Services) },
            receivers = application.childComponents(root, "receiver")
                .takeIf { includes.contains(ManifestInspectionSection.Receivers) },
            providers = application.childComponents(root, "provider")
                .takeIf { includes.contains(ManifestInspectionSection.Providers) },
            text = manifest.text.takeIf { request.includeText },
        )
    }

    private fun Element.toUsesSdk(): ManifestUsesSdk =
        ManifestUsesSdk(
            minSdkVersion = androidAttr("minSdkVersion"),
            targetSdkVersion = androidAttr("targetSdkVersion"),
            maxSdkVersion = androidAttr("maxSdkVersion"),
        )

    private fun Element.toApplicationInfo(root: Element): ManifestApplicationInfo =
        ManifestApplicationInfo(
            name = normalizeComponentName(root, androidAttr("name")),
            rawName = androidAttr("name"),
            label = androidAttr("label"),
            icon = androidAttr("icon"),
            debuggable = androidBooleanAttr("debuggable"),
            allowBackup = androidBooleanAttr("allowBackup"),
            usesCleartextTraffic = androidBooleanAttr("usesCleartextTraffic"),
            networkSecurityConfig = androidAttr("networkSecurityConfig"),
            metaData = firstChildElements("meta-data")
                .mapNotNull { it.toMetaData() }
                .sortedBy { it.name },
        )

    private fun Element?.childComponents(root: Element, tagName: String): List<ManifestComponentInfo> =
        this?.firstChildElements(tagName)
            ?.mapNotNull { it.toComponentInfo(root) }
            ?.sortedBy { it.name }
            .orEmpty()

    private fun Element.toComponentInfo(root: Element): ManifestComponentInfo? {
        val rawName = androidAttr("name") ?: return null
        return ManifestComponentInfo(
            name = normalizeComponentName(root, rawName) ?: rawName,
            rawName = rawName,
            exported = androidBooleanAttr("exported"),
            enabled = androidBooleanAttr("enabled"),
            permission = androidAttr("permission"),
            process = androidAttr("process"),
            authorities = androidAttr("authorities"),
            targetActivity = androidAttr("targetActivity")?.let { normalizeComponentName(root, it) },
            intentFilters = firstChildElements("intent-filter")
                .map { it.toIntentFilter() },
            metaData = firstChildElements("meta-data")
                .mapNotNull { it.toMetaData() }
                .sortedBy { it.name },
        )
    }

    private fun Element.toIntentFilter(): ManifestIntentFilter =
        ManifestIntentFilter(
            actions = firstChildElements("action")
                .mapNotNull { it.androidAttr("name") }
                .distinct()
                .sorted(),
            categories = firstChildElements("category")
                .mapNotNull { it.androidAttr("name") }
                .distinct()
                .sorted(),
            data = firstChildElements("data")
                .map { it.toIntentData() }
                .sortedWith(
                    compareBy(
                        { it.scheme.orEmpty() },
                        { it.host.orEmpty() },
                        { it.path.orEmpty() },
                        { it.mimeType.orEmpty() },
                    ),
                ),
        )

    private fun Element.toIntentData(): ManifestIntentData =
        ManifestIntentData(
            scheme = androidAttr("scheme"),
            host = androidAttr("host"),
            port = androidAttr("port"),
            path = androidAttr("path"),
            pathPrefix = androidAttr("pathPrefix"),
            pathPattern = androidAttr("pathPattern"),
            pathSuffix = androidAttr("pathSuffix"),
            mimeType = androidAttr("mimeType"),
        )

    private fun Element.toMetaData(): ManifestMetaData? {
        val name = androidAttr("name") ?: return null
        return ManifestMetaData(
            name = name,
            value = androidAttr("value"),
            resource = androidAttr("resource"),
        )
    }

    private fun Element.toUsesFeature(): ManifestUsesFeature =
        ManifestUsesFeature(
            name = androidAttr("name"),
            required = androidBooleanAttr("required"),
            glEsVersion = androidAttr("glEsVersion"),
        )

    private fun normalizeComponentName(root: Element, rawName: String?): String? {
        val value = rawName?.trim().orEmpty()
        if (value.isEmpty()) return null
        val packageName = root.getAttribute("package").trim()
        return when {
            value.startsWith(".") -> "$packageName$value"
            '.' in value -> value
            packageName.isNotBlank() -> "$packageName.$value"
            else -> value
        }
    }

    private fun Element.firstChildElements(tagName: String): List<Element> {
        val nodes = childNodes
        return buildList {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                if (node is Element && node.tagName == tagName) {
                    add(node)
                }
            }
        }
    }

    private fun Element.androidAttr(name: String): String? =
        getAttributeNS(ANDROID_NS, name)
            .trim()
            .takeIf { it.isNotEmpty() }
            ?: getAttribute("android:$name")
                .trim()
                .takeIf { it.isNotEmpty() }

    private fun Element.androidBooleanAttr(name: String): Boolean? =
        when (androidAttr(name)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}

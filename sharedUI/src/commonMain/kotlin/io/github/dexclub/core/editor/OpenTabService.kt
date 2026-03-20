package io.github.dexclub.core.editor

import io.github.dexclub.app.model.OpenTabContentSnapshot
import io.github.dexclub.app.model.OpenTabUiModel
import io.github.dexclub.core.workspace.WorkspaceIndexService
import io.github.dexclub.core.workspace.WorkspaceIndexedClassRecord

typealias ExportCodePathForClass = suspend (WorkspaceIndexedClassRecord, String) -> String

data class OpenTabSessionSnapshot(
    val tabs: List<OpenTabUiModel>,
    val selectedTabId: String?,
    val contentsByTabId: Map<String, Map<String, OpenTabContentSnapshot>>,
)

class OpenTabService(
    private val editorSessionRepository: EditorSessionRepository,
    private val workspaceIndexService: WorkspaceIndexService,
) {
    suspend fun loadSessionSnapshot(
        preferredTabId: String?,
        currentSelectedTabId: String?,
    ): OpenTabSessionSnapshot {
        val rawTabs = editorSessionRepository.getAllTabs()
        val classVisualKindsByTargetKey = workspaceIndexService.findByNames(
            rawTabs
                .asSequence()
                .filter { tab -> tab.targetType == EDITOR_SESSION_TARGET_TYPE_CLASS }
                .map(EditorSessionTabRecord::targetKey)
                .distinct()
                .toList()
        ).associate { record ->
            record.className to record.classVisualKind
        }

        val contentsByTabId = mutableMapOf<String, Map<String, OpenTabContentSnapshot>>()
        val tabs = rawTabs.map { tab ->
            val contents = editorSessionRepository.getContentsByTabId(tab.tabId).associateBy { content -> content.kind }
            val panes = editorSessionRepository.getPanesByTabId(tab.tabId)
            val kindPriorities = editorSessionRepository
                .getKindPrioritiesByTabId(tab.tabId)
                .associate { priority -> priority.kind to priority.priority }
            contentsByTabId[tab.tabId] = contents.mapValues { (_, content) -> content.toSnapshot() }
            tab.toUiModel(
                contents = contents,
                panes = panes,
                kindPriorities = kindPriorities,
                classVisualKind = classVisualKindsByTargetKey[tab.targetKey],
            )
        }

        return OpenTabSessionSnapshot(
            tabs = tabs,
            selectedTabId = resolveSelectedTabId(
                tabs = rawTabs,
                preferredTabId = preferredTabId,
                currentSelectedTabId = currentSelectedTabId,
            ),
            contentsByTabId = contentsByTabId,
        )
    }

    suspend fun openClassTab(
        cls: WorkspaceIndexedClassRecord,
        exportCodePathForClass: ExportCodePathForClass,
    ): EditorSessionTabRecord {
        val existed = editorSessionRepository.getTabByTarget(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
        if (existed != null) {
            return existed
        }

        val now = System.currentTimeMillis()
        val tabId = buildTabId(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
        val initialKind = EDITOR_SESSION_KIND_SMALI
        val tab = EditorSessionTabRecord(
            tabId = tabId,
            targetType = EDITOR_SESSION_TARGET_TYPE_CLASS,
            targetKey = cls.className,
            title = cls.displayName,
            subtitle = cls.className.substringBeforeLast('.', ""),
            layoutMode = EDITOR_SESSION_LAYOUT_SINGLE,
            activePaneIndex = 0,
            activeKind = initialKind,
            createdAt = now,
            lastViewedAt = now,
        )
        editorSessionRepository.insertTabWithInitialState(
            tab = tab,
            contents = listOf(
                EditorSessionContentRecord(
                    tabId = tab.tabId,
                    kind = initialKind,
                    codePath = exportCodePathForClass(cls, initialKind),
                    updatedAt = now,
                )
            ),
            panes = listOf(
                EditorSessionPaneRecord(
                    tabId = tab.tabId,
                    paneIndex = 0,
                    kind = initialKind,
                    weight = 1f,
                )
            ),
            kindPriorities = defaultKindPriorities(tab.tabId),
        )
        return tab
    }

    suspend fun loadRequiredContents(
        tabId: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): Map<String, EditorSessionContentRecord> {
        val tab = editorSessionRepository.getTabById(tabId) ?: return emptyMap()
        val panes = editorSessionRepository.getPanesByTabId(tabId)
        val requiredKinds = if (tab.layoutMode == EDITOR_SESSION_LAYOUT_SINGLE) {
            listOf(panes.firstOrNull()?.kind ?: tab.activeKind)
        } else {
            panes.map(EditorSessionPaneRecord::kind).distinct()
        }

        return requiredKinds.associateWith { kind ->
            ensureTabContent(
                tab = tab,
                kind = kind,
                exportCodePathForClass = exportCodePathForClass,
            )
        }
    }

    suspend fun updateTabLastViewedAt(
        tabId: String,
        lastViewedAt: Long,
    ) {
        editorSessionRepository.updateTabLastViewedAt(tabId, lastViewedAt)
    }

    suspend fun getTabById(tabId: String): EditorSessionTabRecord? {
        return editorSessionRepository.getTabById(tabId)
    }

    suspend fun getTabByTarget(targetType: String, targetKey: String): EditorSessionTabRecord? {
        return editorSessionRepository.getTabByTarget(targetType, targetKey)
    }

    suspend fun getPanesByTabId(tabId: String): List<EditorSessionPaneRecord> {
        return editorSessionRepository.getPanesByTabId(tabId)
    }

    suspend fun deleteTabsByIds(tabIds: List<String>) {
        editorSessionRepository.deleteTabsByIds(tabIds)
    }

    suspend fun updateActivePane(
        tabId: String,
        paneIndex: Int,
        kind: String,
    ) {
        val latest = editorSessionRepository.getTabById(tabId) ?: return
        editorSessionRepository.updateTab(
            latest.copy(
                activePaneIndex = paneIndex,
                activeKind = kind,
                lastViewedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun toggleCodeView(
        tabId: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        val current = editorSessionRepository.getTabById(tabId) ?: return
        val orderedKinds = orderedKinds(tabId)
        val primary = orderedKinds.firstOrNull() ?: EDITOR_SESSION_KIND_SMALI
        val secondary = orderedKinds.getOrNull(1) ?: oppositeKind(primary)

        if (current.layoutMode != EDITOR_SESSION_LAYOUT_SINGLE) {
            applySingleMode(current, primary, exportCodePathForClass)
        } else if (current.activeKind == primary) {
            applySingleMode(current, secondary, exportCodePathForClass)
        } else {
            applyMixedMode(current, primary, secondary, exportCodePathForClass)
        }
    }

    suspend fun prioritizeKind(
        tabId: String,
        preferredKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        val latest = editorSessionRepository.getTabById(tabId) ?: return

        val dbPriorities = editorSessionRepository.getKindPrioritiesByTabId(tabId)
        val fallback = defaultKindPriorities(tabId)
        val base = if (dbPriorities.isEmpty()) fallback else dbPriorities

        val kinds = base
            .sortedBy(EditorSessionKindPriorityRecord::priority)
            .map(EditorSessionKindPriorityRecord::kind)
            .toMutableList()

        if (preferredKind !in kinds) {
            kinds.add(preferredKind)
        }

        val reorderedKinds = buildList {
            add(preferredKind)
            addAll(kinds.filter { kind -> kind != preferredKind })
        }.distinct()

        editorSessionRepository.replaceKindPriorities(
            tabId = tabId,
            priorities = reorderedKinds.mapIndexed { index, kind ->
                EditorSessionKindPriorityRecord(
                    tabId = tabId,
                    kind = kind,
                    priority = index,
                )
            },
        )

        if (latest.layoutMode != EDITOR_SESSION_LAYOUT_SINGLE) {
            val secondaryKind = reorderedKinds.firstOrNull { kind -> kind != preferredKind }
                ?: oppositeKind(preferredKind)
            applyMixedMode(
                tab = latest,
                leftKind = preferredKind,
                rightKind = secondaryKind,
                exportCodePathForClass = exportCodePathForClass,
            )
        }
    }

    suspend fun ensureClassTab(cls: WorkspaceIndexedClassRecord): EditorSessionTabRecord {
        return editorSessionRepository.getTabByTarget(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
            ?: createClassTabWithoutContents(cls)
    }

    suspend fun ensureTabReadyForNavigationKind(
        tab: EditorSessionTabRecord,
        preferredKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): String? {
        val usableKind = selectUsableKind(
            tab = tab,
            preferredKind = preferredKind,
            exportCodePathForClass = exportCodePathForClass,
        ) ?: return null
        syncTabPaneForKind(tab.tabId, usableKind, exportCodePathForClass)
        return usableKind
    }

    private fun resolveSelectedTabId(
        tabs: List<EditorSessionTabRecord>,
        preferredTabId: String?,
        currentSelectedTabId: String?,
    ): String? {
        if (preferredTabId != null && tabs.any { tab -> tab.tabId == preferredTabId }) {
            return preferredTabId
        }
        if (currentSelectedTabId != null && tabs.any { tab -> tab.tabId == currentSelectedTabId }) {
            return currentSelectedTabId
        }
        return tabs.maxWithOrNull(
            compareBy<EditorSessionTabRecord> { tab -> tab.lastViewedAt }
                .thenBy { tab -> tab.createdAt }
        )?.tabId
    }

    private suspend fun ensureTabContent(
        tab: EditorSessionTabRecord,
        kind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): EditorSessionContentRecord {
        editorSessionRepository.getContent(tab.tabId, kind)?.let { return it }

        val codePath = when (tab.targetType) {
            EDITOR_SESSION_TARGET_TYPE_CLASS -> {
                val cls = workspaceIndexService.findByName(tab.targetKey)
                    ?: throw IllegalStateException("Class not found: ${tab.targetKey}")
                exportCodePathForClass(cls, kind)
            }

            else -> throw IllegalStateException("Unsupported targetType: ${tab.targetType}")
        }

        val created = EditorSessionContentRecord(
            tabId = tab.tabId,
            kind = kind,
            codePath = codePath,
            updatedAt = System.currentTimeMillis(),
        )
        editorSessionRepository.insertContent(created)
        return created
    }

    private suspend fun orderedKinds(tabId: String): List<String> {
        val fromDb = editorSessionRepository.getKindPrioritiesByTabId(tabId)
        val priorities = if (fromDb.isEmpty()) {
            defaultKindPriorities(tabId).also { defaultPriorities ->
                editorSessionRepository.insertKindPriorities(defaultPriorities)
            }
        } else {
            fromDb
        }

        val priorityMap = priorities.associate { priority -> priority.kind to priority.priority }.toMutableMap()
        if (!priorityMap.containsKey(EDITOR_SESSION_KIND_SMALI)) {
            priorityMap[EDITOR_SESSION_KIND_SMALI] = Int.MAX_VALUE - 1
        }
        if (!priorityMap.containsKey(EDITOR_SESSION_KIND_JAVA)) {
            priorityMap[EDITOR_SESSION_KIND_JAVA] = Int.MAX_VALUE
        }

        return priorityMap.keys.sortedWith(
            compareBy<String> { kind -> priorityMap[kind] ?: Int.MAX_VALUE }
                .thenBy { kind -> kind }
        )
    }

    private suspend fun applySingleMode(
        tab: EditorSessionTabRecord,
        kind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        ensureTabContent(tab, kind, exportCodePathForClass)

        editorSessionRepository.replacePanesAndUpdateTab(
            tab = tab.copy(
                layoutMode = EDITOR_SESSION_LAYOUT_SINGLE,
                activePaneIndex = 0,
                activeKind = kind,
                lastViewedAt = System.currentTimeMillis(),
            ),
            panes = listOf(
                EditorSessionPaneRecord(
                    tabId = tab.tabId,
                    paneIndex = 0,
                    kind = kind,
                    weight = 1f,
                )
            ),
        )
    }

    private suspend fun applyMixedMode(
        tab: EditorSessionTabRecord,
        leftKind: String,
        rightKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        ensureTabContent(tab, leftKind, exportCodePathForClass)
        ensureTabContent(tab, rightKind, exportCodePathForClass)

        editorSessionRepository.replacePanesAndUpdateTab(
            tab = tab.copy(
                layoutMode = EDITOR_SESSION_LAYOUT_SPLIT_HORIZONTAL,
                activePaneIndex = 0,
                activeKind = leftKind,
                lastViewedAt = System.currentTimeMillis(),
            ),
            panes = listOf(
                EditorSessionPaneRecord(
                    tabId = tab.tabId,
                    paneIndex = 0,
                    kind = leftKind,
                    weight = 1f,
                ),
                EditorSessionPaneRecord(
                    tabId = tab.tabId,
                    paneIndex = 1,
                    kind = rightKind,
                    weight = 1f,
                ),
            ),
        )
    }

    private suspend fun createClassTabWithoutContents(
        cls: WorkspaceIndexedClassRecord,
    ): EditorSessionTabRecord {
        val now = System.currentTimeMillis()
        val tabId = buildTabId(EDITOR_SESSION_TARGET_TYPE_CLASS, cls.className)
        val initialKind = EDITOR_SESSION_KIND_SMALI
        val tab = EditorSessionTabRecord(
            tabId = tabId,
            targetType = EDITOR_SESSION_TARGET_TYPE_CLASS,
            targetKey = cls.className,
            title = cls.displayName,
            subtitle = cls.className.substringBeforeLast('.', ""),
            layoutMode = EDITOR_SESSION_LAYOUT_SINGLE,
            activePaneIndex = 0,
            activeKind = initialKind,
            createdAt = now,
            lastViewedAt = now,
        )
        editorSessionRepository.insertTabWithInitialState(
            tab = tab,
            contents = emptyList(),
            panes = listOf(
                EditorSessionPaneRecord(
                    tabId = tab.tabId,
                    paneIndex = 0,
                    kind = initialKind,
                    weight = 1f,
                )
            ),
            kindPriorities = defaultKindPriorities(tab.tabId),
        )
        return tab
    }

    private suspend fun selectUsableKind(
        tab: EditorSessionTabRecord,
        preferredKind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ): String? {
        val normalizedPreferred = normalizeNavigationKind(
            targetKind = preferredKind,
            fallbackKind = tab.activeKind,
        )
        val fallbackKind = oppositeKind(normalizedPreferred)
        val candidates = listOf(normalizedPreferred, fallbackKind).distinct()

        for (candidate in candidates) {
            val succeeded = runCatching {
                ensureTabContent(tab, candidate, exportCodePathForClass)
            }.isSuccess
            if (succeeded) return candidate
        }
        return null
    }

    private suspend fun syncTabPaneForKind(
        tabId: String,
        kind: String,
        exportCodePathForClass: ExportCodePathForClass,
    ) {
        val latest = editorSessionRepository.getTabById(tabId) ?: return
        val panes = editorSessionRepository.getPanesByTabId(tabId)
        val matchedPane = panes.firstOrNull { pane -> pane.kind == kind }

        if (matchedPane == null) {
            if (latest.layoutMode == EDITOR_SESSION_LAYOUT_SINGLE) {
                applySingleMode(latest, kind, exportCodePathForClass)
            } else {
                val secondaryKind = panes.firstOrNull { pane -> pane.kind != kind }?.kind ?: oppositeKind(kind)
                applyMixedMode(
                    tab = latest,
                    leftKind = kind,
                    rightKind = secondaryKind,
                    exportCodePathForClass = exportCodePathForClass,
                )
            }
            return
        }

        if (latest.activePaneIndex != matchedPane.paneIndex || latest.activeKind != kind) {
            editorSessionRepository.updateTab(
                latest.copy(
                    activePaneIndex = matchedPane.paneIndex,
                    activeKind = kind,
                    lastViewedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun normalizeNavigationKind(
        targetKind: String,
        fallbackKind: String,
    ): String {
        val normalizedTarget = targetKind.lowercase()
        if (normalizedTarget == EDITOR_SESSION_KIND_JAVA || normalizedTarget == EDITOR_SESSION_KIND_SMALI) {
            return normalizedTarget
        }

        val normalizedFallback = fallbackKind.lowercase()
        if (normalizedFallback == EDITOR_SESSION_KIND_JAVA || normalizedFallback == EDITOR_SESSION_KIND_SMALI) {
            return normalizedFallback
        }
        return EDITOR_SESSION_KIND_SMALI
    }

    private fun oppositeKind(kind: String): String {
        return if (kind == EDITOR_SESSION_KIND_JAVA) {
            EDITOR_SESSION_KIND_SMALI
        } else {
            EDITOR_SESSION_KIND_JAVA
        }
    }

    private fun buildTabId(targetType: String, targetKey: String): String {
        return "$targetType::$targetKey"
    }
}

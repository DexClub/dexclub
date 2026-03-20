package io.github.dexclub.app.res

import java.util.Locale

interface StringRes {
    companion object {
        val current: StringRes
            get() = when (Locale.getDefault()) {
                else -> ZH
            }
    }

    val appName: String

    val deleteWorkspaceMessage: String

    val selectWorkspace: String

    val newWorkspace: String

    val openWorkspace: String

    val workspaceName: String

    val close: String

    val createWorkspace: String

    val projectName: String

    val inputProjectNamePlaceholder: String

    val selectTargetFile: String

    val targetFile: String

    val selectTargetFilePlaceholder: String

    val cancel: String

    val confirm: String

    val delete: String
}

object ZH : StringRes {
    override val appName: String by lazy { "DexClub" }

    override val deleteWorkspaceMessage: String by lazy { "你确定要删除 %s 工作项目吗？" }

    override val selectWorkspace: String by lazy { "选择已有的工作项目" }

    override val newWorkspace: String by lazy { "New Workspace" }

    override val openWorkspace: String by lazy { "Open Workspace" }

    override val workspaceName: String by lazy { "Workspace" }

    override val close: String by lazy { "Close" }

    override val createWorkspace: String by lazy { "新建工作项目" }

    override val projectName: String by lazy { "项目名称" }

    override val inputProjectNamePlaceholder: String by lazy { "请输入项目名称" }

    override val targetFile: String by lazy { "目标文件(apk, dex)" }

    override val selectTargetFilePlaceholder: String by lazy { "请选择目标文件" }

    override val selectTargetFile: String by lazy { "选择目标文件" }

    override val cancel: String by lazy { "取消" }

    override val confirm: String by lazy { "确认" }

    override val delete: String by lazy { "删除" }
}
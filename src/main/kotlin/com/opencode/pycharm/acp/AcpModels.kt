package com.opencode.pycharm.acp

data class AcpRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Map<String, Any?> = emptyMap()
)

data class AcpNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: Map<String, Any?> = emptyMap()
)

data class EditorState(
    val filePath: String,
    val visibleStartLine: Int,
    val visibleEndLine: Int,
    val caretLine: Int,
    val caretColumn: Int,
    val hasSelection: Boolean,
    val selectedText: String?
)

enum class WorkspaceEditType {
    Replace,
    Insert,
    Delete
}

data class WorkspaceEdit(
    val filePath: String,
    val type: WorkspaceEditType,
    val startOffset: Int,
    val endOffset: Int,
    val text: String = ""
)

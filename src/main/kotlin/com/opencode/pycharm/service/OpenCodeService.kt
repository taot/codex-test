package com.opencode.pycharm.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.opencode.pycharm.acp.AcpClient
import com.opencode.pycharm.editor.GhostTextService
import com.opencode.pycharm.editor.virtualFile
import com.opencode.pycharm.acp.EditorState
import com.opencode.pycharm.acp.WorkspaceEdit
import com.opencode.pycharm.acp.WorkspaceEditType
import java.util.concurrent.Executors

@Service(Service.Level.APP)
class OpenCodeService {
    private val logger = Logger.getInstance(OpenCodeService::class.java)
    private val executor = Executors.newSingleThreadExecutor()

    private val acpClient = AcpClient(
        endpoint = "ws://localhost:8080/acp",
        tokenProvider = { System.getenv("OPENCODE_TOKEN") ?: "dev-token" }
    )

    fun connect() {
        acpClient.connect(
            onMessage = { message -> logger.info("ACP message: $message") },
            onClosed = { logger.info("ACP closed") }
        )
    }

    fun disconnect() {
        acpClient.disconnect()
        executor.shutdownNow()
    }

    fun sendStateSync(editor: Editor) {
        val document = editor.document
        val model = editor.scrollingModel.visibleArea
        val startLine = editor.xyToLogicalPosition(model.location).line
        val endLine = editor.xyToLogicalPosition(model.location.apply { y += model.height }).line
        val caret = editor.caretModel.logicalPosition
        val selected = editor.selectionModel.selectedText
        val state = EditorState(
            filePath = editor.virtualFile?.path.orEmpty(),
            visibleStartLine = startLine,
            visibleEndLine = endLine,
            caretLine = caret.line,
            caretColumn = caret.column,
            hasSelection = selected != null,
            selectedText = selected
        )

        acpClient.sendNotification(
            method = "state.sync",
            params = mapOf(
                "filePath" to state.filePath,
                "visibleStartLine" to state.visibleStartLine,
                "visibleEndLine" to state.visibleEndLine,
                "caretLine" to state.caretLine,
                "caretColumn" to state.caretColumn,
                "hasSelection" to state.hasSelection,
                "selectedText" to state.selectedText,
                "documentLength" to document.textLength
            )
        )
    }

    fun readFile(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return String(virtualFile.contentsToByteArray())
    }

    fun listFiles(project: Project, query: String, limit: Int = 200): List<String> {
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilenames(project)
            .asSequence()
            .filter { it.contains(query, ignoreCase = true) }
            .flatMap { name -> FilenameIndex.getVirtualFilesByName(project, name, scope).asSequence() }
            .map(VirtualFile::getPath)
            .distinct()
            .take(limit)
            .toList()
    }

    fun searchWorkspace(project: Project, keyword: String, limit: Int = 100): List<Map<String, Any>> {
        val results = mutableListOf<Map<String, Any>>()
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            if (results.size >= limit) return@forEach
            val text = String(file.contentsToByteArray())
            text.lineSequence().forEachIndexed { idx, line ->
                if (line.contains(keyword, ignoreCase = true) && results.size < limit) {
                    results.add(mapOf("path" to file.path, "line" to idx + 1, "snippet" to line.trim()))
                }
            }
        }
        return results
    }

    fun applyWorkspaceEdits(project: Project, edits: List<WorkspaceEdit>) {
        WriteCommandAction.runWriteCommandAction(project, "OpenCode Apply Workspace Edits", null, Runnable {
            edits.forEach { edit ->
                val file = LocalFileSystem.getInstance().findFileByPath(edit.filePath) ?: return@forEach
                val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file) ?: return@forEach
                when (edit.type) {
                    WorkspaceEditType.Replace -> document.replaceString(edit.startOffset, edit.endOffset, edit.text)
                    WorkspaceEditType.Insert -> document.insertString(edit.startOffset, edit.text)
                    WorkspaceEditType.Delete -> document.deleteString(edit.startOffset, edit.endOffset)
                }
            }
        })
    }

    fun requestInlineEdit(prompt: String, editor: Editor) {
        executor.submit {
            acpClient.sendRequest(
                method = "inline.edit",
                params = mapOf(
                    "prompt" to prompt,
                    "filePath" to editor.virtualFile?.path,
                    "selection" to editor.selectionModel.selectedText
                )
            )
        }
    }

    fun requestGhostText(editor: Editor) {
        executor.submit {
            acpClient.sendRequest(
                method = "completion.ghostText",
                params = mapOf(
                    "filePath" to editor.virtualFile?.path,
                    "caretOffset" to editor.caretModel.offset,
                    "line" to editor.caretModel.logicalPosition.line,
                    "column" to editor.caretModel.logicalPosition.column
                )
            )
            GhostTextService.getInstance().setSuggestion(editor, "/* opencode suggestion */")
        }
    }

    companion object {
        fun getInstance(): OpenCodeService = ApplicationManager.getApplication().getService(OpenCodeService::class.java)
    }
}

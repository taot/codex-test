package com.opencode.pycharm.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor

@Service(Service.Level.APP)
class GhostTextService {
    private val suggestions = mutableMapOf<Editor, String>()

    fun setSuggestion(editor: Editor, suggestion: String) {
        suggestions[editor] = suggestion
    }

    fun getSuggestion(editor: Editor): String? = suggestions[editor]

    fun clear(editor: Editor) {
        suggestions.remove(editor)
    }

    fun acceptAll(editor: Editor) {
        val text = suggestions[editor] ?: return
        val offset = editor.caretModel.offset
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(editor.project) {
            editor.document.insertString(offset, text)
            editor.caretModel.moveToOffset(offset + text.length)
        }
        clear(editor)
    }

    fun acceptNextWord(editor: Editor) {
        val full = suggestions[editor] ?: return
        val next = full.trimStart().split(" ").firstOrNull() ?: return
        val insert = if (full.startsWith(" ")) " $next" else next
        val offset = editor.caretModel.offset
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(editor.project) {
            editor.document.insertString(offset, insert)
            editor.caretModel.moveToOffset(offset + insert.length)
        }
        suggestions[editor] = full.removePrefix(insert)
        if (suggestions[editor].isNullOrEmpty()) clear(editor)
    }

    companion object {
        fun getInstance(): GhostTextService = ApplicationManager.getApplication().getService(GhostTextService::class.java)
    }
}

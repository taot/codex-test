package com.opencode.pycharm.service

import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenCodeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.IO) {
            OpenCodeService.getInstance().connect()
        }

        val multicaster = com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster
        multicaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val editor = event.editor
                if (editor.project == project) {
                    OpenCodeService.getInstance().sendStateSync(editor)
                }
            }
        }, project)

        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val editor = com.intellij.openapi.editor.EditorFactory.getInstance()
                    .getEditors(event.document, project)
                    .firstOrNull() ?: return
                OpenCodeService.getInstance().requestGhostText(editor)
            }
        }, project)
    }
}

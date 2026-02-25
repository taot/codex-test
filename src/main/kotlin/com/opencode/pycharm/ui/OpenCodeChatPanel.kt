package com.opencode.pycharm.ui

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.opencode.pycharm.service.OpenCodeService
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class OpenCodeChatPanel(private val project: Project) : JPanel(BorderLayout(8, 8)) {
    private val history = JBTextArea()
    private val input = JBTextField()

    init {
        border = JBUI.Borders.empty(8)
        history.isEditable = false
        history.lineWrap = true
        history.wrapStyleWord = true

        add(JBScrollPane(history), BorderLayout.CENTER)

        val inputRow = JPanel(BorderLayout(6, 0))
        input.toolTipText = "输入 @ 触发上下文自动补全"
        val send = JButton("Send")
        val apply = JButton("Apply to Editor")

        inputRow.add(input, BorderLayout.CENTER)
        val buttonRow = JPanel(BorderLayout(4, 0))
        buttonRow.add(send, BorderLayout.WEST)
        buttonRow.add(apply, BorderLayout.EAST)
        inputRow.add(buttonRow, BorderLayout.EAST)
        add(inputRow, BorderLayout.SOUTH)

        input.addCaretListener {
            val text = input.text
            if (text.endsWith("@")) {
                showMentionCompletion()
            }
        }

        send.addActionListener {
            val prompt = input.text.trim()
            if (prompt.isBlank()) return@addActionListener
            history.append("\n你：$prompt\n")
            input.text = ""
            OpenCodeService.getInstance().connect()
        }

        apply.addActionListener {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@addActionListener
            val selected = editor.selectionModel.selectedText ?: return@addActionListener
            OpenCodeService.getInstance().requestInlineEdit(
                prompt = "Apply this snippet to editor:\n$selected",
                editor = editor
            )
            history.append("\n[系统] 已发送 Apply to Editor 请求。\n")
        }
    }

    private fun showMentionCompletion() {
        val items = listOf("@current_file", "@directory", "@symbol", "@codebase")
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setItemChosenCallback { picked -> input.text = input.text + picked.removePrefix("@") + " " }
            .createPopup()
            .showUnderneathOf(input)
    }
}

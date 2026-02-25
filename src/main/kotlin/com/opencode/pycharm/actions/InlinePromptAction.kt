package com.opencode.pycharm.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBTextField
import com.opencode.pycharm.service.OpenCodeService
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class InlinePromptAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
            ?: EditorFactory.getInstance().allEditors.firstOrNull { it.project == project }
            ?: return

        val promptField = JBTextField()
        val panel = JPanel(BorderLayout(8, 8))
        panel.add(JLabel("OpenCode 指令（Cmd/Ctrl+Enter 接受，Esc 拒绝）"), BorderLayout.NORTH)
        panel.add(promptField, BorderLayout.CENTER)
        val submit = JButton("发送")
        panel.add(submit, BorderLayout.EAST)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, promptField)
            .setTitle("Inline Edit")
            .setRequestFocus(true)
            .setResizable(false)
            .setMovable(true)
            .createPopup()

        submit.addActionListener {
            val prompt = promptField.text.trim()
            if (prompt.isNotEmpty()) {
                OpenCodeService.getInstance().requestInlineEdit(prompt, editor)
                OpenCodeService.getInstance().sendStateSync(editor)
            }
            popup.cancel()
        }

        popup.showInBestPositionFor(editor)
    }
}

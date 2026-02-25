package com.opencode.pycharm.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.opencode.pycharm.editor.GhostTextService

class AcceptGhostTextAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        GhostTextService.getInstance().acceptAll(editor)
    }
}

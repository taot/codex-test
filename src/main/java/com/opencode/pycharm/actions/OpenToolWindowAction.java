package com.opencode.pycharm.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindowManager;

public class OpenToolWindowAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }
        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OpenCode");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }
}

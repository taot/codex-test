package com.opencode.pycharm.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.opencode.pycharm.services.OpenCodeStateService;

public class InlinePromptAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        var project = e.getProject();
        var editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) {
            return;
        }

        String selection = editor.getSelectionModel().getSelectedText();
        if (selection == null || selection.isBlank()) {
            Messages.showInfoMessage(project, "Please select code first.", "OpenCode");
            return;
        }

        var state = OpenCodeStateService.getInstance(project);
        var conversation = state.getConversations().isEmpty()
                ? state.createConversation("Inline Prompt")
                : state.getConversations().get(0);

        state.appendMessage(conversation.id(), "user", "Please explain/refactor this selection:\n" + selection);
        state.appendMessage(
                conversation.id(),
                "assistant",
                "(Mock) Inline assistant received " + selection.length() + " chars. Connect here to OpenCode API streaming response."
        );

        Messages.showInfoMessage(project, "Selection sent to OpenCode chat.", "OpenCode");
    }
}

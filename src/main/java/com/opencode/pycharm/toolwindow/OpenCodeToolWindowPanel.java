package com.opencode.pycharm.toolwindow;

import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.opencode.pycharm.services.OpenCodeStateService;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class OpenCodeToolWindowPanel extends JPanel {
    private final OpenCodeStateService state;
    private final DefaultListModel<OpenCodeStateService.Conversation> conversationModel = new DefaultListModel<>();
    private final JBList<OpenCodeStateService.Conversation> conversationList = new JBList<>(conversationModel);

    private final JTextPane transcript = new JTextPane();
    private final JBTextArea composer = new JBTextArea(3, 40);

    private final DefaultListModel<String> contextModel = new DefaultListModel<>();
    private final JBList<String> contextList = new JBList<>(contextModel);

    private String selectedConversationId;

    public OpenCodeToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.state = OpenCodeStateService.getInstance(project);

        setBorder(JBUI.Borders.empty(8));
        setupModels();
        add(buildLayout(), BorderLayout.CENTER);
    }

    private void setupModels() {
        for (var conversation : state.getConversations()) {
            conversationModel.addElement(conversation);
        }

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                var c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof OpenCodeStateService.Conversation conv) {
                    setText(conv.title());
                }
                return c;
            }
        });

        conversationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                var selected = conversationList.getSelectedValue();
                selectedConversationId = selected == null ? null : selected.id();
                refreshTranscript();
            }
        });

        if (!conversationModel.isEmpty()) {
            conversationList.setSelectedIndex(0);
            selectedConversationId = conversationModel.getElementAt(0).id();
            refreshTranscript();
        }

        contextModel.addElement("Current file: (auto attach in next iteration)");
        contextModel.addElement("Selection: use 'Ask About Selection' from editor menu");
        contextModel.addElement("Rules: Keep style consistent with project");
    }

    private JComponent buildLayout() {
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(220, 600));
        left.setBorder(BorderFactory.createTitledBorder("Conversations"));
        left.add(new JBScrollPane(conversationList), BorderLayout.CENTER);
        left.add(newConversationButton(), BorderLayout.SOUTH);

        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.setBorder(BorderFactory.createTitledBorder("Chat"));
        middle.add(new JBScrollPane(transcript), BorderLayout.CENTER);
        middle.add(composerPanel(), BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(240, 600));
        right.setBorder(BorderFactory.createTitledBorder("Context"));
        right.add(new JBScrollPane(contextList), BorderLayout.CENTER);

        JBSplitter split = new JBSplitter(false, 0.23f);
        split.setFirstComponent(left);

        JBSplitter inner = new JBSplitter(false, 0.74f);
        inner.setFirstComponent(middle);
        inner.setSecondComponent(right);

        split.setSecondComponent(inner);
        return split;
    }

    private JComponent newConversationButton() {
        JButton button = new JButton("+ New Chat");
        button.addActionListener(event -> {
            String title = JOptionPane.showInputDialog(this, "Conversation title:");
            if (title != null && !title.isBlank()) {
                var conversation = state.createConversation(title);
                conversationModel.add(0, conversation);
                conversationList.setSelectedIndex(0);
            }
        });
        return button;
    }

    private JComponent composerPanel() {
        JButton send = new JButton("Send");
        send.addActionListener(event -> {
            String text = composer.getText().trim();
            if (!text.isEmpty() && selectedConversationId != null) {
                state.appendMessage(selectedConversationId, "user", text);
                state.appendMessage(
                        selectedConversationId,
                        "assistant",
                        "(Mock) I understood your request: '" + text + "'. Next step is wiring this to OpenCode backend API."
                );
                composer.setText("");
                refreshTranscript();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(new JBScrollPane(composer), BorderLayout.CENTER);
        panel.add(send, BorderLayout.EAST);
        return panel;
    }

    private void refreshTranscript() {
        if (selectedConversationId == null) {
            return;
        }
        StringBuilder buffer = new StringBuilder();
        for (var message : state.getMessages(selectedConversationId)) {
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(message.role().toUpperCase()).append(": ").append(message.content());
        }
        transcript.setText(buffer.toString());
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }
}

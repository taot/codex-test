package com.opencode.pycharm.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Service.Level;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(Level.PROJECT)
public final class OpenCodeStateService {
    public record Conversation(String id, String title, LocalDateTime createdAt) {}
    public record ChatMessage(String role, String content, LocalDateTime createdAt) {}

    private final List<Conversation> conversations = new ArrayList<>();
    private final Map<String, List<ChatMessage>> messageStore = new HashMap<>();

    public OpenCodeStateService() {
        var now = LocalDateTime.now();
        var quickStart = new Conversation("quick-start", "Quick Start", now);
        var refactor = new Conversation("refactor-auth", "Refactor auth flow", now);
        conversations.add(quickStart);
        conversations.add(refactor);

        messageStore.put("quick-start", new ArrayList<>(List.of(
                new ChatMessage("assistant", "Hi, I'm OpenCode. I can help explain, edit, and refactor your project.", now)
        )));
        messageStore.put("refactor-auth", new ArrayList<>(List.of(
                new ChatMessage("user", "Can you suggest a safer token refresh strategy?", now)
        )));
    }

    public List<Conversation> getConversations() {
        return List.copyOf(conversations);
    }

    public List<ChatMessage> getMessages(String conversationId) {
        return List.copyOf(messageStore.getOrDefault(conversationId, List.of()));
    }

    public void appendMessage(String conversationId, String role, String content) {
        var messages = messageStore.computeIfAbsent(conversationId, key -> new ArrayList<>());
        messages.add(new ChatMessage(role, content, LocalDateTime.now()));
    }

    public Conversation createConversation(String title) {
        var id = "conv-" + System.nanoTime();
        var conversation = new Conversation(id, title, LocalDateTime.now());
        conversations.add(0, conversation);
        messageStore.put(id, new ArrayList<>(List.of(
                new ChatMessage("assistant", "Started new conversation: " + title, LocalDateTime.now())
        )));
        return conversation;
    }

    public static OpenCodeStateService getInstance(Project project) {
        return ServiceManager.getService(project, OpenCodeStateService.class);
    }
}

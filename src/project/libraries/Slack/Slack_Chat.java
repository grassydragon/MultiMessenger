package project.libraries.Slack;

import project.Chat;
import project.Message;

import java.util.Collections;
import java.util.LinkedList;

public class Slack_Chat implements Chat {

    String title;

    String id;

    LinkedList<Message> messages;

    boolean allMessagesLoaded;

    Slack_Chat(String title, String id) {
        this.title = title;
        this.id = id;
        messages = new LinkedList<>();
        allMessagesLoaded = false;
    }

    @Override
    public String getLibraryName() {
        return Slack_API_Library.getInstance().getName();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public LinkedList<Message> getMessages() {
        return messages;
    }

    @Override
    public Message getFirstMessage() {
        return messages.getFirst();
    }

    @Override
    public Message getLastMessage() {
        return messages.getLast();
    }

    @Override
    public boolean areAllMessagesLoaded() {
        return allMessagesLoaded;
    }

    @Override
    public boolean update() {
        return Slack_API_Library.getInstance().updateChat(this);
    }

    @Override
    public void loadMessages() {
        Slack_API_Library.getInstance().loadChatMessages(this);
    }

    @Override
    public void unloadMessages() {
        Message lastMessage = messages.getLast();
        messages.clear();
        messages.addLast(lastMessage);
        allMessagesLoaded = false;
    }

    @Override
    public void sendMessage(String text) {
        Slack_API_Library.getInstance().sendChatMessage(this, text);
    }

    @Override
    public int compareTo(Chat anotherChat) {
        return messages.getLast().compareTo(anotherChat.getLastMessage());
    }
}

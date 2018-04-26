package project.libraries.VK;

import project.Chat;
import project.Message;

import java.util.LinkedList;

public class VK_Chat implements Chat {

    String title;

    int id;

    LinkedList<Message> messages;

    boolean allMessagesLoaded;

    VK_Chat(String title, int id) {
        this.title = title;
        this.id = id;
        messages = new LinkedList<>();
        allMessagesLoaded = false;
    }

    @Override
    public String getLibraryName() {
        return VK_API_Library.getInstance().getName();
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
        return VK_API_Library.getInstance().updateChat(this);
    }

    @Override
    public void loadMessages() {
        VK_API_Library.getInstance().loadChatMessages(this);
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
        VK_API_Library.getInstance().sendChatMessage(this, text);
    }

    @Override
    public int compareTo(Chat anotherChat) {
        return messages.getLast().compareTo(anotherChat.getLastMessage());
    }
}

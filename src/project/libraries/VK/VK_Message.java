package project.libraries.VK;

import project.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VK_Message implements Message {

    String text;

    Date date;

    int id;

    int userId;

    boolean outgoing;

    VK_Message(String text, Date date, int id, int userId, boolean outgoing) {
        this.text = text;
        this.date = date;
        this.id = id;
        this.userId = userId;
        this.outgoing = outgoing;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public boolean isOutgoing() {
        return outgoing;
    }

    @Override
    public int compareTo(Message anotherMessage) {
        return -date.compareTo(anotherMessage.getDate());
    }

}

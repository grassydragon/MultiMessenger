package project.libraries.Slack;

import project.Message;

import java.util.Date;

public class Slack_Message implements Message {

    String text;

    String timestamp;

    Date date;

    String username;

    boolean outgoing;

    Slack_Message(String text, String timestamp, String username, boolean outgoing) {
        this.text = text;
        this.timestamp = timestamp;
        date = new Date((long) Double.parseDouble(timestamp) * 1000);
        this.username = username;
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

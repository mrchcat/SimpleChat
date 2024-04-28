package utils;

import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final String author;
    private final String body;

    public Message(MessageType type, String author,  String body) {
        this.type = type;
        this.author=author;
        this.body = body;
    }

    public Message(MessageType type) {
        this(type, "","");
    }

    public MessageType getType() {
        return type;
    }

    public String getBody() {
        return body;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public String toString() {
        return type + ": " + body;
    }
}

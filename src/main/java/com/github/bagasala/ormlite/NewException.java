package com.github.bagasala.ormlite;

public class NewException extends RuntimeException{
    private long chat_id;
    public NewException(String message,long chat_id) {
        super(message);
        this.chat_id = chat_id;
    }

    public long getChat_id() {
        return chat_id;
    }
}

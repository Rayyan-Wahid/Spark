package com.example.spark;

public class ChatModel {
    private String name;
    private int age;
    private String lastMessage;
    private int imageResId;
    private boolean unread;

    public ChatModel(String name, int age, String lastMessage, int imageResId, boolean unread) {
        this.name = name;
        this.age = age;
        this.lastMessage = lastMessage;
        this.imageResId = imageResId;
        this.unread = unread;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getLastMessage() { return lastMessage; }
    public int getImageResId() { return imageResId; }
    public boolean isUnread() { return unread; }
}
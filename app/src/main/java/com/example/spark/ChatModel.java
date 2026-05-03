package com.example.spark;

public class ChatModel {
    private String name;
    private int age;
    private String lastMessage;
    private String profileImageUrl;
    private boolean unread;
    private String otherUserId;

    public ChatModel() {}

    public ChatModel(String name, int age, String lastMessage, String profileImageUrl, boolean unread, String otherUserId) {
        this.name = name;
        this.age = age;
        this.lastMessage = lastMessage;
        this.profileImageUrl = profileImageUrl;
        this.unread = unread;
        this.otherUserId = otherUserId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isUnread() { return unread; }
    public void setUnread(boolean unread) { this.unread = unread; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }
}

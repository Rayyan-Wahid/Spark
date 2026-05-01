package com.example.spark;

public class ProfileModel {
    private String name;
    private int age;
    private String distance;
    private int imageResId;

    public ProfileModel(String name, int age, String distance, int imageResId) {
        this.name = name;
        this.age = age;
        this.distance = distance;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getDistance() { return distance; }
    public int getImageResId() { return imageResId; }
}
package com.example.spark;

import java.util.List;

public class ProfileModel {

    private String id;
    private String name;
    private int age;
    private String distance;
    private int imageResId;
    private String bio;
    private List<String> interests;
    private boolean isPublic;
    private String profileImageUrl;
    private boolean profileCompleted;
    private List<String> matchedUserIds;
    private List<String> likedByUids; // UIDs of users who liked this user
    private String gender;
    private double latitude;
    private double longitude;

    public ProfileModel() {
        // Required for Firebase
        this.matchedUserIds = new java.util.ArrayList<>();
        this.likedByUids = new java.util.ArrayList<>();
    }

    public ProfileModel(String name, int age, String distance, int imageResId) {
        this.name = name;
        this.age = age;
        this.distance = distance;
        this.imageResId = imageResId;
        this.interests = new java.util.ArrayList<>();
        this.matchedUserIds = new java.util.ArrayList<>();
        this.likedByUids = new java.util.ArrayList<>();
        this.isPublic = true;
        this.profileCompleted = false;
        this.gender = "Prefer not to say";
    }

    public ProfileModel(String id, String name, int age, String bio, List<String> interests, boolean isPublic, String profileImageUrl, String gender) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.interests = interests;
        this.isPublic = isPublic;
        this.profileImageUrl = profileImageUrl;
        this.profileCompleted = true;
        this.matchedUserIds = new java.util.ArrayList<>();
        this.likedByUids = new java.util.ArrayList<>();
        this.gender = gender;
    }

    public List<String> getLikedByUids() { return likedByUids; }
    public void setLikedByUids(List<String> likedByUids) { this.likedByUids = likedByUids; }

    public List<String> getMatchedUserIds() { return matchedUserIds; }
    public void setMatchedUserIds(List<String> matchedUserIds) { this.matchedUserIds = matchedUserIds; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Support both 'id' and 'uid' keys from Firebase
    @com.google.firebase.database.PropertyName("uid")
    public String getUid() { return id; }
    @com.google.firebase.database.PropertyName("uid")
    public void setUid(String uid) { this.id = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }

    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
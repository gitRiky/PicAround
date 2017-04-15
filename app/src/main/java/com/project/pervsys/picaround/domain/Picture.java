package com.project.pervsys.picaround.domain;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Picture {

    private String id;
    private String name;
    private String description;
    private String path;
    private int views;
    private int likes;
    private double popularity;
    private String timestamp;
    private String type;
    private String userId;
    private String username;
    private String userIcon;
    private Place place;
    private HashMap<String,String> likesList;
    private HashMap<String,String> viewsList;

    public Picture(){
        // Default constructor required for calls to DataSnapshot.getValue(Picture.class)
    }

    public Picture(String name, String description, String path, String timestamp, String type,
                   String userId, String username, Place place) {
        this.name = name;
        this.description = description;
        this.path = path;
        this.timestamp = timestamp;
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.place = place;
        this.likesList = new HashMap<>();
        this.viewsList = new HashMap<>();
    }


    //TODO: remove this constructor
    // temp constructor for simulating the upload of a picture
    public Picture(String name, String description, String path, String userId, String username, String userIcon){
        this.name = name;
        this.description = description;
        this.path = path;
        this.userId = userId;
        this.username = username;
        this.userIcon = userIcon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public String getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(String userIcon) {
        this.userIcon = userIcon;
    }

    public HashMap<String, String> getLikesList() {
        return likesList;
    }

    public void setLikesList(HashMap<String, String> likesList) {
        this.likesList = likesList;
    }

    public HashMap<String, String> getViewsList() {
        return viewsList;
    }

    public void setViewsList(HashMap<String, String> viewsList) {
        this.viewsList = viewsList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Picture picture = (Picture) o;

        return id.equals(picture.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Picture{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", path='" + path + '\'' +
                ", views=" + views +
                ", likes=" + likes +
                ", popularity=" + popularity +
                ", timestamp='" + timestamp + '\'' +
                ", type='" + type + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", userIcon='" + userIcon + '\'' +
                ", place=" + place + '\'' +
                ", likesList=" + likesList + '\'' +
                ", viewsList=" + viewsList +
                '}';
    }
}

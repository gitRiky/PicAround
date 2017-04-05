package com.project.pervsys.picaround.domain;

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
    private Place place;

    public Picture(){

    }

    public Picture(String id, String name, String description, String path, int views, int likes,
                   double popularity, String timestamp, String type, String userId, String username, Place place) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.path = path;
        this.views = views;
        this.likes = likes;
        this.popularity = popularity;
        this.timestamp = timestamp;
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.place = place;
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
                ", place=" + place +
                '}';
    }
}

package com.project.pervsys.picaround.domain;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Picture implements Parcelable {

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
    private String pointId;
    private boolean inPlace;
    private HashMap<String,String> likesList;
    private HashMap<String,String> viewsList;


    public Picture(){
        // Default constructor required for calls to DataSnapshot.getValue(Picture.class)
    }

    public Picture(String name, String description, String path, String userId, String username, String userIcon, String pointId){

        this.name = name;
        this.description = description;
        this.path = path;
        this.userId = userId;
        this.username = username;
        this.userIcon = userIcon;
        this.pointId = pointId;
        this.inPlace = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPointId(String pointId){
        this.pointId = pointId;
    }

    public String getPointId(){
        return pointId;
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

    public void addView(String id){
        if (viewsList == null)
            viewsList = new HashMap<>();
        viewsList.put(id, id);
    }

    public boolean isInPlace(){
        return inPlace;
    }

    public void setLikesList(HashMap<String, String> likesList) {
        this.likesList = likesList;
    }

    public void addLike(String id){
       if (likesList == null)
           likesList = new HashMap<>();
       likesList.put(id, id);
   }

    public void removeLike(String id){
       likesList.remove(id);
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

        return id != null ? id.equals(picture.id) : picture.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
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
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", userIcon='" + userIcon + '\'' +
                ", place=" + place +
                ", pointId='" + pointId + '\'' +
                ", inPlace=" + inPlace +
                ", likesList=" + likesList +
                ", viewsList=" + viewsList +
                '}';
    }

    protected Picture(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        path = in.readString();
        views = in.readInt();
        likes = in.readInt();
        popularity = in.readDouble();
        timestamp = in.readString();
        type = in.readString();
        userId = in.readString();
        username = in.readString();
        userIcon = in.readString();
        place = (Place) in.readValue(Place.class.getClassLoader());
        likesList = (HashMap) in.readValue(HashMap.class.getClassLoader());
        viewsList = (HashMap) in.readValue(HashMap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(path);
        dest.writeInt(views);
        dest.writeInt(likes);
        dest.writeDouble(popularity);
        dest.writeString(timestamp);
        dest.writeString(type);
        dest.writeString(userId);
        dest.writeString(username);
        dest.writeString(userIcon);
        dest.writeValue(place);
        dest.writeValue(likesList);
        dest.writeValue(viewsList);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Picture> CREATOR = new Parcelable.Creator<Picture>() {
        @Override
        public Picture createFromParcel(Parcel in) {
            return new Picture(in);
        }

        @Override
        public Picture[] newArray(int size) {
            return new Picture[size];
        }
    };
}
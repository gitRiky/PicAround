package com.project.pervsys.picaround.domain;

import java.util.LinkedList;
import java.util.List;

public class Point {

    private String id;
    private String name;
    private double lat;
    private double lon;
    private String description;
    private String icon;
    private String category;
    private String type;
    private List<Integer> places;
    private List<Picture> pictures;

    public Point(){
        // Default constructor required for calls to DataSnapshot.getValue(Point.class)
    }

    public Point(String name, double lat, double lon, String description, String icon, String category, String type) {
        this.id = null;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.description = description;
        this.type = type;
        this.icon = icon;
        this.category = category;
        this.places = new LinkedList<>();
        this.pictures = new LinkedList<>();
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

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Integer> getPlaces() {
        return places;
    }

    public void setPlaces(List<Integer> places) {
        this.places = places;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        return id.equals(point.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Point{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", category='" + category + '\'' +
                ", type='" + type + '\'' +
                ", places=" + places +
                ", pictures=" + pictures +
                '}';
    }
}

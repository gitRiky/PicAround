package com.project.pervsys.picaround.domain;

public class Place {

    private String id;
    private String address;
    private double lat;
    private double lon;
    private String pointId;

    public Place(){
        // Default constructor required for calls to DataSnapshot.getValue(Place.class)
    }

    public Place(String id, String address, double lat, double lon, String pointId) {
        this.id = id;
        this.address = address;
        this.lat = lat;
        this.lon = lon;
        this.pointId = pointId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Place place = (Place) o;

        return id.equals(place.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Place{" +
                "id='" + id + '\'' +
                ", address='" + address + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", pointId='" + pointId + '\'' +
                '}';
    }
}

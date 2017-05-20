package com.project.pervsys.picaround.utility;


import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.project.pervsys.picaround.domain.Point;

public class MarkerClusterItem implements ClusterItem {

    private LatLng mPosition;
    private String mTitle;
    private String mSnippet;
    private Point point;

    public MarkerClusterItem(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
    }

    public MarkerClusterItem(double lat, double lng, String title, String snippet) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
    }

    @Override
    public String toString(){
        return point.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarkerClusterItem that = (MarkerClusterItem) o;

        return point != null ? point.equals(that.point) : that.point == null;

    }

    @Override
    public int hashCode() {
        return point != null ? point.hashCode() : 0;
    }

    public void setPoint(Point o){
        point = o;
    }

    public Point getPoint(){
        return point;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }
}

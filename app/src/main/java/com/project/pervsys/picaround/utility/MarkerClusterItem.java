package com.project.pervsys.picaround.utility;


import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.project.pervsys.picaround.domain.Point;

public class MarkerClusterItem implements ClusterItem {

    private LatLng mPosition;
    private String mTitle;
    private String mSnippet;
    private Point mPoint;
    private BitmapDescriptor mIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

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
        return mPoint.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MarkerClusterItem that = (MarkerClusterItem) o;

        return mPoint != null ? mPoint.equals(that.mPoint) : that.mPoint == null;

    }

    @Override
    public int hashCode() {
        return mPoint != null ? mPoint.hashCode() : 0;
    }

    public void setmPoint(Point o){
        mPoint = o;
    }

    public Point getmPoint(){
        return mPoint;
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

    public BitmapDescriptor getIcon(){
        //TODO: change this from static to dynamic loading of icons
        return mIcon;
    }

    public void setIcon(int resource){
        mIcon = BitmapDescriptorFactory.fromResource(resource);
    }
}

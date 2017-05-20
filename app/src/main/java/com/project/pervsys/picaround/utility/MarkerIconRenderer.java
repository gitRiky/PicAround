package com.project.pervsys.picaround.utility;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Created by federico on 20/05/17.
 */

public class MarkerIconRenderer extends DefaultClusterRenderer<MarkerClusterItem> {

    public MarkerIconRenderer(Context context, GoogleMap map, ClusterManager<MarkerClusterItem> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(MarkerClusterItem item, MarkerOptions markerOptions) {
        markerOptions.icon(item.getIcon());
        markerOptions.snippet(item.getSnippet());
        markerOptions.title(item.getTitle());
        super.onBeforeClusterItemRendered(item, markerOptions);
    }
}

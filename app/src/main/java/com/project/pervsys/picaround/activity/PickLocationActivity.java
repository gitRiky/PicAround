package com.project.pervsys.picaround.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.claudiodegio.msv.OnSearchViewListener;
import com.claudiodegio.msv.SuggestionMaterialSearchView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.project.pervsys.picaround.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.project.pervsys.picaround.utility.Config.LOCATION_EXTRA;
import static com.project.pervsys.picaround.utility.Config.NUM_SUGGESTED_ADDRESSES;
import static com.project.pervsys.picaround.utility.Config.SHARED_MAP_POSITION;

public class PickLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FloatingActionButton mPickLocation;
    private CameraPosition mCameraPosition;
    private SuggestionMaterialSearchView mSearchView;


    Button.OnClickListener mPickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePickLocationSelected();
                }
            };

    private void handlePickLocationSelected() {
        if (mMap != null) {
            LatLng selectedLatLng = mMap.getCameraPosition().target;
            String location = selectedLatLng.toString();
            Intent data = new Intent();
            data.putExtra(LOCATION_EXTRA, location);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        // Set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        mPickLocation = (FloatingActionButton) findViewById(R.id.pick_location_button);

        // Load map configurations, if available
        SharedPreferences settings = getSharedPreferences(SHARED_MAP_POSITION, 0);
        double latitude = Double.parseDouble(settings.getString("latitude", "0"));
        double longitude = Double.parseDouble(settings.getString("longitude", "0"));
        float zoom = Float.parseFloat(settings.getString("zoom", "0"));

        mSearchView = (SuggestionMaterialSearchView) findViewById(R.id.sv);
        mSearchView.setOnSearchViewListener(new OnSearchViewListener() {
            @Override
            public void onSearchViewShown() {
                mPickLocation.setVisibility(View.INVISIBLE);
                findViewById(R.id.marker_centered).setVisibility(View.INVISIBLE);
            }

            @Override
            public void onSearchViewClosed() {
                mPickLocation.setVisibility(View.VISIBLE);
                findViewById(R.id.marker_centered).setVisibility(View.VISIBLE);
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
                try
                {
                    List<Address> addresses = geoCoder.getFromLocationName(s, NUM_SUGGESTED_ADDRESSES);
                    if (addresses.size() > 0)
                    {
                        Double lat = addresses.get(0).getLatitude();
                        Double lon = addresses.get(0).getLongitude();

                        Log.d("lat-long", "" + lat + "......." + lon);
                        final LatLng address = new LatLng(lat, lon);

                        // Move the camera instantly with a zoom of 15.
                        if(mMap != null) {
                            CameraPosition posAddr = new CameraPosition.Builder()
                                    .target(address)
                                    .zoom(15)
                                    .build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(posAddr));
                        }
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                mSearchView.closeSearch();
                return true;
            }

            @Override
            public void onQueryTextChange(String s) {
//                Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
//                try
//                {
//                    String[] adds = new String[NUM_SUGGESTED_ADDRESSES];
//                    List<Address> addresses = geoCoder.getFromLocationName(s, NUM_SUGGESTED_ADDRESSES);
//
//                    int i = 0;
//                    for(Address addr : addresses){
//                        adds[i] = addr.getLocality() + ", " + addr.getCountryName();
//                        i++;
//                    }
//
//                    SuggestionMaterialSearchView sugg = (SuggestionMaterialSearchView) mSearchView;
//                    sugg.setSuggestion(adds, true);
//                }
//                catch (IOException e)
//                {
//                    e.printStackTrace();
//                }
            }
        });

        LatLng startPosition = new LatLng(latitude, longitude);

        mCameraPosition = new CameraPosition.Builder()
                .target(startPosition)
                .zoom(zoom)
                .build();                   // Creates a CameraPosition from the builder
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Restore previous configurations of the map, if available
        if (mCameraPosition != null)
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Nothing to do if permissions are not granted
        }
        else{
            mMap.setMyLocationEnabled(true);
        }
        mPickLocation.setOnClickListener(mPickListener);
    }

    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pick_location_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        mSearchView.setMenuItem(item);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed(){
        if (mSearchView.isOpen()) {
            mSearchView.closeSearch();
        }
        else{
            super.onBackPressed();
        }
    }
}

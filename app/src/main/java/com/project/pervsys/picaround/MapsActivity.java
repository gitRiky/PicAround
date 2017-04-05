package com.project.pervsys.picaround;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Place;
import com.project.pervsys.picaround.domain.Point;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.utility.Config;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements LocationListener,OnMapReadyCallback, OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.InfoWindowAdapter{

    private static final LatLng PERTH = new LatLng(-31.952854, 115.857342);
    private static final LatLng SYDNEY = new LatLng(-33.87365, 151.20689);
    private static final LatLng BRISBANE = new LatLng(-27.47093, 153.0235);
    private static final LatLng ROME = new LatLng(41.890635, 12.490726);

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_UPLOAD_PHOTO = 2;
    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String TAG = "MapsActivity";
    private static final String FIRST_TIME_INFOWINDOW = "FirstTime";
    private static final String PHOTO_PATH = "photoPath";

    private GoogleMap mMap;
    private Marker mRome;
    private JSONArray listOfPoints = null;
    private Marker mPerth;
    private ImageView mImageView;

    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    private LocationManager locationManager = null;
    private String provider;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mDatabaseRef = null;

    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            System.out.println("StorageDir: " + storageDir.toString());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        Log.d(getAlbumName(), "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        System.out.println("ALBUM: " + albumF);
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap*/
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);


        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(View.VISIBLE);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch(actionCode) {
            case REQUEST_TAKE_PHOTO:
                File f = null;

                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void handleBigCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            Log.i(TAG, "The photo has been taken");
            setPic();
            Log.i(TAG, "Set pic ok");
            galleryAddPic();
            Log.i(TAG, "Gallery pic ok");
            //Start the UploadPhotoActivity, passing the photo's path
            Intent i = new Intent(this, UploadPhotoActivity.class);
            i.putExtra(PHOTO_PATH, mCurrentPhotoPath);
            Log.i(TAG, "Starting Upload activity");
            startActivityForResult(i, REQUEST_UPLOAD_PHOTO);
            mCurrentPhotoPath = null;
        }

    }

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(REQUEST_TAKE_PHOTO);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Set toolbar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        // Firebase authentication
        final String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.i(TAG, "Logged with Firebase, UID: " + user.getUid());
        }
        else {
            Log.i(TAG, "Not logged with Firebase");
        }
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };


        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageBitmap = null;

        mAlbumStorageDirFactory = new AlbumStorageDirFactory();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        setBtnListenerOrDisable(
                fab,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        mAlbumStorageDirFactory = new AlbumStorageDirFactory();

        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        }
        else {
            Toast.makeText(this,"Location not available",Toast.LENGTH_SHORT).show();
        }

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    /* Remove the location listener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit().
                putString(Config.LOG_PREF_INFO, null).apply();
        ApplicationClass.setGoogleApiClient(null);
        ApplicationClass.setGoogleSignInResult(null);
        Log.i(TAG, "onDestroy");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO: {
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            } // REQUEST_TAKE_PHOTO
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null) );
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(
                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
                        ImageView.VISIBLE : ImageView.INVISIBLE
        );
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action The Intent action to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(
            FloatingActionButton btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        if (isIntentAvailable(this, intentName)) {
            btn.setOnClickListener(onClickListener);
        } else {
            Toast.makeText(this, "ERROR related to " + btn.toString(),Toast.LENGTH_SHORT).show();
            btn.setClickable(false);
        }
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

        setupGPS();
        //TODO: maybe it's a good idea to start an AsyncTask to pull data from firebase
        populatePoints();

        mMap = googleMap;
        mMap.setMyLocationEnabled(true);


        // Create points from data
        Point point = new Point(
                "478765", "Colosseum", 41.890638, 12.49075,
                "Monumental 3-tiered roman amphitheater once used for gladiatorial games, with guided tour option.",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/icon.png?alt=media&token=bcbbf65e-e6f9-4974-95c7-83486cc8f4fc",
                "Historical Landmark", "normal");

        Picture pic1 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c01.jpg?alt=media&token=3864cad4-ca38-4ac4-90eb-4022fa5b58e3",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));
        Picture pic2 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c02.jpg?alt=media&token=74823bd1-2796-4f96-b146-a930532958fb",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));
        Picture pic3 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c03.jpg?alt=media&token=1fc22b30-f271-4822-b0a8-8091f6796b1e",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));
        Picture pic4 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c04.jpg?alt=media&token=04eb6baf-0d4f-4b9f-a7b7-c8e031899401",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));
        Picture pic5 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c05.jpg?alt=media&token=5cd84317-ee22-4f58-b93d-32f49f6da033",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));
        Picture pic6 = new Picture("42131","pic1","Colosseum at sunset",
                "https://firebasestorage.googleapis.com/v0/b/picaround-34ab3.appspot.com/o/c06.jpg?alt=media&token=ea713bf2-d258-467b-8b0b-3cd8ad42ae72",
                123,41,0.36,"4561176412","artistic","657314","dbern",
                new Place("43254","Via dei Fori Imperiali 86",42.062131,12.999619,"478765"));

        point.getPictures().add(pic1);
        point.getPictures().add(pic2);
        point.getPictures().add(pic3);
        point.getPictures().add(pic4);
        point.getPictures().add(pic5);
        point.getPictures().add(pic6);


        LatLng pointPosition = new LatLng(point.getLat(),point.getLon());

        // Add some markers to the map, and add a data object to each marker.
        mRome = mMap.addMarker(new MarkerOptions()
                .position(pointPosition)
                .title(point.getName())
                .snippet(FIRST_TIME_INFOWINDOW) // Value is not relevant, it is used only for distinguishing from null
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_red_24dp)));
        mRome.setTag(point);


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(pointPosition)      // Sets the center of the map to the point position
                .zoom(16)                   // Sets the zoom
                .build();                   // Creates a CameraPosition from the builder
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);

        // Set a listener for infoWindow click.
        mMap.setOnInfoWindowClickListener(this);

        // Set InfoWindowAdapter
        mMap.setInfoWindowAdapter(this);
    }

    private void populatePoints() {
        // get all the points
        //mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child("points")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // each child is a single point
                        for(DataSnapshot child : dataSnapshot.getChildren()){
                            try {
                                Map<String, String> point = (Map<String, String>) child.getValue();
                                JSONObject jsonPoint = new JSONObject(point);
                                String lat = jsonPoint.getString("lat");
                                String lon = jsonPoint.getString("lon");
                                mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(lat), Double.parseDouble(lon))));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    private void setupGPS() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        if (!enabled) {
            new AlertDialog.Builder(this)
                    .setTitle("GPS enabling")
                    .setMessage("It seems your GPS is turned off. Would you like to turn it ON?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with GPS activation
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Calculate required horizontal shift for current screen density
        final int dX = getResources().getDimensionPixelSize(R.dimen.map_dx);
        // Calculate required vertical shift for current screen density
        final int dY = getResources().getDimensionPixelSize(R.dimen.map_dy);
        final Projection projection = mMap.getProjection();
        final android.graphics.Point markerPoint = projection.toScreenLocation(marker.getPosition());
        // Shift the point we will use to center the map
        markerPoint.offset(dX, dY);
        final LatLng newLatLng = projection.fromScreenLocation(markerPoint);
        // Smoothly move camera
        mMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));

        mMap.getUiSettings().setMapToolbarEnabled(true);

        marker.showInfoWindow();

        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //TODO: start PointActivity
        String toShow = marker.getPosition().toString();
        Toast.makeText(this, toShow,Toast.LENGTH_SHORT).show();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // First opening the infoWindow: loading
        if (marker.getSnippet() != null) {
            View loadingView = getLayoutInflater().inflate(R.layout.basic_loading_info_window, null);

            View v = getLayoutInflater().inflate(R.layout.info_window, null);

            ImageView icon = (ImageView) v.findViewById(R.id.info_icon);
            GridLayout gridLayout = (GridLayout) v.findViewById(R.id.info_pictures);

            Point point = (Point) marker.getTag();

            Picasso.with(this)
                    .load(point.getIcon())
                    .into(icon, new MarkerCallback(marker));

            addPictures(marker, point, gridLayout);

            return loadingView;
        }
        // Second opening of the infoWindow: show info window
        else {
            View v = getLayoutInflater().inflate(R.layout.info_window, null);

            ImageView icon = (ImageView) v.findViewById(R.id.info_icon);
            TextView title = (TextView) v.findViewById(R.id.info_title);
            TextView category = (TextView) v.findViewById(R.id.info_category);
            GridLayout gridLayout = (GridLayout) v.findViewById(R.id.info_pictures);
            TextView description = (TextView) v.findViewById(R.id.info_description);

            Point point = (Point) marker.getTag();

            title.setText(point.getName());
            category.setText(point.getCategory());
            description.setText(point.getDescription());

            String iconPath = point.getIcon();
            Picasso.with(this)
                    .load(iconPath)
                    .into(icon, new MarkerCallback(marker));

            addPictures(marker, point, gridLayout);

            return v;
        }
    }

    private void addPictures(Marker marker, Point point, GridLayout gridLayout) {
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, this.getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, this.getResources().getDisplayMetrics());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);

        List<Picture> pictures = point.getPictures();
        for (Picture pic : pictures) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            gridLayout.addView(imageView);

            String path = pic.getPath();

            Picasso.with(this)
                    .load(path)
                    .into(imageView, new MarkerCallback(marker));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        int lat = (int) (location.getLatitude());
        int lng = (int) (location.getLongitude());
        if(mMap != null){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat,lng)));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
        Log.e(TAG, "LOGGED WITH " + logged);
        //if the user is not logged, then add login to the menu
        if(logged != null && !logged.equals(Config.NOT_LOGGED))
            menu.add(R.string.logout);
            //if the user is logged, then add logout to the menu
        else
            menu.add(R.string.login);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                Log.i(TAG, "Settings has been selected");
                Toast.makeText(this, "Selected settings", Toast.LENGTH_SHORT).show();
                //Settings activity
                return true;
            case R.id.contact:
                Log.i(TAG, "Contact has been selected");
                Toast.makeText(this, "Selected contact", Toast.LENGTH_SHORT).show();
                //Contact activity
                return true;
            case R.id.help:
                Log.i(TAG, "Help has been selected");
                Toast.makeText(this, "Selected help", Toast.LENGTH_SHORT).show();
                //Help activity
                return true;
            case R.id.info:
                //TODO: I'm using this item for starting my UploadPhotoActivity
                Log.i(TAG, "Info has been selected");
                Toast.makeText(this, "Selected info", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, UploadPhotoActivity.class);
                startActivity(i);
                //Info activity
                return true;
            case R.id.profile:
                Log.i(TAG, "Profile has been selected");
                Toast.makeText(this, "Selected profile", Toast.LENGTH_SHORT).show();
                //Profile activity
                return true;
            case R.id.search:
                Log.i(TAG, "Search has been selected");
                Toast.makeText(this, "Selected search", Toast.LENGTH_SHORT).show();
                //Search activity
                return true;
            default:
                String title = (String) item.getTitle();
                if (title.equals(getResources().getString(R.string.login))) {
                    Log.i(TAG, "Login has been selected");
                    Toast.makeText(this, "Selected login", Toast.LENGTH_SHORT).show();
                    getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                            .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
                    startLogin();
                    return true;
                } else {
                    Log.i(TAG, "Logout has been selected");
                    Toast.makeText(this, "Selected logout", Toast.LENGTH_SHORT).show();
                    prepareLogOut();
                }
        }
        return false;
    }

    private void prepareLogOut(){
        mGoogleApiClient = ApplicationClass.getGoogleApiClient();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logOut();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
        dialog.show();
    }

    private void logOut(){
        FirebaseAuth.getInstance().signOut();
        // logout Facebook
        if (Profile.getCurrentProfile() != null){
            LoginManager.getInstance().logOut();
            Log.i(TAG, "Logout from Facebook");
            getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                    .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
        }
        String logged = getSharedPreferences(Config.LOG_PREFERENCES,MODE_PRIVATE)
                .getString(Config.LOG_PREF_INFO,null);
        //logout Google
        if (logged != null){
            if (logged.equals(Config.GOOGLE_LOGGED)) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Logout from Google");
                                    getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                                            .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
                                    ApplicationClass.setGoogleApiClient(null);
                                    startLogin();
                                } else
                                    Log.e(TAG, "Error during the Google logout");
                            }
                        });
            }
        }
        startLogin();
    }

    private void startLogin(){
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

}

class MarkerCallback implements Callback {

    private static final int THUMBNAILS_NUMBER = 7;
    private static int counter = 0;

    Marker marker=null;

    MarkerCallback(Marker marker) {
        this.marker=marker;
    }

    @Override
    public void onError() {
        Log.e(getClass().getSimpleName(), "Error loading thumbnail!");
    }

    @Override
    public void onSuccess() {
        counter++;
        if (counter == THUMBNAILS_NUMBER && marker != null && marker.isInfoWindowShown()) {
            marker.setSnippet(null);
            marker.hideInfoWindow();
            marker.showInfoWindow();
        }
    }
}
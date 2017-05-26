package com.project.pervsys.picaround.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.claudiodegio.msv.OnSearchViewListener;
import com.claudiodegio.msv.SuggestionMaterialSearchView;
import com.claudiodegio.msv.adapter.SearchSuggestRvAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.database.Query;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.project.pervsys.picaround.R;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Point;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.User;
import com.project.pervsys.picaround.localDatabase.DBManager;
import com.project.pervsys.picaround.utility.InfoWindowView;
import com.project.pervsys.picaround.utility.MarkerClusterItem;
import com.project.pervsys.picaround.utility.MarkerIconRenderer;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import static com.project.pervsys.picaround.utility.Config.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.project.pervsys.picaround.utility.Config.SHARED_MAP_POSITION;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, GoogleMap.InfoWindowAdapter {


    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String IMAGE_TYPE = "image/*";
    private static final String TAG = "MapsActivity";
    private static final String FIRST_TIME_INFOWINDOW = "FirstTime";
    private static final int MIN_TIME_LOCATION_UPDATE = 400;
    private static final int MIN_DISTANCE_LOCATION_UPDATE = 1000;

    private ProgressDialog progress;
    private GoogleMap mMap;

    private SlidingUpPanelLayout mSlidingUpPanel;
    private SuggestionMaterialSearchView mSearchView;

    private DBManager mDbManager;
    private ArrayList<String> mUsernames;
    private SearchAdapter mAdapter;

    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    private LocationManager mLocationManager = null;
    private String mProvider;

    private String mUsername;
    private String mProfilePicture;
    private FloatingActionMenu mFloatingActionMenu;

    private FirebaseUser mUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mDatabaseRef = null;
    private CameraPosition mCameraPosition;
    private InfoWindowView mInfoWindow = null;
    private Point mLastPoint;
    private ClusterManager<MarkerClusterItem> mClusterManager;

    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            System.out.println("StorageDir: " + storageDir.toString());
            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
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


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        Log.i(TAG, "Photo added in gallery");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent(int actionCode) {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            switch (actionCode) {
                case REQUEST_TAKE_PHOTO:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    } else {
                        File f = null;
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            f = setUpPhotoFile();
                            mCurrentPhotoPath = f.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                        } catch (IOException e) {
                            e.printStackTrace();
                            f = null;
                            mCurrentPhotoPath = null;
                        }

                        startActivityForResult(takePictureIntent, actionCode);
                    }
                    break;
                default:
                    break;
            } // switch
        } else {
            // user not logged
            AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this)
                    .setTitle(R.string.login_required)
                    .setMessage(R.string.login_for_upload)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startLogin();
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing
                        }
                    });
            dialog.show();
        }
    }

    private void handleBigCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            Log.i(TAG, "The photo has been taken");
            galleryAddPic();
            boolean fromCamera = true;
            //Start the UploadPhotoActivity, passing the photo's path
            startUploadPhotoActivity(fromCamera);
        }

    }

    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFloatingActionMenu.isOpened())
                        mFloatingActionMenu.close(false);
                    dispatchTakePictureIntent(REQUEST_TAKE_PHOTO);
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

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

        // Firebase authentication
        final String logged = getSharedPreferences(LOG_PREFERENCES, 0)
                .getString(LOG_PREF_INFO, null);
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            Log.i(TAG, "Logged with Firebase, UID: " + mUser.getUid());
        } else {
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
                // Recreate menu in order to change "Login" to "Logout"
                invalidateOptionsMenu();
            }
        };

        mDbManager = new DBManager(MapsActivity.this);

        mSearchView = (SuggestionMaterialSearchView) findViewById(R.id.sv);
        mSearchView.setOnSearchViewListener(new OnSearchViewListener() {
            @Override
            public void onSearchViewShown() {
                populateUsernames();
                mFloatingActionMenu.hideMenuButton(true);
            }

            @Override
            public void onSearchViewClosed() {
                mFloatingActionMenu.showMenuButton(true);
                mUsernames.clear();
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                if (mUsernames.contains(s)) {
                    Intent intent = new Intent(MapsActivity.this, UserActivity.class);
                    intent.putExtra(USERNAME, s);
                    startActivity(intent);
                    mSearchView.closeSearch();
                }
                else {
                    Toast.makeText(MapsActivity.this, R.string.no_users_found, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onQueryTextChange(String s) {
            }
        });

        mUsernames = new ArrayList<>();

        // Set the Sliding up panel
        setSlidingUpPanel();

        // Set file settings
        mAlbumStorageDirFactory = new AlbumStorageDirFactory();

        mFloatingActionMenu = (FloatingActionMenu) findViewById(R.id.menu);
        mFloatingActionMenu.setClosedOnTouchOutside(true);

        mFloatingActionMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUser = mAuth.getCurrentUser();
                if (mUser == null) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this)
                            .setTitle(R.string.login_required)
                            .setMessage(R.string.login_for_upload)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    startLogin();
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //do nothing
                                }
                            });
                    dialog.show();
                    if (mFloatingActionMenu.isOpened())
                        mFloatingActionMenu.close(true);
                }
                else if (mFloatingActionMenu.isOpened())
                    mFloatingActionMenu.close(true);
                else
                    mFloatingActionMenu.open(true);
            }
        });

        FloatingActionButton cameraButton = (FloatingActionButton) findViewById(R.id.menu_item_camera);
        FloatingActionButton galleryButton = (FloatingActionButton) findViewById(R.id.menu_item_gallery);
        setBtnListenerOrDisable(
                cameraButton,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFloatingActionMenu.isOpened())
                    mFloatingActionMenu.close(false);
                selectPicture();
            }
        });

        // Get the location manager
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        //Obtain the mUsername
        if (mUser != null) {
            //first usage, not query the db
            String passedUsername = getIntent().getStringExtra(USERNAME);
            if (passedUsername != null){
                mUsername = passedUsername;
                mProfilePicture = getIntent().getStringExtra(PROFILE_PICTURE);
                Log.d(TAG, "First usage, mUsername = " + mUsername + "\nProfile picture :" + mProfilePicture );
            }
            else {
                getProfileInfo();
            }
        }

        // Set the last Map configurations if available

        SharedPreferences settings = getSharedPreferences(SHARED_MAP_POSITION, 0);
        double latitude = Double.parseDouble(settings.getString("latitude", "0"));
        double longitude = Double.parseDouble(settings.getString("longitude", "0"));
        float zoom = Float.parseFloat(settings.getString("zoom", "0"));

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

    private void setSlidingUpPanel() {
        mSlidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }
  
    private void getProfileInfo() {
        mDatabaseRef.child(USERS).orderByKey().equalTo(mUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            if (child != null) {
                                User user = child.getValue(User.class);
                                Log.d(TAG, user.toString());
                                mUsername = user.getUsername();
                                mProfilePicture = user.getProfilePicture();
                                Log.i(TAG, "Username= " + mUsername
                                        + ", profilePicturePath = " + mProfilePicture);
                                if (progress != null)
                                    progress.dismiss();
                            } else
                                Log.e(TAG, "Cannot obtain profile info");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
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
        // TODO: The app executes populatePoints() in onResume() also !
        if (mMap != null)
            populatePoints();

        String newProfilePicturePath = ApplicationClass.getNewProfilePicturePath();
        if (newProfilePicturePath != null){
            Log.i(TAG, "Profile image has been updated");
            mProfilePicture = newProfilePicturePath;
            ApplicationClass.setNewProfilePicturePath(null);
        }
    }

    /* Remove the location listener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save map configurations

        CameraPosition mMyCam = mMap.getCameraPosition();
        String latitude = mMyCam.target.latitude + "";
        String longitude = mMyCam.target.longitude + "";
        String zoom = mMyCam.zoom + "";

        SharedPreferences settings = getSharedPreferences(SHARED_MAP_POSITION, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("latitude", latitude);
        editor.putString("longitude", longitude);
        editor.putString("zoom", zoom);

        editor.apply();

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FirebaseAuth.getInstance().signOut();
        getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit().
                putString(LOG_PREF_INFO, null).apply();
        ApplicationClass.setGoogleApiClient(null);
        ApplicationClass.setGoogleSignInResult(null);
        Log.i(TAG, "onDestroy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) throws SecurityException {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission given");

                    setupGPS(this);
                    mProvider = mLocationManager.getBestProvider(new Criteria(), true);
                    mLocationManager.requestLocationUpdates(mProvider, MIN_TIME_LOCATION_UPDATE, MIN_DISTANCE_LOCATION_UPDATE, this);
                    mMap.setMyLocationEnabled(true);
                }
            }
            break;

            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {

            }
            break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            //upload of a photo taken by the application's camera
            //in this case, if the upload is cancelled, then the image is deleted
            case REQUEST_UPLOAD_PHOTO:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Photo in uploading");
                    String[] latLong = data.getStringExtra(LOCATION_EXTRA).split(",");
                    Double lat = Double.parseDouble(latLong[0]);
                    Double lon = Double.parseDouble(latLong[1]);
                    if(mMap != null){
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
                    }
                }
                if (resultCode == RESULT_CANCELED) {
                    Log.i(TAG, "Photo upload cancelled");
                    File f = new File(mCurrentPhotoPath);
                    f.delete();
                    deleteFileFromMediaStore(this.getContentResolver(), f);
                    Log.i(TAG, "Image has been deleted");
                }
                mCurrentPhotoPath = null;
                break;
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Photo has been picked");
                    Uri photoUri = data.getData();
                    mCurrentPhotoPath = getRealPathFromURI(this, photoUri);
                    boolean fromCamera = false;
                    startUploadPhotoActivity(fromCamera);
                }
                break;
            //upload of a photo taken from gallery
            //in this case, no deletion needed
            case REQUEST_UPLOAD_PHOTO_FROM_GALLERY:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Photo taken from gallery in uploading");
                    String[] latLong = data.getStringExtra(LOCATION_EXTRA).split(",");
                    Double lat = Double.parseDouble(latLong[0]);
                    Double lon = Double.parseDouble(latLong[1]);
                    if(mMap != null){
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15));
                    }
                }
                if (resultCode == RESULT_CANCELED)
                    Log.i(TAG, "Photo upload cancelled");
                break;
        }
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void startUploadPhotoActivity(boolean fromCamera) {
        Location currentLocation = null;
        Intent i = new Intent(this, UploadPhotoActivity.class);
        i.putExtra(PHOTO_PATH, mCurrentPhotoPath);
        i.putExtra(USERNAME, mUsername);
        i.putExtra(PROFILE_PICTURE, mProfilePicture);
        Log.i(TAG, "Starting Upload activity");
        if (fromCamera) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                currentLocation = mLocationManager.getLastKnownLocation(mProvider);
            }
            if(currentLocation != null){
                Double lat = currentLocation.getLatitude();
                Double lng = currentLocation.getLongitude();
                i.putExtra(LATITUDE, lat);
                i.putExtra(LONGITUDE, lng);
            }
            startActivityForResult(i, REQUEST_UPLOAD_PHOTO);
        }
        else
            startActivityForResult(i, REQUEST_UPLOAD_PHOTO_FROM_GALLERY);
    }

    //method used for deleting the image from gallery
    public static void deleteFileFromMediaStore(final ContentResolver contentResolver, final File file) {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[] {canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
//        mImageView.setImageBitmap(mImageBitmap);
//        mImageView.setVisibility(
//                savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ?
//                        ImageView.VISIBLE : ImageView.INVISIBLE
//        );
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
            Toast.makeText(this, "ERROR related to " + btn.toString(), Toast.LENGTH_SHORT).show();
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

        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(mSlidingUpPanel != null)
                    mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        Location location = null;

        // Restore previous configurations of the map, if available
        if (mCameraPosition != null)
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.i(TAG, "Permission asked");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
        else{
            Log.i(TAG, "Permission not asked");
            setupGPS(this);
            mProvider = mLocationManager.getBestProvider(new Criteria(), true);
            mLocationManager.requestLocationUpdates(mProvider, MIN_TIME_LOCATION_UPDATE, MIN_DISTANCE_LOCATION_UPDATE, this);
            mMap.setMyLocationEnabled(true);
            location = mLocationManager.getLastKnownLocation(mProvider);
        }

        if(location != null){
            Log.i(TAG, "getLastKnownLocation is not null !!");
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16);
            mMap.animateCamera(cameraUpdate);
        }

        //TODO: maybe it's a good idea to start an AsyncTask to pull data from firebase
        setUpClusterer();
        // Set InfoWindowAdapter
//        mMap.setInfoWindowAdapter(this);
    }

    private void showPoint(Point point) {
        populateSlidingPanel(point, this);
        mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        // Reverse geocode the coordinates
        ArrayList<String> address = reverseGeocode(point.getLat(), point.getLon());
        TextView titleTextView = (TextView) findViewById(R.id.marker_title);
        TextView detailsTextView = (TextView) findViewById(R.id.marker_details);
        titleTextView.setText(address.get(0));
        address.remove(0);
        String details = TextUtils.join(", ", address);
        detailsTextView.setText(details);

//        // Calculate required horizontal shift for current screen density
//        final int dX = getResources().getDimensionPixelSize(R.dimen.map_dx);
//        // Calculate required vertical shift for current screen density
//        final int dY = getResources().getDimensionPixelSize(R.dimen.map_dy);
//        final Projection projection = mMap.getProjection();
//        final android.graphics.Point markerPoint = projection.toScreenLocation(new LatLng(point.getLat(), point.getLon()));
//        // Shift the point we will use to center the map
//        markerPoint.offset(dX, dY);
//        final LatLng newLatLng = projection.fromScreenLocation(markerPoint);
//        // Smoothly move camera
//        mMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));
//
//        mMap.getUiSettings().setMapToolbarEnabled(true);
    }

    private void setUpClusterer() {
        if(mMap == null)
            return;

        mClusterManager = new ClusterManager<MarkerClusterItem>(this, mMap);
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MarkerClusterItem>() {
            @Override
            public boolean onClusterItemClick(MarkerClusterItem item) {
                Point p = item.getmPoint();
                showPoint(p);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(item.getPosition(), mMap.getCameraPosition().zoom);
                mMap.animateCamera(cameraUpdate);
                return true;
            }
        });

        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MarkerClusterItem>() {
            @Override
            public boolean onClusterClick(Cluster<MarkerClusterItem> cluster) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), mMap.getCameraPosition().zoom + 2);
                mMap.animateCamera(cameraUpdate);
                return true;
            }
        });

        mClusterManager.setRenderer(new MarkerIconRenderer(this, mMap, mClusterManager));

        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        populatePoints();
    }

    private void populatePoints() {
        // get all the points
        mDatabaseRef.child(POINTS).keepSynced(true);
        mDatabaseRef.child(POINTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // each child is a single point
                        for(DataSnapshot child : dataSnapshot.getChildren()){
                            Point p = child.getValue(Point.class);
                            p.setType(POINT);
                            MarkerClusterItem mci = new MarkerClusterItem(p.getLat(), p.getLon());
                            mci.setmPoint(p);
                            double popularity = 1 - p.getPopularity();

                            if(popularity <= 0.20 )
                                mci.setIcon(R.drawable.marker_blue_popularity);
                            else if(popularity > 0.20 && popularity <= 0.40)
                                mci.setIcon(R.drawable.marker_azure_popularity);
                            else if(popularity > 0.40 && popularity <= 0.60)
                                mci.setIcon(R.drawable.marker_green_popularity);
                            else if(popularity > 0.60 && popularity <= 0.80)
                                mci.setIcon(R.drawable.marker_yellow_popularity);
                            else
                                mci.setIcon(R.drawable.marker_red_popularity);

                            if(p.getId() == null){
                                Log.e(TAG, "ERROR, some point has null ID");
                            }
                            else if(!mClusterManager.getMarkerCollection().getMarkers().contains(mci)) {
//                                Log.i(TAG, "The point " + mci + "has been added");
                                mClusterManager.addItem(mci);
                            }
//                            mMap.addMarker(new MarkerOptions()
//                                    .snippet(FIRST_TIME_INFOWINDOW) // Value is not relevant, it is used only for distinguishing from null
//                                    .position(new LatLng(p.getLat(), p.getLon())))
//                                    .setTag(p);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        // get all the places
        mDatabaseRef.child(PLACES).keepSynced(true);
        mDatabaseRef.child(PLACES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // each child is a single point
                        for(DataSnapshot child : dataSnapshot.getChildren()){
                            Point p = child.getValue(Point.class);
                            p.setType(PLACE);
                            MarkerClusterItem mci = new MarkerClusterItem(p.getLat(), p.getLon());
                            mci.setmPoint(p);

                            double popularity = 1 - p.getPopularity();

                            if(popularity <= 0.20 )
                                mci.setIcon(R.drawable.marker_place_blue);
                            else if(popularity > 0.20 && popularity <= 0.40)
                                mci.setIcon(R.drawable.marker_place_azure);
                            else if(popularity > 0.40 && popularity <= 0.60)
                                mci.setIcon(R.drawable.marker_place_green);
                            else if(popularity > 0.60 && popularity <= 0.80)
                                mci.setIcon(R.drawable.marker_place_yellow);
                            else
                                mci.setIcon(R.drawable.marker_place_red);

                            if(!mClusterManager.getMarkerCollection().getMarkers().contains(mci)) {
//                                Log.i(TAG, "The point " + mci + "has been added");
                                mClusterManager.addItem(mci);
                            }
//                            mMap.addMarker(new MarkerOptions()
//                                    .snippet(FIRST_TIME_INFOWINDOW) // Value is not relevant, it is used only for distinguishing from null
//                                    .position(new LatLng(p.getLat(), p.getLon())))
//                                    .setTag(p);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    private void setupGPS(Context context) {

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    private ArrayList<String> reverseGeocode(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        ArrayList<String> addressFragments = new ArrayList<String>();

        try {
            addresses = geocoder.getFromLocation(
                    lat,
                    lon,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            addressFragments.add(getString(R.string.address_not_found));
            Log.e(TAG, "ERROR in reverseGeocode, IOException");
            return addressFragments;
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, "ERROR in reverseGeocode" + ". " +
                    "Latitude = " + lat +
                    ", Longitude = " +
                    lon);
            addressFragments.add(getString(R.string.address_not_found));
            return addressFragments;
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.e(TAG, "ERROR in reverseGeocode, address not found");
            addressFragments.add(getString(R.string.address_not_found));
        }
        else {
            Address address = addresses.get(0);

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
                Log.d(TAG, "addressLine: " + address.getAddressLine(i));
            }
            Log.i(TAG, "Address Found");
        }
        return addressFragments;
    }

    private void populateSlidingPanel(final Point point, final Context context) {

        final GridView pointPictures = (GridView) findViewById(R.id.pictures_grid);
        final LinkedHashMap<String, Picture> pictures = new LinkedHashMap<>();

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        Query photos;

        if (point.getType().equals(PLACE)){
            databaseRef.child(PLACES).keepSynced(true);
            photos = databaseRef.child(PLACES).child(point.getId()).child(PICTURES);
        }
        else {
            databaseRef.child(POINTS).keepSynced(true);
            photos = databaseRef.child(POINTS).child(point.getId()).child(PICTURES)
                    .orderByChild(POPULARITY);
        }

        photos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot picture : dataSnapshot.getChildren()) {
                    Picture pic = picture.getValue(Picture.class);
                    pictures.put(picture.getKey(), pic);
                }

                TextView pictureNumberTextView = (TextView) findViewById(R.id.picture_number);
                int pictureNumber = pictures.size();
                if (pictureNumber > 1)
                    pictureNumberTextView.setText(pictureNumber + " " + getString(R.string.pictures));
                else
                    pictureNumberTextView.setText(pictureNumber + " " + getString(R.string.picture));

                ImageAdapter adapter = new ImageAdapter(context, pictures);
                pointPictures.setAdapter(adapter);
                pointPictures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                        Picture picture = (Picture) adapterView.getItemAtPosition(position);

                        Log.i(TAG, "Picture: " + picture);

                        // Start PictureSliderActivity
                        Intent i = new Intent(MapsActivity.this, PictureSliderActivity.class);
                        i.putExtra(PICTURES, pictures.values().toArray(new Picture[pictures.size()]));
                        i.putExtra(POSITION, position);
                        startActivity(i);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: what to do here?
            }
        });
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        Point point = (Point) marker.getTag();
        if(mInfoWindow != null && mLastPoint.equals(point)) {
            View toReturn = mInfoWindow;
            mLastPoint = null;
            mInfoWindow = null;
            return toReturn;
        }
        else {
            mLastPoint = point;
            mInfoWindow = new InfoWindowView(this, marker, point);
            View loadingView = getLayoutInflater().inflate(R.layout.basic_loading_info_window, null);
            return loadingView;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16);
        mMap.animateCamera(cameraUpdate);
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new mProvider " + provider,
                Toast.LENGTH_SHORT).show();
        mProvider = mLocationManager.getBestProvider(new Criteria(), true);
        Log.i(TAG, "New provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled mProvider " + provider,
                Toast.LENGTH_SHORT).show();
        mProvider = mLocationManager.getBestProvider(new Criteria(), true);
        Log.i(TAG, "New provider disabled: " + provider);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        String logged = getSharedPreferences(LOG_PREFERENCES, 0)
                .getString(LOG_PREF_INFO, null);

        MenuItem item = menu.findItem(R.id.action_search);
        mSearchView.setMenuItem(item);

        Log.i(TAG, "LOGGED WITH " + logged);

        if(logged != null && !logged.equals(NOT_LOGGED))
            menu.findItem(R.id.logout).setVisible(true);
        else
            menu.findItem(R.id.login).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.help:
                Log.i(TAG, "Help has been selected");
                Toast.makeText(this, "Selected help", Toast.LENGTH_SHORT).show();
                //Help activity
                return true;
            case R.id.info:
                Log.i(TAG, "Info has been selected");
                Toast.makeText(this, "Selected info", Toast.LENGTH_SHORT).show();
                //Info activity
                return true;
            case R.id.profile:
                Log.i(TAG, "Profile has been selected");
                String logType = getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE)
                        .getString(LOG_PREF_INFO, null);
                if (logType != null && !logType.equals(NOT_LOGGED)){
                    Intent i = new Intent(this, ProfileActivity.class);
                    startActivity(i);
                }
                else
                    Toast.makeText(this, R.string.not_logged_mex, Toast.LENGTH_LONG).show();
                return true;
            case R.id.login:
                getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit()
                        .putString(LOG_PREF_INFO, NOT_LOGGED).apply();
                startLogin();
                return true;
            case R.id.user:
                mUser = mAuth.getCurrentUser();
                if (mUser != null) {
                    Intent i = new Intent(this, UserActivity.class);
                    i.putExtra(USER_ID, mUser.getUid());
                    startActivity(i);
                }
                else
                    Toast.makeText(this, R.string.not_logged_mex, Toast.LENGTH_LONG).show();
                return true;
            case R.id.logout:
                Log.i(TAG, "Logout has been selected");
                prepareLogOut();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //start the gallery Intent
    private void selectPicture(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            if (Build.VERSION.SDK_INT <= 19) {
                Intent intent = new Intent();
                intent.setType(IMAGE_TYPE);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.start_gallery_intent_title)), REQUEST_PICK_IMAGE);
            } else if (Build.VERSION.SDK_INT > 19) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.start_gallery_intent_title)), REQUEST_PICK_IMAGE);
            }
        }
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
            getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit()
                    .putString(LOG_PREF_INFO, NOT_LOGGED).apply();
        }
        String logged = getSharedPreferences(LOG_PREFERENCES,MODE_PRIVATE)
                .getString(LOG_PREF_INFO,null);
        //logout Google
        if (logged != null){
            if (logged.equals(GOOGLE_LOGGED)) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Logout from Google");
                                    getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit()
                                            .putString(LOG_PREF_INFO, NOT_LOGGED).apply();
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

    @Override
    public void onBackPressed() {
        if (mFloatingActionMenu.isOpened()){
            mFloatingActionMenu.close(true);
        }
        else if (mSearchView.isOpen()) {
            mSearchView.closeSearch();
        }
        else if (mSlidingUpPanel != null &&
                (mSlidingUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                        mSlidingUpPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED)
                ) {
            mSlidingUpPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
        else  {
            super.onBackPressed();
        }
    }

    private void populateUsernames(){
        mDbManager.dropTable();
        mDbManager.createTable();
        mDatabaseRef.child(USERNAMES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            String username = (String)child.getValue();
                            mDbManager.insert(username);
                        }
                        Cursor result = mDbManager.query();
                        result.moveToFirst();
                        for (int i = 0; i < result.getCount(); i++) {
                            String username = result.getString(result.getColumnIndex(USERNAME));
                            Log.d(TAG, username);
                            result.moveToNext();
                            mUsernames.add(username);
                        }
                        mSearchView.setSuggestAdapter(new SearchSuggestRvAdapter(MapsActivity.this, mUsernames));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    private void search(String text){
        Log.d(TAG, "Usernames before query: " + mUsernames);
        mDbManager.createTable();
        mUsernames.clear();
        Log.d(TAG, "Usernames before query (clear): " + mUsernames);
        Log.d(TAG, "textEmpty="+ text.isEmpty() + ", Text=" + text);
        Log.d(TAG, "------------ QUERY RESULTS:");
        Cursor result = mDbManager.queryLike(text);
//        if (result != null) {
            result.moveToFirst();
            for (int i = 0; i < result.getCount(); i++) {
                String username = result.getString(result.getColumnIndex(USERNAME));
                mUsernames.add(username);
                Log.d(TAG, username);
                result.moveToNext();
            }
            Log.d(TAG, "Usernames after query != null: " + mUsernames);
//        }
//        else {
//            Log.d(TAG, "RESULT NULL");
//            Log.d(TAG, "Usernames after query == null: " + mUsernames);
//        }
        Log.d(TAG, "Usernames after query, before updateList: " + mUsernames);
        mAdapter.updateList(mUsernames, false);
    }

}
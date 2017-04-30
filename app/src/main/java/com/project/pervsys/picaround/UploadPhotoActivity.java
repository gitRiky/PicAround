package com.project.pervsys.picaround;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import com.google.firebase.storage.OnProgressListener;

import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

import static com.project.pervsys.picaround.utility.Config.LOCATION_EXTRA;
import static com.project.pervsys.picaround.utility.Config.PHOTO_PATH;
import static com.project.pervsys.picaround.utility.Config.POINTS;

import id.zelory.compressor.Compressor;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Point;
import static com.project.pervsys.picaround.utility.Config.*;

public class UploadPhotoActivity extends AppCompatActivity {
    private final static String TAG = "UploadPhotoActivity";
    private static final String SEPARATOR = "_";
    private static final int PIC_HOR_RIGHT = 1;
    private static final int PIC_HOR_LEFT = 3;
    private static final int PIC_VER_TOP = 6;
    private static final int PIC_VER_BOTTOM = 8;
    private static final int COMPRESSION_QUALITY = 65;
    private static final int NOT_BAR_SLEEP = 1000; //in milliseconds


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mUser;
    private StorageReference mStorageRef;
    private ImageView mImageView;
    private EditText mDescriptionField;
    private String mPhotoPath;
    private String mDescription;
    private String mUsername;
    private String mPhotoId;
    private String mTimestamp;
    private String mLatitude;
    private String profilePicture;
    private String mLongitude;
    private String mPointId;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private int orientation;
    private Picture picture;
    private int photoW;
    private int photoH;
    private int maxWidth;
    private int maxHeight;
    private long transferredBytes;
    private long totalBytes;
    private boolean inUpload = false;
    private boolean uploadError = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);
        // Set toolbar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.upload_title);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
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
        mUser = mAuth.getCurrentUser();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mPhotoPath = getIntent().getStringExtra(PHOTO_PATH);
        mUsername = getIntent().getStringExtra(USERNAME);
        profilePicture = getIntent().getStringExtra(PROFILE_PICTURE);
        Log.d(TAG, "Started activity, photo's path = " + mPhotoPath);
        mImageView = (ImageView) findViewById(R.id.image_to_upload);
        mDescriptionField = (EditText) findViewById(R.id.photo_description);
        try {
            ExifInterface exif = new ExifInterface(mPhotoPath);
            Log.i(TAG, "The path of the photo is: " + mPhotoPath);
            takeExifInfo(exif);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        }

        //set the image into imageView
        setPic();
        if (mLatitude == null || mLongitude == null ||
                !mLatitude.getClass().equals(Double.class) ||
                !mLongitude.getClass().equals(Double.class)) {
            Log.d(TAG, "Position not available in the metadata");
            Intent pickLocationIntent = new Intent(this, PickLocationActivity.class);
            startActivityForResult(pickLocationIntent, REQUEST_PICK_LOCATION);
        }

        Log.i(TAG, "Photo put into the imageView");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_PICK_LOCATION:
                if(resultCode == RESULT_OK) {
                    String[] latlong = data.getStringExtra(LOCATION_EXTRA).split(",");
                    if (mLatitude == null && mLongitude == null) {
                        mLatitude = latlong[0].substring(10);
                        mLongitude = latlong[1].replace(")", "");
                        Log.d(TAG, "FROM pickLocation Activity: -> Timestamp = " + mTimestamp + " lat = " + mLatitude + " long = " + mLongitude);
                    }
                }
                //TODO: if RESULT_CANCELED then we should revert the upload of the picture.
                else{
                    finish();
                }
                break;
        }
    }


    private void takeExifInfo(ExifInterface exif) {
        mTimestamp = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if(mTimestamp == null)
            mTimestamp = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
        if(mTimestamp == null)
            mTimestamp = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
        if(mTimestamp == null)
            mTimestamp = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
        if(mTimestamp == null)
            mTimestamp = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
        if(mTimestamp != null)
            mTimestamp = mTimestamp.replace(" ", SEPARATOR);
        else{
            TimePicker tp = new TimePicker(this);
            String h = tp.getCurrentHour() + "";
            String m = tp.getCurrentMinute() + "";
            mTimestamp = h + ":" + m;
        }

        mLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        //myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
        mLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        //myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
        //myAttribute += getTagString(ExifInterface.TAG_IMAGE_LENGTH, exif);
        //myAttribute += getTagString(ExifInterface.TAG_IMAGE_WIDTH, exif);
        //myAttribute += getTagString(ExifInterface.TAG_ORIENTATION, exif);
        Log.d(TAG, "Timestamp = " + mTimestamp + " lat = " + mLatitude + " long = " + mLongitude);

        orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.upload_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed(){
        Intent i = getIntent();
        setResult(RESULT_CANCELED, i);
        finish();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch(id){
            case R.id.upload:
                Log.i(TAG, "Upload has been selected");
                mDescription = mDescriptionField.getText().toString();
                if(checkDescription()) {
                    Log.d(TAG, "Ready for sending data to db");
                    //put the photo into the storage

                    Point toPut = new Point();
                    toPut.setLat(Double.parseDouble(mLatitude));
                    toPut.setLon(Double.parseDouble(mLongitude));

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference pushReference = databaseReference.child(POINTS).push();
                    mPointId = pushReference.getKey();
                    toPut.setId(mPointId);
                    pushReference.setValue(toPut);

                    File compressedFile = new Compressor.Builder(this)
                            .setMaxHeight(photoW)
                            .setMaxWidth(photoW)
                            .setQuality(COMPRESSION_QUALITY)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .build()
                            .compressToFile(new File(mPhotoPath));
                    Uri file = Uri.fromFile(compressedFile);
                    totalBytes = compressedFile.length();

                    //save the image as username_timestamp
                    mPhotoId = mUsername + SEPARATOR + mTimestamp;
                    StorageReference riversRef = mStorageRef.child(mPhotoId);
                    inUpload = true;
                    showProgressBar();

                    riversRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Get a URL to the uploaded content
                                    getPath();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    // ...
                                    Log.e(TAG, "Error during the upload, " + exception.toString());
                                    Toast.makeText(getApplicationContext(),
                                            R.string.upload_failed,
                                            Toast.LENGTH_SHORT).show();
                                    uploadError = true;
                                    inUpload = false;
                                }
                            })
                            .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                @SuppressWarnings("VisibleForTests")
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    transferredBytes = taskSnapshot.getBytesTransferred();
                                    Log.d(TAG, "TransferredBytes " + transferredBytes);
                                }
                            });
                    Intent i = getIntent();
                    setResult(RESULT_OK, i);
                    finish();
                }
                return true;
        }
        return false;
    }

    private void showProgressBar(){
        mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.upload_prog_bar_title))
                .setContentText(getString(R.string.upload_prog_bar_text))           //TODO: change the icon
                .setSmallIcon(R.drawable.btn_google_signin_light_normal_xxxhdpi)
                .setAutoCancel(true);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int percentage;
                        int id = 0;
                        while(inUpload) {
                            // Sets the progress indicator to a max value, the
                            // current completion percentage, and "determinate"
                            // state
                            percentage = (int)((double)(transferredBytes)/(double)(totalBytes)*100);
                            mBuilder.setProgress(100, percentage, false);
                            // Displays the progress bar for the first time.
                            mNotifyManager.notify(id, mBuilder.build());
                            if(percentage == 100)
                                inUpload = false;

                            try {
                                // Sleep for 5 seconds
                                Thread.sleep(NOT_BAR_SLEEP);
                            } catch (InterruptedException e) {
                                Log.d(TAG, "sleep failure");
                            }
                        }
                        if (uploadError){
                            uploadError = false;
                            mBuilder.setProgress(0,0,false);
                            mBuilder.setContentText(getString(R.string.upload_prog_bar_fail))
                                    .setProgress(0,0,false);
                            mNotifyManager.notify(id, mBuilder.build());
                        }
                        else {
                            // When the loop is finished, updates the notification
                            mBuilder.setProgress(0, 0, false);
                            mBuilder.setContentText(getString(R.string.upload_prog_bar_ok))
                                    .setProgress(0, 0, false);
                            mNotifyManager.notify(id, mBuilder.build());
                        }
                    }
                }
        ).start();
    }


    private void setPic() {

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPhotoPath, bmOptions);
        photoW = bmOptions.outWidth;
        photoH = bmOptions.outHeight;

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;
		/* Decode the JPEG file into a Bitmap*/
        Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath, bmOptions);

        //Use the matrix for rotate the image
        Matrix matrix = new Matrix();
        matrix.postRotate(getRotation());
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0,  photoW, photoH, matrix, true);

        //Scale the bitmap without changing the proportions
        int width = rotatedBitmap.getWidth();
        int height = rotatedBitmap.getHeight();
        double scale;
        if (width >= height)
            scale = (double) width / height;
        else
            scale = (double) height / width;
        maxWidth = this.getResources().getDisplayMetrics().widthPixels;
        maxHeight = (width*bitmap.getHeight())/bitmap.getWidth();
        if (orientation == PIC_VER_BOTTOM || orientation == PIC_VER_TOP) {
            if (width > maxHeight || height > maxWidth) {
                if (height > maxWidth) {
                    height = maxWidth;
                    width = (int) (height / scale);
                }
            }
            rotatedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, false);
        }
        else {
            if (width > maxWidth || height > maxHeight) {
                if (width > maxWidth) {
                    width = maxWidth;
                    height = (int) (width / scale);
                }
            }
            rotatedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, false);
        }

        //set image into the imageView
        mImageView.setImageBitmap(rotatedBitmap);
        mImageView.setVisibility(View.VISIBLE);
    }

    private int getRotation(){
        switch(orientation){
            case PIC_HOR_RIGHT:
                return 0;
            case PIC_VER_TOP:
                return 90;
            case PIC_HOR_LEFT:
                return 180;
            case PIC_VER_BOTTOM:
                return 270;
        }
        return 0;
    }


    private boolean checkDescription(){
        //TODO: remove this if no check is needed
        return true;
    }


    private void getPath(){

        Log.d(TAG, "getPath");
        StorageReference pathRef = mStorageRef.child(mPhotoId);

        pathRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.d(TAG, "MyDownloadLink:  " + uri);
                picture = new Picture(mPhotoId, mDescription, uri.toString(),
                        mUser.getUid(), mUsername, profilePicture);
                picture.setTimestamp(mTimestamp);
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                DatabaseReference pushReference = databaseReference.child(PICTURES).push();
                String id = pushReference.getKey();
                picture.setId(id);
                pushReference.setValue(picture);

                databaseReference.child(POINTS).child(mPointId).child(PICTURES).push().setValue(picture);
                Log.i(TAG, "Picture's path sent to db");
                Toast.makeText(getApplicationContext(),
                        R.string.upload_ok,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Log.e(TAG, "Error during the upload, " + exception.toString());
                        Toast.makeText(getApplicationContext(),
                                R.string.upload_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

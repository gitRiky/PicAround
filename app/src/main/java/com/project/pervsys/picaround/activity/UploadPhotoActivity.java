package com.project.pervsys.picaround.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.alexvasilkov.gestures.views.GestureImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.OnProgressListener;

import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import static com.project.pervsys.picaround.utility.Config.LOCATION_EXTRA;
import static com.project.pervsys.picaround.utility.Config.PHOTO_PATH;
import static com.project.pervsys.picaround.utility.Config.POINTS;

import id.zelory.compressor.Compressor;

import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Place;
import com.project.pervsys.picaround.domain.Point;
import com.project.pervsys.picaround.domain.User;

import static com.project.pervsys.picaround.utility.Config.*;

public class UploadPhotoActivity extends AppCompatActivity {
    private final static String TAG = "UploadPhotoActivity";
    private static final String SEPARATOR = "_";
    private static final int PIC_HOR_RIGHT = 1;
    private static final int PIC_HOR_LEFT = 3;
    private static final int PIC_VER_TOP = 6;
    private static final int PIC_VER_BOTTOM = 8;
    private static final int COMPRESSION_QUALITY = 25;
    private static final int INTERM_COMPRESSION_QUALITY = 15;
    private static final int NOT_BAR_SLEEP = 1000; //in milliseconds


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mUser;
    private StorageReference mStorageRef;
    private GestureImageView mImageView;
    private EditText mDescriptionField;
    private String mPhotoPath;
    private String mDescription;
    private String mUsername;
    private String mPhotoId;
    private String mTimestamp;
    private String mLatitude;
    private String profilePicture;
    private String mLongitude;
    private String mPlaceId;
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
    private DatabaseReference mDatabaseRef = null;
    private Bitmap mRotatedBitmap;
    private int rotationDegrees = 0;

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
        ImageButton rotateButton = (ImageButton) findViewById(R.id.rotate_button);
        rotateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rotationDegrees += 90;
                    if (rotationDegrees == 360) {
                        rotationDegrees = 0;
                        setPic(false);
                    }
                    setPic(true);
                }
        });

        mUser = mAuth.getCurrentUser();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mPhotoPath = getIntent().getStringExtra(PHOTO_PATH);
        mUsername = getIntent().getStringExtra(USERNAME);
        profilePicture = getIntent().getStringExtra(PROFILE_PICTURE);
        Log.d(TAG, "Started activity, photo's path = " + mPhotoPath);
        mImageView = (GestureImageView) findViewById(R.id.image_to_upload);
        mDescriptionField = (EditText) findViewById(R.id.photo_description);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(USERS).keepSynced(true);
        Intent i = getIntent();
        if (i.getAction() != null) {
            if (i.getAction().equals("android.intent.action.SEND")) {
                Log.d(TAG, "ACTIVITY STARTED FROM OUTSIDE");
                Uri photoUri = (Uri) i.getExtras().get(Intent.EXTRA_STREAM);
                mPhotoPath = getRealPathFromURI(this, photoUri);
                mUser = mAuth.getCurrentUser();
                if (mUser != null) {
                    getProfileInfo();
                } else {
                    Log.i(TAG, "The user is not logged in, he cannot share pictures");
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                            .setMessage("You are not logged in, you cannot share pictures")
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false);
                    dialog.show();
                }
            }
        }
        else {
            if (mAuth.getCurrentUser() != null)
                mUser = mAuth.getCurrentUser();
            mPhotoPath = i.getStringExtra(PHOTO_PATH);
            mUsername = i.getStringExtra(USERNAME);
            profilePicture = i.getStringExtra(PROFILE_PICTURE);
        }

        try {
            ExifInterface exif = new ExifInterface(mPhotoPath);
            takeExifInfo(exif);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        }

        //set the image into imageView
        setPic(false);
        if (mLatitude == null || mLongitude == null  || mLatitude.equals(DEFAULT_LAT + "") || mLongitude.equals(DEFAULT_LNG + "") || !isDouble(mLatitude) || !isDouble(mLongitude)) {
            Log.d(TAG, "Position not available in the metadata");
            Intent pickLocationIntent = new Intent(this, PickLocationActivity.class);
            startActivityForResult(pickLocationIntent, REQUEST_PICK_LOCATION);
        }

        Log.i(TAG, "Photo put into the imageView");
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
                                profilePicture = user.getProfilePicture();
                                Log.i(TAG, "Username= " + mUsername
                                        + ", profilePicturePath = " + profilePicture);
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


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_PICK_LOCATION:
                if(resultCode == RESULT_OK) {
                    String[] latLong = data.getStringExtra(LOCATION_EXTRA).split(",");
                    mLatitude = latLong[0].substring(10);
                    mLongitude = latLong[1].replace(")", "");
                    Log.d(TAG, "FROM pickLocation Activity: -> Timestamp = " + mTimestamp + " lat = " + mLatitude + " long = " + mLongitude);

                }
                //TODO: if RESULT_CANCELED then we should revert the upload of the picture.
                else{
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                }
                break;
        }
    }

    private boolean isDouble(String value){
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
                ("[\\x00-\\x20]*"+ // Optional leading "whitespace"
                        "[+-]?(" +         // Optional sign character
                        "NaN|" +           // "NaN" string
                        "Infinity|" +      // "Infinity" string

                        // A decimal floating-point string representing a finite positive
                        // number without a leading sign has at most five basic pieces:
                        // Digits . Digits ExponentPart FloatTypeSuffix
                        //
                        // Since this method allows integer-only strings as input
                        // in addition to strings of floating-point literals, the
                        // two sub-patterns below are simplifications of the grammar
                        // productions from the Java Language Specification, 2nd
                        // edition, section 3.10.2.

                        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                        "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        "(\\.("+Digits+")("+Exp+")?)|"+

                        // Hexadecimal strings
                        "((" +
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "(\\.)?)|" +

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                        ")[pP][+-]?" + Digits + "))" +
                        "[fFdD]?))" +
                        "[\\x00-\\x20]*");// Optional trailing "whitespace"

        if (Pattern.matches(fpRegex, value)){
            return true;
        } else {
            return false;
        }
    }

    private void takeExifInfo(ExifInterface exif) {

        // Take timestamp
        mTimestamp = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if(mTimestamp != null)
            mTimestamp = mTimestamp.replace(" ", SEPARATOR);
        else{
            SimpleDateFormat s = new SimpleDateFormat("yyyy:MM:dd_hh:mm:ss");
            mTimestamp = s.format(new Date());
        }

        // Take latlng

        mLatitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        mLongitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

        if(mLatitude == null)
            mLatitude = getIntent().getDoubleExtra(LATITUDE, DEFAULT_LAT) + "";
        if(mLongitude == null)
            mLongitude = getIntent().getDoubleExtra(LONGITUDE, DEFAULT_LNG) + "";

        Log.d(TAG, "Timestamp = " + mTimestamp + " lat = " + mLatitude + " long = " + mLongitude);

        // Take orientation
        orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
        Log.d(TAG, "orientation: " + orientation);
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
                mDescription = mDescriptionField.getText().toString().trim();
                if(checkDescription()) {
                    Log.d(TAG, "Ready for sending data to db");

                    //save the image as username_timestamp
                    mPhotoId = mUsername + SEPARATOR + mTimestamp;

                    // Generate and upload thumbnail
                    File thumbnailFile = Compressor.getDefault(UploadPhotoActivity.this)
                            .compressToFile(new File(mPhotoPath));
                    Uri thumbnailUri = Uri.fromFile(thumbnailFile);
                    String thumbnailId = THUMB_PREFIX + mPhotoId;
                    Log.d(TAG, "thumbnail bytes: " + thumbnailFile.length());
                    StorageReference thumbRef = mStorageRef.child(thumbnailId);
                    Toast.makeText(this, R.string.uploading_picture, Toast.LENGTH_SHORT).show();
                    thumbRef.putFile(thumbnailUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Log.i(TAG, "Thumbnail successfully uploaded");
                                    uploadIntPicture();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    Log.e(TAG, "Error during uploading the thumbnail, " + exception.toString());
                                    Toast.makeText(getApplicationContext(),
                                            R.string.upload_failed,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                    Intent data = new Intent();
                    String location = mLatitude + "," + mLongitude;
                    data.putExtra(LOCATION_EXTRA, location);
                    setResult(RESULT_OK, data);
                    finish();
                }
                return true;
        }
        return false;
    }


    private void uploadIntPicture() {
        File compressedFile = new Compressor.Builder(this)
                .setMaxHeight(photoH)
                .setMaxWidth(photoW)
                .setQuality(INTERM_COMPRESSION_QUALITY)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .build()
                .compressToFile(new File(mPhotoPath));
        Log.d(TAG, "InterPic bytes: " + compressedFile.length());
        String intPhotoId = INTERM_PREFIX + mPhotoId;
        final StorageReference riversRef = mStorageRef.child(intPhotoId);
        riversRef.putFile(Uri.fromFile(compressedFile))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "Intermediate picture successfully uploaded");
                        uploadPicture();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.e(TAG, "Error during uploading the intermediate picture, "
                                + exception.toString());
                        Toast.makeText(getApplicationContext(),
                                R.string.upload_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void uploadPicture() {
        File compressedFile = new Compressor.Builder(this)
                .setMaxHeight(photoH)
                .setMaxWidth(photoW)
                .setQuality(COMPRESSION_QUALITY)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .build()
                .compressToFile(new File(mPhotoPath));
        final Uri file = Uri.fromFile(compressedFile);
        totalBytes = compressedFile.length();
        Log.d(TAG, "Picture bytes: " + totalBytes);
        final StorageReference riversRef = mStorageRef.child(mPhotoId);
        inUpload = true;
        showProgressBar();
        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "Picture successfully uploaded");
                        // Get a URL to the uploaded content
                        getPath();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.e(TAG, "Error during uploading the picture, " + exception.toString());
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
    }

    private void showProgressBar(){
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.upload_prog_bar_title))
                .setContentText(getString(R.string.upload_prog_bar_text))           //TODO: change the icon
                .setSmallIcon(R.drawable.btn_google_signin_light_normal_xxxhdpi)
                .setAutoCancel(true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MapsActivity.class));
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

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

    private void setPic(boolean rotate) {

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
        if (rotate)
            matrix.postRotate(getRotation() + rotationDegrees);
        else
            matrix.postRotate(getRotation());
        mRotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0,  photoW, photoH, matrix, true);

        //Scale the bitmap without changing the proportions
        int width = mRotatedBitmap.getWidth();
        int height = mRotatedBitmap.getHeight();
        double scale;
        if (width >= height)
            scale = (double) width / height;
        else
            scale = (double) height / width;
        maxWidth = this.getResources().getDisplayMetrics().widthPixels;
        maxHeight = (width*bitmap.getHeight())/bitmap.getWidth();
        if (!rotate) {
            if (orientation == PIC_VER_BOTTOM || orientation == PIC_VER_TOP) {
                if (width > maxHeight || height > maxWidth) {
                    if (height > maxWidth) {
                        height = maxWidth;
                        width = (int) (height / scale);
                    }
                }
                mRotatedBitmap = Bitmap.createScaledBitmap(mRotatedBitmap, width, height, false);
            } else {
                if (width > maxWidth || height > maxHeight) {
                    if (width > maxWidth) {
                        width = maxWidth;
                        height = (int) (width / scale);
                    }
                }
                mRotatedBitmap = Bitmap.createScaledBitmap(mRotatedBitmap, width, height, false);
            }
        }
        else {
            if (orientation == PIC_VER_BOTTOM || orientation == PIC_VER_TOP ) {
                if (rotationDegrees == 90 || rotationDegrees == 270) {
                    if (width > maxWidth || height > maxHeight) {
                        if (width > maxWidth) {
                            width = maxWidth;
                            height = (int) (width / scale);
                        }
                    }
                }
                else {
                    if (width > maxHeight || height > maxWidth) {
                        if (height > maxWidth) {
                            height = maxWidth;
                            width = (int) (height / scale);
                        }
                    }
                }
                mRotatedBitmap = Bitmap.createScaledBitmap(mRotatedBitmap, width, height, false);
            } else {
                if (rotationDegrees == 90 || rotationDegrees == 270) {
                    if (width > maxHeight || height > maxWidth) {
                        if (height > maxWidth) {
                            height = maxWidth;
                            width = (int) (height / scale);
                        }
                    }
                }
                else {
                    if (width > maxWidth || height > maxHeight) {
                        if (width > maxWidth) {
                            width = maxWidth;
                            height = (int) (width / scale);
                        }
                    }
                }
                mRotatedBitmap = Bitmap.createScaledBitmap(mRotatedBitmap, width, height, false);
            }
        }
        //set image into the imageView
        mImageView.setImageBitmap(mRotatedBitmap);
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
                Log.d(TAG, "MyDownloadLink: " + uri);

                //create the new Place
                Place toPut = new Place();
                toPut.setLat(Double.parseDouble(mLatitude));
                toPut.setLon(Double.parseDouble(mLongitude));
                DatabaseReference pushReference = mDatabaseRef.child(PLACES).push();
                mPlaceId = pushReference.getKey();
                toPut.setId(mPlaceId);
                Log.d(TAG, "toPut " + toPut);

                //create the Picture object
                picture = new Picture(mPhotoId, mDescription, uri.toString(),
                        mUser.getUid(), mUsername, profilePicture, mPlaceId,
                        Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
                picture.setTimestamp(mTimestamp);
                DatabaseReference pictureRef = mDatabaseRef.child(PICTURES).push();
                String id = pictureRef.getKey();
                picture.setId(id);

                //the db stores the popularity as 1 - popularity,
                //i.e. the minimum popularity is 1
                picture.setPopularity(1);

                //add the picture to the place and send to db both picture and place
                toPut.addPicture(picture);
                pushReference.setValue(toPut);
                pictureRef.setValue(picture);

                //add the picture to Users
                mDatabaseRef.child(USERS).child(mUser.getUid()).child(PICTURES).child(id).setValue(picture);
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

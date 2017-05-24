package com.project.pervsys.picaround.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.animation.ValueAnimatorCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.User;
import com.project.pervsys.picaround.utility.Functions;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.sql.Timestamp;
import java.util.Calendar;

import id.zelory.compressor.Compressor;

import static com.project.pervsys.picaround.utility.Config.*;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseRef = null;
    private String mEmail;
    private TextView mUsernameView;
    private TextView mNameView;
    private TextView mSurnameView;
    private TextView mAgeView;
    private TextView mEmailView;
    private ImageView mImageView;
    private File mCompressedFile;
    private StorageReference mStorageRef;
    private User mUser;
    private String mUserId;
    private ProgressDialog progress;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //set status bar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        //firebase
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

        mUsernameView = (TextView) findViewById(R.id.profile_username);
        mNameView = (TextView) findViewById(R.id.profile_name);
        mSurnameView = (TextView) findViewById(R.id.profile_surname);
        mAgeView = (TextView) findViewById(R.id.profile_age);
        mEmailView = (TextView) findViewById(R.id.profile_email);
        mImageView = (ImageView) findViewById(R.id.profile_picture);
        registerForContextMenu(mImageView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            mEmail = user.getEmail();
            getProfileInfo();
        }

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

    private void getProfileInfo(){
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(USERS).orderByChild(EMAIL).equalTo(mEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                            mUser = userSnap.getValue(User.class);
                            mUserId = userSnap.getKey();
                            Picasso.with(getApplicationContext())
                                    .load(mUser.getProfilePicture())
                                    .into(mImageView);
                            mUsernameView.setText(mUser.getUsername());
                            mNameView.setText(mUser.getName());
                            mSurnameView.setText(mUser.getSurname());
                            mEmailView.setText(mUser.getEmail());
                            mAgeView.setText(getAge(mUser.getDate()));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    private String getAge(String date){
        //date in format yyyy/mm/dd
        int age;
        String[] split = date.split("/");
        int year = Integer.parseInt(split[0]);
        int month = Integer.parseInt(split[1]);
        int day = Integer.parseInt(split[2]);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        if (currentMonth > month) {
            age = currentYear - year;
        }
        else if (currentMonth == month) {
            if (currentDay >= day)
                age = currentYear - year;
            else
                age = currentYear - year - 1;
        }
        else {
            age = currentYear - year - 1;
        }
        return "" + age;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch(id){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_profile_image_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_profile_image:
                Log.i(TAG, "Selected update profile image");
                selectPicture();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Photo has been picked");
                    Uri photoUri = data.getData();
                    String currentPhotoPath = Functions.getRealPathFromURI(this, photoUri);
                    mCompressedFile = Compressor.getDefault(this)
                            .compressToFile(new File(currentPhotoPath));
                    Log.d(TAG, "Path: " + currentPhotoPath);

                    //load the new photo
                    Picasso.with(getApplicationContext())
                            .load(mCompressedFile)
                            .fit()
                            .centerInside()
                            .into(mImageView);

                    startConfirmDialog();
                }
                break;
        }
    }

    private void startConfirmDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_update_profile_image_mex)
                .setTitle(R.string.update_profile_image)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //start the update
                        Log.i(TAG, "Updating profile picture");
                        //save the image into the storage
                        uploadImage();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //put the old profile image into the image view
                        Log.d(TAG, "Put back the old photo");
                        Picasso.with(getApplicationContext())
                                .load(mUser.getProfilePicture())
                                .into(mImageView);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

    }

    private void uploadImage(){
        progress = Functions.startProgressBar(this);
        Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
        String photoId = mUser.getUsername() + "_" + timestamp.toString().replace(" ", "_").replace(".",":");
        mStorageRef =  FirebaseStorage.getInstance().getReference().child(photoId);
        mStorageRef.putFile(Uri.fromFile(mCompressedFile))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Log.i(TAG, "Image has been uploaded");
                        getPhotoPath();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getApplicationContext(),
                                R.string.upload_failed, Toast.LENGTH_LONG)
                                .show();
                        if (progress != null)
                            progress.dismiss();
                        Log.e(TAG, "Error during the upload, " + exception.toString());
                    }
                });
    }

    private void getPhotoPath(){
        mStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(final Uri uri) {
                Log.d(TAG, "Uri: " + uri);
                ApplicationClass.setNewProfilePicturePath(uri.toString());

                //update profile picture in the users root
                mUser.setProfilePicture(uri.toString());
                DatabaseReference pushRef = mDatabaseRef.child(USERS).child(mUserId);
                pushRef.setValue(mUser);

                //update profile picture of all the photos already taken
                mDatabaseRef.child(PICTURES).orderByChild(USER_ID).equalTo(mUser.getId())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                                    Picture picture = userSnap.getValue(Picture.class);
                                    Log.d(TAG, "Name: " + picture.getName());
                                    picture.setUserIcon(uri.toString());
                                    mDatabaseRef.child(PICTURES).child(picture.getId())
                                            .setValue(picture);
                                }
                                if (progress != null)
                                    progress.dismiss();
                                Log.i(TAG, "All the pictures with UserId = " +
                                        mUser.getId() + " have been updated");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "Error during the update of pictures, " +
                                        databaseError.toString());
                                if (progress != null)
                                    progress.dismiss();
                            }
                        });
            }
        });
    }


    //start the gallery Intent
    private void selectPicture(){
        if (Build.VERSION.SDK_INT <= 19) {
            Intent intent = new Intent();
            intent.setType("image/*");
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

package com.project.pervsys.picaround;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.User;
import com.squareup.picasso.Picasso;

import java.util.Calendar;

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //set status bar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.profile);
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
                        Log.d(TAG, "onDataChange");
                        for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                            Log.d(TAG, "inside the for");
                            User user = userSnap.getValue(User.class);
                            Picasso.with(getApplicationContext())
                                    .load(user.getProfilePicture())
                                    .into(mImageView);
                            mUsernameView.setText(user.getUsername());
                            mNameView.setText(user.getName());
                            mSurnameView.setText(user.getSurname());
                            mEmailView.setText(user.getEmail());
                            mAgeView.setText(getAge(user.getDate()));
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
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        Log.d(TAG, currentDay + "/" + currentMonth + "/" + currentYear);
        if (currentMonth > month)
            age = currentYear - year;
        else if (currentMonth == month) {
            if (currentDay >= day)
                age = currentYear - year;
            else
                age = currentYear - year - 1;
        }
        else
            age = currentYear - year - 1;

        return "" + age;
    }
}

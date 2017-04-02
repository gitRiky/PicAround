package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.Profile;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.User;
import com.project.pervsys.picaround.utility.Config;

public class GetBasicInfoActivity extends AppCompatActivity {
    private static final int MIN_AGE = 6;
    private final static int MAX_AGE = 95;
    private final static String TAG = "GetBasicInfoActivity";
    private final static String USERS = "users";
    private final static String USERNAME = "username";
    private User newUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_basic_info);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.i(TAG, "Logged with Firebase, UID: " + user.getUid());
        }
        else {
            Log.i(TAG, "Not logged with Firebase");
        }
        newUser = new User();
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
        final String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
        if(logged != null){
            //set up user
            newUser.setEmail(user.getEmail());
            if (logged.equals(Config.FB_LOGGED)){
                //get information from facebook
                Profile fbProfile = Profile.getCurrentProfile();
                newUser.setName(fbProfile.getFirstName());
                newUser.setSurname(fbProfile.getLastName());
                newUser.setProfile_picture(fbProfile.getProfilePictureUri(10,10).toString());
            }
            else{
                //get information from google
                GoogleSignInAccount googProfile = ApplicationClass.getGoogleSignInResult().getSignInAccount();
                newUser.setName(googProfile.getGivenName());
                newUser.setSurname(googProfile.getFamilyName());
                newUser.setProfile_picture(googProfile.getPhotoUrl().toString());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onBackPressed(){
        Toast.makeText(this, R.string.registration_needed, Toast.LENGTH_SHORT).show();
    }


    public void onClick(View w){
        EditText ageField = (EditText) findViewById(R.id.age);
        String age = ageField.getText().toString();
        EditText usernameField = (EditText) findViewById(R.id.username);
        String username = usernameField.getText().toString();
        if (checkAge(age)){
            newUser.setAge(Integer.parseInt(age));
            if (!checkUsername(username)) {
                usernameField.setHint(R.string.username);
                usernameField.setText("");
            }
        }
        else{
            ageField.setHint(R.string.age);
            ageField.setText("");
        }
    }

    private boolean checkAge(String age){
        if (age.equals("")) {
            Toast.makeText(this, R.string.age_missing, Toast.LENGTH_SHORT).show();
            return false;
        }
        int a = Integer.parseInt(age);
        if (a < MIN_AGE || a > MAX_AGE){
            Toast.makeText(this, R.string.age_not_in_range, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkUsername(final String username){
        if (username.equals("")){
            Toast.makeText(this, R.string.username_missing, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.contains(" ")){
            Toast.makeText(this, R.string.username_with_spaces,Toast.LENGTH_SHORT).show();
            return false;
        }
        //query to database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(USERS).orderByChild(USERNAME).equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // username already used
                            Toast.makeText(getApplicationContext(),
                                    R.string.username_unavailable,
                                    Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Username unavailable");
                        } else {
                            //username not used
                            newUser.setUsername(username);
                            // put the user into the database
                            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                            databaseRef.child(USERS).push().setValue(newUser);
                            Toast.makeText(getApplicationContext(), R.string.registration_ok,
                                    Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "User registered, data sent to database");
                            Intent i = getIntent();
                            setResult(RESULT_OK,i);
                            Log.e(TAG, newUser.toString());
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
        return true;
    }

}

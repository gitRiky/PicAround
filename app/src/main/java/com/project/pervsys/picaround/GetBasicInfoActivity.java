package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GetBasicInfoActivity extends AppCompatActivity {
    private static final int MIN_AGE = 6;
    private final static int MAX_AGE = 95;
    private final static String TAG = "GetBasicInfoActivity";
    private final static String USERS = "users";
    private final static String USERNAME = "username";
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /*@Override
    public void onBackPressed(){
        Toast.makeText(this, "You have to complete the registration!", Toast.LENGTH_LONG).show();
    }
*/

    public void onClick(View w){
        EditText ageField = (EditText) findViewById(R.id.age);
        String age = ageField.getText().toString();
        EditText usernameField = (EditText) findViewById(R.id.username);
        String username = usernameField.getText().toString();
        if (checkAge(age)){
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
            Toast.makeText(this, "Age is missing", Toast.LENGTH_SHORT).show();
            return false;
        }
        int a = Integer.parseInt(age);
        if (a < MIN_AGE || a > MAX_AGE){
            Toast.makeText(this, "Age not in range. Please put your real age", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean checkUsername(String username){
        if (username.equals("")){
            Toast.makeText(this, "Username is missing", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.contains(" ")){
            Toast.makeText(this, "Username cannot contain spaces",Toast.LENGTH_SHORT).show();

            return false;
        }
        //query to database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(USERS).orderByChild(USERNAME).equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.e(TAG, "I'M IN DATA CHANGE");
                        if (dataSnapshot.exists()) {
                            // email already registered
                            Toast.makeText(getApplicationContext(),
                                    "Sorry, this username is not available",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Registration successful",
                                    Toast.LENGTH_SHORT).show();
                            Intent i = getIntent();
                            setResult(RESULT_OK,i);
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

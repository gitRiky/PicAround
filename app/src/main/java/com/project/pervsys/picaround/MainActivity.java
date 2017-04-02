package com.project.pervsys.picaround;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.utility.Config;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity {

    //this boolean will be removed, info passed by putExtra intent
    private boolean firstTime;
    private final static String EMAIL = "email";
    private final static String USERS = "users";
    private final static String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        final String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
        final TextView t = (TextView) findViewById(R.id.textView);
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
        if (!getIntent().getBooleanExtra(Config.REGISTERED, true)){
            Log.i(TAG, "First use of the application for the user");
            /*BasicInfoDialog dialog = new BasicInfoDialog();
            dialog.show(getFragmentManager(),"");*/
            Intent intent = new Intent(this, GetBasicInfoActivity.class);
            startActivityForResult(intent, Config.RC_REGISTRATION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
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
                Log.i(TAG, "Info has been selected");
                Toast.makeText(this, "Selected info", Toast.LENGTH_SHORT).show();
                //Info activity
                return true;
            case R.id.profile:
                Log.i(TAG, "Profile has been selected");
                Toast.makeText(this, "Selected profile", Toast.LENGTH_SHORT).show();
                //Profile activity
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
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this)
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

    @Override
    protected void onDestroy(){
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit().
                putString(Config.LOG_PREF_INFO, null).apply();
        ApplicationClass.setGoogleApiClient(null);
        ApplicationClass.setGoogleSignInResult(null);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == Config.RC_REGISTRATION) {
            //do nothing
            Log.i(TAG, "returned from registration");
        }
    }

}

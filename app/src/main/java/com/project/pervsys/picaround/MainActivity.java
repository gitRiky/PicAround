package com.project.pervsys.picaround;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.project.pervsys.picaround.utility.Config;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity {

    //this boolean will be removed, info passed by putExtra intent
    private final static boolean firstTime = true;
    private final static String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        if (firstTime){
            Log.i(TAG, "First use of the application for the user");
            BasicInfoDialog dialog = new BasicInfoDialog();
            dialog.show(getFragmentManager(),"");
        }
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0)
                .getString(Config.LOG_PREF_INFO, null);
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText(logged);

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
        if (Profile.getCurrentProfile() != null){
            LoginManager.getInstance().logOut();
            Log.i(TAG, "Logout from Facebook");
            getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit()
                    .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED).apply();
            startLogin();
        }
        String logged = getSharedPreferences(Config.LOG_PREFERENCES,MODE_PRIVATE)
                .getString(Config.LOG_PREF_INFO,null);
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
    }


    private void startLogin(){
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit().
                putString(Config.LOG_PREF_INFO, null).apply();
        ApplicationClass.setGoogleApiClient(null);
    }
}

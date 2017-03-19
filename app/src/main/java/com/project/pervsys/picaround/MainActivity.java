package com.project.pervsys.picaround;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {

    //this boolean will be removed, info passed by putExtra intent
    private final static boolean firstTime = true;
    private final static String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (firstTime){
            Log.i(TAG, "First use of the application for the user");
            BasicInfoDialog dialog = new BasicInfoDialog();
            dialog.show(getFragmentManager(),"");
        }
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0).getString(Config.LOG_PREF_INFO, null);
        //TODO: add an or: facebook or google account
        //if the user is not logged with facebook
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText(logged);

    }


    public void onLogOutClickListener(View w) {
        if (Profile.getCurrentProfile() != null){
            LoginManager.getInstance().logOut();
            Log.i(TAG, "Logout from Facebook");
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
        String logged = getSharedPreferences(Config.LOG_PREFERENCES,MODE_PRIVATE).
                getString(Config.LOG_PREF_INFO,null);
        if (logged != null & logged.equals(Config.GOOGLE_LOGGED)){

        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit().putString(Config.LOG_PREF_INFO, null).apply();
    }
}

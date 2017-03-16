package com.project.pervsys.picaround;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.facebook.Profile;
import com.facebook.login.LoginManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: add an or: facebook or google account
        //if the user is not logged with facebook
        if (Profile.getCurrentProfile() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }

    }


    public void onLogOutClickListener(View w) {
        if (Profile.getCurrentProfile() != null) {
            LoginManager.getInstance().logOut();
            Toast.makeText(this, R.string.logout_text, Toast.LENGTH_LONG).show();
            /*TODO: decide if use a simple toast and remain on the current activity or open
                    the activity login
             */
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }
}

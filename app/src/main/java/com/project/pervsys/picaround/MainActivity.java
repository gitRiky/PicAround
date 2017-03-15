package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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


    public void onLogOutClickListener(View w){
        LoginManager.getInstance().logOut();
    }
}

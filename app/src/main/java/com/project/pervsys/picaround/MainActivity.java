package com.project.pervsys.picaround;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.project.pervsys.picaround.utility.Config;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String logged = getSharedPreferences("Logging", 0).getString("Logged", null);
        System.out.println("LOGGGGGGGGGGGGGGG " + logged);
        //TODO: add an or: facebook or google account
        //if the user is not logged with facebook
        if (logged == null) {
            if (Profile.getCurrentProfile() == null) {
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
            }
            else
                getSharedPreferences("Logging",0).edit().putString("Logged", Config.FB_LOGGED).commit();
        }

    }





    public void onLogOutClickListener(View w) {
        if (Profile.getCurrentProfile() != null) {
            LoginManager.getInstance().logOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        getSharedPreferences("Logging", 0).edit().putString("Logged", null).commit();
        System.out.println("LOGGGGGGGGG " + getSharedPreferences("Logging",0).getString("Logged",null));
    }
}

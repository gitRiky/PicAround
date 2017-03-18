package com.project.pervsys.picaround;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.project.pervsys.picaround.utility.Config;

public class MainActivity extends AppCompatActivity {
    //this boolean will be removed, info passed by putExtra intent
    private final static boolean firstTime = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (firstTime){
            BasicInfoDialog dialog = new BasicInfoDialog();
            dialog.show(getFragmentManager(),"");
        }
        System.out.println("AAAAACCCCCTIVITY MAIN!!!");
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, 0).getString(Config.LOG_PREF_INFO, null);
        //TODO: add an or: facebook or google account
        //if the user is not logged with facebook
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText(logged);

    }


    public void onLogOutClickListener(View w) {
        System.out.println("LOGGGGG "+ getSharedPreferences(Config.LOG_PREFERENCES, 0).getString(Config.LOG_PREF_INFO, null));
        if (Profile.getCurrentProfile() != null){
            LoginManager.getInstance().logOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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
        getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE).edit().putString(Config.LOG_PREF_INFO, null).apply();
    }
}

package com.project.pervsys.picaround.activity;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.User;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;


import static com.project.pervsys.picaround.utility.Config.*;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private DatabaseReference mDatabaseRef = null;
    private String mUserId;
    private User mUser;
    private HashMap<String,Picture> mPictures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Intent intent = getIntent();
        mUserId = intent.getStringExtra(USER_ID);

        final ImageView userIcon = (ImageView) findViewById(R.id.user_icon);
        final TextView username = (TextView) findViewById(R.id.username);
        final TextView fullName = (TextView) findViewById(R.id.user_fullname);
        final GridView userPictures = (GridView) findViewById(R.id.user_pictures);

        mPictures = new HashMap<>();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(USERS).keepSynced(true);
        mDatabaseRef.child(USERS).orderByChild(ID).equalTo(mUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                            mUser = userSnap.getValue(User.class);
                            username.setText(mUser.getUsername());

                            int age = getAge(mUser.getDate());
                            fullName.setText(mUser.getName() + " " + mUser.getSurname() + ", " + age);

                            mPictures = mUser.getPictures();

                            Picasso.with(UserActivity.this)
                                    .load(mUser.getProfilePicture())
                                    .into(userIcon);

                            ImageAdapter adapter = new ImageAdapter(UserActivity.this, mPictures);
                            userPictures.setAdapter(adapter);
                            userPictures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                    Picture picture = (Picture) adapterView.getItemAtPosition(position);

                                    Log.i(TAG, "Picture: " + picture);

                                    // Start PictureActivity
                                    Intent i = new Intent(UserActivity.this, PictureActivity.class);
                                    i.putExtra(PICTURE_ID, picture.getId());
                                    startActivity(i);
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
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
                String logType = getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE)
                        .getString(LOG_PREF_INFO, null);
                if (logType != null && !logType.equals(NOT_LOGGED)){
                    Intent i = new Intent(this, ProfileActivity.class);
                    startActivity(i);
                }
                else
                    Toast.makeText(this, R.string.not_logged_mex, Toast.LENGTH_LONG).show();
                return true;

            case android.R.id.home:
                finish();
                return true;
            default:
                return onOptionsItemSelected(item);
        }
    }

    private int getAge(String date){
        //date in format yyyy/mm/dd
        int age;
        String[] split = date.split("/");
        int year = Integer.parseInt(split[0]);
        int month = Integer.parseInt(split[1]);
        int day = Integer.parseInt(split[2]);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
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

        return age;
    }
}

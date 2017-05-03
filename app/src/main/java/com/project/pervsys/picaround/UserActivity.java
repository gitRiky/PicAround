package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

                            int year = Integer.parseInt(mUser.getDate().substring(0,4));
                            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                            int age = currentYear-year;
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
}

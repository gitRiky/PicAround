package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Point;
import static com.project.pervsys.picaround.utility.Config.*;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class PointActivity extends AppCompatActivity {

    private static final String TAG = "PointActivity";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseRef = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point);

        // Set toolbar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle(R.string.point_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
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

        Intent intent = getIntent();
        String pointId = intent.getStringExtra(POINT_ID);

        final GridView pointPictures = (GridView) findViewById(R.id.point_pictures);

        final LinkedHashMap<String, Picture> pictures = new LinkedHashMap<>();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child("points").child(pointId).child("pictures").orderByChild("popularity")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot picture : dataSnapshot.getChildren()) {
                            Picture pic = picture.getValue(Picture.class);
                            pictures.put(picture.getKey(),pic);
                        }

                        ImageAdapter adapter = new ImageAdapter(PointActivity.this, pictures);
                        pointPictures.setAdapter(adapter);
                        pointPictures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                Picture picture = (Picture) adapterView.getItemAtPosition(position);

                                Log.i(TAG, "Picture: " + picture);

                                // Start PictureActivity
                                Intent i = new Intent(PointActivity.this, PictureActivity.class);
                                i.putExtra(PICTURE_ID, picture.getId());
                                startActivity(i);
                            }
                        });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //database error, e.g. permission denied (not logged with Firebase)
                    Log.e(TAG, databaseError.toString());
                }
            });

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
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
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
//            case R.id.search:
//                Log.i(TAG, "Search has been selected");
//                Toast.makeText(this, "Selected search", Toast.LENGTH_SHORT).show();
//                //Profile activity
//                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return true;
        }
    }
}

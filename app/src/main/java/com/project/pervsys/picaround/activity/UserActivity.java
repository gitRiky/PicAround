package com.project.pervsys.picaround.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
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
import com.project.pervsys.picaround.localDatabase.DBManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.claudiodegio.msv.OnSearchViewListener;
import com.claudiodegio.msv.SuggestionMaterialSearchView;
import com.claudiodegio.msv.adapter.SearchSuggestRvAdapter;


import static com.project.pervsys.picaround.utility.Config.*;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private DatabaseReference mDatabaseRef = null;
    private String mUserId;
    private String mUsername;
    private User mUser;
    private HashMap<String,Picture> mPictures;
    private SuggestionMaterialSearchView mSearchView;
    private DBManager mDbManager;
    private ArrayList<String> mUsernames;

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
        mUsername = intent.getStringExtra(USERNAME);

        String orderByParameter = null;
        String equalToParameter = null;
        if (mUserId != null){
            orderByParameter = ID;
            equalToParameter = mUserId;
        }
        else if (mUsername != null){
            orderByParameter = USERNAME;
            equalToParameter = mUsername.toLowerCase();
        }

        final ImageView userIcon = (ImageView) findViewById(R.id.user_icon);
        final TextView username = (TextView) findViewById(R.id.username);
        final TextView fullName = (TextView) findViewById(R.id.user_fullname);
        final TextView noPictures = (TextView) findViewById(R.id.no_pictures);
        final GridView userPictures = (GridView) findViewById(R.id.user_pictures);

        mDbManager = new DBManager(UserActivity.this);

        mSearchView = (SuggestionMaterialSearchView) findViewById(R.id.sv);
        mSearchView.setOnSearchViewListener(new OnSearchViewListener() {
            @Override
            public void onSearchViewShown() {
                populateUsernames();
//                mFloatingActionMenu.hideMenuButton(true);
            }

            @Override
            public void onSearchViewClosed() {
//                mFloatingActionMenu.showMenuButton(true);
                mUsernames.clear();
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                if (mUsernames.contains(s)) {
                    Intent intent = new Intent(UserActivity.this, UserActivity.class);
                    intent.putExtra(USERNAME, s);
                    startActivity(intent);
                    mSearchView.closeSearch();
                }
                else {
                    Toast.makeText(UserActivity.this, R.string.no_users_found, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onQueryTextChange(String s) {
            }
        });

        mUsernames = new ArrayList<>();

        mPictures = new HashMap<>();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(USERS).keepSynced(true);
        mDatabaseRef.child(USERS).orderByChild(orderByParameter).equalTo(equalToParameter)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        ProgressDialog progress = ApplicationClass.getProgress();
                        if (progress != null)
                            progress.dismiss();

                        for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                            mUser = userSnap.getValue(User.class);
                            username.setText(mUser.getUsername());

                            Picasso.with(UserActivity.this)
                                    .load(mUser.getProfilePicture())
                                    .into(userIcon);

                            mPictures = mUser.getPictures(); //TODO: retrieve pictures ordered by timestamp
                            Log.d(TAG, "Pictures: " + mPictures);

                            if (mPictures.isEmpty()){
                                noPictures.setVisibility(View.VISIBLE);
                                fullName.setText(mUser.getName() + " " + mUser.getSurname());
                            }
                            else {
                                int picturesNumber = mPictures.size();
                                if (mPictures.size() == 1) {
                                    fullName.setText(mUser.getName() + " " + mUser.getSurname() + ", " +
                                            picturesNumber + " " + getString(R.string.picture));

                                } else {
                                    fullName.setText(mUser.getName() + " " + mUser.getSurname() + ", " +
                                            picturesNumber + " " + getString(R.string.pictures));
                                }

                                final Picture[] pictures = mPictures.values().toArray(new Picture[picturesNumber]);

                                ImageAdapter adapter = new ImageAdapter(UserActivity.this, mPictures);
                                userPictures.setAdapter(adapter);
                                userPictures.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                        Picture picture = (Picture) adapterView.getItemAtPosition(position);

                                        Log.i(TAG, "Picture: " + picture);

                                        // Start PictureSliderActivity

                                        Intent i = new Intent(UserActivity.this, PictureSliderActivity.class);
                                        i.putExtra(PICTURES, pictures);
                                        i.putExtra(POSITION, position);
                                        startActivity(i);
                                    }
                                });
                            }
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

        MenuItem item = menu.findItem(R.id.action_search);
        mSearchView.setMenuItem(item);

        MenuItem userItem = menu.findItem(R.id.user);
        userItem.setVisible(false);

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

    private void populateUsernames(){
        mDbManager.dropTable();
        mDbManager.createTable();
        mDatabaseRef.child(USERNAMES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            String username = (String)child.getValue();
                            mDbManager.insert(username);
                        }
                        Cursor result = mDbManager.query();
                        result.moveToFirst();
                        for (int i = 0; i < result.getCount(); i++) {
                            String username = result.getString(result.getColumnIndex(USERNAME));
                            Log.d(TAG, username);
                            result.moveToNext();
                            mUsernames.add(username);
                        }
                        mSearchView.setSuggestAdapter(new SearchSuggestRvAdapter(UserActivity.this, mUsernames));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }
}

package com.project.pervsys.picaround.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.claudiodegio.msv.OnSearchViewListener;
import com.claudiodegio.msv.SuggestionMaterialSearchView;
import com.claudiodegio.msv.adapter.SearchSuggestRvAdapter;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.project.pervsys.picaround.utility.Config.ID;
import static com.project.pervsys.picaround.utility.Config.NUM_COLUMNS;
import static com.project.pervsys.picaround.utility.Config.USERNAME;
import static com.project.pervsys.picaround.utility.Config.USERNAMES;
import static com.project.pervsys.picaround.utility.Config.USERS;
import static com.project.pervsys.picaround.utility.Config.USER_ID;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private DatabaseReference mDatabaseRef = null;
    private String mUserId;
    private User mUser;
    private HashMap<String,Picture> mPictures;
    private SuggestionMaterialSearchView mSearchView;
    private DBManager mDbManager;
    private ArrayList<String> mUsernames;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

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
        final TextView noPictures = (TextView) findViewById(R.id.no_pictures);

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
        mDatabaseRef.child(USERS).orderByChild(ID).equalTo(mUserId)
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

                            mPictures = mUser.getPictures();
                            Log.d(TAG, "Pictures: " + mPictures);

                            if (mPictures.isEmpty()){
                                noPictures.setVisibility(View.VISIBLE);
                                fullName.setText(mUser.getName() + " " + mUser.getSurname());
                            }
                            else {
                                int picturesNumber = mPictures.size();
                                if (mPictures.size() == 1) {
                                    fullName.setText(mUser.getName() + " " + mUser.getSurname()
                                            + ", " + picturesNumber + " " + getString(R.string.picture));

                                } else {
                                    fullName.setText(mUser.getName() + " " + mUser.getSurname()
                                            + ", " + picturesNumber + " " + getString(R.string.pictures));
                                }
                                //sort the hashmap
                                List<Picture> pictureList = new LinkedList<>(mPictures.values());
                                Collections.sort(pictureList, new Comparator<Picture>() {
                                    //sort from the most recent to the oldest photo
                                    @Override
                                    public int compare(Picture o1, Picture o2) {
                                        return -o1.getTimestamp().compareTo(o2.getTimestamp());
                                    }
                                });

                                final Picture[] pictures = pictureList.toArray(new Picture[picturesNumber]);
                                mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

                                // use this setting to improve performance if you know that changes
                                // in content do not change the layout size of the RecyclerView
                                mRecyclerView.setHasFixedSize(true);

                                mRecyclerView.setItemViewCacheSize(20);
                                mRecyclerView.setDrawingCacheEnabled(true);
                                mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

                                mLayoutManager = new GridLayoutManager(UserActivity.this, NUM_COLUMNS);
                                mRecyclerView.setLayoutManager(mLayoutManager);
//                                int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
//                                mRecyclerView.addItemDecoration(new SpacesItemDecoration(8));
                                mAdapter = new GridAdapter(UserActivity.this, mRecyclerView, pictures);
                                mRecyclerView.setAdapter(mAdapter);

//                                Drawable horizontalDivider = ContextCompat.getDrawable(UserActivity.this, R.drawable.divider);
//                                Drawable verticalDivider = ContextCompat.getDrawable(UserActivity.this, R.drawable.divider);
//
//                                mRecyclerView.setLayoutManager(new GridLayoutManager(UserActivity.this, NUM_COLUMNS));
//
//                                mRecyclerView.addItemDecoration(new GridDividerItemDecoration
//                                        (horizontalDivider, verticalDivider, NUM_COLUMNS));
//
//                                mAdapter = new GridAdapter(UserActivity.this, mRecyclerView, pictures);
//                                mRecyclerView.setAdapter(mAdapter);
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
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
                            String id = child.getKey();
                            String username = (String)child.getValue();
                            mDbManager.insert(id,username);
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

    class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

            outRect.top = space;

            int i = parent.getChildLayoutPosition(view);
            Log.d(TAG, "i = " + i);
            if ((i%3 != 0) && ((i-1)%3 == 0)) {
                outRect.left = space;
                outRect.right = space;
                Log.d(TAG, "SPACE");
            }
        }
    }
}

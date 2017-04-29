package com.project.pervsys.picaround;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.Picture;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import static com.project.pervsys.picaround.utility.Config.*;

public class PictureActivity extends AppCompatActivity {

    private static final String TAG = "PictureActivity";

    private FirebaseUser mUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseRef = null;
    private Picture mPicture;
    private String mPictureId;
    private boolean mLike = false;
    private HashMap<String,String> mLikesList;
    private HashMap<String,String> mViewsList;
    private int mViewsNumber;
    private int mLikesNumber;
    private TextView mViewsTextView;
    private boolean localLike;
    private boolean increasedViews = false;
    private TextView mLikesTextView;
    private TextView mPopularityTextView;
    private TextView mUsername;
    private TextView mDescription;
    private ImageButton mLikeButton;
    private ImageView mPictureView;
    private ImageView mUserIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_picture);

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

        mPictureView = (ImageView) findViewById(R.id.picture);
        mUserIcon = (ImageView) findViewById(R.id.user_icon);
        mUsername = (TextView) findViewById(R.id.username);
        mDescription = (TextView) findViewById(R.id.description);
        mViewsTextView = (TextView) findViewById(R.id.views);
        mLikesTextView = (TextView) findViewById(R.id.likes);
        mPopularityTextView = (TextView) findViewById(R.id.popularity);
        mLikeButton = (ImageButton) findViewById(R.id.like_button);

        Intent intent = getIntent();
        mPictureId = intent.getStringExtra(PICTURE_ID);

        createView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        createView();
    }

    private void createView(){
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(PICTURES).keepSynced(true);
        mDatabaseRef.child(PICTURES).orderByKey().equalTo(mPictureId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot pictureSnap : dataSnapshot.getChildren()) {
                            mPicture = pictureSnap.getValue(Picture.class);
                            mUsername.setText(mPicture.getUsername());
                            mDescription.setText(mPicture.getDescription());

                            mLikesNumber = mPicture.getLikes();
                            mViewsNumber = mPicture.getViews();

                            mViewsList = mPicture.getViewsList();
                            mLikesList = mPicture.getLikesList();

                            if (mViewsList == null)
                                mViewsList = new HashMap<>();
                            if (mLikesList == null)
                                mLikesList = new HashMap<>();

                            if (mUser != null){
                                if(!mViewsList.containsValue(mUser.getUid())) {
                                    mDatabaseRef.child(PICTURES).child(mPictureId).child(VIEWS_LIST).push().setValue(mUser.getUid());
                                    mViewsNumber++;
                                    increasedViews = true;
                                    increaseViews();
                                }
                                if(mLikesList.containsValue(mUser.getUid()))
                                    mLike = true;
                                localLike = mLike;
                            }
                            if (mLike)
                                mLikeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this,
                                        R.color.colorAccent));
                            else
                                mLikeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this,
                                        R.color.secondary_text_black));

                            setTextView(mLikesNumber, mLikesTextView);
                            setTextView(mViewsNumber,mViewsTextView);
                            setPopularity(mPopularityTextView);

                            Picasso.with(PictureActivity.this)
                                    .load(mPicture.getUserIcon())
                                    .into(mUserIcon);

                            Picasso.with(PictureActivity.this)
                                    .load(mPicture.getPath())
                                    .resize(getApplicationContext().getResources().getDisplayMetrics().widthPixels,
                                            getApplicationContext().getResources().getDisplayMetrics().heightPixels)
                                    .centerInside()
                                    .into(mPictureView);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUser != null) {
                    //local update of like
                    if (!localLike) {
                        Log.d(TAG, "local like is false, we are putting a like");
                        //set colour
                        mLikeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this,
                                R.color.colorAccent));
                        localLike = true;
                        mLikesNumber++;
                        Log.d(TAG, "likes number = " + mLikesNumber);
                    } else {
                        Log.d(TAG, "local like is true, we are removing a like");
                        mLikeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this,
                                R.color.secondary_text_black));
                        localLike = false;
                        mLikesNumber--;
                        Log.d(TAG, "likes number = " + mLikesNumber);
                    }
                    setTextView(mLikesNumber, mLikesTextView);
                    setPopularity(mPopularityTextView);
                }
                else {
                    // user not logged
                    AlertDialog.Builder dialog = new AlertDialog.Builder(PictureActivity.this)
                            .setTitle(R.string.login_required)
                            .setMessage(R.string.login_for_like)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(PictureActivity.this, LoginActivity.class);
                                    i.putExtra(PICTURE_ID, mPictureId);
                                    startActivity(i);
                                }
                            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //do nothing
                                }
                            });
                    dialog.show();
                }

            }
        });
    }

    private void increaseViews(){
        mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData mutableData) {
                Picture picture = mutableData.getValue(Picture.class);
                if (picture == null)
                    return Transaction.success(mutableData);
                //take the number of views
                int views = picture.getViews();
                //increase it by one
                picture.setViews(views + 1);
                //store the new views value to db
                mutableData.setValue(picture);
                //add the id to viewsList
                //mutableData.child(VIEWS_LIST).setValue(mUser);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete transaction increaseView , error:" + databaseError);
            }
        });
    }


    private void setPopularity(TextView popularityTextView) {
        String popularityString = getString(R.string.popularity);
        double popularity;
        if (mViewsNumber != 0)
            popularity = ((double) mLikesNumber/ (double) mViewsNumber);
        else
            popularity = 0;
        popularityTextView.setText((int)(popularity*100) + "% " + popularityString);
    }

    private void setTextView(int number, TextView textView){
        if (textView.getId() == R.id.likes) {
            String likesString;
            if (number == 1) likesString = getString(R.string.like);
            else likesString = getString(R.string.likes);
            textView.setText(number + " " + likesString);
        }
        else {
            String viewsString;
            if (number == 1) viewsString = getString(R.string.view);
            else viewsString = getString(R.string.views);
            textView.setText(number + " " + viewsString);
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                Log.i(TAG, "Settings has been selected");
                Toast.makeText(this, "Selected settings", Toast.LENGTH_SHORT).show();
                //Settings activity
                return true;
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
            case R.id.search:
                Log.i(TAG, "Search has been selected");
                Toast.makeText(this, "Selected search", Toast.LENGTH_SHORT).show();
                //Profile activity
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return true;
        }
    }

    private HashMap<String,String> getViewsList(Picture picture){
        final HashMap<String,String> result = new HashMap<>();
        mDatabaseRef.child(PICTURES).child(picture.getId()).child(VIEWS_LIST)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            result.put(child.getKey(), child.getValue().toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        return result;
    }

    private HashMap<String,String> getLikesList(Picture picture){
        final HashMap<String,String> result = new HashMap<>();
        mDatabaseRef.child(PICTURES).child(picture.getId()).child(LIKES_LIST)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            result.put(child.getKey(), child.getValue().toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        return result;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //if the like has been locally updated
        if (mLike != localLike){
            Log.d(TAG, "Like value has been changed");
            //start likes transaction
            updateLikes();
            updatePopularity();
            Log.i(TAG, "Updated likes and popularity");
        }
        else if (increasedViews) {
            updatePopularity();
            Log.i(TAG, "Updated popularity");
        }
    }


    private void updatePopularity(){
        mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Picture picture = mutableData.getValue(Picture.class);
                if (picture == null)
                    return Transaction.success(mutableData);
                //get the number of likes
                mLikesNumber = picture.getLikes();
                //get views
                mViewsNumber = picture.getViews();
                double popularity;
                if (mViewsNumber != 0)
                    popularity = (double) mLikesNumber / (double) mViewsNumber;
                else
                    popularity = 0;
                picture.setPopularity(1 - popularity);
                mutableData.setValue(picture);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete transaction updatePopularity, error:" + databaseError);
            }
        });
    }

    private void updateLikes(){
        mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Picture picture = mutableData.getValue(Picture.class);
                if (picture == null)
                    return Transaction.success(mutableData);
                //get the number of likes
                mLikesNumber = picture.getLikes();
                if(localLike) {
                    //add the new like
                    picture.setLikes(mLikesNumber + 1);
                    picture.addLike(mUser.getUid());
                }
                else {
                    //remove one like
                    picture.setLikes(mLikesNumber - 1);
                    picture.removeLike(mUser.getUid());
                }
                mutableData.setValue(picture);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete Transaction updateLikes, error:" + databaseError);
            }
        });
    }
}
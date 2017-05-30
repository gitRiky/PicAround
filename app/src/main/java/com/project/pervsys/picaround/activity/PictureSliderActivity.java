package com.project.pervsys.picaround.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;

import static com.project.pervsys.picaround.utility.Config.PICTURES;
import static com.project.pervsys.picaround.utility.Config.POSITION;

public class PictureSliderActivity extends FragmentActivity {

    private static final String TAG = "PictureSliderActivity";

    protected FirebaseUser mUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    protected DatabaseReference mDatabaseRef = null;
    protected boolean visible = true;

    // The pager widget, which handles animation and allows swiping horizontally to access previous and next wizard steps.
    private ViewPager mPager;
    // The pager adapter, which provides the pages to the view pager widget
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_slide);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
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
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        Intent i = getIntent();

        Parcelable[] parcelablePictures = i.getParcelableArrayExtra(PICTURES);
        Picture[] pictures = new Picture[parcelablePictures.length];
        for(int j = 0; j < parcelablePictures.length; j++) {
            pictures[j] = (Picture) parcelablePictures[j];
        }
        int position = i.getIntExtra(POSITION, 0);

        // Instantiate a ViewPager and a PagerAdapter
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new PictureSliderAdapter(getSupportFragmentManager(), pictures);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(position);

    }


    private class PictureSliderAdapter extends FragmentStatePagerAdapter {

        private Picture[] pictures;

        public PictureSliderAdapter(FragmentManager fm, Picture[] pictures) {
            super(fm);
            this.pictures = pictures;
        }

        @Override
        public Fragment getItem(int position) {
            return PictureFragment.newInstance(pictures[position]);
        }

        @Override
        public int getCount() {
            return pictures.length;
        }
    }
}

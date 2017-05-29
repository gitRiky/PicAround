package com.project.pervsys.picaround.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.views.GestureImageView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.project.pervsys.picaround.utility.Config.*;

public class PictureFragment extends Fragment {

    private static final String TAG = "PictureFragment";

    private Picture mPicture;
    private String mPictureId;
    private String mPicturePath;
    private int mViewsNumber;
    private int mLikesNumber;
    private HashMap<String,Boolean> mViewsList;
    private HashMap<String,Boolean> mLikesList;
    private ImageButton mLikeButton;
    private TextView mLikesTextView;
    private TextView mViewsTextView;
    private TextView mLocationTextView;
    private boolean mLike = false;
    private boolean localLike;
    private boolean increasedViews = false;
    private String mUserId;
    private PictureSliderActivity mActivity;
    private FirebaseUser mUser;
    private ImageView mUserIconView;
    private RelativeLayout mUserLayout;
    private RelativeLayout mInfoLayout;
    private RelativeLayout mBackgroundGradientLayout;
    private LinearLayout mLocationLayout;
    private boolean visible = false;
    private boolean created = false;
    private String mDescription;
    private DatabaseReference mDatabaseRef;

    public static PictureFragment newInstance(Picture picture){
        PictureFragment fragment = new PictureFragment();

        Bundle args = new Bundle();
        args.putParcelable(PICTURE, picture);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (PictureSliderActivity)getActivity();
        mUser = mActivity.mUser;
        if (mUser != null)
            mUserId = mUser.getUid();
        created = true;
        mDatabaseRef = mActivity.mDatabaseRef;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_picture, container, false);

        final RelativeLayout transitionContainer = (RelativeLayout) rootView.findViewById(R.id.transition_container);
        mUserLayout = (RelativeLayout) rootView.findViewById(R.id.user);
        mInfoLayout = (RelativeLayout) rootView.findViewById(R.id.info);
        mBackgroundGradientLayout = (RelativeLayout) rootView.findViewById(R.id.background_gradient);
        mLocationLayout = (LinearLayout) rootView.findViewById(R.id.location_layout);


        mUserIconView = (ImageView) rootView.findViewById(R.id.user_icon);
        TextView usernameView = (TextView) rootView.findViewById(R.id.username);
        TextView descriptionView = (TextView) rootView.findViewById(R.id.description);
        mViewsTextView = (TextView) rootView.findViewById(R.id.views);
        mLikesTextView = (TextView) rootView.findViewById(R.id.likes);
        mLocationTextView = (TextView) rootView.findViewById(R.id.location);
        mLikeButton = (ImageButton) rootView.findViewById(R.id.like_button);

        final GestureImageView pictureView = (GestureImageView) rootView.findViewById(R.id.picture);
        pictureView.getController().getSettings()
                .setGravity(Gravity.CENTER);
        pictureView.getController().setOnGesturesListener(new GestureController.OnGestureListener() {
            @Override
            public void onDown(@NonNull MotionEvent e) {}

            @Override
            public void onUpOrCancel(@NonNull MotionEvent e) {}

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(transitionContainer);
                }
                mActivity.visible = !mActivity.visible;
                mUserLayout.setVisibility(mActivity.visible ? View.VISIBLE : View.INVISIBLE);
                mInfoLayout.setVisibility(mActivity.visible ? View.VISIBLE : View.INVISIBLE);
                mBackgroundGradientLayout.setVisibility(mActivity.visible ? View.VISIBLE : View.INVISIBLE);
                return false;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                mActivity.openContextMenu(pictureView);
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                return false;
            }
        });
        pictureView.getController().setLongPressEnabled(true);
        registerForContextMenu(pictureView);

        Bundle bundle = getArguments();
        final Picture picture = bundle.getParcelable(PICTURE);

        mPictureId = picture.getId();
        mPicturePath = picture.getPath();
        String username = picture.getUsername();
        mDescription = picture.getDescription();

        descriptionView.setText(mDescription);
        usernameView.setText(username);
        usernameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mActivity, UserActivity.class);
                i.putExtra(USER_ID, picture.getUserId());
                startActivity(i);
            }
        });

        mActivity.mDatabaseRef.child(PICTURES).keepSynced(true);
        mActivity.mDatabaseRef.child(PICTURES).child(mPictureId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        mPicture = dataSnapshot.getValue(Picture.class);

                        Picasso.with(getContext())
                                .load(mPicture.getUserIcon())
                                .into(mUserIconView);

                        mLocationLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Create a Uri from an intent string. Use the result to create an Intent.
                                Uri gmmIntentUri = Uri.parse("google.streetview:cbll="
                                        + mPicture.getLat() + "," + mPicture.getLon());

                                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                // Make the Intent explicit by setting the Google Maps package
                                mapIntent.setPackage("com.google.android.apps.maps");

                                // Attempt to start an activity that can handle the Intent
                                startActivity(mapIntent);
                            }
                        });

                        mViewsList = mPicture.getViewsList();
                        mLikesList = mPicture.getLikesList();
                        mViewsNumber = mPicture.getViews();
                        mLikesNumber = mPicture.getLikes();

                        if (mViewsList == null)
                            mViewsList = new HashMap<>();
                        if (mLikesList == null)
                            mLikesList = new HashMap<>();

                        if (mUser != null){
                            if (visible)
                                checkViews();
                            if (mLikesList.containsKey(mUser.getUid())){
                                mLike = true;
                            }
                            localLike = mLike;
                        }
                        if (mLike)
                            mLikeButton.setColorFilter(ContextCompat.getColor(getContext(),
                                    R.color.colorAccent));
                        else
                            mLikeButton.setColorFilter(ContextCompat.getColor(getContext(),
                                    R.color.white));
                        mLikeButton.setVisibility(View.VISIBLE);

                        setTextView(mLikesNumber, mLikesTextView);
                        setTextView(mViewsNumber, mViewsTextView);
                        mLocationTextView.setText(reverseGeocode(picture.getLat(), picture.getLon()));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });


        Picasso.with(getContext())
                .load(mPicturePath)
                .resize(getApplicationContext().getResources().getDisplayMetrics().widthPixels,
                        getApplicationContext().getResources().getDisplayMetrics().heightPixels)
                .centerInside()
                .into(pictureView);

        mLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUser != null){
                    // local update of like
                    if (!localLike){
                        Log.d(TAG, "localLike is false, we are putting a like");
                        mLikeButton.setColorFilter(ContextCompat.getColor(getContext(),
                                R.color.colorAccent));
                        localLike = true;
                        mLikesNumber++;
                    }
                    else {
                        Log.d(TAG, "localLike is true, we are removing a like");
                        mLikeButton.setColorFilter(ContextCompat.getColor(getContext(),
                                R.color.white));
                        localLike = false;
                        mLikesNumber--;
                    }
                    setTextView(mLikesNumber, mLikesTextView);
                    mLocationTextView.setText(reverseGeocode(picture.getLat(), picture.getLon()));
                }
                else {
                    // user not logged
                    AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.login_required)
                            .setMessage(R.string.login_for_like)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(mActivity, LoginActivity.class);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
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

        return rootView;
    }


    @Override
    public void onResume(){
        super.onResume();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private void checkViews() {
        if (!mViewsList.containsKey(mUser.getUid())){
            Log.i(TAG, "Increment views");
            mViewsNumber++;
            increasedViews = true;

            mViewsList.put(mUserId, true);
            increaseViews();
            setTextView(mViewsNumber, mViewsTextView);
        }
        Log.d(TAG, mDescription + ": created=" + created + ", visible=" + visible + ", mActivity.visible=" + mActivity.visible);
        if (created && visible && mActivity.visible) {
            mUserLayout.setVisibility(View.VISIBLE);
            mInfoLayout.setVisibility(View.VISIBLE);
            mBackgroundGradientLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            visible = true;
            if (created && mUser != null)
                checkViews();
        } else {
            visible = false;
        }
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
        created = false;
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

    private void increaseViews(){
        mActivity.mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData mutableData) {
                Picture picture = mutableData.getValue(Picture.class);
                if (picture == null)
                    return Transaction.success(mutableData);
                //take the number of views
                int views = picture.getViews();
                //increase it by one
                picture.setViews(views + 1);
                picture.addView(mUserId);
                //store the new views value to db
                mutableData.setValue(picture);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete transaction increaseView , error:" + databaseError);
            }
        });
    }

    private void updatePopularity(){
        mActivity.mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Picture picture = mutableData.getValue(Picture.class);
                if (picture == null)
                    return Transaction.success(mutableData);
                Log.d(TAG, "PICTURE: " + picture);
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
                // update popularity in places or points
                Log.d(TAG, "UPDATE POPULARITY IN PLACE? " + picture.isInPlace());
                Log.d(TAG, "POINT/PLACE ID = " + picture.getPointId());
                if (picture.isInPlace()){
                    DatabaseReference placesRef = mActivity.mDatabaseRef
                            .child(PLACES).child(picture.getPointId());
                    placesRef.child(PICTURES).child(mPictureId).setValue(picture);
                    //update place's popularity
                    placesRef.child(POPULARITY).setValue(1 - popularity);
                }
                else {
                    DatabaseReference pointsRef = mActivity.mDatabaseRef
                            .child(POINTS).child(picture.getPointId())
                            .child(PICTURES).child(mPictureId);
                    pointsRef.setValue(picture);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.d(TAG, "onComplete transaction updatePopularity, error:" + databaseError);
            }
        });

    }

    private void updateLikes(){
        mActivity.mDatabaseRef.child(PICTURES).child(mPictureId).runTransaction(new Transaction.Handler() {
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
                    picture.addLike(mUserId);
                }
                else {
                    //remove one like
                    picture.setLikes(mLikesNumber - 1);
                    picture.removeLike(mUserId);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.context_picture_menu, menu);
        if (mPicture.getUserId().equals(mUserId))
            menu.findItem(R.id.delete_picture).setVisible(true);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_picture:
                Log.i(TAG, "Selected delete");
                startDeleteDialog();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private String reverseGeocode(double lat, double lon) {
        Geocoder geocoder = new Geocoder(mActivity, Locale.getDefault());
        List<Address> addresses = null;

        String addressFragment = "";
        try {
            addresses = geocoder.getFromLocation(
                    lat,
                    lon,
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            addressFragment = getString(R.string.address_not_found);
            return addressFragment;
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            addressFragment = getString(R.string.address_not_found);
            return addressFragment;
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            Log.e(TAG, "ERROR in reverseGeocode, address not found");
            addressFragment = getString(R.string.address_not_found);
        }
        else {
            Address address = addresses.get(0);
            addressFragment = address.getLocality() + ", " + address.getCountryName();

            Log.i(TAG, "Address Found");
        }
        return addressFragment;
    }

    private void startDeleteDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.delete_picture)
                .setMessage(R.string.delete_picture_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "The current picture will be removed");
                        deletePicture();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
        dialog.show();
    }

    private void deletePicture(){

        //delete picture in places or in points
        if (mPicture.isInPlace()){
            final DatabaseReference placesRef = mDatabaseRef.child(PLACES);
            placesRef.child(mPicture.getPointId()).child(PICTURES).child(mPictureId).setValue(null)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //once deleted the picture, the associated place will be empty, so delete it
                            placesRef.child(mPicture.getPointId()).setValue(null);
                        }
                    });
        }
        else {
            DatabaseReference pointsRef = mDatabaseRef.child(POINTS);
            pointsRef.child(mPicture.getPointId()).child(PICTURES).child(mPictureId).setValue(null);
        }

        //delete picture in user
        DatabaseReference usersRef = mDatabaseRef.child(USERS);
        usersRef.child(mUserId).child(PICTURES).child(mPictureId).setValue(null);

        //delete picture in pictures
        DatabaseReference picturesRef = mDatabaseRef.child(PICTURES);
        picturesRef.child(mPictureId).setValue(null);

        //delete picture from the storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String picName = mPicture.getName();
        storageReference.child(picName).delete();
        storageReference.child(THUMB_PREFIX + picName).delete();
        storageReference.child(INTERM_PREFIX + picName).delete();
        Log.i(TAG, "Image deleted");
        Toast.makeText(getContext(), getString(R.string.delete_picture_ok),
                Toast.LENGTH_LONG).show();

        //start the mapsActivity
        NavUtils.navigateUpFromSameTask(mActivity);
    }
}

package com.project.pervsys.picaround.activity;

import android.content.DialogInterface;
import android.content.Intent;
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

import java.util.HashMap;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.project.pervsys.picaround.utility.Config.*;

public class PictureFragment extends Fragment {

    private static final String TAG = "PictureFragment";

    private Picture mPicture;
    private String mPictureId;
    private int mViewsNumber;
    private int mLikesNumber;
    private HashMap<String,String> mViewsList;
    private HashMap<String,String> mLikesList;
    private ImageButton mLikeButton;
    private TextView mLikesTextView;
    private TextView mViewsTextView;
    private TextView mPopularityTextView;
    private boolean mLike = false;
    private boolean localLike;
    private boolean increasedViews = false;
    private String mUserId;
    private PictureSliderActivity mActivity;
    private FirebaseUser mUser;
    private RelativeLayout mUserLayout;
    private RelativeLayout mInfoLayout;
    private boolean visible = false;
    private boolean created = false;
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

        ImageView userIcon = (ImageView) rootView.findViewById(R.id.user_icon);
        TextView usernameView = (TextView) rootView.findViewById(R.id.username);
        TextView descriptionView = (TextView) rootView.findViewById(R.id.description);
        mViewsTextView = (TextView) rootView.findViewById(R.id.views);
        mLikesTextView = (TextView) rootView.findViewById(R.id.likes);
        mPopularityTextView = (TextView) rootView.findViewById(R.id.popularity);
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
        Picture picture = bundle.getParcelable(PICTURE);

        mPictureId = picture.getId();
        String picturePath = picture.getPath();
        String userIconPath = picture.getUserIcon();
        String username = picture.getUsername();
        String description = picture.getDescription();

        descriptionView.setText(description);
        usernameView.setText(username);
        usernameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mActivity, UserActivity.class);
                i.putExtra(USER_ID, mUserId);
                startActivity(i);
            }
        });

        mActivity.mDatabaseRef.child(PICTURES).keepSynced(true);
        mActivity.mDatabaseRef.child(PICTURES).child(mPictureId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        mPicture = dataSnapshot.getValue(Picture.class);
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
                            if (mLikesList.containsValue(mUser.getUid())){
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
                        setPopularity();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        Picasso.with(getContext())
                .load(userIconPath)
                .into(userIcon);

        Picasso.with(getContext())
                .load(picturePath)
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
                    setPopularity();
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

    private void checkViews() {
        if (!mViewsList.containsValue(mUser.getUid())){
            Log.i(TAG, "Increment views");
            mViewsNumber++;
            increasedViews = true;

            mViewsList.put(mUserId, mUserId);
            increaseViews();
            setTextView(mViewsNumber, mViewsTextView);
        }
        if (created && visible && mActivity.visible) {
            mUserLayout.setVisibility(View.VISIBLE);
            mInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            visible = true;
            if (created)
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

    private void setPopularity() {
        String popularityString = getString(R.string.popularity);
        double popularity;
        if (mViewsNumber != 0)
            popularity = ((double) mLikesNumber/ (double) mViewsNumber);
        else
            popularity = 0;
        mPopularityTextView.setText((int)(popularity*100) + "% " + popularityString);
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
                            .child(PLACES).child(picture.getPointId())
                            .child(PICTURE).child(mPictureId);
                    placesRef.setValue(picture);
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

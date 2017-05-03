package com.project.pervsys.picaround;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Point;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.project.pervsys.picaround.utility.Config.NUM_COLUMN_INFOWINDOW;
import static com.project.pervsys.picaround.utility.Config.PICTURES;
import static com.project.pervsys.picaround.utility.Config.POINTS;
import static com.project.pervsys.picaround.utility.Config.POPULARITY;
import static com.project.pervsys.picaround.utility.Config.THUMBNAILS_NUMBER;
import static com.project.pervsys.picaround.utility.Config.THUMB_PREFIX;


public class InfoWindowView extends GridLayout {

    private static final String TAG = "InfoWindowView";

    public InfoWindowView(Context context, Marker marker, Point point) {
        super(context);
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, this.getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, this.getResources().getDisplayMetrics());
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
        setColumnCount(NUM_COLUMN_INFOWINDOW);

        addPictures(context,marker,point,layoutParams);
    }

    private void addPictures(final Context context, final Marker marker, Point point, final LinearLayout.LayoutParams layoutParams) {

        final MarkerCallback mc = new MarkerCallback(marker, context);

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(POINTS).keepSynced(true);

        Query photos;
        photos = databaseRef.child(POINTS).child(point.getId()).child(PICTURES)
                .orderByChild(POPULARITY).limitToFirst(THUMBNAILS_NUMBER);

        photos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int nThumb = (int) dataSnapshot.getChildrenCount();
                int theCounter = THUMBNAILS_NUMBER;
                if (nThumb < THUMBNAILS_NUMBER)
                    theCounter = nThumb;
                mc.setCounter(theCounter);

                for(DataSnapshot photoSnap : dataSnapshot.getChildren()){
                    Picture picture = photoSnap.getValue(Picture.class);
                    String pictureName = picture.getName();
                    // TODO: removed thumbnails feature
                    String thumbnailName = /*THUMB_PREFIX +*/ pictureName;
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference pathReference = storage.getReference().child(thumbnailName);
                    Log.i(TAG, "storageRef=" + pathReference);

                    pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Log.i(TAG, "onSuccess executed");
                            ImageView iv = new ImageView(context);
                            iv.setLayoutParams(layoutParams);
                            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            addView(iv);

                            Picasso.with(context)
                                    .load(uri)
                                    .into(iv, mc);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.i(TAG, "onFailure executed");
                            Log.e(TAG, exception.toString());
                            // TODO: very naive
                            mc.onSuccess();
                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: what to do if the call fails?
            }
        });
    }
}

class MarkerCallback implements Callback {

    private Marker marker = null;
    private int counter;
    private Context context;

    private static final int WAIT_FOR_RELOAD_INFO_WINDOW = 250;

    MarkerCallback(Marker marker, Context context) {
        this.marker=marker;
        this.counter = THUMBNAILS_NUMBER;
        this.context = context;
    }

    @Override
    public void onError() {
        Log.e(getClass().getSimpleName(), "Error loading thumbnail!");
    }

    @Override
    public void onSuccess() {
        counter--;
        //Toast.makeText(context, "The counter is: " + counter, Toast.LENGTH_SHORT).show();
        if (counter == 0 && marker != null && marker.isInfoWindowShown()) {
            //Toast.makeText(context, "Picasso callback done", Toast.LENGTH_SHORT).show();
            marker.hideInfoWindow();
            try {
                Thread.sleep(WAIT_FOR_RELOAD_INFO_WINDOW);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            marker.showInfoWindow();
        }
    }

    void setCounter(int counter) {
        this.counter = counter;
    }
}

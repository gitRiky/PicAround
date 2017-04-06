package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.Point;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PointActivity extends AppCompatActivity {

    private static final String TAG = "MapsActivity";
    private static final String POINT_ID = "pointId";
    private DatabaseReference mDatabaseRef = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point);

        // Set toolbar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.point_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Intent intent = getIntent();
        String pointId = intent.getStringExtra(POINT_ID);

        final TextView pointNameView = (TextView) findViewById(R.id.point_name);
        final TextView pointAddressView = (TextView) findViewById(R.id.point_address);
        final TextView pointDescriptionView = (TextView) findViewById(R.id.point_description);
        final GridLayout pointPictures = (GridLayout) findViewById(R.id.point_pictures);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();

        mDatabaseRef.child("points").orderByKey().equalTo(pointId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        Point point = child.getValue(Point.class);
                        pointNameView.setText(point.getName());
                        pointAddressView.setText(point.getCategory());
                        pointDescriptionView.setText(point.getDescription());
                        addPictures(point, pointPictures);
                        addPictures(point, pointPictures);
                        addPictures(point, pointPictures);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //database error, e.g. permission denied (not logged with Firebase)
                    Log.e(TAG, databaseError.toString());
                }
            });

    }

    private void addPictures(Point point, GridLayout gridLayout) {
        int dim = gridLayout.getWidth()/3;
//        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160, this.getResources().getDisplayMetrics());
//        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 109, this.getResources().getDisplayMetrics());
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
//        Log.i(TAG, "width2=" + width);

        List<Picture> pictures = point.getPictures();
        for (Picture pic : pictures) {
            ImageView imageView = new ImageView(this);

            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            gridLayout.addView(imageView);

            String path = pic.getPath();

            Log.i(TAG, path);

            Picasso.with(this)
                    .load(path)
                    .into(imageView);
        }
    }
}

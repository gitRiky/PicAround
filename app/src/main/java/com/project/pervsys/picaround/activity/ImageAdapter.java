package com.project.pervsys.picaround.activity;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.pervsys.picaround.domain.Picture;
import com.squareup.picasso.Picasso;
import static com.project.pervsys.picaround.utility.Config.*;
import java.util.HashMap;

public class ImageAdapter extends BaseAdapter{

    private static final int GRID_SPACE = 24;
    private Context mContext;
    private HashMap<String,Picture> mPictures;
    private String[] mKeys;
    private String mPath;
    private static final String TAG = "ImageAdapter";

    public ImageAdapter(Context context, HashMap<String,Picture> pictures) {
        this.mContext = context;
        this.mPictures = pictures;
        this.mKeys = mPictures.keySet().toArray(new String[pictures.size()]);
    }

    @Override
    public int getCount() {
        return mPictures.size();
    }

    @Override
    public Object getItem(int i) {
        return mPictures.get(mKeys[i]);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        Picture picture = (Picture) ((GridView) viewGroup).getItemAtPosition(i);
        final ImageView imageView;

        if (view == null){
            imageView = new ImageView(mContext);
            int dim = (viewGroup.getWidth()-GRID_SPACE)/3;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dim, dim);
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        else
            imageView = (ImageView) view;

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference pathReference = storage.getReference()
                .child(INTERM_PREFIX + picture.getName());
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                mPath = uri.toString();
                Picasso.with(mContext)
                        .load(mPath)
                        .fit()
                        .centerInside()
                        .into(imageView);
            }
        });

        return imageView;
    }

}

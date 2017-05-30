package com.project.pervsys.picaround.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.project.pervsys.picaround.domain.Picture;
import com.squareup.picasso.Picasso;

import static com.project.pervsys.picaround.utility.Config.PICTURES;
import static com.project.pervsys.picaround.utility.Config.POSITION;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    private Context mContext;
    private RecyclerView mRecyclerView;
    private Picture[] mPictures;
    private final View.OnClickListener mOnClickListener = new MyOnClickListener();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;

        public ViewHolder(ImageView v) {
            super(v);
            mImageView = v;
        }
    }

    public GridAdapter(Context context, RecyclerView recyclerView, Picture[] pictures) {
        this.mContext = context;
        this.mRecyclerView = recyclerView;
        this.mPictures = pictures;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public GridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
//        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_view_layout, parent, false);
        ImageView imageView = new ImageView(mContext);
        ViewHolder viewHolder = new ViewHolder(imageView);
//        ImageView imageView = (ImageView) v.findViewById(R.id.image_view);
//        ImageView imageView = new ImageView(mContext);
        int dim = (parent.getWidth())/3;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dim, dim);
        imageView.setLayoutParams(layoutParams);
        imageView.setOnClickListener(mOnClickListener);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Picture picture = mPictures[position];
        Picasso.with(mContext)
                .load(picture.getPath())
                .fit()
                .centerCrop()
                .into(holder.mImageView);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mPictures.length;
    }

    class MyOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            int itemPosition = mRecyclerView.getChildLayoutPosition(view);
            Intent i = new Intent(mContext, PictureSliderActivity.class);
            i.putExtra(PICTURES, mPictures);
            i.putExtra(POSITION, itemPosition);
            mContext.startActivity(i);
        }
    }
}

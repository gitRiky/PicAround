package com.project.pervsys.picaround.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.Picture;
import com.project.pervsys.picaround.domain.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import static com.project.pervsys.picaround.utility.Config.*;

public class SearchAdapter extends BaseAdapter {

    private final static String TAG = "SearchAdapter";

    private Context mContext;
    private ArrayList<String> mUsernames;
    private LayoutInflater mLayoutInflater;
    private boolean mIsFilterList;

    public SearchAdapter(Context context, ArrayList<String> usernames, boolean isFilterList) {
        this.mContext = context;
        this.mUsernames = usernames;
        this.mIsFilterList = isFilterList;
    }

    public void updateList(ArrayList<String> filterList, boolean isFilterList) {
        this.mUsernames = filterList;
        this.mIsFilterList = isFilterList;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mUsernames.size();
    }

    @Override
    public String getItem(int position) {
        return mUsernames.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder = null;
        if(v == null){
            holder = new ViewHolder();

            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = mLayoutInflater.inflate(R.layout.list_item_search, parent, false);

            holder.username = (TextView) v.findViewById(R.id.username);
            holder.userIcon = (ImageView) v.findViewById(R.id.user_icon);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        String username = mUsernames.get(position);

        holder.username.setText(username);

        DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        final ViewHolder finalHolder = holder;
        mDatabaseRef.child(USERS).orderByChild(USERNAME).equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot userSnap : dataSnapshot.getChildren()) {
                            User user = userSnap.getValue(User.class);

                            Picasso.with(mContext)
                                    .load(user.getProfilePicture())
                                    .into(finalHolder.userIcon);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

//        Drawable searchDrawable,recentDrawable;
//
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//            searchDrawable = mContext.getResources().getDrawable(R.drawable.ic_magnify_grey600_24dp, null);
//            recentDrawable = mContext.getResources().getDrawable(R.drawable.ic_backup_restore_grey600_24dp, null);
//
//        } else {
//            searchDrawable = mContext.getResources().getDrawable(R.drawable.ic_magnify_grey600_24dp);
//            recentDrawable = mContext.getResources().getDrawable(R.drawable.ic_backup_restore_grey600_24dp);
//        }
//        if(mIsFilterList) {
//            holder.txtCountry.setCompoundDrawablesWithIntrinsicBounds(searchDrawable, null, null, null);
//        }else {
//            holder.txtCountry.setCompoundDrawablesWithIntrinsicBounds(recentDrawable, null, null, null);
//
//        }
        return v;
    }

}

class ViewHolder {
    ImageView userIcon;
    TextView username;
}
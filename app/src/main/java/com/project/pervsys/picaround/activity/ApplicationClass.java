package com.project.pervsys.picaround.activity;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.project.pervsys.picaround.R;

public class ApplicationClass extends Application{
    private static GoogleApiClient mGoogleApiClient;
    private static GoogleSignInResult result;
    private static boolean alreadyEnabledPersistence;
    private static String newProfilePicturePath;
    private static ProgressDialog progress;


    public static void setGoogleApiClient(GoogleApiClient gac){
        mGoogleApiClient = gac;
    }

    public static void setGoogleSignInResult(GoogleSignInResult r){
        result = r;
    }

    public static GoogleApiClient getGoogleApiClient(){
        return mGoogleApiClient;
    }

    public static GoogleSignInResult getGoogleSignInResult(){
        return result;
    }

    public static String getNewProfilePicturePath(){
        return newProfilePicturePath;
    }

    public static void setNewProfilePicturePath(String newPath){
        newProfilePicturePath = newPath;
    }


    public static void setAlreadyEnabledPersistence(boolean state){
        alreadyEnabledPersistence = state;
    }

    public static boolean alreadyEnabledPersistence(){
        return alreadyEnabledPersistence;
    }

    public static void startProgressBar(Context context){
        progress = new ProgressDialog(context);
        progress.setMessage(context.getString(R.string.loading));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCanceledOnTouchOutside(false);
        progress.show();
    }

    public static ProgressDialog getProgress(){
        return progress;
    }


}

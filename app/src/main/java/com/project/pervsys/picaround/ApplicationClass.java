package com.project.pervsys.picaround;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class ApplicationClass extends Application{
    private static GoogleApiClient mGoogleApiClient;
    private static GoogleSignInResult result;

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
}

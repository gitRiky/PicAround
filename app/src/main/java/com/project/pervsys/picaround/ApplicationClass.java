package com.project.pervsys.picaround;

import android.app.Application;
import android.content.Context;
import com.google.android.gms.common.api.GoogleApiClient;

public class ApplicationClass extends Application{
    private static GoogleApiClient mGoogleApiClient;

    public static void setGoogleApiClient(GoogleApiClient gac){
        mGoogleApiClient = gac;
    }

    public static GoogleApiClient getGoogleApiClient(){
        return mGoogleApiClient;
    }
}

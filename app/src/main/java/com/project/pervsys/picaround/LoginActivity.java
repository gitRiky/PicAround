package com.project.pervsys.picaround;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    private CallbackManager callbackManager;
    private TextView textView;
    private GoogleApiClient mGoogleApiClient;
    private LoginButton loginButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /* FACEBOOK LOGIN */

        loginButton = (LoginButton) findViewById(R.id.fb_login_button);
        //email requires explicit permission
        loginButton.setReadPermissions("email");
        textView = (TextView) findViewById(R.id.textView2);
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            private ProfileTracker mProfileTracker;

            @Override
            public void onSuccess(LoginResult loginResult) {

                //It is needed for the profile update
                if (Profile.getCurrentProfile() == null){
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                            mProfileTracker.stopTracking();
                        }
                    };
                }

                //TODO: connection with the db, if it is not already a user, save basic info into db

                //Here we have access to the public profile and the email
                //We can make a GraphRequest for obtaining information (specified in parameters)
                /*GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject me, GraphResponse response) {
                                if (response.getError() != null) {
                                    // handle error
                                } else {
                                    print(me.toString());
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender");
                request.setParameters(parameters);
                request.executeAsync();*/
                finish();
            }

            @Override
            public void onCancel() {
                System.out.println("Cancelled!!");
            }

            @Override
            public void onError(FacebookException exception) {
                System.out.println("Error!!");
            }
        });

        /* GOOGLE LOGIN */

        //require the access to the email and the basic profile info
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        print("ERROR");
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            //start the authentication intent
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                print("Starting activity");
                startActivityForResult(signInIntent, RC_SIGN_IN);
                print("Returning from the onClick");
            }
        });
    }

    public void onClick(View view){
        int id = view.getId();
        switch (id){
            case R.id.fb_fake:
                loginButton.performClick();
                break;
            case R.id.no_login:
                SharedPreferences settings = getSharedPreferences("Log",0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("Logged", false);
                editor.commit();
                finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            print("Passing the result to handleSignIn");
            handleSignInResult(result);

        }
        else
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //maybe it could be integrated to onActivityResult
    private void handleSignInResult(GoogleSignInResult result) {
        print(result.getStatus().toString());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            System.out.println("YESSS");
            textView.setText(acct.getEmail() + " " + acct.getGivenName() + "" + acct.getFamilyName());
            //TODO: connection with the db, if it is not already a user, save basic info into db
            finish();
        } else {
            // Signed out, show unauthenticated UI.
            System.out.println("Not authenticated");
        }
    }

    private void print(String s){
        System.out.println(s);
    }
}

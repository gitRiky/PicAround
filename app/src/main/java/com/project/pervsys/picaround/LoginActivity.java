package com.project.pervsys.picaround;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.project.pervsys.picaround.utility.Config;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    private static final int RC_LINK = 2;
    private static final String EMAIL = "email";
    private static final String FIELDS = "fields";
    private static final String NEEDED_FB_INFO = "name,email";
    private static final String TAG = "LoginActivity";
    private CallbackManager callbackManager;
    private GoogleApiClient mGoogleApiClient;
    private LoginButton loginButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private AuthCredential facebookCredentialToLink;
    private String facebookEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        String logged = getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE)
                .getString(Config.LOG_PREF_INFO,null);
        if (Profile.getCurrentProfile() == null){
             /* FACEBOOK LOGIN */
            setUpFbLogin();
            /* GOOGLE LOGIN */
            setUpGoogleLogin();
        }
        else {
            setLogged(Config.FB_LOGGED);
            Log.i(TAG, "Logged with Facebook");
            startMain();
        }

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    startMain();
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    @Override
    public void onStart(){
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);
        String logged = getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE)
                .getString(Config.LOG_PREF_INFO,null);
        if(logged == null){
            OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi
                    .silentSignIn(mGoogleApiClient);
            if (opr.isDone()) {
                GoogleSignInResult result = opr.get();
                handleSignInResult(result);
            }
        }
        else if (logged == Config.FB_LOGGED){
            handleFacebookAccessToken(AccessToken.getCurrentAccessToken());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void onClick(View view){
        int id = view.getId();
        switch (id){
            case R.id.fb_fake:
                loginButton.performClick();
                break;
            case R.id.no_login:
                setLogged(Config.NOT_LOGGED);
                Log.i(TAG, "Not Logged");
                startMain();
        }
    }

    private void setUpGoogleLogin(){

        //require the access to the email and the basic profile info
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.w(TAG, "Error during the Google Login");
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
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        else if (requestCode == RC_LINK) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleLinkResult(result);
        }
        else
            callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //maybe it could be integrated to onActivityResult
    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            ApplicationClass.setGoogleApiClient(mGoogleApiClient);
            GoogleSignInAccount acct = result.getSignInAccount();
            firebaseAuthWithGoogle(acct);
            Log.i(TAG, "Logged with Google");
            setLogged(Config.GOOGLE_LOGGED);
        } else {
            Toast.makeText(this, R.string.auth_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setUpFbLogin(){
        loginButton = (LoginButton) findViewById(R.id.fb_login_button);
        //email requires explicit permission
        loginButton.setReadPermissions(EMAIL);
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            private ProfileTracker mProfileTracker;

            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
                //It is needed for the profile update
                if (Profile.getCurrentProfile() == null){
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                            mProfileTracker.stopTracking();
                        }
                    };
                }
                Log.i(TAG, "Logged with Facebook");
                setLogged(Config.FB_LOGGED);
                //Here we have access to the public profile and the email
                //We can make a GraphRequest for obtaining information (specified in parameters)
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject me, GraphResponse response) {
                                if (response.getError() != null) {
                                    // handle error
                                    Log.e(TAG, "Error during the graph request");
                                } else {
                                    try {
                                        facebookEmail = me.getString(EMAIL);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString(FIELDS, NEEDED_FB_INFO);
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.i(TAG, "Error during the Facebook Login");
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        facebookCredentialToLink = credential;
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            if (task.getException().getClass() == FirebaseAuthUserCollisionException.class) {
                                AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle(R.string.registered_email)
                                        .setMessage(R.string.link_account_message)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                linkGoogleWithFacebook();
                                            }
                                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                //do nothing
                                            }
                                        });
                                dialog.show();
                            }
                            else {
                                Log.w(TAG, "signInWithCredential", task.getException());
                                Toast.makeText(LoginActivity.this, R.string.auth_error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            startMain();
                        }
                    }
                });
    }

    private void linkGoogleWithFacebook(){
        Log.i(TAG, "Linking Google and Facebook accounts");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_LINK);
    }

    private void handleLinkResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            ApplicationClass.setGoogleApiClient(mGoogleApiClient);
            GoogleSignInAccount acct = result.getSignInAccount();
            if(acct.getEmail().equals(facebookEmail)) {
                firebaseLinkWithGoogle(acct);
                Log.i(TAG, "Correctly logged with Google for the linking");
            }
            else {
                Toast.makeText(LoginActivity.this,
                        R.string.auth_error_link,
                        Toast.LENGTH_LONG).show();

                // Logout from the wrong Google account
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.i(TAG, "Logout from the wrong Google account");
                                    getSharedPreferences(Config.LOG_PREFERENCES, MODE_PRIVATE)
                                            .edit()
                                            .putString(Config.LOG_PREF_INFO, Config.NOT_LOGGED)
                                            .apply();
                                    ApplicationClass.setGoogleApiClient(null);
                                } else
                                    Log.e(TAG, "Error during the Google logout");
                            }
                        });
            }
        } else {
            Toast.makeText(this, R.string.auth_error, Toast.LENGTH_LONG).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.auth_error,
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void firebaseLinkWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseLinkWithGoogle:" + acct.getId());

        final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, R.string.auth_error,
                                    Toast.LENGTH_SHORT).show();
                        }
                        else
                            link();
                    }
                });
    }

    private void link(){
        mAuth.getCurrentUser().linkWithCredential(facebookCredentialToLink)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "linkWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, R.string.auth_error,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, task.getException().toString());
                        }
                        else {
                            Toast.makeText(LoginActivity.this, R.string.auth_ok_link,
                                    Toast.LENGTH_SHORT).show();

                            // logout from the first Google login
                            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                                    new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(Status status) {
                                            if (status.isSuccess()) {
                                                ApplicationClass.setGoogleApiClient(null);
                                            } else
                                                Log.e(TAG, "Error during the Google logout");
                                        }
                                    });
                            startMain();
                        }
                    }
                });
    }

    private void setLogged(String type){
        SharedPreferences settings = getSharedPreferences(Config.LOG_PREFERENCES,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Config.LOG_PREF_INFO, type);
        editor.apply();
    }

    private void startMain(){
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }


}

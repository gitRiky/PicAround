package com.project.pervsys.picaround.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.R;
import com.project.pervsys.picaround.domain.User;

import org.json.JSONException;
import org.json.JSONObject;

import static com.project.pervsys.picaround.utility.Config.DATE;
import static com.project.pervsys.picaround.utility.Config.EMAIL;
import static com.project.pervsys.picaround.utility.Config.FB_LOGGED;
import static com.project.pervsys.picaround.utility.Config.FULL_NAME;
import static com.project.pervsys.picaround.utility.Config.GOOGLE_LOGGED;
import static com.project.pervsys.picaround.utility.Config.LOG_PREFERENCES;
import static com.project.pervsys.picaround.utility.Config.LOG_PREF_INFO;
import static com.project.pervsys.picaround.utility.Config.NOT_LOGGED;
import static com.project.pervsys.picaround.utility.Config.PROFILE_PICTURE;
import static com.project.pervsys.picaround.utility.Config.RC_GET_INFO_FB;
import static com.project.pervsys.picaround.utility.Config.RC_GET_INFO_GOOGLE;
import static com.project.pervsys.picaround.utility.Config.RC_LINK;
import static com.project.pervsys.picaround.utility.Config.RC_SIGN_IN;
import static com.project.pervsys.picaround.utility.Config.USERNAME;
import static com.project.pervsys.picaround.utility.Config.USERNAMES;
import static com.project.pervsys.picaround.utility.Config.USERS;

public class LoginActivity extends AppCompatActivity {


    private static final String FIELDS = "fields";
    private static final String NEEDED_FB_INFO = "name,email";
    private static final String TAG = "LoginActivity";
    public static final int PROFILE_PICTURE_DIM = 200;

    private CallbackManager callbackManager;
    private GoogleApiClient mGoogleApiClient;
    private LoginButton loginButton;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String mUserId;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private AuthCredential facebookCredentialToLink;
    private String facebookEmail;
    private ProgressDialog progress;
    private LoginResult loginRes;
    private boolean firstLog;
    private User newUser;
    private String mUsername;
    private String mDate;
    private DatabaseReference mDatabaseRef;
    private GoogleSignInAccount acct;
    private boolean googleSignInDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /*if (!ApplicationClass.alreadyEnabledPersistence()){
            Log.i(TAG, "Enabling database persistence");
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            ApplicationClass.setAlreadyEnabledPersistence(true);
        }*/

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child(USERS).keepSynced(true);

        if (Profile.getCurrentProfile() == null){
             /* FACEBOOK LOGIN */
            setUpFbLogin();
            /* GOOGLE LOGIN */
            setUpGoogleLogin();
        }
        else
            startProgressBar();
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                // Logged with Facebook
                if (user != null && Profile.getCurrentProfile() != null) {
                    // User is signed in
                    startMain();
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                }
                else if (user != null){
                    if (!googleSignInDone)
                        Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid() + ", performing Silent-signin");
                }
                else {
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

        //already logged with facebook
        if (Profile.getCurrentProfile() != null){
            setLogged(FB_LOGGED);
            Log.i(TAG, "Logged with Facebook");
            handleFacebookAccessToken(AccessToken.getCurrentAccessToken());
        }
        else {
            if (!firstLog) {
                //silent Google sign-in
                OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi
                        .silentSignIn(mGoogleApiClient);
                if (opr.isDone()) {
                    GoogleSignInResult result = opr.get();
                    handleSignInResult(result);
                }
            }
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
            case R.id.no_login:
                setLogged(NOT_LOGGED);
                Log.i(TAG, "Not Logged");
                FirebaseAuth user = FirebaseAuth.getInstance();
                if (user != null){
                    user.signOut();
                    Log.d(TAG, "Sign out from Firebase");
                }
                Intent i = new Intent(getApplicationContext(), MapsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
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

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
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
        switch(requestCode) {

            case RC_SIGN_IN:
                //Google Sign-in
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
                break;
            case RC_LINK:
                //Link between accounts with the same email
                GoogleSignInResult res = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleLinkResult(res);
                break;
            case RC_GET_INFO_FB:
                //return from GetBasicInfoActivity
                if (resultCode == RESULT_OK) {
                    startProgressBar();
                    mUsername = data.getStringExtra(USERNAME);
                    mDate = data.getStringExtra(DATE);
                    if (loginRes != null)
                        handleFacebookAccessToken(loginRes.getAccessToken());
                    else
                        Log.e(TAG, "ERROR!");
                }
                else
                    firstLog = false;
                break;
            case RC_GET_INFO_GOOGLE:
                if (resultCode == RESULT_OK){
                    startProgressBar();
                    mUsername = data.getStringExtra(USERNAME);
                    mDate = data.getStringExtra(DATE);
                    firebaseAuthWithGoogle(acct);
                }
                else
                    firstLog = false;
                break;
            default:
                //Facebook
                callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    //Handle the result of Sign-in with Google
    private void handleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {
            startProgressBar();
            ApplicationClass.setGoogleSignInResult(result);
            ApplicationClass.setGoogleApiClient(mGoogleApiClient);
            acct = result.getSignInAccount();
            checkEmailGoogle(acct);
            Log.i(TAG, "Logged with Google");
            setLogged(GOOGLE_LOGGED);
        } else {
            Toast.makeText(this, R.string.auth_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setUpFbLogin(){
        loginButton = (LoginButton) findViewById(R.id.login_button);
        //email requires explicit permission
        loginButton.setReadPermissions(EMAIL);
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            private ProfileTracker mProfileTracker;

            @Override
            public void onSuccess(final LoginResult loginResult) {
                //It is needed for the profile update
                if (Profile.getCurrentProfile() == null){
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                            mProfileTracker.stopTracking();
                        }
                    };
                }
                startProgressBar();
                Log.i(TAG, "Logged with Facebook");
                setLogged(FB_LOGGED);
                loginRes = loginResult;
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
                                        checkEmailFb(loginResult.getAccessToken());

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


    private void checkEmailFb(final AccessToken token){
        //String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        mDatabaseRef.child(USERS).orderByChild(EMAIL).equalTo(facebookEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // email already registered
                            Log.i(TAG, "User already registered");
                            handleFacebookAccessToken(token);
                        } else {
                            // email not registered
                            Log.i(TAG, "User not registered");
                            Profile profile = Profile.getCurrentProfile();
                            String fullName = profile.getName();
                            String profilePicture = profile.getProfilePictureUri(PROFILE_PICTURE_DIM,PROFILE_PICTURE_DIM)
                                    .toString();
                            Intent i = new Intent(LoginActivity.this, GetBasicInfoActivity.class);
                            i.putExtra(FULL_NAME, fullName);
                            i.putExtra(PROFILE_PICTURE, profilePicture);
                            firstLog = true;
                            if (progress != null)
                                progress.dismiss();
                            startActivityForResult(i, RC_GET_INFO_FB);

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }

    private void checkEmailGoogle(final GoogleSignInAccount acct){
        mDatabaseRef.child(USERS).orderByChild(EMAIL).equalTo(acct.getEmail())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // email already registered
                            Log.i(TAG, "User already registered");
                            firebaseAuthWithGoogle(acct);
                        } else {
                            // email not registered
                            Log.i(TAG, "User not registered");
                            String fullName = acct.getDisplayName();
                            String profilePicture = acct.getPhotoUrl().toString();
                            Intent i = new Intent(LoginActivity.this, GetBasicInfoActivity.class);
                            i.putExtra(FULL_NAME, fullName);
                            i.putExtra(PROFILE_PICTURE, profilePicture);
                            firstLog = true;
                            if (progress != null)
                                progress.dismiss();
                            startActivityForResult(i, RC_GET_INFO_GOOGLE);

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
    }


    //Use the Facebook access token for obtaining credential for Firebase authentication
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        facebookCredentialToLink = credential;
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            //if the email is already registered with Google, ask for the linking
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
                            if (firstLog){
                                Log.i(TAG, "First usage for the user");
                                Profile profile = Profile.getCurrentProfile();
                                mUser = mAuth.getCurrentUser();
                                mUserId = mUser.getUid();
                                newUser = new User(mUsername, facebookEmail, profile.getFirstName(),
                                        profile.getLastName(), mDate,
                                        profile.getProfilePictureUri(PROFILE_PICTURE_DIM,PROFILE_PICTURE_DIM).toString(),
                                        mUserId);

                                mDatabaseRef.child(USERS).child(mUserId).setValue(newUser);
                                mDatabaseRef.child(USERNAMES).child(mUserId).setValue(mUsername.toLowerCase());

                                Log.i(TAG, "User has been registered");
                            }
                            if (progress != null)
                                progress.dismiss();
                        }
                    }
                });
    }


    private void linkGoogleWithFacebook(){
        Log.i(TAG, "Linking Google and Facebook accounts");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_LINK);
    }


    //After another log with google, check if the email is the same as the facebook one
    //if yes, make the linking
    //otherwise, error on the used account (emails are different)
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
                                    getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE)
                                            .edit()
                                            .putString(LOG_PREF_INFO, NOT_LOGGED)
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


    //authenticate the google account with firebase
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
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
                        else{
                            if (firstLog){
                                Log.i(TAG, "First usage for the user");
                                mUser = mAuth.getCurrentUser();
                                mUserId = mUser.getUid();
                                newUser = new User(mUsername, acct.getEmail(), acct.getGivenName(),
                                        acct.getFamilyName(), mDate,
                                        acct.getPhotoUrl().toString(),
                                        mUserId);

                                mDatabaseRef.child(USERS).child(mUserId).setValue(newUser);
                                mDatabaseRef.child(USERNAMES).child(mUserId).setValue(mUsername.toLowerCase());

                                Log.i(TAG, "User has been registered");
                            }
                            if (progress != null)
                                progress.dismiss();
                            startMain();
                        }


                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "OnFailure " + e.toString());
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


    //manage the link
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
                            if (progress != null)
                                progress.dismiss();
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
        SharedPreferences settings = getSharedPreferences(LOG_PREFERENCES,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(LOG_PREF_INFO, type);
        editor.apply();
    }

    private void startMain(){
        Intent i = new Intent(this, MapsActivity.class);
        if (firstLog) {
            i.putExtra(USERNAME, mUsername);
            String log = getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE)
                    .getString(LOG_PREF_INFO, null);
            if (Profile.getCurrentProfile() == null) {
                i.putExtra(PROFILE_PICTURE, acct.getPhotoUrl().toString());
            }
            else {
                i.putExtra(PROFILE_PICTURE,
                        Profile.getCurrentProfile().getProfilePictureUri(PROFILE_PICTURE_DIM, PROFILE_PICTURE_DIM).toString());
            }
        }
        Log.d(TAG, "First log = " + firstLog);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void startProgressBar(){
        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.loading));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCanceledOnTouchOutside(false);
        progress.show();
    }
}

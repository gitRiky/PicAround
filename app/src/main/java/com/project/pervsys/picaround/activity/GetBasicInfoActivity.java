package com.project.pervsys.picaround.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.R;

import static com.project.pervsys.picaround.utility.Config.*;

import java.util.Calendar;

public class GetBasicInfoActivity extends AppCompatActivity {
    private static final int MIN_AGE = 6;
    private final static int MAX_AGE = 95;
    private final static String TAG = "GetBasicInfoActivity";

    private GoogleApiClient mGoogleApiClient;
    private String month;
    private String date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_basic_info);
        // Set toolbar
        Toolbar toolbar  = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.registration);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        String[] days = createDayArray();
        String[] years = createYearArray();
        Spinner daySpin = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, days);
        // Apply the adapter to the spinner
        daySpin.setAdapter(adapter);
        Spinner monthSpin = (Spinner) findViewById(R.id.spinner2);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.months,
                android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        monthSpin.setAdapter(adapter2);
        monthSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                month = "" + (position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
        Spinner yearSpin = (Spinner) findViewById(R.id.spinner3);
        ArrayAdapter<CharSequence> adapter3 = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, years);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        yearSpin.setAdapter(adapter3);

    }


    private String[] createDayArray(){
        String[] days = new String[31];
        for (int i = 0; i < 31; i++)
            days[i] = "" + (i + 1);
        return days;
    }


    private String[] createYearArray(){
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = currentYear - MAX_AGE;
        int maxYear = currentYear - MIN_AGE;
        String[] years = new String[maxYear - minYear];
        for (int i = maxYear, j = 0; i > minYear; i--, j++){
            years[j] = "" + i;
        }
        return years;
    }

    @Override
    public void onBackPressed(){
        prepareLogOut();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.registration_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }



    private boolean checkDate(int d, int m, int y){
        if (d == 31 && (m == 2 || m == 4 || m == 6 || m == 9 || m == 11))
            return false;
        if (m == 2 && d >= 29)
            if (d == 29 && ((y%4 == 0 && y%100 != 0) || (y%400 == 0)))
                return true;
            else
                return false;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch(id){
            case R.id.confirm:
                EditText usernameField = (EditText) findViewById(R.id.username);
                String username = usernameField.getText().toString().toLowerCase();
                Spinner daySpin = (Spinner) findViewById(R.id.spinner);
                Spinner yearSpin = (Spinner) findViewById(R.id.spinner3);
                String day = daySpin.getSelectedItem().toString();
                if (day.length() == 1)
                    day = "0" + day;
                if (month.length() == 1)
                    month = "0" + month;
                String year = yearSpin.getSelectedItem().toString();
                if (checkDate(Integer.parseInt(day), Integer.parseInt(month), Integer.parseInt(year))) {
                    date = year + "/" + month + "/" + day;
                    if (!checkUsername(username)) {
                        usernameField.setText("");
                    }
                }
                else
                    Toast.makeText(this, R.string.invalid_date, Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }


    private boolean checkUsername(final String username){
        if (username.equals("")){
            Toast.makeText(this, R.string.username_missing, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (username.contains(" ")){
            Toast.makeText(this, R.string.username_with_spaces,Toast.LENGTH_SHORT).show();
            return false;
        }
        String lowUsername = username.toLowerCase();

        //query to database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(USERNAMES).orderByValue().equalTo(lowUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // username already used
                            Toast.makeText(getApplicationContext(),
                                    R.string.username_unavailable,
                                    Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Username unavailable");
                        } else {
                            //username not used
                            Log.i(TAG, "Username ok");
                            Intent i = getIntent();
                            i.putExtra(USERNAME, username);
                            i.putExtra(DATE, date);
                            setResult(RESULT_OK, i);
                            Log.i(TAG, "Data sent to LoginActivity");
                            /*Toast.makeText(getApplicationContext(),
                                    R.string.registration_ok,
                                    Toast.LENGTH_SHORT).show();*/
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });
        return true;
    }

    private void prepareLogOut(){
        mGoogleApiClient = ApplicationClass.getGoogleApiClient();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        AlertDialog.Builder dialog = new AlertDialog.Builder(GetBasicInfoActivity.this)
                .setTitle(R.string.exit)
                .setMessage(R.string.registration_exit)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        logOut();
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
        dialog.show();
    }

    private void logOut() {

        // logout Facebook
        if (Profile.getCurrentProfile() != null) {
            LoginManager.getInstance().logOut();
            Log.i(TAG, "Logout from Facebook");
            getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit()
                    .putString(LOG_PREF_INFO, NOT_LOGGED).apply();
            setResult(RESULT_CANCELED);
            finish();
        }
        else{
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Logout from Google");
                            getSharedPreferences(LOG_PREFERENCES, MODE_PRIVATE).edit()
                                    .putString(LOG_PREF_INFO, NOT_LOGGED).apply();
                            ApplicationClass.setGoogleApiClient(null);
                            setResult(RESULT_CANCELED);
                            finish();
                        } else
                            Log.e(TAG, "Error during the Google logout");
                    }
                });
        }
    }
}

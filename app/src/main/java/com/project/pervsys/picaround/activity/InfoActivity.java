package com.project.pervsys.picaround.activity;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import com.project.pervsys.picaround.BuildConfig;
import com.project.pervsys.picaround.R;

import org.w3c.dom.Text;

public class InfoActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            int tab = getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView = null;

            switch(tab){

                case 1:
                    // pick the fragment_info.xml as a View
                    rootView = inflater.inflate(R.layout.fragment_info, container, false);
                    // set the textView
                    TextView appName = (TextView) rootView.findViewById(R.id.info_app_name);
                    appName.setText(getString(R.string.info_section_app_name));

                    TextView appVersion = (TextView) rootView.findViewById(R.id.info_app_version);
                    appVersion.setText(getString(R.string.app_version) + ": " + BuildConfig.VERSION_NAME);

                    TextView appDetails = (TextView) rootView.findViewById(R.id.info_app_details);
                    appDetails.setText(getString(R.string.app_details));

                    break;

                case 2:
                    // pick the fragment_contact_us.xml as a View
                    rootView = inflater.inflate(R.layout.fragment_contact_us, container, false);
                    // set the fab
                    FloatingActionButton  fab = (FloatingActionButton) rootView.findViewById(R.id.fab_fragment_activity);
                    final View finalRootView = rootView;

                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view){
                            EditText message = (EditText) finalRootView.findViewById(R.id.contact_us_message);
                            String text = message.getText().toString();
                            RatingBar rb = (RatingBar) finalRootView.findViewById(R.id.ratingBar);
                            text += "\n\n" +  "The rating is: " + rb.getRating();

                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                    "mailto",getString(R.string.email_address_to_contact), null));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.default_subject_for_email));
                            emailIntent.putExtra(Intent.EXTRA_TEXT, text);
                            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
                        }
                    });

                    TextView contactInfo = (TextView) rootView.findViewById(R.id.contact_info);
                    contactInfo.setText(getString(R.string.contact_info));
                    break;
            }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.info_tab);
                case 1:
                    return getString(R.string.contact_us_tab);
            }
            return null;
        }
    }
}

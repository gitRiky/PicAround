package com.project.pervsys.picaround.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.project.pervsys.picaround.R;

public class TutorialActivity extends AppIntro {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroFragment.newInstance(getString(R.string.welcome_to_picaround),
                getString(R.string.tutorial1_text),
                R.drawable.tutorial1, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial2_title),
                getString(R.string.tutorial2_text),
                R.drawable.tutorial2, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial3_title),
                getString(R.string.tutorial3_text),
                R.drawable.tutorial3, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial4_title),
                getString(R.string.tutorial4_text),
                R.drawable.tutorial4, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial5_title),
                getString(R.string.tutorial5_text),
                R.drawable.tutorial5, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial6_title),
                getString(R.string.tutorial6_text),
                R.drawable.tutorial6, getColor(R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.tutorial7_title),
                getString(R.string.tutorial7_text),
                R.drawable.tutorial7, getColor(R.color.colorPrimary)));

        setDoneText(getString(R.string.done));
        setSkipText(getString(R.string.skip));
        setSeparatorColor(getColor(R.color.white));
        showSkipButton(true);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }

}
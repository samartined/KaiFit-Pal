package com.tfg.kaifit_pal.utilities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tfg.kaifit_pal.R;

public class AppBarHandler {

    public static void setUpActionBar(@NonNull AppCompatActivity activity, String title, boolean displayHomeAsUpEnabled, boolean displayShowHomeEnabled) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setContentInsetStartWithNavigation(0);
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled);
            actionBar.setDisplayShowHomeEnabled(displayShowHomeEnabled);
            if (displayHomeAsUpEnabled) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow_black);
            }
        }
    }

    public static void resetActionBar(@NonNull AppCompatActivity activity) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle("KaiFit-Pal");
        }
    }

    public static void setTitle(@NonNull AppCompatActivity activity, String title) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }
}
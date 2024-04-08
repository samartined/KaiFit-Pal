package com.tfg.kaifit_pal.utilities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tfg.kaifit_pal.R;

/**
 * Utility class for handling AppBar related operations.
 */
public class AppBarHandler {

    /**
     * Sets up the ActionBar for the given activity.
     *
     * @param activity               The activity where the ActionBar is to be set up.
     * @param title                  The title to be displayed in the ActionBar.
     * @param displayHomeAsUpEnabled Whether the home button should be displayed in the ActionBar.
     * @param displayShowHomeEnabled Whether the home button should be enabled in the ActionBar.
     */
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

    /**
     * Resets the ActionBar for the given activity.
     *
     * @param activity The activity where the ActionBar is to be reset.
     */
    public static void resetActionBar(@NonNull AppCompatActivity activity) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle("KaiFit-Pal");
        }
    }

    /**
     * Sets the title of the ActionBar for the given activity.
     *
     * @param activity The activity where the ActionBar title is to be set.
     * @param title    The title to be displayed in the ActionBar.
     */
    public static void setTitle(@NonNull AppCompatActivity activity, String title) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }
}
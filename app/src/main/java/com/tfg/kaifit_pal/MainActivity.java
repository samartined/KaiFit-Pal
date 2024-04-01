package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tfg.kaifit_pal.fragments.Calculator;
import com.tfg.kaifit_pal.fragments.Help;
import com.tfg.kaifit_pal.fragments.KaiQ;
import com.tfg.kaifit_pal.fragments.Profile;
import com.tfg.kaifit_pal.fragments.Settings;
import com.tfg.kaifit_pal.fragments.TdeeMacros;

/**
 * Main activity class that holds all the fragments.
 */
public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    /**
     * FragmentManager to manage fragments
     */
    private FragmentManager fragmentManager;

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the fragmentManager
        fragmentManager = getSupportFragmentManager();

        // Add the default fragment
        addDefaultFragment(savedInstanceState);

        // Setup bottom navigation view
        setupBottomNavigationView();

    }

    /**
     * Sets up the bottom navigation view and its item selection listener.
     */
    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.user_profile_menu_option) {
                selectedFragment = new Profile();
            } else if (itemId == R.id.calculator_menu_option) {
                selectedFragment = new Calculator();
            } else if (itemId == R.id.assistant_menu_option) {
                selectedFragment = new KaiQ();
            } else if (itemId == R.id.help_info_menu_option) {
                selectedFragment = new Help();
            } else if (itemId == R.id.settings_menu_option) {
                selectedFragment = new Settings();
            } else {
                Log.e("MainActivity", "Invalid itemId: " + itemId);
                return false;
            }

            // Replace the current fragment with the selected one
            fragmentManager.beginTransaction().replace(R.id.fragment_container_view, selectedFragment).commit();
            return true;
        });
    }

    /**
     * Adds the default fragment to the activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment defaultFragment = new Calculator();
            fragmentManager.beginTransaction().add(R.id.fragment_container_view, defaultFragment).commit();

            // Set the selected item in the bottom navigation view to Calculator
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
        }
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the activity has detected the user's press of the back key.
     */
    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Callback for the result from calculating TDEE.
     *
     * @param tdeeResult The result of the TDEE calculation.
     */
    @Override
    public void onCalculateClick(int tdeeResult) {
        Fragment newFragment = fragmentManager.findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TdeeMacros() : new Calculator();

        // We create a bundle to pass the data to the new fragment
        Bundle bundle = new Bundle();
        bundle.putInt("tdeeResult", tdeeResult);
        newFragment.setArguments(bundle);

        fragmentManager.beginTransaction().replace(R.id.fragment_container_view, newFragment).addToBackStack(null).commit();
    }
}
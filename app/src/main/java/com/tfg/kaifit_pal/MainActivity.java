package com.tfg.kaifit_pal;

import androidx.annotation.NonNull;
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
 * MainActivity class that extends AppCompatActivity and implements Calculator.OnCalculateClickListener
 * This class is the main activity of the app, it contains the bottom navigation view and the fragments
 * that are displayed when the user clicks on the bottom navigation view.
 */
public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    Fragment defaultFragment; // Default app fragment
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the action bar
        fragmentManager = getSupportFragmentManager();

        // Set the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        addDefaultFragment(savedInstanceState);

        // Set the fragments exchange stack
        fragmentsExchangeStack();
    }

    private void fragmentsExchangeStack() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            currentFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.user_profile_menu_option) {
                currentFragment = new Profile();
            } else if (itemId == R.id.calculator_menu_option) {
                currentFragment = new Calculator();
            } else if (itemId == R.id.assistant_menu_option) {
                currentFragment = new KaiQ();
            } else if (itemId == R.id.help_info_menu_option) {
                currentFragment = new Help();
            } else if (itemId == R.id.settings_menu_option) {
                currentFragment = new Settings();
            } else {
                Log.e("MainActivity", "Invalid itemId: " + itemId);
                return false;
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, this.currentFragment)
                    .addToBackStack(null)
                    .commit();

            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            defaultFragment = new Calculator();

            fragmentManager.beginTransaction().add(R.id.fragment_container_view, defaultFragment).commit();

            bottomNavigationView = findViewById(R.id.bottom_navigation);

            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option); // Highlight the calculator menu item option
        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onCalculateClick(int tdeeResult) {
        Fragment newFragment = fragmentManager.findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TdeeMacros() : new Calculator();

        Bundle bundle = new Bundle();
        bundle.putInt("tdeeResult", tdeeResult);
        newFragment.setArguments(bundle);

        fragmentManager.beginTransaction().replace(R.id.fragment_container_view, newFragment).addToBackStack(null).commit();
    }
}
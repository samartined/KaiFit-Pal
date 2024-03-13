package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tfg.kaifit_pal.fragments.Calculator;
import com.tfg.kaifit_pal.fragments.Help;
import com.tfg.kaifit_pal.fragments.KaiQ;
import com.tfg.kaifit_pal.fragments.Profile;
import com.tfg.kaifit_pal.fragments.Settings;
import com.tfg.kaifit_pal.fragments.TDEE_Macros;

import java.util.Stack;

public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    // FragmentManager to manage fragments
    private FragmentManager fragmentManager;

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

    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment defaultFragment = new Calculator();
            fragmentManager.beginTransaction().add(R.id.fragment_container_view, defaultFragment).commit();
            
            // Set the selected item in the bottom navigation view to Calculator
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
        }
    }

    @Override
    public void onCalculateClick() {
        Fragment newFragment = fragmentManager.findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TDEE_Macros() : new Calculator();
        fragmentManager.beginTransaction().replace(R.id.fragment_container_view, newFragment).addToBackStack(null).commit();
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
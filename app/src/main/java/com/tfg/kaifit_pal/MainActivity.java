package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tfg.kaifit_pal.fragments.Calculator;
import com.tfg.kaifit_pal.fragments.Help;
import com.tfg.kaifit_pal.fragments.KaiQ;
import com.tfg.kaifit_pal.fragments.Profile;
import com.tfg.kaifit_pal.fragments.Settings;
import com.tfg.kaifit_pal.fragments.TDEE_Macros;

public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    @Override
    // onCreate method
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addDefaultFragment(savedInstanceState);
        setupBottomNavigationView();
    }

    // Method to setup bottom navigation view
    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // First, we equal the selectedFragment to null to avoid any possible errors
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
                // If the itemId is not valid, we log an error and return false
                Log.e("MainActivity", "Invalid itemId: " + itemId);
                return false;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view, selectedFragment).commit();
            return true;
        });
    }
        bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);

    // Method to add default fragment, in this case it will be the calculator
    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {

            // Replaces the fragment_container_view with the Calculator fragment
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view, new Calculator()).commit();

            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
        }
    }

    @Override
    public void onCalculateClick() {
        // We check if the current fragment is the Calculator, if so, we replace it with the TDEE_Macros fragment, and vice versa
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view,
                getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TDEE_Macros() : new Calculator()).commit();
    }
}

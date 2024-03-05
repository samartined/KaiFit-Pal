package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    // onCreate method
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupBottomNavigationView();
        addDefaultFragment(savedInstanceState);
    }

    // Method to setup bottom navigation view
    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
    }

    // Method to add default fragment, in this case it will be the calculator
    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) { // If the fragment is not being restored
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container_view, new Calculator()).commit();
        }
    }
}
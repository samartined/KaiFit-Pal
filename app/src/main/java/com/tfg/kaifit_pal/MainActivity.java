package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tfg.kaifit_pal.fragments.Calculator;
import com.tfg.kaifit_pal.fragments.TDEE_Macros;

public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);

        // We set the initial fragment to the Calculator, and if it is not already created, we create it checking if the savedInstanceState is null
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container_view, new Calculator()).commit();
        }
    }

    @Override
    public void onCalculateClick() {
        // We check if the current fragment is the Calculator, if so, we replace it with the TDEE_Macros fragment, and vice versa
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_view,
                getSupportFragmentManager().findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TDEE_Macros() : new Calculator()).commit();
    }
}
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
 * Main activity class that holds all the fragments.
 */
public class MainActivity extends AppCompatActivity implements Calculator.OnCalculateClickListener {

    /**
     * BottomNavigationView to navigate between fragments
     */
    private BottomNavigationView bottomNavigationView;

    /**
     * FragmentManager to manage fragments
     */
    private FragmentManager fragmentManager;
    Fragment defaultFragment;
    private Fragment currentFragment;

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

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Add the default fragment to the activity
        addDefaultFragment(savedInstanceState);

        // Setup bottom navigation view
        fragmentsExchangeStack();
    }

    /**
     * This method sets up the bottom navigation view and its item selection listener.
     * It is responsible for handling the navigation between different fragments in the application.
     * When a menu item in the bottom navigation view is selected, this method creates an instance of the corresponding fragment,
     * replaces the current fragment with the newly created fragment, and adds the transaction to the back stack.
     */
    private void fragmentsExchangeStack() {
        // Get a reference to the bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set an item selected listener for the bottom navigation view
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Initialize the selected fragment as null
            currentFragment = null;

            // Get the id of the selected item
            int itemId = item.getItemId();

            // Create an instance of the corresponding fragment based on the selected item id
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

            // Replace the current fragment with the selected fragment and add the transaction to the back stack
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, this.currentFragment)
                    .addToBackStack(null)
                    .commit();

            return true;
        });
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is responsible for adding the default fragment to the activity.
     * It is called during the onCreate() lifecycle method of the activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle). Null if this is the first
     *                           time the activity is created.
     */
    private void addDefaultFragment(Bundle savedInstanceState) {
        // Check if this is the first time the activity is created
        if (savedInstanceState == null) {
            // Create an instance of the Calculator fragment which is the default fragment
            defaultFragment = new Calculator();

            // Begin a new transaction, add the default fragment to the container view, and commit the transaction. This makes the Calculator fragment visible in the UI.
            fragmentManager.beginTransaction().add(R.id.fragment_container_view, defaultFragment).commit();

            // Get a reference to the bottom navigation view
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

            // Set the selected item in the bottom navigation view to Calculator. 
            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
        }
    }

    /**
     * This method is called when the back button is pressed.
     * It is responsible for handling the back button press event.
     */
    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * This method is called when the calculate button is clicked in the Calculator fragment.
     * It is responsible for creating a new fragment based on the current fragment in the container view,
     * passing the calculated TDEE result to the new fragment, and replacing the current fragment with the new fragment.
     *
     * @param tdeeResult The calculated Total Daily Energy Expenditure (TDEE) result.
     */
    @Override
    public void onCalculateClick(int tdeeResult) {
        // Check the current fragment in the container view. If it's an instance of Calculator, create a new TdeeMacros fragment. Otherwise, create a new Calculator fragment.
        Fragment newFragment = fragmentManager.findFragmentById(R.id.fragment_container_view) instanceof Calculator ? new TdeeMacros() : new Calculator();

        // Create a new bundle to pass the TDEE result to the new fragment
        Bundle bundle = new Bundle();
        bundle.putInt("tdeeResult", tdeeResult);
        newFragment.setArguments(bundle);

        fragmentManager.beginTransaction().replace(R.id.fragment_container_view, newFragment).addToBackStack(null).commit();
    }
}
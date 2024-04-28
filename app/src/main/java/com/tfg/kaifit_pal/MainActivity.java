package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tfg.kaifit_pal.views.fragments.Calculator;
import com.tfg.kaifit_pal.views.fragments.Help;
import com.tfg.kaifit_pal.views.fragments.Profile;
import com.tfg.kaifit_pal.views.fragments.Settings;
import com.tfg.kaifit_pal.views.fragments.TdeeMacros;
import com.tfg.kaifit_pal.views.fragments.CalculateListenerInterface;
import com.tfg.kaifit_pal.utilities.AppBarHandler;
import com.tfg.kaifit_pal.views.fragments.kaiq.KaiQ;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * MainActivity class that extends AppCompatActivity and implements Calculator.OnCalculateClickListener
 * This class is the main activity of the app, it contains the bottom navigation view and the fragments
 * that are displayed when the user clicks on the bottom navigation view.
 */
public class MainActivity extends AppCompatActivity implements CalculateListenerInterface {

    // Child fragment for TDEE calculations
    private TdeeMacros childFragmentTdee;
    // Bottom navigation view
    private BottomNavigationView bottomNavigationView;
    // Fragment manager for managing fragments
    private FragmentManager fragmentManager;
    // Default fragment to be displayed
    private Fragment defaultFragment;
    // Currently displayed fragment
    private Fragment currentFragment;

    // List of main fragments
    private final List<Class<? extends Fragment>> mainFragments = Arrays.asList(Profile.class, Calculator.class, KaiQ.class, Help.class, Settings.class); // We use a list of fragments to create the fragments HashMap
    // HashMap of main fragments
    private HashMap<String, Fragment> mainFragmentsHashMap = new HashMap<>(); // The key is the fragment class name and the value is the fragment

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the action bar
        fragmentManager = getSupportFragmentManager();

        // Set the main fragments
        mainFragmentsHashMap = new HashMap<>();
        for (Class<? extends Fragment> fragmentClass : mainFragments) {
            try {
                Fragment fragment = fragmentClass.newInstance();
                mainFragmentsHashMap.put(fragmentClass.getSimpleName(), fragment);
            } catch (IllegalAccessException | InstantiationException e) {
                Log.e("MainActivity", "Error creating fragment: " + fragmentClass.getSimpleName(), e);
            }
        }

        AppBarHandler.setUpActionBar(this, "KaiFit-Pal", false, false); // Set the action bar

        // Set the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        addDefaultFragment(savedInstanceState);

        // Set the fragments exchange stack
        fragmentsExchangeStack();
    }

    /**
     * Sets up the fragments exchange stack.
     */
    private void fragmentsExchangeStack() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            String fragmentTag = "";

            if (itemId == R.id.user_profile_menu_option) {
                fragmentTag = "Profile";
            } else if (itemId == R.id.calculator_menu_option) {
                fragmentTag = "Calculator";
            } else if (itemId == R.id.assistant_menu_option) {
                fragmentTag = "KaiQ";
            } else if (itemId == R.id.help_info_menu_option) {
                fragmentTag = "Help";
            } else if (itemId == R.id.settings_menu_option) {
                fragmentTag = "Settings";
            } else {
                Log.e("MainActivity", "Invalid itemId: " + itemId);
                return false;
            }

            selectedFragment = mainFragmentsHashMap.get(fragmentTag);
            assert selectedFragment != null;
            if (currentFragment instanceof TdeeMacros) {
                AppBarHandler.resetActionBar(this);
            }

            // Hide the current fragment and show the selected fragment
            if (selectedFragment.isAdded()) {
                fragmentManager.beginTransaction().hide(currentFragment).show(selectedFragment).addToBackStack(fragmentTag).commit();
                // If the fragment is already added, just show it
            } else {
                fragmentManager.beginTransaction().hide(currentFragment).add(R.id.fragment_container_view, selectedFragment).addToBackStack(fragmentTag).commit();
            }
            currentFragment = selectedFragment;
            setAppBar();
            return true;
        });
    }

    /**
     * Sets the app bar title based on the current fragment.
     */
    public void setAppBar() {
        if (currentFragment instanceof Profile) {
            AppBarHandler.setTitle(this, "Perfil");
        } else if (currentFragment instanceof Calculator) {
            AppBarHandler.setTitle(this, "KaiFit-Pal");
        } else if (currentFragment instanceof KaiQ) {
            AppBarHandler.setTitle(this, "Kai-Q");
        } else if (currentFragment instanceof Help) {
            AppBarHandler.setTitle(this, "FAQs");
        } else if (currentFragment instanceof Settings) {
            AppBarHandler.setTitle(this, "Ajustes");
        }
    }

    /**
     * Adds the default fragment to the activity.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    private void addDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            defaultFragment = mainFragmentsHashMap.get("Calculator");
            currentFragment = defaultFragment;

            fragmentManager.beginTransaction().add(R.id.fragment_container_view, defaultFragment, "Calculator").commit();

            bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option); // Highlight the calculator menu item option
        }
    }

    /**
     * Called when the activity has detected the user's press of the back key.
     */
    @SuppressWarnings("MissingSuperCall") // super call is not needed in this context
    @Override
    public void onBackPressed() {
        if (currentFragment instanceof Calculator) {
            finish();
        } else if (currentFragment instanceof TdeeMacros) {
            returnToCalculator();
        } else {
            returnToPreviousFragmentOrCalculator();
        }
    }

    /**
     * Returns to the Calculator fragment.
     */
    private void returnToCalculator() {
        fragmentManager.popBackStackImmediate();
        bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
    }

    /**
     * Returns to the previous fragment or the Calculator fragment.
     */
    private void returnToPreviousFragmentOrCalculator() {
        int index = fragmentManager.getBackStackEntryCount() - 2;
        if (index >= 0 && isPreviousFragmentTdeeMacros(index)) {
            returnToTdeeMacros();
        } else {
            returnToDefaultFragment();
        }
    }

    /**
     * Checks if the previous fragment is TdeeMacros.
     *
     * @param index The index of the previous fragment.
     * @return True if the previous fragment is TdeeMacros, false otherwise.
     */
    private boolean isPreviousFragmentTdeeMacros(int index) {
        FragmentManager.BackStackEntry backEntry = fragmentManager.getBackStackEntryAt(index);
        String tag = backEntry.getName();
        return tag != null && tag.equals("TdeeMacros");
    }

    /**
     * Returns to the TdeeMacros fragment.
     */
    private void returnToTdeeMacros() {
        fragmentManager.popBackStackImmediate();
        bottomNavigationView.getMenu().findItem(R.id.calculator_menu_option).setChecked(false);
        currentFragment = childFragmentTdee;
    }

    /**
     * Returns to the default fragment.
     */
    private void returnToDefaultFragment() {
        fragmentManager.beginTransaction().hide(currentFragment).show(defaultFragment).commit();
        currentFragment = defaultFragment;
        bottomNavigationView.setSelectedItemId(R.id.calculator_menu_option);
    }

    /**
     * Called when the calculate button is clicked in the Calculator fragment.
     *
     * @param tdeeResult The result of the TDEE calculation.
     */
    @Override
    public void onCalculateClick(int tdeeResult) {
        childFragmentTdee = new TdeeMacros();
        Bundle bundle = new Bundle();
        bundle.putInt("tdeeResult", tdeeResult);
        childFragmentTdee.setArguments(bundle);

        fragmentManager.beginTransaction().hide(currentFragment).add(R.id.fragment_container_view, childFragmentTdee).addToBackStack("TdeeMacros").commit();
        currentFragment = childFragmentTdee;
    }
}
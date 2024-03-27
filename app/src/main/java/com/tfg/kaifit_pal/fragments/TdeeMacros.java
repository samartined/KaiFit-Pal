package com.tfg.kaifit_pal.fragments;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tfg.kaifit_pal.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TdeeMacros is a Fragment that handles the Total Daily Energy Expenditure (TDEE) and macronutrient calculations.
 * It allows the user to adjust their TDEE and macronutrient ratios and see the results in real-time.
 */
public class TdeeMacros extends Fragment {

    // Default macronutrient percentages
    private final double defaultCarbPercentage = 0.55;
    private final double defaultProteinPercentage = 0.25;
    private final double defaultFatPercentage = 0.20;

    // TDEE and modifier variables
    private int tdeeResult, originalTDEE;
    private double modifierPercentage = 0;

    // Flags to track user interactions
    private boolean isManualChange = false; // Flag to check if the change in TDEE is manual
    private boolean userChangedPickers = false; // Flag to check if the user changed the NumberPickers

    // UI components
    private TextView textViewTdee;
    private TextView modifierTdeeTextView;
    private TextView intensityModifierTextView;

    private NumberPicker proteinsNumberPicker, fatNumberPicker, carbsNumberPicker;
    private ArrayList<NumberPicker> numberPickers;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false);
        setupActionBar();

        // We configure the scroll view to show the bottom of the view when the fragment is created
        ScrollView scrollView = view.findViewById(R.id.scrollView);

        // We set scroll according if user is writing in number pickers, setting down by default and putting it up when user is writing them
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        // Initialize UI components
        initializeUIComponents(view);

        return view;
    }

    /**
     * Setup the ActionBar with the appropriate title and back button.
     */
    private void setupActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        assert activity != null;
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setContentInsetStartWithNavigation(0);
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("TDEE & Macros");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow_black);
        }
        setHasOptionsMenu(true);
    }

    /**
     * Initialize the UI components.
     *
     * @param view The current view.
     */
    private void initializeUIComponents(@NonNull View view) {
        // Initialize UI components
        textViewTdee = view.findViewById(R.id.tdeeResultTextView);
        modifierTdeeTextView = view.findViewById(R.id.modifierTDEEtextView);
        modifierTdeeTextView.setText(String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));
        intensityModifierTextView = view.findViewById(R.id.intensityModifierTextView);
        intensityModifierTextView.setText("Mantenimiento");

        // Get TDEE result from arguments
        if (getArguments() != null) {
            tdeeResult = getArguments().getInt("tdeeResult");
            originalTDEE = tdeeResult;
            textViewTdee.setText(String.valueOf(tdeeResult));
        }

        // Setup buttons
        setupButtons(view);

        // Initialize all the macros titles by initializing the TextViews at once
        ArrayList<TextView> macroTitles = new ArrayList<>(Arrays.asList(
                view.findViewById(R.id.proteinTitle),
                view.findViewById(R.id.fatTitle),
                view.findViewById(R.id.carbTitle)
        ));

        for (TextView titles : macroTitles) {
            titles.setTextColor(getResources().getColor(R.color.black));
            titles.setTextSize(15);
            titles.setTypeface(titles.getTypeface(), Typeface.BOLD);
            titles.setGravity(1);
        }

        ArrayList<TextView> macroPercentages = new ArrayList<>(Arrays.asList(
                view.findViewById(R.id.proteinsPercentageTextView),
                view.findViewById(R.id.fatsPercentageTextView),
                view.findViewById(R.id.carbsPercentageTextView)
        ));

        for (TextView macroPercentage : macroPercentages) {
            macroPercentage.setTextSize(15);
            macroPercentage.setTypeface(macroPercentage.getTypeface(), Typeface.BOLD);
            macroPercentage.setGravity(1);
        }

        // Initialize NumberPickers
        proteinsNumberPicker = view.findViewById(R.id.proteinsNumberPicker);
        fatNumberPicker = view.findViewById(R.id.fatsNumberPicker);
        carbsNumberPicker = view.findViewById(R.id.carbsNumberPicker);

        // Add NumberPickers to ArrayList
        numberPickers = new ArrayList<>(Arrays.asList(proteinsNumberPicker, fatNumberPicker, carbsNumberPicker));

        // Setup NumberPickers
        for (NumberPicker numberPicker : numberPickers) {
            numberPicker.setGravity(1);
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(tdeeResult / (numberPicker == fatNumberPicker ? 9 : 4));
            numberPicker.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);
            numberPicker.setValue((int) (tdeeResult * (numberPicker == proteinsNumberPicker ? defaultProteinPercentage : (numberPicker == fatNumberPicker ? defaultFatPercentage : defaultCarbPercentage)) / (numberPicker == fatNumberPicker ? 9 : 4)));
            numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                userChangedPickers = true;
                updateNumberPickers();
            });
        }

        // Initialize the macronutrient percentages TextViews
        for (TextView macroPercentage : macroPercentages) {
            macroPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", (macroPercentage == macroPercentages.get(0) ? defaultProteinPercentage : (macroPercentage == macroPercentages.get(1) ? defaultFatPercentage : defaultCarbPercentage)) * 100));
        }
    }

    /**
     * Setup the
     * /**
     * plus and minus calorie buttons.
     *
     * @param view The current view.
     */
    private void setupButtons(@NonNull View view) {
        Button buttonMinusCalories = view.findViewById(R.id.btnMinusCalories);
        Button buttonPlusCalories = view.findViewById(R.id.btnPlusCalories);

        buttonMinusCalories.setOnClickListener(v -> modifyTDEE(-0.05));
        buttonPlusCalories.setOnClickListener(v -> modifyTDEE(0.05));
    }

    /**
     * Modify the TDEE by a certain percentage.
     *
     * @param modifier The percentage to modify the TDEE by.
     */
    private void modifyTDEE(double modifier) {

        double newModifierPercentage = modifierPercentage + modifier;

        if (newModifierPercentage >= -0.20 && newModifierPercentage <= 0.20) {
            modifierPercentage = newModifierPercentage;
            tdeeResult = (int) (originalTDEE * (1 + modifierPercentage));

            // We use the TreeMap to get the intensity modifier text
            TreeMap<Double, String> intensityModifiers = getDoubleStringLabelModifiersTreeMap();
            intensityModifierTextView.setText(Objects.requireNonNull(intensityModifiers.floorEntry(modifierPercentage)).getValue());


            if (!isManualChange && !userChangedPickers) {
                for (NumberPicker numberPicker : numberPickers) {
                    numberPicker.setValue((int) (tdeeResult * (numberPicker == proteinsNumberPicker ? defaultProteinPercentage : (numberPicker == fatNumberPicker ? defaultFatPercentage : defaultCarbPercentage)) / (numberPicker == fatNumberPicker ? 9 : 4)));
                }
            } else {
                for (NumberPicker numberPicker : numberPickers) {
                    double value = numberPicker.getValue() / (double) originalTDEE;
                    numberPicker.setValue((int) (tdeeResult * value));
                    numberPicker.setMaxValue(tdeeResult / (numberPicker == fatNumberPicker ? 9 : 4));
                }
            }

            textViewTdee.setText(String.valueOf(tdeeResult));


            modifierTdeeTextView.setText(Math.abs(modifierPercentage) < 0.00001 ? "0%" : String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));
        }
    }

    /**
     * Update the NumberPickers when TDEE value is changed.
     */
    private void updateNumberPickers() {
        if (!userChangedPickers) return;
        isManualChange = true;

        tdeeResult = 0;
        for (NumberPicker numberPicker : numberPickers) {
            tdeeResult += numberPicker.getValue() * (numberPicker == proteinsNumberPicker ? 4 : (numberPicker == fatNumberPicker ? 9 : 4));
        }

        if (modifierPercentage == 0) {
            originalTDEE = tdeeResult;
        }

        textViewTdee.setText(String.valueOf(tdeeResult));

        userChangedPickers = false;
        isManualChange = false;
    }

    /**
     * Returns a TreeMap where the keys are Double values representing intensity modifiers and the values are
     * corresponding String labels. This map is used to display the appropriate label for a given intensity modifier.
     *
     * @return TreeMap<Double, String> where the keys are intensity modifiers and the values are corresponding labels.
     */
    @NonNull
    private static TreeMap<Double, String> getDoubleStringLabelModifiersTreeMap() {
        TreeMap<Double, String> intensityModifiers = new TreeMap<>();
        intensityModifiers.put(-0.20, "Déficit intenso");
        intensityModifiers.put(-0.15, "Déficit moderado-intenso");
        intensityModifiers.put(-0.10, "Déficit moderado");
        intensityModifiers.put(-0.05, "Déficit ligero");
        intensityModifiers.put(0.00, "Mantenimiento");
        intensityModifiers.put(0.05, "Volumen ligero");
        intensityModifiers.put(0.10, "Volumen moderado");
        intensityModifiers.put(0.15, "Volumen moderado-intenso");
        intensityModifiers.put(0.20, "Volumen intenso");
        return intensityModifiers;
    }

    /**
     * Handle options item selections.
     *
     * @param item The selected menu item.
     * @return Return false to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            requireActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the fragment is no longer in use.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        resetActionBar();
    }

    /**
     * Reset the ActionBar to its default state.
     */
    private void resetActionBar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle("KaiFit-Pal");
        }
    }
}
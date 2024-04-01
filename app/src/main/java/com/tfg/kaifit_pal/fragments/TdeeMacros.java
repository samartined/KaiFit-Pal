package com.tfg.kaifit_pal.fragments;

import static com.tfg.kaifit_pal.logic.MacrosManager.getMacrosPercentagesForModifier;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.logic.MacrosManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TdeeMacros is a Fragment class that handles the Total Daily Energy Expenditure (TDEE) and macronutrient calculations.
 * It provides an interface for the user to adjust their TDEE and macronutrient ratios.
 */
public class TdeeMacros extends Fragment {

    // Variables to store the TDEE result and the original TDEE
    private int tdeeResult, originalTDEE;
    // Variable to store the modifier percentage
    private double modifierPercentage = 0;
    // Flag to check if the user has changed the pickers
    private boolean userChangedPickers = false;

    // TextViews to display the TDEE, modifier TDEE and intensity modifier
    private TextView textViewTdee;
    private TextView modifierTdeeTextView;
    private TextView intensityModifierTextView;

    // NumberPickers for proteins, fats and carbs
    private NumberPicker proteinsNumberPicker, fatNumberPicker, carbsNumberPicker;
    // ArrayList to store the macro percentages
    ArrayList<TextView> macroPercentages = new ArrayList<>();
    // ArrayList to store the NumberPickers
    private ArrayList<NumberPicker> numberPickers;

    // MacrosManager object to handle the macros proportion
    private MacrosManager macrosProportion;

    /**
     * Method to create the view of the Fragment
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container          The parent view that the fragment's UI should be attached to
     * @param savedInstanceState A Bundle object containing the activity's previously saved state
     * @return The view of the Fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false);
        setupActionBar();
        ScrollView scrollView = view.findViewById(R.id.scrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        initializeUIComponents(view);
        return view;
    }

    /**
     * Method to set up the ActionBar
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
     * Method to initialize the UI components
     *
     * @param view The view where the components are located
     */
    private void initializeUIComponents(@NonNull View view) {
        textViewTdee = view.findViewById(R.id.tdeeResultTextView);
        modifierTdeeTextView = view.findViewById(R.id.modifierTDEEtextView);
        modifierTdeeTextView.setText(String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));
        intensityModifierTextView = view.findViewById(R.id.intensityModifierTextView);
        intensityModifierTextView.setText("Mantenimiento");

        if (getArguments() != null) {
            tdeeResult = getArguments().getInt("tdeeResult");
            originalTDEE = tdeeResult;
            textViewTdee.setText(String.valueOf(tdeeResult));
        }

        setupButtons(view);

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

        macroPercentages = new ArrayList<>(Arrays.asList(
                view.findViewById(R.id.proteinsPercentageTextView),
                view.findViewById(R.id.fatsPercentageTextView),
                view.findViewById(R.id.carbsPercentageTextView)
        ));

        for (TextView macroPercentage : macroPercentages) {
            macroPercentage.setTextSize(15);
            macroPercentage.setTypeface(macroPercentage.getTypeface(), Typeface.BOLD);
            macroPercentage.setGravity(1);
        }

        proteinsNumberPicker = view.findViewById(R.id.proteinsNumberPicker);
        fatNumberPicker = view.findViewById(R.id.fatsNumberPicker);
        carbsNumberPicker = view.findViewById(R.id.carbsNumberPicker);

        numberPickers = new ArrayList<>(Arrays.asList(proteinsNumberPicker, fatNumberPicker, carbsNumberPicker));

        Double[] defaultMacrosPercentages = getMacrosPercentagesForModifier("Mantenimiento");

        for (NumberPicker numberPicker : numberPickers) {
            numberPicker.setGravity(1);
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(tdeeResult / (numberPicker == fatNumberPicker ? 9 : 4));
            numberPicker.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);

            numberPicker.setValue((int) (tdeeResult * (numberPicker == proteinsNumberPicker ? defaultMacrosPercentages[0] : (numberPicker == fatNumberPicker ? defaultMacrosPercentages[1] : defaultMacrosPercentages[2])) / (numberPicker == fatNumberPicker ? 9 : 4)));
            numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                userChangedPickers = true;
                updateNumberPickers();
                updateMacroPercentages();
            });
        }

        for (TextView macroPercentage : macroPercentages) {
            macroPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", (macroPercentage == macroPercentages.get(0) ? defaultMacrosPercentages[0] : (macroPercentage == macroPercentages.get(1) ? defaultMacrosPercentages[1] : defaultMacrosPercentages[2])) * 100));
        }
    }

    /**
     * Method to set up the buttons
     *
     * @param view The view where the buttons are located
     */
    private void setupButtons(@NonNull View view) {
        view.findViewById(R.id.btnMinusCalories).setOnClickListener(v -> modifyTDEE(-0.05));
        view.findViewById(R.id.btnPlusCalories).setOnClickListener(v -> modifyTDEE(0.05));
    }

    /**
     * Method to modify the TDEE
     *
     * @param modifier The modifier to apply to the TDEE
     */
    private void modifyTDEE(double modifier) {
        double newModifierPercentage = modifierPercentage + modifier;

        if (newModifierPercentage >= -0.20 && newModifierPercentage <= 0.20) {
            modifierPercentage = newModifierPercentage;
            tdeeResult = (int) (originalTDEE * (1 + modifierPercentage));

            TreeMap<Double, String> intensityModifiers = getDoubleStringLabelModifiersTreeMap();
            if (newModifierPercentage < 0.05 && newModifierPercentage > -0.0001) {
                intensityModifierTextView.setText(intensityModifiers.get(0.00));
                modifierPercentage = 0.00;
            } else {
                intensityModifierTextView.setText(Objects.requireNonNull(intensityModifiers.floorEntry(modifierPercentage)).getValue());
            }

            for (NumberPicker numberPicker : numberPickers) {
                numberPicker.setValue((int) (tdeeResult * getMacroPercentageForPicker(numberPicker) / (numberPicker == fatNumberPicker ? 9 : 4)));
            }

            updateMacroPercentages();

            textViewTdee.setText(String.valueOf(tdeeResult));
            modifierTdeeTextView.setText(Math.abs(modifierPercentage) < 0.00001 ? "0%" : String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));
        }
    }

    /**
     * Method to get the macro percentage for a given NumberPicker
     *
     * @param numberPicker The NumberPicker to get the macro percentage for
     * @return The macro percentage for the given NumberPicker
     */
    private double getMacroPercentageForPicker(NumberPicker numberPicker) {
        if (modifierPercentage == 0) {
            return getMacrosPercentagesForModifier("Mantenimiento")[numberPickers.indexOf(numberPicker)];
        } else if (modifierPercentage < 0) {
            return getMacrosPercentagesForModifier("Definición")[numberPickers.indexOf(numberPicker)];
        } else {
            return getMacrosPercentagesForModifier("Volumen")[numberPickers.indexOf(numberPicker)];
        }
    }

    /**
     * Method to update the macro percentages
     */
    private void updateMacroPercentages() {
        for (int i = 0; i < numberPickers.size(); i++) {
            NumberPicker numberPicker = numberPickers.get(i);
            double macroPercentage = (double) numberPicker.getValue() * (numberPicker == fatNumberPicker ? 9 : 4) / tdeeResult;
            macroPercentages.get(i).setText(String.format(Locale.getDefault(), "%.0f%%", macroPercentage * 100));
        }
    }

    /**
     * Method to update the NumberPickers
     */
    private void updateNumberPickers() {
        if (!userChangedPickers) return;
        boolean isManualChange = true;

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

        updateMacroPercentages();
    }

    /**
     * Method to get the intensity modifiers
     *
     * @return The intensity modifiers
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
     * Method to handle the back button press
     *
     * @return True if the back button was pressed, false otherwise
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
     * This method is called when the Fragment is no longer visible to the user.
     * This could happen when the user navigates away from the Fragment or the Fragment is otherwise removed or replaced.
     * In this method, we call the superclass implementation of onPause() and then call the updateNumberPickers() method.
     * The updateNumberPickers() method is used to update the NumberPickers before leaving the Fragment to avoid losing the changes.
     */
    @Override
    public void onPause() {
        super.onPause();
        updateNumberPickers();
    }

    /**
     * This method is called before the Fragment's state is saved.
     * It allows the Fragment to save its dynamic state, so the state can be restored in onCreate(Bundle) or onCreateView(LayoutInflater, ViewGroup, Bundle).
     * In this method, we call the superclass implementation of onSaveInstanceState() and then call the updateNumberPickers() method.
     * The updateNumberPickers() method is used to update the NumberPickers before the state is saved to avoid losing the changes.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateNumberPickers();
    }

    /**
     * This method is called when the view's saved state has been restored.
     * The onViewStateRestored method is called after onStart() and before onResume().
     * The updateNumberPickers() method is used to update the NumberPickers when the view's state has been restored.
     *
     * @param savedInstanceState Bundle containing the state of the view
     */
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        updateNumberPickers();
    }

    /**
     * This method is called when the Fragment is no longer interacting with the user.
     * It is the next step in the lifecycle after onPause() and can be followed by either onRestart() or onDestroy().
     * In this method, we call the superclass implementation of onStop() and then call the resetActionBar() method.
     * The resetActionBar() method is used to reset the ActionBar to its default state when the Fragment is no longer visible.
     */
    @Override
    public void onStop() {
        super.onStop(); // Call to the superclass implementation of onStop()
        resetActionBar(); // Call to reset the ActionBar
    }

    /**
     * This method is used to reset the ActionBar to its default state.
     * It is typically called when the Fragment is no longer visible to the user.
     * The ActionBar's navigation button is hidden, the home button is disabled, and the title is set to "KaiFit-Pal".
     */
    private void resetActionBar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false); // Hide the navigation button
            actionBar.setDisplayShowHomeEnabled(false); // Disable the home button
            actionBar.setTitle("KaiFit-Pal"); // Set the title to "KaiFit-Pal"
        }
    }
}
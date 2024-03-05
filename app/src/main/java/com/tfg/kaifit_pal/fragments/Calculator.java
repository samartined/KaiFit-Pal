package com.tfg.kaifit_pal.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Calculator#newInstance} factory method to
 * create an instance of this fragment.
 */

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.calculator_logic.CalculatorUtils;

public class Calculator extends Fragment {

    // Variables related to user age declaration
    private TextView dynamicAge;
    private Button ageButtonPlus, ageButtonMinus;
    private int predefinedAge = 25;


    public Calculator() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_calculator, container, false);
        initViews(rootView);
        initListeners();
        setupGenderButtons(rootView);
        setupTextChangeListeners(rootView);
        initSpinner(rootView);
        initCalculateButton(rootView);
        return rootView;
    }

    // Initialize views and buttons
    private void initViews(View rootView) {
        dynamicAge = rootView.findViewById(R.id.ageTextView);
        ageButtonMinus = rootView.findViewById(R.id.btnMinus);
        ageButtonPlus = rootView.findViewById(R.id.btnPlus);
    }

    // Initialize listeners
    private void initListeners() {

        ageButtonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseAge();
            }
        });

        ageButtonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseAge();
            }
        });
    }

    private void decreaseAge() {
        if (predefinedAge > 18) {
            predefinedAge--;
            dynamicAge.setText(String.valueOf(predefinedAge));
        } else {
            predefinedAge = 18;
        }
    }

    private void increaseAge() {
        if (predefinedAge < 90) {
            predefinedAge++;
            dynamicAge.setText(String.valueOf(predefinedAge));
        } else {
            predefinedAge = 90;
        }
    }

    // Method to set up gender buttons
    private void setupGenderButtons(View rootView) {
        // Gender buttons control
        Button maleButton = rootView.findViewById(R.id.ButtonMale);
        Button femaleButton = rootView.findViewById(R.id.ButtonFemale);
        final EditText femaleEditText = rootView.findViewById(R.id.editTextHip);

        // Configure button behaviors
        maleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGender(true, false, femaleEditText);
            }
        });

        femaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGender(false, true, femaleEditText);
            }
        });
    }

    // Method to select gender
    private void selectGender(boolean maleSelected, boolean femaleSelected, EditText femaleEditText) {

        View view = getView(); // Get the current view
        if (view != null) { // Check if the view is not null
            Button maleButton = getView().findViewById(R.id.ButtonMale);
            Button femaleButton = getView().findViewById(R.id.ButtonFemale);

            if (maleButton != null && femaleButton != null) { // Enable or disable buttons by checking they are not null
                maleButton.setSelected(maleSelected);
                maleButton.setBackgroundResource(maleSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
                femaleButton.setSelected(femaleSelected);
                femaleButton.setBackgroundResource(femaleSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
            }

            if (femaleEditText != null) { // Enable or disable text field by checking it's not null
                femaleEditText.setEnabled(femaleSelected);
                femaleEditText.setBackgroundResource(femaleSelected ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
            }
        }
    }

    // Method to set up text listeners for relevant EditTexts
    private void setupTextChangeListeners(View rootView) {
        EditText weightEditText = rootView.findViewById(R.id.editTextWeight);
        EditText heightEditText = rootView.findViewById(R.id.editTextHeight);
        EditText neckEditText = rootView.findViewById(R.id.editTextNeck);
        EditText waistEditText = rootView.findViewById(R.id.editTextWaist);
        EditText hipEditText = rootView.findViewById(R.id.editTextHip);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setUpFatPercentage(); // Recalcular el porcentaje de grasa corporal
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        try {


            // Assing the same TextWatcher to all relevant EditTexts
            weightEditText.addTextChangedListener(textWatcher);
            heightEditText.addTextChangedListener(textWatcher);
            neckEditText.addTextChangedListener(textWatcher);
            waistEditText.addTextChangedListener(textWatcher);
            hipEditText.addTextChangedListener(textWatcher);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpFatPercentage() {
        // Get the root view
        View view = getView();
        if (view == null) {
            return;
        }

        // Get references to UI elements
        TextView inputAge = view.findViewById(R.id.ageText);
        EditText inputWeight = view.findViewById(R.id.editTextWeight);
        EditText inputHeight = view.findViewById(R.id.editTextHeight);
        EditText inputNeck = view.findViewById(R.id.editTextNeck);
        EditText inputWaist = view.findViewById(R.id.editTextWaist);
        EditText inputHip = view.findViewById(R.id.editTextHip);
        Button maleButton = view.findViewById(R.id.ButtonMale);
        Button femaleButton = view.findViewById(R.id.ButtonFemale);

        // Check gender selection
        boolean isMale = maleButton.isSelected();
        boolean isFemale = femaleButton.isSelected();

        // Use CalculatorUtils class to calculate fat percentage
        CalculatorUtils calculatorUtils = new CalculatorUtils(inputAge, inputWeight, inputHeight, inputNeck, inputWaist, inputHip, isMale, isFemale);

        // Calculate fat percentage
        double fatPercentage = calculatorUtils.calculateFatPercentage(true, false, inputWeight, inputHeight, inputNeck, inputWaist, inputHip, inputAge);

        // Set fat percentage to the corresponding EditText
        EditText fatPercentageEditText = view.findViewById(R.id.editTextFatPercent);
        fatPercentageEditText.setText(String.valueOf(fatPercentage));
    }


    // Initialize sports activity spinner
    private void initSpinner(View view) {

        Spinner activityFactorSpinner = view.findViewById(R.id.spinnerActivityFactor);
        activityFactorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get selected string
                String[] activityRatios = getResources().getStringArray(R.array.activity_factors);

                // Convert string to float
                float selectedActivityFactor = Float.parseFloat(activityRatios[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Implement actions if nothing is selected
            }
        });
    }

    // Initialize calculate button
    private void initCalculateButton(View rootView) {
        Button calculateButton = rootView.findViewById(R.id.buttonCalculate);
    }
}
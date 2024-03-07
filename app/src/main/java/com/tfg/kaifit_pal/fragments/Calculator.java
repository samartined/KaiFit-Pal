package com.tfg.kaifit_pal.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
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
    private final DecimalFormat df = new DecimalFormat("#.##");
    private static final Logger logger = Logger.getLogger(Calculator.class.getName()); // we create a logger object to log messages in case of error


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
                selectGender(true, femaleEditText);
            }
        });

        femaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGender(false, femaleEditText);
            }
        });
    }

    // Method to select gender
    private void selectGender(boolean sex, EditText femaleEditText) {

        View view = getView(); // Get the current view
        if (view != null) { // Check if the view is not null
            Button maleButton = getView().findViewById(R.id.ButtonMale);
            Button femaleButton = getView().findViewById(R.id.ButtonFemale);

            if (maleButton != null && femaleButton != null) { // Enable or disable buttons by checking they are not null
                maleButton.setSelected(sex);
                maleButton.setBackgroundResource(sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
                femaleButton.setSelected(!sex);
                femaleButton.setBackgroundResource(!sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
            }

            if (femaleEditText != null) { // Enable or disable text field by checking it's not null
                femaleEditText.setEnabled(!sex);
                femaleEditText.setBackgroundResource(!sex ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
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
        TextView fatPercentageEditText = rootView.findViewById(R.id.TextViewFatPercent);
        Button femaleButton = rootView.findViewById(R.id.ButtonFemale);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {

                boolean sex = femaleButton.isSelected();
                double valueWeight, valueNeck, valueWaist, valueHip;
                int valueHeight;

                String weightStr = weightEditText.getText().toString().trim();
                String heightStr = heightEditText.getText().toString().trim();
                String neckStr = neckEditText.getText().toString().trim();
                String waistStr = waistEditText.getText().toString().trim();
                String hipStr = hipEditText.getText().toString().trim();

                // We assign the values to the variables, if the input is empty we assign 0
                valueWeight = weightStr.isEmpty() ? 0 : Double.parseDouble(weightStr);
                valueHeight = heightStr.isEmpty() ? 0 : Integer.parseInt(heightStr);
                valueNeck = neckStr.isEmpty() ? 0 : Double.parseDouble(neckStr);
                valueWaist = waistStr.isEmpty() ? 0 : Double.parseDouble(waistStr);
                valueHip = hipStr.isEmpty() ? 0 : Double.parseDouble(hipStr);

                CalculatorUtils calculatorUtils = new CalculatorUtils(sex, valueWeight, valueHeight, valueNeck, valueWaist, valueHip);

                // Calculate fat percentage
                double fatPercentage = calculatorUtils.calculateFatPercentage(sex);

                fatPercentageEditText.setText(df.format(fatPercentage) + "%");
            }
        };

        try { // We use a try-catch block to handle exceptions
            // Try to set up text change listeners
            weightEditText.addTextChangedListener(textWatcher);
            heightEditText.addTextChangedListener(textWatcher);
            neckEditText.addTextChangedListener(textWatcher);
            waistEditText.addTextChangedListener(textWatcher);
            hipEditText.addTextChangedListener(textWatcher);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up text change listeners", e); // Log error message
        }
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
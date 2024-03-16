package com.tfg.kaifit_pal;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
        initSpinner(rootView);
        initCalculateButton(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set the male button as selected after the view is created
        view.post(new Runnable() {
            @Override
            public void run() {
                selectGender(true, false, view.findViewById(R.id.editTextHip));
            }
        });
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
        if (predefinedAge > 0) {
            predefinedAge--;
            dynamicAge.setText(String.valueOf(predefinedAge));
        }
    }

    private void increaseAge() {
        if (predefinedAge < 100) {
            predefinedAge++;
            dynamicAge.setText(String.valueOf(predefinedAge));
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
                femaleEditText.setText("");
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

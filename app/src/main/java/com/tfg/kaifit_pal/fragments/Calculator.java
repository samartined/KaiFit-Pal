package com.tfg.kaifit_pal.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.calculator_logic.CalculatorUtils;

public class Calculator extends Fragment {

    // We declare some global variables for a communication between fragments and activities

    private int tdeeResult;

    private double fatPercentage;

    private TextView dynamicAge, fatPercentageEditText;

    private EditText weightEditText, heightEditText, neckEditText, waistEditText, hipEditText;

    private Button femaleButton;

    Spinner activityFactorSpinner;

    private OnCalculateClickListener callback; // The callback will allow us to communicate with the activity and get the data from the user
    private CalculatorUtils calculatorUtils;

    // We declare an onClickListener to communicate with the activity using the callback
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            callback = (OnCalculateClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " debe implementar OnCalculateClickListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);
        dynamicAge = view.findViewById(R.id.ageTextView);
        setupButtons(view);

        // We need to separate this listener from the others to avoid being called when the calculator is not parameterized
        view.findViewById(R.id.buttonCalculate).setOnClickListener(v -> {
            double activityFactor = getActivityFactor(view);

            // We need to create here the instance of the CalculatorUtils class to pass the activityFactor updated
            calculatorUtils = CalculatorUtils.createInstance(femaleButton.isSelected(), parseInt(dynamicAge.getText().toString()), parseInt(heightEditText.getText().toString().trim()), parseDouble(weightEditText.getText().toString().trim()), parseDouble(neckEditText.getText().toString().trim()), parseDouble(waistEditText.getText().toString().trim()), parseDouble(hipEditText.getText().toString().trim()));
            calculatorUtils.setActivityFactor(activityFactor);

            tdeeResult = calculatorUtils.calculateTDEE();
            callback.onCalculateClick(tdeeResult);
        });
        setupTextChangeListeners(view);

        return view;
    }

    // The main purpose of this method is to set the default appearance of the sex buttons
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> selectGender(true, view));
    }

    private void setupButtons(@NonNull View view) {
        view.findViewById(R.id.btnMinus).setOnClickListener(v -> updateAge(-1));
        view.findViewById(R.id.btnPlus).setOnClickListener(v -> updateAge(1));
        view.findViewById(R.id.ButtonMale).setOnClickListener(v -> selectGender(true, view));
        view.findViewById(R.id.ButtonFemale).setOnClickListener(v -> selectGender(false, view));
    }

    private void updateAge(int change) {
        int predefinedAge = Integer.parseInt(dynamicAge.getText().toString());
        predefinedAge = Math.max(18, Math.min(90, predefinedAge + change));
        dynamicAge.setText(String.valueOf(predefinedAge));
    }

    private void selectGender(boolean sex, @NonNull View view) {
        Button maleButton = view.findViewById(R.id.ButtonMale);
        femaleButton = view.findViewById(R.id.ButtonFemale);
        EditText femaleEditText = view.findViewById(R.id.editTextHip);

        maleButton.setSelected(sex);
        femaleEditText.setText("");
        maleButton.setBackgroundResource(sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleButton.setSelected(!sex);
        femaleButton.setBackgroundResource(!sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleEditText.setEnabled(!sex);
        femaleEditText.setBackgroundResource(!sex ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
    }

    private void setupTextChangeListeners(@NonNull View view) {
        weightEditText = view.findViewById(R.id.editTextWeight);
        heightEditText = view.findViewById(R.id.editTextHeight);
        neckEditText = view.findViewById(R.id.editTextNeck);
        waistEditText = view.findViewById(R.id.editTextWaist);
        hipEditText = view.findViewById(R.id.editTextHip);
        fatPercentageEditText = view.findViewById(R.id.TextViewFatPercent);
        femaleButton = view.findViewById(R.id.ButtonFemale);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                int age = Integer.parseInt(dynamicAge.getText().toString());
                boolean sex = femaleButton.isSelected();
                double valueWeight = parseDouble(weightEditText.getText().toString().trim());
                int valueHeight = parseInt(heightEditText.getText().toString().trim());
                double valueNeck = parseDouble(neckEditText.getText().toString().trim());
                double valueWaist = parseDouble(waistEditText.getText().toString().trim());
                double valueHip = parseDouble(hipEditText.getText().toString().trim());

                // We instance the CalculatorUtils class with the data from the UI
                calculatorUtils = CalculatorUtils.createInstance(sex, age, valueHeight, valueWeight, valueNeck, valueWaist, valueHip);
                fatPercentage = calculatorUtils.getFatPercentage();

                if (valueHeight == 0 && valueWeight == 0 && valueNeck == 0 && valueWaist == 0 && valueHip == 0) {
                    fatPercentageEditText.setText("");
                } else if (valueHeight == 0 || valueWeight == 0 || valueNeck == 0 || valueWaist == 0 || (sex && valueHip == 0)) {
                    fatPercentageEditText.setText("Calculando...");
                } else if (Double.isNaN(fatPercentage) || Double.isInfinite(fatPercentage) || fatPercentage < 0 || fatPercentage > 100) {
                    fatPercentageEditText.setText("Datos incorrectos.");
                } else {
                    fatPercentageEditText.setText(String.format("%.2f%%", fatPercentage));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
        };

        weightEditText.addTextChangedListener(textWatcher);
        heightEditText.addTextChangedListener(textWatcher);
        neckEditText.addTextChangedListener(textWatcher);
        waistEditText.addTextChangedListener(textWatcher);
        hipEditText.addTextChangedListener(textWatcher);
    }

    // We'll get the activity factor from the spinner
    private double getActivityFactor(@NonNull View view) {
        activityFactorSpinner = view.findViewById(R.id.spinnerActivityFactor);

        // Now we use the activity factor values defined in activity_factor_values.xml
        int selectedFactorIndex = activityFactorSpinner.getSelectedItemPosition();

        String[] activityFactorValues = getResources().getStringArray(R.array.activity_factors);

        if (selectedFactorIndex >= 0 && selectedFactorIndex < activityFactorValues.length) { // This condition check is necessary to avoid ArrayIndexOutOfBoundsException. If the selectedFactorIndex is out of bounds, it will return the Log.e message and the default value

            // We capture eventual NumberFormatExceptions
            try {
                return Double.parseDouble(activityFactorValues[selectedFactorIndex]);
            } catch (NumberFormatException e) {
                Log.e("Calculator", "Invalid activity factor: " + activityFactorValues[selectedFactorIndex]);
            }

        } else {
            Log.e("Calculator", "Invalid selectedFactorIndex: " + selectedFactorIndex);
        }
        return 1.2; // Default value
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Interface to communicate with the activity and get the data from the user
    public interface OnCalculateClickListener {
        void onCalculateClick(int tdeeResult);
    }
}
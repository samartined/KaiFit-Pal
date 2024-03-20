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

    private int tdeeResult;

    private double fatPercentage;

    private TextView dynamicAge, fatPercentageEditText;

    private EditText weightEditText, heightEditText, neckEditText, waistEditText, hipEditText;

    private Button femaleButton;

    private Spinner activityFactorSpinner;
    private OnCalculateClickListener callback; // The callback will allow us to communicate with the activity and get the data from the user
    private CalculatorUtils calculatorUtils;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            callback = (OnCalculateClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " debe implementar OnCalculateClickListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calculator, container, false);
        dynamicAge = rootView.findViewById(R.id.ageTextView);
        setupButtons(rootView);
        setupTextChangeListeners(rootView);
        rootView.findViewById(R.id.buttonCalculate).setOnClickListener(v -> {
            tdeeResult = calculatorUtils.calculateTDEE();
            callback.onCalculateClick(tdeeResult);
        });
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        rootView.post(() -> selectGender(true, rootView));
    }

    private void setupButtons(@NonNull View rootView) {
        rootView.findViewById(R.id.btnMinus).setOnClickListener(v -> updateAge(-1));
        rootView.findViewById(R.id.btnPlus).setOnClickListener(v -> updateAge(1));
        rootView.findViewById(R.id.ButtonMale).setOnClickListener(v -> selectGender(true, rootView));
        rootView.findViewById(R.id.ButtonFemale).setOnClickListener(v -> selectGender(false, rootView));
    }

    private void updateAge(int change) {
        int predefinedAge = Integer.parseInt(dynamicAge.getText().toString());
        predefinedAge = Math.max(18, Math.min(90, predefinedAge + change));
        dynamicAge.setText(String.valueOf(predefinedAge));
    }

    private void selectGender(boolean sex, @NonNull View rootView) {
        Button maleButton = rootView.findViewById(R.id.ButtonMale);
        femaleButton = rootView.findViewById(R.id.ButtonFemale);
        EditText femaleEditText = rootView.findViewById(R.id.editTextHip);

        maleButton.setSelected(sex);
        femaleEditText.setText("");
        maleButton.setBackgroundResource(sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleButton.setSelected(!sex);
        femaleButton.setBackgroundResource(!sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleEditText.setEnabled(!sex);
        femaleEditText.setBackgroundResource(!sex ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
    }

    private void setupTextChangeListeners(@NonNull View rootView) {
        weightEditText = rootView.findViewById(R.id.editTextWeight);
        heightEditText = rootView.findViewById(R.id.editTextHeight);
        neckEditText = rootView.findViewById(R.id.editTextNeck);
        waistEditText = rootView.findViewById(R.id.editTextWaist);
        hipEditText = rootView.findViewById(R.id.editTextHip);
        fatPercentageEditText = rootView.findViewById(R.id.TextViewFatPercent);
        femaleButton = rootView.findViewById(R.id.ButtonFemale);

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
                double activityFactor = getActivityFactor(rootView);


                // We instance the CalculatorUtils class with the data from the UI
                calculatorUtils = CalculatorUtils.createInstance(sex, age, valueHeight, valueWeight, valueNeck, valueWaist, valueHip, activityFactor);
                fatPercentage = calculatorUtils.getFatPercentage();

                if (valueHeight == 0 && valueWeight == 0 && valueNeck == 0 && valueWaist == 0 && valueHip == 0) {
                    fatPercentageEditText.setText("");
                } else if
                (valueWeight == 0 || valueHeight == 0 || valueNeck == 0 || valueWaist == 0 || valueHip == 0) {
                    fatPercentageEditText.setText("Calculando...");
                } else if (fatPercentage == 0) {
                    fatPercentageEditText.setText("Ingrese datos.");
                } else if (fatPercentage > 100 || fatPercentage < 0) {
                    fatPercentageEditText.setText("Datos incorrectos.");
                } else if (Double.isNaN(fatPercentage) || Double.isInfinite(fatPercentage)) {
                    fatPercentageEditText.setText("Datos incorrectos.");
                } else {
                    fatPercentageEditText.setText(String.format("%.2f%%", fatPercentage));
                }
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
    private double getActivityFactor(@NonNull View rootView) {
        activityFactorSpinner = rootView.findViewById(R.id.spinnerActivityFactor);

        // Now we use the activity factor values defined in activity_factor_values.xml
        int selectedFactorIndex = activityFactorSpinner.getSelectedItemPosition();

        String[] activityFactorValues = getResources().getStringArray(R.array.activity_factors);

        if (selectedFactorIndex >= 0 && selectedFactorIndex < activityFactorValues.length) { // This condition check is necessary to avoid ArrayIndexOutOfBoundsException. If the selectedFactorIndex is out of bounds, it will return the Log.e message and the default value

            // We capture eventual NumberFormatExceptions
            try {
                return Double.parseDouble(activityFactorValues[selectedFactorIndex]);
            } catch (NumberFormatException e) {
                Log.e("Calculator", "Invalid actity factor: " + activityFactorValues[selectedFactorIndex]);
            }

        } else {
            Log.e("Calculator", "Invalid selectedFactorIndex: " + selectedFactorIndex);
        }
        return 1.2; // Default value
    }

    public interface OnCalculateClickListener {
        void onCalculateClick(int tdeeResult);
    }
}
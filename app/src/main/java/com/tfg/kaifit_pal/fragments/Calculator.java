package com.tfg.kaifit_pal.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.calculator_logic.CalculatorUtils;

public class Calculator extends Fragment {

    private TextView dynamicAge;
    private int predefinedAge = 25;
    OnCalculateClickListener callback; // The callback will allow us to communicate with the activity and get the data from the user

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            callback = (OnCalculateClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " debe implementar OnCalculateClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calculator, container, false);
        dynamicAge = rootView.findViewById(R.id.ageTextView);
        setupButtons(rootView);
        setupTextChangeListeners(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> selectGender(true, view));
    }

    private void setupButtons(View rootView) {
        rootView.findViewById(R.id.btnMinus).setOnClickListener(v -> updateAge(-1));
        rootView.findViewById(R.id.btnPlus).setOnClickListener(v -> updateAge(1));
        rootView.findViewById(R.id.ButtonMale).setOnClickListener(v -> selectGender(true, rootView));
        rootView.findViewById(R.id.ButtonFemale).setOnClickListener(v -> selectGender(false, rootView));
        rootView.findViewById(R.id.buttonCalculate).setOnClickListener(v -> callback.onCalculateClick());
    }

    private void updateAge(int change) {
        predefinedAge = Math.max(18, Math.min(90, predefinedAge + change));
        dynamicAge.setText(String.valueOf(predefinedAge));
    }

    private void selectGender(boolean sex, View rootView) {
        Button maleButton = rootView.findViewById(R.id.ButtonMale);
        Button femaleButton = rootView.findViewById(R.id.ButtonFemale);
        EditText femaleEditText = rootView.findViewById(R.id.editTextHip);

        maleButton.setSelected(sex);
        femaleEditText.setText("");
        maleButton.setBackgroundResource(sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleButton.setSelected(!sex);
        femaleButton.setBackgroundResource(!sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
        femaleEditText.setEnabled(!sex);
        femaleEditText.setBackgroundResource(!sex ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
    }

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
            public void afterTextChanged(Editable editable) {
                boolean sex = femaleButton.isSelected();
                double valueWeight = parseDouble(weightEditText.getText().toString().trim());
                int valueHeight = parseInt(heightEditText.getText().toString().trim());
                double valueNeck = parseDouble(neckEditText.getText().toString().trim());
                double valueWaist = parseDouble(waistEditText.getText().toString().trim());
                double valueHip = parseDouble(hipEditText.getText().toString().trim());


                CalculatorUtils calculatorUtils = new CalculatorUtils(sex, valueHeight, valueWeight, valueNeck, valueWaist, valueHip);
                double fatPercentage = calculatorUtils.calculateFatPercentage();

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

    public interface OnCalculateClickListener {
        void onCalculateClick();
    }
}
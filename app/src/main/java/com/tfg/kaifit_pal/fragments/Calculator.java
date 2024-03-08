package com.tfg.kaifit_pal.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.tfg.kaifit_pal.R;

public class Calculator extends Fragment {

    private TextView dynamicAge;
    private Button ageButtonPlus, ageButtonMinus, calculateButton, calculateFatPercentageButton, maleButton, femaleButton;
    private EditText femaleEditText;
    private int predefinedAge = 25;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_calculator, container, false);
        initViews(rootView);
        initListeners();
        return rootView;
    }

    private void initViews(View rootView) {
        dynamicAge = rootView.findViewById(R.id.ageTextView);
        ageButtonMinus = rootView.findViewById(R.id.btnMinus);
        ageButtonPlus = rootView.findViewById(R.id.btnPlus);
        calculateButton = rootView.findViewById(R.id.buttonCalculate);
        calculateFatPercentageButton = rootView.findViewById(R.id.buttonFatPercentageCalculate);
        maleButton = rootView.findViewById(R.id.ButtonMale);
        femaleButton = rootView.findViewById(R.id.ButtonFemale);
        femaleEditText = rootView.findViewById(R.id.editTextHip);
    }

    private void initListeners() {
        ageButtonMinus.setOnClickListener(v -> decreaseAge());
        ageButtonPlus.setOnClickListener(v -> increaseAge());

        View.OnClickListener genderButtonListener = v -> {
            boolean isMale = v.getId() == R.id.ButtonMale;
            selectGender(isMale, !isMale);
        };

        maleButton.setOnClickListener(genderButtonListener);
        femaleButton.setOnClickListener(genderButtonListener);

        View.OnClickListener calculationButtonsListener = v -> {
            if (!femaleButton.isSelected() && !maleButton.isSelected()) {
                showToast("Selecciona tu sexo antes de calcular.");
            }
        };

        calculateButton.setOnClickListener(calculationButtonsListener);
        calculateFatPercentageButton.setOnClickListener(calculationButtonsListener);

        Spinner activityFactorSpinner = rootView.findViewById(R.id.spinnerActivityFactor);
        activityFactorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] activityRatios = getResources().getStringArray(R.array.activity_factors);
                float selectedActivityFactor = Float.parseFloat(activityRatios[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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

    private void selectGender(boolean maleSelected, boolean femaleSelected) {

        maleButton.setSelected(maleSelected);
        femaleButton.setSelected(femaleSelected);

        int maleButtonBackground = maleSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed;
        int femaleButtonBackground = femaleSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed;
        int femaleEditTextBackground = femaleSelected ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background;

        maleButton.setBackgroundResource(maleButtonBackground);
        femaleButton.setBackgroundResource(femaleButtonBackground);
        femaleEditText.setEnabled(femaleSelected);
        femaleEditText.setBackgroundResource(femaleEditTextBackground);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
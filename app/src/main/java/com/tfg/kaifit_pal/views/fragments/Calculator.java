package com.tfg.kaifit_pal.views.fragments;

import static java.lang.Integer.parseInt;

import android.app.AlertDialog;
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
import com.tfg.kaifit_pal.logic.CalculatorLogic;
import com.tfg.kaifit_pal.utilities.DataParser;

public class Calculator extends Fragment {

    private int tdeeResult;
    private double fatPercentage;
    private TextView dynamicAge;
    private TextView fatPercentageEditText;
    private EditText weightEditText;
    private EditText heightEditText;
    private EditText neckEditText;
    private EditText waistEditText;
    private EditText hipEditText;
    private Button femaleButton;
    private Spinner activityFactorSpinner;

    private CalculatorLogic calculatorInstance;
    private CalculateListenerInterface callback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            callback = (CalculateListenerInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " debe implementar OnCalculateClickListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);
        dynamicAge = view.findViewById(R.id.ageTextView);
        setUpComponents(view);

        view.findViewById(R.id.buttonCalculate).setOnClickListener(v -> {
            calculateTDEEAndNotify(view);
        });
        setUpTextChangeListeners(view);

        return view;
    }

    private void calculateTDEEAndNotify(View view) {
        boolean isFemale = femaleButton.isSelected();
        int age = DataParser.parseIntUtility(dynamicAge.getText().toString());
        int height = DataParser.parseIntUtility(heightEditText.getText().toString().trim());
        double weight = DataParser.parseDoubleUtility(weightEditText.getText().toString().trim());
        double neck = DataParser.parseDoubleUtility(neckEditText.getText().toString().trim());
        double waist = DataParser.parseDoubleUtility(waistEditText.getText().toString().trim());
        double hip = DataParser.parseDoubleUtility(hipEditText.getText().toString().trim());
        double activityFactor = getActivityFactor(view);

        calculatorInstance = CalculatorLogic.createInstance(isFemale, age, height, weight, neck, waist, hip);
        calculatorInstance.setActivityFactor(activityFactor);

        tdeeResult = calculatorInstance.calculateTDEE();
        callback.onCalculateClick(tdeeResult);
    }

    private void setUpComponents(@NonNull View view) {
        view.findViewById(R.id.btnMinus).setOnClickListener(v -> updateAge(-1));
        view.findViewById(R.id.btnPlus).setOnClickListener(v -> updateAge(1));
        view.findViewById(R.id.ButtonMale).setOnClickListener(v -> selectGender(true, view));
        view.findViewById(R.id.ButtonFemale).setOnClickListener(v -> selectGender(false, view));

        setUpInfoImageViews(view);
    }


    private void setUpInfoImageViews(@NonNull View view) {
        view.findViewById(R.id.waistInfo).setOnClickListener(v -> showInfoDialog("Para medir la cintura, " +
                "coloca la cinta métrica alrededor de tu cintura si eres hombre " +
                "o justo por encima de tu ombligo si eres mujer."));

        view.findViewById(R.id.neckInfo).setOnClickListener(v -> showInfoDialog("Para medir el cuello," +
                " coloca la cinta métrica alrededor de tu cuello, " +
                "justo por debajo de la laringe y ligeramente inclinada hacia adelante."));

        view.findViewById(R.id.hipsInfo).setOnClickListener(v -> showInfoDialog("Esta medida es opcional para hombres y obligatoria para mujeres. " +
                "Para medir las caderas, coloca la cinta métrica alrededor de la parte más ancha de tus caderas."));

        view.findViewById(R.id.percentInfo).setOnClickListener(v -> showInfoDialog("El porcentaje de grasa corporal es un número que representa " +
                "la cantidad de grasa en tu cuerpo en relación con tu peso total."));

        view.findViewById(R.id.actFactorInfo).setOnClickListener(v -> showInfoDialog("El factor de actividad es " +
                "un número que representa tu nivel de actividad física. " +
                "Cuanto más activo seas, mayor será tu factor de actividad."));
    }

    private void showInfoDialog(String message) {
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }


    private void updateAge(int change) {
        int predefinedAge = parseInt(dynamicAge.getText().toString());
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

    private void setUpTextChangeListeners(@NonNull View view) {
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
                int age = parseInt(dynamicAge.getText().toString());
                boolean sex = femaleButton.isSelected();
                double valueWeight = DataParser.parseDoubleUtility(weightEditText.getText().toString().trim());
                int valueHeight = DataParser.parseIntUtility(heightEditText.getText().toString().trim());
                double valueNeck = DataParser.parseDoubleUtility(neckEditText.getText().toString().trim());
                double valueWaist = DataParser.parseDoubleUtility(waistEditText.getText().toString().trim());
                double valueHip = DataParser.parseDoubleUtility(hipEditText.getText().toString().trim());

                calculatorInstance = CalculatorLogic.createInstance(sex, age, valueHeight, valueWeight, valueNeck, valueWaist, valueHip);
                fatPercentage = calculatorInstance.getFatPercentage();

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

    private double getActivityFactor(@NonNull View view) {
        activityFactorSpinner = view.findViewById(R.id.spinnerActivityFactor);

        int selectedFactorIndex = activityFactorSpinner.getSelectedItemPosition();

        String[] activityFactorValues = getResources().getStringArray(R.array.activity_factors);

        if (selectedFactorIndex >= 0 && selectedFactorIndex < activityFactorValues.length) { // This condition

            try {
                return Double.parseDouble(activityFactorValues[selectedFactorIndex]);
            } catch (NumberFormatException e) {
                Log.e("Calculator", "Invalid activity factor: " + activityFactorValues[selectedFactorIndex]);
            }

        } else {
            Log.e("Calculator", "Invalid selectedFactorIndex: " + selectedFactorIndex);
        }
        return 1.2;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> selectGender(true, view));
    }
}
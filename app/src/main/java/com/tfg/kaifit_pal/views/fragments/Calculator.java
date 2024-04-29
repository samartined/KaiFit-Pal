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

/**
 * This class is used to create the Calculator fragment. This fragment is used to calculate the Total Daily Energy Expenditure (TDEE) and body fat percentage of the user.
 */
public class Calculator extends Fragment {

    // This class contains several private variables that are used throughout the Calculator class.

    private int tdeeResult; // This variable holds the result of the Total Daily Energy Expenditure (TDEE) calculation.
    private double fatPercentage; // This variable holds the calculated body fat percentage.

    private TextView dynamicAge; // This TextView displays the age that the user inputs dynamically.
    private TextView fatPercentageEditText; // This TextView displays the calculated body fat percentage.

    // These EditText fields are used to input the user's weight, height, neck, waist, and hip measurements.
    private EditText weightEditText;
    private EditText heightEditText;
    private EditText neckEditText;
    private EditText waistEditText;
    private EditText hipEditText;

    private Button femaleButton; // This button is used to select the female gender.

    private Spinner activityFactorSpinner; // This Spinner allows the user to select their activity level.

    // This instance of the CalculatorLogic class is used to perform the TDEE and body fat percentage calculations.
    private CalculatorLogic calculatorInstance;
    private CalculateListenerInterface callback;

    /**
     * This method is called when the fragment is first attached to its context.
     *
     * @param context The context to which the fragment is attached.
     * @throws ClassCastException if the context does not implement the OnCalculateClickListener interface.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Attempt to cast the context to an OnCalculateClickListenerInterface
            callback = (CalculateListenerInterface) context;
        } catch (ClassCastException e) {
            // If the context does not implement OnCalculateClickListenerInterface, throw an exception
            throw new ClassCastException(context + " debe implementar OnCalculateClickListener");
        }
    }

    /**
     * This method is called when the fragment is created. It inflates the layout for the fragment and sets up the buttons and text change listeners.
     *
     * @param inflater           The LayoutInflater used to inflate the layout for the fragment.
     * @param container          The ViewGroup container for the fragment.
     * @param savedInstanceState The Bundle containing the saved instance state of the fragment.
     * @return The View for the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calculator, container, false);
        dynamicAge = view.findViewById(R.id.ageTextView);
        setUpComponentes(view);

        // We need to separate this listener from the others to avoid being called when the calculator is not parameterized
        view.findViewById(R.id.buttonCalculate).setOnClickListener(v -> {
            calculateTDEEAndNotify(view);
        });
        setupTextChangeListeners(view);

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

    /**
     * This method sets up the buttons for the Calculator fragment.
     * It assigns onClickListeners to each button to perform their respective actions when clicked.
     *
     * @param view The View for the fragment.
     */
    private void setUpComponentes(@NonNull View view) {
        // The minus button decreases the age by 1 when clicked.
        view.findViewById(R.id.btnMinus).setOnClickListener(v -> updateAge(-1));

        // The plus button increases the age by 1 when clicked.
        view.findViewById(R.id.btnPlus).setOnClickListener(v -> updateAge(1));

        // The male button sets the gender to male when clicked.
        view.findViewById(R.id.ButtonMale).setOnClickListener(v -> selectGender(true, view));

        // The female button sets the gender to female when clicked.
        view.findViewById(R.id.ButtonFemale).setOnClickListener(v -> selectGender(false, view));

        // Initialize the info imageviews
        setUpInfoImageViews(view);
    }

    /**
     * This method sets up the info imageviews for the Calculator fragment.
     * It assigns onClickListeners to each info imageview to display a dialog with information when clicked.
     *
     * @param view The View for the fragment.
     */
    private void setUpInfoImageViews(@NonNull View view) {
        // The waist info imageview displays a dialog with information on how to measure the waist when clicked.
        view.findViewById(R.id.waistInfo).setOnClickListener(v -> showInfoDialog("Para medir la cintura, " +
                "coloca la cinta métrica alrededor de tu cintura si eres hombre " +
                "o justo por encima de tu ombligo si eres mujer."));

        // The neck info imageview displays a dialog with information on how to measure the neck when clicked.
        view.findViewById(R.id.neckInfo).setOnClickListener(v -> showInfoDialog("Para medir el cuello," +
                " coloca la cinta métrica alrededor de tu cuello, " +
                "justo por debajo de la laringe y ligeramente inclinada hacia adelante."));

        // The hips info imageview displays a dialog with information on how to measure the hips when clicked.
        view.findViewById(R.id.hipsInfo).setOnClickListener(v -> showInfoDialog("Esta medida es opcional para hombres y obligatoria para mujeres. " +
                "Para medir las caderas, coloca la cinta métrica alrededor de la parte más ancha de tus caderas."));

        // The percent info imageview displays a dialog with information on body fat percentage when clicked.
        view.findViewById(R.id.percentInfo).setOnClickListener(v -> showInfoDialog("El porcentaje de grasa corporal es un número que representa " +
                "la cantidad de grasa en tu cuerpo en relación con tu peso total."));

        // The activity factor info imageview displays a dialog with information on activity factor when clicked.
        view.findViewById(R.id.actFactorInfo).setOnClickListener(v -> showInfoDialog("El factor de actividad es " +
                "un número que representa tu nivel de actividad física. " +
                "Cuanto más activo seas, mayor será tu factor de actividad."));
    }

    /**
     * This method displays a dialog with a given message.
     * It is used to provide information to the user when they click on an info imageview.
     *
     * @param message The message to be displayed in the dialog.
     */
    private void showInfoDialog(String message) {
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * This method updates the age displayed in the dynamicAge TextView.
     *
     * @param change The amount by which to change the age.
     */
    private void updateAge(int change) {
        int predefinedAge = parseInt(dynamicAge.getText().toString());
        predefinedAge = Math.max(18, Math.min(90, predefinedAge + change));
        dynamicAge.setText(String.valueOf(predefinedAge));
    }

    /**
     * This method is used to select the gender in the Calculator fragment.
     * It updates the UI based on the selected gender.
     *
     * @param sex  The gender selected by the user. True for male, false for female.
     * @param view The View for the fragment.
     */
    private void selectGender(boolean sex, @NonNull View view) {
        // Get the male and female buttons from the view
        Button maleButton = view.findViewById(R.id.ButtonMale);
        femaleButton = view.findViewById(R.id.ButtonFemale);

        // Get the EditText for the hip measurement, which is only applicable for females
        EditText femaleEditText = view.findViewById(R.id.editTextHip);

        // Set the selected state of the male button based on the selected gender
        maleButton.setSelected(sex);

        // Clear the text in the female EditText
        femaleEditText.setText("");

        // Update the background resource of the male button based on the selected gender
        maleButton.setBackgroundResource(sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);

        // Set the selected state of the female button based on the selected gender
        femaleButton.setSelected(!sex);

        // Update the background resource of the female button based on the selected gender
        femaleButton.setBackgroundResource(!sex ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);

        // Enable or disable the female EditText based on the selected gender
        femaleEditText.setEnabled(!sex);

        // Update the background resource of the female EditText based on the selected gender
        femaleEditText.setBackgroundResource(!sex ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
    }

    /**
     * This method sets up the text change listeners for the weight, height, neck, waist, and hip fields.
     * It calculates the body fat percentage based on the user's input and updates the fatPercentageEditText accordingly.
     *
     * @param view The View for the fragment.
     */
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
                int age = parseInt(dynamicAge.getText().toString());
                boolean sex = femaleButton.isSelected();
                double valueWeight = DataParser.parseDoubleUtility(weightEditText.getText().toString().trim());
                int valueHeight = DataParser.parseIntUtility(heightEditText.getText().toString().trim());
                double valueNeck = DataParser.parseDoubleUtility(neckEditText.getText().toString().trim());
                double valueWaist = DataParser.parseDoubleUtility(waistEditText.getText().toString().trim());
                double valueHip = DataParser.parseDoubleUtility(hipEditText.getText().toString().trim());

                // We instance the CalculatorLogic class with the data from the UI
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

    /**
     * This method gets the activity factor selected by the user from the Spinner.
     *
     * @param view The View for the fragment.
     * @return The activity factor selected by the user.
     */
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

    /**
     * This method is called when the fragment is created and the view is created.
     * It sets the initial values for the age, weight, height, neck, waist, and hip fields.
     *
     * @param view               The View for the fragment.
     * @param savedInstanceState The Bundle containing the saved instance state of the fragment.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.post(() -> selectGender(true, view));
    }
}
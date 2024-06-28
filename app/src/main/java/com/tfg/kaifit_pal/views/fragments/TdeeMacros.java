package com.tfg.kaifit_pal.views.fragments;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;


public class TdeeMacros extends Fragment {

    private int tdeeResult, originalTDEE;
    private double modifierPercentage = 0;
    private boolean userChangedPickers = false;
    private TextView textViewTdee;
    private TextView modifierTdeeTextView;
    private TextView intensityModifierTextView;
    private NumberPicker proteinsNumberPicker, fatNumberPicker, carbsNumberPicker;
    ArrayList<TextView> macroPercentages = new ArrayList<>();
    private ArrayList<NumberPicker> numberPickers;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false);
        setUpActionBar();
        ScrollView scrollView = view.findViewById(R.id.scrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        initializeUIComponents(view);
        return view;
    }

    private void setUpActionBar() {
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

    private void initializeUIComponents(@NonNull View view) {
        textViewTdee = view.findViewById(R.id.tdeeResultTextView);
        modifierTdeeTextView = view.findViewById(R.id.modifierTDEEtextView);
        intensityModifierTextView = view.findViewById(R.id.intensityModifierTextView);

        proteinsNumberPicker = view.findViewById(R.id.proteinsNumberPicker);
        fatNumberPicker = view.findViewById(R.id.fatsNumberPicker);
        carbsNumberPicker = view.findViewById(R.id.carbsNumberPicker);

        if (getArguments() != null) {
            tdeeResult = getArguments().getInt("tdeeResult");
            originalTDEE = tdeeResult;
            textViewTdee.setText(String.valueOf(tdeeResult));
        }

        setUpButtons(view);
        setUpTextViews(view);
        setUpNumberPickers();
    }

    private void setUpTextViews(@NonNull View view) {
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
    }

    private void setUpNumberPickers() {
        numberPickers = new ArrayList<>(Arrays.asList(proteinsNumberPicker, fatNumberPicker, carbsNumberPicker));
        Double[] defaultMacrosPercentages = getMacrosPercentagesForModifier("Mantenimiento");

        for (NumberPicker numberPicker : numberPickers) {
            if (tdeeResult == 0) {
                numberPicker.setGravity(1);
                numberPicker.setMinValue(0);
                numberPicker.setMaxValue(0);
                numberPicker.setValue(0);

            } else {
                numberPicker.setGravity(1);
                numberPicker.setMinValue(0);
                numberPicker.setMaxValue(tdeeResult / (numberPicker == fatNumberPicker ? 9 : 4));
                numberPicker.setValue((int) (tdeeResult * (numberPicker == proteinsNumberPicker ? defaultMacrosPercentages[0] : (numberPicker == fatNumberPicker ? defaultMacrosPercentages[1] : defaultMacrosPercentages[2])) / (numberPicker == fatNumberPicker ? 9 : 4)));
            }

            numberPicker.setFormatter(new NumberPicker.Formatter() {
                @Override
                public String format(int value) {
                    return value + " g";
                }
            });

            numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
                userChangedPickers = true;
                updateNumberPickers();
                updateMacroPercentages();
            });
        }

        for (TextView macroPercentage : macroPercentages) {
            if (proteinsNumberPicker.getValue() == 0 || fatNumberPicker.getValue() == 0 || carbsNumberPicker.getValue() == 0) {
                macroPercentage.setText("0%");
            } else {
                macroPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", (macroPercentage == macroPercentages.get(0) ? defaultMacrosPercentages[0] : (macroPercentage == macroPercentages.get(1) ? defaultMacrosPercentages[1] : defaultMacrosPercentages[2])) * 100));
            }
        }
    }

    private void setUpButtons(@NonNull View view) {
        view.findViewById(R.id.btnMinusCalories).setOnClickListener(v -> modifyTdee(-0.05));
        view.findViewById(R.id.btnPlusCalories).setOnClickListener(v -> modifyTdee(0.05));
    }

    private void modifyTdee(double modifier) {

        if (tdeeResult == 0) return;

        double newModifierPercentage = modifierPercentage + modifier;
        if (newModifierPercentage >= -0.20 && newModifierPercentage <= 0.20) {
            modifierPercentage = newModifierPercentage;
            tdeeResult = (int) (originalTDEE * (1 + modifierPercentage));

            TreeMap<Double, String> intensityModifiers = getDoubleStringLabelModifiersTreeMap();
            if (newModifierPercentage < 0.05 && newModifierPercentage > -0.0001) {
                intensityModifierTextView.setText(intensityModifiers.get(0.00));
                modifierPercentage = 0.00;
            } else {
                intensityModifierTextView.setText(Objects.requireNonNull(
                        intensityModifiers.floorEntry(modifierPercentage)).getValue()
                );
            }

            for (NumberPicker numberPicker : numberPickers) {
                numberPicker.setValue((int) (
                        tdeeResult * getMacroPercentageForPicker(numberPicker) / (numberPicker == fatNumberPicker ? 9 : 4))
                );
            }

            updateMacroPercentages();

            textViewTdee.setText(String.valueOf(tdeeResult));
            modifierTdeeTextView.setText(
                    Math.abs(modifierPercentage) < 0.00001 ? "0%" : String.format(
                            Locale.getDefault(), "%.0f%%", modifierPercentage * 100
                    )
            );
        }
    }

    private double getMacroPercentageForPicker(NumberPicker numberPicker) {
        if (modifierPercentage == 0) {
            return getMacrosPercentagesForModifier("Mantenimiento")[numberPickers.indexOf(numberPicker)];
        } else if (modifierPercentage < 0) {
            return getMacrosPercentagesForModifier("Definición")[numberPickers.indexOf(numberPicker)];
        } else {
            return getMacrosPercentagesForModifier("Volumen")[numberPickers.indexOf(numberPicker)];
        }
    }

    private void updateMacroPercentages() {
        for (int i = 0; i < numberPickers.size(); i++) {
            NumberPicker numberPicker = numberPickers.get(i);
            double macroPercentage = (double) numberPicker.getValue() * (numberPicker == fatNumberPicker ? 9 : 4) / tdeeResult;
            macroPercentages.get(i).setText(String.format(Locale.getDefault(), "%.0f%%", macroPercentage * 100));
        }
    }

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            requireActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        updateNumberPickers();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        updateNumberPickers();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        updateNumberPickers();
    }

    @Override
    public void onStop() {
        super.onStop();
        resetActionBar();
    }

    private void resetActionBar() {
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle("KaiFit-Pal");
        }
    }
}
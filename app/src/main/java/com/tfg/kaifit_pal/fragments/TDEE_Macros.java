package com.tfg.kaifit_pal.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tfg.kaifit_pal.R;

import java.util.Locale;
import java.util.TreeMap;

public class TDEE_Macros extends Fragment {

    int tdeeResult;

    double newTdee;
    double modifierPercentage = 0;

    TextView textViewTdee, modifierTdeeTextView, intensityModifierTextVier, proteinTextView, fatTextView, carbsTextView, caloriesTextView;
    Button buttonMinusCalories, buttonPlusCalories;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false);
        setupActionBar();

        textViewTdee = view.findViewById(R.id.tdeeResultTextView);
        modifierTdeeTextView = view.findViewById(R.id.modifierTDEEtextView);
        modifierTdeeTextView.setText(String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));
        intensityModifierTextVier = view.findViewById(R.id.intensityModifierTextView);
        intensityModifierTextVier.setText("Mantenimiento");

        // We recover data from the bundle
        if (getArguments() != null) {
            tdeeResult = getArguments().getInt("tdeeResult");
            textViewTdee.setText(String.valueOf(tdeeResult));
        }

        setupButtons(view);


        return view;
    }

    // This method sets up the action bar to show the back arrow and the title of the fragment
    private void setupActionBar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setContentInsetStartWithNavigation(0); // We set the distance between the back arrow and the title to 0 to reduce the space between them
        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("TDEE & Macros");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow_black); // We set the back arrow icon
        }
        setHasOptionsMenu(true);
    }

    // This method is called when the back arrow is pressed to go back to the previous fragment using the bottom navigation view items
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtons(@NonNull View view) {
        buttonMinusCalories = view.findViewById(R.id.btnMinusCalories);
        buttonPlusCalories = view.findViewById(R.id.btnPlusCalories);

        buttonMinusCalories.setOnClickListener(v -> modifyTDEE(-0.05));
        buttonPlusCalories.setOnClickListener(v -> modifyTDEE(0.05));
    }

    private void modifyTDEE(double modifier) {
        // Calculate the new TDEE
        double newModifierPercentage = modifierPercentage + modifier;

        // Check if the new modifier percentage is between -30% and 30%
        if (newModifierPercentage >= -0.30 && newModifierPercentage <= 0.30) {
            // Update the modifier percentage and the new TDEE
            modifierPercentage = newModifierPercentage;
            newTdee = tdeeResult * (1 + modifierPercentage);

            // Update the text views
            textViewTdee.setText(String.valueOf((int) newTdee));
            modifierTdeeTextView.setText(String.format(Locale.getDefault(), "%.0f%%", modifierPercentage * 100));

            TreeMap<Double, String> intensityModifiers = new TreeMap<>();
            intensityModifiers.put(-0.30, "Pérdida extrema de peso");
            intensityModifiers.put(-0.25, "Déficit extremo");
            intensityModifiers.put(-0.20, "Déficit intenso");
            intensityModifiers.put(-0.15, "Déficit moderado-intenso");
            intensityModifiers.put(-0.10, "Definición moderada");
            intensityModifiers.put(-0.05, "Definición ligera");
            intensityModifiers.put(0.00, "Mantenimiento");
            intensityModifiers.put(0.05, "Volumen ligero");
            intensityModifiers.put(0.10, "Volumen moderado");
            intensityModifiers.put(0.15, "Volumen moderado-intenso");
            intensityModifiers.put(0.20, "Volumen intenso");
            intensityModifiers.put(0.25, "Volumen extremo");
            intensityModifiers.put(0.30, "Ganancia extrema de peso");

            String intensityModifierText = intensityModifiers.floorEntry(modifierPercentage).getValue();
            intensityModifierTextVier.setText(intensityModifierText);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetActionBar();
    }

    // This method resets the action bar to its default state and it is called when the fragment is stopped
    private void resetActionBar() {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle("KaiFit-Pal");
        }
    }
}
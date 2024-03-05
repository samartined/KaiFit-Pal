package com.tfg.kaifit_pal;

import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Calculadora#newInstance} factory method to
 * create an instance of this fragment.
 */

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class Calculadora extends Fragment {

    private TextView edadDinamica;
    private Button buttonEdadMas, buttonEdadMenos;
    private int edadPredefinida = 25;

    public Calculadora() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_calculadora, container, false);
        initViews(rootView);
        initListeners();
        setupGenderButtons(rootView);
        initSpinner(rootView);
        initCalculateButton(rootView);
        return rootView;
    }

    // Inicializar vistas y botones
    private void initViews(View rootView) {
        edadDinamica = rootView.findViewById(R.id.textView);
        buttonEdadMenos = rootView.findViewById(R.id.btnMenos);
        buttonEdadMas = rootView.findViewById(R.id.btnMas);
    }

    // Inicializar listeners
    private void initListeners() {

        buttonEdadMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrementarEdad();
            }
        });

        buttonEdadMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementarEdad();
            }
        });
    }

    private void decrementarEdad() {
        if (edadPredefinida > 0) {
            edadPredefinida--;
            edadDinamica.setText(String.valueOf(edadPredefinida));

        }
    }

    private void incrementarEdad() {
        if (edadPredefinida < 100) {
            edadPredefinida++;
            edadDinamica.setText(String.valueOf(edadPredefinida));
        }
    }

    // Método para configurar los botones de género
    private void setupGenderButtons(View rootView) {
        // Control botones de sexo
        Button buttonHombre = rootView.findViewById(R.id.ButtonHombre);
        Button buttonMujer = rootView.findViewById(R.id.ButtonMujer);
        final EditText editTextFemenino = rootView.findViewById(R.id.editTextCadera);

        // Configurar comportamiento botones
        buttonHombre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGender(true, false, editTextFemenino);
            }
        });

        buttonMujer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectGender(false, true, editTextFemenino);
            }
        });
    }

    // Método para seleccionar el género
    private void selectGender(boolean hombreSelected, boolean mujerSelected, EditText editTextFemenino) {

        View view = getView(); // Obtener la vista actual
        if (view != null) { // Verificar que la vista no sea nula
            Button buttonHombre = getView().findViewById(R.id.ButtonHombre);
            Button buttonMujer = getView().findViewById(R.id.ButtonMujer);

            if (buttonHombre != null && buttonMujer != null) { // Habilitar o deshabilitar los botones verificando que no sean nulos
                buttonHombre.setSelected(hombreSelected);
                buttonHombre.setBackgroundResource(hombreSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
                buttonMujer.setSelected(mujerSelected);
                buttonMujer.setBackgroundResource(mujerSelected ? R.drawable.rect_button_pressed : R.drawable.rect_button_notpressed);
            }

            if (editTextFemenino != null) { // Habilitar o deshabilitar el campo de texto verificando que no sea nulo
                editTextFemenino.setEnabled(mujerSelected);
                editTextFemenino.setBackgroundResource(mujerSelected ? R.drawable.edittext_borders : R.drawable.edittext_disabled_background);
            }
        }
    }

    // Inicializar spinner de actividad deportiva
    private void initSpinner(View view) {

        Spinner spinnerFactorActividad = view.findViewById(R.id.spinnerFactorActividad);
        spinnerFactorActividad.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Obtener la cadena seleccionada
                String[] cocientesActividad = getResources().getStringArray(R.array.cocientes);

                // Convertir la cadena a float
                float factorActividadSeleccionado = Float.parseFloat(cocientesActividad[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Implementa acciones si no se selecciona nada
            }
        });
    }

    // Inicializar botón de calcular
    private void initCalculateButton(View rootView) {
        Button buttonCalcular = rootView.findViewById(R.id.buttonCalcular);
    }
}
package com.tfg.kaifit_pal;

import android.os.Bundle;

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
        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        // Se añade gradiente del título programáticamente
        TextView tituloCalculadora = rootView.findViewById(R.id.tituloCalculadora);
        float width = tituloCalculadora.getPaint().measureText("Calculadora KaiFit");
        Shader textShader = new LinearGradient(0, 0, width, tituloCalculadora.getTextSize(),
                new int[]{Color.parseColor("#87CEFA"), Color.parseColor("#32CD32")},
                null, Shader.TileMode.CLAMP);
        tituloCalculadora.getPaint().setShader(textShader);

        // Comportamiento botones edad
        edadDinamica = rootView.findViewById(R.id.textView);
        buttonEdadMenos = rootView.findViewById(R.id.btnMenos);
        buttonEdadMas = rootView.findViewById(R.id.btnMas);

        edadDinamica.setText(String.valueOf(edadPredefinida));
        buttonEdadMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edadPredefinida > 0) {
                    edadPredefinida--;
                    edadDinamica.setText(String.valueOf(edadPredefinida));
                }
            }
        });

        buttonEdadMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edadPredefinida < 100) {
                    edadPredefinida++;
                    edadDinamica.setText(String.valueOf(edadPredefinida));
                }
            }
        });

        // Control botones de sexo
        Button buttonHombre = rootView.findViewById(R.id.ButtonHombre);
        Button buttonMujer = rootView.findViewById(R.id.ButtonMujer);
        final EditText editTextFemenino = rootView.findViewById(R.id.editTextCadera);

        // Configurar comportamiento botones

        buttonHombre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Marcar el botón "Hombre" y desmarcar el botón "Mujer"
                buttonHombre.setSelected(true);
                buttonHombre.setBackgroundResource(R.drawable.rect_button_pressed);
                buttonMujer.setSelected(false);
                buttonMujer.setBackgroundResource(R.drawable.rect_button_notpressed);

                // Deshabilitar el EditText y cambiar su estilo visual para que solo se permite interactuar con el si se selecciona el buttonMujer
                editTextFemenino.setEnabled(false);
                editTextFemenino.setBackgroundResource(R.drawable.edittext_disabled_background);
            }
        });

        buttonMujer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Marcar el botón "Hombre" y desmarcar el botón "Mujer"
                buttonHombre.setSelected(true);
                buttonHombre.setBackgroundResource(R.drawable.rect_button_notpressed);
                buttonMujer.setSelected(false);
                buttonMujer.setBackgroundResource(R.drawable.rect_button_pressed);

                // Habilitar el EditText y cambiar su estilo visual
                editTextFemenino.setEnabled(true);
                editTextFemenino.setBackgroundResource(R.drawable.edittext_borders);
            }
        });

        Spinner spinnerActividadDeportiva = rootView.findViewById(R.id.spinnerFactorActividad);
        spinnerActividadDeportiva.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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

        // Inicializar botón de calcular
        Button buttonCalcular = rootView.findViewById(R.id.buttonCalcular);

        return rootView;
    }
}
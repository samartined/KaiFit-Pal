package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView edadDinamica;
    private Button buttonEdadMas, buttonEdadMenos;
    private int edadPredefinida = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Se añade gradiente del título programáticamente
        TextView tituloCalculadora = findViewById(R.id.tituloCalculadora);
        float width = tituloCalculadora.getPaint().measureText("Calculadora KaiFit");
        Shader textShader = new LinearGradient(0, 0, width, tituloCalculadora.getTextSize(),
                new int[]{Color.parseColor("#87CEFA"), Color.parseColor("#32CD32")},
                null, Shader.TileMode.CLAMP);
        tituloCalculadora.getPaint().setShader(textShader);

        // Comportamiento botones edad
        edadDinamica = findViewById(R.id.textView);
        buttonEdadMenos = findViewById(R.id.btnMenos);
        buttonEdadMas = findViewById(R.id.btnMas);

        edadDinamica.setText(String.valueOf(edadPredefinida));
        buttonEdadMenos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Restar 1 a la cantidad
                if (edadPredefinida > 0) {
                    edadPredefinida--;
                    edadDinamica.setText(String.valueOf(edadPredefinida));
                }
            }
        });

        buttonEdadMas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sumar 1 a la cantidad
                if (edadPredefinida < 100) {
                    edadPredefinida++;
                    edadDinamica.setText(String.valueOf(edadPredefinida));
                }
            }
        });

        // Control botones de sexo
        Button buttonHombre = findViewById(R.id.ButtonHombre);
        Button buttonMujer = findViewById(R.id.ButtonMujer);
        final EditText editTextFemenino = findViewById(R.id.editTextCadera);

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

        Spinner spinnerActividadDeportiva = findViewById(R.id.spinnerFactorActividad);
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

        Button buttonCalcular = findViewById(R.id.buttonCalcular);

        GradientDrawable gradientDrawable = new GradientDrawable(

                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        Color.parseColor("#87CEFA"),
                        Color.parseColor("#32CD32")
                });
        gradientDrawable.setCornerRadius(200);
        buttonCalcular.setBackground(gradientDrawable);
    }
}
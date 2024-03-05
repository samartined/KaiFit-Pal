package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Comprueba si se ha guardado previamente una instancia del estado
        if (savedInstanceState == null) {
            // Si no hay ninguna instancia guardada, añade el fragmento predeterminado
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_view, new Calculadora())
                    .commit();
        }
    }
}
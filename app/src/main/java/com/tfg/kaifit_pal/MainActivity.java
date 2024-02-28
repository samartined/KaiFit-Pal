package com.tfg.kaifit_pal;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tituloCalculadora = findViewById(R.id.tituloCalculadora);
        Shader textShader = new LinearGradient(0, 0, tituloCalculadora.getPaint().measureText("Calculadora KaiFit"), 0,
                new int[]{Color.BLUE, Color.GREEN, Color.GREEN, Color.BLUE},
                new float[]{0, 0.5f, 0.7f, 1}, Shader.TileMode.CLAMP);
        tituloCalculadora.getPaint().setShader(textShader);


    }
}
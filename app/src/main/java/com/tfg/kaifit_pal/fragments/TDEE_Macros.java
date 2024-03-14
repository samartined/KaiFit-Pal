package com.tfg.kaifit_pal.fragments;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;

// ...

import com.tfg.kaifit_pal.R;

public class TDEE_Macros extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false);

        // We set the toolbar
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar); // This method allows us to use the toolbar as the action bar

        // We instantiate the action bar and set the back button
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle("TDEE & Macros");
            actionBar.setDisplayHomeAsUpEnabled(true); // This method allows us to show the back button in the action bar
            actionBar.setDisplayShowHomeEnabled(true); // The difference between this method and the previous one is that this one shows the back button as a home button

            // We change the color of the back button. We use the DrawableCompat class to change the color of the back button because the setTint method is not available in the Drawable class
            final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
            DrawableCompat.setTint(upArrow, Color.WHITE); // Replace Color.WHITE with your desired color
            actionBar.setHomeAsUpIndicator(upArrow);
        }

        setHasOptionsMenu(true);
        return view;
    }

    // We override the method to show the back button in the action bar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // We remove the back button from the action bar
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }
    }
}
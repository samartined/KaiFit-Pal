package com.tfg.kaifit_pal.utilities

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tfg.kaifit_pal.R

object AppBarHandler {
    fun setUpActionBar(
        activity: AppCompatActivity,
        title: String?,
        displayHomeAsUpEnabled: Boolean,
        displayShowHomeEnabled: Boolean
    ) {
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        toolbar.contentInsetStartWithNavigation = 0
        activity.setSupportActionBar(toolbar)

        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.title = title
            actionBar.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
            actionBar.setDisplayShowHomeEnabled(displayShowHomeEnabled)
            if (displayHomeAsUpEnabled) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow_black)
            }
        }
    }

    fun resetActionBar(activity: AppCompatActivity) {
        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.title = "KaiFit-Pal"
        }
    }

    fun setTitle(activity: AppCompatActivity, title: String?) {
        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.title = title
        }
    }
}
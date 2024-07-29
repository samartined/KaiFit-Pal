package com.tfg.kaifit_pal.views.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.logic.MacrosManager
import java.util.Arrays
import java.util.Locale
import java.util.Objects
import java.util.TreeMap
import kotlin.math.abs

class TdeeMacros : Fragment() {
    private var tdeeResult = 0
    private var originalTDEE = 0
    private var modifierPercentage = 0.0
    private var userChangedPickers = false
    private var textViewTdee: TextView? = null
    private var modifierTdeeTextView: TextView? = null
    private var intensityModifierTextView: TextView? = null
    private var proteinsNumberPicker: NumberPicker? = null
    private var fatNumberPicker: NumberPicker? = null
    private var carbsNumberPicker: NumberPicker? = null
    var macroPercentages: ArrayList<TextView> = ArrayList()
    private var numberPickers: ArrayList<NumberPicker?>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_t_d_e_e__macros, container, false)
        setUpActionBar()
        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        initializeUIComponents(view)
        return view
    }

    private fun setUpActionBar() {
        val activity = checkNotNull(activity as AppCompatActivity?)
        val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
        toolbar.contentInsetStartWithNavigation = 0
        activity.setSupportActionBar(toolbar)

        val actionBar = activity.supportActionBar
        if (actionBar != null) {
            actionBar.title = "TDEE & Macros"
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow_black)
        }
        setHasOptionsMenu(true)
    }

    private fun initializeUIComponents(view: View) {
        textViewTdee = view.findViewById(R.id.tdeeResultTextView)
        modifierTdeeTextView = view.findViewById(R.id.modifierTDEEtextView)
        intensityModifierTextView = view.findViewById(R.id.intensityModifierTextView)

        proteinsNumberPicker = view.findViewById(R.id.proteinsNumberPicker)
        fatNumberPicker = view.findViewById(R.id.fatsNumberPicker)
        carbsNumberPicker = view.findViewById(R.id.carbsNumberPicker)

        if (arguments != null) {
            tdeeResult = arguments!!.getInt("tdeeResult")
            originalTDEE = tdeeResult
            textViewTdee.setText(tdeeResult.toString())
        }

        setUpButtons(view)
        setUpTextViews(view)
        setUpNumberPickers()
    }

    private fun setUpTextViews(view: View) {
        val macroTitles = ArrayList(
            Arrays.asList(
                view.findViewById(R.id.proteinTitle),
                view.findViewById(R.id.fatTitle),
                view.findViewById<TextView>(R.id.carbTitle)
            )
        )

        for (titles in macroTitles) {
            titles.setTextColor(resources.getColor(R.color.black))
            titles.textSize = 15f
            titles.setTypeface(titles.typeface, Typeface.BOLD)
            titles.gravity = 1
        }

        macroPercentages = ArrayList(
            Arrays.asList(
                view.findViewById(R.id.proteinsPercentageTextView),
                view.findViewById(R.id.fatsPercentageTextView),
                view.findViewById(R.id.carbsPercentageTextView)
            )
        )

        for (macroPercentage in macroPercentages) {
            macroPercentage.textSize = 15f
            macroPercentage.setTypeface(macroPercentage.typeface, Typeface.BOLD)
            macroPercentage.gravity = 1
        }
    }

    private fun setUpNumberPickers() {
        numberPickers =
            ArrayList(Arrays.asList(proteinsNumberPicker, fatNumberPicker, carbsNumberPicker))
        val defaultMacrosPercentages =
            MacrosManager.getMacrosPercentagesForModifier("Mantenimiento")

        for (numberPicker in numberPickers!!) {
            if (tdeeResult == 0) {
                numberPicker!!.gravity = 1
                numberPicker.minValue = 0
                numberPicker.maxValue = 0
                numberPicker.value = 0
            } else {
                numberPicker!!.gravity = 1
                numberPicker.minValue = 0
                numberPicker.maxValue =
                    tdeeResult / (if (numberPicker === fatNumberPicker) 9 else 4)
                numberPicker.value =
                    (tdeeResult * ((if (numberPicker === proteinsNumberPicker) defaultMacrosPercentages!![0] else (if (numberPicker === fatNumberPicker) defaultMacrosPercentages!![1] else defaultMacrosPercentages!![2]))!!) / (if (numberPicker === fatNumberPicker) 9 else 4)).toInt()
            }

            numberPicker.setFormatter { value -> "$value g" }

            numberPicker.setOnValueChangedListener { picker: NumberPicker?, oldVal: Int, newVal: Int ->
                userChangedPickers = true
                updateNumberPickers()
                updateMacroPercentages()
            }
        }

        for (macroPercentage in macroPercentages) {
            if (proteinsNumberPicker!!.value == 0 || fatNumberPicker!!.value == 0 || carbsNumberPicker!!.value == 0) {
                macroPercentage.text = "0%"
            } else {
                macroPercentage.text = String.format(
                    Locale.getDefault(), "%.0f%%",
                    (if (macroPercentage === macroPercentages[0]) defaultMacrosPercentages!![0] else (if (macroPercentage === macroPercentages[1]) defaultMacrosPercentages!![1] else defaultMacrosPercentages!![2]))!! * 100
                )
            }
        }
    }

    private fun setUpButtons(view: View) {
        view.findViewById<View>(R.id.btnMinusCalories)
            .setOnClickListener { v: View? -> modifyTdee(-0.05) }
        view.findViewById<View>(R.id.btnPlusCalories)
            .setOnClickListener { v: View? -> modifyTdee(0.05) }
    }

    private fun modifyTdee(modifier: Double) {
        if (tdeeResult == 0) return

        val newModifierPercentage = modifierPercentage + modifier
        if (newModifierPercentage >= -0.20 && newModifierPercentage <= 0.20) {
            modifierPercentage = newModifierPercentage
            tdeeResult = (originalTDEE * (1 + modifierPercentage)).toInt()

            val intensityModifiers = doubleStringLabelModifiersTreeMap
            if (newModifierPercentage < 0.05 && newModifierPercentage > -0.0001) {
                intensityModifierTextView!!.text = intensityModifiers[0.00]
                modifierPercentage = 0.00
            } else {
                intensityModifierTextView!!.text = Objects.requireNonNull(
                    intensityModifiers.floorEntry(modifierPercentage)
                ).value
            }

            for (numberPicker in numberPickers!!) {
                numberPicker!!.value =
                    (tdeeResult * getMacroPercentageForPicker(numberPicker) / (if (numberPicker === fatNumberPicker) 9 else 4)).toInt()
            }

            updateMacroPercentages()

            textViewTdee!!.text = tdeeResult.toString()
            modifierTdeeTextView!!.text =
                if (abs(modifierPercentage) < 0.00001) "0%" else String.format(
                    Locale.getDefault(), "%.0f%%", modifierPercentage * 100
                )
        }
    }

    private fun getMacroPercentageForPicker(numberPicker: NumberPicker?): Double {
        return if (modifierPercentage == 0.0) {
            MacrosManager.getMacrosPercentagesForModifier("Mantenimiento")[numberPickers!!.indexOf(
                numberPicker
            )]
        } else if (modifierPercentage < 0) {
            MacrosManager.getMacrosPercentagesForModifier("Definición")[numberPickers!!.indexOf(
                numberPicker
            )]
        } else {
            MacrosManager.getMacrosPercentagesForModifier("Volumen")[numberPickers!!.indexOf(
                numberPicker
            )]
        }
    }

    private fun updateMacroPercentages() {
        for (i in numberPickers!!.indices) {
            val numberPicker = numberPickers!![i]
            val macroPercentage =
                numberPicker!!.value.toDouble() * (if (numberPicker === fatNumberPicker) 9 else 4) / tdeeResult
            macroPercentages[i].text =
                String.format(Locale.getDefault(), "%.0f%%", macroPercentage * 100)
        }
    }

    private fun updateNumberPickers() {
        if (!userChangedPickers) return
        var isManualChange = true

        tdeeResult = 0
        for (numberPicker in numberPickers!!) {
            tdeeResult += numberPicker!!.value * (if (numberPicker === proteinsNumberPicker) 4 else (if (numberPicker === fatNumberPicker) 9 else 4))
        }

        if (modifierPercentage == 0.0) {
            originalTDEE = tdeeResult
        }

        textViewTdee!!.text = tdeeResult.toString()
        userChangedPickers = false
        isManualChange = false

        updateMacroPercentages()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireActivity().onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        updateNumberPickers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateNumberPickers()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        updateNumberPickers()
    }

    override fun onStop() {
        super.onStop()
        resetActionBar()
    }

    private fun resetActionBar() {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setDisplayShowHomeEnabled(false)
            actionBar.title = "KaiFit-Pal"
        }
    }

    companion object {
        private val doubleStringLabelModifiersTreeMap: TreeMap<Double, String>
            get() {
                val intensityModifiers = TreeMap<Double, String>()
                intensityModifiers[-0.20] = "Déficit intenso"
                intensityModifiers[-0.15] = "Déficit moderado-intenso"
                intensityModifiers[-0.10] = "Déficit moderado"
                intensityModifiers[-0.05] = "Déficit ligero"
                intensityModifiers[0.00] = "Mantenimiento"
                intensityModifiers[0.05] = "Volumen ligero"
                intensityModifiers[0.10] = "Volumen moderado"
                intensityModifiers[0.15] = "Volumen moderado-intenso"
                intensityModifiers[0.20] = "Volumen intenso"
                return intensityModifiers
            }
    }
}
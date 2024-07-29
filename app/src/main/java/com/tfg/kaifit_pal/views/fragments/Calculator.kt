package com.tfg.kaifit_pal.views.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.logic.CalculatorLogic
import com.tfg.kaifit_pal.utilities.DataParser
import kotlin.math.max
import kotlin.math.min

class Calculator : Fragment() {
    private var tdeeResult = 0
    private var fatPercentage = 0.0
    private var dynamicAge: TextView? = null
    private var fatPercentageEditText: TextView? = null
    private var weightEditText: EditText? = null
    private var heightEditText: EditText? = null
    private var neckEditText: EditText? = null
    private var waistEditText: EditText? = null
    private var hipEditText: EditText? = null
    private var femaleButton: Button? = null
    private var activityFactorSpinner: Spinner? = null

    private var calculatorInstance: CalculatorLogic? = null
    private var callback: CalculateListenerInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callback = context as CalculateListenerInterface
        } catch (e: ClassCastException) {
            throw ClassCastException("$context debe implementar OnCalculateClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calculator, container, false)
        dynamicAge = view.findViewById(R.id.ageTextView)
        setUpComponents(view)

        view.findViewById<View>(R.id.buttonCalculate).setOnClickListener { v: View? ->
            calculateTDEEAndNotify(view)
        }
        setUpTextChangeListeners(view)

        return view
    }

    private fun calculateTDEEAndNotify(view: View) {
        val isFemale = femaleButton!!.isSelected
        val age: Int = DataParser.Companion.parseIntUtility(dynamicAge!!.text.toString())
        val height: Int = DataParser.Companion.parseIntUtility(
            heightEditText!!.text.toString().trim { it <= ' ' })
        val weight: Double = DataParser.Companion.parseDoubleUtility(
            weightEditText!!.text.toString().trim { it <= ' ' })
        val neck: Double = DataParser.Companion.parseDoubleUtility(
            neckEditText!!.text.toString().trim { it <= ' ' })
        val waist: Double = DataParser.Companion.parseDoubleUtility(
            waistEditText!!.text.toString().trim { it <= ' ' })
        val hip: Double = DataParser.Companion.parseDoubleUtility(
            hipEditText!!.text.toString().trim { it <= ' ' })
        val activityFactor = getActivityFactor(view)

        calculatorInstance = CalculatorLogic.Companion.createInstance(
            isFemale,
            age,
            height.toDouble(),
            weight,
            neck,
            waist,
            hip
        )
        calculatorInstance?.activityFactor = activityFactor

        tdeeResult = calculatorInstance!!.calculateTDEE()
        callback!!.onCalculateClick(tdeeResult)
    }

    private fun setUpComponents(view: View) {
        view.findViewById<View>(R.id.btnMinus).setOnClickListener { v: View? -> updateAge(-1) }
        view.findViewById<View>(R.id.btnPlus).setOnClickListener { v: View? -> updateAge(1) }
        view.findViewById<View>(R.id.ButtonMale)
            .setOnClickListener { v: View? -> selectGender(true, view) }
        view.findViewById<View>(R.id.ButtonFemale)
            .setOnClickListener { v: View? -> selectGender(false, view) }

        setUpInfoImageViews(view)
    }


    private fun setUpInfoImageViews(view: View) {
        view.findViewById<View>(R.id.waistInfo).setOnClickListener { v: View? ->
            showInfoDialog(
                "Para medir la cintura, " +
                        "coloca la cinta métrica alrededor de tu cintura si eres hombre " +
                        "o justo por encima de tu ombligo si eres mujer."
            )
        }

        view.findViewById<View>(R.id.neckInfo).setOnClickListener { v: View? ->
            showInfoDialog(
                "Para medir el cuello," +
                        " coloca la cinta métrica alrededor de tu cuello, " +
                        "justo por debajo de la laringe y ligeramente inclinada hacia adelante."
            )
        }

        view.findViewById<View>(R.id.hipsInfo).setOnClickListener { v: View? ->
            showInfoDialog(
                "Esta medida es opcional para hombres y obligatoria para mujeres. " +
                        "Para medir las caderas, coloca la cinta métrica alrededor de la parte más ancha de tus caderas."
            )
        }

        view.findViewById<View>(R.id.percentInfo).setOnClickListener { v: View? ->
            showInfoDialog(
                "El porcentaje de grasa corporal es un número que representa " +
                        "la cantidad de grasa en tu cuerpo en relación con tu peso total."
            )
        }

        view.findViewById<View>(R.id.actFactorInfo).setOnClickListener { v: View? ->
            showInfoDialog(
                "El factor de actividad es " +
                        "un número que representa tu nivel de actividad física. " +
                        "Cuanto más activo seas, mayor será tu factor de actividad."
            )
        }
    }

    private fun showInfoDialog(message: String) {
        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


    private fun updateAge(change: Int) {
        var predefinedAge = dynamicAge!!.text.toString().toInt()
        predefinedAge = max(18.0, min(90.0, (predefinedAge + change).toDouble()))
            .toInt()
        dynamicAge!!.text = predefinedAge.toString()
    }

    private fun selectGender(sex: Boolean, view: View) {
        val maleButton = view.findViewById<Button>(R.id.ButtonMale)
        femaleButton = view.findViewById(R.id.ButtonFemale)
        val femaleEditText = view.findViewById<EditText>(R.id.editTextHip)

        maleButton.isSelected = sex
        femaleEditText.setText("")
        maleButton.setBackgroundResource(if (sex) R.drawable.rect_button_pressed else R.drawable.rect_button_notpressed)

        val localFemaleButton = femaleButton
        localFemaleButton?.setSelected(!sex)

        localFemaleButton?.setBackgroundResource(
            if (!sex)
                R.drawable.rect_button_pressed
            else
                R.drawable.rect_button_notpressed
        )

        femaleEditText.isEnabled = !sex
        femaleEditText.setBackgroundResource(if (!sex) R.drawable.edittext_borders else R.drawable.edittext_disabled_background)
    }

    private fun setUpTextChangeListeners(view: View) {
        weightEditText = view.findViewById(R.id.editTextWeight)
        heightEditText = view.findViewById(R.id.editTextHeight)
        neckEditText = view.findViewById(R.id.editTextNeck)
        waistEditText = view.findViewById(R.id.editTextWaist)
        hipEditText = view.findViewById(R.id.editTextHip)
        fatPercentageEditText = view.findViewById(R.id.TextViewFatPercent)
        femaleButton = view.findViewById(R.id.ButtonFemale)
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                val localDynamicAge = dynamicAge
                val localFemaleButton = femaleButton
                val localWeightEditText = weightEditText
                val localHeightEditText = heightEditText
                val localNeckEditText = neckEditText
                val localWaistEditText = waistEditText
                val localHipEditText = hipEditText

                val age = localDynamicAge!!.text.toString().toInt()
                val sex = localFemaleButton?.isSelected ?: false

                val valueWeight: Double = DataParser.Companion.parseDoubleUtility(
                    localWeightEditText?.getText().toString().trim { it <= ' ' })

                val valueHeight: Int = DataParser.Companion.parseIntUtility(
                    localHeightEditText?.getText().toString().trim { it <= ' ' })

                val valueNeck: Double = DataParser.Companion.parseDoubleUtility(
                    localNeckEditText?.getText().toString().trim { it <= ' ' })

                val valueWaist: Double = DataParser.Companion.parseDoubleUtility(
                    localWaistEditText?.getText().toString().trim { it <= ' ' })

                val valueHip: Double = DataParser.Companion.parseDoubleUtility(
                    localHipEditText?.getText().toString().trim { it <= ' ' })

                calculatorInstance = CalculatorLogic.Companion.createInstance(
                    sex,
                    age,
                    valueHeight.toDouble(),
                    valueWeight,
                    valueNeck,
                    valueWaist,
                    valueHip
                )
                fatPercentage = calculatorInstance.getFatPercentage()

                if (valueHeight == 0 && valueWeight == 0.0 && valueNeck == 0.0 && valueWaist == 0.0 && valueHip == 0.0) {
                    fatPercentageEditText.setText("")
                } else if (valueHeight == 0 || valueWeight == 0.0 || valueNeck == 0.0 || valueWaist == 0.0 || (sex && valueHip == 0.0)) {
                    fatPercentageEditText.setText("Calculando...")
                } else if (java.lang.Double.isNaN(fatPercentage) || java.lang.Double.isInfinite(
                        fatPercentage
                    ) || fatPercentage < 0 || fatPercentage > 100
                ) {
                    fatPercentageEditText.setText("Datos incorrectos.")
                } else {
                    fatPercentageEditText.setText(String.format("%.2f%%", fatPercentage))
                }
            }

            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }
        }

        weightEditText.addTextChangedListener(textWatcher)
        heightEditText.addTextChangedListener(textWatcher)
        neckEditText.addTextChangedListener(textWatcher)
        waistEditText.addTextChangedListener(textWatcher)
        hipEditText.addTextChangedListener(textWatcher)
    }

    private fun getActivityFactor(view: View): Double {
        activityFactorSpinner = view.findViewById(R.id.spinnerActivityFactor)

        val selectedFactorIndex = activityFactorSpinner.getSelectedItemPosition()

        val activityFactorValues = resources.getStringArray(R.array.activity_factors)

        if (selectedFactorIndex >= 0 && selectedFactorIndex < activityFactorValues.size) { // This condition

            try {
                return activityFactorValues[selectedFactorIndex].toDouble()
            } catch (e: NumberFormatException) {
                Log.e(
                    "Calculator",
                    "Invalid activity factor: " + activityFactorValues[selectedFactorIndex]
                )
            }
        } else {
            Log.e("Calculator", "Invalid selectedFactorIndex: $selectedFactorIndex")
        }
        return 1.2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post { selectGender(true, view) }
    }
}
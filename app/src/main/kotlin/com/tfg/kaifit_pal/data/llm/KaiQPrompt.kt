package com.tfg.kaifit_pal.data.llm

import com.tfg.kaifit_pal.data.profile.SavedProfile
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.Goal
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeMethod
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * System prompt of Kai-Q: its persona and, when there is one, the profile the user saved.
 *
 * It is input for the model, not text of the interface, so it lives in code and not in the
 * string resources. It is always in Spanish, with numbers in the Spanish format whatever the
 * language of the device, so the model gets the same text everywhere. The model is asked for
 * plain text, which is how the chat shows its replies.
 */
object KaiQPrompt {

    private val SPANISH: Locale = Locale.forLanguageTag("es-ES")

    private val PERSONA = """
        Eres Kai-Q, el asistente de nutrición y entrenamiento de la app KaiFit-Pal.
        Respondes siempre en español, de forma breve, clara y práctica.
        Escribes solo texto sin formato: no uses Markdown, ni asteriscos, ni almohadillas, ni tablas.
        No eres médico: no diagnosticas enfermedades ni recetas medicamentos ni tratamientos.
        Si el usuario menciona una enfermedad, un embarazo, una medicación o un trastorno de la conducta alimentaria, le recomiendas que consulte a un médico o a un dietista-nutricionista antes de cambiar su alimentación o su entrenamiento.
        Si no sabes algo, lo dices en lugar de inventarlo.
    """.trimIndent()

    private val NO_PROFILE = """
        El usuario aún no ha guardado sus datos en la app. Si la pregunta depende de ellos (por ejemplo, cuántas calorías o cuánta proteína necesita), sugiérele que use la Calculadora de KaiFit-Pal y guarde el resultado en su perfil.
    """.trimIndent()

    /** The system prompt for a conversation with the user of [profile], or with no profile saved. */
    fun systemInstruction(profile: SavedProfile?): String =
        PERSONA + "\n\n" + if (profile == null) NO_PROFILE else profileBlock(profile)

    private fun profileBlock(profile: SavedProfile): String {
        val m = profile.measurements
        val tdee = profile.tdee
        val bodyFat = tdee.bodyFatPercent
        val targetKcal = profile.goalModifier.applyTo(tdee.tdeeKcal)
        val plan = profile.macroPlan
        return buildString {
            appendLine("Datos del usuario, guardados en la app:")
            appendLine("- Sexo: ${m.sex.label}")
            appendLine("- Edad: ${m.ageYears} años")
            appendLine("- Altura: ${decimal(m.heightCm)} cm")
            appendLine("- Peso: ${decimal(m.weightKg)} kg")
            if (bodyFat != null) {
                appendLine("- Grasa corporal: ${String.format(SPANISH, "%.1f", bodyFat)} %, estimada con la fórmula US Navy")
            } else {
                appendLine("- Grasa corporal: sin estimar")
            }
            appendLine("- Nivel de actividad: ${profile.activityLevel.label}, factor ${decimal(profile.activityLevel.factor)}")
            appendLine("- Gasto energético diario (TDEE): ${tdee.tdeeKcal} kcal/día, calculado con ${tdee.method.label}")
            appendLine(
                "- Objetivo: ${profile.goalModifier.goal.label}, ${signed(profile.goalModifier.percent)} % " +
                    "sobre el TDEE, es decir, $targetKcal kcal/día",
            )
            appendLine(
                "- Macronutrientes al día: ${plan.proteinGrams} g de proteína, ${plan.fatGrams} g de grasa " +
                    "y ${plan.carbGrams} g de carbohidratos",
            )
            append(
                "Usa estos datos para personalizar tus respuestas. Si el usuario dice que han cambiado, " +
                    "sugiérele que vuelva a calcularlos en la Calculadora.",
            )
        }
    }

    /** Up to three decimals, without trailing zeros: 180 → "180", 80,5 → "80,5", 1,375 → "1,375". */
    private fun decimal(value: Double): String = DecimalFormat("0.###", DecimalFormatSymbols(SPANISH)).format(value)

    private fun signed(percent: Int): String = if (percent > 0) "+$percent" else percent.toString()

    private val Sex.label: String
        get() = when (this) {
            Sex.MALE -> "Hombre"
            Sex.FEMALE -> "Mujer"
        }

    private val ActivityLevel.label: String
        get() = when (this) {
            ActivityLevel.SEDENTARY -> "sedentario (poco o ningún ejercicio)"
            ActivityLevel.LIGHT -> "ligero (ejercicio 1 a 3 días a la semana)"
            ActivityLevel.MODERATE -> "moderado (ejercicio 3 a 5 días a la semana)"
            ActivityLevel.ACTIVE -> "activo (ejercicio 6 o 7 días a la semana)"
            ActivityLevel.VERY_ACTIVE -> "muy activo (entrenamiento diario intenso o trabajo físico)"
        }

    private val TdeeMethod.label: String
        get() = when (this) {
            TdeeMethod.KATCH_MCARDLE -> "Katch-McArdle"
            TdeeMethod.MIFFLIN_ST_JEOR -> "Mifflin-St Jeor"
        }

    private val Goal.label: String
        get() = when (this) {
            Goal.DEFICIT -> "déficit"
            Goal.MAINTENANCE -> "mantenimiento"
            Goal.SURPLUS -> "superávit"
        }
}

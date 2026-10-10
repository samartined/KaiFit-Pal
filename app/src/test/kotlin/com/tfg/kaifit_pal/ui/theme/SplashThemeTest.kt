package com.tfg.kaifit_pal.ui.theme

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.core.splashscreen.R as SplashR

/**
 * The launch theme and the window theme, as the system reads them: the splash icon on its brand
 * circle, and a window background equal to the splash background in both themes, so closing the
 * splash screen shows no other color.
 */
@RunWith(AndroidJUnit4::class)
class SplashThemeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // SP1
    @Test
    fun startingThemeShowsTheIconForegroundOnTheLightPrimary() {
        val theme = theme(R.style.Theme_KaiFitPal_Starting)

        assertEquals(R.drawable.ic_launcher_foreground, theme.resolve(SplashR.attr.windowSplashScreenAnimatedIcon).resourceId)
        assertEquals(0xFF3B7D2C.toInt(), theme.color(SplashR.attr.windowSplashScreenIconBackgroundColor))
        assertEquals(0xFFF5F5EE.toInt(), theme.color(SplashR.attr.windowSplashScreenBackground))
    }

    // SP2
    @Test
    @Config(qualifiers = "night")
    fun startingThemeInDarkModeUsesTheDarkPrimaryContainerAndBackground() {
        val theme = theme(R.style.Theme_KaiFitPal_Starting)

        assertEquals(R.drawable.ic_launcher_foreground, theme.resolve(SplashR.attr.windowSplashScreenAnimatedIcon).resourceId)
        assertEquals(0xFF245F17.toInt(), theme.color(SplashR.attr.windowSplashScreenIconBackgroundColor))
        assertEquals(0xFF121410.toInt(), theme.color(SplashR.attr.windowSplashScreenBackground))
    }

    // SP3
    @Test
    fun windowBackgroundIsTheLightSplashBackground() {
        assertEquals(0xFFF5F5EE.toInt(), windowBackgroundColor(R.style.Theme_KaiFitPal))
    }

    // SP4
    @Test
    @Config(qualifiers = "night")
    fun windowBackgroundInDarkModeIsTheDarkSplashBackground() {
        assertEquals(0xFF121410.toInt(), windowBackgroundColor(R.style.Theme_KaiFitPal))
    }

    private fun theme(@StyleRes style: Int): Resources.Theme =
        context.resources.newTheme().apply { applyStyle(style, true) }

    /** The value of [attr] in this theme, following references to their final resource. */
    private fun Resources.Theme.resolve(@AttrRes attr: Int): TypedValue {
        val value = TypedValue()
        assertTrue("attribute not set in the theme", resolveAttribute(attr, value, true))
        return value
    }

    /** The color that [attr] resolves to in this theme. */
    private fun Resources.Theme.color(@AttrRes attr: Int): Int {
        val value = resolve(attr)
        assertTrue(
            "not a color: type ${value.type}",
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT,
        )
        return value.data
    }

    /** The window background of [style], which must be a plain color. */
    private fun windowBackgroundColor(@StyleRes style: Int): Int {
        val attributes = theme(style).obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        try {
            val drawable = attributes.getDrawable(0)
            assertTrue("not a plain color: $drawable", drawable is ColorDrawable)
            return (drawable as ColorDrawable).color
        } finally {
            attributes.recycle()
        }
    }
}

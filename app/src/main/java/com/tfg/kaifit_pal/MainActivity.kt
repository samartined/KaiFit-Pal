package com.tfg.kaifit_pal

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.tfg.kaifit_pal.utilities.AppBarHandler
import com.tfg.kaifit_pal.views.fragments.CalculateListenerInterface
import com.tfg.kaifit_pal.views.fragments.Calculator
import com.tfg.kaifit_pal.views.fragments.Help
import com.tfg.kaifit_pal.views.fragments.Profile
import com.tfg.kaifit_pal.views.fragments.Settings
import com.tfg.kaifit_pal.views.fragments.TdeeMacros
import com.tfg.kaifit_pal.views.fragments.kaiq.KaiQ
import java.util.Arrays

/**
 * MainActivity class that extends AppCompatActivity and implements Calculator.OnCalculateClickListener
 * This class is the main activity of the app, it contains the bottom navigation view and the fragments
 * that are displayed when the user clicks on the bottom navigation view.
 */
class MainActivity : AppCompatActivity(), CalculateListenerInterface {
    private var childFragmentTdee: TdeeMacros? = null
    private var bottomNavigationView: BottomNavigationView? = null
    private var fragmentManager: FragmentManager? = null
    private var defaultFragment: Fragment? = null
    private var currentFragment: Fragment? = null

    private val mainFragments: List<Class<out Fragment>> = Arrays.asList(
        Profile::class.java,
        Calculator::class.java,
        KaiQ::class.java,
        Help::class.java,
        Settings::class.java
    )
    private var mainFragmentsHashMap = HashMap<String, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fragmentManager = supportFragmentManager

        mainFragmentsHashMap = HashMap()
        for (fragmentClass in mainFragments) {
            try {
                val fragment = fragmentClass.newInstance()
                mainFragmentsHashMap[fragmentClass.simpleName] = fragment
            } catch (e: IllegalAccessException) {
                Log.e(
                    "MainActivity", "Error creating fragment: " + fragmentClass.simpleName, e
                )
            } catch (e: InstantiationException) {
                Log.e(
                    "MainActivity", "Error creating fragment: " + fragmentClass.simpleName, e
                )
            }
        }

        AppBarHandler.setUpActionBar(
            this, "KaiFit-Pal", false, false
        )

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        addDefaultFragment(savedInstanceState)

        fragmentsExchangeStack()
    }

    private fun fragmentsExchangeStack() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView?.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item: MenuItem ->
            var selectedFragment: Fragment? = null
            val itemId = item.itemId
            var fragmentTag = ""

            fragmentTag = if (itemId == R.id.user_profile_menu_option) {
                "Profile"
            } else if (itemId == R.id.calculator_menu_option) {
                "Calculator"
            } else if (itemId == R.id.assistant_menu_option) {
                "KaiQ"
            } else if (itemId == R.id.help_info_menu_option) {
                "Help"
            } else if (itemId == R.id.settings_menu_option) {
                "Settings"
            } else {
                Log.e("MainActivity", "Invalid itemId: $itemId")
                return@OnItemSelectedListener false
            }

            selectedFragment = mainFragmentsHashMap[fragmentTag]
            checkNotNull(selectedFragment)
            if (currentFragment is TdeeMacros) {
                AppBarHandler.resetActionBar(this)
            }

            if (selectedFragment.isAdded) {
                fragmentManager?.let { fm ->
                    fm.beginTransaction()
                        .hide(currentFragment!!)
                        .show(selectedFragment)
                        .addToBackStack(fragmentTag)
                        .commit()
                }
            } else {
                fragmentManager?.let { fm ->
                    fm.beginTransaction()
                        .hide(currentFragment!!)
                        .add(
                            R.id.fragment_container_view,
                            selectedFragment!!
                        )
                        .addToBackStack(fragmentTag)
                        .commit()
                }
            }

            currentFragment = selectedFragment
            setAppBar()
            true
        })
    }

    fun setAppBar() {
        if (currentFragment is Profile) {
            AppBarHandler.setTitle(this, "Perfil")
        } else if (currentFragment is Calculator) {
            AppBarHandler.setTitle(this, "KaiFit-Pal")
        } else if (currentFragment is KaiQ) {
            AppBarHandler.setTitle(this, "Kai-Q")
        } else if (currentFragment is Help) {
            AppBarHandler.setTitle(this, "FAQs")
        } else if (currentFragment is Settings) {
            AppBarHandler.setTitle(this, "Ajustes")
        }
    }

    private fun addDefaultFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            defaultFragment = mainFragmentsHashMap["Calculator"]
            currentFragment = defaultFragment

            fragmentManager!!.beginTransaction().add(
                R.id.fragment_container_view, defaultFragment!!, "Calculator"
            ).commit()

            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView?.setSelectedItemId(R.id.calculator_menu_option)
        }
    }

    override fun onBackPressed() {
        if (currentFragment is Calculator) {
            finish()
        } else if (currentFragment is TdeeMacros) {
            returnToCalculator()
        } else {
            returnToPreviousFragmentOrCalculator()
        }
    }

    private fun returnToCalculator() {
        fragmentManager!!.popBackStackImmediate()
        bottomNavigationView!!.selectedItemId = R.id.calculator_menu_option
    }

    private fun returnToPreviousFragmentOrCalculator() {
        val index = fragmentManager!!.backStackEntryCount - 2
        if (index >= 0 && isPreviousFragmentTdeeMacros(index)) {
            returnToTdeeMacros()
        } else {
            returnToDefaultFragment()
        }
    }

    private fun isPreviousFragmentTdeeMacros(index: Int): Boolean {
        val backEntry = fragmentManager!!.getBackStackEntryAt(index)
        val tag = backEntry.name
        return tag != null && tag == "TdeeMacros"
    }

    private fun returnToTdeeMacros() {
        fragmentManager!!.popBackStackImmediate()
        bottomNavigationView!!.menu.findItem(R.id.calculator_menu_option).setChecked(false)
        currentFragment = childFragmentTdee
    }

    private fun returnToDefaultFragment() {
        fragmentManager!!.beginTransaction().hide(currentFragment!!).show(defaultFragment!!)
            .commit()
        currentFragment = defaultFragment
        bottomNavigationView!!.selectedItemId = R.id.calculator_menu_option
    }

    override fun onCalculateClick(tdeeResult: Int) {
        childFragmentTdee = TdeeMacros()
        val bundle = Bundle()
        bundle.putInt("tdeeResult", tdeeResult)
        childFragmentTdee!!.arguments = bundle

        fragmentManager!!.beginTransaction().hide(currentFragment!!).add(
            R.id.fragment_container_view, childFragmentTdee!!
        ).addToBackStack("TdeeMacros").commit()
        currentFragment = childFragmentTdee
    }
}
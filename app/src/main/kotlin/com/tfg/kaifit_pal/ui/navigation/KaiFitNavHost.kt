package com.tfg.kaifit_pal.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.ui.assistant.AssistantScreen
import com.tfg.kaifit_pal.ui.calculator.CalculatorScreen
import com.tfg.kaifit_pal.ui.help.HelpScreen
import com.tfg.kaifit_pal.ui.profile.ProfileScreen
import com.tfg.kaifit_pal.ui.results.ResultsScreen
import com.tfg.kaifit_pal.ui.settings.SettingsScreen

/** Man of 30 years, 180 cm, 80 kg, neck 38 and waist 85 cm, moderately active: 2819 kcal/day. */
private val SampleResults = ResultsRoute(
    sex = Sex.MALE,
    ageYears = 30,
    heightCm = 180.0,
    weightKg = 80.0,
    neckCm = 38.0,
    waistCm = 85.0,
    activityLevel = ActivityLevel.MODERATE,
)

/**
 * App shell: a [NavigationBar] with the five tabs around the navigation graph.
 *
 * The bar is only shown on top-level destinations; detail destinations such as Results use the
 * back arrow of their own top app bar instead. Each screen owns its [Scaffold] and top app bar,
 * so this outer [Scaffold] adds no window insets of its own: it only reserves the bar's height
 * and marks it as consumed for the screen below.
 */
@Composable
fun KaiFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val showNavigationBar = currentDestination == null || currentDestination.isTopLevel()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showNavigationBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.hasRoute(destination.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            // The label already names the tab, so the icon is decorative.
                            icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                            label = {
                                Text(
                                    stringResource(destination.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CalculatorRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<ProfileRoute> { ProfileScreen() }
            composable<CalculatorRoute> {
                // Temporary entry point to the Results destination until the calculator exists.
                CalculatorScreen(onShowResults = dropUnlessResumed { navController.navigate(SampleResults) })
            }
            composable<AssistantRoute> { AssistantScreen() }
            composable<HelpRoute> { HelpScreen() }
            composable<SettingsRoute> { SettingsScreen() }
            composable<ResultsRoute> {
                ResultsScreen(
                    onBack = dropUnlessResumed { navController.popBackStack() },
                    onAskAssistant = dropUnlessResumed {
                        navController.navigateToTopLevel(TopLevelDestination.ASSISTANT)
                    },
                )
            }
        }
    }
}

private fun NavDestination.isTopLevel(): Boolean =
    TopLevelDestination.entries.any { destination -> hierarchy.any { it.hasRoute(destination.route::class) } }

/**
 * Switches tabs keeping a single copy of each one: the back stack is popped to the start
 * destination, and the state of the tab being left is saved and restored when it is reselected.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

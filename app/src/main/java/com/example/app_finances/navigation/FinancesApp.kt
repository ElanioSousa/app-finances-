package com.example.app_finances.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.app_finances.R
import com.example.app_finances.feature.cards.CardsScreen
import com.example.app_finances.feature.dashboard.DashboardScreen
import com.example.app_finances.feature.more.MoreScreen
import com.example.app_finances.feature.transactions.TransactionsScreen
import kotlinx.coroutines.launch

/** Esqueleto do app: barra inferior, botão "+" e as telas de cada aba. */
@Composable
fun FinancesApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = TopLevelDestination.entries.firstOrNull { destination ->
        backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
    } ?: TopLevelDestination.Home
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)

    Scaffold(
        bottomBar = { BottomBar(current) { navController.navigateToTab(it) } },
        floatingActionButton = {
            if (current.showsAddButton) {
                AddButton { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AppNavHost(navController, padding.withFabClearance(current.showsAddButton))
    }
}

/** Espaço extra no fim das listas para o botão "+" não cobrir o último item. */
@Composable
private fun PaddingValues.withFabClearance(hasFab: Boolean): PaddingValues {
    if (!hasFab) return this
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
        bottom = calculateBottomPadding() + FabClearance,
    )
}

private val FabClearance = 80.dp

@Composable
private fun AppNavHost(navController: NavHostController, padding: PaddingValues) {
    NavHost(navController = navController, startDestination = TopLevelDestination.Home.route) {
        composable(TopLevelDestination.Home.route) { DashboardScreen(padding) }
        composable(TopLevelDestination.Transactions.route) { TransactionsScreen(padding) }
        composable(TopLevelDestination.Cards.route) { CardsScreen(padding) }
        composable(TopLevelDestination.More.route) { MoreScreen(padding) }
    }
}

@Composable
private fun BottomBar(current: TopLevelDestination, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        val itemColors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
        )
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = itemColors,
            )
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_new_transaction))
    }
}

/** Troca de aba sem empilhar telas e preservando o estado de cada aba. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

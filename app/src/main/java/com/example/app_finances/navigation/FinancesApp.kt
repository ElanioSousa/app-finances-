package com.example.app_finances.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.app_finances.R
import com.example.app_finances.core.ui.LocalSnackbarHostState
import com.example.app_finances.feature.accounts.AccountsScreen
import com.example.app_finances.feature.cards.CardsScreen
import com.example.app_finances.feature.cards.detail.CardDetailActions
import com.example.app_finances.feature.cards.detail.CardDetailScreen
import com.example.app_finances.feature.dashboard.DashboardActions
import com.example.app_finances.feature.dashboard.DashboardScreen
import com.example.app_finances.feature.categories.CategoriesScreen
import com.example.app_finances.feature.more.MoreScreen
import com.example.app_finances.feature.reports.ReportsScreen
import com.example.app_finances.feature.settings.SettingsScreen
import com.example.app_finances.feature.recurring.RecurringScreen
import com.example.app_finances.feature.transactions.TransactionsScreen
import com.example.app_finances.feature.transactions.form.FormRequest
import com.example.app_finances.feature.transactions.form.TransactionFormSheet

/** Valor do editor de lançamento para "novo" (ids reais começam em 1). */
/** Esqueleto do app: barra inferior, botão "+", telas e o formulário de lançamento. */
@Composable
fun FinancesApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val current = TopLevelDestination.of(backStackEntry?.destination)
    // Na aba Cartões o botão também aparece no detalhe do cartão (compra já nesse cartão).
    val showFab = current.showsAddButton && (route == current.route || route == Routes.CARD_DETAIL)
    val snackbarHostState = remember { SnackbarHostState() }
    var formRequest by rememberSaveable(stateSaver = FormRequest.Saver) { mutableStateOf<FormRequest?>(null) }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            bottomBar = { BottomBar(current) { navController.selectTab(it, current) } },
            floatingActionButton = { if (showFab) AddButton(current) { formRequest = addRequest(current, backStackEntry) } },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            AppNavHost(navController, padding.withFabClearance(showFab)) { formRequest = FormRequest.Edit(it) }
        }
        formRequest?.let { request -> TransactionFormSheet(request, onDismiss = { formRequest = null }) }
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

/** O "+" da aba Cartões lança uma compra no cartão; no detalhe, já nesse cartão. */
private fun addRequest(current: TopLevelDestination, entry: NavBackStackEntry?): FormRequest {
    if (current != TopLevelDestination.Cards) return FormRequest.New
    val cardId = entry?.arguments?.getLong(Routes.CARD_ID)?.takeIf { entry.destination.route == Routes.CARD_DETAIL }
    return FormRequest.CardPurchase(cardId)
}

/** O que as telas de todas as abas precisam para navegar. */
private class AppNavContext(
    val navController: NavHostController,
    val padding: PaddingValues,
    val onOpenTransaction: (Long) -> Unit,
)

@Composable
private fun AppNavHost(navController: NavHostController, padding: PaddingValues, onOpenTransaction: (Long) -> Unit) {
    val nav = AppNavContext(navController, padding, onOpenTransaction)
    // Troca de tela imediata, sem animação.
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Home.graphRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        homeGraph(nav)
        tabGraph(TopLevelDestination.Transactions) {
            composable(it) { TransactionsScreen(padding, onOpenTransaction, onOpenCard = { id -> navController.navigateToCard(id) }) }
        }
        cardsGraph(nav)
        moreGraph(nav)
    }
}

/** Grafo de uma aba: começa na tela da aba e reúne as telas internas dela. */
private fun NavGraphBuilder.tabGraph(tab: TopLevelDestination, screens: NavGraphBuilder.(String) -> Unit) =
    navigation(startDestination = tab.route, route = tab.graphRoute) { screens(tab.route) }

private fun NavGraphBuilder.homeGraph(nav: AppNavContext) = tabGraph(TopLevelDestination.Home) { route ->
    composable(route) {
        DashboardScreen(
            nav.padding,
            DashboardActions(
                onOpenTransaction = nav.onOpenTransaction,
                onOpenCard = { nav.navController.navigateToCard(it) },
                onSeeAll = { nav.navController.navigateToTab(TopLevelDestination.Transactions) },
            ),
        )
    }
}

private fun NavGraphBuilder.cardsGraph(nav: AppNavContext) = tabGraph(TopLevelDestination.Cards) { route ->
    composable(route) { CardsScreen(nav.padding, onOpenCard = { nav.navController.navigate(Routes.cardDetail(it)) }) }
    composable(Routes.CARD_DETAIL, arguments = listOf(navArgument(Routes.CARD_ID) { type = NavType.LongType })) { entry ->
        val cardId = entry.arguments?.getLong(Routes.CARD_ID) ?: return@composable
        CardDetailScreen(cardId, nav.padding, CardDetailActions(nav.navController::popBackStack, nav.onOpenTransaction))
    }
}

private fun NavGraphBuilder.moreGraph(nav: AppNavContext) = tabGraph(TopLevelDestination.More) { route ->
    composable(route) { MoreScreen(nav.padding, onOpen = nav.navController::navigate) }
    composable(Routes.ACCOUNTS) { AccountsScreen(nav.padding, onBack = nav.navController::popBackStack) }
    composable(Routes.RECURRING) { RecurringScreen(nav.padding, onBack = nav.navController::popBackStack) }
    composable(Routes.CATEGORIES) { CategoriesScreen(nav.padding, onBack = nav.navController::popBackStack) }
    composable(Routes.REPORTS) { ReportsScreen(nav.padding, onBack = nav.navController::popBackStack) }
    composable(Routes.SETTINGS) { SettingsScreen(nav.padding, onBack = nav.navController::popBackStack) }
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

/** "+" de novo lançamento; na aba Cartões vira "Nova compra", com texto, para ficar claro. */
@Composable
private fun AddButton(current: TopLevelDestination, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary
    if (current == TopLevelDestination.Cards) {
        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.action_new_card_purchase)) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
        )
    } else {
        FloatingActionButton(onClick = onClick, containerColor = containerColor, contentColor = contentColor) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_new_transaction))
        }
    }
}

/** Abre o detalhe de um cartão dentro da aba Cartões (de onde quer que se esteja). */
private fun NavHostController.navigateToCard(cardId: Long) {
    navigateToTab(TopLevelDestination.Cards)
    navigate(Routes.cardDetail(cardId))
}

/** Tocar na aba atual volta ao início dela; tocar em outra troca de aba. */
private fun NavHostController.selectTab(destination: TopLevelDestination, current: TopLevelDestination) {
    if (destination == current) popBackStack(destination.route, inclusive = false) else navigateToTab(destination)
}

/** Troca de aba sem empilhar telas e preservando a pilha de cada aba. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.graphRoute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

package pl.xsenia.fv.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.R
import pl.xsenia.fv.config.ConfigState
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.ui.invoices.InvoiceListScreen
import pl.xsenia.fv.ui.invoices.InvoiceListViewModel
import pl.xsenia.fv.ui.settings.SettingsScreen

private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    SALES("sales", R.string.nav_sales, Icons.AutoMirrored.Filled.Send),
    PURCHASE("purchase", R.string.nav_purchase, Icons.Filled.ShoppingCart),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

/** Main screen after setup: sales and purchase invoice lists and settings in a bottom navigation. */
@Composable
fun MainScaffold(container: AppContainer, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val sync by viewModel.sync.collectAsStateWithLifecycle()
    val configIssues = (configState as? ConfigState.Loaded)?.config?.issues?.size ?: 0

    // Runs after every unlock (this screen is recreated); the service skips it if synced < 15 min ago.
    LaunchedEffect(Unit) { viewModel.syncIfStale() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val showBadge = tab == Tab.SETTINGS && (configIssues > 0 || configState == ConfigState.FolderInaccessible)
                            BadgedBox(badge = { if (showBadge) Badge { if (configIssues > 0) Text(configIssues.toString()) } }) {
                                Icon(tab.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.SALES.route, modifier = Modifier.padding(padding)) {
            composable(Tab.SALES.route) { InvoiceListRoute(InvoiceCategory.SALES, container, viewModel) }
            composable(Tab.PURCHASE.route) { InvoiceListRoute(InvoiceCategory.PURCHASE, container, viewModel) }
            composable(Tab.SETTINGS.route) { SettingsScreen(viewModel = viewModel, onBack = null) }
        }
    }
}

@Composable
private fun InvoiceListRoute(category: InvoiceCategory, container: AppContainer, mainViewModel: MainViewModel) {
    val listViewModel: InvoiceListViewModel = viewModel(
        key = category.name,
        factory = viewModelFactory { initializer { InvoiceListViewModel(category, container) } },
    )
    val state by listViewModel.state.collectAsStateWithLifecycle()
    val sync by mainViewModel.sync.collectAsStateWithLifecycle()
    InvoiceListScreen(
        category = category,
        state = state,
        sync = sync,
        onPreviousMonth = listViewModel::previousMonth,
        onNextMonth = listViewModel::nextMonth,
        onSelectMonth = listViewModel::selectMonth,
        onQueryChange = listViewModel::setQuery,
        onRefresh = mainViewModel::syncNow,
        // Invoice details arrive in ZAD-010.
        onOpenInvoice = {},
    )
}

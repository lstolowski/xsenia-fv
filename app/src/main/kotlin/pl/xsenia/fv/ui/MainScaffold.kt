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
import pl.xsenia.fv.ui.details.InvoiceDetailsScreen
import pl.xsenia.fv.ui.details.InvoiceDetailsViewModel
import pl.xsenia.fv.ui.invoices.InvoiceListScreen
import pl.xsenia.fv.ui.issue.NewInvoiceScreen
import pl.xsenia.fv.ui.issue.NewInvoiceViewModel
import pl.xsenia.fv.ui.invoices.InvoiceListViewModel
import pl.xsenia.fv.ui.settings.SettingsScreen

private const val DETAILS_ROUTE = "invoice"
private const val NEW_INVOICE_ROUTE = "new-invoice"

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
    LaunchedEffect(Unit) {
        viewModel.syncIfStale()
        // An invoice sent before the app was closed: finish checking its status.
        container.sendService.resumeIfPending()
    }

    Scaffold(
        bottomBar = {
            if (Tab.entries.none { it.route == currentRoute }) return@Scaffold
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
            val openInvoice = { category: InvoiceCategory, ksefNumber: String ->
                navController.navigate("$DETAILS_ROUTE/${category.name}/$ksefNumber")
            }
            composable(Tab.SALES.route) {
                InvoiceListRoute(InvoiceCategory.SALES, container, viewModel, openInvoice, onNewInvoice = { navController.navigate(NEW_INVOICE_ROUTE) })
            }
            composable(Tab.PURCHASE.route) { InvoiceListRoute(InvoiceCategory.PURCHASE, container, viewModel, openInvoice, onNewInvoice = null) }
            composable(NEW_INVOICE_ROUTE) {
                val newInvoiceViewModel: NewInvoiceViewModel = viewModel(factory = viewModelFactory { initializer { NewInvoiceViewModel(container) } })
                NewInvoiceScreen(newInvoiceViewModel, onClose = { navController.popBackStack() })
            }
            composable(Tab.SETTINGS.route) { SettingsScreen(viewModel = viewModel, onBack = null) }
            composable("$DETAILS_ROUTE/{category}/{ksefNumber}") { entry ->
                val category = InvoiceCategory.valueOf(requireNotNull(entry.arguments?.getString("category")))
                val ksefNumber = requireNotNull(entry.arguments?.getString("ksefNumber"))
                val detailsViewModel: InvoiceDetailsViewModel = viewModel(
                    factory = viewModelFactory { initializer { InvoiceDetailsViewModel(category, ksefNumber, container) } },
                )
                val state by detailsViewModel.state.collectAsStateWithLifecycle()
                InvoiceDetailsScreen(state = state, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun InvoiceListRoute(
    category: InvoiceCategory,
    container: AppContainer,
    mainViewModel: MainViewModel,
    onOpenInvoice: (InvoiceCategory, String) -> Unit,
    onNewInvoice: (() -> Unit)?,
) {
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
        onOpenInvoice = { onOpenInvoice(category, it.ksefNumber) },
        onNewInvoice = onNewInvoice,
    )
}

package pl.xsenia.fv.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.R
import pl.xsenia.fv.profile.ProfileSession
import pl.xsenia.fv.invoices.InvoiceRow
import pl.xsenia.fv.mirror.InvoiceCategory
import pl.xsenia.fv.ui.company.BankAccountFormScreen
import pl.xsenia.fv.ui.company.BankAccountFormViewModel
import pl.xsenia.fv.ui.company.CompanyFormScreen
import pl.xsenia.fv.ui.company.CompanyFormViewModel
import pl.xsenia.fv.ui.counterparties.CounterpartyFormScreen
import pl.xsenia.fv.ui.counterparties.CounterpartyFormViewModel
import pl.xsenia.fv.ui.counterparties.CounterpartyListScreen
import pl.xsenia.fv.ui.details.InvoiceDetailsScreen
import pl.xsenia.fv.ui.details.IssueSimilarAction
import pl.xsenia.fv.ui.details.InvoiceDetailsViewModel
import pl.xsenia.fv.ui.invoices.InvoiceListScreen
import pl.xsenia.fv.ui.issue.NewInvoiceScreen
import pl.xsenia.fv.ui.issue.NewInvoiceViewModel
import pl.xsenia.fv.ui.invoices.InvoiceListViewModel
import pl.xsenia.fv.ui.settings.SettingsScreen

private const val DETAILS_ROUTE = "invoice"
private const val NEW_INVOICE_ROUTE = "new-invoice"
private const val COMPANY_ROUTE = "company"
private const val BANK_ACCOUNT_ROUTE = "bank-account"
private const val COUNTERPARTIES_ROUTE = "counterparties"
private const val COUNTERPARTY_ROUTE = "counterparty"

/** Saved-state key: id of a counterparty created from the invoice form, to select it there (ZAD-024). */
private const val CREATED_COUNTERPARTY_KEY = "createdCounterpartyId"

private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    SALES("sales", R.string.nav_sales, Icons.AutoMirrored.Filled.Send),
    PURCHASE("purchase", R.string.nav_purchase, Icons.Filled.ShoppingCart),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

/** Main screen after setup: sales and purchase invoice lists and settings in a bottom navigation, for one company profile. */
@Composable
fun MainScaffold(container: AppContainer, viewModel: MainViewModel, session: ProfileSession) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Runs after every unlock (this screen is recreated); the service skips it if synced < 15 min ago.
    LaunchedEffect(Unit) {
        session.syncService.syncIfStale()
        // An invoice sent before the app was closed: finish checking its status.
        session.sendService.resumeIfPending()
        // UPOs that KSeF had not provided right after accepting an invoice.
        session.sendService.fetchMissingUpos()
    }

    Scaffold(
        bottomBar = {
            if (Tab.entries.none { it.route == currentRoute }) return@Scaffold
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
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
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.SALES.route, modifier = Modifier.padding(padding)) {
            val openInvoice = { category: InvoiceCategory, row: InvoiceRow ->
                // An unsent invoice opens in the form for editing (US-007); others show their details.
                row.unsentId?.let { navController.navigate("$NEW_INVOICE_ROUTE?unsentId=$it") }
                    ?: navController.navigate("$DETAILS_ROUTE/${category.name}/${row.ksefNumber}")
            }
            composable(Tab.SALES.route) {
                InvoiceListRoute(InvoiceCategory.SALES, session, openInvoice, onNewInvoice = { navController.navigate(NEW_INVOICE_ROUTE) })
            }
            composable(Tab.PURCHASE.route) { InvoiceListRoute(InvoiceCategory.PURCHASE, session, openInvoice, onNewInvoice = null) }
            composable(
                "$NEW_INVOICE_ROUTE?unsentId={unsentId}",
                arguments = listOf(navArgument("unsentId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val unsentId = entry.arguments?.getString("unsentId")
                val newInvoiceViewModel: NewInvoiceViewModel = viewModel(
                    key = "invoice-form-${unsentId ?: "new"}",
                    factory = viewModelFactory { initializer { NewInvoiceViewModel(container, session, unsentId) } },
                )
                val createdCounterparty by entry.savedStateHandle.getStateFlow<String?>(CREATED_COUNTERPARTY_KEY, null).collectAsStateWithLifecycle()
                LaunchedEffect(createdCounterparty) {
                    createdCounterparty?.let {
                        newInvoiceViewModel.selectCounterparty(it)
                        entry.savedStateHandle[CREATED_COUNTERPARTY_KEY] = null
                    }
                }
                NewInvoiceScreen(
                    newInvoiceViewModel,
                    onClose = { navController.popIfOn(NEW_INVOICE_ROUTE) },
                    onNewCounterparty = { navController.navigate(COUNTERPARTY_ROUTE) },
                    onOpenCompany = { navController.navigate(COMPANY_ROUTE) },
                )
            }
            composable(Tab.SETTINGS.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    session = session,
                    onEditCompany = { navController.navigate(COMPANY_ROUTE) },
                    onOpenCounterparties = { navController.navigate(COUNTERPARTIES_ROUTE) },
                    onBack = null,
                )
            }
            composable(COMPANY_ROUTE) {
                val companyViewModel: CompanyFormViewModel = viewModel(
                    factory = viewModelFactory { initializer { CompanyFormViewModel(session, container.companyLookup) } },
                )
                CompanyFormScreen(
                    viewModel = companyViewModel,
                    onClose = { navController.popIfOn(COMPANY_ROUTE) },
                    onAddAccount = { navController.navigate(BANK_ACCOUNT_ROUTE) },
                    onEditAccount = { number -> navController.navigate("$BANK_ACCOUNT_ROUTE?number=$number") },
                )
            }
            composable(
                "$BANK_ACCOUNT_ROUTE?number={number}",
                arguments = listOf(navArgument("number") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val number = entry.arguments?.getString("number")
                val accountViewModel: BankAccountFormViewModel = viewModel(
                    key = "bank-account-${number ?: "new"}",
                    factory = viewModelFactory { initializer { BankAccountFormViewModel(session, number) } },
                )
                BankAccountFormScreen(accountViewModel, onClose = { navController.popIfOn(BANK_ACCOUNT_ROUTE) })
            }
            composable(COUNTERPARTIES_ROUTE) {
                CounterpartyListScreen(
                    session = session,
                    onBack = { navController.popIfOn(COUNTERPARTIES_ROUTE) },
                    onAdd = { navController.navigate(COUNTERPARTY_ROUTE) },
                    // A foreign identifier may contain any characters (ADR 0009).
                    onEdit = { id -> navController.navigate("$COUNTERPARTY_ROUTE?id=${Uri.encode(id)}") },
                )
            }
            composable(
                "$COUNTERPARTY_ROUTE?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                val counterpartyViewModel: CounterpartyFormViewModel = viewModel(
                    key = "counterparty-${id ?: "new"}",
                    factory = viewModelFactory { initializer { CounterpartyFormViewModel(session, container.companyLookup, id) } },
                )
                CounterpartyFormScreen(counterpartyViewModel, onClose = { savedId ->
                    // Opened from the invoice form: the new counterparty is selected there.
                    savedId?.let { navController.previousBackStackEntry?.savedStateHandle?.set(CREATED_COUNTERPARTY_KEY, it) }
                    navController.popIfOn(COUNTERPARTY_ROUTE)
                })
            }
            composable("$DETAILS_ROUTE/{category}/{ksefNumber}") { entry ->
                val category = InvoiceCategory.valueOf(requireNotNull(entry.arguments?.getString("category")))
                val ksefNumber = requireNotNull(entry.arguments?.getString("ksefNumber"))
                val detailsViewModel: InvoiceDetailsViewModel = viewModel(
                    factory = viewModelFactory { initializer { InvoiceDetailsViewModel(category, ksefNumber, session) } },
                )
                val state by detailsViewModel.state.collectAsStateWithLifecycle()
                val sendingNewInvoice by detailsViewModel.sendingNewInvoice.collectAsStateWithLifecycle()
                val similarReady by detailsViewModel.similarReady.collectAsStateWithLifecycle()
                val similarFailed by detailsViewModel.similarFailed.collectAsStateWithLifecycle()
                val context = LocalContext.current
                // "Wystaw podobną" (US-015): the copy is already the new-invoice draft — the form opens it as usual.
                LaunchedEffect(similarReady) {
                    if (similarReady) {
                        detailsViewModel.similarOpened()
                        navController.navigate(NEW_INVOICE_ROUTE)
                    }
                }
                LaunchedEffect(similarFailed) {
                    if (similarFailed) {
                        Toast.makeText(context, R.string.details_issue_similar_failed, Toast.LENGTH_LONG).show()
                        detailsViewModel.similarFailureShown()
                    }
                }
                val sales = category == InvoiceCategory.SALES
                InvoiceDetailsScreen(
                    state = state,
                    canShare = sales,
                    issueSimilar = if (sales) IssueSimilarAction(detailsViewModel::issueSimilar, similarBlocked = sendingNewInvoice) else null,
                    onBack = { navController.popIfOn(DETAILS_ROUTE) },
                )
            }
        }
    }
}

@Composable
private fun InvoiceListRoute(
    category: InvoiceCategory,
    session: ProfileSession,
    onOpenInvoice: (InvoiceCategory, InvoiceRow) -> Unit,
    onNewInvoice: (() -> Unit)?,
) {
    val listViewModel: InvoiceListViewModel = viewModel(
        key = category.name,
        factory = viewModelFactory { initializer { InvoiceListViewModel(category, session) } },
    )
    val state by listViewModel.state.collectAsStateWithLifecycle()
    val sync by session.syncService.state.collectAsStateWithLifecycle()
    InvoiceListScreen(
        category = category,
        state = state,
        sync = sync,
        onPreviousMonth = listViewModel::previousMonth,
        onNextMonth = listViewModel::nextMonth,
        onSelectMonth = listViewModel::selectMonth,
        onQueryChange = listViewModel::setQuery,
        onRefresh = session.syncService::syncNow,
        onOpenInvoice = { onOpenInvoice(category, it) },
        onNewInvoice = onNewInvoice,
    )
}

/**
 * Closes the screen only while it is still on top — a second tap (e.g. "Gotowe" during the exit animation)
 * must not pop the invoice list below it, which would leave an empty screen.
 */
private fun NavHostController.popIfOn(routePrefix: String) {
    if (currentDestination?.route?.startsWith(routePrefix) == true) popBackStack()
}

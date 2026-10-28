package pl.xsenia.fv.ui.issue

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.R
import pl.xsenia.fv.ui.components.ShareInvoiceButton
import pl.xsenia.fv.ui.components.SupportDialog
import pl.xsenia.fv.currency.problemMessage
import pl.xsenia.fv.invoice.currency.InvoiceCurrency
import pl.xsenia.fv.invoice.profile.CompanyProfile
import pl.xsenia.fv.invoice.profile.ProfileGap
import pl.xsenia.fv.invoice.issue.DraftIssue
import pl.xsenia.fv.invoice.issue.DraftIssueType
import pl.xsenia.fv.invoice.issue.DraftLine
import pl.xsenia.fv.invoice.issue.InvoiceCalculator
import pl.xsenia.fv.invoice.issue.VatRate
import pl.xsenia.fv.issue.DecimalInput
import pl.xsenia.fv.issue.FormIssue
import pl.xsenia.fv.issue.FormIssueType
import pl.xsenia.fv.issue.LineForm
import pl.xsenia.fv.issue.LineUnits
import pl.xsenia.fv.issue.SendState
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.security.BiometricGate
import pl.xsenia.fv.ui.components.InfoRow
import pl.xsenia.fv.ui.components.BankAccountText
import pl.xsenia.fv.ui.components.CounterpartyText
import pl.xsenia.fv.ui.components.DropdownField
import pl.xsenia.fv.ui.components.LoadingScreen
import pl.xsenia.fv.ui.components.PrimaryButton
import pl.xsenia.fv.ui.components.SecondaryButton
import pl.xsenia.fv.ui.components.SectionCard
import pl.xsenia.fv.ui.components.Tile
import pl.xsenia.fv.ui.components.ksefFailureText
import pl.xsenia.fv.ui.details.InvoiceDetailsContent
import pl.xsenia.fv.ui.format.Formats
import pl.xsenia.fv.ui.theme.ButtonShape
import pl.xsenia.fv.ui.theme.FailureColor
import pl.xsenia.fv.ui.theme.SuccessColor
import pl.xsenia.fv.ui.theme.WarningAmber
import pl.xsenia.fv.ui.theme.WarningText as WarningTextColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(viewModel: NewInvoiceViewModel, onClose: () -> Unit, onNewCounterparty: () -> Unit, onOpenCompany: () -> Unit) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val sending = sendState is SendState.Sending
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    LaunchedEffect(closed) { if (closed) onClose() }
    val deleteFailed by viewModel.deleteFailed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(deleteFailed) {
        if (deleteFailed) {
            Toast.makeText(context, R.string.unsent_delete_failed, Toast.LENGTH_LONG).show()
            viewModel.deleteFailureShown()
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    // After the second issued invoice, leaving the success screen shows the support dialog once (US-016).
    val supportPromptPending by viewModel.supportPromptPending.collectAsStateWithLifecycle()
    var showSupport by remember { mutableStateOf(false) }
    val acceptedShown = step == IssueStep.STATUS && sendState is SendState.Accepted
    // [done] — "Gotowe": the result was seen, so the send state goes back to idle.
    val leave = { done: Boolean ->
        if (acceptedShown && supportPromptPending) {
            showSupport = true
        } else {
            if (done) viewModel.finish()
            onClose()
        }
    }

    BackHandler(enabled = step == IssueStep.PREVIEW) { viewModel.backToForm() }
    BackHandler(enabled = step == IssueStep.STATUS && sending) { /* stay until KSeF answers */ }
    BackHandler(enabled = acceptedShown && supportPromptPending) { leave(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (step) {
                                IssueStep.FORM -> if (state?.unsentId != null) R.string.unsent_edit_title else R.string.new_invoice_title
                                IssueStep.PREVIEW -> R.string.new_invoice_preview_title
                                IssueStep.STATUS -> R.string.new_invoice_status_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    if (!sending) {
                        IconButton(onClick = { if (step == IssueStep.PREVIEW) viewModel.backToForm() else leave(false) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    // PDF of the invoice (US-013): in the preview — not in KSeF yet; after sending — with the KSeF number.
                    val preview by viewModel.preview.collectAsStateWithLifecycle()
                    val shown = preview
                    val accepted = sendState as? SendState.Accepted
                    if (shown != null && step == IssueStep.PREVIEW) ShareInvoiceButton(shown, ksefNumber = null)
                    if (shown != null && step == IssueStep.STATUS && accepted != null) ShareInvoiceButton(shown, accepted.ksefNumber)
                    if (step == IssueStep.FORM && state?.unsentId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.unsent_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = state
        Column(
            // Every step starts at the top.
            Modifier.fillMaxSize().padding(padding).verticalScroll(remember(step) { ScrollState(0) }).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                current == null -> LoadingScreen()
                step == IssueStep.STATUS -> StatusContent(sendState, viewModel, onClose, onDone = { leave(true) })
                step == IssueStep.PREVIEW -> PreviewContent(viewModel)
                current.profile == null -> LoadingScreen()
                !current.profile.canIssueInvoices -> ProfileGapsContent(current.profile.gaps, onOpenCompany, onNewCounterparty)
                else -> FormContent(current, current.profile, viewModel, onNewCounterparty)
            }
        }
    }
    if (showSupport) {
        SupportDialog(onDismiss = {
            showSupport = false
            viewModel.finish()
            onClose()
        })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.unsent_delete_confirm_title)) },
            text = { Text(stringResource(R.string.unsent_delete_confirm_text, state?.form?.invoiceNumber.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteUnsent()
                }) { Text(stringResource(R.string.unsent_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun FormContent(state: NewInvoiceUiState, profile: CompanyProfile, viewModel: NewInvoiceViewModel, onNewCounterparty: () -> Unit) {
    val form = state.form
    val check = state.check
    val formIssues = if (state.showErrors) check?.formIssues.orEmpty() else emptyList()
    val draftIssues = check?.draftIssues.orEmpty().filter { state.showErrors || !it.blocking }

    state.rejection?.let { rejection ->
        SectionCard(stringResource(R.string.unsent_rejection, rejection.statusCode, rejection.description)) {
            rejection.details.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            rejection.originalKsefNumber?.let { Text(stringResource(R.string.unsent_rejection_duplicate, it)) }
        }
    }

    // Counterparty: the chosen one as a tile, the choice in a dialog with one tile each (US-008)
    FormSectionTitle(stringResource(R.string.field_counterparty))
    TileChoiceField(
        options = profile.counterparties,
        key = { it.id },
        selectedKey = form.counterpartyId,
        placeholder = stringResource(R.string.field_counterparty_choose),
        error = formIssues.messageFor(FormIssueType.COUNTERPARTY_MISSING),
        onSelect = viewModel::selectCounterparty,
        addLabel = stringResource(R.string.counterparty_new_from_invoice),
        onAdd = onNewCounterparty,
    ) { CounterpartyText(it) }

    // Number and dates (ADR 0005: the number first)
    FormSectionTitle(stringResource(R.string.form_section_dates))
    val numberError = draftIssues.firstOrNull { it.lineIndex == null && it.type in NUMBER_ISSUES }
    OutlinedTextField(
        value = form.invoiceNumber,
        onValueChange = viewModel::setInvoiceNumber,
        label = { Text(stringResource(R.string.field_invoice_number)) },
        singleLine = true,
        isError = numberError?.blocking == true,
        // Only when there is something to say — an empty supporting text would leave a gap above the dates.
        supportingText = if (numberError == null && form.invoiceNumber == state.suggestedNumber) {
            null
        } else {
            {
                Column {
                    numberError?.let { Text(draftIssueText(it), color = if (it.blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) }
                    if (form.invoiceNumber != state.suggestedNumber) Text(stringResource(R.string.field_number_suggested, state.suggestedNumber))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (form.invoiceNumber != state.suggestedNumber) {
        TextButton(onClick = viewModel::useSuggestedNumber, shape = ButtonShape) { Text(stringResource(R.string.field_number_use_suggested)) }
    }
    DateField(stringResource(R.string.field_issue_date), form.issueDate, draftIssues.messageFor(DraftIssueType.ISSUE_DATE_IN_FUTURE), viewModel::setIssueDate)
    DateField(stringResource(R.string.field_delivery_date), form.deliveryDate, null, viewModel::setDeliveryDate)

    // Lines
    FormSectionTitle(stringResource(R.string.lines_title))
    form.lines.forEachIndexed { index, line ->
        LineCard(
            index = index,
            line = line,
            canRemove = form.lines.size > 1,
            currency = form.currency,
            onCurrencyChange = viewModel::selectCurrency,
            formIssues = formIssues.filter { it.lineIndex == index },
            draftIssues = if (state.showErrors) draftIssues.filter { it.lineIndex == index } else emptyList(),
            onChange = { viewModel.updateLine(index, it) },
            onRemove = { viewModel.removeLine(index) },
        )
    }
    SecondaryButton(onClick = viewModel::addLine, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Text(stringResource(R.string.line_add))
    }
    // A foreign currency (chosen next to the line price) shows the working NBP rate (ADR 0008)
    ExchangeRateInfo(state.rate, onRetry = viewModel::retryRate)

    // Bank account: below the lines, before the payment term (ADR 0005), chosen like the counterparty
    FormSectionTitle(stringResource(R.string.field_bank_account))
    TileChoiceField(
        options = profile.invoiceBankAccounts,
        key = { it.number },
        selectedKey = form.bankAccountNumber,
        placeholder = stringResource(R.string.field_bank_account_choose),
        error = formIssues.messageFor(FormIssueType.BANK_ACCOUNT_MISSING),
        onSelect = viewModel::selectBankAccount,
    ) { BankAccountText(it) }
    profile.invoiceBankAccounts.firstOrNull { it.number == form.bankAccountNumber }?.takeIf { it.currency != form.currency.code }?.let {
        WarningText(stringResource(R.string.bank_account_currency_mismatch, it.currency, form.currency.code))
    }

    // Payment and totals
    Text(
        stringResource(R.string.payment_info, Formats.date(InvoiceCalculator.paymentDueDate(form.issueDate))),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val totals = check?.draft?.let { InvoiceCalculator.totals(it) }
    if (totals != null) {
        val currency = form.currency.code
        Tile {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow(stringResource(R.string.details_total_net), Formats.money(totals.net, currency))
                InfoRow(stringResource(R.string.details_total_vat), Formats.money(totals.vat, currency))
                totals.vatPln?.let { InfoRow(stringResource(R.string.total_vat_pln), Formats.money(it, "PLN")) }
                InfoRow(stringResource(R.string.details_total_gross), Formats.money(totals.gross, currency), emphasized = true)
            }
        }
    }
    // The bank account warning is shown at the account, the date error at the date.
    draftIssues.filter { it.lineIndex == null && it.type !in NUMBER_ISSUES && it.type !in FIELD_ISSUES }.forEach {
        ErrorText(draftIssueText(it))
    }
    state.previewRateProblem?.let { problem ->
        ErrorText(stringResource(R.string.preview_rate_missing))
        problem.problemMessage(LocalResources.current)?.let { ErrorText(it) }
    }
    PrimaryButton(onClick = viewModel::openPreview, modifier = Modifier.fillMaxWidth(), enabled = !state.previewLoading) {
        if (state.previewLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.action_preview))
        }
    }
}

@Composable
private fun LineCard(
    index: Int,
    line: LineForm,
    canRemove: Boolean,
    /** The invoice currency — shown next to the price of every line; a change applies to the whole invoice. */
    currency: InvoiceCurrency,
    onCurrencyChange: (InvoiceCurrency) -> Unit,
    formIssues: List<FormIssue>,
    draftIssues: List<DraftIssue>,
    onChange: (LineForm) -> Unit,
    onRemove: () -> Unit,
) {
    Tile {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.line_title, index + 1), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (canRemove) {
                    IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.line_remove)) }
                }
            }
            val nameError = draftIssues.messageFor(DraftIssueType.LINE_NAME_MISSING, DraftIssueType.LINE_NAME_TOO_LONG)
            OutlinedTextField(
                value = line.name,
                onValueChange = { onChange(line.copy(name = it)) },
                label = { Text(stringResource(R.string.line_name)) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quantityError = formIssues.messageFor(FormIssueType.LINE_QUANTITY_INVALID)
                    ?: draftIssues.messageFor(DraftIssueType.LINE_QUANTITY_NOT_POSITIVE, DraftIssueType.LINE_QUANTITY_TOO_PRECISE, DraftIssueType.LINE_QUANTITY_TOO_LARGE)
                OutlinedTextField(
                    value = line.quantity,
                    onValueChange = { onChange(line.copy(quantity = it)) },
                    label = { Text(stringResource(R.string.line_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                    modifier = Modifier.weight(1f),
                )
                val unitError = draftIssues.messageFor(DraftIssueType.LINE_UNIT_MISSING, DraftIssueType.LINE_UNIT_TOO_LONG)
                // A unit outside the list (draft saved before ADR 0005) stays selectable until changed.
                val units = if (line.unit in LineUnits.ALL) LineUnits.ALL else LineUnits.ALL + line.unit
                DropdownField(
                    label = stringResource(R.string.line_unit),
                    options = units,
                    selected = line.unit,
                    optionText = { it },
                    error = unitError,
                    onSelect = { onChange(line.copy(unit = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
            val priceError = formIssues.messageFor(FormIssueType.LINE_PRICE_INVALID)
                ?: draftIssues.messageFor(
                    DraftIssueType.LINE_PRICE_NEGATIVE,
                    DraftIssueType.LINE_PRICE_TOO_PRECISE,
                    DraftIssueType.LINE_PRICE_TOO_LARGE,
                    DraftIssueType.LINE_AMOUNT_TOO_LARGE,
                )
            // Price 3/5, currency 2/5 — wide enough for the code next to the arrow, also with a larger font (US-011).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = line.unitNetPrice,
                    onValueChange = { onChange(line.copy(unitNetPrice = it)) },
                    label = { Text(stringResource(R.string.line_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    modifier = Modifier.weight(3f),
                )
                DropdownField(
                    label = stringResource(R.string.field_currency_invoice),
                    options = InvoiceCurrency.entries,
                    selected = currency,
                    optionText = { it.code },
                    error = null,
                    onSelect = onCurrencyChange,
                    modifier = Modifier.weight(2f),
                )
            }
            DropdownField(
                label = stringResource(R.string.line_vat_rate),
                options = VatRate.entries,
                selected = line.vatRate,
                optionText = { vatRateText(it) },
                error = null,
                onSelect = { onChange(line.copy(vatRate = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            val quantity = DecimalInput.parse(line.quantity)
            val price = DecimalInput.parse(line.unitNetPrice)
            if (quantity != null && price != null) {
                val amounts = InvoiceCalculator.line(DraftLine(line.name, quantity, line.unit, price, line.vatRate))
                Text(
                    stringResource(R.string.line_amounts, Formats.money(amounts.net, currency.code), Formats.money(amounts.gross, currency.code)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PreviewContent(viewModel: NewInvoiceViewModel) {
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val invoice = preview ?: return
    val activity = LocalContext.current.findFragmentActivity()
    val title = stringResource(R.string.send_confirm_title)
    val subtitle = stringResource(R.string.send_confirm_subtitle, invoice.invoiceNumber, Formats.money(invoice.grossTotal, invoice.currency))
    val testEnvironment = viewModel.environment != KsefEnvironment.PROD

    Text(
        stringResource(if (testEnvironment) R.string.preview_environment_test else R.string.preview_environment_prod),
        color = if (testEnvironment) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.SemiBold,
    )
    val today = LocalDate.now(AppContainer.POLAND)
    if (invoice.issueDate.isBefore(today)) {
        Text(stringResource(R.string.preview_issue_date_past, Formats.date(invoice.issueDate)), color = MaterialTheme.colorScheme.tertiary)
        SecondaryButton(onClick = viewModel::useTodayAsIssueDate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.preview_use_today))
        }
    }
    val previewRate by viewModel.previewRate.collectAsStateWithLifecycle()
    previewRate?.let { rate ->
        val final = rate.final
        val working = rate.working
        val table = stringResource(R.string.preview_rate_table, final.tableNumber, Formats.date(final.effectiveDate))
        if (rate.changed && working != null) {
            Text(
                stringResource(R.string.preview_rate_changed, Formats.exchangeRate(working.rate), Formats.exchangeRate(final.rate), table),
                color = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            Text(stringResource(R.string.preview_rate_final, final.currency.code, Formats.exchangeRate(final.rate), table))
        }
    }
    InvoiceDetailsContent(invoice, ksefNumber = null)
    val swiftMissing by viewModel.previewSwiftMissing.collectAsStateWithLifecycle()
    if (swiftMissing) SwiftMissingWarning()
    PrimaryButton(
        onClick = {
            BiometricGate.authenticate(activity, title, subtitle, onSuccess = viewModel::send, onFailure = {})
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.preview_send)) }
    SecondaryButton(onClick = viewModel::saveWithoutSending, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.preview_save_unsent)) }
    TextButton(onClick = viewModel::backToForm, modifier = Modifier.fillMaxWidth(), shape = ButtonShape) { Text(stringResource(R.string.preview_back)) }
}

@Composable
private fun StatusContent(state: SendState, viewModel: NewInvoiceViewModel, onClose: () -> Unit, onDone: () -> Unit) {
    when (state) {
        SendState.Idle -> LoadingScreen()
        is SendState.Sending -> Column(
            Modifier.fillMaxWidth().padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircularProgressIndicator(Modifier.size(64.dp))
            Text(
                stringResource(if (state.submitted) R.string.status_processing else R.string.status_sending, state.invoiceNumber),
                textAlign = TextAlign.Center,
            )
        }
        is SendState.Accepted -> {
            val preview by viewModel.preview.collectAsStateWithLifecycle()
            StatusMark(success = true)
            SuccessTicket(state, gross = preview?.let { Formats.money(it.grossTotal, it.currency) })
            PrimaryButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_done)) }
        }
        is SendState.Rejected -> {
            val rejection = state.rejection
            StatusMark(success = false)
            Tile(borderColor = MaterialTheme.colorScheme.error) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.status_rejected, state.invoiceNumber, rejection.statusCode, rejection.description),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                    rejection.details.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    rejection.originalKsefNumber?.let { Text(stringResource(R.string.status_duplicate, it)) }
                    Text(stringResource(R.string.status_kept_unsent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(onClick = viewModel::fixInvoice, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_fix)) }
            SecondaryButton(onClick = { viewModel.finish(); onClose() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_close)) }
        }
        is SendState.RateUnavailable -> {
            StatusMark(success = false)
            Tile(borderColor = MaterialTheme.colorScheme.error) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.status_rate_unavailable, state.invoiceNumber),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.problem.problemMessage(LocalResources.current)?.let { Text(it) }
                    Text(
                        stringResource(if (state.unsentId != null) R.string.status_rate_kept_unsent else R.string.status_rate_kept_draft),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrimaryButton(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_retry)) }
            SecondaryButton(onClick = viewModel::fixInvoice, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_fix)) }
            TextButton(onClick = { viewModel.finish(); onClose() }, modifier = Modifier.fillMaxWidth(), shape = ButtonShape) { Text(stringResource(R.string.status_close)) }
        }
        is SendState.Failed -> {
            StatusMark(success = false)
            Tile(borderColor = MaterialTheme.colorScheme.error) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(if (state.pendingInKsef) R.string.status_failed_pending else R.string.status_failed, state.invoiceNumber),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(ksefFailureText(state.failure))
                    if (!state.pendingInKsef) {
                        Text(stringResource(R.string.status_kept_unsent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            PrimaryButton(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_retry)) }
            if (!state.pendingInKsef) {
                SecondaryButton(onClick = viewModel::fixInvoice, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_fix)) }
                TextButton(onClick = { viewModel.finish(); onClose() }, modifier = Modifier.fillMaxWidth(), shape = ButtonShape) { Text(stringResource(R.string.status_close)) }
            }
        }
    }
}

/** The template's success card: a "ticket" with info rows, a perforation and the amount in the accent color. */
@Composable
private fun SuccessTicket(state: SendState.Accepted, gross: String?) {
    // Vertical centre of the perforation, measured in the card; the card outline is cut there (TicketShape).
    var notchY by remember { mutableFloatStateOf(0f) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TicketShape(notchY),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.status_sent_label), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.status_success_icon), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow(stringResource(R.string.field_invoice_number), state.invoiceNumber)
            Text(stringResource(R.string.status_field_ksef), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer { Text(state.ksefNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
            InfoRow(stringResource(R.string.status_field_upo), stringResource(if (state.upoSaved) R.string.status_upo_saved else R.string.status_upo_missing))
        }
        if (gross != null) {
            TicketPerforation(Modifier.onPlaced { notchY = it.positionInParent().y + it.size.height / 2f })
            InfoRow(stringResource(R.string.details_total_gross), gross, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), emphasized = true)
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Dashed line between the half-circle notches cut by [TicketShape], as in the template. */
@Composable
private fun TicketPerforation(modifier: Modifier = Modifier) {
    val dash = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(32.dp)) {
        val y = size.height / 2
        val inset = TICKET_NOTCH_RADIUS.toPx() + 8.dp.toPx()
        drawLine(
            color = dash,
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx())),
        )
    }
}

private val TICKET_NOTCH_RADIUS = 12.dp

/**
 * Card outline with half-circle notches at both edges at [notchY] (px from the top), so the border follows them.
 * Without a measured position (0) it is a plain rounded card.
 */
private class TicketShape(private val notchY: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val corner = with(density) { 8.dp.toPx() }
        val card = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(corner))) }
        if (notchY <= 0f) return Outline.Generic(card)
        val radius = with(density) { TICKET_NOTCH_RADIUS.toPx() }
        val notches = Path().apply {
            addOval(Rect(Offset(0f, notchY), radius))
            addOval(Rect(Offset(size.width, notchY), radius))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, card, notches))
    }
}

@Composable
private fun StatusMark(success: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(140.dp).clip(CircleShape).background(if (success) SuccessColor else FailureColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (success) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = stringResource(if (success) R.string.status_success_icon else R.string.status_failure_icon),
                tint = Color.White,
                modifier = Modifier.size(92.dp),
            )
        }
    }
}

/** What is missing before an invoice can be issued, each with „Uzupełnij” (ZAD-025). */
@Composable
private fun ProfileGapsContent(gaps: List<ProfileGap>, onOpenCompany: () -> Unit, onNewCounterparty: () -> Unit) {
    SectionCard(stringResource(R.string.gaps_title)) {
        gaps.forEach { gap ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(
                        when (gap) {
                            ProfileGap.COMPANY -> R.string.gap_company
                            ProfileGap.BANK_ACCOUNT -> R.string.gap_bank_account
                            ProfileGap.COUNTERPARTY -> R.string.gap_counterparty
                        },
                    ),
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(onClick = if (gap == ProfileGap.COUNTERPARTY) onNewCounterparty else onOpenCompany) {
                    Text(stringResource(R.string.action_fill))
                }
            }
        }
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** Yellow notice before sending an invoice to a foreign buyer from an account without BIC (SWIFT) (US-014). */
@Composable
private fun SwiftMissingWarning() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningAmber.copy(alpha = 0.15f))
            .padding(12.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
        Text(stringResource(R.string.preview_swift_missing), color = WarningTextColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WarningText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
}

/** Working NBP rate of a foreign currency invoice; nothing for PLN. */
@Composable
private fun ExchangeRateInfo(status: RateStatus, onRetry: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    when (status) {
        RateStatus.None -> Unit
        RateStatus.Loading -> Text(stringResource(R.string.rate_loading), style = MaterialTheme.typography.bodySmall, color = secondary)
        is RateStatus.Ready -> Column {
            val rate = status.rate
            Text(
                stringResource(R.string.rate_working, rate.currency.code, Formats.exchangeRate(rate.rate), Formats.date(rate.effectiveDate), rate.tableNumber),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.rate_working_hint), style = MaterialTheme.typography.bodySmall, color = secondary)
        }
        is RateStatus.Unavailable -> Column {
            status.problem.problemMessage(LocalResources.current)?.let { WarningText(it) }
            Text(stringResource(R.string.rate_later), style = MaterialTheme.typography.bodySmall, color = secondary)
            TextButton(onClick = onRetry, shape = ButtonShape) { Text(stringResource(R.string.rate_retry)) }
        }
    }
}

/** Tile with the chosen option (or a prompt to choose one); a click opens a dialog with all options as tiles. */
@Composable
private fun <T> TileChoiceField(
    options: List<T>,
    key: (T) -> String,
    selectedKey: String?,
    placeholder: String,
    error: String?,
    onSelect: (String) -> Unit,
    addLabel: String? = null,
    onAdd: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { key(it) == selectedKey }
    Tile(onClick = { open = true }, borderColor = if (error != null) MaterialTheme.colorScheme.error else null) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (selected != null) {
                    content(selected)
                } else {
                    Text(placeholder, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    error?.let { ErrorText(it) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(placeholder) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options, key = key) { option ->
                        ChoiceTile(selected = key(option) == selectedKey, onClick = {
                            open = false
                            onSelect(key(option))
                        }) { content(option) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }, shape = ButtonShape) { Text(stringResource(R.string.action_cancel)) } },
            // E.g. "+ Nowy kontrahent" — adds an option without leaving the form (ZAD-024).
            dismissButton = if (onAdd != null && addLabel != null) {
                {
                    TextButton(onClick = {
                        open = false
                        onAdd()
                    }, shape = ButtonShape) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(addLabel)
                    }
                }
            } else {
                null
            },
        )
    }
}

/** Option as a tile (US-008); the chosen one has an accent border and a selected radio button. */
@Composable
private fun ChoiceTile(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Tile(onClick = onClick, selected = selected) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { content() }
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        }
    }
}

/** "np" — not taxable in Poland, reverse charge (ADR 0005). */
@Composable
private fun vatRateText(rate: VatRate): String = when (rate) {
    VatRate.NOT_TAXABLE -> stringResource(R.string.vat_rate_not_taxable)
    else -> stringResource(R.string.vat_rate_percent, rate.percent.toPlainString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, error: String?, onSelect: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = Formats.date(date),
        onValueChange = {},
        readOnly = true,
        enabled = false,
        colors = selectableFieldColors(),
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.field_choose_date)) },
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        modifier = Modifier.fillMaxWidth().clickable { open = true },
    )
    if (open) {
        // DatePicker works with UTC midnight milliseconds.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    pickerState.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

/** Fields opened by tapping (dialogs) are disabled for typing but must not look inactive. */
@Composable
private fun selectableFieldColors() = OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val FIELD_ISSUES = setOf(DraftIssueType.ISSUE_DATE_IN_FUTURE, DraftIssueType.BANK_ACCOUNT_CURRENCY_MISMATCH)

private val NUMBER_ISSUES = setOf(
    DraftIssueType.NUMBER_MISSING,
    DraftIssueType.NUMBER_TOO_LONG,
    DraftIssueType.NUMBER_DUPLICATE,
    DraftIssueType.NUMBER_MONTH_MISMATCH,
)

@Composable
private fun List<FormIssue>.messageFor(type: FormIssueType): String? = firstOrNull { it.type == type }?.let {
    stringResource(
        when (it.type) {
            FormIssueType.COUNTERPARTY_MISSING -> R.string.issue_counterparty_missing
            FormIssueType.BANK_ACCOUNT_MISSING -> R.string.issue_bank_account_missing
            FormIssueType.LINE_QUANTITY_INVALID -> R.string.issue_quantity_invalid
            FormIssueType.LINE_PRICE_INVALID -> R.string.issue_price_invalid
        },
    )
}

@Composable
private fun List<DraftIssue>.messageFor(vararg types: DraftIssueType): String? = firstOrNull { it.type in types }?.let { draftIssueText(it) }

@Composable
private fun draftIssueText(issue: DraftIssue): String = stringResource(
    when (issue.type) {
        DraftIssueType.NUMBER_MISSING -> R.string.issue_number_missing
        DraftIssueType.NUMBER_TOO_LONG -> R.string.issue_number_too_long
        DraftIssueType.NUMBER_DUPLICATE -> R.string.issue_number_duplicate
        DraftIssueType.NUMBER_MONTH_MISMATCH -> R.string.issue_number_month_mismatch
        DraftIssueType.ISSUE_DATE_IN_FUTURE -> R.string.issue_issue_date_future
        DraftIssueType.NO_LINES -> R.string.issue_no_lines
        DraftIssueType.LINE_NAME_MISSING -> R.string.issue_line_name_missing
        DraftIssueType.LINE_NAME_TOO_LONG -> R.string.issue_line_name_too_long
        DraftIssueType.LINE_UNIT_MISSING -> R.string.issue_line_unit_missing
        DraftIssueType.LINE_UNIT_TOO_LONG -> R.string.issue_line_unit_too_long
        DraftIssueType.LINE_QUANTITY_NOT_POSITIVE -> R.string.issue_line_quantity_not_positive
        DraftIssueType.LINE_QUANTITY_TOO_PRECISE -> R.string.issue_line_quantity_too_precise
        DraftIssueType.LINE_PRICE_NEGATIVE -> R.string.issue_line_price_negative
        DraftIssueType.LINE_PRICE_TOO_PRECISE -> R.string.issue_line_price_too_precise
        DraftIssueType.TOTAL_NOT_POSITIVE -> R.string.issue_total_not_positive
        DraftIssueType.LINE_QUANTITY_TOO_LARGE -> R.string.issue_line_quantity_too_large
        DraftIssueType.LINE_PRICE_TOO_LARGE -> R.string.issue_line_price_too_large
        DraftIssueType.LINE_AMOUNT_TOO_LARGE -> R.string.issue_line_amount_too_large
        DraftIssueType.AMOUNT_TOO_LARGE -> R.string.issue_amount_too_large
        DraftIssueType.EXCHANGE_RATE_MISSING -> R.string.issue_exchange_rate_missing
        DraftIssueType.BANK_ACCOUNT_CURRENCY_MISMATCH -> R.string.issue_bank_account_currency_mismatch
    },
)

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("NewInvoiceScreen must be hosted in a FragmentActivity")
}

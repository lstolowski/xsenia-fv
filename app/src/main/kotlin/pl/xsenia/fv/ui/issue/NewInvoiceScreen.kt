package pl.xsenia.fv.ui.issue

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.xsenia.fv.AppContainer
import pl.xsenia.fv.R
import pl.xsenia.fv.invoice.config.InvoicingConfig
import pl.xsenia.fv.invoice.issue.DraftIssue
import pl.xsenia.fv.invoice.issue.DraftIssueType
import pl.xsenia.fv.invoice.issue.InvoiceCalculator
import pl.xsenia.fv.issue.DecimalInput
import pl.xsenia.fv.issue.FormIssue
import pl.xsenia.fv.issue.FormIssueType
import pl.xsenia.fv.issue.LineForm
import pl.xsenia.fv.issue.SendState
import pl.xsenia.fv.ksef.KsefEnvironment
import pl.xsenia.fv.security.BiometricGate
import pl.xsenia.fv.ui.components.ChoicePill
import pl.xsenia.fv.ui.components.InfoRow
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(viewModel: NewInvoiceViewModel, onClose: () -> Unit) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val sending = sendState is SendState.Sending
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    LaunchedEffect(closed) { if (closed) onClose() }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = step == IssueStep.PREVIEW) { viewModel.backToForm() }
    BackHandler(enabled = step == IssueStep.STATUS && sending) { /* stay until KSeF answers */ }

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
                        IconButton(onClick = { if (step == IssueStep.PREVIEW) viewModel.backToForm() else onClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
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
                step == IssueStep.STATUS -> StatusContent(sendState, viewModel, onClose)
                step == IssueStep.PREVIEW -> PreviewContent(viewModel)
                current.config?.canIssueInvoices != true ->
                    Text(stringResource(R.string.new_invoice_unavailable), color = MaterialTheme.colorScheme.error)
                else -> FormContent(current, current.config, viewModel)
            }
        }
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
private fun FormContent(state: NewInvoiceUiState, config: InvoicingConfig, viewModel: NewInvoiceViewModel) {
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

    // Counterparty: one tile each (US-008)
    FormSectionTitle(stringResource(R.string.field_counterparty))
    config.counterparties.forEach { counterparty ->
        CounterpartyTile(
            name = counterparty.name,
            nip = counterparty.nip,
            address = listOfNotNull(counterparty.address.line1, counterparty.address.line2).joinToString(", "),
            selected = counterparty.nip == form.counterpartyNip,
            onClick = { viewModel.selectCounterparty(counterparty.nip) },
        )
    }
    formIssues.messageFor(FormIssueType.COUNTERPARTY_MISSING)?.let { ErrorText(it) }

    // Bank account: pills like the template's tip buttons
    FormSectionTitle(stringResource(R.string.field_bank_account))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        config.invoiceBankAccounts.forEach { account ->
            ChoicePill(account.name, selected = account.number == form.bankAccountNumber, onClick = { viewModel.selectBankAccount(account.number) })
        }
    }
    config.invoiceBankAccounts.firstOrNull { it.number == form.bankAccountNumber }?.let { account ->
        Text(
            listOfNotNull(account.bank, Formats.bankAccount(account.number)).joinToString("\n"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    formIssues.messageFor(FormIssueType.BANK_ACCOUNT_MISSING)?.let { ErrorText(it) }

    // Dates and number
    FormSectionTitle(stringResource(R.string.form_section_dates))
    DateField(stringResource(R.string.field_issue_date), form.issueDate, draftIssues.messageFor(DraftIssueType.ISSUE_DATE_IN_FUTURE), viewModel::setIssueDate)
    DateField(stringResource(R.string.field_delivery_date), form.deliveryDate, null, viewModel::setDeliveryDate)
    val numberError = draftIssues.firstOrNull { it.lineIndex == null && it.type in NUMBER_ISSUES }
    OutlinedTextField(
        value = form.invoiceNumber,
        onValueChange = viewModel::setInvoiceNumber,
        label = { Text(stringResource(R.string.field_invoice_number)) },
        singleLine = true,
        isError = numberError?.blocking == true,
        supportingText = {
            Column {
                numberError?.let { Text(draftIssueText(it), color = if (it.blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary) }
                if (form.invoiceNumber != state.suggestedNumber) Text(stringResource(R.string.field_number_suggested, state.suggestedNumber))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (form.invoiceNumber != state.suggestedNumber) {
        TextButton(onClick = viewModel::useSuggestedNumber, shape = ButtonShape) { Text(stringResource(R.string.field_number_use_suggested)) }
    }

    // Lines
    FormSectionTitle(stringResource(R.string.lines_title))
    form.lines.forEachIndexed { index, line ->
        LineCard(
            index = index,
            line = line,
            canRemove = form.lines.size > 1,
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

    // Payment and totals
    Text(
        stringResource(R.string.payment_info, Formats.date(InvoiceCalculator.paymentDueDate(form.issueDate))),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val totals = check?.draft?.let { InvoiceCalculator.totals(it.lines) }
    if (totals != null) {
        Tile {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow(stringResource(R.string.details_total_net), Formats.money(totals.net, "PLN"))
                InfoRow(stringResource(R.string.details_total_vat), Formats.money(totals.vat, "PLN"))
                InfoRow(stringResource(R.string.details_total_gross), Formats.money(totals.gross, "PLN"), emphasized = true)
            }
        }
    }
    draftIssues.filter { it.lineIndex == null && it.type !in NUMBER_ISSUES && it.type != DraftIssueType.ISSUE_DATE_IN_FUTURE }.forEach {
        ErrorText(draftIssueText(it))
    }
    PrimaryButton(onClick = viewModel::openPreview, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_preview)) }
}

@Composable
private fun LineCard(
    index: Int,
    line: LineForm,
    canRemove: Boolean,
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
                    ?: draftIssues.messageFor(DraftIssueType.LINE_QUANTITY_NOT_POSITIVE, DraftIssueType.LINE_QUANTITY_TOO_PRECISE)
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
                OutlinedTextField(
                    value = line.unit,
                    onValueChange = { onChange(line.copy(unit = it)) },
                    label = { Text(stringResource(R.string.line_unit)) },
                    singleLine = true,
                    isError = unitError != null,
                    supportingText = unitError?.let { { Text(it) } },
                    modifier = Modifier.weight(1f),
                )
            }
            val priceError = formIssues.messageFor(FormIssueType.LINE_PRICE_INVALID)
                ?: draftIssues.messageFor(DraftIssueType.LINE_PRICE_NEGATIVE, DraftIssueType.LINE_PRICE_TOO_PRECISE)
            OutlinedTextField(
                value = line.unitNetPrice,
                onValueChange = { onChange(line.copy(unitNetPrice = it)) },
                label = { Text(stringResource(R.string.line_price)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = priceError != null,
                supportingText = priceError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            val quantity = DecimalInput.parse(line.quantity)
            val price = DecimalInput.parse(line.unitNetPrice)
            if (quantity != null && price != null) {
                val amounts = InvoiceCalculator.line(pl.xsenia.fv.invoice.issue.DraftLine(line.name, quantity, line.unit, price))
                Text(
                    stringResource(R.string.line_amounts, Formats.money(amounts.net, "PLN"), Formats.money(amounts.gross, "PLN")),
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
    InvoiceDetailsContent(invoice, ksefNumber = null)
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
private fun StatusContent(state: SendState, viewModel: NewInvoiceViewModel, onClose: () -> Unit) {
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
            PrimaryButton(onClick = { viewModel.finish(); onClose() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.status_done)) }
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
    Tile {
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
            TicketPerforation()
            InfoRow(stringResource(R.string.details_total_gross), gross, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), emphasized = true)
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Dashed line with half-circle notches at both edges, as in the template. */
@Composable
private fun TicketPerforation() {
    val notch = MaterialTheme.colorScheme.background
    val dash = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(32.dp)) {
        val radius = 12.dp.toPx()
        val y = size.height / 2
        drawLine(
            color = dash,
            start = Offset(radius + 8.dp.toPx(), y),
            end = Offset(size.width - radius - 8.dp.toPx(), y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx())),
        )
        drawCircle(notch, radius, Offset(0f, y))
        drawCircle(notch, radius, Offset(size.width, y))
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

@Composable
private fun FormSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** Counterparty as a tile (US-008); the chosen one has an accent border and a selected radio button. */
@Composable
private fun CounterpartyTile(name: String, nip: String, address: String, selected: Boolean, onClick: () -> Unit) {
    Tile(onClick = onClick, selected = selected) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.list_nip, nip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        }
    }
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
    },
)

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("NewInvoiceScreen must be hosted in a FragmentActivity")
}

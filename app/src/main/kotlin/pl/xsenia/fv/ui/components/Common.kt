package pl.xsenia.fv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.xsenia.fv.ui.theme.ButtonShape
import pl.xsenia.fv.ui.theme.CardShape

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/**
 * Tile of the "Payment UI" template: surface card with 8 dp corners and a thin border.
 * [selected] draws an accent border (e.g. the chosen counterparty).
 */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, borderColor ?: MaterialTheme.colorScheme.outlineVariant)
    }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = CardShape, border = border, colors = colors, content = content)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = CardShape, border = border, colors = colors, content = content)
    }
}

/** Tile with a title, used to group content (settings, invoice details). */
@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Tile {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** Screen header of the template: large title and, optionally, an amount in the accent color. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, amount: String? = null, caption: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Text(title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        amount?.let { Text(it, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Row of the template's "Info": gray label on the left, semibold value on the right. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

/** Main action of the template: accent background, 4 dp corners. */
@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = ButtonShape, contentPadding = ButtonPadding, content = content)
}

/** Secondary action: accent outline, 4 dp corners. */
@Composable
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        content = content,
    )
}

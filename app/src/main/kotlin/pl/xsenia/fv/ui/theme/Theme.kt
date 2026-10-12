package pl.xsenia.fv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.xsenia.fv.R

// Palette of the "Payment UI" template (Compose-Ui-Templates, Apache 2.0); its decorative circles are left out (US-008).

/** Accent of the template ("GreenLight"): amounts, main buttons, selection. */
val Accent = Color(0xFF20B7B4)
private val AccentDark = Color(0xFF4FD3D0)
private val Blue = Color(0xFF1095FE)
private val Gray900 = Color(0xFF212121)
private val DarkGray = Color(0xFF364155)

/** Warning color of unsent invoices (yellow triangle), the same in both themes. */
val WarningAmber = Color(0xFFF9A825)
val WarningText = Color(0xFFB26A00)

/** Result colors of sending (US-006): accent check mark / red cross. */
val SuccessColor = Accent
val FailureColor = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F3F2),
    onPrimaryContainer = Color(0xFF00504E),
    secondary = DarkGray,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F3F2),
    onSecondaryContainer = Color(0xFF00504E),
    tertiary = Blue,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Color(0xFFF5F8F7),
    onSurfaceVariant = Color(0xFF7A7F87),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF5F8F7),
    surfaceContainerHighest = Color(0xFFEEF2F1),
    outline = Color(0xFFC4C9CC),
    outlineVariant = Color(0xFFDDE1E3),
    error = FailureColor,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00201F),
    primaryContainer = Color(0xFF00504E),
    onPrimaryContainer = Color(0xFFD5F3F2),
    secondary = Color(0xFFB9C3D6),
    secondaryContainer = Color(0xFF00504E),
    onSecondaryContainer = Color(0xFFD5F3F2),
    tertiary = Color(0xFF8CCBFF),
    background = Color(0xFF111414),
    onBackground = Color(0xFFE6E9E8),
    surface = Color(0xFF111414),
    onSurface = Color(0xFFE6E9E8),
    surfaceVariant = Color(0xFF1C2121),
    onSurfaceVariant = Color(0xFFA3AAAD),
    surfaceContainerLowest = Color(0xFF0C0F0F),
    surfaceContainerLow = Color(0xFF181C1C),
    surfaceContainer = Color(0xFF1C2121),
    surfaceContainerHigh = Color(0xFF232828),
    surfaceContainerHighest = Color(0xFF2B3131),
    outline = Color(0xFF5A6264),
    outlineVariant = Color(0xFF353B3C),
    error = Color(0xFFFF8A80),
)

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

/** Corner radii of the template: 8 dp cards, 4 dp buttons. */
val CardShape = RoundedCornerShape(8.dp)
val ButtonShape = RoundedCornerShape(4.dp)

/** Light theme of the template; the dark one keeps the same accent and shapes. */
@Composable
fun XseniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography().withFamily(Poppins),
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(8.dp), medium = CardShape),
        content = content,
    )
}

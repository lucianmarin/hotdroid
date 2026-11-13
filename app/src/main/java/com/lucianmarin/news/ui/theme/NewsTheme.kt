package com.lucianmarin.news.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lucianmarin.news.R
import com.lucianmarin.news.state.NewsStateController

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "System"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromId(id: String): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

val Route159FontFamily = FontFamily(
    Font(R.font.route159_regular, FontWeight.Normal),
    Font(R.font.route159_bold, FontWeight.Bold)
)

/**
 * Exact Hotnews color palette matching `../news/static/style.sass`,
 * `../macnews/Sources/Macnews/Views/Theme.swift`, and `../subdroid`.
 */
@Immutable
data class NewsPalette(
    val isDark: Boolean,
    val background: Color,
    val oddRowBackground: Color,
    val evenRowBackground: Color,
    val floatingBarBackground: Color,
    val gainsboro: Color,
    val silver: Color,
    val gray: Color = Color(0xFF808080),
    val darkGray: Color,
    val visitedText: Color,
    val primaryText: Color,
    val accent: Color = Color(0xFFFF4500)
)

val LightNewsPalette = NewsPalette(
    isDark = false,
    background = Color(0xFFFFFFFF),
    oddRowBackground = Color(0xFFF0F0F0),
    evenRowBackground = Color(0xFFFFFFFF),
    floatingBarBackground = Color(0xFFFFFFFF),
    gainsboro = Color(0xFFE0E0E0),
    silver = Color(0xFFC0C0C0),
    gray = Color(0xFF808080),
    darkGray = Color(0xFF404040),
    visitedText = Color(0xFF404040),
    primaryText = Color(0xFF000000),
    accent = Color(0xFFFF4500)
)

val DarkNewsPalette = NewsPalette(
    isDark = true,
    background = Color(0xFF000000),
    oddRowBackground = Color(0xFF141414),
    evenRowBackground = Color(0xFF000000),
    floatingBarBackground = Color(0xFF242424),
    gainsboro = Color(0xFF242424),
    silver = Color(0xFF404040),
    gray = Color(0xFF808080),
    darkGray = Color(0xFFC0C0C0),
    visitedText = Color(0xFFC0C0C0),
    primaryText = Color(0xFFFFFFFF),
    accent = Color(0xFFFF4500)
)

val LocalNewsPalette = staticCompositionLocalOf { LightNewsPalette }

object NewsTheme {
    val background: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.background

    val oddRowBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.oddRowBackground

    val evenRowBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.evenRowBackground

    val floatingBarBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.floatingBarBackground

    val gainsboro: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.gainsboro

    val silver: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.silver

    val gray: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.gray

    val darkGray: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.darkGray

    val visitedText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.visitedText

    val primaryText: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalNewsPalette.current.primaryText

    val accent: Color = Color(0xFFFF4500)

    /**
     * Exact 5 warm vertical stripe colors from `../macnews/icon.svg`:
     * `#FBB03B`, `#F7931E`, `#F15A24`, `#ED1C24`, `#C1272D`.
     */
    val iconStripeColors: List<Color> = listOf(
        Color(0xFFFBB03B),
        Color(0xFFF7931E),
        Color(0xFFF15A24),
        Color(0xFFED1C24),
        Color(0xFFC1272D)
    )

    val maxReadingWidth: Dp = 520.dp
    val horizontalPadding: Dp = 20.dp
    val rowVerticalPadding: Dp = 10.dp
    val maxColumnWidth: Dp = maxReadingWidth + (horizontalPadding * 2) // 560.dp

    @Composable
    @ReadOnlyComposable
    fun rowBackground(forZeroBasedIndex: Int): Color =
        if (NewsStateController.isOddRowHighlighted(forZeroBasedIndex)) {
            oddRowBackground
        } else {
            evenRowBackground
        }

    @Composable
    @ReadOnlyComposable
    fun lastRowBackground(entryCount: Int): Color =
        if (NewsStateController.isLastRowOddHighlighted(entryCount)) {
            oddRowBackground
        } else {
            evenRowBackground
        }
}

private val NewsTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Route159FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun HotnewsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (isDark) DarkNewsPalette else LightNewsPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.setBackgroundDrawable(ColorDrawable(palette.background.toArgb()))
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.background,
            surfaceVariant = palette.oddRowBackground,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.gray,
            outline = palette.gainsboro,
            error = palette.accent
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.accent,
            background = palette.background,
            surface = palette.background,
            surfaceVariant = palette.oddRowBackground,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.gray,
            outline = palette.gainsboro,
            error = palette.accent
        )
    }

    CompositionLocalProvider(LocalNewsPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NewsTypography,
            content = content
        )
    }
}

@Composable
fun NewsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = NewsTheme.gainsboro
    )
}

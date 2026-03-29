package io.github.shadcn.ui.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformSpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


internal val DefaultTextStyle =
    TextStyle.Default.copy(
        platformStyle = PlatformTextStyle(
            spanStyle = PlatformSpanStyle.Default,
            paragraphStyle = PlatformParagraphStyle.Default,
        ),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

@Immutable
data class ShadcnTheme(
    val shapes: Shapes = Shapes(),
    val textStyles: TextStyles = TextStyles(),
    val colors: Colors = Colors.light,
) {
    @Immutable
    data class Shapes(
        val small: Shape = RoundedCornerShape(4.dp),
        val medium: Shape = RoundedCornerShape(8.dp),
        val large: Shape = RoundedCornerShape(16.dp),
    )

    @Immutable
    data class TextStyles(
        val displayLarge: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            lineHeight = 80.sp,
            letterSpacing = (-0.02).sp,
        ),
        val displayMedium: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 60.sp,
            lineHeight = 68.sp,
            letterSpacing = (-0.02).sp,
        ),
        val displaySmall: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.02).sp,
        ),
        val headlineLarge: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.01).sp,
        ),
        val headlineMedium: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.01).sp,
        ),
        val headlineSmall: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.01).sp,
        ),
        val titleLarge: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        val titleMedium: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.01.sp,
        ),
        val titleSmall: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.01.sp,
        ),
        val bodyLarge: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        val bodyMedium: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.01.sp,
        ),
        val bodySmall: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.01.sp,
        ),
        val labelLarge: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.01.sp,
        ),
        val labelMedium: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.01.sp,
        ),
        val labelSmall: TextStyle = DefaultTextStyle.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.01.sp,
        ),
    )

    @Immutable
    data class Colors(
        val background: Color,
        val foreground: Color,
        val card: Color,
        val cardForeground: Color,
        val popover: Color,
        val popoverForeground: Color,
        val primary: Color,
        val primaryForeground: Color,
        val secondary: Color,
        val secondaryForeground: Color,
        val muted: Color,
        val mutedForeground: Color,
        val accent: Color,
        val accentForeground: Color,
        val destructive: Color,
        val border: Color,
        val input: Color,
        val ring: Color,
        val chart1: Color,
        val chart2: Color,
        val chart3: Color,
        val chart4: Color,
        val chart5: Color,
        val sidebar: Color,
        val sidebarForeground: Color,
        val sidebarPrimary: Color,
        val sidebarPrimaryForeground: Color,
        val sidebarAccent: Color,
        val sidebarAccentForeground: Color,
        val sidebarBorder: Color,
        val sidebarRing: Color,
    ) {
        companion object {
            @Stable
            val light = Colors(
                background = Color(255, 255, 255),
                foreground = Color(10, 10, 10),
                card = Color(255, 255, 255),
                cardForeground = Color(10, 10, 10),
                popover = Color(255, 255, 255),
                popoverForeground = Color(10, 10, 10),
                primary = Color(23, 23, 23),
                primaryForeground = Color(250, 250, 250),
                secondary = Color(245, 245, 245),
                secondaryForeground = Color(23, 23, 23),
                muted = Color(245, 245, 245),
                mutedForeground = Color(115, 115, 115),
                accent = Color(245, 245, 245),
                accentForeground = Color(23, 23, 23),
                destructive = Color(231, 0, 11),
                border = Color(229, 229, 229),
                input = Color(229, 229, 229),
                ring = Color(161, 161, 161),
                chart1 = Color(245, 74, 0),
                chart2 = Color(0, 150, 137),
                chart3 = Color(16, 78, 100),
                chart4 = Color(255, 186, 0),
                chart5 = Color(253, 154, 0),
                sidebar = Color(250, 250, 250),
                sidebarForeground = Color(10, 10, 10),
                sidebarPrimary = Color(23, 23, 23),
                sidebarPrimaryForeground = Color(250, 250, 250),
                sidebarAccent = Color(245, 245, 245),
                sidebarAccentForeground = Color(23, 23, 23),
                sidebarBorder = Color(229, 229, 229),
                sidebarRing = Color(161, 161, 161),
            )

            @Stable
            val dark = Colors(
                background = Color(10, 10, 10),
                foreground = Color(250, 250, 250),
                card = Color(23, 23, 23),
                cardForeground = Color(250, 250, 250),
                popover = Color(38, 38, 38),
                popoverForeground = Color(250, 250, 250),
                primary = Color(229, 229, 229),
                primaryForeground = Color(23, 23, 23),
                secondary = Color(38, 38, 38),
                secondaryForeground = Color(250, 250, 250),
                muted = Color(38, 38, 38),
                mutedForeground = Color(161, 161, 161),
                accent = Color(64, 64, 64),
                accentForeground = Color(250, 250, 250),
                destructive = Color(255, 100, 103),
                border = Color(255, 255, 255, 25),
                input = Color(255, 255, 255, 38),
                ring = Color(115, 115, 115),
                chart1 = Color(20, 71, 230),
                chart2 = Color(0, 188, 125),
                chart3 = Color(253, 154, 0),
                chart4 = Color(173, 70, 255),
                chart5 = Color(255, 32, 86),
                sidebar = Color(23, 23, 23),
                sidebarForeground = Color(250, 250, 250),
                sidebarPrimary = Color(20, 71, 230),
                sidebarPrimaryForeground = Color(250, 250, 250),
                sidebarAccent = Color(38, 38, 38),
                sidebarAccentForeground = Color(250, 250, 250),
                sidebarBorder = Color(255, 255, 255, 25),
                sidebarRing = Color(82, 82, 82)
            )
        }
    }

    companion object {
        val shapes: Shapes
            @Composable @ReadOnlyComposable
            get() = LocalShadcnShapes.current

        val textStyles: TextStyles
            @Composable @ReadOnlyComposable
            get() = LocalShadcnTextStyles.current

        val colors: Colors
            @Composable @ReadOnlyComposable
            get() = LocalShadcnColors.current

        val darkTheme: ShadcnTheme
            get() = ShadcnTheme(colors = Colors.dark)

        val lightTheme: ShadcnTheme
            get() = ShadcnTheme(colors = Colors.light)
    }
}

val LocalShadcnTheme = staticCompositionLocalOf<ShadcnTheme> {
    error("No ShadcnTheme provided")
}

val LocalShadcnColors = staticCompositionLocalOf<ShadcnTheme.Colors> {
    error("No ShadcnColors provided")
}

val LocalShadcnTextStyles = staticCompositionLocalOf<ShadcnTheme.TextStyles> {
    error("No ShadcnTextStyles provided")
}

val LocalShadcnShapes = staticCompositionLocalOf<ShadcnTheme.Shapes> {
    error("No ShadcnShapes provided")
}

@Composable
fun rememberShadcnTheme(theme: ShadcnTheme = ShadcnTheme()) = remember(theme) { theme }

@Composable
fun ShadcnTheme(
    isDarkTheme: Boolean = false,
    theme: ShadcnTheme = rememberShadcnTheme(if (isDarkTheme) ShadcnTheme.darkTheme else ShadcnTheme.lightTheme),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalShadcnTheme provides theme,
        LocalShadcnColors provides theme.colors,
        LocalShadcnTextStyles provides theme.textStyles,
        LocalShadcnShapes provides theme.shapes,
    ) {
        CompositionLocalProvider(
            values = arrayOf(
                LocalContentColor provides ShadcnTheme.colors.foreground,
                LocalTextStyle provides ShadcnTheme.textStyles.bodyMedium.copy(
                    color = ShadcnTheme.colors.foreground
                ),
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = ShadcnTheme.colors.primary,
                    backgroundColor = ShadcnTheme.colors.primary.copy(alpha = 0.4f)
                )
            ),
        ) {
            content()
        }
    }
}
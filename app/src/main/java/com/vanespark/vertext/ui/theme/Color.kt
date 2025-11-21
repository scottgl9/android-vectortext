package com.vanespark.vertext.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Predefined color themes for VectorText
 * Each theme has light and dark variants following Material 3 guidelines
 */

// ========== Default Purple Theme (Material You) ==========

private val DefaultPurple = Color(0xFF6750A4)
private val DefaultPurpleLight = Color(0xFFEADDFF)
private val DefaultPurpleDark = Color(0xFFD0BCFF)

val DefaultLightColorScheme = lightColorScheme(
    primary = DefaultPurple,
    onPrimary = Color.White,
    primaryContainer = DefaultPurpleLight,
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

val DefaultDarkColorScheme = darkColorScheme(
    primary = DefaultPurpleDark,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = DefaultPurpleLight,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

// ========== Ocean Blue Theme ==========

private val OceanBlue = Color(0xFF0077BE)
private val OceanBlueLight = Color(0xFFB3E5FC)
private val OceanBlueDark = Color(0xFF64B5F6)

val OceanBlueLightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanBlueLight,
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF4F6473),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E4F4),
    onSecondaryContainer = Color(0xFF0B1D2A),
    tertiary = Color(0xFF64597D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF1F1537),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F)
)

val OceanBlueDarkColorScheme = darkColorScheme(
    primary = OceanBlueDark,
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004B73),
    onPrimaryContainer = OceanBlueLight,
    secondary = Color(0xFFB6C8D8),
    onSecondary = Color(0xFF21323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD2E4F4),
    tertiary = Color(0xFFCEC0E8),
    onTertiary = Color(0xFF352A4D),
    tertiaryContainer = Color(0xFF4C4165),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199)
)

// ========== Forest Green Theme ==========

private val ForestGreen = Color(0xFF2E7D32)
private val ForestGreenLight = Color(0xFFC8E6C9)
private val ForestGreenDark = Color(0xFF81C784)

val ForestGreenLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreenLight,
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F0F),
    tertiary = Color(0xFF38656A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF0),
    onTertiaryContainer = Color(0xFF002023),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF72796F)
)

val ForestGreenDarkColorScheme = darkColorScheme(
    primary = ForestGreenDark,
    onPrimary = Color(0xFF00390D),
    primaryContainer = Color(0xFF00531A),
    onPrimaryContainer = ForestGreenLight,
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B38),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFA0CFD4),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D52),
    onTertiaryContainer = Color(0xFFBCEBF0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    outline = Color(0xFF8C9388)
)

// ========== Sunset Orange Theme ==========

private val SunsetOrange = Color(0xFFE65100)
private val SunsetOrangeLight = Color(0xFFFFE0B2)
private val SunsetOrangeDark = Color(0xFFFFB74D)

val SunsetOrangeLightColorScheme = lightColorScheme(
    primary = SunsetOrange,
    onPrimary = Color.White,
    primaryContainer = SunsetOrangeLight,
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Color(0xFF735A42),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDBE),
    onSecondaryContainer = Color(0xFF291806),
    tertiary = Color(0xFF5C6546),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0EBC4),
    onTertiaryContainer = Color(0xFF1A2009),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201B16),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201B16),
    surfaceVariant = Color(0xFFF0DFD1),
    onSurfaceVariant = Color(0xFF51443A),
    outline = Color(0xFF837468)
)

val SunsetOrangeDarkColorScheme = darkColorScheme(
    primary = SunsetOrangeDark,
    onPrimary = Color(0xFF4B2800),
    primaryContainer = Color(0xFF6A3B00),
    onPrimaryContainer = SunsetOrangeLight,
    secondary = Color(0xFFE2C1A4),
    onSecondary = Color(0xFF412D19),
    secondaryContainer = Color(0xFF59432D),
    onSecondaryContainer = Color(0xFFFFDDBE),
    tertiary = Color(0xFFC4CFAA),
    onTertiary = Color(0xFF2E351C),
    tertiaryContainer = Color(0xFF444C31),
    onTertiaryContainer = Color(0xFFE0EBC4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201B16),
    onBackground = Color(0xFFEBE0DA),
    surface = Color(0xFF201B16),
    onSurface = Color(0xFFEBE0DA),
    surfaceVariant = Color(0xFF51443A),
    onSurfaceVariant = Color(0xFFD5C3B5),
    outline = Color(0xFF9D8E81)
)

// ========== Rose Pink Theme ==========

private val RosePink = Color(0xFFD81B60)
private val RosePinkLight = Color(0xFFF8BBD0)
private val RosePinkDark = Color(0xFFF06292)

val RosePinkLightColorScheme = lightColorScheme(
    primary = RosePink,
    onPrimary = Color.White,
    primaryContainer = RosePinkLight,
    onPrimaryContainer = Color(0xFF3E001F),
    secondary = Color(0xFF74565F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF837377)
)

val RosePinkDarkColorScheme = darkColorScheme(
    primary = RosePinkDark,
    onPrimary = Color(0xFF650033),
    primaryContainer = Color(0xFF8E0049),
    onPrimaryContainer = RosePinkLight,
    secondary = Color(0xFFE2BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF48290C),
    tertiaryContainer = Color(0xFF613F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201A1B),
    onBackground = Color(0xFFECDFE0),
    surface = Color(0xFF201A1B),
    onSurface = Color(0xFFECDFE0),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    outline = Color(0xFF9E8C90)
)

// ========== Midnight Blue Theme ==========

private val MidnightBlue = Color(0xFF1A237E)
private val MidnightBlueLight = Color(0xFFC5CAE9)
private val MidnightBlueDark = Color(0xFF7986CB)

val MidnightBlueLightColorScheme = lightColorScheme(
    primary = MidnightBlue,
    onPrimary = Color.White,
    primaryContainer = MidnightBlueLight,
    onPrimaryContainer = Color(0xFF00006E),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF18192C),
    tertiary = Color(0xFF765668),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8EC),
    onTertiaryContainer = Color(0xFF2D1323),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680)
)

val MidnightBlueDarkColorScheme = darkColorScheme(
    primary = MidnightBlueDark,
    onPrimary = Color(0xFF0000A3),
    primaryContainer = Color(0xFF0000C1),
    onPrimaryContainer = MidnightBlueLight,
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2E42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE6BAD0),
    onTertiary = Color(0xFF442739),
    tertiaryContainer = Color(0xFF5D3E4F),
    onTertiaryContainer = Color(0xFFFFD8EC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF918F9A)
)

// ========== Crimson Red Theme ==========

private val CrimsonRed = Color(0xFFC62828)
private val CrimsonRedLight = Color(0xFFFFCDD2)
private val CrimsonRedDark = Color(0xFFE57373)

val CrimsonRedLightColorScheme = lightColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonRedLight,
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775653),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF251A00),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF4DDDC),
    onSurfaceVariant = Color(0xFF534342),
    outline = Color(0xFF857371)
)

val CrimsonRedDarkColorScheme = darkColorScheme(
    primary = CrimsonRedDark,
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = CrimsonRedLight,
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFDFC38C),
    onTertiary = Color(0xFF3E2E04),
    tertiaryContainer = Color(0xFF564419),
    onTertiaryContainer = Color(0xFFFCDFA6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF201A1A),
    onBackground = Color(0xFFECE0DF),
    surface = Color(0xFF201A1A),
    onSurface = Color(0xFFECE0DF),
    surfaceVariant = Color(0xFF534342),
    onSurfaceVariant = Color(0xFFD7C1BF),
    outline = Color(0xFFA08C8A)
)

/**
 * Enum representing all available color themes
 */
enum class ColorTheme(
    val displayName: String,
    val lightScheme: androidx.compose.material3.ColorScheme,
    val darkScheme: androidx.compose.material3.ColorScheme
) {
    DEFAULT("Purple (Default)", DefaultLightColorScheme, DefaultDarkColorScheme),
    OCEAN_BLUE("Ocean Blue", OceanBlueLightColorScheme, OceanBlueDarkColorScheme),
    FOREST_GREEN("Forest Green", ForestGreenLightColorScheme, ForestGreenDarkColorScheme),
    SUNSET_ORANGE("Sunset Orange", SunsetOrangeLightColorScheme, SunsetOrangeDarkColorScheme),
    ROSE_PINK("Rose Pink", RosePinkLightColorScheme, RosePinkDarkColorScheme),
    MIDNIGHT_BLUE("Midnight Blue", MidnightBlueLightColorScheme, MidnightBlueDarkColorScheme),
    CRIMSON_RED("Crimson Red", CrimsonRedLightColorScheme, CrimsonRedDarkColorScheme);

    companion object {
        fun fromName(name: String): ColorTheme {
            return entries.find { it.name == name } ?: DEFAULT
        }
    }
}

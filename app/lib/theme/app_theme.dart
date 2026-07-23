import 'package:flutter/material.dart';

import 'tokens.dart';

ThemeData buildDarkTheme() {
  const darkBg = Color(0xFF0B0F19);
  const darkCard = Color(0xFF161F33);
  const darkBorder = Color(0x1FFFFFFF);

  final colorScheme = ColorScheme.fromSeed(
    seedColor: ClTokens.colorBrandPrimary,
    brightness: Brightness.dark,
    primary: ClTokens.colorBrandPrimary,
    surface: darkCard,
    error: ClTokens.colorSemanticDanger,
  );

  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: darkBg,
    fontFamily: ClTokens.fontFamilyFlutter,
    appBarTheme: const AppBarTheme(
      backgroundColor: Color(0xFF0D1322),
      foregroundColor: Colors.white,
      elevation: 0,
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: darkCard,
      shape: RoundedRectangleBorder(
        side: const BorderSide(color: darkBorder),
        borderRadius: BorderRadius.circular(ClTokens.radiusLg),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: const Color(0x0FFFFFFF),
      labelStyle: const TextStyle(color: Colors.white70),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(ClTokens.radiusFull),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: const Color(0xFF0D1322),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: darkBorder),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: darkBorder),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: ClTokens.colorBrandPrimary),
      ),
    ),
  );
}

ThemeData buildLightTheme() {
  const lightBg = Color(0xFFF1F5F9);
  const lightCard = Colors.white;
  const lightBorder = Color(0x1F0F172A);

  final colorScheme = ColorScheme.fromSeed(
    seedColor: ClTokens.colorBrandPrimary,
    brightness: Brightness.light,
    primary: ClTokens.colorBrandPrimary,
    surface: lightCard,
    error: ClTokens.colorSemanticDanger,
  );

  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: lightBg,
    fontFamily: ClTokens.fontFamilyFlutter,
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.white,
      foregroundColor: Color(0xFF0F172A),
      elevation: 0,
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: lightCard,
      shape: RoundedRectangleBorder(
        side: const BorderSide(color: lightBorder),
        borderRadius: BorderRadius.circular(ClTokens.radiusLg),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: const Color(0x0F0F172A),
      labelStyle: const TextStyle(color: Color(0xFF475569)),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(ClTokens.radiusFull),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: lightBorder),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: lightBorder),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: ClTokens.colorBrandPrimary),
      ),
    ),
  );
}

ThemeData buildAppTheme() => buildDarkTheme();

/// Funnel status → color, straight from tokens (same hues as the web).
Color statusColor(String status) => switch (status) {
      'LEAD' => ClTokens.colorStatusLead,
      'HOT_LEAD' => ClTokens.colorStatusHotLead,
      'APPLICATION' => ClTokens.colorStatusApplication,
      'STUDENT' => ClTokens.colorStatusStudent,
      'STALLED' => ClTokens.colorStatusStalled,
      _ => ClTokens.colorNeutral400,
    };

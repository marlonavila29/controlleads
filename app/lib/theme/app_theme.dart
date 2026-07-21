import 'package:flutter/material.dart';

import 'tokens.dart';

/// ThemeData assembled from the generated design tokens (ClTokens),
/// keeping the app visually consistent with the Angular web client.
ThemeData buildAppTheme() {
  final colorScheme = ColorScheme.fromSeed(
    seedColor: ClTokens.colorBrandPrimary,
    primary: ClTokens.colorBrandPrimary,
    error: ClTokens.colorSemanticDanger,
    surface: ClTokens.colorNeutral0,
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: colorScheme,
    scaffoldBackgroundColor: ClTokens.colorNeutral50,
    fontFamily: ClTokens.fontFamilyFlutter,
    cardTheme: CardThemeData(
      elevation: 0,
      color: ClTokens.colorNeutral0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(ClTokens.radiusLg),
      ),
    ),
    chipTheme: ChipThemeData(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(ClTokens.radiusFull),
      ),
    ),
  );
}

/// Funnel status → color, straight from tokens (same hues as the web).
Color statusColor(String status) => switch (status) {
      'LEAD' => ClTokens.colorStatusLead,
      'HOT_LEAD' => ClTokens.colorStatusHotLead,
      'APPLICATION' => ClTokens.colorStatusApplication,
      'STUDENT' => ClTokens.colorStatusStudent,
      'STALLED' => ClTokens.colorStatusStalled,
      _ => ClTokens.colorNeutral400,
    };

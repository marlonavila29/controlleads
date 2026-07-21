import 'package:flutter/foundation.dart';

/// Dev API endpoint. The Android emulator reaches the host via 10.0.2.2.
/// Replaced by environment-based config when the generated client lands.
abstract final class ApiConfig {
  static final String baseUrl = defaultTargetPlatform == TargetPlatform.android
      ? 'http://10.0.2.2:8090'
      : 'http://localhost:8090';
}

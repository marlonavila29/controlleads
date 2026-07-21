import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';

import '../core/api_config.dart';

class AuthUser {
  const AuthUser({required this.id, required this.name, required this.email, required this.role});

  final String id;
  final String name;
  final String email;
  final String role;

  bool get isAdmin => role == 'ADMINISTRATOR';

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        id: json['id'] as String,
        name: json['name'] as String,
        email: json['email'] as String,
        role: json['role'] as String,
      );
}

/// Session state + login/logout against /api/auth.
/// Tokens live in memory for now; secure storage (flutter_secure_storage)
/// arrives with the generated Dart client (see CLAUDE.md next steps).
class AuthService extends ChangeNotifier {
  AuthService._();

  static final AuthService instance = AuthService._();

  String? _accessToken;
  String? _refreshToken;
  AuthUser? _user;

  AuthUser? get user => _user;
  String? get accessToken => _accessToken;
  bool get isAuthenticated => _accessToken != null;

  Future<void> login(String email, String password) async {
    final client = HttpClient();
    try {
      final request = await client.postUrl(Uri.parse('${ApiConfig.baseUrl}/api/auth/login'));
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode({'email': email, 'password': password}));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        throw AuthException(response.statusCode == 401
            ? 'Invalid email or password.'
            : 'Something went wrong. Try again.');
      }
      final json = jsonDecode(body) as Map<String, dynamic>;
      _accessToken = json['accessToken'] as String;
      _refreshToken = json['refreshToken'] as String;
      _user = AuthUser.fromJson(json['user'] as Map<String, dynamic>);
      notifyListeners();
    } on AuthException {
      rethrow;
    } catch (_) {
      throw AuthException('Cannot reach the server. Is the backend running?');
    } finally {
      client.close();
    }
  }

  Future<void>? _refreshing;

  /// Single-flight refresh: concurrent 401s share one rotation call (the
  /// rotating refresh token is one-shot, so parallel calls would fail).
  Future<void> refresh() {
    return _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  }

  Future<void> _doRefresh() async {
    final token = _refreshToken;
    if (token == null) return;
    final client = HttpClient();
    try {
      final request = await client.postUrl(Uri.parse('${ApiConfig.baseUrl}/api/auth/refresh'));
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode({'refreshToken': token}));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      if (response.statusCode != 200) {
        logout();
        return;
      }
      final json = jsonDecode(body) as Map<String, dynamic>;
      _accessToken = json['accessToken'] as String;
      _refreshToken = json['refreshToken'] as String;
      notifyListeners();
    } catch (_) {
      // Network hiccup: keep current session; next 401 will log out.
    } finally {
      client.close();
    }
  }

  void logout() {
    _accessToken = null;
    _refreshToken = null;
    _user = null;
    notifyListeners();
  }
}

class AuthException implements Exception {
  const AuthException(this.message);

  final String message;

  @override
  String toString() => message;
}

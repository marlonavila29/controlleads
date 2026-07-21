import 'dart:convert';
import 'dart:io';

import '../auth/auth_service.dart';
import 'api_config.dart';

/// Minimal authenticated JSON client over dart:io.
/// Swapped for the generated dio client when the contract pipeline lands.
class ApiClient {
  const ApiClient._();

  static const ApiClient instance = ApiClient._();

  Future<dynamic> get(String path, [Map<String, String>? query]) =>
      _send('GET', path, query: query);

  Future<dynamic> post(String path, Map<String, dynamic> body) =>
      _send('POST', path, body: body);

  Future<dynamic> patch(String path, Map<String, dynamic> body) =>
      _send('PATCH', path, body: body);

  Future<dynamic> _send(String method, String path,
      {Map<String, String>? query, Map<String, dynamic>? body}) async {
    final client = HttpClient();
    try {
      var uri = Uri.parse('${ApiConfig.baseUrl}$path');
      if (query != null && query.isNotEmpty) {
        uri = uri.replace(queryParameters: {...uri.queryParameters, ...query});
      }
      final request = await client.openUrl(method, uri);
      final token = AuthService.instance.accessToken;
      if (token != null) {
        request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      }
      if (body != null) {
        request.headers.contentType = ContentType.json;
        request.write(jsonEncode(body));
      }
      final response = await request.close();
      final text = await response.transform(utf8.decoder).join();

      if (response.statusCode == 401) {
        AuthService.instance.logout();
        throw const ApiError(401, 'Session expired — sign in again.');
      }
      if (response.statusCode >= 400) {
        String message = 'Request failed';
        try {
          message = (jsonDecode(text) as Map<String, dynamic>)['title'] as String? ?? message;
        } catch (_) {}
        throw ApiError(response.statusCode, message);
      }
      return text.isEmpty ? null : jsonDecode(text);
    } finally {
      client.close();
    }
  }
}

class ApiError implements Exception {
  const ApiError(this.status, this.message);

  final int status;
  final String message;

  @override
  String toString() => message;
}

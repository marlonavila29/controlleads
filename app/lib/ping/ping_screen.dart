import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../theme/tokens.dart';

/// Fase 0 dummy screen — proves the app reaches the same API as the web.
/// The hand-rolled HttpClient call is replaced by the generated Dart client
/// (shared/api-contract) once the contract pipeline is wired (see CLAUDE.md).
class PingScreen extends StatefulWidget {
  const PingScreen({super.key});

  @override
  State<PingScreen> createState() => _PingScreenState();
}

class _PingScreenState extends State<PingScreen> {
  String? _status;
  bool _error = false;

  // Android emulator reaches host machine via 10.0.2.2.
  static final String _baseUrl = defaultTargetPlatform == TargetPlatform.android
      ? 'http://10.0.2.2:8090'
      : 'http://localhost:8090';

  @override
  void initState() {
    super.initState();
    _ping();
  }

  Future<void> _ping() async {
    setState(() {
      _status = null;
      _error = false;
    });
    try {
      final client = HttpClient();
      final request = await client.getUrl(Uri.parse('$_baseUrl/api/ping'));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      final json = jsonDecode(body) as Map<String, dynamic>;
      setState(() => _status = json['status'] as String?);
    } catch (_) {
      setState(() => _error = true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(ClTokens.space5),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'ControlLeads',
                style: TextStyle(
                  fontSize: ClTokens.fontSizeDisplay,
                  fontWeight: FontWeight.bold,
                  color: ClTokens.colorBrandPrimary,
                ),
              ),
              const SizedBox(height: ClTokens.space2),
              Text(
                'Lead management for international student recruitment',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: ClTokens.fontSizeSm,
                  color: ClTokens.colorNeutral600,
                ),
              ),
              const SizedBox(height: ClTokens.space5),
              if (_status != null)
                Chip(
                  label: Text('API $_status'),
                  backgroundColor: ClTokens.colorBrandPrimarySoft,
                  labelStyle: TextStyle(color: ClTokens.colorBrandPrimary),
                )
              else if (_error)
                Chip(
                  label: const Text('API unreachable'),
                  backgroundColor: const Color(0xFFFEF2F2),
                  labelStyle: TextStyle(color: ClTokens.colorSemanticDanger),
                )
              else
                const CircularProgressIndicator(),
              const SizedBox(height: ClTokens.space4),
              TextButton(onPressed: _ping, child: const Text('Retry')),
            ],
          ),
        ),
      ),
    );
  }
}

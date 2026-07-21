import 'package:flutter/material.dart';

import '../auth/auth_service.dart';
import '../theme/tokens.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final user = AuthService.instance.user;
    return Scaffold(
      appBar: AppBar(
        title: const Text('ControlLeads'),
        actions: [
          IconButton(
            tooltip: 'Sign out',
            icon: const Icon(Icons.logout),
            onPressed: AuthService.instance.logout,
          ),
        ],
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(ClTokens.space5),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Welcome, ${user?.name ?? ''}',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: ClTokens.fontSizeXl,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: ClTokens.space2),
              Text(
                'Your pipeline will live here (Fase 1b–1d).',
                style: TextStyle(
                  fontSize: ClTokens.fontSizeSm,
                  color: ClTokens.colorNeutral600,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

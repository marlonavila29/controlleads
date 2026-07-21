import 'package:flutter/material.dart';

import 'auth/auth_service.dart';
import 'auth/login_screen.dart';
import 'leads/leads_screen.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const ControlLeadsApp());
}

class ControlLeadsApp extends StatelessWidget {
  const ControlLeadsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ControlLeads',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: ListenableBuilder(
        listenable: AuthService.instance,
        builder: (context, _) => AuthService.instance.isAuthenticated
            ? const LeadsScreen()
            : const LoginScreen(),
      ),
    );
  }
}

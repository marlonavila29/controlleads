import 'package:flutter/material.dart';

import 'auth/auth_service.dart';
import 'auth/login_screen.dart';
import 'home/main_shell_screen.dart';
import 'theme/app_theme.dart';
import 'theme/theme_service.dart';

void main() {
  runApp(const ControlLeadsApp());
}

class ControlLeadsApp extends StatelessWidget {
  const ControlLeadsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: ThemeService.instance.themeMode,
      builder: (context, mode, _) {
        return MaterialApp(
          title: 'ControlLeads',
          debugShowCheckedModeBanner: false,
          theme: buildLightTheme(),
          darkTheme: buildDarkTheme(),
          themeMode: mode,
          home: ListenableBuilder(
            listenable: AuthService.instance,
            builder: (context, _) => AuthService.instance.isAuthenticated
                ? const MainShellScreen()
                : const LoginScreen(),
          ),
        );
      },
    );
  }
}

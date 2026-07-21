import 'package:flutter/material.dart';

import 'ping/ping_screen.dart';
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
      home: const PingScreen(),
    );
  }
}

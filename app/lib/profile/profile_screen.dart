import 'package:flutter/material.dart';

import '../auth/auth_service.dart';
import '../theme/theme_service.dart';
import '../theme/tokens.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key, this.onTriggerTour});

  final VoidCallback? onTriggerTour;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  @override
  Widget build(BuildContext context) {
    final user = AuthService.instance.user;
    final isDark = ThemeService.instance.isDark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile & Settings'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(ClTokens.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // User Information Header Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(ClTokens.space4),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 28,
                      backgroundColor: ClTokens.colorBrandPrimary,
                      child: Text(
                        (user?.name ?? 'U').substring(0, 1).toUpperCase(),
                        style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    const SizedBox(width: ClTokens.space4),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            user?.name ?? 'User Profile',
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            user?.email ?? 'user@controlleads.local',
                            style: TextStyle(
                              fontSize: 13,
                              color: Theme.of(context).textTheme.bodyMedium?.color?.withAlpha(180),
                            ),
                          ),
                          const SizedBox(height: 6),
                          Chip(
                            padding: EdgeInsets.zero,
                            labelPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: -4),
                            label: Text(
                              user?.isAdmin == true ? '🛡️ Administrator' : '👤 Team Member',
                              style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                            ),
                            backgroundColor: ClTokens.colorBrandPrimary.withAlpha(30),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: ClTokens.space4),

            // App Appearance Settings
            const Text(
              'Appearance & Theme',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: ClTokens.space2),
            Card(
              child: SwitchListTile(
                secondary: Icon(
                  isDark ? Icons.dark_mode : Icons.light_mode,
                  color: ClTokens.colorBrandPrimary,
                ),
                title: const Text('Dark Mode'),
                subtitle: Text(isDark ? 'Obsidian dark theme active' : 'Clean light theme active'),
                value: isDark,
                onChanged: (_) {
                  setState(() {
                    ThemeService.instance.toggleTheme();
                  });
                },
              ),
            ),
            const SizedBox(height: ClTokens.space4),

            // System Information
            const Text(
              'System & Account',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: ClTokens.space2),
            Card(
              child: Column(
                children: [
                  const ListTile(
                    leading: Icon(Icons.verified_user_outlined),
                    title: Text('Security & Roles'),
                    subtitle: Text('JWT Authenticated · Access Token Active'),
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.auto_awesome, color: Colors.amber),
                    title: const Text('🎓 Interactive System Tour'),
                    subtitle: const Text('Highlights features and UI locations in real time'),
                    onTap: () {
                      if (widget.onTriggerTour != null) {
                        widget.onTriggerTour!();
                      }
                    },
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.info_outline),
                    title: const Text('ControlLeads Version'),
                    subtitle: const Text('v1.0.0 (Build 2026.07)'),
                  ),
                ],
              ),
            ),
            const SizedBox(height: ClTokens.space5),

            // Prominent Sign Out Button
            FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: ClTokens.colorSemanticDanger,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              icon: const Icon(Icons.logout),
              label: const Text(
                'Sign Out / Sair da Conta',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
              ),
              onPressed: () async {
                final confirm = await showDialog<bool>(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('🚪 Sign Out'),
                    content: const Text('Are you sure you want to log out of your account?'),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.of(ctx).pop(false),
                        child: const Text('Cancel'),
                      ),
                      FilledButton(
                        style: FilledButton.styleFrom(backgroundColor: ClTokens.colorSemanticDanger),
                        onPressed: () => Navigator.of(ctx).pop(true),
                        child: const Text('Sign Out'),
                      ),
                    ],
                  ),
                );

                if (confirm == true) {
                  AuthService.instance.logout();
                }
              },
            ),
          ],
        ),
      ),
    );
  }
}

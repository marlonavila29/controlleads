import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../theme/tokens.dart';

class OnboardingSlide {
  const OnboardingSlide({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.badge,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final String badge;
}

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key, this.isReplay = false});

  final bool isReplay;

  static Future<bool> shouldShow() async {
    final prefs = await SharedPreferences.getInstance();
    return !(prefs.getBool('controlleads_onboarding_seen') ?? false);
  }

  static Future<void> markSeen() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('controlleads_onboarding_seen', true);
  }

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _controller = PageController();
  int _currentIndex = 0;

  final List<OnboardingSlide> _slides = const [
    OnboardingSlide(
      title: 'Welcome to ControlLeads',
      subtitle: 'The all-in-one CRM built for international student recruitment, candidate tracking, and enrollment growth.',
      icon: Icons.rocket_launch,
      color: Colors.indigo,
      badge: '🚀 WELCOME',
    ),
    OnboardingSlide(
      title: 'Visual Pipeline & Kanban',
      subtitle: 'Track candidate status from Lead to Enrolled Student. Change statuses smoothly with confirmation alerts.',
      icon: Icons.view_kanban,
      color: Colors.deepPurple,
      badge: '📊 PIPELINE BOARD',
    ),
    OnboardingSlide(
      title: 'SLA Clock & Follow-Ups',
      subtitle: 'Never lose a prospective student. Monitor SLA timers on Hot Leads and log Calls, Emails, WhatsApp & Meetings.',
      icon: Icons.access_time_filled,
      color: Colors.amber,
      badge: '⏱️ SLA TRACKING',
    ),
    OnboardingSlide(
      title: 'Bulk Messaging Campaigns',
      subtitle: 'Reach hundreds of candidates via Email and WhatsApp using dynamic placeholders like {name} and {course}.',
      icon: Icons.send_rounded,
      color: Colors.green,
      badge: '✉️ BROADCASTS',
    ),
    OnboardingSlide(
      title: 'Custom Themes & Profile',
      subtitle: 'Switch between Obsidian Dark Mode and Slate Light Mode seamlessly. You can re-open this tour in Profile Settings.',
      icon: Icons.dark_mode,
      color: Colors.blueAccent,
      badge: '🎨 CUSTOM THEME',
    ),
  ];

  Future<void> _complete() async {
    await OnboardingScreen.markSeen();
    if (mounted) {
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final slide = _slides[_currentIndex];

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        actions: [
          TextButton(
            onPressed: _complete,
            child: const Text('Skip Tour', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _controller,
                itemCount: _slides.length,
                onPageChanged: (index) => setState(() => _currentIndex = index),
                itemBuilder: (context, index) {
                  final s = _slides[index];
                  return Padding(
                    padding: const EdgeInsets.all(ClTokens.space5),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Animated Hero Icon Card
                        Container(
                          width: 140,
                          height: 140,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: s.color.withAlpha(30),
                            border: Border.all(color: s.color.withAlpha(100), width: 2),
                            boxShadow: [
                              BoxShadow(
                                color: s.color.withAlpha(40),
                                blurRadius: 30,
                                spreadRadius: 5,
                              ),
                            ],
                          ),
                          child: Icon(s.icon, size: 64, color: s.color),
                        ),
                        const SizedBox(height: ClTokens.space5),

                        // Badge Pill
                        Chip(
                          label: Text(
                            s.badge,
                            style: TextStyle(
                              color: s.color,
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                            ),
                          ),
                          backgroundColor: s.color.withAlpha(20),
                        ),
                        const SizedBox(height: ClTokens.space4),

                        // Title
                        Text(
                          s.title,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: ClTokens.space3),

                        // Subtitle
                        Text(
                          s.subtitle,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 15,
                            height: 1.5,
                            color: Theme.of(context).textTheme.bodyMedium?.color?.withAlpha(180),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),

            // Indicator Dots & Navigation Controls Row
            Padding(
              padding: const EdgeInsets.all(ClTokens.space5),
              child: Column(
                children: [
                  // Dots
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      _slides.length,
                      (index) => AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        width: _currentIndex == index ? 24 : 8,
                        height: 8,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(4),
                          color: _currentIndex == index
                              ? slide.color
                              : Theme.of(context).dividerColor.withAlpha(100),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: ClTokens.space5),

                  // Actions Row
                  Row(
                    children: [
                      if (_currentIndex > 0)
                        Expanded(
                          child: OutlinedButton(
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            onPressed: () {
                              _controller.previousPage(
                                duration: const Duration(milliseconds: 300),
                                curve: Curves.easeInOut,
                              );
                            },
                            child: const Text('Back'),
                          ),
                        )
                      else
                        const Spacer(),

                      const SizedBox(width: ClTokens.space3),

                      Expanded(
                        child: FilledButton(
                          style: FilledButton.styleFrom(
                            backgroundColor: slide.color,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          onPressed: () {
                            if (_currentIndex < _slides.length - 1) {
                              _controller.nextPage(
                                duration: const Duration(milliseconds: 300),
                                curve: Curves.easeInOut,
                              );
                            } else {
                              _complete();
                            }
                          },
                          child: Text(
                            _currentIndex == _slides.length - 1 ? 'Get Started' : 'Next',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

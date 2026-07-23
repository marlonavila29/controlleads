import 'package:flutter/material.dart';

import '../broadcasts/broadcasts_screen.dart';
import '../leads/leads_screen.dart';
import '../notifications/notifications_screen.dart';
import '../notifications/notifications_service.dart';
import '../onboarding/spotlight_tour.dart';
import '../profile/profile_screen.dart';

class MainShellScreen extends StatefulWidget {
  const MainShellScreen({super.key});

  @override
  State<MainShellScreen> createState() => _MainShellScreenState();
}

class _MainShellScreenState extends State<MainShellScreen> {
  int _currentIndex = 0;
  int _unreadCount = 0;
  bool _showSpotlightTour = false;

  final GlobalKey _viewSwitcherKey = GlobalKey();
  final GlobalKey _searchKey = GlobalKey();
  final GlobalKey _candidateCardKey = GlobalKey();
  final GlobalKey _fabKey = GlobalKey();
  final GlobalKey _bottomNavKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    _refreshUnread();
    _checkFirstTimeOnboarding();
  }

  Future<void> _checkFirstTimeOnboarding() async {
    final shouldShow = await SpotlightTourOverlay.shouldShow();
    if (shouldShow && mounted) {
      setState(() {
        _showSpotlightTour = true;
      });
    }
  }

  Future<void> _refreshUnread() async {
    try {
      final count = await NotificationsService.instance.unreadCount();
      if (mounted) setState(() => _unreadCount = count);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final pages = [
      LeadsScreen(
        viewSwitcherKey: _viewSwitcherKey,
        searchKey: _searchKey,
        candidateCardKey: _candidateCardKey,
        fabKey: _fabKey,
      ),
      const BroadcastsScreen(),
      const NotificationsScreen(),
      ProfileScreen(
        onTriggerTour: () {
          setState(() {
            _currentIndex = 0;
            _showSpotlightTour = true;
          });
        },
      ),
    ];

    return Stack(
      children: [
        Scaffold(
          body: IndexedStack(
            index: _currentIndex,
            children: pages,
          ),
          bottomNavigationBar: NavigationBar(
            key: _bottomNavKey,
            selectedIndex: _currentIndex,
            onDestinationSelected: (index) {
              setState(() {
                _currentIndex = index;
              });
              if (index == 2) {
                _refreshUnread();
              }
            },
            destinations: [
              const NavigationDestination(
                icon: Icon(Icons.people_outline),
                selectedIcon: Icon(Icons.people),
                label: 'Candidates',
              ),
              const NavigationDestination(
                icon: Icon(Icons.send_outlined),
                selectedIcon: Icon(Icons.send),
                label: 'Broadcasts',
              ),
              NavigationDestination(
                icon: Badge(
                  isLabelVisible: _unreadCount > 0,
                  label: Text(_unreadCount > 9 ? '9+' : '$_unreadCount'),
                  child: const Icon(Icons.notifications_outlined),
                ),
                selectedIcon: Badge(
                  isLabelVisible: _unreadCount > 0,
                  label: Text(_unreadCount > 9 ? '9+' : '$_unreadCount'),
                  child: const Icon(Icons.notifications),
                ),
                label: 'Alerts',
              ),
              const NavigationDestination(
                icon: Icon(Icons.person_outline),
                selectedIcon: Icon(Icons.person),
                label: 'Profile',
              ),
            ],
          ),
        ),

        // Interactive Spotlight Feature Tour Overlay with Exact Measured GlobalKeys
        if (_showSpotlightTour)
          SpotlightTourOverlay(
            viewSwitcherKey: _viewSwitcherKey,
            searchKey: _searchKey,
            candidateCardKey: _candidateCardKey,
            fabKey: _fabKey,
            bottomNavKey: _bottomNavKey,
            onDismiss: () {
              setState(() => _showSpotlightTour = false);
            },
          ),
      ],
    );
  }
}

import 'package:flutter/material.dart';

import '../leads/lead_detail_screen.dart';
import '../theme/tokens.dart';
import 'notifications_service.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  List<AppNotification>? _items;
  String? _error;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    try {
      final items = await NotificationsService.instance.list();
      if (mounted) setState(() => _items = items);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  Color _dotColor(String type) => switch (type) {
        'SLA_BREACH' => ClTokens.colorStatusHotLead,
        'FOLLOW_UP_DUE' => ClTokens.colorBrandAccent,
        _ => ClTokens.colorNeutral400,
      };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          TextButton(
            onPressed: () async {
              await NotificationsService.instance.markAllRead();
              _reload();
            },
            child: const Text('Mark all read'),
          ),
        ],
      ),
      body: _error != null
          ? Center(child: Text(_error!))
          : _items == null
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _reload,
                  child: _items!.isEmpty
                      ? ListView(children: const [
                          SizedBox(height: 120),
                          Center(child: Text("You're all caught up 🎉")),
                        ])
                      : ListView.separated(
                          itemCount: _items!.length,
                          separatorBuilder: (_, _) => const Divider(height: 1),
                          itemBuilder: (context, index) {
                            final n = _items![index];
                            return Container(
                              color: n.isRead
                                  ? null
                                  : ClTokens.colorBrandPrimarySoft,
                              child: ListTile(
                                leading: CircleAvatar(
                                    radius: 5, backgroundColor: _dotColor(n.type)),
                                title: Text(n.text,
                                    style: const TextStyle(fontSize: 14)),
                                subtitle: Text(
                                  n.createdAt.substring(0, 16).replaceFirst('T', ' '),
                                  style: const TextStyle(fontSize: 12),
                                ),
                                onTap: () async {
                                  if (!n.isRead) {
                                    await NotificationsService.instance.markRead(n.id);
                                  }
                                  if (n.leadId != null && context.mounted) {
                                    await Navigator.of(context).push(
                                      MaterialPageRoute(
                                        builder: (_) =>
                                            LeadDetailScreen(leadId: n.leadId!),
                                      ),
                                    );
                                  }
                                  _reload();
                                },
                              ),
                            );
                          },
                        ),
                ),
    );
  }
}

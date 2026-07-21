import '../core/api_client.dart';

class AppNotification {
  const AppNotification(this.json);

  final Map<String, dynamic> json;

  String get id => json['id'] as String;
  String get type => json['type'] as String;
  String? get leadId => json['leadId'] as String?;
  String? get leadName => json['leadName'] as String?;
  String? get readAt => json['readAt'] as String?;
  String get createdAt => json['createdAt'] as String;

  bool get isRead => readAt != null;

  String get text {
    final name = leadName ?? 'A lead';
    return switch (type) {
      'SLA_BREACH' => '$name is going cold — no contact within SLA',
      'FOLLOW_UP_DUE' => 'Follow-up due for $name',
      _ => name,
    };
  }
}

class NotificationsService {
  const NotificationsService._();

  static const NotificationsService instance = NotificationsService._();

  Future<List<AppNotification>> list() async {
    final data = await ApiClient.instance.get('/api/notifications') as List<dynamic>;
    return data
        .map((e) => AppNotification(e as Map<String, dynamic>))
        .toList(growable: false);
  }

  Future<int> unreadCount() async {
    final data = await ApiClient.instance.get('/api/notifications/unread-count')
        as Map<String, dynamic>;
    return (data['count'] as num).toInt();
  }

  Future<void> markRead(String id) async {
    await ApiClient.instance.post('/api/notifications/$id/read', {});
  }

  Future<void> markAllRead() async {
    await ApiClient.instance.post('/api/notifications/read-all', {});
  }
}

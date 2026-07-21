import '../core/api_client.dart';

const funnelStages = ['LEAD', 'HOT_LEAD', 'APPLICATION', 'STUDENT'];
const allStatuses = ['LEAD', 'HOT_LEAD', 'APPLICATION', 'STUDENT', 'STALLED'];

const statusLabels = {
  'LEAD': 'Lead',
  'HOT_LEAD': 'Hot lead',
  'APPLICATION': 'Application',
  'STUDENT': 'Student',
  'STALLED': 'Stalled',
};

String? nextStage(String status) => switch (status) {
      'LEAD' => 'HOT_LEAD',
      'HOT_LEAD' => 'APPLICATION',
      'APPLICATION' => 'STUDENT',
      _ => null,
    };

class CatalogItem {
  const CatalogItem({required this.id, required this.name, required this.active});

  final String id;
  final String name;
  final bool active;

  factory CatalogItem.fromJson(Map<String, dynamic> json) => CatalogItem(
        id: json['id'] as String,
        name: json['name'] as String,
        active: json['active'] as bool,
      );
}

class Lead {
  const Lead(this.json);

  final Map<String, dynamic> json;

  String get id => json['id'] as String;
  String get fullName => json['fullName'] as String;
  String get countryCode => json['countryCode'] as String;
  String? get email => json['email'] as String?;
  String? get phone => json['phone'] as String?;
  String get courseId => json['courseId'] as String;
  String get channelId => json['channelId'] as String;
  String get status => json['status'] as String;
  String? get stalledFromStatus => json['stalledFromStatus'] as String?;
  String? get stallReasonId => json['stallReasonId'] as String?;
  String get assignedToName => json['assignedToName'] as String;
  String get createdAt => json['createdAt'] as String;
}

const activityTypes = ['NOTE', 'CALL', 'EMAIL', 'WHATSAPP', 'MEETING', 'FOLLOW_UP'];

const activityLabels = {
  'NOTE': 'Note',
  'CALL': 'Call',
  'EMAIL': 'Email',
  'WHATSAPP': 'WhatsApp',
  'MEETING': 'Meeting',
  'FOLLOW_UP': 'Follow-up',
};

const activityIcons = {
  'NOTE': '📝',
  'CALL': '📞',
  'EMAIL': '✉️',
  'WHATSAPP': '💬',
  'MEETING': '🤝',
  'FOLLOW_UP': '⏰',
};

class Activity {
  const Activity(this.json);

  final Map<String, dynamic> json;

  String get id => json['id'] as String;
  String get type => json['type'] as String;
  String get content => json['content'] as String;
  String? get dueAt => json['dueAt'] as String?;
  String? get completedAt => json['completedAt'] as String?;
  String get createdByName => json['createdByName'] as String;
  String get createdAt => json['createdAt'] as String;
}

class StatusEvent {
  const StatusEvent(this.json);

  final Map<String, dynamic> json;

  String? get fromStatus => json['fromStatus'] as String?;
  String get toStatus => json['toStatus'] as String;
  String? get stallReasonId => json['stallReasonId'] as String?;
  String? get note => json['note'] as String?;
  String get changedByName => json['changedByName'] as String;
  String get changedAt => json['changedAt'] as String;
}

class LeadsService {
  const LeadsService._();

  static const LeadsService instance = LeadsService._();

  static final Map<String, String> _catalogNames = {};

  Future<List<CatalogItem>> catalog(String kind) async {
    final data = await ApiClient.instance.get('/api/$kind') as List<dynamic>;
    final items = data
        .map((e) => CatalogItem.fromJson(e as Map<String, dynamic>))
        .toList(growable: false);
    for (final item in items) {
      _catalogNames[item.id] = item.name;
    }
    return items;
  }

  String catalogName(String? id) => id == null ? '—' : (_catalogNames[id] ?? '—');

  Future<List<Lead>> list({String? status, String? q}) async {
    final data = await ApiClient.instance.get('/api/leads', {
      'size': '100',
      if (status != null) 'status': status,
      if (q != null && q.isNotEmpty) 'q': q,
    }) as Map<String, dynamic>;
    return (data['content'] as List<dynamic>)
        .map((e) => Lead(e as Map<String, dynamic>))
        .toList(growable: false);
  }

  Future<(Lead, List<StatusEvent>)> detail(String id) async {
    final data = await ApiClient.instance.get('/api/leads/$id') as Map<String, dynamic>;
    final events = (data['statusHistory'] as List<dynamic>)
        .map((e) => StatusEvent(e as Map<String, dynamic>))
        .toList(growable: false);
    return (Lead(data['lead'] as Map<String, dynamic>), events);
  }

  Future<Lead> create(Map<String, dynamic> payload) async {
    final data = await ApiClient.instance.post('/api/leads', payload);
    return Lead(data as Map<String, dynamic>);
  }

  Future<List<Activity>> activities(String leadId) async {
    final data =
        await ApiClient.instance.get('/api/leads/$leadId/activities') as List<dynamic>;
    return data
        .map((e) => Activity(e as Map<String, dynamic>))
        .toList(growable: false);
  }

  Future<void> addActivity(String leadId, String type, String content,
      {String? dueAt}) async {
    await ApiClient.instance.post('/api/leads/$leadId/activities', {
      'type': type,
      'content': content,
      if (dueAt != null) 'dueAt': dueAt,
    });
  }

  Future<void> completeActivity(String activityId) async {
    await ApiClient.instance.post('/api/activities/$activityId/complete', {});
  }

  Future<Lead> transition(String id, String toStatus,
      {String? stallReasonId, String? note}) async {
    final data = await ApiClient.instance.post('/api/leads/$id/transition', {
      'toStatus': toStatus,
      if (stallReasonId != null) 'stallReasonId': stallReasonId,
      if (note != null && note.isNotEmpty) 'note': note,
    });
    return Lead(data as Map<String, dynamic>);
  }
}

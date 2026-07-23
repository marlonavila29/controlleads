import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import 'leads_service.dart';

class LeadDetailScreen extends StatefulWidget {
  const LeadDetailScreen({super.key, required this.leadId});

  final String leadId;

  @override
  State<LeadDetailScreen> createState() => _LeadDetailScreenState();
}

class _LeadDetailScreenState extends State<LeadDetailScreen> {
  Lead? _lead;
  List<StatusEvent> _history = [];
  List<Activity> _activities = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    try {
      final (lead, history) = await LeadsService.instance.detail(widget.leadId);
      List<Activity> activities = [];
      try {
        activities = await LeadsService.instance.activities(widget.leadId);
      } catch (_) {
        // Activities endpoint may not be live yet.
      }
      if (mounted) {
        setState(() {
          _lead = lead;
          _history = history.reversed.toList();
          _activities = activities;
          _error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  Future<void> _addActivitySheet() async {
    String type = 'NOTE';
    final contentController = TextEditingController();
    DateTime? dueAt;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => StatefulBuilder(
        builder: (sheetContext, setSheetState) => Padding(
          padding: EdgeInsets.only(
            left: ClTokens.space4,
            right: ClTokens.space4,
            top: ClTokens.space4,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + ClTokens.space4,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Log activity',
                  style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: ClTokens.space3),
              Wrap(
                spacing: ClTokens.space2,
                children: [
                  for (final t in activityTypes)
                    ChoiceChip(
                      selected: type == t,
                      label: Text('${activityIcons[t]} ${activityLabels[t]}'),
                      onSelected: (_) => setSheetState(() => type = t),
                    ),
                ],
              ),
              const SizedBox(height: ClTokens.space3),
              TextField(
                controller: contentController,
                decoration: const InputDecoration(
                    labelText: 'What happened?', hintText: 'Called about documents…'),
                maxLines: 2,
              ),
              if (type == 'FOLLOW_UP') ...[
                const SizedBox(height: ClTokens.space3),
                OutlinedButton.icon(
                  icon: const Icon(Icons.event),
                  label: Text(dueAt == null
                      ? 'Pick due date'
                      : dueAt.toString().substring(0, 16)),
                  onPressed: () async {
                    final now = DateTime.now();
                    final date = await showDatePicker(
                      context: sheetContext,
                      firstDate: now,
                      lastDate: now.add(const Duration(days: 365)),
                    );
                    if (date != null) {
                      setSheetState(() =>
                          dueAt = DateTime(date.year, date.month, date.day, 9));
                    }
                  },
                ),
              ],
              const SizedBox(height: ClTokens.space4),
              FilledButton(
                onPressed: () {
                  final content = contentController.text.trim();
                  if (content.isEmpty) return;
                  Navigator.of(sheetContext).pop();
                  LeadsService.instance
                      .addActivity(widget.leadId, type, content,
                          dueAt: dueAt?.toUtc().toIso8601String())
                      .then((_) => _reload())
                      .catchError((Object e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context)
                          .showSnackBar(SnackBar(content: Text(e.toString())));
                    }
                  });
                },
                child: const Text('Save'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _confirmTransition(String to, {String? reasonId, String? note}) async {
    if (_lead == null) return;
    final fromLabel = statusLabels[_lead!.status] ?? _lead!.status;
    final toLabel = statusLabels[to] ?? to;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('⚡ Confirm Status Change'),
        content: Text('Are you sure you want to move candidate "${_lead!.fullName}" from $fromLabel to $toLabel?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Confirm Move'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      _transition(to, reasonId: reasonId, note: note);
    }
  }

  Future<void> _transition(String to, {String? reasonId, String? note}) async {
    try {
      await LeadsService.instance
          .transition(widget.leadId, to, stallReasonId: reasonId, note: note);
      _reload();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  Future<void> _askStallReason() async {
    final reasons =
        (await LeadsService.instance.catalog('stall-reasons')).where((r) => r.active);
    if (!mounted) return;
    String? selected;
    final noteController = TextEditingController();
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => StatefulBuilder(
        builder: (sheetContext, setSheetState) => Padding(
          padding: EdgeInsets.only(
            left: ClTokens.space4,
            right: ClTokens.space4,
            top: ClTokens.space4,
            bottom: MediaQuery.of(sheetContext).viewInsets.bottom + ClTokens.space4,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Why did this lead stop?',
                  style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: ClTokens.space3),
              RadioGroup<String>(
                groupValue: selected,
                onChanged: (v) => setSheetState(() => selected = v),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    for (final reason in reasons)
                      RadioListTile<String>(
                        dense: true,
                        title: Text(reason.name),
                        value: reason.id,
                      ),
                  ],
                ),
              ),
              TextField(
                controller: noteController,
                decoration: const InputDecoration(labelText: 'Optional note'),
              ),
              const SizedBox(height: ClTokens.space4),
              FilledButton(
                onPressed: () {
                  if (selected == null) return;
                  Navigator.of(sheetContext).pop();
                  _transition('STALLED',
                      reasonId: selected, note: noteController.text.trim());
                },
                child: const Text('Mark as stalled'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final lead = _lead;
    return Scaffold(
      appBar: AppBar(title: Text(lead?.fullName ?? 'Lead')),
      body: _error != null
          ? Center(child: Text(_error!))
          : lead == null
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                  padding: const EdgeInsets.all(ClTokens.space4),
                  children: [
                    Row(
                      children: [
                        Chip(
                          label: Text(statusLabels[lead.status] ?? lead.status),
                          backgroundColor: statusColor(lead.status),
                          labelStyle:
                              const TextStyle(color: Colors.white, fontSize: 12),
                        ),
                        const SizedBox(width: ClTokens.space2),
                        if (lead.status == 'STALLED')
                          Expanded(
                            child: Text(
                              'Stalled at ${statusLabels[lead.stalledFromStatus] ?? '—'} — '
                              '${LeadsService.instance.catalogName(lead.stallReasonId)}',
                              style: TextStyle(
                                  fontSize: ClTokens.fontSizeXs,
                                  color: ClTokens.colorNeutral600),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: ClTokens.space4),
                    _info('Country', lead.countryCode),
                    _info('Email', lead.email ?? '—'),
                    _info('Phone', lead.phone ?? '—'),
                    _info('Course', LeadsService.instance.catalogName(lead.courseId)),
                    _info('Channel', LeadsService.instance.catalogName(lead.channelId)),
                    _info('Owner', lead.assignedToName),
                    const SizedBox(height: ClTokens.space5),
                    if (nextStage(lead.status) case final next?)
                      FilledButton.icon(
                        icon: const Icon(Icons.arrow_forward),
                        label: Text('Advance to ${statusLabels[next]}'),
                        onPressed: () => _confirmTransition(next),
                      ),
                    if (lead.status == 'STALLED')
                      FilledButton.icon(
                        icon: const Icon(Icons.replay),
                        label: Text(
                            'Reactivate to ${statusLabels[lead.stalledFromStatus] ?? ''}'),
                        onPressed: () => _transition(lead.stalledFromStatus!),
                      )
                    else if (lead.status != 'STUDENT') ...[
                      const SizedBox(height: ClTokens.space2),
                      OutlinedButton.icon(
                        icon: Icon(Icons.pause_circle_outline,
                            color: ClTokens.colorSemanticDanger),
                        label: Text('Mark as stalled',
                            style: TextStyle(color: ClTokens.colorSemanticDanger)),
                        onPressed: _askStallReason,
                      ),
                    ],
                    const SizedBox(height: ClTokens.space6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Timeline',
                            style: TextStyle(
                                fontSize: ClTokens.fontSizeMd,
                                fontWeight: FontWeight.w600)),
                        TextButton.icon(
                          icon: const Icon(Icons.add_comment_outlined, size: 18),
                          label: const Text('Log activity'),
                          onPressed: _addActivitySheet,
                        ),
                      ],
                    ),
                    const SizedBox(height: ClTokens.space2),
                    for (final entry in _timelineEntries()) entry,
                  ],
                ),
    );
  }

  /// Status events + activities merged, newest first.
  List<Widget> _timelineEntries() {
    final entries = <(String, Widget)>[
      for (final event in _history)
        (
          event.changedAt,
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: CircleAvatar(
                radius: 6, backgroundColor: statusColor(event.toStatus)),
            title: Text(
              event.fromStatus == null
                  ? 'Created as ${statusLabels[event.toStatus]}'
                  : '${statusLabels[event.fromStatus]} → ${statusLabels[event.toStatus]}'
                      '${event.stallReasonId != null ? ' (${LeadsService.instance.catalogName(event.stallReasonId)})' : ''}',
              style: const TextStyle(fontSize: 14),
            ),
            subtitle: Text(
              '${event.changedByName} · ${event.changedAt.substring(0, 16).replaceFirst('T', ' ')}'
              '${event.note != null ? '\n“${event.note}”' : ''}',
              style: const TextStyle(fontSize: 12),
            ),
          ),
        ),
      for (final activity in _activities)
        (
          activity.createdAt,
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: Text(activityIcons[activity.type] ?? '•',
                style: const TextStyle(fontSize: 16)),
            title: Text(
              '${activityLabels[activity.type]}: ${activity.content}',
              style: const TextStyle(fontSize: 14),
            ),
            subtitle: Text(
              '${activity.createdByName} · ${activity.createdAt.substring(0, 16).replaceFirst('T', ' ')}'
              '${activity.type == 'FOLLOW_UP' && activity.completedAt != null ? ' · ✓ done' : ''}',
              style: const TextStyle(fontSize: 12),
            ),
            trailing: activity.type == 'FOLLOW_UP' && activity.completedAt == null
                ? TextButton(
                    onPressed: () => LeadsService.instance
                        .completeActivity(activity.id)
                        .then((_) => _reload()),
                    child: const Text('Done ✓'),
                  )
                : null,
          ),
        ),
    ]..sort((a, b) => b.$1.compareTo(a.$1));
    return entries.map((e) => e.$2).toList(growable: false);
  }

  Widget _info(String label, String value) => Padding(
        padding: const EdgeInsets.symmetric(vertical: ClTokens.space1),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 90,
              child: Text(label,
                  style: TextStyle(
                      fontSize: ClTokens.fontSizeSm,
                      color: ClTokens.colorNeutral600)),
            ),
            Expanded(
              child: Text(value,
                  style: const TextStyle(fontSize: ClTokens.fontSizeSm)),
            ),
          ],
        ),
      );
}

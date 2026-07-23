import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../theme/tokens.dart';

class BroadcastsScreen extends StatefulWidget {
  const BroadcastsScreen({super.key});

  @override
  State<BroadcastsScreen> createState() => _BroadcastsScreenState();
}

class _BroadcastsScreenState extends State<BroadcastsScreen> {
  String _channel = 'EMAIL'; // EMAIL or WHATSAPP
  String _targetGroup = 'ALL'; // ALL, HOT_LEAD, APPLICATION, STUDENT, STALLED
  final _subjectController = TextEditingController(text: 'Important Update Regarding Your Application');
  final _messageController = TextEditingController(
    text: 'Hello {name},\n\nWe have an exciting update regarding your application for {course}. Please contact your counselor {counselor} to proceed.\n\nBest regards,\nControlLeads Team',
  );

  bool _sending = false;
  String? _statusMessage;
  List<dynamic> _logs = [];

  @override
  void initState() {
    super.initState();
    _fetchLogs();
  }

  Future<void> _fetchLogs() async {
    try {
      final res = await ApiClient.instance.get('/api/communications/logs?size=20');
      if (mounted && res.statusCode == 200) {
        setState(() {
          _logs = (res.data['content'] as List? ?? []);
        });
      }
    } catch (_) {}
  }

  Future<void> _sendBroadcast() async {
    final msg = _messageController.text.trim();
    if (msg.isEmpty) return;

    setState(() {
      _sending = true;
      _statusMessage = null;
    });

    try {
      final payload = {
        'channel': _channel,
        'targetGroup': _targetGroup,
        'subject': _channel == 'EMAIL' ? _subjectController.text.trim() : null,
        'template': msg,
      };

      final res = await ApiClient.instance.post('/api/communications/broadcast', payload);
      if (mounted) {
        setState(() {
          _sending = false;
          _statusMessage = '✅ Broadcast sent! ${res.data['dispatchedCount']} messages dispatched.';
        });
        _fetchLogs();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _sending = false;
          _statusMessage = '❌ Error sending broadcast: $e';
        });
      }
    }
  }

  void _insertPlaceholder(String token) {
    final text = _messageController.text;
    final selection = _messageController.selection;
    if (selection.start >= 0) {
      final newText = text.replaceRange(selection.start, selection.end, token);
      _messageController.value = TextEditingValue(
        text: newText,
        selection: TextSelection.collapsed(offset: selection.start + token.length),
      );
    } else {
      _messageController.text = text + token;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bulk Messages'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(ClTokens.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Channel Selector
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: 'EMAIL', label: Text('✉️ Email'), icon: Icon(Icons.email)),
                ButtonSegment(value: 'WHATSAPP', label: Text('💬 WhatsApp'), icon: Icon(Icons.chat)),
              ],
              selected: {_channel},
              onSelectionChanged: (set) => setState(() => _channel = set.first),
            ),
            const SizedBox(height: ClTokens.space4),

            // Target Group Selector
            Card(
              child: Padding(
                padding: const EdgeInsets.all(ClTokens.space3),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Target Candidates', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: ClTokens.space2),
                    DropdownButtonFormField<String>(
                      initialValue: _targetGroup,
                      decoration: const InputDecoration(border: OutlineInputBorder()),
                      items: const [
                        DropdownMenuItem(value: 'ALL', child: Text('👥 All Candidates')),
                        DropdownMenuItem(value: 'HOT_LEAD', child: Text('🔥 Hot Leads Only')),
                        DropdownMenuItem(value: 'APPLICATION', child: Text('📋 Application Submitted')),
                        DropdownMenuItem(value: 'STUDENT', child: Text('🎓 Enrolled Students')),
                        DropdownMenuItem(value: 'STALLED', child: Text('⚠️ Stalled Candidates')),
                      ],
                      onChanged: (val) => setState(() => _targetGroup = val ?? 'ALL'),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: ClTokens.space3),

            if (_channel == 'EMAIL') ...[
              TextField(
                controller: _subjectController,
                decoration: const InputDecoration(labelText: 'Email Subject', border: OutlineInputBorder()),
              ),
              const SizedBox(height: ClTokens.space3),
            ],

            // Message Editor & Placeholders
            Card(
              child: Padding(
                padding: const EdgeInsets.all(ClTokens.space3),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Message Template', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        ActionChip(label: const Text('{name}'), onPressed: () => _insertPlaceholder('{name}')),
                        ActionChip(label: const Text('{course}'), onPressed: () => _insertPlaceholder('{course}')),
                        ActionChip(label: const Text('{counselor}'), onPressed: () => _insertPlaceholder('{counselor}')),
                      ],
                    ),
                    const SizedBox(height: ClTokens.space2),
                    TextField(
                      controller: _messageController,
                      maxLines: 5,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        hintText: 'Compose broadcast message...',
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: ClTokens.space4),

            if (_statusMessage != null) ...[
              Text(_statusMessage!, style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: ClTokens.space3),
            ],

            FilledButton.icon(
              icon: _sending ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.send),
              label: Text(_sending ? 'Dispatching...' : 'Dispatch Bulk Campaign'),
              onPressed: _sending ? null : _sendBroadcast,
            ),

            const SizedBox(height: ClTokens.space5),
            const Text('Recent Campaign History', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const SizedBox(height: ClTokens.space2),

            if (_logs.isEmpty)
              const Padding(
                padding: EdgeInsets.all(16),
                child: Center(child: Text('No broadcast history recorded yet.')),
              )
            else
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _logs.length,
                itemBuilder: (context, index) {
                  final log = _logs[index];
                  final isFailed = (log['status'] == 'FAILED');
                  return Card(
                    child: ListTile(
                      leading: Icon(log['channel'] == 'EMAIL' ? Icons.email : Icons.chat, color: log['channel'] == 'EMAIL' ? Colors.indigo : Colors.green),
                      title: Text(log['leadName'] ?? 'Candidate'),
                      subtitle: Text('${log['recipientAddress']} · ${log['contentSummary'] ?? ''}'),
                      trailing: Chip(
                        label: Text(log['status'] ?? 'SENT', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                        backgroundColor: isFailed ? Colors.red.withAlpha(50) : Colors.green.withAlpha(50),
                      ),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}

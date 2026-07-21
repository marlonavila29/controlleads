import 'package:flutter/material.dart';

import '../auth/auth_service.dart';
import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import 'lead_detail_screen.dart';
import 'lead_form_screen.dart';
import 'leads_service.dart';

class LeadsScreen extends StatefulWidget {
  const LeadsScreen({super.key});

  @override
  State<LeadsScreen> createState() => _LeadsScreenState();
}

class _LeadsScreenState extends State<LeadsScreen> {
  List<Lead>? _leads;
  String? _statusFilter;
  String _query = '';
  String? _error;

  @override
  void initState() {
    super.initState();
    // Warm the catalog name cache for list rows.
    LeadsService.instance.catalog('courses');
    LeadsService.instance.catalog('channels');
    LeadsService.instance.catalog('stall-reasons');
    _reload();
  }

  Future<void> _reload() async {
    try {
      final leads = await LeadsService.instance.list(status: _statusFilter, q: _query);
      if (mounted) setState(() => _leads = leads);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My leads'),
        actions: [
          IconButton(
            tooltip: 'Sign out',
            icon: const Icon(Icons.logout),
            onPressed: AuthService.instance.logout,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          final created = await Navigator.of(context).push<bool>(
            MaterialPageRoute(builder: (_) => const LeadFormScreen()),
          );
          if (created == true) _reload();
        },
        icon: const Icon(Icons.add),
        label: const Text('New lead'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
                ClTokens.space4, ClTokens.space3, ClTokens.space4, 0),
            child: TextField(
              decoration: const InputDecoration(
                prefixIcon: Icon(Icons.search),
                hintText: 'Search name, email or phone…',
              ),
              onSubmitted: (value) {
                _query = value;
                _reload();
              },
            ),
          ),
          SizedBox(
            height: 56,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(
                  horizontal: ClTokens.space4, vertical: ClTokens.space2),
              children: [
                for (final status in [null, ...allStatuses])
                  Padding(
                    padding: const EdgeInsets.only(right: ClTokens.space2),
                    child: FilterChip(
                      selected: _statusFilter == status,
                      label: Text(status == null ? 'All' : statusLabels[status]!),
                      onSelected: (_) {
                        setState(() => _statusFilter = status);
                        _reload();
                      },
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            child: _error != null
                ? Center(child: Text(_error!))
                : _leads == null
                    ? const Center(child: CircularProgressIndicator())
                    : RefreshIndicator(
                        onRefresh: _reload,
                        child: _leads!.isEmpty
                            ? ListView(children: const [
                                SizedBox(height: 120),
                                Center(child: Text('No leads yet — add the first one!')),
                              ])
                            : ListView.separated(
                                itemCount: _leads!.length,
                                separatorBuilder: (_, _) => const Divider(height: 1),
                                itemBuilder: (context, index) {
                                  final lead = _leads![index];
                                  return ListTile(
                                    leading: CircleAvatar(
                                      radius: 6,
                                      backgroundColor: statusColor(lead.status),
                                    ),
                                    title: Text(lead.fullName),
                                    subtitle: Text(
                                      '${lead.countryCode} · '
                                      '${LeadsService.instance.catalogName(lead.courseId)}',
                                    ),
                                    trailing: Text(
                                      statusLabels[lead.status] ?? lead.status,
                                      style: TextStyle(
                                        color: statusColor(lead.status),
                                        fontWeight: FontWeight.w600,
                                        fontSize: ClTokens.fontSizeXs,
                                      ),
                                    ),
                                    onTap: () async {
                                      await Navigator.of(context).push(
                                        MaterialPageRoute(
                                          builder: (_) => LeadDetailScreen(leadId: lead.id),
                                        ),
                                      );
                                      _reload();
                                    },
                                  );
                                },
                              ),
                      ),
          ),
        ],
      ),
    );
  }
}

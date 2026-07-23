import 'package:flutter/material.dart';

import '../theme/app_theme.dart';
import '../theme/tokens.dart';
import 'lead_detail_screen.dart';
import 'lead_form_screen.dart';
import 'leads_service.dart';

class LeadsScreen extends StatefulWidget {
  const LeadsScreen({
    super.key,
    this.initialTab = 0,
    this.viewSwitcherKey,
    this.searchKey,
    this.candidateCardKey,
    this.fabKey,
  });

  final int initialTab;
  final GlobalKey? viewSwitcherKey;
  final GlobalKey? searchKey;
  final GlobalKey? candidateCardKey;
  final GlobalKey? fabKey;

  @override
  State<LeadsScreen> createState() => _LeadsScreenState();
}

class _LeadsScreenState extends State<LeadsScreen> {
  List<Lead>? _leads;
  String? _statusFilter;
  String? _courseFilter;
  String? _channelFilter;
  String _query = '';
  String? _error;
  late int _currentTab;

  List<CatalogItem> _courses = [];
  List<CatalogItem> _channels = [];

  @override
  void initState() {
    super.initState();
    _currentTab = widget.initialTab;
    _loadCatalogs();
    _reload();
  }

  @override
  void didUpdateWidget(LeadsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.initialTab != widget.initialTab) {
      setState(() {
        _currentTab = widget.initialTab;
      });
    }
  }

  Future<void> _loadCatalogs() async {
    try {
      final courses = await LeadsService.instance.catalog('courses');
      final channels = await LeadsService.instance.catalog('channels');
      await LeadsService.instance.catalog('stall-reasons');
      if (mounted) {
        setState(() {
          _courses = courses.where((c) => c.active).toList();
          _channels = channels.where((c) => c.active).toList();
        });
      }
    } catch (_) {}
  }

  Future<void> _reload() async {
    try {
      final leads = await LeadsService.instance.list(status: _statusFilter, q: _query);
      if (mounted) setState(() => _leads = leads);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    }
  }

  List<Lead> get _filteredLeads {
    if (_leads == null) return [];
    return _leads!.where((l) {
      if (_courseFilter != null && l.courseId != _courseFilter) return false;
      if (_channelFilter != null && l.channelId != _channelFilter) return false;
      return true;
    }).toList();
  }

  Future<void> _confirmStatusChange(Lead lead, String newStatus) async {
    if (lead.status == newStatus) return;
    final fromLabel = statusLabels[lead.status] ?? lead.status;
    final toLabel = statusLabels[newStatus] ?? newStatus;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('⚡ Confirm Status Change'),
        content: Text('Are you sure you want to move candidate "${lead.fullName}" from $fromLabel to $toLabel?'),
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
      try {
        await LeadsService.instance.transition(lead.id, newStatus);
        _reload();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
        }
      }
    }
  }

  void _resetFilters() {
    setState(() {
      _query = '';
      _statusFilter = null;
      _courseFilter = null;
      _channelFilter = null;
    });
    _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_currentTab == 0 ? 'Candidates Directory' : 'Pipeline Board'),
      ),
      floatingActionButton: FloatingActionButton.extended(
        key: widget.fabKey,
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
          // View Switcher Tabs (Candidate List vs Pipeline Board)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: ClTokens.space4, vertical: ClTokens.space2),
            child: Row(
              children: [
                Expanded(
                  child: SegmentedButton<int>(
                    key: widget.viewSwitcherKey,
                    segments: const [
                      ButtonSegment(value: 0, label: Text('📋 Candidates'), icon: Icon(Icons.list)),
                      ButtonSegment(value: 1, label: Text('📊 Board'), icon: Icon(Icons.view_kanban)),
                    ],
                    selected: {_currentTab},
                    onSelectionChanged: (set) => setState(() => _currentTab = set.first),
                  ),
                ),
              ],
            ),
          ),

          // Search Bar
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: ClTokens.space4, vertical: ClTokens.space2),
            child: TextField(
              key: widget.searchKey,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.search),
                hintText: 'Search candidate name, email, phone...',
                suffixIcon: _query.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          setState(() => _query = '');
                          _reload();
                        },
                      )
                    : null,
              ),
              onSubmitted: (value) {
                _query = value;
                _reload();
              },
            ),
          ),

          // Column Select Filters Row (Course, Channel, Status)
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: ClTokens.space4, vertical: 4),
            child: Row(
              children: [
                // Course Dropdown Container
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 2),
                  decoration: BoxDecoration(
                    color: Theme.of(context).cardColor,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: Theme.of(context).dividerColor.withAlpha(60)),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String?>(
                      value: _courseFilter,
                      hint: const Text('All Courses', style: TextStyle(fontSize: 13)),
                      onChanged: (val) => setState(() => _courseFilter = val),
                      items: [
                        const DropdownMenuItem(value: null, child: Text('All Courses')),
                        for (final c in _courses) DropdownMenuItem(value: c.id, child: Text(c.name)),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: ClTokens.space3),

                // Channel Dropdown Container
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 2),
                  decoration: BoxDecoration(
                    color: Theme.of(context).cardColor,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: Theme.of(context).dividerColor.withAlpha(60)),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String?>(
                      value: _channelFilter,
                      hint: const Text('All Channels', style: TextStyle(fontSize: 13)),
                      onChanged: (val) => setState(() => _channelFilter = val),
                      items: [
                        const DropdownMenuItem(value: null, child: Text('All Channels')),
                        for (final c in _channels) DropdownMenuItem(value: c.id, child: Text(c.name)),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: ClTokens.space3),

                if (_statusFilter != null || _courseFilter != null || _channelFilter != null || _query.isNotEmpty)
                  TextButton.icon(
                    style: TextButton.styleFrom(
                      foregroundColor: ClTokens.colorSemanticDanger,
                    ),
                    icon: const Icon(Icons.clear_all, size: 16),
                    label: const Text('Clear Filters'),
                    onPressed: _resetFilters,
                  ),
              ],
            ),
          ),

          // Status Filter Chips
          SizedBox(
            height: 48,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: ClTokens.space4, vertical: 4),
              children: [
                for (final status in [null, ...allStatuses])
                  Padding(
                    padding: const EdgeInsets.only(right: ClTokens.space2),
                    child: FilterChip(
                      selected: _statusFilter == status,
                      label: Text(status == null ? 'All Statuses' : statusLabels[status]!),
                      onSelected: (_) {
                        setState(() => _statusFilter = status);
                        _reload();
                      },
                    ),
                  ),
              ],
            ),
          ),

          // Content Area (List View vs Pipeline Board View)
          Expanded(
            child: _error != null
                ? Center(child: Text(_error!))
                : _leads == null
                    ? const Center(child: CircularProgressIndicator())
                    : RefreshIndicator(
                        onRefresh: _reload,
                        child: _currentTab == 0 ? _buildListView() : _buildBoardView(),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildListView() {
    final leads = _filteredLeads;
    if (leads.isEmpty) {
      return ListView(children: const [
        SizedBox(height: 100),
        Center(child: Text('No candidates found matching selected filters.')),
      ]);
    }

    return ListView.separated(
      itemCount: leads.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final lead = leads[index];
        final sColor = statusColor(lead.status);
        return ListTile(
          key: index == 0 ? widget.candidateCardKey : null,
          leading: CircleAvatar(
            radius: 6,
            backgroundColor: sColor,
          ),
          title: Text(lead.fullName, style: const TextStyle(fontWeight: FontWeight.bold)),
          subtitle: Text(
            '${lead.countryCode} · ${LeadsService.instance.catalogName(lead.courseId)} · ${lead.assignedToName}',
            style: const TextStyle(fontSize: 12),
          ),
          trailing: PopupMenuButton<String>(
            initialValue: lead.status,
            child: Chip(
              label: Text(
                statusLabels[lead.status] ?? lead.status,
                style: TextStyle(
                  color: sColor,
                  fontWeight: FontWeight.w700,
                  fontSize: ClTokens.fontSizeXs,
                ),
              ),
              backgroundColor: sColor.withAlpha(30),
            ),
            onSelected: (newStatus) => _confirmStatusChange(lead, newStatus),
            itemBuilder: (ctx) => [
              for (final st in allStatuses)
                PopupMenuItem(
                  value: st,
                  child: Text(statusLabels[st] ?? st),
                ),
            ],
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
    );
  }

  Widget _buildBoardView() {
    final leads = _filteredLeads;
    return ListView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.all(ClTokens.space3),
      children: [
        for (final status in allStatuses) ...[
          _buildKanbanColumn(status, leads.where((l) => l.status == status).toList()),
          const SizedBox(width: ClTokens.space3),
        ],
      ],
    );
  }

  Widget _buildKanbanColumn(String status, List<Lead> columnLeads) {
    final sColor = statusColor(status);
    final borderColor = Theme.of(context).dividerColor.withAlpha(50);
    return Container(
      width: 280,
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(ClTokens.radiusLg),
        border: Border.all(color: borderColor),
      ),
      padding: const EdgeInsets.all(ClTokens.space3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Column Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  CircleAvatar(radius: 5, backgroundColor: sColor),
                  const SizedBox(width: 8),
                  Text(
                    statusLabels[status] ?? status,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                ],
              ),
              Badge(
                label: Text('${columnLeads.length}'),
                backgroundColor: sColor.withAlpha(50),
                textColor: sColor,
              ),
            ],
          ),
          const Divider(height: 16),

          // Column Candidate Cards
          Expanded(
            child: columnLeads.isEmpty
                ? const Center(child: Text('No candidates in stage', style: TextStyle(fontSize: 12, color: Colors.grey)))
                : ListView.builder(
                    itemCount: columnLeads.length,
                    itemBuilder: (context, index) {
                      final lead = columnLeads[index];
                      return Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                          title: Text(lead.fullName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                          subtitle: Text(
                            '🎓 ${LeadsService.instance.catalogName(lead.courseId)}\n👤 ${lead.assignedToName}',
                            style: const TextStyle(fontSize: 11),
                          ),
                          trailing: PopupMenuButton<String>(
                            icon: const Icon(Icons.more_vert, size: 18),
                            onSelected: (newStatus) => _confirmStatusChange(lead, newStatus),
                            itemBuilder: (ctx) => [
                              for (final st in allStatuses)
                                PopupMenuItem(
                                  value: st,
                                  child: Text('Move to ${statusLabels[st]}'),
                                ),
                            ],
                          ),
                          onTap: () async {
                            await Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) => LeadDetailScreen(leadId: lead.id),
                              ),
                            );
                            _reload();
                          },
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

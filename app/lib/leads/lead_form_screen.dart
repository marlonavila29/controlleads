import 'package:flutter/material.dart';

import '../theme/tokens.dart';
import 'leads_service.dart';

class LeadFormScreen extends StatefulWidget {
  const LeadFormScreen({super.key});

  @override
  State<LeadFormScreen> createState() => _LeadFormScreenState();
}

class _LeadFormScreenState extends State<LeadFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _country = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();

  List<CatalogItem> _courses = [];
  List<CatalogItem> _channels = [];
  String? _courseId;
  String? _channelId;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadCatalogs();
  }

  Future<void> _loadCatalogs() async {
    final courses = await LeadsService.instance.catalog('courses');
    final channels = await LeadsService.instance.catalog('channels');
    if (mounted) {
      setState(() {
        _courses = courses.where((c) => c.active).toList();
        _channels = channels.where((c) => c.active).toList();
      });
    }
  }

  @override
  void dispose() {
    _name.dispose();
    _country.dispose();
    _email.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false) || _submitting) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await LeadsService.instance.create({
        'fullName': _name.text.trim(),
        'countryCode': _country.text.trim().toUpperCase(),
        if (_email.text.trim().isNotEmpty) 'email': _email.text.trim(),
        if (_phone.text.trim().isNotEmpty) 'phone': _phone.text.trim(),
        'courseId': _courseId,
        'channelId': _channelId,
      });
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        setState(() {
          _submitting = false;
          _error = e.toString();
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Register New Candidate')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: ClTokens.space4, vertical: ClTokens.space5),
          children: [
            TextFormField(
              controller: _name,
              decoration: const InputDecoration(
                labelText: 'Full Candidate Name *',
                hintText: 'e.g. Maria Silva',
              ),
              validator: (v) => (v == null || v.trim().isEmpty) ? 'Name is required' : null,
            ),
            const SizedBox(height: ClTokens.space4),

            TextFormField(
              controller: _country,
              maxLength: 2,
              textCapitalization: TextCapitalization.characters,
              decoration: const InputDecoration(
                labelText: 'Country Code (ISO 2-letter) *',
                hintText: 'e.g. BR, MX, VN',
                counterText: '',
              ),
              validator: (v) =>
                  (v == null || !RegExp(r'^[A-Za-z]{2}$').hasMatch(v.trim()))
                      ? 'Enter 2-letter ISO country code'
                      : null,
            ),
            const SizedBox(height: ClTokens.space4),

            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(
                labelText: 'Email Address',
                hintText: 'candidate@example.com',
              ),
            ),
            const SizedBox(height: ClTokens.space4),

            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Phone / WhatsApp',
                hintText: '+55 11 99999-9999',
              ),
            ),
            const SizedBox(height: ClTokens.space4),

            DropdownButtonFormField<String>(
              initialValue: _courseId,
              decoration: const InputDecoration(labelText: 'Program of Interest *'),
              items: [
                for (final c in _courses)
                  DropdownMenuItem(value: c.id, child: Text(c.name)),
              ],
              onChanged: (v) => setState(() => _courseId = v),
              validator: (v) => v == null ? 'Please select a course' : null,
            ),
            const SizedBox(height: ClTokens.space4),

            DropdownButtonFormField<String>(
              initialValue: _channelId,
              decoration: const InputDecoration(labelText: 'Acquisition Source Channel *'),
              items: [
                for (final c in _channels)
                  DropdownMenuItem(value: c.id, child: Text(c.name)),
              ],
              onChanged: (v) => setState(() => _channelId = v),
              validator: (v) => v == null ? 'Please select a channel' : null,
            ),

            if (_error != null) ...[
              const SizedBox(height: ClTokens.space4),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: ClTokens.colorSemanticDanger.withAlpha(25),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: ClTokens.colorSemanticDanger.withAlpha(80)),
                ),
                child: Text(_error!, style: TextStyle(color: ClTokens.colorSemanticDanger, fontWeight: FontWeight.bold)),
              ),
            ],
            const SizedBox(height: ClTokens.space5),

            FilledButton(
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onPressed: _submitting ? null : _submit,
              child: Text(
                _submitting ? 'Registering Candidate...' : 'Create Candidate Lead',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

import 'package:flutter_test/flutter_test.dart';

import 'package:controlleads_app/main.dart';

void main() {
  testWidgets('renders app shell', (WidgetTester tester) async {
    await tester.pumpWidget(const ControlLeadsApp());
    expect(find.text('ControlLeads'), findsOneWidget);
  });
}

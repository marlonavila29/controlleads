import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../theme/tokens.dart';

class TourStep {
  const TourStep({
    required this.title,
    required this.description,
    required this.icon,
    required this.targetKey,
    this.fallbackRect,
  });

  final String title;
  final String description;
  final IconData icon;
  final GlobalKey? targetKey;
  final Rect? fallbackRect;
}

class SpotlightTourOverlay extends StatefulWidget {
  const SpotlightTourOverlay({
    super.key,
    required this.onDismiss,
    this.viewSwitcherKey,
    this.searchKey,
    this.candidateCardKey,
    this.fabKey,
    this.bottomNavKey,
  });

  final VoidCallback onDismiss;
  final GlobalKey? viewSwitcherKey;
  final GlobalKey? searchKey;
  final GlobalKey? candidateCardKey;
  final GlobalKey? fabKey;
  final GlobalKey? bottomNavKey;

  static Future<bool> shouldShow() async {
    final prefs = await SharedPreferences.getInstance();
    return !(prefs.getBool('controlleads_onboarding_seen') ?? false);
  }

  static Future<void> markSeen() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('controlleads_onboarding_seen', true);
  }

  @override
  State<SpotlightTourOverlay> createState() => _SpotlightTourOverlayState();
}

class _SpotlightTourOverlayState extends State<SpotlightTourOverlay>
    with SingleTickerProviderStateMixin {
  int _currentStep = 0;
  late AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  List<TourStep> get _steps => [
        TourStep(
          title: 'View Switcher (Lista vs Board)',
          description: 'Alterne instantaneamente entre a Lista de Candidatos e o Kanban Visual do Pipeline Board.',
          icon: Icons.view_kanban,
          targetKey: widget.viewSwitcherKey,
        ),
        TourStep(
          title: 'Busca & Filtros por Colunas',
          description: 'Filtre candidatos por Nome, Curso (Program), Canal de Origem (Google, Indicação) ou Status.',
          icon: Icons.filter_alt,
          targetKey: widget.searchKey,
        ),
        TourStep(
          title: 'Mudança Rápida de Status',
          description: 'Toque no selo de status de qualquer candidato para alterar a etapa do funil com diálogo de confirmação.',
          icon: Icons.touch_app,
          targetKey: widget.candidateCardKey,
        ),
        TourStep(
          title: 'Cadastrar Novo Candidato',
          description: 'Clique no botão flutuante "+ New lead" a qualquer momento para cadastrar um novo aluno interessado.',
          icon: Icons.add_circle,
          targetKey: widget.fabKey,
        ),
        TourStep(
          title: 'Menu Inferior & Perfil',
          description: 'Navegue entre Candidatos, Disparos em Bloco, Central de Alertas e Tela de Perfil/Modo Escuro.',
          icon: Icons.navigation,
          targetKey: widget.bottomNavKey,
        ),
      ];

  Future<void> _finish() async {
    await SpotlightTourOverlay.markSeen();
    widget.onDismiss();
  }

  Rect? _getWidgetRect(GlobalKey? key) {
    if (key == null || key.currentContext == null) return null;
    final renderObject = key.currentContext!.findRenderObject();
    if (renderObject is RenderBox) {
      final offset = renderObject.localToGlobal(Offset.zero);
      final size = renderObject.size;
      return Rect.fromLTWH(offset.dx, offset.dy, size.width, size.height);
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final step = _steps[_currentStep];
    final targetRect = _getWidgetRect(step.targetKey);
    final screenSize = MediaQuery.of(context).size;

    final isUpperHalf = targetRect == null || targetRect.top < screenSize.height * 0.45;

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Stack(
        children: [
          // Dark Backdrop
          GestureDetector(
            onTap: () {
              if (_currentStep < _steps.length - 1) {
                setState(() => _currentStep++);
              } else {
                _finish();
              }
            },
            child: Container(
              width: double.infinity,
              height: double.infinity,
              color: Colors.black.withAlpha(210),
            ),
          ),

          // Dynamic Pixel-Perfect Spotlight Ring
          if (targetRect != null)
            AnimatedBuilder(
              animation: _pulseController,
              builder: (context, child) {
                final pulse = _pulseController.value * 6;
                return Positioned(
                  left: targetRect.left - 6 - (pulse / 2),
                  top: targetRect.top - 6 - (pulse / 2),
                  width: targetRect.width + 12 + pulse,
                  height: targetRect.height + 12 + pulse,
                  child: IgnorePointer(
                    child: Container(
                      decoration: BoxDecoration(
                        color: Colors.white.withAlpha(20),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: ClTokens.colorBrandPrimary,
                          width: 2.5,
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: ClTokens.colorBrandPrimary.withAlpha(140),
                            blurRadius: 16 + pulse,
                            spreadRadius: 2,
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),

          // Skip Button Top Right
          Positioned(
            top: 50,
            right: 20,
            child: SafeArea(
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.black87,
                  foregroundColor: Colors.white,
                  elevation: 6,
                  shape: const StadiumBorder(),
                ),
                icon: const Icon(Icons.close, size: 16),
                label: const Text('Pular Tour', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                onPressed: _finish,
              ),
            ),
          ),

          // Dynamically Positioned Tooltip Card
          Positioned(
            left: 20,
            right: 20,
            top: targetRect != null
                ? (isUpperHalf ? targetRect.bottom + 16 : null)
                : screenSize.height * 0.3,
            bottom: targetRect != null
                ? (!isUpperHalf ? (screenSize.height - targetRect.top) + 16 : null)
                : null,
            child: SafeArea(
              child: Card(
                elevation: 16,
                color: Theme.of(context).cardColor,
                shape: RoundedRectangleBorder(
                  side: const BorderSide(color: ClTokens.colorBrandPrimary, width: 1.5),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(ClTokens.space4),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          CircleAvatar(
                            radius: 18,
                            backgroundColor: ClTokens.colorBrandPrimary.withAlpha(40),
                            child: Icon(step.icon, size: 20, color: ClTokens.colorBrandPrimary),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              step.title,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: ClTokens.colorBrandPrimary,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(
                              '${_currentStep + 1}/${_steps.length}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 11,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: ClTokens.space3),

                      Text(
                        step.description,
                        style: TextStyle(
                          fontSize: 14,
                          height: 1.4,
                          color: Theme.of(context).textTheme.bodyMedium?.color?.withAlpha(200),
                        ),
                      ),
                      const SizedBox(height: ClTokens.space4),

                      // Navigation Actions Row
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          if (_currentStep > 0)
                            TextButton.icon(
                              icon: const Icon(Icons.arrow_back, size: 16),
                              label: const Text('Anterior'),
                              onPressed: () {
                                setState(() => _currentStep--);
                              },
                            )
                          else
                            const SizedBox(),

                          FilledButton.icon(
                            style: FilledButton.styleFrom(
                              backgroundColor: ClTokens.colorBrandPrimary,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(10),
                              ),
                            ),
                            icon: Icon(
                              _currentStep == _steps.length - 1 ? Icons.check_circle : Icons.arrow_forward,
                              size: 16,
                            ),
                            label: Text(
                              _currentStep == _steps.length - 1 ? 'Entendido! Entrar' : 'Próximo →',
                              style: const TextStyle(fontWeight: FontWeight.bold),
                            ),
                            onPressed: () {
                              if (_currentStep < _steps.length - 1) {
                                setState(() => _currentStep++);
                              } else {
                                _finish();
                              }
                            },
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

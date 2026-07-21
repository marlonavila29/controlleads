# Project Stack — ControlLeads

- **Backend/API**: Spring Boot (Java) + PostgreSQL. Fonte única da lógica de negócio.
- **Web**: Angular (signals) + ECharts (gráficos animados: funil, mapa por país, séries).
- **App**: Flutter (Android + iOS), consumindo as mesmas APIs.

## Reaproveitamento Angular ⇄ Flutter
UI não é compartilhável (TS vs Dart). Reuso real em 3 eixos:
1. **Contrato OpenAPI** (springdoc) como fonte única → clientes tipados gerados para Angular (TS) e Flutter (Dart).
2. **Design tokens** compartilhados (ex.: Style Dictionary) → SCSS/CSS vars no Angular + `ThemeData` no Flutter.
3. **Lógica no servidor** (Spring) → clientes magros; regras de funil/SLA no backend.

- **Topologia recomendada**: monorepo com `/backend`, `/web`, `/app`, `/shared` (OpenAPI + tokens). [Confirmar na Fase 0.]
- **Requisito**: UI bonita e polida, consistente entre web e app.

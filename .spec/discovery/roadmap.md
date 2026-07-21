---
brief: .spec/discovery/brief.md
modulos: .spec/discovery/module_*.md
entidades: .spec/discovery/entities.md
status: rascunho   # rascunho | aprovado
versao: 1
criado-em: 2026-07-21
---

# Roadmap de Execução — ControlLeads

## Critérios usados (nesta ordem)

1. **Desbloqueio**: Auth e catálogos destravam tudo; Leads destrava Activities/Dashboard/SLA.
2. **Risco técnico**: pipeline OpenAPI→clientes TS/Dart e design tokens compartilhados são a aposta arquitetural — validar na Fase 0 com 1 endpoint dummy nos **dois** clientes.
3. **Valor entregue**: caminho mais curto até "equipe cadastrando e movendo leads".
4. **Reversibilidade**: schema do funil (Status Events) e contrato de API vão cedo; gráficos/polish são refatoráveis.

---

## Fase 0 — Fundação técnica

- [ ] Monorepo `/backend` + `/web` + `/app` + `/shared` (contrato OpenAPI + design tokens)
- [ ] Scaffold Spring Boot + PostgreSQL (docker-compose) + Flyway inicial
- [ ] Scaffold Angular (workspace, tema base) e Flutter (projeto, tema base)
- [ ] **Pipeline de contrato**: springdoc → `openapi.yaml` → geração de cliente **TS** (web) e **Dart** (app), com 1 endpoint dummy consumido pelos dois
- [ ] **Design tokens** (Style Dictionary): JSON → CSS vars/SCSS (Angular) + `ThemeData` (Flutter)
- [ ] Error handling padronizado (ProblemDetails/RFC 7807 + handler global) refletido nos dois clientes
- [ ] CI: lint + test + geração de contrato em PR
- [ ] Observabilidade básica (logs estruturados, Actuator)

**Critério de pronto**: endpoint dummy visível na web **e** no app, tipado, com tema vindo dos tokens; CI verde.

---

## Fase 1a — Auth & Users

> Referência: [module_auth.md](module_auth.md) · **Por que primeiro**: tudo depende de identidade + visibilidade por dono; é o gate de todas as APIs.
> **Riscos atacados**: JWT/refresh nos dois clientes (interceptor Angular + Dio/Flutter).

**Specs sugeridas**:
- [ ] SPEC-001: Login/JWT/refresh + enforcement de roles no backend
- [ ] SPEC-002: Gestão de usuários pelo Admin (CRUD, desativação, reset de senha)
- [ ] SPEC-003: Telas de login web/app + sessão persistente no app

**Critério de pronto**: Admin cria membro; membro loga na web e no app; API nega acesso cruzado (membro não vê lead alheio — testado).

---

## Fase 1b — Catalogs & Leads & Pipeline (o coração)

> Referência: [module_settings.md](module_settings.md) + [module_leads.md](module_leads.md) · **Por que agora**: valor central; catálogos entram juntos porque o form de lead depende deles.
> **Riscos atacados**: modelo de Status Events (irreversível-caro), Kanban drag-and-drop nos dois clientes.

**Specs sugeridas**:
- [ ] SPEC-004: Catálogos (Course, Channel, StallReason) + settings de SLA
- [ ] SPEC-005: CRUD de lead + origem/UTM + dono + detecção de duplicado
- [ ] SPEC-006: Funil de status + Status Events imutáveis + STALLED (etapa+motivo) + reativação
- [ ] SPEC-007: Lista/filtros + Kanban (web e app) + detalhe do lead

**Critério de pronto**: fluxo completo LEAD→…→STUDENT e parada STALLED com motivo, tudo gerando eventos; Kanban funcional nos dois clientes.

---

## Fase 1c — Activities & Timeline

> Referência: [module_activities.md](module_activities.md) · **Por que agora**: enriquece o lead e produz `last_contacted_at`, pré-requisito do SLA.

**Specs sugeridas**:
- [ ] SPEC-008: Atividades (tipos, autor, janela de edição) + atualização de `last_contacted_at`
- [ ] SPEC-009: Timeline unificada (atividades + status + dono) + "Minhas tarefas" (follow-ups)

**Critério de pronto**: timeline completa no detalhe do lead; follow-up agendável e concluível nos dois clientes.

---

## Fase 1d — Dashboard, Analytics & Reports

> Referência: [module_dashboard.md](module_dashboard.md) · **Por que agora**: precisa de eventos reais das fases anteriores para ter o que mostrar; é a vitrine do produto.
> **Riscos atacados**: agregações de funil no Postgres; paridade visual ECharts × fl_chart.

**Specs sugeridas**:
- [ ] SPEC-010: Endpoints de analytics (funil, drop-off/motivos, séries, por canal/curso/país, leaderboard)
- [ ] SPEC-011: Dashboard web (ECharts: funil animado, mapa-múndi, séries, KPIs)
- [ ] SPEC-012: Dashboard app (Flutter) + export CSV/PDF (backend)

**Critério de pronto**: RNF de "mesmos números na web e no app" testado; dashboard < 2s.

---

## Fase 1e — Notifications & SLA

> Referência: [module_notifications.md](module_notifications.md) · **Por que por último no MVP**: consome tudo que veio antes (status, atividades, settings); push é integração externa (FCM/APNs) que não deve travar o resto.

**Specs sugeridas**:
- [ ] SPEC-013: Job de SLA + lembretes de follow-up + central in-app (anti-spam)
- [ ] SPEC-014: Push (FCM/APNs) no app + e-mail (digest)

**Critério de pronto**: HOT_LEAD sem contato dispara alerta 1x; contato zera o relógio; push chega no aparelho.

---

## Fase 2 — Captação pública

> Referência: module_leads.md#rf-09 · Formulário embutível → cria lead com Channel/UTM automáticos + proteção anti-bot.

**Specs sugeridas**:
- [ ] SPEC-015: Endpoint público + formulário embutível + atribuição de dono (fila/rodízio?)

---

## Futuro (fora do escopo atual)

WhatsApp (API oficial) · Gestão de documentos da aplicação · Sequências de e-mail (nutrição) · Perfil TEAM_LEAD.

---

## Marcos

| Marco | O que valida |
|-------|--------------|
| Fundação pronta | Contrato + tokens fluindo para os 2 clientes (aposta arquitetural validada) |
| Funil operável | Fases 1a–1b: equipe cadastra e move leads no dia a dia |
| MVP fechado | Fases 1a–1e completas: relatórios + SLA rodando |
| Beta com a equipe real | Uso diário; feedback de UX; números conferidos |

## Riscos transversais

| Risco | Prob. | Impacto | Mitigação |
|-------|-------|---------|-----------|
| Divergência visual/numérica web × app | média | alto | Tokens compartilhados + agregação só no backend + teste de paridade |
| Pipeline OpenAPI→Dart imaturo no fluxo do time | média | médio | Validar na Fase 0 com endpoint dummy antes de qualquer feature |
| Gráfico de mapa no Flutter (país) | média | médio | Spike na SPEC-012; fallback: ranking por país em barras |
| Push (FCM/APNs) atrasar MVP | baixa | médio | Fase 1e isolada no fim; in-app/e-mail entregam o valor mínimo |

## Decisões pendentes que travam o roadmap

- [ ] Confirmar **monorepo** (recomendado) — trava a Fase 0
- [ ] Janela de edição de Activity · seed de Channel/StallReason · e-mail digest vs imediato (não travam; decidir nas specs)

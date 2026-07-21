---
status: rascunho   # rascunho | aprovado
versao: 1
criado-em: 2026-07-21
atualizado-em: 2026-07-21
---

# Brief — ControlLeads

> Base de descoberta para o sistema de controle de leads de recrutamento internacional de estudantes.
> Foundational e leve — o detalhamento por módulo mora nos `module_<nome>.md`.

## 1. Visão de produto

ControlLeads é um sistema de **gestão de funil (CRM) para recrutamento internacional de estudantes**. A equipe de marketing cadastra e acompanha leads desde o primeiro contato até a conversão em aluno matriculado, registrando **quem trouxe o lead**, **por qual canal**, e **em que etapa cada um avança ou para**. O valor central é dar à instituição relatórios e gráficos claros e animados sobre conversão, gargalos do funil e desempenho da equipe — para decidir onde investir e quem/o que está performando. Todo o produto é em **inglês**.

## 2. Atores e perfis

- **Administrator**: acesso total. Vê e gerencia **todos** os leads, todos os relatórios, gerencia usuários da equipe, configura catálogos (cursos, canais) e regras (SLA). Exporta dados.
- **marketing_team**: cria e trabalha **os próprios leads** (dono = `assigned_to`). Vê e edita apenas os seus; enxerga o próprio desempenho no dashboard. **Não** administra o sistema.
- **Integrações externas** (fora do MVP): futuro formulário público de captação, WhatsApp, e-mail.

> Decisão estrutural: **visibilidade por dono** — cada membro do marketing só vê/edita os leads que são dele; o Administrator vê tudo. Isso sustenta as métricas de "quem converteu".

## 3. Arquitetura macro

- **Multi-tenant?** Não. **Single-tenant** — uma instituição. Modelo de dados simplificado (sem RLS/isolamento por tenant).
- **Quem cadastra quem?** O **Administrator** cadastra os usuários da equipe (convite/criação). Não há auto-cadastro público. Leads são cadastrados pela equipe (e, no futuro, por formulário público).
- **Stack**:
  - **Backend/API**: Spring Boot (Java) + PostgreSQL. Fonte única da lógica de negócio; clientes são "magros".
  - **Web**: Angular (signals) + ECharts para gráficos animados (funil, mapa por país, séries).
  - **App mobile**: Flutter (Android + iOS), consumindo as **mesmas** APIs.
- **Topologia**: três entregáveis (`backend` Spring, `web` Angular, `app` Flutter) coordenados por dois artefatos compartilhados (ver Reaproveitamento).

### Estratégia de reaproveitamento (Angular ⇄ Flutter)

Angular é TypeScript e Flutter é Dart — **não há como compartilhar código de UI** entre eles. O reaproveitamento real acontece em três eixos, e é isso que mantém web e app consistentes:

1. **Contrato de API como fonte única** — o backend expõe **OpenAPI** (springdoc). A partir do mesmo `openapi.yaml` geram-se **clientes tipados** para Angular (TS) e Flutter (Dart). Contrato muda num lugar só; os dois clientes acompanham. Evita divergência entre web e app.
2. **Design tokens compartilhados** — cores, tipografia, espaçamentos e raios definidos em **uma fonte de tokens** (ex.: JSON via Style Dictionary) e gerados para os dois lados: variáveis SCSS/CSS no Angular e `ThemeData` no Flutter. Garante a mesma identidade visual bonita nos dois clientes com uma fonte só.
3. **Lógica no servidor** — regras de negócio (transições de status válidas, cálculo de funil, SLA) vivem no Spring Boot. Web e app apenas apresentam. Menos duplicação, menos bug de "regra diferente em cada lugar".

> Recomendação de repositório: **monorepo** com `/backend`, `/web`, `/app` e `/shared` (contrato OpenAPI + design tokens), para que os artefatos compartilhados tenham um lar único. Polyrepo funciona, mas exige versionar contrato/tokens como pacotes. **[Ponto em aberto — confirmar na Fase 0.]**

## 4. Fases de entrega

- **Fase 0 (Fundação)**: monorepo/estrutura, CI, PostgreSQL, scaffold Spring Boot, OpenAPI + geração de clientes (TS/Dart), design tokens compartilhados, error handling padronizado, observabilidade, scaffold de auth.
- **Fase 1 (MVP)**:
  - **Auth & Users** (login, JWT, papéis, visibilidade por dono)
  - **Leads & Pipeline** (CRUD, funil de status, **histórico de transições**, origem+UTM, dono)
  - **Activities & Timeline** (notas e follow-ups por lead)
  - **Notifications & SLA** (alerta de lead HOT sem contato há X horas)
  - **Dashboard, Analytics & Reports** (funil, conversão, drop-off por etapa, leaderboard, mapa por país, por canal; export CSV/PDF)
  - **Catalogs & Configuration** (cursos, canais, config de SLA)
- **Fase 2**: **Captação pública** — formulário embutível no site → cria lead com origem/UTM automáticos.
- **Futuro** (fora do escopo atual): **WhatsApp**, **gestão de documentos** da aplicação, **sequências de e-mail** (nutrição).

## 5. Lista de módulos funcionais

> Detalhados um a um na Fase 2 (um `module_<nome>.md` por módulo).

1. **Auth & Users** — `module_auth.md`
2. **Leads & Pipeline** — `module_leads.md`
3. **Activities & Timeline** — `module_activities.md`
4. **Notifications & SLA** — `module_notifications.md`
5. **Dashboard, Analytics & Reports** — `module_dashboard.md`
6. **Catalogs & Configuration** — `module_settings.md`

## 6. Restrições conhecidas

- **Idioma**: todo o produto (UI, enums, mensagens) em **inglês**.
- **Privacidade**: o sistema guarda dados pessoais de leads (nome, e-mail, telefone, país). Log de auditoria em mudanças de status/dono; base para futura conformidade (LGPD/GDPR) mesmo sem exigência explícita hoje.
- **Design**: requisito explícito de **UI bonita e polida**, consistente entre web e app (sustentado pelos design tokens).
- **Relatórios**: gráficos **animados e claros** são requisito de produto, não enfeite — dimensionam a escolha por ECharts (web) e por libs de chart do Flutter (app).
- **Integrações obrigatórias**: nenhuma no MVP. Futuras: WhatsApp (API oficial), provedor de e-mail, storage de documentos.

## 7. Decisões estruturais salvas em memory

- [x] `project_overview.md` — visão de produto + atores + visibilidade por dono
- [x] `project_phases.md` — fases de entrega e o que cabe em cada
- [x] `project_stack.md` — Angular + Flutter + Spring Boot + Postgres + estratégia de reaproveitamento

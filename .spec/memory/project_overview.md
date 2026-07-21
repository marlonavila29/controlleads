# Project Overview — ControlLeads

CRM/funil para **recrutamento internacional de estudantes** (single-tenant, uma instituição). Equipe de marketing cadastra e acompanha leads do primeiro contato até virar aluno matriculado, rastreando quem trouxe o lead, por qual canal, e em que etapa avança/para. Foco em relatórios e gráficos animados de conversão, gargalos e desempenho da equipe. Todo o produto em **inglês**.

## Atores
- **Administrator** — acesso total: vê/gerencia todos os leads e relatórios, gerencia usuários, configura catálogos e SLA, exporta dados.
- **marketing_team** — trabalha **os próprios leads** (dono = `assigned_to`); vê/edita só os seus; enxerga o próprio desempenho.

## Decisão estrutural
- **Visibilidade por dono**: cada membro vê só os seus leads; Admin vê tudo. Sustenta a métrica "quem converteu".
- **Single-tenant** (sem multi-tenant/RLS).

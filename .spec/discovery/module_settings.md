# Módulo — Catalogs & Configuration

## Visão Geral
Catálogos gerenciáveis pelo Admin que alimentam formulários e relatórios: cursos, canais de origem, motivos de parada e parâmetros de SLA. Fase 1 (MVP).

## Atores
- **Administrator**: CRUD dos catálogos e configurações.
- **marketing_team**: consome (dropdowns nos formulários).

## Conceitos-Chave
- **Catálogo**: lista gerenciável (nome + ativo/inativo) — nunca exclusão física, pois registros históricos referenciam.
- Catálogos do MVP: **Courses**, **Channels** (origens), **Stall Reasons** (motivos de parada).
- **Settings**: parâmetros do sistema (ex.: `hot_lead_max_hours`).

## Requisitos Funcionais
- **RF-01** (Fase 1): CRUD de **Courses** (nome, ativo). Desativar esconde de formulários; leads existentes mantêm a referência.
- **RF-02** (Fase 1): CRUD de **Channels** (nome, ativo) — mesma semântica.
- **RF-03** (Fase 1): CRUD de **Stall Reasons** (nome, ativo) — alimenta o STALLED e o relatório de motivos.
- **RF-04** (Fase 1): Configurar `hot_lead_max_hours` do SLA.
- **RF-05** (Fase 1): Países são lista fixa ISO 3166 (não gerenciável).

## Requisitos Não Funcionais
- **RNF-01 (Integridade)**: itens de catálogo referenciados jamais são apagados — apenas desativados.

## Regras de Negócio
- **RN-01**: Alteração de SLA vale a partir dali (não reprocessa alertas passados).
- **RN-02**: Renomear item de catálogo reflete em todos os registros (referência por id).

## Fluxos Principais
### Fluxo 1 — Novo curso ofertado
1. Admin cadastra "Data Science BSc".
2. Curso aparece nos formulários e nos filtros do dashboard.

## Integrações
- Nenhuma.

## Pontos em Aberto
- Seed inicial de canais (Instagram, Google Ads, Fair, Referral, Website, Other?) — definir na spec.

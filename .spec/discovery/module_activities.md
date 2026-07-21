# Módulo — Activities & Timeline

## Visão Geral
Histórico de relacionamento com o lead: notas e follow-ups agendados, exibidos numa timeline única junto com as mudanças de status. Alimenta o SLA (`last_contacted_at`). Fase 1 (MVP).

## Atores
- **marketing_team**: registra atividades nos próprios leads.
- **Administrator**: idem em qualquer lead.

## Conceitos-Chave
- **Activity**: registro datado no lead. Tipos: `NOTE`, `CALL`, `EMAIL`, `WHATSAPP`, `MEETING`, `FOLLOW_UP`.
- **Follow-up**: atividade com **data futura** (`due_at`) e estado aberto/concluído — vira lembrete.
- **Timeline**: fusão cronológica de atividades + status events + trocas de dono.
- **last_contacted_at**: derivado da última atividade de contato (CALL/EMAIL/WHATSAPP/MEETING) — insumo do SLA.

## Requisitos Funcionais
- **RF-01** (Fase 1): Adicionar atividade (tipo + texto) a um lead; autor e data automáticos.
- **RF-02** (Fase 1): Agendar follow-up com `due_at`; concluir ou reagendar depois.
- **RF-03** (Fase 1): Timeline do lead em ordem cronológica misturando atividades e mudanças de status.
- **RF-04** (Fase 1): "Minhas tarefas": lista de follow-ups abertos do usuário, ordenada por vencimento (home do app).
- **RF-05** (Fase 1): Editar/excluir a **própria** atividade em janela curta (correção de digitação); depois disso, imutável.

## Requisitos Não Funcionais
- **RNF-01 (UX)**: registrar atividade em ≤ 2 toques no app (uso em campo/feira).

## Regras de Negócio
- **RN-01**: Atividade de contato atualiza `last_contacted_at` do lead (zera o relógio do SLA).
- **RN-02**: Atividades pertencem ao lead: reatribuição de dono preserva todo o histórico.
- **RN-03**: Follow-up vencido e aberto entra nos lembretes (módulo Notifications).

## Fluxos Principais
### Fluxo 1 — Registro rápido de contato
1. Membro liga para o lead.
2. Abre o lead → "+ Call" → nota curta → salvar.
3. `last_contacted_at` atualizado; SLA satisfeito.

### Fluxo 2 — Follow-up
1. Lead diz "me procura mês que vem".
2. Membro agenda FOLLOW_UP p/ o dia 25.
3. No dia, lembrete dispara; membro liga e conclui a tarefa.

## Integrações
- Nenhuma no MVP (futuro: WhatsApp/e-mail geram atividades automáticas).

## Pontos em Aberto
- Janela de edição da atividade (15 min? 24h?).

# Módulo — Notifications & SLA

## Visão Geral
Motor de alertas para o lead não esfriar: SLA de contato para HOT_LEAD e lembretes de follow-up. Entrega in-app, push (Flutter) e e-mail. Fase 1 (MVP).

## Atores
- **marketing_team**: recebe alertas dos próprios leads.
- **Administrator**: configura SLA; visão agregada de violações (dashboard).

## Conceitos-Chave
- **SLA de contato**: tempo máximo que um HOT_LEAD pode ficar sem atividade de contato (`hot_lead_max_hours`, configurável).
- **Violação de SLA**: HOT_LEAD com `now - last_contacted_at > limite`.
- **Notification**: mensagem por usuário com tipo, payload (lead), lida/não lida.

## Requisitos Funcionais
- **RF-01** (Fase 1): Job periódico detecta HOT_LEADs além do SLA e notifica o dono.
- **RF-02** (Fase 1): Lembrete de follow-up vencendo/vencido para o autor.
- **RF-03** (Fase 1): Central de notificações in-app (web + app) com estado lido/não lido.
- **RF-04** (Fase 1): Push notification no app Flutter (FCM/APNs).
- **RF-05** (Fase 1): E-mail como canal secundário (digest ou imediato — ver Pontos em Aberto).
- **RF-06** (Fase 1): Admin enxerga violações de SLA agregadas (contagem por membro) no dashboard.

## Requisitos Não Funcionais
- **RNF-01 (Anti-spam)**: no máx. **1 alerta por lead por violação** (re-alerta só se novo ciclo, ex. a cada 24h) — alerta repetido vira ruído ignorado.
- **RNF-02 (Latência)**: detecção com granularidade de minutos (job a cada 5–15 min é suficiente; não precisa ser tempo real).

## Regras de Negócio
- **RN-01**: SLA só se aplica a `HOT_LEAD` (no MVP); STALLED/STUDENT nunca alertam.
- **RN-02**: Qualquer atividade de contato zera o relógio do SLA (ver module_activities.md#rn-01).
- **RN-03**: Notificação vai para o **dono** do lead; Admin vê o agregado, não recebe cada alerta.

## Fluxos Principais
### Fluxo 1 — Lead esfriando
1. Lead vira HOT_LEAD; ninguém contata por 24h (limite configurado).
2. Job detecta → push + in-app para o dono: "Maria (Vietnam, Nursing) uncontacted for 24h".
3. Dono liga, registra CALL → relógio zera.

## Integrações
- **FCM/APNs** (push) · provedor de e-mail transacional (mesmo do Auth).

## Pontos em Aberto
- E-mail imediato por alerta vs digest diário (recomendação: digest).
- Horário silencioso (não notificar de madrugada — leads em fusos variados, equipe local).

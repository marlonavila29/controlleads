# Módulo — Dashboard, Analytics & Reports

## Visão Geral
A vitrine do produto: gráficos **animados e claros** respondendo "quantos convertem, onde param, quem traz resultado, qual canal funciona". Tudo derivado dos **Status Events** + dimensões do lead. Fase 1 (MVP).

## Atores
- **Administrator**: visão global + leaderboard da equipe + violações de SLA.
- **marketing_team**: os mesmos gráficos, filtrados para **os próprios leads**.

## Conceitos-Chave
- **Conversion rate**: % de leads que chegam a STUDENT (global e etapa-a-etapa).
- **Drop-off por etapa**: em qual etapa os STALLED pararam (+ motivos).
- **Time-in-stage / time-to-convert**: tempo médio em cada etapa e do cadastro até STUDENT.
- **Leaderboard**: ranking da equipe por conversões e leads trabalhados (Admin).

## Requisitos Funcionais
- **RF-01** (Fase 1): KPIs no topo: total de leads, novos no período, conversion rate, tempo médio até STUDENT, HOT_LEADs em violação de SLA.
- **RF-02** (Fase 1): **Funil animado**: contagem por etapa + % de passagem etapa-a-etapa, com destaque do maior gargalo.
- **RF-03** (Fase 1): **Drop-off**: onde os STALLED pararam, com quebra por **motivo** (ex.: preço) — pergunta central do negócio.
- **RF-04** (Fase 1): **Mapa-múndi** por país de origem (volume e conversão por país).
- **RF-05** (Fase 1): Séries temporais: leads criados × convertidos por semana/mês.
- **RF-06** (Fase 1): Desempenho **por canal/UTM**: volume e conversão por origem.
- **RF-07** (Fase 1): Desempenho **por curso**: cursos que mais atraem e mais convertem.
- **RF-08** (Fase 1): **Leaderboard** da equipe (Admin): conversões, leads ativos, tempo médio de resposta.
- **RF-09** (Fase 1): Filtro global de **período** (+ curso/canal/país) aplicado a todos os gráficos.
- **RF-10** (Fase 1): Export **CSV** (dados) e **PDF** (relatório visual).

## Requisitos Não Funcionais
- **RNF-01 (UX/Design)**: gráficos com transições animadas (ECharts na web; equivalente no Flutter), tema consistente via design tokens; requisito de produto, não enfeite.
- **RNF-02 (Performance)**: dashboard carrega < 2s com dezenas de milhares de leads (agregações no Postgres; cache curto se preciso).
- **RNF-03 (Consistência)**: web e app mostram os **mesmos números** — agregações calculadas só no backend, nunca no cliente.

## Regras de Negócio
- **RN-01**: Métricas de funil derivam **exclusivamente** dos Status Events (nunca do status atual).
- **RN-02**: Conversão credita o **dono no momento da transição** para STUDENT.
- **RN-03**: `marketing_team` vê tudo filtrado por si; leaderboard e agregado de SLA são exclusivos do Admin.

## Fluxos Principais
### Fluxo 1 — Reunião mensal
1. Admin abre dashboard, filtra o mês.
2. Funil mostra gargalo em APPLICATION; drop-off mostra "price" como motivo nº 1.
3. Exporta PDF e leva a discussão de pricing para a direção.

## Integrações
- Nenhuma externa. Consome agregações do backend (endpoints de analytics dedicados).

## Pontos em Aberto
- Lib de gráficos no Flutter (fl_chart vs graphic vs ECharts via webview — decidir na spec; recomendação: **fl_chart** + mapa dedicado).
- PDF: gerado no backend (consistente) vs no cliente. Recomendação: backend.

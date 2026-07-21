# Módulo — Leads & Pipeline

## Visão Geral
O coração do sistema: cadastro de leads e o funil de status com **histórico de transições** — a fonte de todos os relatórios de conversão e drop-off. Fase 1 (MVP).

## Atores
- **marketing_team**: cria e trabalha os próprios leads; move no funil.
- **Administrator**: tudo acima, sobre todos os leads; reatribui dono.

## Conceitos-Chave
- **Status (funil)**: `LEAD → HOT_LEAD → APPLICATION → STUDENT` + `STALLED` (parou).
- **STALLED**: estado único de parada; sempre registra **a etapa em que parou** e o **motivo**. Lead STALLED pode ser reativado (volta à etapa em que estava).
- **Status Event**: registro imutável de cada transição (`de`, `para`, `quem`, `quando`, `motivo`).
- **Source/UTM**: canal de origem (catálogo) + utm_source/utm_medium/utm_campaign.

## Requisitos Funcionais
- **RF-01** (Fase 1): Criar lead com **nome, país de origem, e-mail, telefone, curso de interesse** + canal de origem (+UTM opcional). Dono default = quem criou; Admin pode atribuir a outro.
- **RF-02** (Fase 1): Editar dados do lead; Admin pode **reatribuir dono** (fica registrado).
- **RF-03** (Fase 1): Mudar status seguindo o funil; toda mudança gera **Status Event** imutável.
- **RF-04** (Fase 1): Marcar como STALLED exige **motivo**; sistema grava automaticamente a etapa em que parou. Reativar devolve à etapa anterior.
- **RF-05** (Fase 1): Lista com busca e filtros combináveis: status, país, curso, canal, dono (Admin), período.
- **RF-06** (Fase 1): **Visão Kanban** do funil com drag-and-drop entre colunas (web e app).
- **RF-07** (Fase 1): Detecção de duplicado no cadastro (mesmo e-mail ou telefone) com aviso e link para o existente.
- **RF-08** (Fase 1): Página de detalhe do lead: dados, status atual, dono, origem, timeline (módulo Activities) e histórico de status.
- **RF-09** (Fase 2): Criação de lead via formulário público (captação) com origem/UTM automáticos.

## Requisitos Não Funcionais
- **RNF-01 (Integridade)**: Status Events são **imutáveis** (nunca editados/apagados) — são a fonte dos relatórios.
- **RNF-02 (Performance)**: lista/kanban paginados; filtros indexados.

## Regras de Negócio
- **RN-01**: Transições válidas: avanço sequencial (`LEAD→HOT_LEAD→APPLICATION→STUDENT`), qualquer etapa → `STALLED`, `STALLED` → etapa em que estava. Retroceder etapa é permitido ao Admin (com registro).
- **RN-02**: `STUDENT` é terminal (convertido). Sair de STUDENT só via Admin (correção de erro), com registro.
- **RN-03**: Todo lead tem exatamente **um dono**; troca de dono gera registro de auditoria.
- **RN-04**: Duplicado (e-mail/telefone já existente) **avisa mas não bloqueia** — decisão fica com o usuário.

## Fluxos Principais
### Fluxo 1 — Vida feliz do lead
1. Membro cadastra lead (feira, Instagram…).
2. Contato feito → move p/ HOT_LEAD.
3. Lead inicia aplicação → APPLICATION.
4. Matrícula concluída → STUDENT. ✅ conversão contabilizada p/ o dono.

### Fluxo 2 — Lead que para
1. Lead em APPLICATION some/desiste.
2. Membro marca STALLED + motivo ("price", "visa denied", "chose another school"…).
3. Relatórios mostram: parou na etapa APPLICATION, motivo X.
4. (Opcional) Lead responde meses depois → reativado, volta a APPLICATION.

## Integrações
- Nenhuma no MVP. Fase 2: endpoint público de captação.

## Pontos em Aberto
- Lista fixa de motivos de STALLED (catálogo gerenciável pelo Admin) vs texto livre. Recomendação: **catálogo + campo opcional de detalhe** (senão relatório de motivos vira texto solto).
- País: ISO 3166 (lista padrão) — assumido.

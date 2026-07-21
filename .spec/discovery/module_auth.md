# Módulo — Auth & Users

## Visão Geral
Autenticação e gestão de usuários internos. Sustenta a regra central de visibilidade: cada membro do marketing trabalha os próprios leads; o Administrator vê tudo. Fase 1 (MVP).

## Atores
- **Administrator**: gerencia usuários (criar, editar, desativar), acessa tudo.
- **marketing_team**: faz login, acessa os próprios leads e o próprio desempenho.

## Conceitos-Chave
- **Role**: `ADMINISTRATOR` | `MARKETING_TEAM` (fixas no código; porta aberta p/ futura `TEAM_LEAD`).
- **Owner (`assigned_to`)**: usuário dono do lead — base da visibilidade e das métricas por pessoa.

## Requisitos Funcionais
- **RF-01** (Fase 1): Login com e-mail/senha retornando JWT (access + refresh token).
- **RF-02** (Fase 1): Admin cria usuário com nome, e-mail, role; sistema envia definição de senha por e-mail (ou senha temporária — ver Pontos em Aberto).
- **RF-03** (Fase 1): Admin edita e **desativa** usuários (nunca exclui — leads históricos apontam para eles).
- **RF-04** (Fase 1): Recuperação de senha por e-mail (token de uso único com expiração).
- **RF-05** (Fase 1): Perfil próprio: trocar senha e nome de exibição.
- **RF-06** (Fase 1): Autorização por role em toda a API (enforcement no backend, nunca só na UI).

## Requisitos Não Funcionais
- **RNF-01 (Segurança)**: senhas com hash forte (bcrypt/argon2); rate-limit no login; refresh token rotativo.
- **RNF-02 (Segurança)**: usuário desativado tem sessões invalidadas e não autentica mais.

## Regras de Negócio
- **RN-01**: `MARKETING_TEAM` só lê/edita leads onde `assigned_to = ele`; `ADMINISTRATOR` lê/edita todos.
- **RN-02**: Desativar usuário não órfã leads: Admin é alertado dos leads ativos do usuário e pode reatribuí-los.
- **RN-03**: Não há auto-cadastro; toda conta nasce pelo Admin.

## Fluxos Principais
### Fluxo 1 — Onboarding de membro da equipe
1. Admin cadastra nome, e-mail, role.
2. Membro recebe e-mail, define senha, faz login.

### Fluxo 2 — Saída de membro
1. Admin desativa a conta.
2. Sistema lista leads ativos do membro → Admin reatribui (em lote ou um a um).

## Integrações
- Provedor de e-mail transacional (definição/recuperação de senha) — o mesmo que servirá notificações.

## Pontos em Aberto
- Convite por e-mail vs senha temporária no primeiro acesso.
- Expiração de sessão no app Flutter (refresh silencioso? biometria no futuro?).

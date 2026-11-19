# Boletim Plus — API Java

Global Solution 2026/2 · CitizenTech · Disciplina: Java Advanced

| Nome                      | RM     |
| ------------------------- | ------ |
| Gustavo Barrios de Araújo | 563358 |
| Phietro Solon Oliveira    | 563842 |

## O problema e a solução

O responsável costuma saber da nota baixa ou das faltas do filho só no fim do bimestre. O
**Boletim Plus** é um boletim digital: o professor lança notas e chamada, e o responsável acompanha
no app. Esta API Java cuida da conta, do boletim e da **análise do boletim com IA**:
o LLM lê as notas e a frequência e escreve uma orientação curta para a família. Cada matéria em
atenção (média < 60 ou frequência < 75%) vira um evento no RabbitMQ, e o consumidor decide quais
responsáveis avisar.

## Tecnologias

| Requisito do PDF            | Como foi feito                                                                 |
| --------------------------- | ------------------------------------------------------------------------------ |
| 1. API REST do domínio      | Spring Boot 4.1, Spring Data JPA, Oracle. CRUD paginado de responsáveis e vínculos |
| 2. Validação e erros        | Bean Validation nos DTOs; `@RestControllerAdvice` com corpo de erro padrão     |
| 3. Segurança                | Spring Security + JWT (HS256); papéis DIRETOR, PROFESSOR, GUARDIAO, ALUNO      |
| 4. Spring AI                | `ChatClient` + Ollama: análise do boletim dentro do fluxo do responsável       |
| 5. Arquitetura distribuída  | **Opção A: RabbitMQ.** Alerta de desempenho publicado e consumido de forma assíncrona |
| 6. Documentação             | Swagger em `/swagger-ui.html`; este README; app mobile consome a API           |

## Como rodar

Pré-requisitos: Docker com Docker Compose. Para rodar fora do Docker: Java 21+.

1. Copie o arquivo de variáveis: `cp .env.example .env`.
2. Troque `JWT_SECRET` no `.env` por uma chave com 32 ou mais caracteres. Use a mesma chave na API .NET.
3. Suba tudo: `docker compose up -d --build`.
4. Espere a API ficar pronta: `docker compose logs -f api` até aparecer `Started BoletimPlusApplication`.
5. Abra o Swagger: <http://localhost:8080/swagger-ui.html>.

Na primeira subida, o Oracle cria as tabelas (2 a 4 min) e o Ollama baixa o modelo
`qwen2.5:1.5b` (cerca de 1 GB). A API só responde depois disso.

O Compose sobe 4 containers:

| Serviço    | Porta         | O que é                                                          |
| ---------- | ------------- | ---------------------------------------------------------------- |
| `api`      | 8080          | Esta API                                                         |
| `oracle`   | 1521          | Oracle Free 23ai, usuário `boletim`, com o DDL e a DML do projeto |
| `rabbitmq` | 5672 / 15672  | Fila de alertas; painel em <http://localhost:15672> (guest/guest) |
| `ollama`   | 11434         | LLM local                                                        |

### Rodar a API fora do Docker

1. Suba só a infraestrutura: `docker compose up -d oracle rabbitmq ollama`.
2. Rode a API: `JWT_SECRET=<sua-chave> ./gradlew bootRun`.

Sem Docker nenhum, a API precisa só de um Oracle com o DDL e a DML do projeto (por exemplo, o da FIAP):

```bash
DB_URL=jdbc:oracle:thin:@//oracle.fiap.com.br:1521/ORCL DB_USER=<rm> DB_PASSWORD=<senha> \
OLLAMA_PULL=never JWT_SECRET=<sua-chave> ./gradlew bootRun
```

| Fora do ar | O que acontece                                                                 |
| ---------- | ------------------------------------------------------------------------------ |
| RabbitMQ   | A API funciona. Os alertas não são enviados, e o log mostra erros de conexão.   |
| Ollama     | A API funciona. `POST /alunos/{cod}/boletim/analise` devolve 503 em cerca de 2 s. |

Nos dois casos, `/actuator/health` responde 503, porque mostra o serviço que caiu.

### Testes

```bash
./gradlew test
```

Os testes de integração sobem a API inteira com H2 em modo Oracle, carregado com os mesmos scripts
de `docker/oracle`. O LLM e o RabbitMQ são simulados com Mockito.

### Variáveis de ambiente

| Variável           | Padrão                                       | Uso                                         |
| ------------------ | -------------------------------------------- | ------------------------------------------- |
| `JWT_SECRET`       | obrigatória, sem padrão                      | Chave HS256, 32+ caracteres, igual à do .NET |
| `JWT_ISSUER`       | `boletim-plus`                               | Claim `iss` do token                         |
| `DB_URL`           | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` | Banco Oracle                                 |
| `DB_USER`          | `boletim`                                    | Usuário do banco                             |
| `DB_PASSWORD`      | `boletim`                                    | Senha do banco                               |
| `RABBITMQ_HOST`    | `localhost`                                  | Host do RabbitMQ                             |
| `OLLAMA_BASE_URL`  | `http://localhost:11434`                     | Endereço do Ollama                           |
| `OLLAMA_MODEL`     | `qwen2.5:1.5b`                               | Modelo do Ollama                             |
| `PORT`             | `8080`                                       | Porta HTTP                                   |

## Usuários de teste

Os dados vêm da `02_dml.sql` do repositório de banco. O script `docker/oracle/03_senhas_demo.sql`
grava a senha `123456` nos usuários da semente. Ele só existe para o ambiente local.

| CPF         | Pessoa          | Papel     | O que testar                              |
| ----------- | --------------- | --------- | ----------------------------------------- |
| 12345678900 | Seymour Skinner | Diretor   | CRUD de responsáveis e vínculos           |
| 12345678901 | Edna Krabappel  | Professor | Boletim dos alunos das turmas dela        |
| 12345678903 | Marge Simpson   | Guardiã   | Dependentes, boletim e análise do Bart    |
| 12345678904 | Bart Simpson    | Aluno     | O próprio boletim                         |
| 12345678902 | Homer Simpson   | Bloqueado | Login recusado com 403                    |

## Endpoints

Toda rota, menos `/auth/login` e `/auth/cadastro`, exige `Authorization: Bearer <jwt>`.

| Método | Rota                           | Papel                        | Resposta                       |
| ------ | ------------------------------ | ---------------------------- | ------------------------------ |
| POST   | `/auth/login`                  | Público                      | `Sessao` (token, pessoa, papéis) |
| POST   | `/auth/cadastro`               | Público                      | 201 `Sessao` (novo responsável) |
| PUT    | `/auth/senha`                  | Logado                       | 204                            |
| PUT    | `/auth/nome`                   | Diretor, guardião            | `Pessoa`                       |
| POST   | `/auth/logout`                 | Logado                       | 204                            |
| GET    | `/guardioes/{cod}/dependentes` | O próprio guardião           | `Dependente[]`                 |
| GET    | `/alunos/{cod}/boletim`        | Aluno, guardião, professor, diretor | `BoletimAluno`          |
| POST   | `/alunos/{cod}/boletim/analise`| Aluno, guardião, professor, diretor | `AnaliseBoletim` (IA)   |
| GET    | `/guardioes?nome=&page=&size=&sort=` | Diretor                | Página de responsáveis         |
| GET    | `/guardioes/{cod}`             | Diretor                      | Responsável                    |
| POST   | `/guardioes`                   | Diretor                      | 201 Responsável                |
| PUT    | `/guardioes/{cod}`             | Diretor                      | Responsável (nome alterado)    |
| DELETE | `/guardioes/{cod}`             | Diretor                      | 204 (409 com vínculos)         |
| GET    | `/vinculos?guardiaoCod=&alunoCod=&page=` | Diretor            | Página de vínculos             |
| GET    | `/vinculos/{cod}`              | Diretor                      | Vínculo                        |
| POST   | `/vinculos`                    | Diretor                      | 201 Vínculo (409 se repetido)  |
| PUT    | `/vinculos/{cod}`              | Diretor                      | Vínculo (parentesco alterado)  |
| DELETE | `/vinculos/{cod}`              | Diretor                      | 204                            |
| GET    | `/tipos/presenca`, `/tipos/nota`, `/tipos/curso` | Logado     | `Enumerador[]`                 |
| POST   | `/tipos/curso`                 | Diretor                      | 201 `Enumerador` (409 se repetido) |
| GET    | `/professores`                 | Diretor                      | `ProfessorResumo[]`            |
| POST   | `/professores` + `Idempotency-Key` | Diretor                  | 201 `{ professorCod }`         |
| PUT    | `/professores/{cod}`           | Diretor                      | 204                            |
| GET    | `/professores/{cod}/turmas`    | O próprio professor          | `TurmaResumo[]`                |
| GET    | `/alunos`                      | Diretor                      | `AlunoResumo[]`                |
| POST   | `/alunos` + `Idempotency-Key`  | Diretor                      | 201 `{ alunoCod }`             |
| PUT    | `/alunos/{cod}`                | Diretor                      | 204                            |
| GET    | `/diretores/{cod}/escolas`     | O próprio diretor            | `EscolaResumo[]`               |
| GET    | `/escolas/{cod}`               | Diretor da escola            | `EscolaResumo`                 |
| GET    | `/escolas/{cod}/turmas`        | Diretor da escola            | `TurmaResumo[]`                |
| POST   | `/escolas/{cod}/turmas` + `Idempotency-Key` | Diretor da escola | 201 `{ cod }`              |
| GET    | `/escolas/{cod}/professores`   | Diretor da escola            | `ProfessorDaEscola[]`          |
| GET    | `/escolas/{cod}/alunos`        | Diretor da escola            | `AlunoDaEscola[]`              |
| GET    | `/turmas/{cod}`                | Professor ou diretor da turma | `TurmaDetalhe`                |
| POST   | `/turmas/{cod}/alunos`         | Diretor da escola            | 204 (matrícula)                |
| POST   | `/turmas/{cod}/avaliacoes` + `Idempotency-Key` | Professor da turma | 201 `{ cod }`          |
| GET    | `/avaliacoes/{cod}`            | Professor ou diretor da turma | `AvaliacaoDetalhe`            |
| PUT    | `/avaliacoes/{cod}`            | Professor da turma           | 204                            |
| DELETE | `/avaliacoes/{cod}`            | Professor da turma           | 204                            |
| PUT    | `/avaliacoes/{cod}/notas`      | Professor da turma           | 204                            |
| GET    | `/turmas/{cod}/chamada?data=YYYY-MM-DD` | Professor ou diretor da turma | `ItemChamada[]`   |
| PUT    | `/turmas/{cod}/chamada?data=YYYY-MM-DD` | Professor da turma  | 204                            |
| GET    | `/tipos/parentesco`            | Logado                       | `Enumerador[]`                 |
| GET    | `/actuator/health`             | Público                      | Saúde da API, banco e RabbitMQ |

Paginação: `page` começa em 0, `size` padrão 10, `sort` no formato `pessoa.nome,asc`. A resposta traz
`conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

Erro: todo erro devolve o mesmo corpo. O app mostra `mensagem`.

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Informe nome e sobrenome.",
  "caminho": "/auth/cadastro",
  "timestamp": "2026-11-18T23:00:00Z",
  "campos": [{ "campo": "nome", "mensagem": "Informe nome e sobrenome." }]
}
```

## Modelagem

A API usa o DDL do repositório de banco sem mudanças. As entidades JPA mapeiam as tabelas:

```
Pessoa 1─1 Usuario ─N─1 StatusUsuario
Pessoa 1─1 Diretor | Professor | Guardiao | Aluno        (papéis)
Guardiao 1─N AlunoGuardiao N─1 Aluno                       (vínculo, com TipoParentesco)
Escola N─1 Diretor;  Turma N─1 Escola, TipoCurso, Professor
TurmaAluno (matrícula) N─1 Turma, Aluno
Boletim (nota) N─1 TurmaAluno, Avaliacao;  ListaPresenca N─1 TurmaAluno, TipoPresenca
```

O CRUD cobre 3 entidades relacionadas: `Guardiao` (com a `Pessoa`), `AlunoGuardiao` (o vínculo)
e `Aluno` (lido no vínculo e no boletim). O vínculo é o que libera o boletim do aluno para o responsável.

## Autenticação JWT

1. O app chama `POST /auth/login` com CPF e senha.
2. O `AuthService` compara a senha com `Usuario.senha_hash` (bcrypt). Só usuário `Ativo` entra.
3. O `JwtService` assina o token com HS256: `sub` = `Usuario.cod`, `iss`, `iat` e `exp` (8 h).
4. O app manda `Authorization: Bearer <token>` em toda rota.
5. O `JwtAuthenticationFilter` valida assinatura, emissor e validade, e lê os papéis no banco pelo
   `sub`. O token não carrega papel: um usuário bloqueado perde o acesso na hora.
6. `@PreAuthorize("hasRole('DIRETOR')")` protege as rotas por papel. As regras por dado (ex.: o
   responsável só vê o próprio dependente) ficam no `AcessoService`.

Sem token ou com token inválido: 401. Com token válido sem o papel: 403.

## Funcionalidade com Spring AI

`POST /alunos/{cod}/boletim/analise`, no card "Análise com IA" do boletim no app:

1. O `BoletimService` monta o boletim do banco: notas, médias e frequência por matéria.
2. O `AnaliseBoletimService` publica um alerta no RabbitMQ para cada matéria em atenção.
3. O boletim vira texto (só o primeiro nome do aluno, sem CPF) e vai ao LLM pelo `ChatClient`.
4. O prompt de sistema (`AiConfig`) define o papel de orientador pedagógico, proíbe inventar
   dados e limita a resposta a 6 linhas.
5. A API devolve `{ texto, geradoEm }`. Se o Ollama falhar, responde 503 com uma mensagem clara.

O modelo padrão é o `qwen2.5:1.5b`: responde em 2 a 8 s em CPU, dentro do limite de 15 s do app.
Por ser pequeno, às vezes erra um detalhe do texto. Para um texto melhor, troque `OLLAMA_MODEL`
por um modelo maior (ex.: `qwen2.5:7b`) numa máquina com GPU. O modelo fica carregado por 30 min
(`keep-alive`), então só a primeira análise depois disso é mais lenta.

## Arquitetura distribuída: opção A (RabbitMQ)

```
POST /analise ─► AnaliseBoletimService ─► exchange boletim.eventos ─► fila boletim.alertas ─► AlertaConsumer
                    (publica e segue)        (chave alerta.desempenho)                        (avisa responsáveis)
```

- **Produtor:** `AlertaPublisher` publica um `AlertaDesempenhoEvent` em JSON por matéria em atenção.
  Se o RabbitMQ cair, a análise continua e o alerta vai para o log.
- **Consumidor:** `AlertaConsumer` (`@RabbitListener`) busca os vínculos do aluno e registra um
  aviso para cada responsável. Um canal real (push, e-mail, WhatsApp) entraria nesse ponto.
- **Onde ver:** `docker compose logs -f api` mostra `Alerta publicado` e, logo depois,
  `Aviso para Homer Simpson (Pai)`. O painel do RabbitMQ mostra a fila `boletim.alertas`.

## Uso de IA generativa

| Ferramenta | Para quê                                  |
| ---------- | ----------------------------------------- |
| Claude     | Apoio na geração do código e da documentação |
| Ollama     | LLM da funcionalidade de análise do boletim |

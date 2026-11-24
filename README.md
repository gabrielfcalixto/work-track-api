# Work Track · API

Aplicação para acompanhar clientes, projetos, tarefas e horas trabalhadas. A API usa Java 21, Spring Boot, Spring Security e Spring Data JPA. A interface Angular fica no repositório `work-track-angular`.

## Demonstração local

Com o JDK 21 ativo, execute:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run
```

Em macOS ou Linux, use `sh ./mvnw -Dspring-boot.run.profiles=demo spring-boot:run`. O perfil demo usa um banco H2 em memória que é recriado ao iniciar a API. Ele disponibiliza a API em `127.0.0.1:8080` e inclui dados fictícios:

| Login | Perfil |
| --- | --- |
| `admin` | Administrador |
| `gestor` | Gestor |
| `dev` | Colaborador |

A senha das três contas de demonstração é `WorktrackDemo!`. No frontend, execute `npm ci` e `npm start` e abra [localhost:4200](http://localhost:4200).

## Banco MySQL

Crie um banco MySQL e inicie a API sem o perfil `demo`. A configuração é lida das variáveis de ambiente:

| Variável | Uso |
| --- | --- |
| `DB_URL` | URL JDBC. O padrão local é `jdbc:mysql://localhost:3306/projeto`. |
| `DB_USERNAME` e `DB_PASSWORD` | Credenciais do banco. |
| `DB_DDL_AUTO` | Estratégia de esquema do Hibernate. O padrão é `update`. |
| `JWT_SECRET` | Chave HS512 com pelo menos 64 bytes aleatórios codificados em Base64. |
| `JWT_EXPIRATION_MS` | Duração do token em milissegundos. O padrão é 9.000.000. |
| `ADMIN_LOGIN`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` | Configuram a criação da conta inicial se o login ainda não existir. `ADMIN_LOGIN` tem padrão `admin`. |
| `FRONTEND_ORIGINS` | Origens permitidas para acesso direto à API, separadas por vírgula. |
| `SMTP_ENABLED`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` e `SMTP_PASSWORD` | Envio de senha inicial e recuperação por e-mail. Sem SMTP, o administrador define a senha inicial no cadastro. |

Para gerar a chave no PowerShell:

```powershell
$jwtBytes = New-Object byte[] 64
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtBytes) } finally { $jwtRandom.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

As instalações existentes conservam os registros do MySQL e as contas já cadastradas. Se o banco recebeu usuários iniciais do código antigo, revise essas contas e troque suas senhas.

## API e testes

- [Swagger UI](http://127.0.0.1:8080/swagger-ui/index.html) descreve os endpoints. Autentique em `POST /auth/login` com `login` e `password` e envie o token como `Authorization: Bearer <token>`.
- `GET /dashboard`, `GET /task/me`, `GET /time-entries/me` e `GET /profile/me` retornam dados da sessão autenticada.
- O painel inclui horas diárias de segunda a domingo. `PUT /dashboard/weekly-goal` salva a meta da conta em horas, com padrão de 40 h enquanto estiver sem configuração.
- Os lançamentos validam duração e sobreposição. Edições e exclusões atualizam o total da tarefa. Contas com registros vinculados e projetos com tarefas recebem um erro de conflito ao serem excluídos.

```powershell
.\mvnw.cmd clean verify
```

O comando executa os testes de integração no H2 e gera `target/project-manager-0.0.1-SNAPSHOT.jar`.

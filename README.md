# Cadastro de Eventos — Centro Cultural

API REST em Java para cadastrar os eventos de um centro cultural, construída com **Spring Boot 3** (Java 21), com arquitetura em camadas, injeção de dependências por construtor e dados em memória.

## Integrantes

| Nome |
|------|
| Cinthia Emanuelly |
| Daniel Dias |
| Ires Vitória |
| Jady Wéllyda |
| João Vitor Lemos |

## Migração: Mini Spring → Spring Boot

Este projeto foi migrado do **Mini Spring** (framework próprio feito pelo grupo, sobre Jetty) para o **Spring Boot**, mantendo o tema, as funcionalidades, as rotas e as regras de negócio. A versão anterior continua disponível na branch [`mini-spring`](../../tree/mini-spring) para comparação.

### Principais mudanças

| Aspecto | Antes (Mini Spring) | Agora (Spring Boot) |
|---------|---------------------|---------------------|
| Build / dependências | `pom.xml` com Jetty, Jackson, slf4j-simple e JUnit declarados um a um | `spring-boot-starter-parent` + starters `web`, `validation` e `test` |
| Inicialização | `MiniApplication.run(...)` | `@SpringBootApplication` + `SpringApplication.run(...)` |
| Servidor HTTP | Jetty configurado manualmente | Tomcat embutido, configurado por `application.properties` |
| Container de DI | `ApplicationContext` e `ComponentScanner` próprios | Container do Spring (varredura de `@RestController`, `@Service`, `@Repository`) |
| Anotações | `@Route`, `@Component`, `@RequestBody`, `@RequestParam`, `@PathVariable`, `@Value` próprias | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` e as anotações equivalentes do Spring |
| Roteamento e JSON | `DispatcherServlet` próprio e conversão manual com Jackson | `DispatcherServlet` do Spring MVC e conversão automática de JSON |
| Validação | `Validator` próprio com `@NotBlank` e `@Positive` | Bean Validation (`jakarta.validation`) com `@Valid` nos controllers |
| Respostas HTTP | `HttpResult` | `ResponseEntity` (201 com `Location`, 204 sem corpo) |
| Tratamento de erros | Tratamento centralizado dentro do framework próprio | `@RestControllerAdvice` (`GlobalExceptionHandler`), mantendo o mesmo formato de erro |
| Log e `X-Request-Id` | `LoggingInterceptor` / `RequestInterceptor` | `RequestIdFilter` (`OncePerRequestFilter`) |
| Testes | Servidor Mini Spring iniciado no teste | `@SpringBootTest` com porta aleatória |

### O que não mudou

- Rotas, códigos de status e formato das respostas de erro (`timestamp`, `status`, `message`, `path`, `details`, `requestId`).
- Regra de negócio: no máximo **5.000 participantes** por evento (422 quando excedida).
- Camadas Controller → Service → Repository (interface), com injeção por **construtor**.
- Armazenamento em memória (`ConcurrentHashMap` + `AtomicLong`).
- Os pacotes (`br.com.minispring.exemplo...`) foram mantidos para facilitar a comparação entre as duas versões.

## Domínio

O tema sorteado foi **Cadastro de eventos**. Cada evento do centro cultural (festival, mostra de teatro, sarau, exposição...) tem:

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | número | Gerado automaticamente; nunca vem do cliente e é preservado nas atualizações |
| `titulo` | texto | Obrigatório; não aceita vazio nem só espaços (o valor salvo é "aparado" com `trim`) |
| `participantes` | inteiro | Maior que zero e, no máximo, **5.000** |

## Regra de negócio

> Um evento pode ter **no máximo 5.000 participantes**.

- Valor entre 1 e 5000: aceito (5000 é válido).
- Valor acima de 5000: **422 Unprocessable Entity**.
- Valor menor ou igual a zero, título em branco ou JSON inválido: **400 Bad Request** (validação de entrada, não regra de negócio).
- A regra fica no `EventoService` e vale igualmente no cadastro e na atualização. Quando a operação é rejeitada, nada é gravado.

## Como executar

Requisitos: **Java 21** e **Maven 3.9+**.

```bash
# executar os testes
mvn test

# subir a API em http://127.0.0.1:8080
mvn spring-boot:run

# ou gerar o jar executável
mvn package
java -jar target/cadastro-eventos-1.0.0.jar
```

A porta e o nome da aplicação ficam em `src/main/resources/application.properties`. Os exemplos de chamadas estão em [`requests.http`](requests.http) (extensão REST Client do VS Code ou IntelliJ).

## Rotas

| Método | Rota | Descrição | Sucesso | Erros |
|--------|------|-----------|---------|-------|
| POST | `/eventos` | Cadastra um evento | **201** + cabeçalho `Location: /eventos/{id}` | 400, 422 |
| GET | `/eventos` | Lista todos os eventos | **200** | — |
| GET | `/eventos?titulo=abc` | Filtra por parte do título, sem diferenciar maiúsculas/minúsculas | **200** | — |
| GET | `/eventos/{id}` | Consulta por ID | **200** | 400 (ID não numérico), 404 |
| PUT | `/eventos/{id}` | Atualiza (o ID é preservado) | **200** | 400, 404, 422 |
| DELETE | `/eventos/{id}` | Exclui | **204** (sem corpo) | 404 |
| GET | `/health` | Verificação de saúde | **200** | — |
| GET | `/` | Informações da aplicação | **200** | — |

Exemplo de corpo para POST/PUT:

```json
{ "titulo": "Festival de Música", "participantes": 3000 }
```

Respostas de erro seguem um formato estruturado (`timestamp`, `status`, `message`, `path`, `details` e `requestId`), produzido pelo `GlobalExceptionHandler` (`@RestControllerAdvice`).

### Cadastro de pessoas

Além dos eventos, a API permite cadastrar as pessoas que participam de cada evento. Cada pessoa tem `id` (gerado automaticamente), `nome`, `email` e `eventoId` (o evento ao qual ela está vinculada).

| Método | Rota | Descrição | Sucesso | Erros |
|--------|------|-----------|---------|-------|
| POST | `/pessoas` | Cadastra uma pessoa | **201** + cabeçalho `Location: /pessoas/{id}` | 400, 422 |
| GET | `/pessoas` | Lista todas as pessoas | **200** | — |
| GET | `/pessoas?nome=abc` | Filtra por parte do nome, sem diferenciar maiúsculas/minúsculas | **200** | — |
| GET | `/pessoas/{id}` | Consulta por ID | **200** | 400 (ID não numérico), 404 |
| PUT | `/pessoas/{id}` | Atualiza (o ID é preservado) | **200** | 400, 404, 422 |
| DELETE | `/pessoas/{id}` | Exclui | **204** (sem corpo) | 404 |

Exemplo de corpo para POST/PUT:

```json
{ "nome": "Ana Souza", "email": "ana@email.com", "eventoId": 1 }
```

Regras:

- `nome` e `email` são obrigatórios e não aceitam vazio nem só espaços: **400**.
- `email` precisa ter um formato válido (ex.: `nome@dominio.com`): **400**.
- `eventoId` deve ser maior que zero: **400**.
- `eventoId` de um evento que não existe: **422**.
- Uma requisição rejeitada não altera os dados armazenados.

Os exemplos de chamadas estão em [`requests-pessoas.http`](requests-pessoas.http).

## Arquitetura

```
Requisição HTTP
   │
   ▼
DispatcherServlet (Spring MVC) ── RequestIdFilter gera o X-Request-Id; Jackson converte o JSON; @Valid valida
   │
   ▼
EventoController   → só trata HTTP: lê entrada, chama o Service, devolve status/Location
   │
   ▼
EventoService      → regra de negócio (limite de 5000), filtro por título, 404
   │
   ▼
EventoRepository (interface) ◄── InMemoryEventoRepository (ConcurrentHashMap + AtomicLong)
```

| Camada | Classe | Responsabilidade |
|--------|--------|------------------|
| Controller | `EventoController` | Mapeia rotas (`@GetMapping`, `@PostMapping`...) e códigos HTTP; sem regra de negócio |
| Service | `EventoService` | Aplica a regra de negócio e lança `HttpException` (404/422) |
| Repository | `EventoRepository` / `InMemoryEventoRepository` | Guarda os dados em memória e gera o ID |
| DTO de entrada | `EventoInput` | Recebe o JSON e declara `@NotBlank` / `@Positive`; o cliente não escolhe o ID |
| Modelo | `Evento` | Registro imutável devolvido pela API |
| Web | `GlobalExceptionHandler`, `RequestIdFilter` | Formato de erro padronizado, `X-Request-Id` e limite de 1 MiB no corpo |

### Injeção de dependências

Tudo é injetado **pelo construtor** pelo container do Spring: o `EventoController` recebe o `EventoService`, e o `EventoService` recebe a **interface** `EventoRepository`. O `@SpringBootApplication` faz a varredura de classes anotadas com `@RestController`, `@Service` e `@Repository`. Por isso o Service pode ser testado com um repositório falso, sem subir servidor.

### Caminho de uma requisição (`POST /eventos`)

1. O Tomcat embutido recebe a requisição e entrega ao `DispatcherServlet`.
2. O Spring MVC casa método + caminho com `EventoController.create`.
3. O corpo JSON é convertido para `EventoInput` e validado (`@Valid`) → 400 se inválido.
4. O Controller chama `EventoService.create`, que aplica a regra dos 5000 → 422 se violada.
5. O Service chama o `EventoRepository`, que gera o ID e guarda o evento.
6. O Controller devolve `ResponseEntity.created(...)` → **201** com `Location`.

## Testes

- `ApiIntegrationTest`: CRUD completo com `@SpringBootTest` em porta aleatória, filtros, limites (1 e 5000), 400/404/422 e verificação de que requisições rejeitadas não alteram os dados.
- `EventoServiceTest`: regra de negócio e filtro isolados, com repositório falso injetado pelo construtor.
- `PessoaServiceTest`: cadastro de pessoas isolado (validação de e-mail, evento inexistente, filtro por nome e preservação do ID).
- `UnexpectedErrorIntegrationTest`: erro inesperado devolve 500 em JSON, sem vazar detalhes internos.

## Uso de IA

Foi utilizada IA (Claude, da Anthropic) como apoio para revisar o projeto, ajustar a versão do Java para 21, escrever o teste do Service e redigir esta documentação. Todos os integrantes revisaram e são capazes de explicar o código entregue.

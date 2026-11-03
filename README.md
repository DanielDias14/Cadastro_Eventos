# Cadastro de Eventos — Centro Cultural

API REST em Java para cadastrar os eventos de um centro cultural, construída sobre o framework didático **mini-spring** (pacote `br.com.minispring.framework`), com arquitetura em camadas, injeção de dependências por construtor e dados em memória.

## Integrantes

| Nome |
|------|
| Cinthia Emanuelly |
| Daniel Dias |
| Ires Vitória |
| Jady Wéllyda |
| João Vitor Lemos |

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

# gerar o jar executável e subir a API em http://127.0.0.1:8080
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

Respostas de erro seguem o formato estruturado do framework (mensagem, detalhes e `requestId`).

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
DispatcherServlet (framework) ── Router encontra a rota e converte parâmetros/JSON
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
| Controller | `EventoController` | Mapeia rotas e códigos HTTP; não contém regra de negócio |
| Service | `EventoService` | Aplica a regra de negócio e lança `HttpException` (404/422) |
| Repository | `EventoRepository` / `InMemoryEventoRepository` | Guarda os dados em memória e gera o ID |
| DTO de entrada | `EventoInput` | Recebe o JSON e declara `@NotBlank` / `@Positive`; o cliente não escolhe o ID |
| Modelo | `Evento` | Registro imutável devolvido pela API |

### Injeção de dependências

Tudo é injetado **pelo construtor**: o `EventoController` recebe o `EventoService`, e o `EventoService` recebe a **interface** `EventoRepository`. Na inicialização, o `ComponentScanner` procura classes anotadas com `@RestController`, `@Service` e `@Repository`, e o `ApplicationContext` as instancia na ordem das dependências, escolhendo a implementação da interface. Por isso o Service pode ser testado com um repositório falso, sem subir servidor.

### Caminho de uma requisição (`POST /eventos`)

1. O Jetty recebe a requisição e entrega ao `DispatcherServlet`.
2. O `Router` casa método + caminho com `EventoController.create`.
3. O corpo JSON é convertido para `EventoInput` e validado (`@NotBlank`, `@Positive`) → 400 se inválido.
4. O Controller chama `EventoService.create`, que aplica a regra dos 5000 → 422 se violada.
5. O Service chama o `EventoRepository`, que gera o ID e guarda o evento.
6. O Controller devolve `HttpResult.created("/eventos/{id}", evento)` → **201** com `Location`.

## Testes

- `ApiIntegrationTest`: CRUD completo contra um Jetty real, filtros, limites (1 e 5000), 400/404/422 e verificação de que requisições rejeitadas não alteram os dados.
- `EventoServiceTest`: regra de negócio e filtro isolados, com repositório falso injetado pelo construtor.
- `PessoaServiceTest`: cadastro de pessoas isolado (validação de e-mail, evento inexistente, filtro por nome e preservação do ID).
- `RouterTest`, `ApplicationContextTest`, `UnexpectedErrorIntegrationTest`: testes do framework.

## Uso de IA

Foi utilizada IA (Claude, da Anthropic) como apoio para revisar o projeto, ajustar a versão do Java para 21, escrever o teste do Service e redigir esta documentação. Todos os integrantes revisaram e são capazes de explicar o código entregue.

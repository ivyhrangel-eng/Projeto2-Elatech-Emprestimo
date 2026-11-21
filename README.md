# API de Empréstimo de Equipamentos

API REST para controlar o empréstimo de equipamentos (notebooks, monitores, headsets) para pessoas colaboradoras: quem está com o quê, quando deve devolver e o que está atrasado.

**API no Render:** https://COLOQUE-AQUI-O-SEU-ENDERECO.onrender.com

## Tecnologias

- Java 25
- Spring Boot 4 (Spring Web, Spring Data JPA)
- Banco H2 (salvo em arquivo na pasta `dados/`)
- JUnit 5
- Docker (deploy no Render)

## Como rodar

### Pelo IntelliJ
1. Abrir a pasta do projeto no IntelliJ e esperar o Maven baixar as dependências.
2. Rodar a classe `EmprestimosApplication` (botão ▶ verde).
3. A API sobe em `http://localhost:8080`.

### Pelo terminal
```bash
mvn spring-boot:run
```

O arquivo `postman_plano_de_testes.json` tem o plano de testes pronto para importar no Postman.

### Testes
- IntelliJ: botão direito em `src/test/java` → **Run 'All Tests'**
- Terminal: `mvn test`

### Console do banco (H2)
`http://localhost:8080/h2-console` · JDBC URL: `jdbc:h2:file:./dados/emprestimos` · usuário `sa`, senha vazia.

## Rotas

### Equipamentos
| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| POST | `/equipamentos` | Cadastra | 201 |
| GET | `/equipamentos` | Lista (filtros opcionais `?tipo=NOTEBOOK` e `?disponivel=true`) | 200 |
| GET | `/equipamentos/{id}` | Busca um equipamento | 200 |
| PUT | `/equipamentos/{id}` | Atualiza nome e tipo | 200 |
| DELETE | `/equipamentos/{id}` | Remove (se nunca foi emprestado) | 204 |

Tipos aceitos: `NOTEBOOK`, `MONITOR`, `HEADSET`, `TECLADO`, `MOUSE`, `OUTRO`.

### Colaboradores
| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| POST | `/colaboradores` | Cadastra | 201 |
| GET | `/colaboradores` | Lista | 200 |
| GET | `/colaboradores/{id}` | Busca uma pessoa | 200 |
| GET | `/colaboradores/{id}/emprestimos` | Histórico de empréstimos da pessoa | 200 |

### Empréstimos
| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| POST | `/emprestimos?equipamentoId=1&colaboradorId=1` | Empresta | 201 |
| GET | `/emprestimos` | Lista todos (com `?atrasados=true`, só os atrasados) | 200 |
| GET | `/emprestimos/{id}` | Busca um empréstimo | 200 |
| PUT | `/emprestimos/{id}/devolucao` | Registra a devolução | 200 |

### Exemplos de corpo (JSON)
```json
POST /equipamentos
{ "patrimonio": "NB-001", "nome": "Notebook Dell", "tipo": "NOTEBOOK" }

POST /colaboradores
{ "nome": "Ana Souza", "email": "ana@empresa.com", "cargo": "Analista" }
```

## Regras de negócio

| Regra | Descrição | Onde está |
|---|---|---|
| R1 | Não existem dois equipamentos com o mesmo patrimônio | `EquipamentoService.cadastrar` |
| R2 | Todo equipamento novo nasce disponível | `EquipamentoService.cadastrar` |
| R3 | Não existem duas pessoas com o mesmo e-mail | `ColaboradorService.cadastrar` |
| R4 | Só se empresta equipamento disponível | `Equipamento.emprestar` |
| R5 | Ao emprestar: equipamento indisponível, empréstimo hoje, devolução prevista em 14 dias | `Emprestimo` (construtor), `Equipamento.emprestar`, `EmprestimoService.emprestar` |
| R6 | Empréstimo devolvido não pode ser devolvido de novo | `Emprestimo.devolver` |
| R7 | Ao devolver: data de devolução hoje e equipamento disponível | `Emprestimo.devolver`, `Equipamento.devolver`, `EmprestimoService.devolver` |
| R8 | Atrasado = não devolvido e com a data prevista já passada (calculado, não é coluna) | `Emprestimo.estaAtrasadoEm`, `EmprestimoRepository` |
| R9 | Equipamento com histórico de empréstimo não pode ser removido | `EquipamentoService.remover` |
| R10 | Quem tem empréstimo atrasado não pode pegar outro | `EmprestimoService.emprestar` |

Quando uma regra é quebrada, a API responde com erro 500 e a mensagem da regra. Id que não existe responde 404.

## Testes unitários

- `EquipamentoTest`: 3 casos (emprestar disponível, emprestar indisponível, devolver)
- `EmprestimoTest`: 7 casos (datas do empréstimo, devolução, devolução dupla e os 4 casos de atraso)

## Bônus feitos

- **Swagger (N1):** documentação das rotas em `/swagger-ui/index.html`
- **Actuator (N1):** `GET /actuator/health` responde `{"status":"UP"}`

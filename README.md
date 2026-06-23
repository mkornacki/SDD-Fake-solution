# Hotel Reservation System — Assessment
Short message
A production-grade Hotel Reservation API built with Java 11 and Spring Boot, following **Spec-Driven Development** principles using **SpecKit**. The project demonstrates a hexagonal architecture with full OAuth2 security, resilience patterns, contract testing, and a fully automated local developer environment via **Dev Containers**.

---

## Directory Structure

```
.
├── .devcontainer/          # Dev Container definition (Dockerfile + devcontainer.json)
├── .github/
│   ├── agents/             # GitHub Copilot custom agent instructions
│   └── prompts/            # Reusable AI prompt templates
├── backend/                # Spring Boot application (main source code)
│   ├── src/
│   │   ├── main/java/      # Production code (hexagonal architecture)
│   │   └── test/java/      # Unit, integration, contract and E2E tests
│   ├── data/sql/           # SQL verification scripts
│   ├── Dockerfile          # Container image for the backend service
│   └── pom.xml             # Maven build descriptor and dependency definitions
├── docker/
│   └── compose/
│       ├── compose.yml     # Docker Compose stack (backend + SQLite storage)
│       ├── .env.example    # Template for local environment variables
│       └── README.md       # Compose-specific quick-start guide
├── docs/
│   └── development/
│       └── environment.md  # Developer runbook: setup, verification, troubleshooting
├── scripts/
│   └── dev/
│       ├── start-env.sh    # Boots the full local stack
│       ├── stop-env.sh     # Graceful shutdown
│       └── reset-env.sh    # Tears down and recreates volumes
└── specs/                  # All feature specifications (Spec-Driven Development)
    ├── 001-backend-foundation/
    ├── 002-hotel-reservation-api/
    ├── 003-resilience-audit-scale/
    └── 004-dev-environment-bootstrap/
```

### Source Code Layout (`backend/src/main/java/com/acme/`)

The backend follows a **hexagonal (ports & adapters)** architecture split into two top-level modules:

| Package | Responsibility |
|---|---|
| `foundation` | Platform infrastructure: health, observability, security config, audit filters, sample data seeding |
| `reservation` | Business domain: reservation lifecycle, room management, PII handling, DLQ, financial breakdown, partner integration |

Each module is internally structured as:

```
adapters/
  inbound/http/       # REST controllers, request/response models
  outbound/persistence/ # JPA entities, repositories
application/
  command/            # Command handlers (use-case implementations)
  ports/              # Inbound and outbound port interfaces
domain/               # Pure domain model (no framework dependencies)
configuration/        # Spring configuration classes
```

---

## Documentation

| Topic | Location |
|---|---|
| Local environment setup & troubleshooting | [docs/development/environment.md](docs/development/environment.md) |
| Docker Compose quick-start | [docker/compose/README.md](docker/compose/README.md) |
| Contributing guidelines | [backend/CONTRIBUTING.md](backend/CONTRIBUTING.md) |
| Feature specifications | [specs/](specs/) |
| API contracts (OpenAPI) | `specs/<feature>/contracts/*.yaml` |
| Test checklists | `specs/<feature>/checklists/` |
| Operational runbooks | `specs/003-resilience-audit-scale/runbooks/` |

### Feature Specs at a Glance

| Spec | Topic |
|---|---|
| [001-backend-foundation](specs/001-backend-foundation/spec.md) | Stable REST foundation, health/readiness endpoints, OAuth2 security baseline |
| [002-hotel-reservation-api](specs/002-hotel-reservation-api/spec.md) | Multi-room reservations, PII protection, idempotency, financial consistency |
| [003-resilience-audit-scale](specs/003-resilience-audit-scale/spec.md) | Circuit-breakers, retry, async partner processing, audit & retention governance |
| [004-dev-environment-bootstrap](specs/004-dev-environment-bootstrap/spec.md) | Docker Compose stack, seeded SQLite database, onboarding developer experience |

Each spec folder contains:

- `spec.md` — functional and non-functional requirements
- `plan.md` — implementation design and decisions
- `tasks.md` — ordered, actionable development tasks
- `data-model.md` — entity and schema definitions
- `research.md` — technology evaluation notes
- `quickstart.md` — feature-level quick-start guide
- `contracts/` — OpenAPI contract definitions
- `checklists/` — acceptance and validation checklists

---

## Technology Stack

### Core Application

| Technology | Role |
|---|---|
| Java 11 | Primary language |
| Spring Boot 2.7 | Application framework |
| Spring Data JPA + Liquibase | Persistence and schema migrations |
| SQLite | Embedded database (local/dev profile) |
| Spring Security + OAuth2 Resource Server | JWT-based authentication |
| SpringDoc OpenAPI (Swagger UI) | Auto-generated API documentation |
| Resilience4j | Circuit-breakers, retry, bulkhead |
| Spring AMQP (RabbitMQ) | Async messaging and dead-letter queue |
| Micrometer + Logstash Logback | Metrics and structured JSON logging |

### Testing

| Technology | Role |
|---|---|
| JUnit 5 + Mockito | Unit and integration tests |
| Testcontainers | Containerised integration testing |
| Pact | Consumer-driven contract tests |
| Playwright | End-to-end and security tests |
| JaCoCo | Code coverage |
| Checkstyle | Code style enforcement |

---

## Key Concepts

### Dev Containers

This project ships with a fully preconfigured [Dev Container](https://containers.dev/) (`.devcontainer/`). Opening the repository in VS Code with the **Dev Containers** extension automatically builds an isolated Linux environment with:

- Java 11, Maven, Gradle
- Docker-in-Docker (DinD) — run `docker compose` commands directly inside the container
- Zsh with Oh My Zsh
- All required VS Code extensions pre-installed (Java Pack, SpecKit, GitHub Copilot)

**You do not need to install any tools on your host machine.** The entire toolchain lives inside the container, making the environment fully reproducible across machines and operating systems.

### SpecKit

[SpecKit](https://marketplace.visualstudio.com/items?itemName=speckit.speckit) is a VS Code extension that provides a structured AI-assisted workflow for creating and evolving feature specifications. It is used throughout this project to generate and maintain the documents inside `specs/`.

SpecKit exposes a set of slash-command agents:

| Agent | Purpose |
|---|---|
| `speckit.specify` | Create or update a feature spec from a plain-language description |
| `speckit.clarify` | Ask targeted questions to refine an underspecified requirement |
| `speckit.plan` | Generate a detailed implementation plan from the spec |
| `speckit.tasks` | Break the plan into ordered, actionable development tasks |
| `speckit.implement` | Execute tasks from `tasks.md` using GitHub Copilot |
| `speckit.analyze` | Cross-check consistency between spec, plan, and tasks |
| `speckit.checklist` | Generate acceptance checklists for QA and review |

### Spec-Driven Development (SDD)

Spec-Driven Development is an engineering practice where **a written specification is the single source of truth** for a feature — written *before* any code. Every design decision, API contract, data model, and acceptance criterion is captured in the `specs/` directory and kept in version control alongside the code.

The workflow followed in this project:

```
1. Write spec.md      → define requirements and acceptance criteria
2. Generate plan.md   → design decisions, architecture, edge cases
3. Generate tasks.md  → ordered implementation checklist
4. Implement          → code driven by tasks, contracts enforced by Pact
5. Verify             → checklists, contract tests, E2E tests
```

Benefits demonstrated in this codebase:

- **Traceability** — every controller, handler, and entity can be traced back to a spec requirement
- **Contract-first APIs** — OpenAPI contracts in `specs/<feature>/contracts/` are defined before implementation and enforced with Pact consumer-driven contract tests
- **AI-assisted, human-reviewed** — SpecKit agents accelerate spec and plan authoring; all artifacts are reviewed and committed as first-class source files
- **Onboarding** — a new developer can read `specs/` to understand *why* architectural decisions were made, not just *what* the code does

---

## Quick Start

```bash
# 1. Copy environment variables template
cp docker/compose/.env.example docker/compose/.env

# 2. Start the full local stack
./scripts/dev/start-env.sh

# 3. Verify the stack is healthy
curl http://localhost:8081/actuator/health/readiness

# 4. Access Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

See [docs/development/environment.md](docs/development/environment.md) for full setup instructions and troubleshooting.

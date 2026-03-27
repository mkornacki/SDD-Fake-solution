# Tasks: Developer Environment Bootstrap with Seeded Database

**Input**: Design documents from `/specs/004-dev-environment-bootstrap/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/dev-environment-api.yaml, quickstart.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unmet dependencies)
- **[Story]**: User story label (`US1`, `US2`, `US3`) for story-phase tasks
- Every task includes an explicit file path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish Docker Compose assets and local configuration baseline

- [X] T001 Create compose stack definition for backend container, mounted SQLite volume, and healthcheck wiring in `docker/compose/compose.yml`
- [X] T002 [P] Add local environment defaults for compose (ports, DB path, profile, credentials) in `docker/compose/.env.example`
- [X] T003 [P] Configure local bootstrap profile and SQLite/Flyway properties in `backend/src/main/resources/application-local.yml`
- [X] T004 Add/adjust container build and runtime configuration for local profile in `backend/Dockerfile`

**Checkpoint**: Local stack can be started from compose assets with consistent defaults

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement migration, seeding, readiness, and startup validation primitives needed by all user stories

**⚠️ CRITICAL**: Complete this phase before any user story implementation

- [X] T005 Create versioned schema baseline migration for local bootstrap in `backend/src/main/resources/db/migration/V100__dev_bootstrap_baseline.sql`
- [X] T006 [P] Add deterministic seed dataset SQL with stable business keys in `backend/src/main/resources/db/seed/sample-data.sql`
- [X] T007 [P] Implement idempotent seed executor that applies seed scripts after migrations in `backend/src/main/java/com/acme/foundation/adapters/outbound/persistence/SampleDataSeeder.java`
- [X] T008 Implement seed status application service (`datasetName`, `datasetVersion`, `seedStatus`, `recordCounts`, `seededAt`) in `backend/src/main/java/com/acme/foundation/application/SampleDataStatusService.java`
- [X] T009 [P] Implement authenticated sample dataset status endpoint (`GET /api/v1/dev/sample-data/status`) in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/dev/SampleDataStatusController.java`
- [X] T010 [P] Extend readiness aggregation to report migration/seed completion state in `backend/src/main/java/com/acme/foundation/adapters/outbound/persistence/DatabaseHealthIndicator.java`
- [X] T011 Implement startup configuration validation for required env vars with actionable messages in `backend/src/main/java/com/acme/foundation/configuration/StartupValidationConfig.java`
- [X] T012 Wire security rules for the dev sample-data status endpoint in `backend/src/main/java/com/acme/foundation/configuration/SecurityConfig.java`

**Checkpoint**: Migrations, deterministic seeding, readiness state, and startup validation are available for story work

---

## Phase 3: User Story 1 - Start Local Development Environment (Priority: P1) 🎯 MVP

**Goal**: Enable one-command startup with observable healthy service status

**Independent Test**: From a clean machine, run the documented startup command and verify compose services show healthy and readiness is `UP`

### Tests for User Story 1

- [X] T013 [P] [US1] Add compose startup smoke script validating one-command up and healthy status in `docker/compose/tests/startup-smoke.sh`
- [X] T014 [P] [US1] Add readiness integration test for local bootstrap health response in `backend/src/test/java/com/acme/foundation/health/ReadinessBootstrapIT.java`
- [X] T015 [P] [US1] Update health endpoint contract test for bootstrap readiness semantics in `backend/src/test/java/com/acme/foundation/contract/HealthEndpointContractTest.java`

### Implementation for User Story 1

- [X] T016 [US1] Create one-command startup helper for developers in `scripts/dev/start-env.sh`
- [X] T017 [US1] Create environment stop helper preserving persisted state in `scripts/dev/stop-env.sh`
- [X] T018 [US1] Add compose service health gate (`depends_on` with health condition) for backend startup ordering in `docker/compose/compose.yml`
- [X] T019 [US1] Improve bootstrap error reporting for startup validation failures in `backend/src/main/java/com/acme/foundation/adapters/inbound/http/error/GlobalExceptionHandler.java`
- [X] T020 [US1] Document canonical startup command and service-status verification command in `docker/compose/README.md`

**Checkpoint**: User Story 1 is independently functional and demoable as MVP

---

## Phase 4: User Story 2 - Use Running Database with Seeded Data (Priority: P1)

**Goal**: Ensure schema and deterministic sample data are present immediately after startup

**Independent Test**: Start stack, verify migration state, and confirm expected baseline sample rows are available via endpoint and/or SQL query

### Tests for User Story 2

- [X] T021 [P] [US2] Add unit test for seed idempotency/upsert behavior across repeated runs in `backend/src/test/java/com/acme/foundation/domain/seed/SampleDatasetIdempotencyTest.java`
- [X] T022 [P] [US2] Add integration test validating migration + seed execution during startup in `backend/src/test/java/com/acme/foundation/seed/DatabaseSeedIT.java`
- [X] T023 [P] [US2] Add contract test for `GET /api/v1/dev/sample-data/status` response schema and auth behavior in `backend/src/test/java/com/acme/foundation/contract/SampleDataStatusContractTest.java`

### Implementation for User Story 2

- [X] T024 [US2] Add seed metadata migration and tracking table for deterministic dataset versioning in `backend/src/main/resources/db/migration/V101__sample_dataset_metadata.sql`
- [X] T025 [US2] Implement persistence adapter to compute sample-data record counts by type in `backend/src/main/java/com/acme/foundation/adapters/outbound/persistence/SampleDataStatusRepositoryAdapter.java`
- [X] T026 [US2] Implement domain representation for seed execution state in `backend/src/main/java/com/acme/foundation/domain/seed/SampleDatasetState.java`
- [X] T027 [US2] Integrate seed completion details into readiness details payload in `backend/src/main/java/com/acme/foundation/application/HealthSummaryService.java`
- [X] T028 [US2] Add SQL verification snippet for seeded baseline rows in `backend/data/sql/verify-sample-data.sql`

**Checkpoint**: User Story 2 independently confirms running database, schema readiness, and deterministic sample dataset availability

---

## Phase 5: User Story 3 - Follow Developer Documentation (Priority: P2)

**Goal**: Provide complete onboarding and troubleshooting documentation for startup, verification, shutdown, and reset

**Independent Test**: A developer unfamiliar with the project can follow documentation end-to-end and recover from a common failure without direct help

### Tests for User Story 3

- [X] T029 [P] [US3] Add runbook smoke script that executes documented start/verify/stop/reset flow in `docker/compose/tests/runbook-smoke.sh`
- [X] T030 [P] [US3] Create onboarding validation checklist for first-time developer dry run in `specs/004-dev-environment-bootstrap/checklists/onboarding-validation.md`

### Implementation for User Story 3

- [X] T031 [US3] Author full local environment runbook (prereqs, startup, verification, shutdown, reset, troubleshooting) in `docs/development/environment.md`
- [X] T032 [US3] Update quickstart with exact verified commands and expected outputs in `specs/004-dev-environment-bootstrap/quickstart.md`
- [X] T033 [US3] Create deterministic reset helper (down -v, rebuild, re-verify) in `scripts/dev/reset-env.sh`
- [X] T034 [US3] Expand troubleshooting guidance for slow DB startup, port conflicts, and env-var failures in `docker/compose/README.md`

**Checkpoint**: User Story 3 independently provides reliable onboarding and recovery documentation

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, contract alignment, and success-criteria evidence capture

- [X] T035 [P] Validate OpenAPI contract alignment for health and sample-data endpoints in `specs/004-dev-environment-bootstrap/contracts/dev-environment-api.yaml`
- [X] T036 [P] Add end-to-end bootstrap verification test covering start -> readiness -> seeded data -> reset in `backend/src/test/java/com/acme/foundation/e2e/DevEnvironmentBootstrapE2ETest.java`
- [X] T037 Measure startup duration and record SC-001 evidence in `specs/004-dev-environment-bootstrap/checklists/performance-baseline.md`
- [X] T038 Capture test and smoke execution evidence for SC-002/SC-003/SC-004/SC-005 in `specs/004-dev-environment-bootstrap/checklists/test-evidence.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 (can run in parallel with US1 after foundation is complete)
- **Phase 5 (US3)**: Depends on US1 and US2 outputs (documentation references verified runtime behavior)
- **Phase 6 (Polish)**: Depends on all user story phases

### User Story Dependencies

- **US1 (P1)**: Starts after Foundational completion
- **US2 (P1)**: Starts after Foundational completion; independent of US1 implementation details
- **US3 (P2)**: Depends on finalized startup/seed behavior from US1 and US2

### Within Each User Story

- Write tests first and confirm they fail before implementation
- Implement runtime logic before scripts/readme updates that depend on it
- Validate independent test criteria before moving to the next story

### Parallel Opportunities

- Setup: T002 and T003 can run in parallel
- Foundational: T006, T007, T009, and T010 can run in parallel after T005
- US1: T013, T014, T015 can run in parallel
- US2: T021, T022, T023 can run in parallel
- US3: T029 and T030 can run in parallel
- Polish: T035 and T036 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Parallel test tasks
Task: "T013 [US1] startup smoke script"
Task: "T014 [US1] readiness integration test"
Task: "T015 [US1] readiness contract test"
```

## Parallel Example: User Story 2

```bash
# Parallel test tasks
Task: "T021 [US2] seed idempotency unit test"
Task: "T022 [US2] migration+seed integration test"
Task: "T023 [US2] sample-data status contract test"
```

## Parallel Example: User Story 3

```bash
# Parallel verification assets
Task: "T029 [US3] runbook smoke script"
Task: "T030 [US3] onboarding validation checklist"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 (Setup)
2. Complete Phase 2 (Foundational)
3. Complete Phase 3 (US1)
4. Validate one-command startup and healthy readiness

### Incremental Delivery

1. Deliver US1 (startup)
2. Deliver US2 (seeded DB verification)
3. Deliver US3 (developer runbook and troubleshooting)
4. Finish Polish for success-criteria evidence and contract alignment

# Data Model: Developer Environment Bootstrap with Seeded Database

## Entity: EnvironmentStack
- Description: Logical representation of the local development runtime assembled by Docker Compose.
- Fields:
  - `stackName` (string, required)
  - `services` (list of `StackService`, required)
  - `status` (enum: STOPPED, STARTING, HEALTHY, DEGRADED, FAILED)
  - `startupCommand` (string, required)
  - `shutdownCommand` (string, required)
  - `resetCommand` (string, required)
- Validation Rules:
  - `startupCommand`, `shutdownCommand`, and `resetCommand` must be documented and executable on supported developer machines.
  - `status=HEALTHY` requires all mandatory services to be healthy.
- Relationships:
  - One-to-many with `StackService`.
  - One-to-one with `DeveloperRunbook`.

## Entity: StackService
- Description: A compose-managed service participating in the local development environment.
- Fields:
  - `serviceName` (string, required)
  - `serviceType` (enum: APPLICATION, DATABASE, SUPPORTING)
  - `containerImage` (string, required)
  - `healthEndpoint` (string, optional)
  - `healthCheckType` (enum: DOCKER_HEALTHCHECK, HTTP, PROCESS)
  - `dependsOn` (string list, optional)
  - `requiredEnvironmentVariables` (string list, optional)
  - `mountedVolumes` (string list, optional)
- Validation Rules:
  - Mandatory services must define a health validation mechanism.
  - All required environment variables must be documented with safe local defaults or explicit setup instructions.

## Entity: DatabaseInstance
- Description: Local SQLite persistence used by the backend during development bootstrap.
- Fields:
  - `databaseEngine` (string, fixed value SQLite)
  - `databaseFilePath` (string, required)
  - `schemaVersion` (string, required)
  - `readinessState` (enum: NOT_INITIALIZED, MIGRATING, READY, FAILED)
  - `lastMigrationAt` (timestamp, nullable)
- Validation Rules:
  - `schemaVersion` must match the latest applied migration version when `readinessState=READY`.
  - `databaseFilePath` must resolve to a persistent mounted path for normal startup.
- Relationships:
  - One-to-many with `MigrationRecord`.
  - One-to-many with `SampleDataRecord`.

## Entity: MigrationRecord
- Description: Versioned schema change applied during environment initialization.
- Fields:
  - `version` (string, required)
  - `description` (string, required)
  - `appliedAt` (timestamp, required)
  - `success` (boolean, required)
- Validation Rules:
  - Migration versions must be unique and ordered.
  - Failed migrations must block readiness and produce actionable diagnostics.

## Entity: SampleDataset
- Description: Deterministic collection of development records inserted after schema initialization.
- Fields:
  - `datasetName` (string, required)
  - `datasetVersion` (string, required)
  - `seedMode` (enum: UPSERT, INSERT_IF_MISSING)
  - `seedStatus` (enum: NOT_STARTED, RUNNING, COMPLETED, FAILED)
  - `seededAt` (timestamp, nullable)
- Validation Rules:
  - Seeding must be idempotent across repeated environment starts.
  - Dataset content must remain non-sensitive and development-only.
- Relationships:
  - One-to-many with `SampleDataRecord`.

## Entity: SampleDataRecord
- Description: Representative seeded business record used to validate local application behavior.
- Fields:
  - `recordType` (string, required)
  - `businessKey` (string, required)
  - `displayName` (string, required)
  - `seedSource` (string, required)
  - `lastSeededAt` (timestamp, required)
- Validation Rules:
  - `businessKey` must be stable across runs to support deterministic upserts.
  - Records must satisfy the same validation rules as runtime-created entities.

## Entity: DeveloperRunbook
- Description: Documentation artifact describing how to start, verify, stop, reset, and troubleshoot the environment.
- Fields:
  - `documentPath` (string, required)
  - `prerequisites` (string list, required)
  - `verificationSteps` (string list, required)
  - `resetSteps` (string list, required)
  - `troubleshootingTopics` (string list, required)
  - `lastReviewedAt` (timestamp, nullable)
- Validation Rules:
  - The runbook must cover prerequisites, startup, verification, shutdown, reset, and troubleshooting.
  - Verification steps must include both service health and seeded-data validation.

## State Transitions
- EnvironmentStack:
  - STOPPED -> STARTING -> HEALTHY
  - STARTING -> DEGRADED -> HEALTHY
  - STARTING -> FAILED
- DatabaseInstance:
  - NOT_INITIALIZED -> MIGRATING -> READY
  - MIGRATING -> FAILED
- SampleDataset:
  - NOT_STARTED -> RUNNING -> COMPLETED
  - RUNNING -> FAILED
# Contributing Guide

## Commit Message Convention

This project enforces **Conventional Commits** via a `commit-msg` git hook.

### Format

```
<type>(<optional scope>): <description>
```

### Types

| Type | When to use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `style` | Code style / formatting |
| `refactor` | Neither fix nor feature |
| `perf` | Performance improvement |
| `test` | Adding or correcting tests |
| `chore` | Build / tooling changes |
| `ci` | CI/CD changes |
| `revert` | Revert a previous commit |

### Examples

```
feat(reservation): add idempotent create endpoint
fix(idempotency): handle concurrent duplicate requests correctly
test(cancellation): add partial cancellation atomicity test
chore(deps): add Resilience4j dependency
```

### Hook Installation

The hook is installed automatically from `.git/hooks/commit-msg`.
If cloning fresh, run:

```sh
cp .git/hooks/commit-msg .git/hooks/commit-msg && chmod +x .git/hooks/commit-msg
```

## Code Style

- Google Java Style Guide enforced via Checkstyle (`checkstyle.xml`).
- Run `./mvnw checkstyle:check` to validate locally.

## Testing

- `./mvnw test` — unit + integration tests
- `./mvnw verify` — full build with quality checks
- SonarQube gate: zero Blocker/Critical/Major violations, ≥ 80% domain coverage.

## Clean Architecture

- `domain/` — no Spring or ORM imports
- `application/` — use cases and ports only
- `adapters/` — Spring, JPA, HTTP, messaging
- `configuration/` — Spring beans and wiring

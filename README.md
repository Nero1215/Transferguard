# TransferGuard

TransferGuard is a small Java portfolio project that demonstrates safe money-transfer logic without hiding the core ideas behind a framework.

## Why this project exists

The first version focuses on transaction fundamentals that are easy to discuss in a junior backend interview: money precision, atomic updates, rollback, idempotency, dependency boundaries, deterministic time, and basic concurrency protection.

## Features

- Transfers money between in-memory accounts using `BigDecimal`
- Rejects invalid transfers and insufficient funds
- Uses request IDs to make repeated transfer requests idempotent
- Rolls back account balances when a downstream audit operation fails
- Injects `Clock` so time-based behavior is deterministic in tests
- Depends on repository and audit abstractions instead of hard-coding infrastructure
- Serializes the in-process transfer operation with `synchronized`
- Includes executable tests and GitHub Actions CI

## Project structure

```text
src/main/java/com/nero/transferguard
├── Main.java
├── domain
│   ├── Account.java
│   └── TransferReceipt.java
├── port
│   └── TransferAuditPort.java
├── repository
│   ├── AccountRepository.java
│   └── InMemoryAccountRepository.java
└── service
    └── TransferService.java

src/test/java/com/nero/transferguard/service
└── TransferServiceTest.java
```

## Run locally

Requirements: JDK 21+

```bash
mkdir -p out
javac -d out $(find src/main/java src/test/java -name "*.java")
java -cp out com.nero.transferguard.service.TransferServiceTest
java -cp out com.nero.transferguard.Main
```

Example output:

```text
All TransferService tests passed.
Transfer completed: REQ-001
A-100 balance: 424.50
B-200 balance: 195.50
```

## Design decisions

### `BigDecimal` for money

Floating-point types such as `double` can introduce rounding surprises. TransferGuard uses `BigDecimal` so monetary values are explicit and predictable.

### Idempotency by request ID

A client may retry the same request after a timeout. The service stores completed request IDs and returns the original receipt instead of applying the transfer twice.

### Manual rollback

This version intentionally uses in-memory objects so the rollback behavior is visible in code. If the audit step fails after balances change, the service reverses both balance updates before rethrowing the error.

### Dependency boundaries

`AccountRepository` and `TransferAuditPort` separate business logic from storage and external side effects. This makes the service easier to test and creates a clean migration path to a real database or message system later.

### Deterministic time

`Clock` is injected instead of calling the system clock directly inside tests. This keeps time-based assertions stable.

## Tests covered

- Successful transfer
- Duplicate request / idempotency
- Rollback after downstream failure
- Insufficient funds

## Current limitations

This is intentionally an initial portfolio version, not a production banking system.

- Data is stored only in memory
- Idempotency state disappears after restart
- `synchronized` protects only one JVM process
- Rollback is manual rather than database-backed
- There is no REST API, authentication, persistent ledger, or distributed locking yet

These limitations are useful extension points for later pull requests as the project evolves.

## Planned evolution

Future iterations can introduce Spring Boot, REST DTOs, validation, JPA, database transactions with `@Transactional`, persistent idempotency keys, integration tests, Docker, and stronger concurrency controls.

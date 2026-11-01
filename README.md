# psoft-g1 — Library Management System

Spring Boot REST API for books, authors, genres, readers, lendings and users.

## Requirements

- JDK 17 or later (`java -version`). Maven is not needed: `./mvnw` downloads it.
- On Windows use `mvnw.cmd` instead of `./mvnw`.

## Build

```sh
./mvnw clean package -DskipTests      # builds target/psoft-g1-0.0.1-SNAPSHOT.jar
```

## Run locally

The default datasource expects an H2 server on `localhost`. To run without one, use an in-memory
database:

```sh
SPRING_DATASOURCE_URL='jdbc:h2:mem:psoft;IGNORECASE=TRUE' java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar
```

- **The API** is at `http://localhost:8080`, and Swagger UI is at `http://localhost:8080/swagger-ui`.
- **Health and build information:** `/actuator/health` and `/actuator/info`.
- **Demo data.** The `bootstrap` profile is active by default and seeds demo data, including users. Their
  credentials are in `src/main/java/pt/psoft/g1/psoftg1/bootstrapping/UserBootstrapper.java`.
- **Logging in.** `POST /api/public/login` with `{"username": "...", "password": "..."}` returns the JWT
  in the `Authorization` response header. Send it as `Authorization: Bearer <token>`.
- **Settings.** Any setting in `src/main/resources/application.properties` can be overridden with an
  environment variable or `--name=value`. For example: `SERVER_PORT=8181`, or
  `--lending.policy.standard.maxActiveLendings=5`.

## Tests

```sh
./mvnw test                     # unit tests (*Test), including the ArchUnit rules
./mvnw verify                   # unit + integration tests (*IT), coverage gate (60%), Checkstyle
./mvnw pitest:mutationCoverage  # mutation testing (fails below 75%); report in target/pit-reports
./mvnw spotless:check checkstyle:check   # formatting and style; ./mvnw spotless:apply fixes formatting
```

Reports:
- test results: `target/surefire-reports` and `target/failsafe-reports`;
- coverage: `target/site/jacoco/index.html`.

## CI/CD and deployment

Jenkins pipelines (`jenkins/`) build and test every push, then deploy `main` through DEV, PP and PROD.
Server and Jenkins setup: [`deploy/README.md`](deploy/README.md).

## Documentation

- **Architecture:** [`docs/architecture`](docs/architecture). It holds the C4 views of the base and the
  current project, and the ADD record (`add.md`).
- **State of the base project and of the current project:**
  [`docs/lmRedesign`](docs/lmRedesign).
- **Testing strategy and mutation testing:** [`docs/testing`](docs/testing).

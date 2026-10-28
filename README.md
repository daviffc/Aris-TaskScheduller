
# Aris — Workflow Automation Engine

[![Aris CI](https://github.com/daviffc/Aris-TaskScheduller/actions/workflows/ci.yml/badge.svg)](https://github.com/daviffc/Aris-TaskScheduller/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

Aris is a backend workflow automation engine built with Java 21 and Spring Boot. It supports scheduled and manually triggered workflows, ordered execution steps, retry policies, and persistent execution history.

The project focuses on backend engineering concepts including execution lifecycle management, relational persistence, asynchronous processing, automated testing, and containerized delivery.

## Features

- REST API for creating and retrieving workflows
- Manual and interval-based workflow triggers
- Ordered workflow steps
- Retry policies and configurable retry backoff
- Execution states and step-level logs
- PostgreSQL persistence using Spring JDBC
- Versioned database migrations with Flyway
- Asynchronous manual execution with a bounded thread pool
- Unit and integration tests with JUnit 5 and Testcontainers
- Docker-based application packaging
- GitHub Actions CI and container publishing
- Isolated local staging environment
- Staging image verification by SHA-256 digest
- Automated local staging update checks and smoke tests

## Technology Stack

| Area | Technologies |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| API | Spring Web, Bean Validation |
| Persistence | Spring JDBC, PostgreSQL 16 |
| Migrations | Flyway |
| Testing | JUnit 5, Spring Boot Test, Testcontainers |
| Build | Apache Maven |
| Containers | Docker, Docker Compose |
| CI/CD | GitHub Actions, GitHub Container Registry |
| Local deployment | PowerShell, Windows Task Scheduler |

## Architecture

Aris follows a layered backend architecture:

```text
                    REST API
                       |
                       v
                WorkflowService
                       |
          +------------+------------+
          |                         |
          v                         v
  WorkflowScheduler          WorkflowExecutor
          |                         |
          |                         v
          |                  Action Executors
          |                  LOG_MESSAGE
          |                  HTTP_REQUEST
          |
          v
    Repository Interfaces
          |
          v
     JDBC Repositories
          |
          v
       PostgreSQL
```

The application separates workflow definitions, execution logic, persistence, and HTTP endpoints.

See [Architecture Documentation](docs/architecture.md) for additional details.

## Execution Model

A workflow contains an ordered list of steps and a trigger.

Supported triggers:

- `MANUAL`: execution is requested through the REST API.
- `CRON`: execution is scheduled using an interval in seconds.

Despite the name `CRON`, the current implementation does not parse traditional cron expressions.

Supported action types:

- `LOG_MESSAGE`: writes a configured message.
- `HTTP_REQUEST`: executes a restricted HTTP request when explicitly enabled.

HTTP actions are disabled by default.

Each execution has a lifecycle that includes states such as:

```text
PENDING -> RUNNING -> SUCCEEDED
                  \-> FAILED
```

Step execution supports retries and records the outcome of each attempt.

## Requirements

- Java 21
- Apache Maven
- Docker Desktop or Docker Engine with Docker Compose
- Git

## Running Locally

### 1. Clone the repository

```bash
git clone https://github.com/daviffc/Aris-TaskScheduller.git
cd Aris-TaskScheduller
```

### 2. Configure PostgreSQL

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and replace `change_me` with a strong development password.

Use a password without shell-special characters for the simplest local setup.

Do not commit `.env`.

### 3. Start PostgreSQL

```bash
docker compose up -d
docker compose ps
```

The PostgreSQL development instance listens on port `5455` on localhost.

### 4. Configure the application environment

In Git Bash:

```bash
export DB_USER=aris
export DB_PASSWORD="$(sed -n 's/^DB_PASSWORD=//p' .env | tr -d '\r')"
export DB_URL="jdbc:postgresql://127.0.0.1:5455/aris"
```

### 5. Run the application

```bash
mvn spring-boot:run
```

Spring Boot starts on:

```text
http://127.0.0.1:8080
```

Flyway automatically applies the database migrations during startup.

## API Examples

### Create a workflow

```bash
curl -i -X POST http://127.0.0.1:8080/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Example workflow",
    "description": "A simple manual workflow",
    "triggerType": "MANUAL",
    "steps": [
      {
        "name": "Write a message",
        "actionType": "LOG_MESSAGE",
        "config": {
          "message": "Hello from Aris"
        },
        "maxRetries": 0,
        "retryBackoffMillis": 0
      }
    ]
  }'
```

The response contains the workflow ID.

### List workflows

```bash
curl http://127.0.0.1:8080/workflows
```

### Retrieve a workflow

```bash
curl http://127.0.0.1:8080/workflows/WORKFLOW_ID
```

### Trigger a manual execution

```bash
curl -i -X POST \
  http://127.0.0.1:8080/workflows/WORKFLOW_ID/executions
```

The API returns HTTP 202 with the execution ID.

### Retrieve an execution

```bash
curl http://127.0.0.1:8080/executions/EXECUTION_ID
```

### Identify the deployed version

```bash
curl http://127.0.0.1:8080/build-info
```

The build information endpoint exposes the application name and the Git commit embedded during the Docker image build.

## Automated Testing

Run all tests with:

```bash
mvn clean verify
```

The test suite covers domain restoration, workflow execution, retry behavior, scheduler behavior, PostgreSQL persistence, execution history, and selected security and capacity controls.

PostgreSQL integration tests use Testcontainers and require Docker.

## CI/CD

GitHub Actions runs Maven verification for pushes and pull requests.

After successful verification, pushes to the configured publishing branches can build and publish Docker images to GitHub Container Registry.

The published images include immutable commit-specific tags.

The local staging environment uses:

- A separate PostgreSQL instance and Docker volume
- An isolated Docker Compose project
- A published GHCR image
- A localhost-only API port
- Post-deployment smoke tests

A PowerShell script checks the staging image digest and can update the local container when a new image is available.

The local scheduled deployment process runs only while the host computer and Docker Desktop are available.

Production cloud deployment is not implemented.

## Security and Limitations

Aris is a personal engineering and learning project. It is not a production-ready multi-tenant workflow platform.

Current limitations include:

- No authentication or authorization for REST endpoints
- Intended for trusted local development and testing
- HTTP actions disabled by default
- HTTP action allowlisting is not a complete SSRF defense
- Scheduler designed for a single application instance
- No distributed locking or coordination
- In-flight executions are not automatically recovered after every unexpected crash
- Execution persistence does not provide exactly-once execution guarantees
- Workflow scheduling uses intervals rather than cron expressions
- No production cloud infrastructure or centralized monitoring

Do not expose the REST API directly to the public internet.

See [Security Policy](SECURITY.md).

## Repository Structure

```text
.github/workflows/          CI and image publishing
scripts/                    Local staging operations
src/main/java/
  actions/                  Action implementations
  api/                      REST endpoints and DTOs
  bootstrap/                Startup initialization
  config/                   Execution pool configuration
  domain/                   Domain models and enums
  engine/                   Workflow scheduling and execution
  repository/               Repository interfaces and JDBC implementations
  service/                  Application services
src/main/resources/
  db/migration/             Flyway migrations
src/test/java/              Unit and integration tests
compose.staging.yml         Isolated local staging
docker-compose.yaml         Development PostgreSQL
Dockerfile                  Multi-stage container build
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).

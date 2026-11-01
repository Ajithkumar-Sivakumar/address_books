# Address Book Microservices

A beginner-friendly address book built with Java 21, Spring Boot, Maven, and PostgreSQL. The Spring Boot application serves both a responsive browser interface and a REST API. Docker runs the app and PostgreSQL as separate containers on a private network, with PostgreSQL data kept in a named volume.

This first phase is intentionally local: it does not provision cloud resources, require a registry, or include Kubernetes, Helm, or CI/CD configuration.

## 1. Project overview

You can add, view, edit, delete, and search contacts by name, email, or phone number. Email addresses are unique, ignoring letter case. The API validates required fields, email format, and phone number format. Swagger UI documents the API, and Spring Boot Actuator provides a health endpoint.

The database contains a few sample contacts the first time its persistent data directory is initialized. You can remove those examples from the web page or through the API.

## 2. Application architecture

```text
Browser
  ├── HTML, CSS, JavaScript served from Spring Boot
  └── /api/contacts REST requests
          ↓
ContactController → ContactService → ContactRepository → PostgreSQL
```

The Java packages separate responsibilities:

- `controller/` handles HTTP requests and response codes.
- `service/` contains contact operations and business rules.
- `repository/` uses Spring Data JPA to query PostgreSQL.
- `entity/` maps the contacts table to Java.
- `dto/` defines validated API requests and API responses.
- `exception/` and `GlobalExceptionHandler` return consistent API errors.

`database/init/` contains SQL files used by the official PostgreSQL container on its first initialization. Hibernate also updates the schema when the application starts, which is convenient for this demonstration. Flyway applies the small versioned migration that removes the retired alternate phone column from an existing database.

## 3. Technology stack

- Java 21 LTS
- Spring Boot 3.4.5, Spring Web, Spring Data JPA, Bean Validation, and Actuator
- Maven
- PostgreSQL 17
- Hibernate and Flyway database migrations
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, Spring MockMvc, and H2 for tests
- HTML, CSS, and JavaScript
- Docker

H2 is used only by the automated integration tests. Normal application runs use PostgreSQL.

## 4. Prerequisites

For running the Java application directly, install:

1. A Java 21 JDK (not just a Java runtime).
2. Apache Maven 3.9 or later.
3. PostgreSQL 17, either installed locally or running as a container.

For Docker builds and the two-container setup, install Docker Desktop (or Docker Engine) and start its daemon.

Check that Java is installed:

```bash
java -version
```

The output should report version `21`. Check Maven:

```bash
mvn -version
```

The output should report Maven 3.9 or later and Java 21. Check Docker:

```bash
docker --version
docker info
```

`docker info` should show server details. If it reports that it cannot connect to the Docker daemon, start Docker Desktop before continuing.

## 5. PostgreSQL and application configuration

PostgreSQL is a separate database server. The application connects to it using these environment variables:

| Variable | Purpose | Default for local Java run |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL of PostgreSQL | `jdbc:postgresql://localhost:5432/addressbook` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `addressuser` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Required |
| `SERVER_PORT` | HTTP port for the app | `8080` |

The database name is `addressbook`. Copy the example environment file and replace the example password with a value chosen for your own machine:

```bash
cp .env.example .env
```

`.env` is ignored by Git. Do not put production credentials in `.env.example` or commit `.env`. The same password must be used for `POSTGRES_PASSWORD` and `SPRING_DATASOURCE_PASSWORD`.

The first PostgreSQL startup runs `database/init/01-create-schema.sql` and `database/init/02-sample-data.sql`. PostgreSQL only runs initialization files when its data directory is empty. Restarting containers keeps data in the `addressbook-data` volume. Removing that volume deletes the database data.

## 6. Run the application locally without a Docker app container

The Java process needs a PostgreSQL server. You can install PostgreSQL locally, or start the database container manually:

```bash
cp .env.example .env
docker network create addressbook-network
docker volume create addressbook-data
docker build -t addressbook-db:1.0 -f docker/database/Dockerfile .
docker run -d --name addressbook-db \
  --network addressbook-network \
  --restart unless-stopped \
  --env-file .env \
  -p 127.0.0.1:5432:5432 \
  -v addressbook-data:/var/lib/postgresql/data \
  addressbook-db:1.0
```

Edit `.env` and set your own local-only password. Wait for `docker inspect --format='{{.State.Health.Status}}' addressbook-db` to report `healthy` before starting the Java application.

Export the settings for the Java process. This example uses a Bash-compatible shell (including the default macOS zsh):

```bash
set -a
source .env
set +a
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/addressbook
```

The Java process on your computer connects through `localhost`. Build and start Spring Boot:

```bash
mvn clean package
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Stop the Java process with `Ctrl+C`. The PostgreSQL container can remain running for the next local run.

If PostgreSQL is installed locally rather than started in Docker, create a database named `addressbook` and a user named `addressuser`, then export the same datasource variables before starting the application.

## 7. Maven build and tests

Run the automated service and REST API tests, then package the executable JAR:

```bash
mvn clean package
```

The integration tests use an in-memory H2 database, so they do not need PostgreSQL or Docker. The resulting JAR is `target/address-book.jar`. To run just the tests:

```bash
mvn test
```

## 8. Build and run Docker containers without Compose

The app and database have separate Dockerfiles. Run these commands from the project root. Copy `.env.example` to `.env` and change the example password before starting the containers:

```bash
cp .env.example .env
docker network create addressbook-network
docker volume create addressbook-data
docker build -t addressbook-db:1.0 -f docker/database/Dockerfile .
docker build -t addressbook-app:1.0 -f docker/Dockerfile .
```

Start PostgreSQL, then wait for its health check to report `healthy`:

```bash
docker run -d --name addressbook-db \
  --network addressbook-network \
  --restart unless-stopped \
  --env-file .env \
  -p 127.0.0.1:5432:5432 \
  -v addressbook-data:/var/lib/postgresql/data \
  addressbook-db:1.0

docker inspect --format='{{.State.Health.Status}}' addressbook-db
```

Start the app on the same network. Its datasource URL uses the database container name; `localhost` inside the app container would refer to the app itself:

```bash
docker run -d --name addressbook-app \
  --network addressbook-network \
  --restart unless-stopped \
  --env-file .env \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://addressbook-db:5432/addressbook \
  -p 8080:8080 \
  addressbook-app:1.0
```

Open [http://localhost:8080](http://localhost:8080). If you change `SERVER_PORT` in `.env`, update the host port mapping to match. View container status and logs with `docker ps`, `docker logs -f addressbook-app`, and `docker logs -f addressbook-db`. No Docker Compose commands are needed.

## 9. Docker networking and persistent storage

Create a dedicated bridge network named `addressbook-network` and a named volume `addressbook-data`. Containers use Docker DNS names on that network: the app connects to `addressbook-db`, not `localhost`. PostgreSQL data remains in the volume when containers are removed.

List networks and inspect the app network:

```bash
docker network ls
docker network inspect addressbook-network
docker volume ls
docker volume inspect addressbook-data
```

## 10. REST API

All endpoints use JSON, except `DELETE`, which returns an empty `204 No Content` response.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/contacts` | List all contacts |
| `GET` | `/api/contacts/{id}` | Get one contact |
| `POST` | `/api/contacts` | Create a contact (`201 Created`) |
| `PUT` | `/api/contacts/{id}` | Replace a contact |
| `DELETE` | `/api/contacts/{id}` | Delete a contact |
| `GET` | `/api/contacts/search?name=ada` | Search first and last names |
| `GET` | `/api/contacts/search?email=example.com` | Search email addresses |
| `GET` | `/api/contacts/search?phone=555` | Search phone numbers |

Use only one search parameter at a time. Names and email/phone searches ignore case. An email already used by another contact returns `409 Conflict`; invalid input returns `400 Bad Request`; a missing ID returns `404 Not Found`.

Example request body:

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phoneNumber": "+1 555 0100",
  "address": "12 Analytical Engine Way",
  "city": "London",
  "state": "England",
  "country": "United Kingdom",
  "postalCode": "SW1A 1AA",
  "company": "Engine Works",
  "jobTitle": "Mathematician"
}
```

## 11. Try the API with curl

List contacts:

```bash
curl http://localhost:8080/api/contacts
```

Create a contact:

```bash
curl -i -X POST http://localhost:8080/api/contacts \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Katherine",
    "lastName": "Johnson",
    "email": "katherine.johnson@example.com",
    "phoneNumber": "+1 555 0123",
    "address": "NASA Langley",
    "city": "Hampton",
    "state": "Virginia",
    "country": "United States",
    "postalCode": "23681",
    "company": "NASA",
    "jobTitle": "Mathematician"
  }'
```

The response includes the created contact ID and a `Location` header. Use that ID to fetch, update, or delete it:

```bash
curl http://localhost:8080/api/contacts/1
curl 'http://localhost:8080/api/contacts/search?name=Katherine'
curl 'http://localhost:8080/api/contacts/search?email=example.com'
curl 'http://localhost:8080/api/contacts/search?phone=555'
```

Update a contact by sending a complete request body:

```bash
curl -i -X PUT http://localhost:8080/api/contacts/1 \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Katherine",
    "lastName": "Johnson",
    "email": "katherine.johnson@example.com",
    "phoneNumber": "+1 555 0123",
    "address": "NASA Langley",
    "city": "Hampton",
    "state": "Virginia",
    "country": "United States",
    "postalCode": "23681",
    "company": "NASA",
    "jobTitle": "Lead Mathematician"
  }'
```

Delete the contact:

```bash
curl -i -X DELETE http://localhost:8080/api/contacts/1
```

IDs in these commands are examples; use an ID returned by your own `POST` response.

## 12. Swagger UI and health

Open Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). The generated OpenAPI document is at [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

Check application health at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). The Docker health check uses the readiness endpoint at `/actuator/health/readiness`.

## 13. Database scripts

- `database/init/01-create-schema.sql` creates the contact table, case-insensitive unique email index, and search indexes.
- `database/init/02-sample-data.sql` adds three demonstration contacts.
- `src/main/resources/db/migration/V1__remove_alternate_phone_number.sql` removes the retired phone column from existing PostgreSQL databases when the app starts. All other contact fields and records are preserved.

These scripts run automatically when the PostgreSQL data volume is created for the first time. If you change an initialization script later, it will not be rerun against an existing volume. Back up any data you want to keep before resetting the volume.

## 14. Troubleshooting

**`mvn` or Java is not found:** Install Maven and a Java 21 JDK, then open a new terminal and rerun `java -version` and `mvn -version`.

**Docker cannot connect to its daemon:** Start Docker Desktop and wait for it to finish starting. Verify with `docker info`.

**The app cannot connect to PostgreSQL:** Check `docker ps` and `docker logs addressbook-db`. The app uses `addressbook-db` inside Docker; a Java process on your computer uses `localhost`. Confirm the database and app passwords match and that both containers are on `addressbook-network`.

**Port 8080 or 5432 is already in use:** Stop the other service or change the host side of the `-p` mapping to an unused port. If you change the app's `SERVER_PORT`, also update its container port mapping.

**Sample rows do not appear:** PostgreSQL only runs initialization scripts on an empty data directory. Existing volumes keep their existing rows and schema. Do not remove the volume unless you intend to delete its data.

**A contact email is rejected as a duplicate:** Email uniqueness is case-insensitive. Try another email address.

**A request returns `400 Bad Request`:** Check that first name, last name, email, and phone are present, that the email is valid, and that phone numbers contain 7–30 digits or common phone symbols.

## 15. Cleanup

Stop and remove the containers and network while keeping database data:

```bash
docker stop addressbook-app addressbook-db
docker rm addressbook-app addressbook-db
docker network rm addressbook-network
```

Remove the database volume only if you intend to permanently delete its data:

```bash
docker volume rm addressbook-data
```

Remove the locally built images when they are no longer needed:

```bash
docker image rm addressbook-app:1.0 addressbook-db:1.0
```

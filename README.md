# HMCTS Dev Test Backend
This will be the backend for the brand new HMCTS case management system. As a potential candidate we are leaving
this in your hands. Please refer to the brief for the complete list of tasks! Complete as much as you can and be
as creative as you want.

You should be able to run `./gradlew build` to start with to ensure it builds successfully. Then from that you
can run the service in IntelliJ (or your IDE of choice) or however you normally would.

There is an example endpoint provided to retrieve an example of a case. You are free to add/remove fields as you
wish.

## Prerequisites

- **JDK 21** installed, with `JAVA_HOME` pointing to it.
- **Docker** installed and running, with **Docker Compose v2 or later** available
  (`docker compose`)
- Ports **4000** (backend) and **5432** (PostgreSQL) available locally.

The Gradle wrapper is included, so a separate Gradle installation is not required.
On Windows, use `gradlew.bat` instead of `./gradlew`.

## Local PostgreSQL

Start the database before running the backend:

```sh
docker compose up -d --wait
./gradlew bootRun
```
## API documentation

With the backend running, open [Swagger UI](http://localhost:4000/swagger-ui/index.html)
to explore and try the API endpoints.

PostgreSQL is exposed on localhost port 5432 with database/user `hmcts` and password
`local-development-only`. These defaults are for local development only. Override `DB_PORT`,
`DB_NAME`, `DB_USER_NAME` and `DB_PASSWORD` as needed. Compose reads these from `.env`;
when running the backend, also export the same variables in its environment (or set them in your IDE).
`DB_HOST` defaults to `localhost`. Flyway creates the database tables on application startup.

Data persists in the `postgres_data` Docker volume when running `docker compose down`.
Running `docker compose down -v` also deletes the stored data.

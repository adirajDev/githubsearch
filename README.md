# GitHub Repository Searcher

A Spring Boot REST API that searches GitHub repositories, stores the returned results in PostgreSQL, and lets clients filter and page through repositories stored by previous searches.

## Features

- Search GitHub by repository name, with an optional language filter.
- Sort GitHub search results by stars, forks, or last update. GitHub results are requested in descending order, with a page size of 30.
- Upsert search results by GitHub repository ID so repeated searches refresh existing rows.
- List stored repositories with optional language and minimum-star filters, sorting, and pagination.
- Expose generated OpenAPI documentation and Swagger UI.
- Use Flyway migrations to manage the database schema.
- Return validation and application errors as RFC 9457 Problem Details.

## Requirements

- Java 25
- Docker and Docker Compose, for the local PostgreSQL database and the Testcontainers-based integration tests
- Maven (the repository includes the Maven Wrapper)

## Run locally

1. Start PostgreSQL:

   ```sh
   docker compose up -d postgres
   ```

   The Compose service exposes PostgreSQL on port `5433`. Its defaults are database `github_search` and username `github_user`. Set `DB_PASSWORD` in your environment before starting it. The application also reads an optional `.env` file from the project root.

2. Set the database password used by the application to the same value used by Compose:

   ```sh
   export DB_PASSWORD='your-local-database-password'
   ```

   The default JDBC URL is `jdbc:postgresql://localhost:5433/github_search`. You can override it with `DB_URL`. An optional GitHub token can be set with `GITHUB_TOKEN`; authenticated requests have a higher GitHub API rate limit. The application uses the public GitHub API by default and does not require a token to start.

3. Run the application:

   ```sh
   ./mvnw spring-boot:run
   ```

   The API listens on port `8080` by default. Set `PORT` to use a different port. Flyway applies the database migrations at startup, and Hibernate validates its mappings against the migrated schema.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5433/github_search` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `github_user` | PostgreSQL username |
| `DB_PASSWORD` | Empty | PostgreSQL password; provide the same value to Compose and the application |
| `GITHUB_TOKEN` | Empty | Optional GitHub personal access token |
| `PORT` | `8080` | HTTP server port |

GitHub API base URL and HTTP connect/read timeouts are configured in `src/main/resources/application.yaml`.

## API

### Search GitHub and store results

`POST /api/github/search`

Example request:

```json
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars"
}
```

`query` is required and must not be blank (maximum 200 characters). `language` is optional (maximum 50 characters). `sort` accepts `stars`, `forks`, or `updated`; it defaults to `stars` when omitted. Sort values are case-insensitive. Search results match repository names and, when supplied, the language. The API requests up to 30 results from GitHub and stores valid items by their GitHub ID.

Example response:

```json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [
    {
      "id": 123456,
      "name": "spring-boot-example",
      "description": "An example repository",
      "owner": "example-user",
      "language": "Java",
      "stars": 450,
      "forks": 120,
      "lastUpdated": "2025-01-01T12:00:00Z"
    }
  ]
}
```

When GitHub returns no valid matching repositories, the response is still `200 OK`, with `message` set to `No repositories found` and an empty `repositories` array.

### List stored repositories

`GET /api/github/repositories`

Supported query parameters:

| Parameter | Default | Description |
| --- | --- | --- |
| `language` | No filter | Exact language match, ignoring case; maximum 50 characters |
| `minStars` | No minimum | Minimum star count; must be zero or greater |
| `sort` | `stars` | Sort by `stars`, `forks`, or `updated`, descending |
| `page` | `0` | Zero-based page index |
| `size` | `30` | Page size, from 1 to 100 |

Results use GitHub ID as a secondary ascending sort key to make page ordering stable. The response contains a `repositories` array. `X-Total-Count` reports the number of matching stored repositories across all pages.

Example:

```sh
curl 'http://localhost:8080/api/github/repositories?language=Java&minStars=100&sort=forks&page=0&size=20'
```

The stored-repositories endpoint only queries the local database; it does not call GitHub.

## API documentation

When the application is running, open [Swagger UI](http://localhost:8080/swagger-ui/index.html). The OpenAPI JSON document is available at [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

## Tests

Run the full test suite with:

```sh
./mvnw test
```

Integration tests use Testcontainers and PostgreSQL 18, so Docker must be available to the test process. The suite covers domain validation, GitHub API request/response handling, database migrations and upserts, service behavior, controller responses, and the generated OpenAPI contract.

## Project layout

```text
src/main/java/com/adidev/githubsearch/
  domain/       Domain models, search criteria, sort fields, and filters
  github/       GitHub HTTP client, configuration, API models, and exceptions
  mapper/       Mapping between GitHub API items, persistence entities, and responses
  repository/   Database access, specifications, and PostgreSQL upsert support
  service/      GitHub search and stored-repository workflows
  web/          REST controller, error handling, and OpenAPI configuration
  web/dto/      API request and response models
src/main/resources/
  application.yaml
  db/migration/ Flyway database migrations
src/test/java/  Unit and integration tests
```

## Implementation notes

- GitHub API calls are made before database writes, so a database transaction is not held open while waiting for GitHub.
- Repository upserts use PostgreSQL `ON CONFLICT` on `github_id`; later results refresh the stored fields and fetch timestamp.
- Invalid items missing fields required by the database are skipped and logged.
- GitHub rate limits are returned as `429 Too Many Requests` with a `Retry-After` header. Upstream GitHub failures are reported as `502`, request timeouts as `504`, and database failures as `503`.

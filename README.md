# CTRL

**Take back your feed.**

CTRL is a social media platform that puts the user in charge of what they see and how long they scroll. The recommendation algorithm is not a black box: every user can tune the weights that rank their feed, each post carries labels explaining why it was shown, and usage limits are built into the product instead of being an afterthought.

The project was built by a team of five as part of the *Advanced Software Engineering* course at TU Wien in the summer term of 2026.

## Features

**A feed you control**

- Adjustable ranking: per-user weights for interaction, recency, exploration, source quality, popularity and topic match
- Explainable posts: labels such as `TRUSTED_SOURCE`, `EXTENDED_NETWORK`, `POPULAR`, `RECENT`, `HIGHLY_DISCUSSED`, `DISPUTED_SOURCE` or `AI_GENERATED` show why a post appears
- Topics that users subscribe to, assigned to posts automatically through embedding similarity
- A public feed and a feed of followed accounts

**Healthy usage**

- Session limits by time and by number of posts viewed, with an optional snooze
- Usage analytics with weekly averages and detailed per-session statistics

**Social platform**

- Posts with images and video (thumbnails and transcoding via ffmpeg), comments and voting
- Follows, friendships and blocking
- Real-time chat over WebSocket (STOMP, with a SockJS fallback), including sharing posts into chats
- Search for users and posts
- Sources with a community trust score (Wilson score interval)
- Content import from public Mastodon accounts

**Accounts and moderation**

- Registration, login (JWT), e-mail verification and password reset
- Business accounts with identity verification through an ID Austria mock (Keycloak, OpenID Connect)
- Reporting of posts, comments, accounts and sources, with an admin area for reviewing them

## Architecture

| Component | Technology | Port (local) |
|---|---|---|
| Frontend | Angular 21, PrimeNG, Chart.js | 4200 |
| Backend | Spring Boot 4, Java 21 | 8080 |
| Database | PostgreSQL 18 with pgvector | 5433 |
| Object storage | Garage (S3-compatible) | 3900 |
| Embeddings | Infinity serving `jinaai/jina-clip-v1` | 7997 |
| Identity verification | Keycloak 24 (ID Austria mock realm) | 8081 |

The backend is a modular monolith organised by feature (`algorithm`, `auth`, `chat`, `comments`, `feed`, `file`, `label`, `post`, `report`, `search`, `session`, `source`, `topic`, `user`). Media is stored in a private and a public bucket and served through presigned URLs. Posts and topics are embedded with a CLIP model, and the vectors are stored and compared in PostgreSQL.

## Repository layout

```
backend/    Spring Boot application (Maven)
frontend/   Angular application, Cypress end-to-end tests
setup/      Init scripts and config for Postgres, Garage and Keycloak
k8s/        Kubernetes manifests of the deployment
ci/         Dockerfiles for the CI images
compose.yaml, Makefile   Local development stack
```

## Getting started

### Prerequisites

- Docker with Docker Compose
- Node.js 22 and npm
- Java 21 and Maven (only for running backend tests outside Docker)
- A Google Cloud OAuth client for the Gmail API. The backend creates its Gmail client at startup and expects `backend/src/main/resources/credentials.json` and an authorised token in `backend/tokens/`. Both are git-ignored and not part of this repository.

### Configuration

Copy the example environment file and fill in your own values:

```bash
cp .env.example .env
```

`JWT_SECRET`, `GARAGE_RPC_SECRET`, `GARAGE_ACCESS_KEY` and `GARAGE_SECRET_KEY` must match the formats noted in `.env.example`. Set `MAIL_REQUIRED=false` to register accounts without e-mail verification.

### Run

Start the backend and its services:

```bash
docker compose up -d
```

On the first start, create the storage buckets and import the access key:

```bash
./setup/garage/garage-setup.sh
```

Start the frontend dev server, which proxies `/api` and `/ws` to the backend:

```bash
cd frontend && npm install && npx ng serve
```

The app is then available at http://localhost:4200. An admin account is created from `ADMIN_USERNAME` and `ADMIN_PASSWORD`, and a handful of test users are seeded on startup.

The `Makefile` wraps the common commands: `make start`, `make stop`, `make restart`, `make logs` and `make db-reset` (which deletes all data).

## Testing

**Backend** unit and integration tests need the Postgres container from the Compose stack (port 5433):

```bash
mvn test -f backend
```

**Frontend** unit tests run with Vitest:

```bash
cd frontend && npx ng test
```

**End-to-end** tests run with Cypress against a running stack:

```bash
cd frontend && npm run cypress:run
```

The admin scenarios log in with the `TEST_ADMIN_PASSWORD` from `frontend/cypress.config.ts`, so `ADMIN_PASSWORD` in `.env` has to match it.

**Linting** uses Spotless (Google Java Format) for the backend and ESLint for the frontend:

```bash
mvn spotless:check -f backend
```

```bash
cd frontend && npm run lint
```

## Deployment

The project was deployed to Kubernetes; the manifests are in `k8s/`. The original pipeline (`.gitlab-ci.yml`) ran linting, tests and builds on GitLab CI and published the backend image with Jib. That pipeline refers to the university's GitLab registry and does not run on GitHub.

## Project history

This repository was migrated from the university's GitLab instance. Issue references in commit messages, such as `feat(#13)`, point to the original issue tracker, which is no longer available.

## Team

Philippe Zorman, Franziska Schrefl, Christoph Zeitler, Felix Wildmann and Marlene Lehotzki.

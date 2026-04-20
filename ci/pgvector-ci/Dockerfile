FROM registry.reset.inso-w.at/pub/docker/pgvector:pg18-trixie

# Prepare writable runtime dirs at build time (root allowed here).
USER root
RUN mkdir -p /ci-pgdata /var/run/postgresql \
    && chown -R postgres:postgres /ci-pgdata /var/run/postgresql \
    && chmod 700 /ci-pgdata \
    && chmod 3775 /var/run/postgresql

# Ensure new databases in CI enable pgvector before app schema creation.
COPY ci/pgvector-ci/init-pgvector.sql /docker-entrypoint-initdb.d/00-init-pgvector.sql

# Default data dir for CI service container.
ENV PGDATA=/ci-pgdata

# Keep normal postgres runtime user.
USER postgres

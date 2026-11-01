FROM postgres:17-alpine

COPY database/init/ /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=5s --timeout=5s --start-period=10s --retries=10 \
    CMD pg_isready -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

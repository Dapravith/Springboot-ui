-- Runs only on a fresh postgres volume (first `docker compose up`).
-- POSTGRES_DB creates customer_db; the remaining service databases are created here.
-- Each service owns its own database: no shared schema between microservices.
CREATE DATABASE account_db OWNER dev_user;

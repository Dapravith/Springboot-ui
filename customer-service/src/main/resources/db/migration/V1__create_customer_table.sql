-- Schema is owned by migrations, not by Hibernate.
-- hibernate.ddl-auto is set to `validate`, so a drift between this file and the
-- entity mapping fails fast at startup instead of silently altering the database.

CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE customer (
    id              BIGINT       PRIMARY KEY,
    customer_number VARCHAR(20)  NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    registered_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_number UNIQUE (customer_number),
    CONSTRAINT uk_customer_email  UNIQUE (email)
);

-- Supports the registration duplicate check and the lookup-by-number endpoint.
CREATE INDEX idx_customer_status ON customer (status);

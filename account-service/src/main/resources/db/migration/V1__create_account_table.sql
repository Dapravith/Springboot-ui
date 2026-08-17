-- Schema is owned by migrations, not by Hibernate.
-- hibernate.ddl-auto is set to `validate`, so a drift between this file and the
-- entity mapping fails fast at startup instead of silently altering the database.

CREATE SEQUENCE account_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE account (
    id               BIGINT        PRIMARY KEY,
    account_number   VARCHAR(20)   NOT NULL,
    customer_number  VARCHAR(20)   NOT NULL,
    balance_amount   NUMERIC(19,4) NOT NULL,
    balance_currency VARCHAR(3)    NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    opened_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_account_number UNIQUE (account_number),
    -- A balance must never go negative. The domain enforces this, and the
    -- database enforces it again: a bug in any future code path still cannot
    -- persist an overdrawn account.
    CONSTRAINT ck_account_balance_non_negative CHECK (balance_amount >= 0)
);

-- Supports "list the accounts belonging to this customer", the hottest query.
CREATE INDEX idx_account_customer_number ON account (customer_number);

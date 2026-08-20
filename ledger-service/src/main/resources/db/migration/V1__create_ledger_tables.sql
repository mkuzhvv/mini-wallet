CREATE TABLE wallets (
                         id         UUID          PRIMARY KEY,
                         user_id    VARCHAR(64)   NOT NULL,
                         currency   VARCHAR(3)    NOT NULL,
                         status     VARCHAR(20)   NOT NULL,
                         balance    NUMERIC(19,2) NOT NULL DEFAULT 0,
                         created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
                         CHECK (user_id = 'SYSTEM' OR balance >= 0),
                         CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE TABLE ledger_transactions (
                                     id               UUID          PRIMARY KEY,
                                     type             VARCHAR(20)   NOT NULL,
                                     status           VARCHAR(20)   NOT NULL,
                                     amount           NUMERIC(19,2) NOT NULL,
                                     currency         VARCHAR(3)    NOT NULL,
                                     source_wallet_id UUID          NOT NULL REFERENCES wallets (id),
                                     target_wallet_id UUID          NOT NULL REFERENCES wallets (id),
                                     external_ref     UUID,
                                     idempotency_key  VARCHAR(64)   NOT NULL,
                                     created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                     CHECK (amount > 0),
                                     CHECK (source_wallet_id <> target_wallet_id),
                                     CHECK (type IN ('DEPOSIT', 'TRANSFER')),
                                     CHECK (status IN ('POSTED', 'REJECTED')),
                                     CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE ledger_entries (
                                id             UUID          PRIMARY KEY,
                                transaction_id UUID          NOT NULL REFERENCES ledger_transactions (id),
                                wallet_id      UUID          NOT NULL REFERENCES wallets (id),
                                direction      VARCHAR(10)   NOT NULL,
                                amount         NUMERIC(19,2) NOT NULL,
                                created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                CHECK (amount > 0),
                                CHECK (direction IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_entries_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_entries_wallet_id      ON ledger_entries (wallet_id);

CREATE TABLE outbox (
                        id         UUID         PRIMARY KEY,
                        event_type VARCHAR(50)  NOT NULL,
                        payload    TEXT         NOT NULL,
                        created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        sent_at    TIMESTAMPTZ
);

INSERT INTO wallets (id, user_id, currency, status, balance)
VALUES ('00000000-0000-0000-0000-000000000000', 'SYSTEM', 'RUB', 'ACTIVE', 0);
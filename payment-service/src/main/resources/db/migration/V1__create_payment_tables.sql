CREATE TABLE payments (
                          id               UUID          PRIMARY KEY,
                          idempotency_key  VARCHAR(64)   NOT NULL,
                          type             VARCHAR(20)   NOT NULL,
                          source_wallet_id UUID,
                          target_wallet_id UUID          NOT NULL,
                          amount           NUMERIC(19,2) NOT NULL,
                          currency         VARCHAR(3)    NOT NULL,
                          status           VARCHAR(20)   NOT NULL,
                          failure_reason   VARCHAR(50),
                          description      VARCHAR(255),
                          created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

                          CHECK (amount > 0),
                          CHECK (type IN ('DEPOSIT', 'TRANSFER')),
                          CHECK (status IN ('NEW', 'PROCESSING', 'SUCCESS', 'REJECTED', 'FAILED')),
                          CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key)
);
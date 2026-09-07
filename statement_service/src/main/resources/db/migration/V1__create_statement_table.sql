CREATE TABLE statement_entries (
                                   id                     UUID          PRIMARY KEY,
                                   transaction_id         UUID          NOT NULL,
                                   wallet_id              UUID          NOT NULL,
                                   direction              VARCHAR(10)   NOT NULL,
                                   amount                 NUMERIC(19,2) NOT NULL,
                                   currency               VARCHAR(3)    NOT NULL,
                                   counterparty_wallet_id UUID,
                                   type                   VARCHAR(20)   NOT NULL,
                                   created_at             TIMESTAMPTZ   NOT NULL,

                                   CHECK (direction IN ('IN', 'OUT')),
                                   CHECK (type IN ('DEPOSIT', 'TRANSFER')),
                                   CONSTRAINT uq_statement_tx_wallet UNIQUE (transaction_id, wallet_id)
);
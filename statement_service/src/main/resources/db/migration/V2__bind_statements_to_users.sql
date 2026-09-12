ALTER TABLE statement_entries
    ADD COLUMN user_id VARCHAR(64);

UPDATE statement_entries
SET user_id = 'LEGACY'
WHERE user_id IS NULL;

ALTER TABLE statement_entries
    ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX idx_statement_user_wallet_created
    ON statement_entries (user_id, wallet_id, created_at DESC);

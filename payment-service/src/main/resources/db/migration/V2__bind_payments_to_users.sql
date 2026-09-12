ALTER TABLE payments
    ADD COLUMN user_id VARCHAR(64);

UPDATE payments
SET user_id = 'LEGACY'
WHERE user_id IS NULL;

ALTER TABLE payments
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE payments
    DROP CONSTRAINT uq_payments_idempotency_key;

ALTER TABLE payments
    ADD CONSTRAINT uq_payments_user_idempotency_key UNIQUE (user_id, idempotency_key);

CREATE INDEX idx_payments_user_id ON payments (user_id);

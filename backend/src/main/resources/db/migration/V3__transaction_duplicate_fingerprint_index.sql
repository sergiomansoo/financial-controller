CREATE INDEX idx_transactions_user_duplicate_fingerprint
    ON transactions (user_id, duplicate_fingerprint);

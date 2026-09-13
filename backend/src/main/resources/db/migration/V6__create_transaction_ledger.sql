-- Phase 7: transaction and payment ledger. No transaction rows are seeded.
ALTER TABLE transactions ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE transactions ADD COLUMN created_by BIGINT;
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_created_by FOREIGN KEY (created_by) REFERENCES users(id);

CREATE INDEX idx_transactions_lot ON transactions(lot_id);
CREATE INDEX idx_transactions_collector ON transactions(collector_id);
CREATE INDEX idx_transactions_recycler ON transactions(recycler_id);
CREATE INDEX idx_transactions_payment_status ON transactions(payment_status);
CREATE INDEX idx_transactions_transaction_time ON transactions(transaction_time);

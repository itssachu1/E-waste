-- Phase 4: real price records. No seed data is inserted.
ALTER TABLE price_records ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE price_records ADD COLUMN IF NOT EXISTS verified_by BIGINT;
ALTER TABLE price_records ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_price_records_material ON price_records(material_id);
CREATE INDEX IF NOT EXISTS idx_price_records_location ON price_records(location);
CREATE INDEX IF NOT EXISTS idx_price_records_verification_status ON price_records(verification_status);
CREATE INDEX IF NOT EXISTS idx_price_records_quoted_at ON price_records(quoted_at);

ALTER TABLE price_records ADD CONSTRAINT fk_price_verified_by
    FOREIGN KEY (verified_by) REFERENCES users(id);

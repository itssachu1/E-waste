-- Phase 6: authenticated digital lots. No lots are seeded.
ALTER TABLE lots RENAME COLUMN selected_quote_id TO selected_price_record_id;
CREATE TABLE IF NOT EXISTS lot_year_counters (
    year_value INT PRIMARY KEY,
    next_number INT NOT NULL
);

CREATE INDEX idx_lots_collector ON lots(collector_id);
CREATE INDEX idx_lots_status ON lots(status);
CREATE INDEX idx_lots_material ON lots(material_id);
CREATE INDEX idx_lots_created_at ON lots(created_at);

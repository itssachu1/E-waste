-- Phase 2: persistent e-waste workflow schema.
-- Structure only: no prices, recyclers, transactions, field surveys, or demo accounts.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    ward_area VARCHAR(50)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS location VARCHAR(160);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS material_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(80) NOT NULL,
    subcategory VARCHAR(80),
    device_type VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    common_name VARCHAR(120),
    recoverable_materials TEXT,
    typical_unit VARCHAR(30),
    requires_special_handling BOOLEAN NOT NULL DEFAULT FALSE,
    battery_related BOOLEAN NOT NULL DEFAULT FALSE,
    crt_related BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recyclers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_name VARCHAR(180) NOT NULL,
    contact_name VARCHAR(120),
    phone VARCHAR(30),
    address VARCHAR(300),
    city VARCHAR(100),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    accepted_materials TEXT,
    pickup_available BOOLEAN NOT NULL DEFAULT FALSE,
    pickup_radius DECIMAL(8,2),
    service_area VARCHAR(300),
    authorization_status VARCHAR(30) NOT NULL,
    registration_number VARCHAR(120),
    verification_source VARCHAR(500),
    last_verified_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    location VARCHAR(160) NOT NULL,
    buyer_type VARCHAR(40) NOT NULL,
    buyer_id BIGINT,
    rate DECIMAL(14,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    grade VARCHAR(80),
    condition_description VARCHAR(300),
    source_type VARCHAR(40) NOT NULL,
    source_reference VARCHAR(500),
    quoted_at TIMESTAMP NOT NULL,
    valid_until TIMESTAMP,
    verification_status VARCHAR(20) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_material FOREIGN KEY (material_id) REFERENCES material_master(id),
    CONSTRAINT fk_price_buyer FOREIGN KEY (buyer_id) REFERENCES recyclers(id),
    CONSTRAINT fk_price_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS lots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lot_reference VARCHAR(30) NOT NULL UNIQUE,
    collector_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    description VARCHAR(1000),
    photo_url VARCHAR(500),
    approximate_weight DECIMAL(12,3),
    final_weight DECIMAL(12,3),
    estimated_value DECIMAL(14,2),
    selected_quote_id BIGINT,
    final_sale_amount DECIMAL(14,2),
    recycler_id BIGINT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lot_collector FOREIGN KEY (collector_id) REFERENCES users(id),
    CONSTRAINT fk_lot_material FOREIGN KEY (material_id) REFERENCES material_master(id),
    CONSTRAINT fk_lot_quote FOREIGN KEY (selected_quote_id) REFERENCES price_records(id),
    CONSTRAINT fk_lot_recycler FOREIGN KEY (recycler_id) REFERENCES recyclers(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lot_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    recycler_id BIGINT,
    amount DECIMAL(14,2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(160),
    payment_status VARCHAR(30) NOT NULL,
    transaction_time TIMESTAMP,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_lot FOREIGN KEY (lot_id) REFERENCES lots(id),
    CONSTRAINT fk_transaction_collector FOREIGN KEY (collector_id) REFERENCES users(id),
    CONSTRAINT fk_transaction_recycler FOREIGN KEY (recycler_id) REFERENCES recyclers(id)
);

CREATE TABLE IF NOT EXISTS handover_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lot_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    recycler_id BIGINT NOT NULL,
    photo_url VARCHAR(500),
    weight DECIMAL(12,3),
    handed_over_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    collector_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    recycler_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    handover_status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_handover_lot FOREIGN KEY (lot_id) REFERENCES lots(id),
    CONSTRAINT fk_handover_collector FOREIGN KEY (collector_id) REFERENCES users(id),
    CONSTRAINT fk_handover_recycler FOREIGN KEY (recycler_id) REFERENCES recyclers(id)
);

CREATE TABLE IF NOT EXISTS lot_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lot_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_data TEXT,
    actor_id BIGINT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lot_event_lot FOREIGN KEY (lot_id) REFERENCES lots(id),
    CONSTRAINT fk_lot_event_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS field_surveys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collector_identifier VARCHAR(160) NOT NULL,
    material_type VARCHAR(120) NOT NULL,
    approximate_quantity DECIMAL(12,3),
    buying_rate DECIMAL(14,2),
    selling_rate DECIMAL(14,2),
    buyer_recycler VARCHAR(180),
    location VARCHAR(160),
    collection_frequency VARCHAR(100),
    current_process TEXT,
    problems_faced TEXT,
    surveyed_at TIMESTAMP NOT NULL,
    researcher VARCHAR(160) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_price_material_validity ON price_records(material_id, valid_until, verification_status);
CREATE INDEX IF NOT EXISTS idx_lots_collector_status ON lots(collector_id, status);
CREATE INDEX IF NOT EXISTS idx_transactions_collector_status ON transactions(collector_id, payment_status);
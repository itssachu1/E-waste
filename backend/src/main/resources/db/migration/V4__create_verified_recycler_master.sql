-- Phase 5: normalized recycler master. No recycler records are seeded.
ALTER TABLE recyclers ADD COLUMN created_by BIGINT;
ALTER TABLE recyclers ADD COLUMN verified_by BIGINT;

CREATE TABLE IF NOT EXISTS recycler_accepted_materials (
    recycler_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    PRIMARY KEY (recycler_id, material_id),
    CONSTRAINT fk_recycler_material_recycler FOREIGN KEY (recycler_id) REFERENCES recyclers(id),
    CONSTRAINT fk_recycler_material_material FOREIGN KEY (material_id) REFERENCES material_master(id)
);

ALTER TABLE recyclers ADD CONSTRAINT fk_recycler_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE recyclers ADD CONSTRAINT fk_recycler_verified_by FOREIGN KEY (verified_by) REFERENCES users(id);

CREATE INDEX idx_recyclers_city ON recyclers(city);
CREATE INDEX idx_recyclers_authorization_status ON recyclers(authorization_status);
CREATE INDEX idx_recyclers_active ON recyclers(active);
CREATE INDEX idx_recycler_accepted_materials_material ON recycler_accepted_materials(material_id);

-- The previous column was never populated by production migrations. Accepted materials are now relational.
ALTER TABLE recyclers DROP COLUMN accepted_materials;

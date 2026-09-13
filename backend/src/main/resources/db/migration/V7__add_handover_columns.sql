-- Phase 8: handover confirmation.
--
-- KNOWN SIMPLIFICATION (hackathon scope): the handover confirmation is stored
-- directly on the lot row (handed_over_at / recycler_confirmed_at) instead of
-- populating the full handover_records audit table (photo/GPS/lat/long). The
-- handover_records table from V1 remains unused on purpose. If time permits,
-- this can be expanded later into a full audit trail with photo + GPS fields.
ALTER TABLE lots ADD COLUMN handed_over_at TIMESTAMP;
ALTER TABLE lots ADD COLUMN recycler_confirmed_at TIMESTAMP;

CREATE INDEX idx_lots_handover ON lots(handed_over_at);
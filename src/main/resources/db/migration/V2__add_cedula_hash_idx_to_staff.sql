-- Migration: Add cedula_hash_idx to staff.staff_user
-- Purpose: Allow staff authentication via Didit verification without storing cedula in plaintext
-- Security: Hash is SHA-256, unique index for fast lookups

ALTER TABLE staff.staff_user
ADD COLUMN IF NOT EXISTS cedula_hash_idx VARCHAR(128) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_staff_cedula_hash ON staff.staff_user(cedula_hash_idx);

COMMENT ON COLUMN staff.staff_user.cedula_hash_idx IS 'SHA-256 hash of cedula for Didit verification linkage. Never stores plaintext cedula.';
